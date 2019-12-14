package org.sansorm.testutils;

import com.zaxxer.q2o.q2o;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.TestUtils;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-04-14
 */
@RunWith(Parameterized.class)
public class GeneralTestConfigurator {

   public enum Database {
      mysql, sqlite, h2
   }

   @Parameterized.Parameters(name = "springTxSupport={0}, database={1}")
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
         {false, Database.h2}, {true, Database.h2}, {false, Database.mysql}, {true, Database.mysql}, {false, Database.sqlite}, {true, Database.sqlite}
//         {false, Database.mysql}
//         {false, Database.h2}
//         {true, Database.sqlite}
      });
   }

   @Parameterized.Parameter(0)
   public boolean withSpringTx;

   @Parameterized.Parameter(1)
   public Database database;

   public DataSource dataSource;

   @Before
   public void setUp() throws Exception {

      switch (database) {
         case h2:
            dataSource = TestUtils.makeH2DataSource();
            break;
         case mysql:
            dataSource = TestUtils.makeMySqlDataSource("q2o", "root", "yxcvbnm");
            break;
         case sqlite:
            dataSource = TestUtils.getSqLiteDataSource(null);
            break;
      }

      if (!withSpringTx) {
         q2o.initializeTxNone(dataSource);
      }
      else {
         q2o.initializeWithSpringTxSupport(dataSource);
      }

      if (database == Database.mysql) {
         q2o.setMySqlMode(true);
      }

   }

   @After
   public void tearDown() throws Exception {
      q2o.deinitialize();
   }
}
