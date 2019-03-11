package com.zaxxer.q2o;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.TestUtils;

import javax.persistence.Id;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 25.05.18
 */
@RunWith(Parameterized.class)
public class Q2ObjListTest {

   @Parameterized.Parameters(name = "withSpringTxSupport={0}")
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
         {false}, {true}
      });
   }

   @Parameterized.Parameter(0)
   public static boolean withSpringTx;

   @Before
   public void setUp() throws Exception {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
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

   public static class MyTest {
      @Id
      int id;
      String note;
   }

   @Test
   public void deleteByWhereClause() {
      try {
         Q2Sql.executeUpdate(
            "CREATE TABLE mytest ("
               + " id BIGINT NOT NULL IDENTITY PRIMARY KEY"
               + ", note VARCHAR(128)"
               + ")");
         int count = Q2Sql.executeUpdate("insert into mytest (note) values('test')");
         assertEquals(1, count);
         count = Q2ObjList.deleteByWhereClause(MyTest.class, "id = ?", 1);
         assertEquals(1, count);
      }
      finally {
         Q2Sql.executeUpdate("drop table mytest");
      }

   }
}
