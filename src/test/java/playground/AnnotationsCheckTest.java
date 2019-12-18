package playground;

import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.entities.DataTypes;
import com.zaxxer.q2o.entities.Left;
import com.zaxxer.q2o.entities.Left1;
import com.zaxxer.q2o.entities.Middle1;
import com.zaxxer.q2o.q2o;
import org.hibernate.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sansorm.DataSources;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 14.12.19
 */
public class AnnotationsCheckTest {

   private static EntityManager entityManager;
   private static EntityTransaction entityTransaction;
   private static SessionFactory sessionFactory;

   @BeforeClass
   public static void beforeClass() throws NamingException {

      final DataSource dataSource = DataSources.makeMySqlDataSource("q2o", "root", "yxcvbnm");

      InitialContext initialContext = new InitialContext();
      Context jdbcCtx = initialContext.createSubcontext("datasources");
      jdbcCtx.bind("mysql", dataSource);

      q2o.initializeTxNone(dataSource);
      q2o.setMySqlMode(true);

      EntityManagerFactory entityManagerFactory = Persistence
         .createEntityManagerFactory("mysql_persistance_unit");
      sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
      entityManager = entityManagerFactory
         .createEntityManager();
      entityTransaction = entityManager.getTransaction();
   }

   @AfterClass
   public static void afterClass(){
      q2o.deinitialize();
      // To delete all created tables.
      sessionFactory.close();
      entityManager.close();
   }

   @Test
   public void persistDataTypes() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setMyInteger(111);
      entityTransaction.begin();
      entityManager.persist(dataTypes);
      entityTransaction.commit();
   }

   @Test
   public void join2Tables() {
      Q2Sql.executeUpdate("insert into RIGHT_TABLE (type) values('right')");

      Q2Sql.executeUpdate("insert into LEFT_TABLE (rightId, type) values(1, 'left')");

      entityTransaction.begin();
      Left left = entityManager.find(Left.class, 1);
      entityTransaction.commit();
      assertEquals("Left{id=1, type='left', right=Right{id=1, type='right'}}", left.toString());
   }

   @Test
   public void join3Tables() {
      entityTransaction.begin();
      Left1 left1 = new Left1();
      left1.setType("left1");
      Middle1 middle1 = new Middle1();
      middle1.setType("Middle1");
      left1.setMiddle(middle1);

      entityManager.persist(middle1);
      entityManager.persist(left1);
      entityTransaction.commit();
   }
}
