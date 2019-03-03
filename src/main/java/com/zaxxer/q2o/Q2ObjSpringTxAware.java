package com.zaxxer.q2o;

import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-02-28
 */
class Q2ObjSpringTxAware extends Q2Object {

   private final SQLErrorCodeSQLExceptionTranslator exceptionTranslator;
   private DataSource dataSource;
   private static DataSource defaultDataSource;
   private static SQLErrorCodeSQLExceptionTranslator defaultExceptionTranslator;

   /**
    * For usage of the default constructor you must have initialized q2o with {@link q2o#initializeWithSpringTxSupport(DataSource)}.
    */
   private Q2ObjSpringTxAware() {
      dataSource = defaultDataSource;
      exceptionTranslator = defaultExceptionTranslator;
   }

   Q2ObjSpringTxAware(final DataSource dataSource) {
      this.dataSource = dataSource;
      exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
   }

   static void setDefaultDataSource(DataSource dataSource) {
      defaultDataSource = dataSource;
      defaultExceptionTranslator =
         dataSource != null ? new SQLErrorCodeSQLExceptionTranslator(dataSource)
                            : null;
   }

   @Override
   <T> T byId(final Connection connection, final Class<T> clazz, final Object... args) {
      return execute(() -> super.byId(connection, clazz, args));
   }

   @Override
   <T> T byId(final Connection connection, final T target) {
      return execute(() -> super.byId(connection, target));
   }

   @Override
   <T> T fromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) {
      return execute(() -> super.fromClause(connection, clazz, clause, args));
   }

   @Override
   <T> int countFromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) {
      return execute(() -> super.countFromClause(connection, clazz, clause, args));
   }

   @Override
   <T> T fromStatement(final PreparedStatement stmt, final Class<T> clazz, final Object... args) {
      return execute(() -> super.fromStatement(stmt, clazz, args));
   }

   @Override
   <T> T fromResultSet(final ResultSet resultSet, final T target) {
      return execute(() -> super.fromResultSet(resultSet, target));
   }

   private <T> T execute(Query<T> query) {
      try {
         return query.execute();
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("", null, e);
      }
   }

   @Override
   <T> T fromResultSet(final ResultSet resultSet, final T target, final Set<String> ignoredColumns) {
      return execute(() -> super.fromResultSet(resultSet, target, ignoredColumns));
   }

   @Override
   <T> T insert(final Connection connection, final T object) {
      return execute(() -> super.insert(connection, object));
   }

   @Override
   <T> T update(final Connection connection, final T object) {
      return execute(() -> super.update(connection, object));
   }

   @Override
   <T> T updateExcludeColumns(final Connection connection, final T object, final String... excludedColumns) {
      return execute(() -> super.updateExcludeColumns(connection, object, excludedColumns));
   }

   @Override
   <T> T updateExcludeColumns(final T object, final String... excludedColumns) {
      HashSet<String> excludedCols = new HashSet<>(excludedColumns.length);
      excludedCols.addAll(Arrays.asList(excludedColumns));
      return SqlClosureSpringTxAware.sqlExecute(connection -> OrmWriter.updateObject(connection, object, excludedCols));
   }

   @Override
   <T> T updateIncludeColumns(final Connection connection, final T object, final String... includedColumns) {
      return execute(() -> super.updateIncludeColumns(connection, object, includedColumns));
   }

   @Override
   <T> T updateIncludeColumns(final T object, final String... includedColumns) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> updateIncludeColumns(connection, object, includedColumns));
   }

   @Override
   <T> int delete(final Connection connection, final T target) {
      return execute(() -> super.delete(connection, target));
   }

   @Override
   <T> int deleteById(final Connection connection, final Class<T> clazz, final Object... args) {
      return execute(() -> super.deleteById(connection, clazz, args));
   }

   @Override
   <T> T refresh(final Connection connection, final T target) {
      return execute(() -> super.refresh(connection, target));
   }

   @Override
   <T> T refresh(final T target) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> refresh(connection, target));
   }

   @Override
   <T> T byId(final Class<T> type, final Object... ids) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> byId(connection, type, ids));
   }

   @Override
   <T> T byId(final T target) {

      return SqlClosureSpringTxAware.sqlExecute(connection -> OrmReader.objectById(connection, target));
   }

   @Override
   <T> T fromClause(final Class<T> type, final String clause, final Object... args) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> fromClause(connection, type, clause, args));
   }

   @Override
   <T> T insert(final T object) {

      return SqlClosureSpringTxAware.sqlExecute(connection -> insert(connection, object));
   }

   @Override
   <T> T update(final T object) {

      return SqlClosureSpringTxAware.sqlExecute(connection -> update(connection, object));
   }

   @Override
   <T> int delete(final T object) {

      return SqlClosureSpringTxAware.sqlExecute(connection ->  delete(connection, object));
   }

   @Override
   <T> int deleteById(final Class<T> clazz, final Object... args) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> deleteById(connection, clazz, args));
   }

   @Override
   <T> int countFromClause(final Class<T> clazz, final String clause, final Object... args) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> countFromClause(connection, clazz, clause, args));
   }

   @Override
   <T> T fromSelect(final Class<T> clazz, final String select, final Object... args) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> {
         PreparedStatement stmnt = connection.prepareStatement(select);
         return fromStatement(stmnt, clazz, args);
      });
   }

   @Override
   <T> T fromSelect(final Connection connection, final Class<T> clazz, final String select, final Object... args) {
      return execute(() -> super.fromSelect(connection, clazz, select, args));
   }

   @Override
   int deleteByWhereClause(final Class<?> clazz, final String whereClause, final Object... args) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> OrmWriter.deleteByWhereClause(connection, clazz, whereClause, args));
   }

   @Override
   int deleteByWhereClause(final Connection connection, final Class<?> clazz, final String whereClause, final Object... args) {
      return execute(() -> super.deleteByWhereClause(connection, clazz, whereClause, args));
   }

}
