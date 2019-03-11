package org.sansorm;

import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.Q2ObjList;
import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.q2o;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static com.zaxxer.q2o.Q2Obj.countFromClause;
import static com.zaxxer.q2o.Q2Obj.insert;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sansorm.TestUtils.makeH2DataSource;


public class Query2SpringTest
{
   @BeforeClass
   public static void beforeClass() throws Throwable {
      q2o.initializeWithSpringTxSupport(makeH2DataSource());
      Q2Sql.executeUpdate(
         "CREATE TABLE TargetClass2 ("
            + " id INTEGER NOT NULL IDENTITY PRIMARY KEY,"
            + " string VARCHAR(128),"
            + " someDate TIMESTAMP," // H2 is case-insensitive to column case, ResultSet::getMetaData will return it as SOMEDATE
            + " )");
   }

   @AfterClass
   public static void afterClass() {
      Q2Sql.executeUpdate("drop table TargetClass2");
      q2o.deinitialize();
   }

   /**
    * Tests @Temporal annotation.
    */
   @Test @Ignore
   public void testObjectFromClause()
   {
      // given
      long timestamp = 42L;
      String string = "Hi";
      TargetClass2 original = new TargetClass2(new Date(timestamp), string);
      Q2Obj.insert(original);

      // when
      TargetClass2 target = Q2Obj.fromClause(TargetClass2.class, "someDate = ?", timestamp);
      TargetClass2 targetAgain = Q2Obj.byId(TargetClass2.class, target.getId());

      // then
      assertThat(targetAgain.getId()).isEqualTo(target.getId());
      assertThat(target.getString()).isEqualTo(string);
      assertThat(target.getSomeDate().getTime()).isEqualTo(timestamp);
      assertThat(targetAgain.getString()).isEqualTo(string);
      assertThat(targetAgain.getSomeDate().getTime()).isEqualTo(timestamp);
   }

   @Test
   public void testListFromClause()
   {
      // given
      long timestamp = 43L;
      String string = "Ho";
      TargetClass2 original = new TargetClass2(new Date(timestamp), string);
      Q2Obj.insert(original);

      // when
      List<TargetClass2> target = Q2ObjList.fromClause(TargetClass2.class, "string = ?", string);

      // then
      assertThat(target.get(0).getString()).isEqualTo(string);
      assertThat(target.get(0).getSomeDate().getTime()).isEqualTo(timestamp);
   }

   @Test
   public void testNumberFromSql() {
      Number initialCount = Q2Sql.numberFromSql("SELECT count(id) FROM TargetClass2");
      insert(new TargetClass2(null, ""));

      Number newCount = Q2Sql.numberFromSql("SELECT count(id) FROM TargetClass2");
      assertThat(newCount.intValue()).isEqualTo(initialCount.intValue() + 1);

      int countCount = countFromClause(TargetClass2.class, null);
      assertThat(countCount).isEqualTo(newCount.intValue());
   }

   @Test
   public void testDate()
   {
      Date date = new Date();

      TargetClass2 target = Q2Obj.insert(new TargetClass2(date, "Date"));
      target = Q2Obj.byId(TargetClass2.class, target.getId());

      assertThat(target.getString()).isEqualTo("Date");
      assertThat(target.getSomeDate().getTime()).isEqualTo(date.getTime());
   }
}
