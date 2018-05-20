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
         return resultSetToList(statementToResultSet(stmt, args), clazz);
      }
   }

   static ResultSet statementToResultSet(final PreparedStatement stmt, final Object... args) throws SQLException
   {
      populateStatementParameters(stmt, args);
      return stmt.executeQuery();
   }

   // COMPLEXITY:OFF
   static <T> List<T> resultSetToList(final ResultSet resultSet, final Class<T> targetClass) throws SQLException
   {
      final List<T> list = new ArrayList<>();
      if (!resultSet.next()) {
         resultSet.close();
         return list;
      }

      final Introspected introspected = Introspector.getIntrospected(targetClass);
      final boolean hasJoinColumns = introspected.hasSelfJoinColumn();

      final ResultSetMetaData metaData = resultSet.getMetaData();
      final int columnCount = metaData.getColumnCount();
      final String[] columnNames = new String[columnCount];
      for (int colIdx = columnCount; colIdx > 0; colIdx--) {
         columnNames[colIdx - 1] = metaData.getColumnName(colIdx).toLowerCase();
      }

      try (final ResultSet closeRS = resultSet) {
         do {
            final T target = targetClass.newInstance();
            list.add(target);
            for (int column = columnCount; column > 0; column--) {
               final Object columnValue = resultSet.getObject(column);
               if (columnValue == null) {
                  continue;
               }

               final String columnName = columnNames[column - 1];
               final AttributeInfo fcInfo = introspected.getFieldColumnInfo(columnName);
               if (fcInfo.isSelfJoinField()) {
                  fcInfo.setValue(target, columnValue);
               }
               else {
                  introspected.set(target, fcInfo, columnValue);
               }
            }
         }
         while (resultSet.next());
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }

      return list;
   }
   // COMPLEXITY:ON

   private static <T> T statementToObject(final PreparedStatement stmt, final T target, final Object... args) throws SQLException
   {
      populateStatementParameters(stmt, args);

      try (final ResultSet resultSet = stmt.executeQuery()) {
         if (resultSet.next()) {
            return resultSetToObject(resultSet, target);
         }
         return null;
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
      final ResultSetMetaData metaData = resultSet.getMetaData();

      final Introspected introspected = Introspector.getIntrospected(target.getClass());

      HashMap<String, Object> tableNameToTarget = new HashMap<>();
      tableNameToTarget.putIfAbsent(introspected.getTableName(), target);

      for (int colIdx = metaData.getColumnCount(); colIdx > 0; colIdx--) {
         final String columnName = metaData.getColumnName(colIdx);
         // To make names in ignoredColumns independend from database case sensitivity. Otherwise you have to write database dependent code.
         if (isIgnoredColumn(ignoredColumns, columnName)) {
            continue;
         }

         final Object columnValue = resultSet.getObject(colIdx);
         if (columnValue == null) {
            continue;
         }
         String tableName = metaData.getTableName(colIdx);
         // tableName is empty when aliases as in "SELECT (t.string_from_number + 1) as string_from_number " were used. See org.sansorm.QueryTest.testConverterLoad().
         if (!tableName.isEmpty() && !tableName.equalsIgnoreCase(introspected.getTableName())) {

            Object currentTarget = tableNameToTarget.computeIfAbsent(tableName, tblName -> {
               try {
                  return introspected.getTableTarget(tblName);
               }
               catch (IllegalAccessException | InstantiationException e) {
                  throw new RuntimeException(e);
               }
            });
            // currentTarget is null if target does not correspond with an actual table. See com.zaxxer.q2o.internal.JoinOneToOneSeveralTablesTest.flattenedTableJoin().
            currentTarget = currentTarget == null ? target : currentTarget;

            Class<?> currentTargetClass = currentTarget.getClass();
            AttributeInfo currentTargetInfo = Introspector.getIntrospected(currentTargetClass).getFieldColumnInfo(columnName);
            // Do not call currentTargetInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
            introspected.set(currentTarget, currentTargetInfo, columnValue);

            // parentInfo is null if target does not correspond with an actual table. See com.zaxxer.q2o.internal.JoinOneToOneSeveralTablesTest.flattenedTableJoin().
            AttributeInfo parentInfo = introspected.getFieldColumnInfo(currentTargetClass);
            if (parentInfo != null) {
               Object parent = tableNameToTarget.computeIfAbsent(parentInfo.getOwnerClassTableName(), tbln -> {
                  try {
                     return parentInfo.getOwnerClazz().newInstance();
                  }
                  catch (InstantiationException | IllegalAccessException e) {
                     throw new RuntimeException(e);
                  }
               });
               // Do not call currentTargetInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
               introspected.set(parent, parentInfo, currentTarget);
            }

         }
         else {
            final AttributeInfo fcInfo = !tableName.isEmpty()
               ? introspected.getFieldColumnInfo(tableName, columnName)
               : introspected.getFieldColumnInfo(columnName);
            Object parent = tableNameToTarget.computeIfAbsent(introspected.getTableName(), tbl -> {
               try {
                  return introspected.getTableTarget(introspected.getTableName());
               }
               catch (IllegalAccessException | InstantiationException e) {
                  throw new RuntimeException(e);
               }
            });
            // If objectFromSelect() does more fields retrieve as are defined on the entity fcInfo is null.
            if (fcInfo != null) {
               // Do not call fcInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
               introspected.set(parent, fcInfo, columnValue);
            }
         }

      }
      return target;
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
}
