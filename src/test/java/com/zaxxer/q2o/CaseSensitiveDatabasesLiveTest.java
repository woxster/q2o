package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.InsertObjectH2;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.DataSources;
import org.sansorm.testutils.GeneralTestConfigurator;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.sansorm.DataSources.makeH2DataSource;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-04-15
 */
public class CaseSensitiveDatabasesLiveTest extends GeneralTestConfigurator {

   @Test
   public void insertObject() {

      q2o.initializeTxNone(DataSources.makeH2DataSource());
      try {
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         InsertObjectH2 obj = Q2Obj.insert(new InsertObjectH2());
         assertEquals(1, obj.Id);
         obj = Q2Obj.byId(InsertObjectH2.class, obj.Id);
         assertNotNull(obj);
         int count = Q2Obj.countFromClause(InsertObjectH2.class, "\"Delimited field name\" = 'delimited field value'");
         assertEquals(1, count);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectGeneratedId() {

      q2o.initializeTxNone(DataSources.makeH2DataSource());
      try {
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         InsertObjectH2 obj = new InsertObjectH2();
         obj = Q2Obj.insert(obj);
         obj.defaultCase = "changed";
         obj = Q2Obj.update(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectGeneratedDelimitedId() {

      @Table(name = "\"Test Class\"")
      class TestClass {
         @Id
         @GeneratedValue
         @Column(name = "\"Id\"")
         int id;
         @Column(name = "\"Delimited field name\"")
         String delimitedFieldName = "delimited field value";
         @Column(name = "Default_Case")
         String defaultCase = "default case value";
      }

      try {
         JdbcDataSource dataSource = makeH2DataSource();
         q2o.initializeTxNone(dataSource);
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "\"Id\" INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         TestClass obj = new TestClass();
         obj = Q2Obj.insert(obj);
         obj.defaultCase = "changed";
         obj = Q2Obj.update(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }
}
