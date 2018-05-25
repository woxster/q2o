package com.zaxxer.q2o;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sansorm.TestUtils;

import javax.persistence.Id;
import javax.sql.DataSource;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 25.05.18
 */
public class Q2ObjListTest {


   private static DataSource dataSource;

   @BeforeClass
   public static void beforeClass() throws Exception {
      dataSource = q2o.initializeTxNone(TestUtils.makeH2DataSource());
   }

   @AfterClass
   public static void afterClass() throws Exception {
      q2o.deinitialize();
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
