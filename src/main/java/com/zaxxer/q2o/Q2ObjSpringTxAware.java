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
      try {
         return super.byId(connection, clazz, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("byId", null, e);
      }
   }

   @Override
   <T> T byId(final Connection connection, final T target) {
      try {
         return super.byId(connection, target);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("byId", null, e);
      }
   }

   @Override
   <T> T fromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) {
      try {
         return super.fromClause(connection, clazz, clause, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("fromClause", clause, e);
      }
   }

   @Override
   <T> int countFromClause(final Connection connection, final Class<T> clazz, final String clause, final Object... args) {
      try {
         return super.countFromClause(connection, clazz, clause, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("countFromClause", clause, e);
      }
   }

   @Override
   <T> T fromStatement(final PreparedStatement stmt, final Class<T> clazz, final Object... args) {
      try {
         return super.fromStatement(stmt, clazz, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("fromStatement", null, e);
      }
   }

   @Override
   <T> T fromResultSet(final ResultSet resultSet, final T target) {
      try {
         return super.fromResultSet(resultSet, target);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("fromResultSet", null, e);
      }
   }

   @Override
   <T> T fromResultSet(final ResultSet resultSet, final T target, final Set<String> ignoredColumns) {
      try {
         return super.fromResultSet(resultSet, target, ignoredColumns);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("fromResultSet", null, e);
      }
   }

   @Override
   <T> T insert(final Connection connection, final T object) {
      try {
         return super.insert(connection, object);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("insert", null, e);
      }
   }

   @Override
   <T> T update(final Connection connection, final T object) {
      try {
         return super.update(connection, object);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("update", null, e);
      }
   }

   @Override
   <T> T updateExcludeColumns(final Connection connection, final T object, final String... excludedColumns) {
      try {
         return super.updateExcludeColumns(connection, object, excludedColumns);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("updateExcludeColumns", null, e);
      }
   }

   @Override
   <T> T updateExcludeColumns(final T object, final String... excludedColumns) {
      HashSet<String> excludedCols = new HashSet<>(excludedColumns.length);
      excludedCols.addAll(Arrays.asList(excludedColumns));
      return SqlClosureSpringTxAware.sqlExecute(connection -> OrmWriter.updateObject(connection, object, excludedCols));
   }

   @Override
   <T> T updateIncludeColumns(final Connection connection, final T object, final String... includedColumns) {
      try {
         return super.updateIncludeColumns(connection, object, includedColumns);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("updateIncludeColumns", null, e);
      }
   }

   @Override
   <T> T updateIncludeColumns(final T object, final String... includedColumns) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> updateIncludeColumns(connection, object, includedColumns));
   }

   @Override
   <T> int delete(final Connection connection, final T target) {
      try {
         return super.delete(connection, target);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("delete", null, e);
      }
   }

   @Override
   <T> int deleteById(final Connection connection, final Class<T> clazz, final Object... args) {
      try {
         return super.deleteById(connection, clazz, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("deleteById", null, e);
      }
   }

   @Override
   <T> T refresh(final Connection connection, final T target) {
      try {
         return super.refresh(connection, target);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("refresh", null, e);
      }
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
      try {
         return super.fromSelect(connection, clazz, select, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("fromSelect", null, e);
      }
   }

   @Override
   int deleteByWhereClause(final Class<?> clazz, final String whereClause, final Object... args) {
      return SqlClosureSpringTxAware.sqlExecute(connection -> {
         return OrmWriter.deleteByWhereClause(connection, clazz, whereClause, args);
      });
   }

   @Override
   int deleteByWhereClause(final Connection connection, final Class<?> clazz, final String whereClause, final Object... args) {
      try {
         return super.deleteByWhereClause(connection, clazz, whereClause, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("deleteByWhereClause", null, e);
      }
   }
}
