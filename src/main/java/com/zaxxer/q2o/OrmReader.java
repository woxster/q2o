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

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

/**
 * OrmReader
 */
// MULTIPLESTRINGS:OFF
class OrmReader extends OrmBase {
   private static final int CACHE_SIZE = Integer.getInteger("com.zaxxer.sansorm.statementCacheSize", 500);
   private static Logger LOGGER = LoggerFactory.getLogger(OrmBase.class);

   private static final Map<String, String> fromClauseStmtCache;

   static {
      fromClauseStmtCache = Collections.synchronizedMap(
         new LinkedHashMap<String, String>(CACHE_SIZE) {
            private static final long serialVersionUID = 6259942586093454872L;

            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<String, String> eldest) {
               return this.size() > CACHE_SIZE;
            }
      });
   }

   static void clearCache()
   {
      fromClauseStmtCache.clear();
   }

   static <T> List<T> statementToList(final PreparedStatement stmt, final Class<T> clazz, final Object... args) throws SQLException {
      LOGGER.debug("{}", stmt);
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
      LOGGER.debug("{}", stmt);
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
      String where = getWhereIdClause(Introspected.getInstance(clazz));
      return objectFromClause(connection, clazz, where, args);
   }

   static <T> T objectById(final Connection connection, final T target) throws SQLException {
      Introspected introspected = Introspected.getInstance(target.getClass());
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
      final Introspected introspected = Introspected.getInstance(target.getClass());
      final String where = getWhereIdClause(introspected);
      final String sql = generateSelectFromWhereClause(target.getClass(), where, true);
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
      final String sql = generateSelectFromWhereClause(clazz, clause, true);
      // TODO SQL hier loggen
      final PreparedStatement stmt = connection.prepareStatement(sql);

      return statementToList(stmt, clazz, args);
   }

   static <T> List<T> listFromRawClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) throws SQLException
   {
      final String sql = generateSelectFromWhereClause(clazz, clause, false);
      final PreparedStatement stmt = connection.prepareStatement(sql);

      return statementToList(stmt, clazz, args);
   }

   static <T> T objectFromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) throws SQLException
   {
      final String sql = generateSelectFromWhereClause(clazz, clause, true);
      final PreparedStatement stmt = connection.prepareStatement(sql);
      return statementToObject(stmt, clazz, args);
   }

   static <T> T objectFromRawClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) throws SQLException
   {
      final String sql = generateSelectFromWhereClause(clazz, clause, false);
      final PreparedStatement stmt = connection.prepareStatement(sql);
      return statementToObject(stmt, clazz, args);
   }

   static <T> T objectFromClause(final Connection connection, final T target, final String clause, final Object... args) throws SQLException {
      final String sql = generateSelectFromWhereClause(target.getClass(), clause, true);
      final PreparedStatement stmt = connection.prepareStatement(sql);
      return statementToObject(stmt, target, args);
   }

   static <T> int countObjectsFromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) throws SQLException
   {
      final Introspected introspected = Introspected.getInstance(clazz);

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

   static <T> List<T> numbersOrStringsFromSql(final Connection connection, Class<T> requiredType, final String sql, final Object... args) throws SQLException
   {
      try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
         populateStatementParameters(stmt, args);
         List<T> numbers = new ArrayList<>();
         DatabaseValueToFieldType databaseValueToFieldType = new DatabaseValueToFieldType();
         try (final ResultSet resultSet = stmt.executeQuery()) {
            if (resultSet.next()) {
               Object value = resultSet.getObject(1);
               Function<Object, T> converter = getConverter(requiredType, databaseValueToFieldType, value);
               if (converter != null) {
                  numbers.add(converter.apply(value));
                  while (resultSet.next()) {
                     value = resultSet.getObject(1);
                     numbers.add(converter.apply(value));
                  }
               }
            }
         }
         return numbers;
      }
   }

   // TODO Float und Short unterstützen
   @Nullable
   private static <T> Function<Object, T> getConverter(final Class<T> requiredType, final DatabaseValueToFieldType databaseValueToFieldType, final Object value)
   {
      Function<Object, T> converter = null;
      if (value instanceof Integer) {
         converter = o -> {
            //noinspection unchecked
            return (T) databaseValueToFieldType.convertInteger(requiredType, o);
         };
      }
      else if (value instanceof Long) {
         converter = o -> {
            //noinspection unchecked
            return (T) databaseValueToFieldType.convertLong(requiredType, o);
         };
      }
      else if (value instanceof Double) {
         converter = o -> {
            //noinspection unchecked
            return (T) databaseValueToFieldType.convertDouble(requiredType, o);
         };
      }
      else if (value instanceof BigInteger) {
         converter = o -> {
            //noinspection unchecked
            return (T) databaseValueToFieldType.convertBigInteger(requiredType, o);
         };
      }
      else if (value instanceof BigDecimal) {
         converter = o -> {
            //noinspection unchecked
            return (T) databaseValueToFieldType.convertBigDecimal(requiredType, o);
         };
      }
      else if (value instanceof String) {
         converter = o -> {
            //noinspection unchecked
            return (T) o;
         };
      }
      return converter;
   }

   static <T> String generateSelectFromWhereClause(final Class<T> clazz, final String clause, final boolean addLackingWhere)
   {
      final String cacheKey = clazz.getName() + clause;

      return fromClauseStmtCache.computeIfAbsent(cacheKey, key -> {
        final Introspected introspected = Introspected.getInstance(clazz);
        final String tableName = introspected.getDelimitedTableName();

        final StringBuilder s = new StringBuilder()
          .append("SELECT ").append(getColumnsCsv(clazz, tableName))
          .append(" FROM ").append(tableName).append(' ').append(tableName);

         generateWhereClause(clause, s, addLackingWhere);

         return s.toString();
      });
   }

   private static void generateWhereClause(final String selectFromClause, final StringBuilder s, final boolean addLackingWhere) {
      if (selectFromClause != null && !selectFromClause.isEmpty()) {
         final String upper = selectFromClause.toUpperCase();
         if (addLackingWhere && !upper.contains("WHERE") && !upper.contains("JOIN")) {
            s.append(" WHERE ");
         }
         s.append(' ').append(selectFromClause);
      }
   }

}
