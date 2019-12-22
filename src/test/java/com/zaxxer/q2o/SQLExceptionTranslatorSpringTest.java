package com.zaxxer.q2o;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sansorm.DataSources;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;

import java.sql.SQLException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-05
 */
public class SQLExceptionTranslatorSpringTest {

   private static JdbcDataSource dataSource;

   @BeforeClass
   public static void beforeClass() throws Exception {
      dataSource = DataSources.getH2DataSource();
   }

   @AfterClass
   public static void afterClass() throws Exception {

   }

   @Test
   public void translate() {
      SQLExceptionTranslatorSpring translatorSpring = new SQLExceptionTranslatorSpring(dataSource){
         @Override
         public DataAccessException translate(final String task, final String sql, final SQLException ex) {
            throw new EmptyResultDataAccessException(1);
         }
      };

   }
}
