package com.zaxxer.q2o;

import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-02
 */
class SqlClosureSpringTxAware<T> {

   private static DataSource defaultDataSource;
   private static SQLErrorCodeSQLExceptionTranslator defaultExceptionTranslator;
   private DataSource dataSource;
   private SQLErrorCodeSQLExceptionTranslator exceptionTranslator;
   private Object[] args;

   /**
    * Can only be used when q2o was initialized with {@link q2o#initializeWithSpringTxSupport(DataSource)}.
    */
   SqlClosureSpringTxAware() {
      dataSource = defaultDataSource;
      if (dataSource == null) {
         throw new RuntimeException("No default DataSource has been set");
      }
      exceptionTranslator = defaultExceptionTranslator;
   }

   /**
    * @see SqlClosure#SqlClosure(DataSource)
    */
   SqlClosureSpringTxAware(final DataSource dataSource) {
      this.dataSource = dataSource;
      exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
   }

   /**
    * @see SqlClosure#SqlClosure(DataSource, Object...)
    */
   SqlClosureSpringTxAware(final DataSource ds, final Object... args) {
      this.dataSource = ds;
      this.args = args;
      exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
   }

   SqlClosureSpringTxAware(final SqlClosureSpringTxAware copyClosure) {
      this.dataSource = copyClosure.dataSource;
      exceptionTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
   }

   /**
    * Can only be used when q2o was initialized with {@link q2o#initializeWithSpringTxSupport(DataSource)}.
    *
    * @see SqlClosure#SqlClosure(Object...)
    */
   SqlClosureSpringTxAware(final Object... args) {
      this.args = args;
   }

   static void setDefaultDataSource(final DataSource dataSource) {
      defaultDataSource = dataSource;
      defaultExceptionTranslator =
         dataSource != null ? new SQLErrorCodeSQLExceptionTranslator(dataSource)
                            : null;
   }

   /**
    * To make use of this method you must have initialized q2o with {@link q2o#initializeWithSpringTxSupport(DataSource)}.
    *
    * @see #execute()
    */
   static <V> V sqlExecute(final SqlFunction<V> functional) {
      return new SqlClosureSpringTxAware<V>() {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * Maps SQLExceptions to some subtype of Spring's {@link org.springframework.dao.DataAccessException}
    */
   final T execute() {
      Connection connection = null;
      try {
         connection = DataSourceUtils.getConnection(dataSource);
         return (args == null)
            ? execute(connection)
            : execute(connection, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("execute", null, e);
      }
      finally {
         if (connection != null) {
            DataSourceUtils.releaseConnection(connection, dataSource);
         }
      }
   }

   static <V> V sqlExecute(final SqlVarArgsFunction<V> functional, final Object... args) {
      return new SqlClosureSpringTxAware<V>() {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }

   /**
    * @see SqlClosure#execute(Connection)
    */
   T execute(Connection connection) throws SQLException {
      throw new AbstractMethodError("You must provide an implementation of SqlClosure#execute(Connection).");
   }

   /**
    * @see SqlClosure#execute(Connection, Object...)
    */
   T execute(final Connection connection, Object... args) throws SQLException {
      throw new AbstractMethodError("You must provide an implementation of SqlClosure#execute(Connection, Object...).");
   }

   /**
    * @see SqlClosure#executeWith(Object...)
    */
   final T executeWith(Object... args) {
      this.args = args;
      return execute();
   }

   /**
    * @see SqlClosure#exec(SqlFunction)
    */
   final <V> V exec(final SqlFunction<V> functional) {
      return new SqlClosureSpringTxAware<V>(this) {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * @see SqlClosure#exec(SqlVarArgsFunction, Object...)
    */
   final <V> V exec(final SqlVarArgsFunction<V> functional, final Object... args) {
      return new SqlClosureSpringTxAware<V>(this) {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }
}
