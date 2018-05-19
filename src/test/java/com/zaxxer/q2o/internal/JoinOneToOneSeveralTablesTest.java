package com.zaxxer.q2o.internal;

import com.zaxxer.q2o.*;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sansorm.TestUtils;
import org.sansorm.testutils.*;

import javax.persistence.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 04.05.18
 */

public class JoinOneToOneSeveralTablesTest {

   @After
   public void tearDown() throws Exception {
      q2o.deinitialize();
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Entity @Table(name = "LEFT_TABLE")
   public static class Left {
      private int id;
      private String type;
      private Right right;

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @OneToOne @JoinColumn(name = "id")
      public Right getRight() {
         return right;
      }

      public void setRight(Right right) {
         this.right = right;
      }

      // TODO Support properties/fields with no or only @Basic annotation
      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      @Override
      public String toString() {
         return "Left{" +
            "id=" + id +
            ", type='" + type + '\'' +
            ", right=" + right +
            '}';
      }
   }

   @Entity @Table(name = "RIGHT_TABLE")
   public static class Right {
      private int id;

      @Id
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @Override
      public String toString() {
         return "Right{" +
            "id=" + id +
            '}';
      }
   }

   @Test
   public void setValue() throws NoSuchFieldException, IllegalAccessException {
      Left left = new Left();
      Field rightField = Left.class.getDeclaredField("right");
      PropertyInfo rightInfo = new PropertyInfo(rightField, Left.class);
      assertTrue(rightInfo.isJoinColumn);
      rightInfo.setValue(left, 1);
      assertNotNull(left.getRight());
      assertEquals(1, left.getRight().getId());
   }

   @Test
   public void getValue() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
      Left left = new Left();
      Right right = new Right();
      right.setId(1);
      left.setRight(right);
      Field rightField = Left.class.getDeclaredField("right");
      PropertyInfo rightInfo = new PropertyInfo(rightField, Left.class);
      int rightId = (int) rightInfo.getValue(left);
      assertEquals(1, rightId);
   }

   @Test
   public void introspect() {
      Introspected introspected = new Introspected(Left.class);

      AttributeInfo[] selectableFcInfos = introspected.getSelectableFcInfos();
      Assertions.assertThat(selectableFcInfos).extracting("name").containsExactly("id", "type", "right");

      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      Assertions.assertThat(insertableFcInfos).extracting("name", "insertable").containsExactly(Tuple.tuple("type", true));

      Assertions.assertThat(introspected.getInsertableColumns()).hasSize(1).contains("type");
      Assertions.assertThat(introspected.getUpdatableColumns()).hasSize(1).contains("type");

      String columnsCsv = OrmReader.getColumnsCsv(Left.class);
      assertEquals("LEFT_TABLE.id,LEFT_TABLE.type", columnsCsv);
   }

   /**
    * Currently no graphs are persisted.
    */
   @Test
   public void insertObject() throws SQLException {
      final String[] fetchedSql = new String[1];
      final String[] params = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql, String[] idColNames) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return JoinOneToOneSeveralTablesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        if (param == 1) {
                           return Types.VARCHAR;
                        }
                        throw new RuntimeException("To many parameters");
                     }
                  };
               }

               @Override
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() {
                        return true;
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return 123;
                     }
                  };
               }

               @Override
               public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
                  params[0] = (String) x;
               }
            };
         }
      };
      Left left = new Left();
      left.setType("left");
      Right right = new Right();
      left.setRight(right);
      OrmWriter.insertObject(con, left);
      assertEquals("INSERT INTO LEFT_TABLE(type) VALUES (?)", fetchedSql[0]);
      assertEquals(123, left.getId());
      assertEquals(0, right.getId());
      assertEquals("left", params[0]);
   }

   @Test
   public void extractTableNameOneToManyInverseSide() throws NoSuchFieldException {
      class Test {
         private Collection<GetterAnnotatedPitMainEntity> notes;

         @OneToMany(mappedBy = "pitMainByPitIdent")
         public Collection<GetterAnnotatedPitMainEntity> getNotes() {
            return notes;
         }

         public void setNotes(Collection<GetterAnnotatedPitMainEntity> notes) {
            this.notes = notes;
         }
      }
      Field field = Test.class.getDeclaredField("notes");
      PropertyInfo info = new PropertyInfo(field, Test.class);
      assertEquals("D_PIT_MAIN", info.getDelimitedTableName());
      assertEquals(Collection.class, info.type);
   }

   @Test
   public void extractTableNameManyToOneOwningSide() throws NoSuchFieldException {
      class Test {
         private GetterAnnotatedPitReferenceEntity pitReferenceByPitIdent;
         @ManyToOne
         @JoinColumn(name = "PIT_IDENT", referencedColumnName = "PIR_PIT_IDENT", nullable = false)
         public GetterAnnotatedPitReferenceEntity getPitReferenceByPitIdent() {
            return pitReferenceByPitIdent;
         }

         public void setPitReferenceByPitIdent(GetterAnnotatedPitReferenceEntity pitReferenceByPitIdent) {
            this.pitReferenceByPitIdent = pitReferenceByPitIdent;
         }
      }
      Field field = Test.class.getDeclaredField("pitReferenceByPitIdent");
      PropertyInfo info = new PropertyInfo(field, Test.class);
      assertEquals("D_PIT_REFERENCE", info.getDelimitedTableName());

      Introspected introspected = new Introspected(Test.class);
      AttributeInfo fcInfo = introspected.getFieldColumnInfo("D_PIT_REFERENCE", "PIR_PIT_IDENT");
      assertNotNull(fcInfo);
   }

   @Test
   public void join2Tables() throws SQLException {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE RIGHT_TABLE ("
               + " id INTEGER UNIQUE"
               + ", CONSTRAINT cnst1 FOREIGN KEY(id) REFERENCES LEFT_TABLE (id)"
               + ")");

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('left')");
         Q2Sql.executeUpdate("insert into RIGHT_TABLE (id) values(1)");

         Left left = Q2Obj.fromSelect(Left.class, "SELECT * FROM LEFT_TABLE, RIGHT_TABLE where LEFT_TABLE.id = RIGHT_TABLE.id and LEFT_TABLE.id = ?", 1);

//         System.out.println(left);
         assertNotNull(left.getRight());
         assertEquals(1, left.getRight().getId());
      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LEFT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE RIGHT_TABLE");
      }
   }

   @Test
   public void leftJoin2Tables() throws SQLException {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE RIGHT_TABLE ("
               + " id INTEGER UNIQUE"
               + ", CONSTRAINT cnst1 FOREIGN KEY(id) REFERENCES LEFT_TABLE (id)"
               + ")");

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('left')");

         Left left = Q2Obj.fromSelect(Left.class, "SELECT * FROM LEFT_TABLE" +
            " left join RIGHT_TABLE on LEFT_TABLE.id = RIGHT_TABLE.id" +
            " where LEFT_TABLE.id = ?", 1);

         assertEquals("Left{id=1, type='left', right=null}", left.toString());
      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LEFT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE RIGHT_TABLE");
      }
   }

   @Entity @Table(name = "LEFT_TABLE")
   public static class Left1 {
      private int id;
      private String type;
      private Middle1 middle;

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @OneToOne @JoinColumn(name = "id")
      public Middle1 getMiddle() {
         return middle;
      }

      public void setMiddle(Middle1 middle) {
         this.middle = middle;
      }

      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      @Override
      public String toString() {
         return "Left1{" +
            "id=" + id +
            ", type='" + type + '\'' +
            ", middle=" + middle +
            '}';
      }
   }

   @Entity @Table(name = "MIDDLE_TABLE")
   public static class Middle1 {
      private int id;
      private String type;
      private int rightId;
      private Right1 right;

      @Id
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      @OneToOne @JoinColumn(name = "rightId")
      public Right1 getRight() {
         return right;
      }

      public void setRight(Right1 right) {
         this.right = right;
      }

      @Column(name = "rightId")
      public int getRightId() {
         return rightId;
      }

      public void setRightId(int rightId) {
         this.rightId = rightId;
      }

      @Override
      public String toString() {
         return "Middle1{" +
            "id=" + id +
            ", type='" + type + '\'' +
            ", rightId=" + rightId +
            ", right=" + right +
            '}';
      }
   }

   @Entity @Table(name = "RIGHT_TABLE")
   public static class Right1 {
      private int id;
      private String type;
      private int farRightId;
      private FarRight1 farRight1;

      @Id
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      @Override
      public String toString() {
         return "Right1{" +
            "id=" + id +
            ", type='" + type + '\'' +
            ", farRightId=" + farRightId +
            ", farRight1=" + farRight1 +
            '}';
      }

      public int getFarRightId() {
         return farRightId;
      }

      public void setFarRightId(int farRightId) {
         this.farRightId = farRightId;
      }

      @OneToOne @JoinColumn(name = "farRightId")
      public FarRight1 getFarRight1() {
         return farRight1;
      }

      public void setFarRight1(FarRight1 right) {
         this.farRight1 = right;
      }
   }

   @Entity @Table(name = "FAR_RIGHT_TABLE")
   public static class FarRight1 {
      private int id;
      private String type;

      @Id
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      @Override
      public String toString() {
         return "FarRight1{" +
            "id=" + id +
            ", type='" + type + '\'' +
            '}';
      }
   }

   @Test
   public void join3Tables() throws SQLException {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE MIDDLE_TABLE ("
               + " id INTEGER UNIQUE"
               + ", type VARCHAR(128)"
               + ", rightId INTEGER UNIQUE"
               + ", CONSTRAINT MIDDLE_TABLE_cnst1 FOREIGN KEY(id) REFERENCES LEFT_TABLE (id)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE RIGHT_TABLE ("
               + " id INTEGER UNIQUE"
               + ", type VARCHAR(128)"
               + ", CONSTRAINT RIGHT_TABLE_cnst1 FOREIGN KEY(id) REFERENCES MIDDLE_TABLE (rightId)"
               + ")");

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('type: left')");
         Q2Sql.executeUpdate("insert into MIDDLE_TABLE (id, type, rightId) values(1, 'type: middle', 1)");
         Q2Sql.executeUpdate("insert into RIGHT_TABLE (id, type) values(1, 'type: right')");

         Left1 left = Q2Obj.fromSelect(Left1.class, "SELECT * FROM LEFT_TABLE, MIDDLE_TABLE, RIGHT_TABLE where LEFT_TABLE.id = MIDDLE_TABLE.id and MIDDLE_TABLE.RIGHTID = RIGHT_TABLE.ID and LEFT_TABLE.id = ?", 1);

         System.out.println(left);
         assertNotNull(left.getMiddle());
         assertNotNull(left.getMiddle().getRight());
         assertEquals("Left1{id=1, type='type: left', middle=Middle1{id=1, type='type: middle', rightId=1, right=Right1{id=1, type='type: right', farRightId=0, farRight1=null}}}", left.toString());
      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LEFT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE MIDDLE_TABLE");
         Q2Sql.executeUpdate("DROP TABLE RIGHT_TABLE");
      }
   }



   @Test
   public void join4Tables() throws SQLException {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE MIDDLE_TABLE ("
               + " id INTEGER UNIQUE"
               + ", type VARCHAR(128)"
               + ", rightId INTEGER UNIQUE"
               + ", CONSTRAINT MIDDLE_TABLE_cnst1 FOREIGN KEY(id) REFERENCES LEFT_TABLE (id)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE RIGHT_TABLE ("
               + " id INTEGER UNIQUE"
               + ", type VARCHAR(128)"
               + ", farRightId INTEGER UNIQUE"
               + ", CONSTRAINT RIGHT_TABLE_cnst1 FOREIGN KEY(id) REFERENCES MIDDLE_TABLE (rightId)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE FAR_RIGHT_TABLE ("
               + " id INTEGER UNIQUE"
               + ", type VARCHAR(128)"
               + ", CONSTRAINT FAR_RIGHT_TABLE_cnst1 FOREIGN KEY(id) REFERENCES RIGHT_TABLE (farRightId)"
               + ")");

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('type: left')");
         Q2Sql.executeUpdate("insert into MIDDLE_TABLE (id, type, rightId) values(1, 'type: middle', 1)");
         Q2Sql.executeUpdate("insert into RIGHT_TABLE (id, type, farRightId) values(1, 'type: right', 1)");
         Q2Sql.executeUpdate("insert into FAR_RIGHT_TABLE (id, type) values(1, 'type: far right')");

         // Retrieve the whole graph with all values
         Left1 left = Q2Obj.fromSelect(Left1.class,
            "SELECT *" +
               " FROM LEFT_TABLE, MIDDLE_TABLE, RIGHT_TABLE, FAR_RIGHT_TABLE" +
               " where" +
               " LEFT_TABLE.id = MIDDLE_TABLE.id" +
               " and MIDDLE_TABLE.RIGHTID = RIGHT_TABLE.ID" +
               " and RIGHT_TABLE.FARRIGHTID = FAR_RIGHT_TABLE.ID" +
               " and LEFT_TABLE.id = ?"
            , 1);

         assertEquals("Left1{id=1, type='type: left', middle=Middle1{id=1, type='type: middle', rightId=1, right=Right1{id=1, type='type: right', farRightId=1, farRight1=FarRight1{id=1, type='type: far right'}}}}", left.toString());


         // The id fields must be selected at least
         Left1 left1 = Q2Obj.fromSelect(Left1.class,
            "select" +
            " LEFT_TABLE.ID, MIDDLE_TABLE.ID" +
            " FROM LEFT_TABLE, MIDDLE_TABLE" +
            " WHERE" +
            " LEFT_TABLE.ID = MIDDLE_TABLE.ID" +
            " AND MIDDLE_TABLE.ID = 1");

         assertEquals("Left1{id=1, type='null', middle=Middle1{id=1, type='null', rightId=0, right=null}}", left1.toString());


      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LEFT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE MIDDLE_TABLE");
         Q2Sql.executeUpdate("DROP TABLE RIGHT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE FAR_RIGHT_TABLE");
      }
   }

   @Entity @Table(name = "LEFT_TABLE")
   public static class LeftOneToMany {
      private int id;
      private String type;
      private Collection<Right> rights;

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @OneToMany @JoinColumn(name = "id", table = "RIGHT_TABLE")
      public Collection<Right> getRights() {
         return rights;
      }

      public void setRights(Collection<Right> rights) {
         this.rights = rights;
      }

      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      @Override
      public String toString() {
         return "LeftOneToMany{" +
            "id=" + id +
            ", type='" + type + '\'' +
            ", rights=" + rights +
            '}';
      }
   }

   /**
    * OneToMany annotation is ignored.
    */
   @Test
   public void oneToMany() throws SQLException {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE RIGHT_TABLE ("
               + " id INTEGER"
               + ")");

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('left')");
         Q2Sql.executeUpdate("insert into RIGHT_TABLE (id) values(1)");

         LeftOneToMany left = SqlClosure.sqlExecute(c -> {
            PreparedStatement pstmt = c.prepareStatement(
               "SELECT * FROM LEFT_TABLE, RIGHT_TABLE where LEFT_TABLE.id = RIGHT_TABLE.id and LEFT_TABLE.id = ?");
            return Q2Obj.fromStatement(pstmt, LeftOneToMany.class, 1);
         });
         assertEquals("LeftOneToMany{id=1, type='left', rights=null}", left.toString());
      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LEFT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE RIGHT_TABLE");
      }
   }

   @Entity(name = "LEFT_TABLE")
   public static class Left2 {
      private int id;
      private String type;
      private Right2 right;

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @OneToOne @JoinColumn(name = "id")
      public Right2 getRight() {
         return right;
      }

      public void setRight(Right2 right) {
         this.right = right;
      }

      // TODO Support properties/fields with no or only @Basic annotation
      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }

      @Override
      public String toString() {
         return "Left{" +
            "id=" + id +
            ", type='" + type + '\'' +
            ", right=" + right +
            '}';
      }
   }

   @Entity(name = "RIGHT_TABLE")
   public static class Right2 {
      private int id;

      @Id
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @Override
      public String toString() {
         return "Right{" +
            "id=" + id +
            '}';
      }
   }

   /**
    * Support for @Entity name element.
    */
   @Test
   public void entityName() throws SQLException {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try {
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE RIGHT_TABLE ("
               + " id INTEGER UNIQUE"
               + ", CONSTRAINT cnst1 FOREIGN KEY(id) REFERENCES LEFT_TABLE (id)"
               + ")");

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('left')");

         Left2 left = Q2Obj.fromSelect(Left2.class, "SELECT * FROM LEFT_TABLE" +
            " left join RIGHT_TABLE on LEFT_TABLE.id = RIGHT_TABLE.id" +
            " where LEFT_TABLE.id = ?", 1);

         assertEquals("Left{id=1, type='left', right=null}", left.toString());
      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LEFT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE RIGHT_TABLE");
      }
   }

   public static class OrderSummary {
      @Column(name = "order_id")
      private int orderId;

      @Column(name = "full_name")
      private String fullName;

      @Column(name = "total_items")
      private int itemCount;

      @Override
      public String toString() {
         return "OrderSummary{" +
            "orderId=" + orderId +
            ", fullName='" + fullName + '\'' +
            ", itemCount=" + itemCount +
            '}';
      }
   }

   @Test
   public void flattenedTableJoin() {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try {
         Q2Sql.executeUpdate("CREATE TABLE customers (\n" +
            " customer_id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,\n" +
            " last_name VARCHAR(255),\n" +
            " first_name VARCHAR(255),\n" +
            " email VARCHAR(255)\n" +
            ")");

         Q2Sql.executeUpdate("CREATE TABLE orders (" +
            " order_id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY" +
            " ,customer_id INTEGER NOT NULL" +
            " ,CONSTRAINT order_cust_fk FOREIGN KEY (customer_id) REFERENCES customers (customer_id)" +
            ")");

         Q2Sql.executeUpdate("CREATE TABLE order_items (" +
            "   order_id INTEGER NOT NULL," +
            "   product_number VARCHAR(64)," +
            "   item_count INTEGER NOT NULL," +
            "   CONSTRAINT item_order_fk FOREIGN KEY (order_id) REFERENCES orders (order_id)," +
            ")");

         Q2Sql.executeUpdate("insert into customers (last_name, first_name, email) values('last name', 'first name', 'email address')");
         Q2Sql.executeUpdate("insert into orders (customer_id) values(1)");
         Q2Sql.executeUpdate("insert into order_items (order_id, product_number, item_count) values(1, 'product number', 123)");

         OrderSummary orderSummary = Q2Obj.fromSelect(OrderSummary.class, "SELECT o.order_id, first_name || ' ' || last_name AS full_name, cast (SUM(oi.item_count) as int) AS total_items " +
            "FROM orders o, customers c, order_items oi " +
            "WHERE c.customer_id = o.customer_id AND oi.order_id = o.order_id AND o.order_id = ?", 1);

         System.out.println(orderSummary);

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE customers");
         Q2Sql.executeUpdate("DROP TABLE orders");
         Q2Sql.executeUpdate("DROP TABLE order_items");
      }

   }

   // TODO joins per composite primary key.

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
