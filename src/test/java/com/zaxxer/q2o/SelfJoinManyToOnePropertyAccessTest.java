package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.PropertyAccessedSelfJoin;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.TestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class SelfJoinManyToOnePropertyAccessTest {

   @Test
   public void selfJoinColumnPropertyAccessH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         // store parent
         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.setType("parent");
         Q2Obj.insert(parent);
         assertTrue(parent.getId() > 0);

         // SansOrm does not persist child when parent is persisted
         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.setType("child");
         child.setParentId(parent);
         Q2Obj.update(parent);
         assertEquals(0, child.getId());

         // persist child explicitely. parentId from parent is also stored.
         OrmWriter.insertObject(con, child);
         assertTrue(child.getId() > 0);
         int count = Q2Obj.countFromClause(PropertyAccessedSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         PropertyAccessedSelfJoin childFromDb = Q2Obj.fromClause
            (PropertyAccessedSelfJoin.class, "id=2");
//         PropertyAccessedOneToOneSelfJoin childFromDb = Q2Obj.objectById(con, PropertyAccessedOneToOneSelfJoin.class, 2);
         assertNotNull(childFromDb.getParentId());
         assertEquals(1, childFromDb.getParentId().getId());

         // To add remaining attributes to parent reload
         assertEquals(null, childFromDb.getParentId().getType());
         Q2Obj.refresh(con, childFromDb.getParentId());
         assertEquals("parent", childFromDb.getParentId().getType());
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClauseSingleObject() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.setType("parent");
         Q2Obj.insert(parent);

         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.setType("child");
         child.setParentId(parent);
         OrmWriter.insertObject(con, child);

         List<PropertyAccessedSelfJoin> objs = Q2ObjList.fromClause(PropertyAccessedSelfJoin.class, "id=2");
         objs.forEach(out::println);
         assertThat(objs).filteredOn(obj -> obj.getParentId() != null && obj.getParentId().getId() == 1).size().isEqualTo(1);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClauseSeveralObjects() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.setType("parent");
         Q2Obj.insert(parent);

         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.setType("child");
         child.setParentId(parent);
         OrmWriter.insertObject(con, child);

         List<PropertyAccessedSelfJoin> objs = Q2ObjList.fromClause(PropertyAccessedSelfJoin.class, "type like '%'");
         objs.forEach(out::println);
         assertThat(objs).filteredOn(obj -> obj.getParentId() != null && obj.getParentId().getId() == 1).size().isEqualTo(1);
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   /**
    * Smoke test
    */
   @Test
   public void listFromClause3() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");
         Q2Sql.executeUpdate("insert into JOINTEST (type) values ('test')");
         Q2Sql.executeUpdate("insert into JOINTEST (type) values ('test2')");

         List<PropertyAccessedSelfJoin> objs = Q2ObjList.fromClause(PropertyAccessedSelfJoin.class, null);
         objs.forEach(System.out::println);
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   /**
    * Smoke test
    */
   @Test
   public void insertListNotBatched() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.setType("parent");
         Q2Obj.insert(parent);

         PropertyAccessedSelfJoin parent2 = new PropertyAccessedSelfJoin();
         parent2.setType("parent");
         Q2Obj.insert(parent2);

         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.setType("child");
         child.setParentId(parent);

         PropertyAccessedSelfJoin child2 = new PropertyAccessedSelfJoin();
         child2.setType("child");
         child2.setParentId(parent2);

         ArrayList<PropertyAccessedSelfJoin> children = new ArrayList<>();
         children.add(child);
         children.add(child2);

         OrmWriter.insertListNotBatched(con, children);

      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

//   /**
//    * Fails because the current implementation can not determine which value corresponds with which of the both instances.
//    */
//   @Test
//   public void fullyLoadChildAndParent() {
//      JdbcDataSource ds = TestUtils.makeH2DataSource();
//      q2o.initializeTxNone(ds);
//      try {
//         executeUpdate(
//            " CREATE TABLE JOINTEST (" +
//               " "
//               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
//               + ", parentId INTEGER"
//               + ", type VARCHAR(128)" +
//               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
//               + ")");
//
//         // store parent
//         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
//         parent.setType("parent");
//         insert(parent);
//         assertTrue(parent.getId() > 0);
//
//         // persist child
//         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
//         child.setType("child");
//         child.setParentId(parent);
//         insert(child);
//         assertTrue(child.getId() > 0);
//
//         PropertyAccessedSelfJoin obj = fromSelect(PropertyAccessedSelfJoin.class, "select * from JOINTEST child, JOINTEST parent where child.parentId = parent.id and child.id = 2");
//         out.println(obj);
//         assertEquals("Test{id=2, parentId=Test{id=1, parentId=null, type='parent'}, type='child'}", obj.toString());
//
//      }
//      catch (Exception e) {
//         e.printStackTrace();
//         throw e;
//      }
//      finally {
//         Q2Sql.executeUpdate("DROP TABLE JOINTEST");
//      }
//
//   }
}
