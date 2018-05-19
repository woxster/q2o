package com.zaxxer.q2o.internal;

import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.q2o;
import com.zaxxer.q2o.SqlClosureElf;
import org.assertj.core.api.Assertions;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.TestUtils;

import javax.persistence.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;


public class SelfJoinOneToOnePropertyAccessTest {

   @org.junit.Test
   public void selfJoinFieldAccess() {
      @Table(name = "TEST")
      class Test {
         @JoinColumn(name = "id", referencedColumnName = "parentId")
         private Test parent;
      }
      Introspected introspected = new Introspected(Test.class);
      AttributeInfo info = introspected.getSelfJoinColumnInfo();
      assertTrue(info.isToBeConsidered());
   }

   @Table(name = "JOINTEST")
   public static class PropertyAccessedOneToOneSelfJoin {
      private int id;
      private PropertyAccessedOneToOneSelfJoin parentId;
      private String type;

      @Override
      public String toString() {
         return "Test{" +
            "id=" + id +
            ", parentId=" + parentId +
            ", type='" + type + '\'' +
            '}';
      }

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @OneToOne
      @JoinColumn(name = "parentId", referencedColumnName = "id")
      public PropertyAccessedOneToOneSelfJoin getParentId() {
         return parentId;
      }

      public void setParentId(PropertyAccessedOneToOneSelfJoin parentId) {
         this.parentId = parentId;
      }

      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }
   }

   @Test
   public void introspectJoinColumn() {

      Introspected introspected = new Introspected(PropertyAccessedOneToOneSelfJoin.class);
      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
//      Arrays.stream(insertableFcInfos).forEach(System.out::println);
      assertEquals(2, insertableFcInfos.length);
   }

   @Test
   public void selfJoinColumnH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         // store parent
         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.type = "parent";
         Q2Obj.insert(parent);
         assertTrue(parent.id > 0);

         // SansOrm does not persist child when parent is persisted
         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.type = "child";
         child.parentId = parent;
         Q2Obj.update(parent);
         assertEquals(0, child.id);

         // persist child explicitely. parentId from parent is also stored.
         OrmWriter.insertObject(con, child);
         assertTrue(child.id > 0);
         int count = Q2Obj.countFromClause(PropertyAccessedOneToOneSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         PropertyAccessedOneToOneSelfJoin childFromDb = Q2Obj.fromClause
            (PropertyAccessedOneToOneSelfJoin.class, "id=2");
//         PropertyAccessedOneToOneSelfJoin childFromDb = Q2Obj.objectById(con, PropertyAccessedOneToOneSelfJoin.class, 2);
         assertNotNull(childFromDb.parentId);
         assertEquals(1, childFromDb.parentId.id);

         // To add remaining attributes to parent reload
         assertEquals(null, childFromDb.parentId.type);
         Q2Obj.refresh(con, childFromDb.parentId);
         assertEquals("parent", childFromDb.parentId.type);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClause() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.type = "parent";
         Q2Obj.insert(parent);

         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.type = "child";
         child.parentId = parent;
         OrmWriter.insertObject(con, child);

         List<PropertyAccessedOneToOneSelfJoin> objs = Q2Obj.objectsFromClause(PropertyAccessedOneToOneSelfJoin.class, "id=2");
         objs.forEach(System.out::println);
         Assertions.assertThat(objs).filteredOn(obj -> obj.parentId != null && obj.parentId.id == 1).size().isEqualTo(1);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   @Test
   public void insertListNotBatched() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedOneToOneSelfJoin parent = new PropertyAccessedOneToOneSelfJoin();
         parent.type = "parent";
         Q2Obj.insert(parent);

         PropertyAccessedOneToOneSelfJoin parent2 = new PropertyAccessedOneToOneSelfJoin();
         parent2.type = "parent";
         Q2Obj.insert(parent2);

         PropertyAccessedOneToOneSelfJoin child = new PropertyAccessedOneToOneSelfJoin();
         child.type = "child";
         child.parentId = parent;

         PropertyAccessedOneToOneSelfJoin child2 = new PropertyAccessedOneToOneSelfJoin();
         child2.type = "child";
         child2.parentId = parent2;

         ArrayList<PropertyAccessedOneToOneSelfJoin> children = new ArrayList<>();
         children.add(child);
         children.add(child2);

         OrmWriter.insertListNotBatched(con, children);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

}
