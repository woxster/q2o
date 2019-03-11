package com.zaxxer.q2o;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.TestUtils;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 22.04.18
 */
@RunWith(Parameterized.class)
public class CompositeKeyTest {

   @Parameterized.Parameters(name = "springTxSupport={0}")
   public static Collection<Object[]> data() {
   	return Arrays.asList(new Object[][] {
   		{false}, {true}
   	});
   }

   @Parameterized.Parameter(0)
   public boolean withSpringTx;
   private DataSource ds;

   @Before
   public void setUp() throws Exception {
      ds = TestUtils.makeH2DataSource();
      if (!withSpringTx) {
         q2o.initializeTxNone(ds);
      }
      else {
         q2o.initializeWithSpringTxSupport(ds);
      }
   }

   @After
   public void tearDown() throws Exception {
      if (!withSpringTx) {
         q2o.deinitialize();
      }
      else {
         q2o.deinitialize();
      }
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void invalidCompositePrimaryKey() {
      class TestClass {
         @Id @GeneratedValue
         String Id1;
         @Id
         String Id2;
         @Id
         String Id3;
         String name;
      }
      thrown.expectMessage("Cannot have multiple @Id annotations and @GeneratedValue at the same time.");
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
   }

   @Table
   public static class TestClass2 {
      @Id
      String id1 = "id1";
      @Id
      String id2 = "id2";
      @Column
      String field;
   }


   @Test
   public void insertObjectCompositeKeyH2() throws SQLException {
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE TestClass2 ("
               + "id1 VARCHAR(128) NOT NULL, "
               + "id2 VARCHAR(128) NOT NULL, "
               + "field VARCHAR(128), "
               + "PRIMARY KEY (id1, id2)"
               + ")");

         String id1 = "id1";
         String id2 = "id2";
         String field = "field";

         TestClass2 obj = Q2Obj.insert(new TestClass2());
         assertEquals(id1, obj.id1);
         obj = Q2Obj.byId(TestClass2.class, obj.id1, obj.id2);
         assertNotNull(obj);

         Q2Sql.executeUpdate(
            "update TestClass2 set field = 'changed'");

         TestClass2 obj2 = Q2Obj.refresh(con, obj);
         assertSame(obj, obj2);
         assertEquals("changed", obj.field);

      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE TestClass2");
      }
   }

   @Test
   public void updateObjectCompositeKeyH2() throws SQLException {
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE TestClass2 ("
               + "id1 VARCHAR(128) NOT NULL, "
               + "id2 VARCHAR(128) NOT NULL, "
               + "field VARCHAR(128), "
               + "PRIMARY KEY (id1, id2)"
               + ")");

         String id1 = "id1";
         String id2 = "id2";
         String field = "field";

         TestClass2 obj = Q2Obj.insert(new TestClass2());

         obj = Q2Obj.byId(obj.getClass(), obj.id1, obj.id2);
         assertNotNull(obj);
         assertNull(obj.field);

         obj.field = "changed";
         Q2Obj.update(con, obj);
         obj = Q2Obj.byId(con, obj.getClass(), obj.id1, obj.id2);
         assertEquals("changed", obj.field);

      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE TestClass2");
      }
   }

   @Test
   public void deleteObjectCompositeKeyH2() throws SQLException {
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE TestClass2 ("
               + "id1 VARCHAR(128) NOT NULL, "
               + "id2 VARCHAR(128) NOT NULL, "
               + "field VARCHAR(128), "
               + "PRIMARY KEY (id1, id2)"
               + ")");

         String id1 = "id1";
         String id2 = "id2";
         String field = "field";

         TestClass2 obj = Q2Obj.insert(new TestClass2());
         int rowCount = Q2Obj.countFromClause(con, obj.getClass(), "field is null");
         assertEquals(1, rowCount);

         Q2Obj.delete(con, obj);
         rowCount = Q2Obj.countFromClause(con, obj.getClass(), "field is null");
         assertEquals(0, 0);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE TestClass2");
      }
   }

}
