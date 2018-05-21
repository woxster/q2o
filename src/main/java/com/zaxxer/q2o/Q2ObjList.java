package com.zaxxer.q2o;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 21.05.18
 */
public class Q2ObjList {

   /**
    * Load a list of objects using the specified where condition.  The clause "WHERE" is automatically
    * appended, so the {@code where} parameter should just be the conditional portion.
    *
    * If the {@code where} parameter is {@code null} a select of every object from the
    * table mapped for the specified class is executed.
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param clause the conditional part of a SQL where clause
    * @param args the query parameters used to find the list of objects
    * @param <T> the type of the object to load
    * @return a list of populated objects
    * @throws SQLException if a {@link SQLException} occurs
    * @see Q2Obj#fromClause(Connection, Class, String, Object...)
    */
   public static <T> List<T> fromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.listFromClause(connection, clazz, clause, args);
   }

   /**
    * Execute a prepared statement (query) with the supplied args set as query parameters (if specified), and
    * return a list of objects as a result. <b>The PreparedStatement will be closed.</b>
    *
    * @param stmt the PreparedStatement to execute
    * @param clazz the class of the objects to instantiate and populate with state
    * @param args optional arguments to set as query parameters in the PreparedStatement
    * @param <T> the class template
    * @return a list of instance of the target class, or an empty list
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> List<T> fromStatement(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.statementToList(stmt, clazz, args);
   }

   /**
    * This method will iterate over a ResultSet that contains columns that map to the
    * target class and return a list of target instances.  <b>Note, this assumes that
    * ResultSet.next() has <i>NOT</i> been called before calling this method.</b>
    * <p>
    * <b>The entire ResultSet will be consumed and closed.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param targetClass the target class
    * @param <T> the class template
    * @return a list of instance of the target class, or an empty list
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> List<T> fromResultSet(ResultSet resultSet, Class<T> targetClass) throws SQLException
   {
      return OrmReader.resultSetToList(resultSet, targetClass);
   }

   /**
    * Gets a list of objects from the database.
    * @param clazz The type of the desired objects.
    * @param clause The from or where clause.
    * @param args The arguments needed for the clause.
    * @param <T> The type of the objects.
    * @return The list of objects.
    * @see Q2Obj#fromClause(Connection, Class, String, Object...)
    */
   public static <T> List<T> fromClause(Class<T> clazz, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> OrmReader.listFromClause(c, clazz, clause, args));
   }

   public static <T> List<T> fromSelect(Class<T> clazz, String select, Object... args) {
      return SqlClosure.sqlExecute(connection -> {
         PreparedStatement stmnt = connection.prepareStatement(select);
         return fromStatement(stmnt, clazz, args);
      });
   }

   public static <T> List<T> fromSelect(Connection connection, Class<T> clazz, String select, Object... args) throws SQLException {
      PreparedStatement stmnt = connection.prepareStatement(select);
      return fromStatement(stmnt, clazz, args);
   }
}
