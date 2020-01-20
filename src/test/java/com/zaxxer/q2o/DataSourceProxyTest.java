package com.zaxxer.q2o;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sansorm.DataSources;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.01.20
 */
public class DataSourceProxyTest {

   private static DataSource dataSource;
   @BeforeClass
   public static void beforeClass(){
      dataSource = DataSources.getH2ImMemoryDataSource(true);
      dataSource = q2o.initializeTxSimple(dataSource);

   }

   @AfterClass
   public static void afterClass(){

   }

   @Test
   public void wrap() throws SQLException
   {
      DataSource q2oDataSource = DataSourceProxy.wrap(dataSource);
      Connection con = q2oDataSource.getConnection();

      PreparedStatement ps = con.prepareStatement("select 1");
      ResultSet rs = ps.executeQuery();
      rs.next();
      int result = rs.getInt(1);

      con.close();
      assertEquals(1, result);

   }
}
