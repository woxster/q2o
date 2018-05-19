package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.CompositeKey;
import com.zaxxer.q2o.entities.Left;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.TestUtils;
import org.sansorm.testutils.DummyConnection;
import org.sansorm.testutils.DummyParameterMetaData;
import org.sansorm.testutils.DummyStatement;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 10.04.18
 */
public class Q2ObjTest {

   @Test
   public void updateObjectExludeColumns() throws SQLException {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id = "xyz";
         @Column(name = "FIELD_1")
         String field1 = "field1";
         @Column(name = "FIELD_2")
         String field2 = "field2";
         @Column(name = "\"Field_3\"")
         String field3 = "field3";
         @Column
         String field4 = "field4";
      }
      final String[] fetchedSql = new String[1];
      Map<Integer, String> idxToValue = new HashMap<>();
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return Q2ObjTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }

               @Override
               public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
                  idxToValue.put(parameterIndex, (String) x);
               }
            };
         }
      };
      TestClass obj = Q2Obj.updateExcludeColumns(con, new TestClass(), "field_1", "Field_3");
      assertEquals("UPDATE Test_Class SET FIELD_2=?,field4=? WHERE id=?", fetchedSql[0]);
      assertEquals("field2", idxToValue.get(1));
      assertEquals("field4", idxToValue.get(2));
      assertEquals("xyz", idxToValue.get(3));
   }

   @Test
   public void byIdWithTarget() {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try {
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");
         Left left = new Left();
         left.setType("test");
         Q2Obj.insert(left);
         assertTrue(left.getId() != 0);

         Left left1 = new Left();
         left1.setId(left.getId());
         Q2Obj.byId(left1);

         assertEquals(left.getType(), left1.getType());
         assertNotSame(left, left1);
      }
      finally {
         Q2Sql.executeUpdate("drop table LEFT_TABLE");
         q2o.deinitialize();
      }
   }

   @Test
   public void byCompositeKeyWithTarget() {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try {
         Q2Sql.executeUpdate(
            "CREATE TABLE COMPOSITEKEY ("
               + " id1 BIGINT NOT NULL"
               + ", id2 BIGINT NOT NULL"
               + ", note VARCHAR(128)"
               + ", PRIMARY KEY (id1, id2)"
               + ")");

         CompositeKey compositeKey = new CompositeKey();
         compositeKey.setId1(1);
         compositeKey.setId2(1);
         compositeKey.setNote("test");
         Q2Obj.insert(compositeKey);

         compositeKey.setNote(null);
         assertNull(compositeKey.getNote());

         Q2Obj.byId(compositeKey);

         assertEquals("test", compositeKey.getNote());

      }
      finally {
         Q2Sql.executeUpdate("drop table COMPOSITEKEY");
         q2o.deinitialize();
      }
   }

   public static class IncludeColumns {
      @Id @GeneratedValue
      int id;
      String note;
      String note2;
   }

   @Test
   public void updateIncludeColumns() {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try {
         Q2Sql.executeUpdate(
            "CREATE TABLE IncludeColumns ("
               + " id BIGINT NOT NULL IDENTITY PRIMARY KEY"
               + ", note VARCHAR(128)"
               + ", note2 VARCHAR(128)"
               + ")");

         IncludeColumns obj = new IncludeColumns();
         obj.note = "first insert";
         Q2Obj.insert(obj);

         assertEquals(1, obj.id);

         obj.note = "note";
         obj.note2 = "note2";

         IncludeColumns note = Q2Obj.updateIncludeColumns(obj, "note");

         IncludeColumns obj2 = Q2Obj.fromClause(IncludeColumns.class, "id=1");
         assertNull(obj2.note2);

      }
      finally {
         Q2Sql.executeUpdate("drop table IncludeColumns");
         q2o.deinitialize();
      }
   }

   @Test
   public void updateIncludeColumnsOmitted() {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try {
         Q2Sql.executeUpdate(
            "CREATE TABLE IncludeColumns ("
               + " id BIGINT NOT NULL IDENTITY PRIMARY KEY"
               + ", note VARCHAR(128)"
               + ", note2 VARCHAR(128)"
               + ")");

         IncludeColumns obj = new IncludeColumns();
         obj.note = "first insert";
         Q2Obj.insert(obj);

         assertEquals(1, obj.id);

         obj.note = "note";
         obj.note2 = "note2";

         IncludeColumns note = Q2Obj.updateIncludeColumns(obj);

         IncludeColumns obj2 = Q2Obj.fromClause(IncludeColumns.class, "id=1");
         assertEquals("note2", obj2.note2);

      }
      finally {
         Q2Sql.executeUpdate("drop table IncludeColumns");
         q2o.deinitialize();
      }
   }

   // ######### Utility methods ######################################################

   private int getParameterCount(String s) {
      int count = 0;
      for (Byte b : s.getBytes()) {
         if ((int)b == '?') {
            count++;
         }
      }
      return count;
   }
}
