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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Provides SQL closures around common query types
 */
public final class SqlClosureElf
{
    private SqlClosureElf()
    {
    }

   /**
    * Gets an object by ID from the database.
    * @param type The type of the desired object.
    * @param ids The ID or IDs of the object.
    * @param <T> The type of the object.
    * @return The object or {@code null}
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> T getObjectById(Class<T> type, Object... ids)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.objectById(c, type, ids));
   }

   /**
    * Gets an object using a from clause.
    * @param type The type of the desired object.
    * @param clause The WHERE clause.
    * @param args The arguments for the WHERE clause.
    * @param <T> The type of the object.
    * @return The object or {@code null}
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> T objectFromClause(Class<T> type, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.objectFromClause(c, type, clause, args));
   }

   /**
    * Inserts the given object into the database.
    * @param object The object to insert.
    * @param <T> The type of the object.
    * @return The inserted object populated with any generated IDs.
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> T insertObject(T object)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.insertObject(c, object));
   }

   /**
    * Updates the given object in the database.
    * @param object The object to update.
    * @param <T> The type of the object.
    * @return The updated object.
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> T updateObject(T object)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.updateObject(c, object));
   }

   /**
    * Delete the given object in the database.
    * @param object the object to delete.
    * @param <T> The type of the object.
    * @return the number of rows affected.
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> int deleteObject(T object)
   {
      return SqlClosure.sqlExecute(c ->  Q2Obj.deleteObject(c, object));
   }

   /**
    * Delete an object from the database by ID.
    * @param clazz the class of the object to delete.
    * @param args the IDs of the object, in order of appearance of declaration in the target object class.
    * @param <T> The type of the object.
    * @return the number of rows affected.
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> int deleteObjectById(Class<T> clazz, Object... args)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.deleteObjectById(c, clazz, args));
   }

   /**
    * Gets a list of objects from the database.
    * @param clazz The type of the desired objects.
    * @param clause The from or where clause.
    * @param args The arguments needed for the clause.
    * @param <T> The type of the objects.
    * @return The list of objects.
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> List<T> listFromClause(Class<T> clazz, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.listFromClause(c, clazz, clause, args));
   }

   /**
    * Counts the number of rows for the given query.
    *
    * @param clazz the class of the object to query.
    * @param clause The conditional part of a SQL where clause.
    * @param args The query parameters used to find the list of objects.
    * @param <T> the type of object to query.
    * @return The result count.
    * @deprecated Use {@link Q2Obj}
    */
   public static <T> int countObjectsFromClause(Class<T> clazz, String clause, Object... args)
   {
      return SqlClosure.sqlExecute(c -> Q2Obj.countObjectsFromClause(c, clazz, clause, args));
   }

   /**
    * @deprecated Use {@link Q2Sql}
    */
   public static Number numberFromSql(String sql, Object... args)
    {
        return Q2Sql.numberFromSql(sql, args);
    }

   /**
    * @deprecated Use {@link Q2Sql}
    */
   public static int executeUpdate(final String sql, final Object... args)
    {
       return Q2Sql.executeUpdate(sql, args);
    }

   /**
    * @deprecated Use {@link Q2Sql}
    */
   @SafeVarargs
   public static <T> String getInClausePlaceholders(final T... items)
   {
      return Q2Sql.getInClausePlaceholdersForCount(items.length);
   }

   /**
    * @deprecated Use {@link Q2Sql}
    */
   public static String getInClausePlaceholdersForCount(final int placeholderCount)
   {
      return Q2Sql.getInClausePlaceholdersForCount(placeholderCount);
   }

   /**
    * @deprecated Use {@link Q2Sql}
    */
   public static Number numberFromSql(Connection connection, String sql, Object... args) throws SQLException
   {
      return Q2Sql.numberFromSql(connection, sql, args);
   }

   /**
    * @deprecated Use {@link Q2Sql}
    */
   public static ResultSet executeQuery(Connection connection, String sql, Object... args) throws SQLException
   {
      return Q2Sql.executeQuery(connection, sql, args);
   }

   /**
    * @deprecated Use {@link Q2Sql}
    */
   public static int executeUpdate(Connection connection, String sql, Object... args) throws SQLException
   {
      return Q2Sql.executeUpdate(connection, sql, args);
   }
}
