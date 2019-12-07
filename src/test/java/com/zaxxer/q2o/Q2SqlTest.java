package com.zaxxer.q2o;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.10.18
 */
public class Q2SqlTest extends GeneralTestConfigurator {

   @Test
   public void executeQuery() throws SQLException {
      switch (database) {
         case h2:
         case sqlite:
            Q2Sql.executeUpdate(
               "CREATE TABLE MY_TABLE ("
                  + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
                  + ", type VARCHAR(128)"
                  + ")");
            break;
         case mysql:
            Q2Sql.executeUpdate(
               "CREATE TABLE MY_TABLE ("
                  + " id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT"
                  + ", type VARCHAR(128)"
                  + ")");
            break;
      }
      try {
         Q2Sql.executeUpdate("insert into MY_TABLE (type) values('one')");
         Q2Sql.executeUpdate("insert into MY_TABLE (type) values('two')");

         String[] expected = {"one", "two"};
         String[] got = new String[2];
         ResultSet rs = Q2Sql.executeQuery(dataSource.getConnection(),"SELECT * FROM MY_TABLE where id > ?", 0);
         int i = 0;
         while (rs.next()) {
            got[i++] = rs.getString("type");
         }
         rs.close();
         Assertions.assertThat(expected).containsAll(Arrays.asList(got));
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE MY_TABLE");
      }
   }
}
