package org.sansorm;

import com.zaxxer.q2o.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zaxxer.q2o.Q2Obj.countFromClause;
import static com.zaxxer.q2o.Q2Obj.insert;
import static com.zaxxer.q2o.Q2Sql.executeUpdate;
import static com.zaxxer.q2o.q2o.initializeTxNone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sansorm.TestUtils.makeH2DataSource;

public class QueryTest
{
   @BeforeClass
   public static void setup() {
      initializeTxNone(makeH2DataSource());
      executeUpdate(
         "CREATE TABLE target_class1 ("
            + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
            + "timestamp TIMESTAMP, "
            + "string VARCHAR(128), "
            + "string_from_number NUMERIC "
            + ")");
   }

   @AfterClass
   public static void tearDown()
   {
      q2o.deinitialize();
   }

   @Test
   public void shouldPerformCRUD()
   {
      TargetClass1 original = new TargetClass1(new Date(0), "Hi");
      assertThat(original.getId()).isEqualTo(0);

      TargetClass1 inserted = Q2Obj.insert(original);
      assertThat(inserted).isSameAs(original).as("insertOject() only set generated id");
      int idAfterInsert = inserted.getId();
      assertThat(idAfterInsert).isNotEqualTo(0);

      List<TargetClass1> selectedAll = Q2ObjList.fromClause(TargetClass1.class, null);
      assertThat(selectedAll).isNotEmpty();

      TargetClass1 selected = Q2Obj.fromClause(TargetClass1.class, "string = ?", "Hi");
      assertThat(selected.getId()).isEqualTo(idAfterInsert);
      assertThat(selected.getString()).isEqualTo("Hi");
      assertThat(selected.getTimestamp().getTime()).isEqualTo(0L);

      selected.setString("Hi edited");
      TargetClass1 updated = Q2Obj.update(selected);
      assertThat(inserted).isSameAs(original).as("updateObject() only set generated id if it was missing");
      assertThat(updated.getId()).isEqualTo(idAfterInsert);
   }

   @Test
   public void testNumberFromSql() {
      Number initialCount = Q2Sql.numberFromSql("SELECT count(id) FROM target_class1");
      insert(new TargetClass1(null, ""));

      Number newCount = Q2Sql.numberFromSql("SELECT count(id) FROM target_class1");
      assertThat(newCount.intValue()).isEqualTo(initialCount.intValue() + 1);

      int countCount = countFromClause(TargetClass1.class, null);
      assertThat(countCount).isEqualTo(newCount.intValue());
   }

   @Test
   public void testDate()
   {
      Date date = new Date();

      TargetClass1 target = Q2Obj.insert(new TargetClass1(date, "Date"));
      target = Q2Obj.byId(TargetClass1.class, target.getId());

      assertThat(target.getString()).isEqualTo("Date");
      assertThat(target.getTimestamp().getTime()).isEqualTo(date.getTime()); // Timestamp <-> Date equality is assymetrical
   }

   @Test
   public void testTimestamp()
   {
      Timestamp tstamp = new Timestamp(System.currentTimeMillis());
      tstamp.setNanos(200);

      TargetTimestampClass1 target = Q2Obj.insert(new TargetTimestampClass1(tstamp, "Timestamp"));
      target = Q2Obj.byId(TargetTimestampClass1.class, target.getId());

      assertThat(target.getString()).isEqualTo("Timestamp");
      assertThat(target.getTimestamp().getClass()).isEqualTo(Timestamp.class);
      assertThat(target.getTimestamp()).isEqualTo(tstamp);
      assertThat(target.getTimestamp().getNanos()).isEqualTo(200);
   }

   @Test
   public void testConverterSave()
   {
      TargetClass1 target = Q2Obj.insert(new TargetClass1(null, null, "1234"));
      Number number = Q2Sql.numberFromSql("SELECT string_from_number + 1 FROM target_class1 where id = ?", target.getId());
      assertThat(number.intValue()).isEqualTo(1235);
   }

   @Test
   public void testConverterLoad() throws Exception
   {
      TargetClass1 target = Q2Obj.insert(new TargetClass1(null, null, "1234"));
      final int targetId = target.getId();
      target = SqlClosure.sqlExecute(c -> {
         PreparedStatement pstmt = c.prepareStatement(
                 "SELECT t.id, t.timestamp, t.string, (t.string_from_number + 1) as string_from_number FROM target_class1 t where id = ?");
         return Q2Obj.fromStatement(pstmt, TargetClass1.class, targetId);
      });
      assertThat(target.getStringFromNumber()).isEqualTo("1235");
   }

   @Test
   public void testConversionFail()
   {
      TargetClass1 target = Q2Obj.insert(new TargetClass1(null, null, "foobar"));
      target = Q2Obj.byId(TargetClass1.class, target.getId());
      assertThat(target.getStringFromNumber()).isNull();
   }


   @Test
   public void testInsertListNotBatched2() {
      // given
      int count = 5;
      Set<TargetClass1> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClass1(new Date(i), String.valueOf(i)))
         .collect(Collectors.toSet());

      // when
      SqlClosure.sqlExecute(c -> {
         Q2Obj.insertNotBatched(c, toInsert);
         return null;
      });

      // then
      Set<Integer> generatedIds = toInsert.stream().map(BaseClass::getId).collect(Collectors.toSet());
      assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
      assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
   }

   @Test
   public void testInsertListBatched() {
      // given
      int count = 5;
      String u = UUID.randomUUID().toString();
      Set<TargetClass1> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClass1(new Date(i), u + String.valueOf(i)))
         .collect(Collectors.toSet());

      // when
      SqlClosure.sqlExecute(c -> {
         Q2Obj.insertBatched(c, toInsert);
         return null;
      });

      // then
      List<TargetClass1> inserted = Q2ObjList.fromClause(
         TargetClass1.class,
         "string in " + Q2Sql.getInClausePlaceholdersForCount(count),
         IntStream.range(0, count).boxed().map(i -> u + String.valueOf(i)).collect(Collectors.toList()).toArray(new Object[]{}));
      Set<Integer> generatedIds = inserted.stream().map(BaseClass::getId).collect(Collectors.toSet());
      assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
      assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
   }
}
