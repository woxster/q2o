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

import com.zaxxer.sansorm.internal.Introspector;
import com.zaxxer.sansorm.internal.OrmReader;
import com.zaxxer.sansorm.internal.OrmWriter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
* Encapsulates object, not SQL centric database operations. For SQL centric operations use {@link SqlElf}.
*/
//CHECKSTYLE:OFF
@SuppressWarnings({"WeakerAccess", "unused"})
public final class OrmElf
{
   /**
    * Private constructor.
    */
   private OrmElf() {
      // private constructor
   }

   // ------------------------------------------------------------------------
   //                               Read Methods
   // ------------------------------------------------------------------------

   /**
    * Load an object by it's ID.  The @Id annotated field(s) of the object is used to
    * set query parameters.
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param args the query parameter used to find the object by it's ID
    * @param <T> the type of the object to load
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T objectById(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.objectById(connection, clazz, args);
   }

   /**
    * Load an object using the specified clause.  If the specified clause contains the text
    * "WHERE" or "JOIN", the clause is appended directly to the generated "SELECT .. FROM" SQL.
    * However, if the clause contains neither "WHERE" nor "JOIN", it is assumed to be
    * just be the conditional portion that would normally appear after the "WHERE", and therefore
    * the clause "WHERE" is automatically appended to the generated "SELECT .. FROM" SQL, followed
    * by the specified clause.  For example:<p>
    * {@code User user = OrmElf.objectFromClause(connection, User.class, "username=?", userName);}
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param clause the conditional part of a SQL where clause
    * @param args the query parameters used to find the object
    * @param <T> the type of the object to load
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T objectFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.objectFromClause(connection, clazz, clause, args);
   }

   /**
    * @see #objectsFromClause(Connection, Class, String, Object...)
    * @deprecated
    */
   public static <T> List<T> listFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.listFromClause(connection, clazz, clause, args);
   }

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
    */
   public static <T> List<T> objectsFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.listFromClause(connection, clazz, clause, args);
   }

   /**
    * Counts the number of rows for the given query.
    *
    * @param connection a SQL connection object.
    * @param clazz the class of the object to query.
    * @param clause The conditional part of a SQL where clause.
    * @param args The query parameters used to find the list of objects.
    * @param <T> the type of object to query.
    * @return The result count.
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> int countObjectsFromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.countObjectsFromClause(connection, clazz, clause, args);
   }

   /**
    * This method takes a PreparedStatement, a target class, and optional arguments to set
    * as query parameters. It sets the parameters automatically, executes the query, and
    * constructs and populates an instance of the target class. <b>The PreparedStatement will be closed.</b>
    *
    * @param stmt the PreparedStatement to execute to construct an object
    * @param clazz the class of the object to instantiate and populate with state
    * @param args optional arguments to set as query parameters in the PreparedStatement
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T statementToObject(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.statementToObject(stmt, clazz, args);
   }

   /**
    * @see #statementToObjects(PreparedStatement, Class, Object...)
    * @deprecated
    */
   public static <T> List<T> statementToList(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      return statementToObjects(stmt, clazz, args);
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
   public static <T> List<T> statementToObjects(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmReader.statementToList(stmt, clazz, args);
   }

   /**
    * Get an object from the specified ResultSet.  ResultSet.next() is <i>NOT</i> called,
    * this should be done by the caller.  <b>The ResultSet is not closed as a result of this
    * method.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param target the target object to set values on
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T resultSetToObject(ResultSet resultSet, T target) throws SQLException
   {
      return OrmReader.resultSetToObject(resultSet, target);
   }

   /**
    * Get an object from the specified ResultSet.  ResultSet.next() is <i>NOT</i> called,
    * this should be done by the caller.  <b>The ResultSet is not closed as a result of this
    * method.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param target the target object to set values on
    * @param ignoredColumns the columns in the result set to ignore. Case as in name element or property name.
    * @param <T> the class template
    * @return the populated object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T resultSetToObject(ResultSet resultSet, T target, Set<String> ignoredColumns) throws SQLException
   {
      return OrmReader.resultSetToObject(resultSet, target, ignoredColumns);
   }

   /**
    * @see #resultSetToObjects(ResultSet, Class)
    * @deprecated
    */
   public static <T> List<T> resultSetToList(ResultSet resultSet, Class<T> targetClass) throws SQLException
   {
      return resultSetToObjects(resultSet, targetClass);
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
   public static <T> List<T> resultSetToObjects(ResultSet resultSet, Class<T> targetClass) throws SQLException
   {
      return OrmReader.resultSetToList(resultSet, targetClass);
   }

   // ------------------------------------------------------------------------
   //                               Write Methods
   // ------------------------------------------------------------------------

   /**
    * @see #insertObjectsNotBatched(Connection, Iterable)
    * @deprecated
    */
   public static <T> void insertListNotBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      OrmWriter.insertListNotBatched(connection, iterable);
   }

   /**
    * Insert a collection of objects in a non-batched manner (i.e. using iteration and individual INSERTs).
    *
    * @param connection a SQL connection
    * @param iterable a list (or other {@link Iterable} collection) of annotated objects to insert
    * @param <T> the class template
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> void insertObjectsNotBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      OrmWriter.insertListNotBatched(connection, iterable);
   }

   /**
    * @see #insertObjectsBatched(Connection, Iterable)
    * @deprecated
    */
   public static <T> void insertListBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      OrmWriter.insertListBatched(connection, iterable);
   }

   /**
    * Insert a collection of objects using JDBC batching.
    *
    * @param connection a SQL connection
    * @param iterable a list (or other {@link Iterable} collection) of annotated objects to insert
    * @param <T> the class template
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> void insertObjectsBatched(Connection connection, Iterable<T> iterable) throws SQLException
   {
      OrmWriter.insertListBatched(connection, iterable);
   }

   /**
    * Insert an annotated object into the database.
    *
    * @param connection a SQL connection
    * @param target the annotated object to insert
    * @param <T> the class template
    * @return the same object that was passed in, but with possibly updated @Id field due to auto-generated keys
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T insertObject(Connection connection, T target) throws SQLException
   {
      return OrmWriter.insertObject(connection, target);
   }

   /**
    * Update a database row using the specified annotated object, the @Id field(s) is used in the WHERE
    * clause of the generated UPDATE statement.
    *
    * @param connection a SQL connection
    * @param target the annotated object to use to update a row in the database
    * @param <T> the class template
    * @return the same object passed in
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> T updateObject(Connection connection, T target) throws SQLException
   {
      return OrmWriter.updateObject(connection, target);
   }

   public static <T> T updateObject(Connection connection, T target, String... excludedColumns) throws SQLException
   {
      HashSet<String> excludedCols = new HashSet<>(excludedColumns.length);
      excludedCols.addAll(Arrays.asList(excludedColumns));
      return OrmWriter.updateObject(connection, target, excludedCols);
   }

   /**
    * Delete a database row using the specified annotated object, the @Id field(s) is used in the WHERE
    * clause of the generated DELETE statement.
    *
    * @param connection a SQL connection
    * @param target the annotated object to use to delete a row in the database
    * @param <T> the class template
    * @return 0 if no row was deleted, 1 if the row was deleted
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> int deleteObject(Connection connection, T target) throws SQLException
   {
      return OrmWriter.deleteObject(connection, target);
   }

   /**
    * @see OrmWriter#deleteObjectById(Connection, Class, Object...)
    */
   public static <T> int deleteObjectById(Connection connection, Class<T> clazz, Object... args) throws SQLException
   {
      return OrmWriter.deleteObjectById(connection, clazz, args);
   }

   // ------------------------------------------------------------------------
   //                             Utility Methods
   // ------------------------------------------------------------------------

   /**
    * @deprecated Use {@link SqlElf#getColumnFromProperty(Class, String)}
    */
   public static String getColumnFromProperty(Class<?> clazz, String propertyName)
   {
      return Introspector.getIntrospected(clazz).getColumnNameForProperty(propertyName);
   }

   /**
    * @deprecated Use {@link SqlElf#getColumnsCsvExclude(Class, String...)}
    */
   public static <T> String getColumnsCsv(Class<T> clazz, String... tablePrefix)
   {
      return OrmReader.getColumnsCsv(clazz, tablePrefix);
   }

   /**
    * @deprecated Use {@link SqlElf#getColumnsCsv(Class, String...)}
    */
   public static <T> String getColumnsCsvExclude(Class<T> clazz, String... excludeColumns)
   {
      return OrmReader.getColumnsCsvExclude(clazz, excludeColumns);
   }

   /**
    * To refresh all fields in case they have changed in database.
    *
    * @param connection a SQL connection
    * @param target an annotated object with at least all @Id fields set.
    * @return the target object with all values updated or null if the object was not found anymore.
    * @throws SQLException if a {@link SQLException} occurs
    * @param <T> the type of the target object
    */
   public static <T> T refresh(Connection connection, T target) throws SQLException {
      return OrmReader.refresh(connection, target);
   }

   public static <T> T refresh(T target) throws SQLException {
      return SqlClosure.sqlExecute(c -> OrmElf.refresh(c, target));
   }

   /**
    * @see #objectById(Class, Object...)
    * @deprecated
    */
   public static <T> T getObjectById(Class<T> type, Object... ids)
   {
      return SqlClosure.sqlExecute(c -> OrmElf.objectById(c, type, ids));
   }

   /**
    * Gets an object by ID from the database.
    * @param type The type of the desired object.
    * @param ids The ID or IDs of the object.
    * @param <T> The type of the object.
    * @return The object or {@code null}
    */
   public static <T> T objectById(Class<T> type, Object... ids)
   {
      return SqlClosure.sqlExecute(c -> OrmElf.objectById(c, type, ids));
   }

   /**
    * Gets an object using a from clause.
    * @param type The type of the desired object.
    * @param clause The WHERE clause.
    * @param args The arguments for the WHERE clause.
    * @param <T> The type of the object.
    * @return The object or {@code null}
    */
   public static <T> T objectFromClause(Class<T> type, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> OrmElf.objectFromClause(c, type, clause, args));
   }

   /**
    * Inserts the given object into the database.
    * @param object The object to insert.
    * @param <T> The type of the object.
    * @return The inserted object populated with any generated IDs.
    */
   public static <T> T insertObject(T object)
   {
      return SqlClosure.sqlExecute(c -> OrmElf.insertObject(c, object));
   }

   /**
    * Updates the given object in the database.
    * @param object The object to update.
    * @param <T> The type of the object.
    * @return The updated object.
    */
   public static <T> T updateObject(T object)
   {
      return SqlClosure.sqlExecute(c -> OrmElf.updateObject(c, object));
   }

   /**
    * Delete the given object in the database.
    * @param object the object to delete.
    * @param <T> The type of the object.
    * @return the number of rows affected.
    */
   public static <T> int deleteObject(T object)
   {
      return SqlClosure.sqlExecute(c ->  OrmElf.deleteObject(c, object));
   }

   /**
    * Delete an object from the database by ID.
    * @param clazz the class of the object to delete.
    * @param args the IDs of the object, in order of appearance of declaration in the target object class.
    * @param <T> The type of the object.
    * @return the number of rows affected.
    */
   public static <T> int deleteObjectById(Class<T> clazz, Object... args)
   {
      return SqlClosure.sqlExecute(c -> OrmElf.deleteObjectById(c, clazz, args));
   }

   /**
    * @see #objectsFromClause(Class, String, Object...)
    * @deprecated
    */
   public static <T> List<T> listFromClause(Class<T> clazz, String clause, Object... args)
   {
      return objectsFromClause(clazz, clause, args);
   }

   /**
    * Gets a list of objects from the database.
    * @param clazz The type of the desired objects.
    * @param clause The from or where clause.
    * @param args The arguments needed for the clause.
    * @param <T> The type of the objects.
    * @return The list of objects.
    */
   public static <T> List<T> objectsFromClause(Class<T> clazz, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> OrmReader.listFromClause(c, clazz, clause, args));
   }

   /**
    * Counts the number of rows for the given query.
    *
    * @param clazz the class of the object to query.
    * @param clause The conditional part of a SQL where clause.
    * @param args The query parameters used to find the list of objects.
    * @param <T> the type of object to query.
    * @return The result count.
    */
   public static <T> int countObjectsFromClause(Class<T> clazz, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> OrmElf.countObjectsFromClause(c, clazz, clause, args));
   }
}
