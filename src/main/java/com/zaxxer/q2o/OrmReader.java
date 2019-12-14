/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.q2o;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OrmReader
 */
// MULTIPLESTRINGS:OFF
class OrmReader extends OrmBase
{
   private static final int CACHE_SIZE = Integer.getInteger("com.zaxxer.sansorm.statementCacheSize", 500);

   private static final Map<String, String> fromClauseStmtCache;

   static {
      fromClauseStmtCache = Collections.synchronizedMap(new LinkedHashMap<String, String>(CACHE_SIZE) {
         private static final long serialVersionUID = 6259942586093454872L;

         @Override
         protected boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });
   }

   static <T> List<T> statementToList(final PreparedStatement stmt, final Class<T> clazz, final Object... args) throws SQLException
   {
      try (final PreparedStatement closeStmt = stmt) {
         ResultSet rs = statementToResultSet(stmt, args);
         boolean next = rs.next();
         if (next) {
            return resultSetToList(rs, clazz);
         }
         else {
            return new ArrayList<T>();
         }
      }
   }

   static ResultSet statementToResultSet(final PreparedStatement stmt, final Object... args) throws SQLException
   {
      populateStatementParameters(stmt, args);
      return stmt.executeQuery();
   }


   /**
    *
    * @param resultSet ResultSet.next() must <i>NOT</i> been called before.
    */
   static <T> List<T> resultSetToList(final ResultSet resultSet, final Class<T> targetClass) throws SQLException {
      ResultSetToObjectProcessor<T> processor = new ResultSetToObjectProcessor<>(resultSet, new HashSet<>());
      return processor.process(targetClass);
   }

   private static <T> T statementToObject(final PreparedStatement stmt, final T target, final Object... args) throws SQLException
   {
      populateStatementParameters(stmt, args);

      try (final ResultSet resultSet = stmt.executeQuery()) {
         return resultSet.next() ? resultSetToObject(resultSet, target) : null;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
      finally {
         stmt.close();
      }
   }

   static <T> T statementToObject(final PreparedStatement stmt, final Class<T> clazz, final Object... args) throws SQLException {
      T target;
      try {
         target = clazz.newInstance();
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
      return statementToObject(stmt, target, args);
   }

   static <T> T resultSetToObject(final ResultSet resultSet, final T target) throws SQLException
   {
      final Set<String> ignoreNone = Collections.emptySet();
      return resultSetToObject(resultSet, target, ignoreNone);
   }

   static <T> T resultSetToObject(final ResultSet resultSet, final T target, final Set<String> ignoredColumns) throws SQLException
   {
      ResultSetToObjectProcessor<T> rsProcessor = new ResultSetToObjectProcessor<>(resultSet, ignoredColumns);
      return rsProcessor.process(target);
   }

   static <T> T objectById(final Connection connection, final Class<T> clazz, final Object... args) throws SQLException
   {
      String where = getWhereIdClause(Introspector.getIntrospected(clazz));
      return objectFromClause(connection, clazz, where, args);
   }

   static <T> T objectById(final Connection connection, final T target) throws SQLException {
      Introspected introspected = Introspector.getIntrospected(target.getClass());
      String where = getWhereIdClause(introspected);
      List<AttributeInfo> idFcInfos = introspected.getIdFcInfos();
      Object[] args = new Object[idFcInfos.size()];
      for (int i = 0; i < idFcInfos.size(); i++) {
         AttributeInfo info = idFcInfos.get(i);
         try {
            args[i] = info.getValue(target);
         }
         catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
         }
      }
      return objectFromClause(connection, target, where, args);
   }

   static <T> T refresh(final Connection connection, final T target) throws SQLException {
      final Introspected introspected = Introspector.getIntrospected(target.getClass());
      final String where = getWhereIdClause(introspected);
      final String sql = generateSelectFromClause(target.getClass(), where);
      final PreparedStatement stmt = connection.prepareStatement(sql);
      return statementToObject(stmt, target, introspected.getActualIds(target));
   }

   private static String getWhereIdClause(Introspected introspected) {
      final StringBuilder where = new StringBuilder();
      String[] idColumnNames = introspected.getIdColumnNames();
      for (String column : idColumnNames) {
         where.append(column).append("=? AND ");
      }
      // the where clause can be length of zero if we are loading an object that is presumed to
      // be the only row in the table and therefore has no id.
      if (where.length() > 0) {
         where.setLength(where.length() - 5);
      }
      return where.toString();
   }

   static <T> List<T> listFromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) throws SQLException
   {
      final String sql = generateSelectFromClause(clazz, clause);
      final PreparedStatement stmt = connection.prepareStatement(sql);

      return statementToList(stmt, clazz, args);
   }

   static <T> T objectFromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) throws SQLException
   {
      final String sql = generateSelectFromClause(clazz, clause);
      final PreparedStatement stmt = connection.prepareStatement(sql);
      return statementToObject(stmt, clazz, args);
   }

   static <T> T objectFromClause(final Connection connection, final T target, final String clause, final Object... args) throws SQLException
   {
      final String sql = generateSelectFromClause(target.getClass(), clause);
      final PreparedStatement stmt = connection.prepareStatement(sql);
      return statementToObject(stmt, target, args);
   }

   static <T> int countObjectsFromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) throws SQLException
   {
      final Introspected introspected = Introspector.getIntrospected(clazz);

      final String tableName = introspected.getDelimitedTableName();
      final String[] idColumnNames = introspected.getIdColumnNames();

      final StringBuilder sql = new StringBuilder()
        .append("SELECT COUNT(").append(tableName).append('.')
        .append(idColumnNames.length > 0 ? idColumnNames[0] : introspected.getColumnNames()[0])
        .append(")")
        .append(" FROM ").append(tableName).append(' ').append(tableName);

      if (clause != null && !clause.isEmpty()) {
         final String upper = clause.toUpperCase();
         if (!upper.contains("WHERE") && !upper.contains("JOIN") && !upper.startsWith("ORDER")) {
            sql.append(" WHERE ");
         }
         sql.append(' ').append(clause);
      }

      return numberFromSql(connection, sql.toString(), args).intValue();
   }

   static Number numberFromSql(final Connection connection, final String sql, final Object... args) throws SQLException
   {
      try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
         populateStatementParameters(stmt, args);
         try (final ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
               return (Number) resultSet.getObject(1);
            }
            return null;
         }
      }
   }

   /**
    * package private for testing.
    */
   static <T> String generateSelectFromClause(final Class<T> clazz, final String clause)
   {
      final String cacheKey = clazz.getName() + clause;

      return fromClauseStmtCache.computeIfAbsent(cacheKey, key -> {
        final Introspected introspected = Introspector.getIntrospected(clazz);
        final String tableName = introspected.getDelimitedTableName();

        final StringBuilder sqlSB = new StringBuilder()
          .append("SELECT ").append(getColumnsCsv(clazz, tableName))
          .append(" FROM ").append(tableName).append(' ').append(tableName);

        if (clause != null && !clause.isEmpty()) {
           final String upper = clause.toUpperCase();
           if (!upper.contains("WHERE") && !upper.contains("JOIN")) {
              sqlSB.append(" WHERE ");
           }
           sqlSB.append(' ').append(clause);
        }

        return sqlSB.toString();
      });
   }

   private static class ResultSetToObjectProcessor<T> {
      private final ResultSet resultSet;
      /** the provided target ({@link #process(Object)}) for a single row result or the target for the currently processed row in a multiple row result. */
      private T target;
      private final Set<String> ignoredColumns;
      private ResultSetMetaData metaData;
      private Introspected introspected;
      /** scope is the currently processed row. */
      private HashMap<String, Object> tableNameToEntitiesInCurrentRow;
      private HashMap<String, Object> tableNameToEntities;
      private int currentRow = 0;
      /** scope is the currently processed column if its a join column. */
      private Object currentEntity;
      /** scope is the currently processed join column if its type is an entity that is first seen in the current row and must be therefore created. */
      private AtomicBoolean isNewEntity;
      /** scope is the currently processed column if its a join column. */
      private AttributeInfo parentInfo;
      private Object currentParent;
      /** the per row created targets in a multi row result */
      private List<T> targets;
      private int colIdx;

      /**
       *
       * @param resultSet With next() already been called on. To be compatible with Spring JDBC.
       */
      public ResultSetToObjectProcessor(final ResultSet resultSet, final Set<String> ignoredColumns) {
         this.resultSet = resultSet;
         this.ignoredColumns = ignoredColumns;
      }

      T forTestOnly(final T target) throws SQLException {
         this.target = target;

         metaData = resultSet.getMetaData();
         introspected = Introspector.getIntrospected(target.getClass());
         tableNameToEntitiesInCurrentRow = new HashMap<>();
         tableNameToEntitiesInCurrentRow.putIfAbsent(introspected.getTableName().toUpperCase(), target);
         tableNameToEntities = new HashMap<>();

         // do ... while collides with usage within org.springframework.jdbc.core.RowMapper
//         do {
            if (currentRow > 0) {
//                  tableNameToEntitiesInCurrentRow.keySet().removeIf(tableName -> !tableName.equalsIgnoreCase(introspected.getTableName()));

               tableNameToEntities = tableNameToEntitiesInCurrentRow;
               tableNameToEntitiesInCurrentRow = new HashMap<>();
            }
            for (colIdx = metaData.getColumnCount(); colIdx > 0; colIdx--) {
               processColumn(colIdx);
            }

//               if (currentRow > 0) {
//                  for (Map.Entry<Object, AttributeInfo> entry : entityTojoinField.entrySet()) {
//                     AttributeInfo info = entry.getValue();
//                     Object trgt = tableNameToEntitiesInCurrentRow.remove(info.getOwnerClassTableName().toUpperCase());
//                     Object value = introspected.get(trgt, info);
//                     Object entity = entry.getKey();
//                     if (info.getType() == Collection.class) {
//                        ((Collection) value).add(entity);
//                     }
//                     else {
//                        introspected.set(trgt, info, entity);
//                     }
//                  }
////                  tableNameToEntitiesInCurrentRow = new HashMap<>();
//                  entityTojoinField = new HashMap<>();
//               }

            currentRow++;
//         } while (resultSet.next());

         return currentRow > 0 ? target : null;
      }

      T process(final T target) throws SQLException {
         this.target = target;
         metaData = resultSet.getMetaData();
         introspected = Introspector.getIntrospected(target.getClass());
         tableNameToEntitiesInCurrentRow = new HashMap<>();
         tableNameToEntitiesInCurrentRow.putIfAbsent(introspected.getTableName().toUpperCase(), target);
         tableNameToEntities = new HashMap<>();

         for (colIdx = metaData.getColumnCount(); colIdx > 0; colIdx--) {
            processColumn(colIdx);
         }

         return target;
      }

      List<T> process(final Class<T> targetClass) throws SQLException {

         metaData = resultSet.getMetaData();
         introspected = Introspector.getIntrospected(targetClass);
         targets = new ArrayList<>();

         do {
            try {
               target = targetClass.newInstance();
               tableNameToEntitiesInCurrentRow = new HashMap<>();
               tableNameToEntitiesInCurrentRow.put(introspected.getTableName().toUpperCase(), target);
            }
            catch (InstantiationException | IllegalAccessException e) {
               throw new RuntimeException(e);
            }

            for (colIdx = metaData.getColumnCount(); colIdx > 0; colIdx--) {
               processColumn(colIdx);
            }

            targets.add(target);
            currentRow++;

         } while (resultSet.next());

         return targets;
      }

      private void processColumn(final int colIdx) throws SQLException {
         final String columnName = metaData.getColumnName(colIdx);
         // To make names in ignoredColumns independend from database case sensitivity. Otherwise you have to write database dependent code.
         if (isIgnoredColumn(ignoredColumns, columnName)) {
            return;
         }

         final Object columnValue = resultSet.getObject(colIdx);
         String tableName = Optional.ofNullable(metaData.getTableName(colIdx)).orElse("");
         // tableName is empty when aliases as in "SELECT (t.string_from_number + 1) as string_from_number " were used. See org.sansorm.QueryTest.testConverterLoad().
         if (tableName.isEmpty() || tableName.equalsIgnoreCase(introspected.getTableName())) {
            final AttributeInfo fcInfo = !tableName.isEmpty()
               ? introspected.getFieldColumnInfo(tableName, columnName)
               : introspected.getFieldColumnInfo(columnName);
            Object parent = tableNameToEntitiesInCurrentRow.computeIfAbsent(introspected.getTableName().toUpperCase(), tbl -> {
               try {
                  return introspected.getTableTarget(introspected.getTableName());
               }
               catch (IllegalAccessException | InstantiationException e) {
                  throw new RuntimeException(e);
               }
            });
            // If objectFromSelect() does more fields retrieve as are defined on the entity then fcInfo is null.
            if (fcInfo != null
               && (!fcInfo.isIdField || !fcInfo.getType().isPrimitive() || columnValue != null)) {
               // Do not call fcInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
               introspected.set(parent, fcInfo, columnValue, metaData.getColumnTypeName(colIdx));
            }
         }
         else {
            processColumnOfJoinedTable(columnName, columnValue, tableName);
         }
      }

      /**
       * Called for every table of a joined table.
       */
      private void processColumnOfJoinedTable(
               final String columnName,
               final Object columnValue,
               final String tableName) throws SQLException {

         isNewEntity = new AtomicBoolean(false);
         currentEntity = tableNameToEntitiesInCurrentRow.computeIfAbsent(tableName.toUpperCase(), tableNameUpperCased -> {
            try {
               isNewEntity.set(true);
               return introspected.getTableTarget(tableNameUpperCased);
            }
            catch (IllegalAccessException | InstantiationException e) {
               throw new RuntimeException(e);
            }
         });
         // currentEntity is null if target does not correspond with an actual table. See com.zaxxer.q2o.internal.JoinOneToOneSeveralTablesTest.flattenedTableJoin().
         currentEntity = currentEntity == null ? target : currentEntity;

         Class<?> currentTargetClass = currentEntity.getClass();
         AttributeInfo currentTargetInfo = Introspector.getIntrospected(currentTargetClass).getFieldColumnInfo(columnName);
         // Do not call currentTargetInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
         if (currentTargetInfo != null && (
               !currentTargetInfo.isIdField
                  || !currentTargetInfo.getType().isPrimitive()
                  || columnValue != null)
         ) {
            if (!(currentTargetInfo.getType().isPrimitive() && columnValue == null)) {
               introspected.set(currentEntity, currentTargetInfo, columnValue, metaData.getColumnTypeName(colIdx));
            }
            // parentInfo is null if target does not correspond with an actual table. See com.zaxxer.q2o.internal.JoinOneToOneSeveralTablesTest.flattenedTableJoin().
            parentInfo = introspected.getFieldColumnInfo(currentTargetClass);
            if (parentInfo != null) {
               currentParent = tableNameToEntitiesInCurrentRow.computeIfAbsent(parentInfo.getOwnerClassTableName().toUpperCase(), tbln -> {
                  try {
                     return parentInfo.getOwnerClazz().newInstance();
                  }
                  catch (InstantiationException | IllegalAccessException e) {
                     throw new RuntimeException(e);
                  }
               });
               // Do not call currentTargetInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
               if (!parentInfo.isOneToManyAnnotated) {
                  introspected.set(currentParent, parentInfo, currentEntity, metaData.getColumnTypeName(colIdx));
               }
               else if (parentInfo.getType() == Collection.class) {
                  setManyToOneField();
               }
            }
         }
      }

      private void setManyToOneField() {
         try {
            Object value = parentInfo.getValue(currentParent);
            if (value == null) {
               Collection collection = new ArrayList();
               collection.add(currentEntity);
               introspected.set(currentParent, parentInfo, collection, metaData.getColumnTypeName(colIdx));

               String parentTableName = parentInfo.getOwnerClassTableName().toUpperCase();
               Object parentEntity = tableNameToEntities.get(parentTableName);
               if (parentEntity != null) {
                  Collection c = (Collection) introspected.get(parentEntity, parentInfo);
                  c.add(currentEntity);
               }
            }
         }
         catch (IllegalAccessException | InvocationTargetException | SQLException e) {
            throw new RuntimeException(e);
         }
      }
   }

}
