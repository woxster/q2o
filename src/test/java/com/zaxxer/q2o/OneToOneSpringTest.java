package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.TestUtils;
import org.sansorm.testutils.DummyConnection;
import org.sansorm.testutils.DummyParameterMetaData;
import org.sansorm.testutils.DummyResultSet;
import org.sansorm.testutils.DummyStatement;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 04.05.18
 */
// TODO Rename to OneToOneTest
@RunWith(Parameterized.class)
public class OneToOneSpringTest {

   @Parameterized.Parameters(name = "withSpringTxSupport={0}")
   public static Collection<Object[]> data() {
   	return Arrays.asList(new Object[][] {
   		{false}, {true}
   	});
   }

   @Parameterized.Parameter(0)
   public static boolean withSpringTx;
   JdbcDataSource ds;

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
                        return OneToOneSpringTest.this.getParameterCount(fetchedSql[0]);
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
   public void join2Tables() throws SQLException {
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

         System.out.println(left);
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
   public void join2TablesTwoRows() throws SQLException {
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

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('left 1')");
         Q2Sql.executeUpdate("insert into RIGHT_TABLE (id) values(1)");
         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('left 2')");
         Q2Sql.executeUpdate("insert into RIGHT_TABLE (id) values(2)");

         List<Left> leftList = Q2ObjList.fromSelect(Left.class, "SELECT * FROM LEFT_TABLE, RIGHT_TABLE where LEFT_TABLE.id = RIGHT_TABLE.id");

         System.out.println(leftList);
         assertEquals("[Left{id=1, type='left 1', right=Right{id=1}}, Left{id=2, type='left 2', right=Right{id=2}}]", leftList.toString());

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

   @Test
   public void join3Tables() throws SQLException {
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
   public void join3TablesSelectOrderChanged() throws SQLException {
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

         Left1 left = Q2Obj.fromSelect(Left1.class, "SELECT * FROM RIGHT_TABLE, LEFT_TABLE, MIDDLE_TABLE where LEFT_TABLE.id = MIDDLE_TABLE.id and MIDDLE_TABLE.RIGHTID = RIGHT_TABLE.ID and LEFT_TABLE.id = ?", 1);

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

   /**
    * Support for @Entity name element.
    */
   @Test
   public void entityName() {
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

   @Test
   public void flattenedTableJoin() {
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

   // IMPROVE joins per composite primary key.

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
