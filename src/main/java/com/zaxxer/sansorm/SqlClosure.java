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

package com.zaxxer.sansorm;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The {@code SqlClosure} class provides a convenient way to execute SQL
 * with proper transaction demarcation and resource clean-up.
 *
 * @param <T> the templated return type of the closure
 * @deprecated
 */
public class SqlClosure<T>
{
   private com.zaxxer.q2o.SqlClosure q2oSqlClosure;

   /**
    * Default constructor using the default DataSource.  The {@code execute(Connection connection)}
    * method will be called when the closure executed.  A RuntimeException is thrown if the default
    * DataSource has not been set.
    * @deprecated
    */
   public SqlClosure() {
      q2oSqlClosure = new com.zaxxer.q2o.SqlClosure();
   }

   /**
    * A constructor taking arguments to be passed to the {@code execute(Connection connection, Object...args)}
    * method when the closure is executed.  Subclasses using this method must call {@code super(args)}.
    * A RuntimeException is thrown if the default DataSource has not been set.
    *
    * @param args arguments to be passed to the execute method
    * @deprecated
    */
   public SqlClosure(final Object... args) {

      q2oSqlClosure = new com.zaxxer.q2o.SqlClosure(args);
   }

   /**
    * Construct a SqlClosure with a specific DataSource.
    *
    * @param ds the DataSource
    * @deprecated
    */
   public SqlClosure(final DataSource ds) {

      q2oSqlClosure = new com.zaxxer.q2o.SqlClosure(ds);
   }

   /**
    * Construct a SqlClosure with a specific DataSource and arguments to be passed to the
    * {@code execute} method.  @see #SqlClosure(Object...args)
    *
    * @param ds the DataSource
    * @param args optional arguments to be used for execution
    * @deprecated
    */
   public SqlClosure(final DataSource ds, final Object... args) {
      q2oSqlClosure = new com.zaxxer.q2o.SqlClosure(ds, args);
   }

   /**
    * Construct a SqlClosure with the same DataSource as the closure passed in.
    *
    * @param copyClosure the SqlClosure to share a common DataSource with
    * @deprecated
    */
   public SqlClosure(final SqlClosure copyClosure)
   {
      q2oSqlClosure = new com.zaxxer.q2o.SqlClosure(copyClosure);
   }

   /**
    * Set the default DataSource used by the SqlClosure when the default constructor
    * is used.
    * @deprecated
    * @param ds the DataSource to use by the default
    */
   static void setDefaultDataSource(final DataSource ds)
   {

      com.zaxxer.q2o.SqlClosure.setDefaultDataSource(ds);
   }

   /**
    * Execute a lambda {@code SqlFunction} closure.
    *
    * @param functional the lambda function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @since 2.5
    * @deprecated
    */
   public static <V> V sqlExecute(final SqlFunction<V> functional)
   {
      return new com.zaxxer.q2o.SqlClosure<V>() {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * Execute a lambda {@code SqlVarArgsFunction} closure.
    *
    * @param functional the lambda function
    * @param args arguments to pass to the lamba function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @since 2.5
    * @deprecated
    */
   public static <V> V sqlExecute(final SqlVarArgsFunction<V> functional, final Object... args)
   {
      return new com.zaxxer.q2o.SqlClosure<V>() {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }

   /**
    * Execute a lambda {@code SqlFunction} closure using the current instance as the base (i.e. share
    * the same DataSource).
    *
    * @param functional the lambda function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @deprecated
    */
   public final <V> V exec(final SqlFunction<V> functional)
   {
      return new com.zaxxer.q2o.SqlClosure<V>(this) {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * Execute a lambda {@code SqlVarArgsFunction} closure using the current instance as the base (i.e. share
    * the same DataSource).
    *
    * @param functional the lambda function
    * @param args arguments to pass to the lamba function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @deprecated
    */
   public final <V> V exec(final SqlVarArgsFunction<V> functional, final Object... args)
   {
      return new com.zaxxer.q2o.SqlClosure<V>(this) {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }

   /**
    * Execute the closure.
    *
    * @return the template return type of the closure
    * @deprecated
    */
   public final T execute()
   {
      return (T) q2oSqlClosure.execute();
   }

   /**
    * Execute the closure with the specified arguments.  Note using this method
    * does not create a true closure because the arguments are not encapsulated
    * within the closure itself.  Meaning you cannot create an instance of the
    * closure and pass it to another executor.
    *
    * @param args arguments to be passed to the {@code execute(Connection connection, Object...args)} method
    * @return the result of the execution
    * @deprecated
    */
   public final T executeWith(Object... args)
   {
      return (T) q2oSqlClosure.executeWith(args);
   }

   /**
    * Subclasses of {@code SqlClosure} must override this method or the alternative
    * {@code execute(Connection connection, Object...args)} method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    * @deprecated
    */
   protected T execute(final Connection connection) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of this method.");
   }

   /**
    * Subclasses of {@code SqlClosure} must override this method or the alternative
    * {@code execute(Connection connection)} method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @param args the arguments passed into the {@code SqlClosure(Object...args)} constructor
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    * @deprecated
    */
   protected T execute(final Connection connection, Object... args) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of this method.");
   }

   /**
    * @param connection The database connection
    * @deprecated
    */
   public static void quietClose(final Connection connection)
   {
      if (connection != null) {
         try {
            connection.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   /**
    * @deprecated
    * @param statement The database connection
    */
   public static void quietClose(final Statement statement)
   {
      if (statement != null) {
         try {
            statement.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   /**
    * @deprecated
    * @param resultSet The database connection
    */
   public static void quietClose(final ResultSet resultSet)
   {
      if (resultSet != null) {
         try {
            resultSet.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

}
