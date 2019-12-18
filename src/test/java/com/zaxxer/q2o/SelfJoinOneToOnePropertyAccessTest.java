package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.PropertyAccessedOneToOneSelfJoin;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.DataSources;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.zaxxer.q2o.OrmWriter.insertObject;
import static com.zaxxer.q2o.Q2Obj.*;
import static java.lang.System.out;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;


public class SelfJoinOneToOnePropertyAccessTest {

   @Test
   public void selfJoinColumnH2() throws SQLException {

      JdbcDataSource ds = DataSources.makeH2DataSource();
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
         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.setType("parent");
         insert(parent);
         assertTrue(parent.getId() > 0);

         // SansOrm does not persist child when parent is persisted
         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.setType("child");
         child.setParentId(parent);
         update(parent);
         assertEquals(0, child.getId());

         // persist child explicitely. parentId from parent is also stored.
         insertObject(con, child);
         assertTrue(child.getId() > 0);
         int count = countFromClause(PropertyAccessedOneToOneSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         PropertyAccessedOneToOneSelfJoin childFromDb = fromClause
            (PropertyAccessedOneToOneSelfJoin.class, "id=2");
//         PropertyAccessedOneToOneSelfJoin childFromDb = Q2Obj.objectById(con, PropertyAccessedOneToOneSelfJoin.class, 2);
         assertNotNull(childFromDb.getParentId());
         assertEquals(1, childFromDb.getParentId().getId());

         // To add remaining attributes to parent reload
         assertEquals(null, childFromDb.getParentId().getType());
         refresh(con, childFromDb.getParentId());
         assertEquals("parent", childFromDb.getParentId().getType());
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClause() throws SQLException {

      JdbcDataSource ds = DataSources.makeH2DataSource();
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

         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.setType("parent");
         insert(parent);

         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.setType("child");
         child.setParentId(parent);
         insertObject(con, child);

         List<PropertyAccessedOneToOneSelfJoin> objs = Q2ObjList.fromClause(PropertyAccessedOneToOneSelfJoin.class, "id=2");
         objs.forEach(out::println);
         assertThat(objs).filteredOn(obj -> obj.getParentId() != null && obj.getParentId().getId() == 1).size().isEqualTo(1);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

   @Test
   public void insertListNotBatched() throws SQLException {

      JdbcDataSource ds = DataSources.makeH2DataSource();
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

         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.setType("parent");
         insert(parent);

         PropertyAccessedOneToOneSelfJoin parent2 = new PropertyAccessedOneToOneSelfJoin();
         parent2.setType("parent");
         insert(parent2);

         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.setType("child");
         child.setParentId(parent);

         PropertyAccessedOneToOneSelfJoin child2 = new PropertyAccessedOneToOneSelfJoin();
         child2.setType("child");
         child2.setParentId(parent2);

         ArrayList<PropertyAccessedOneToOneSelfJoin> children = new ArrayList<>();
         children.add(child);
         children.add(child2);

         OrmWriter.insertListNotBatched(con, children);

      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

}
