package com.zaxxer.q2o;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Test;
import org.sansorm.TestUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.10.18
 */
public class Q2SqlTest {
   @After
   public void tearDown() throws Exception {
      q2o.deinitialize();
   }

   @Test
   public void executeQuery() throws SQLException {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE MY_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate("insert into MY_TABLE (type) values('one')");
         Q2Sql.executeUpdate("insert into MY_TABLE (type) values('two')");


         ResultSet rs = Q2Sql.executeQuery(con,"SELECT * FROM MY_TABLE where id > ?", 0);
         while (rs.next()) {
            System.out.println(rs.getString("type"));
         }

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE MY_TABLE");
      }
   }
}