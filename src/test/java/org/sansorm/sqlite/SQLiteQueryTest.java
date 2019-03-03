package org.sansorm.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.q2o.*;
import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zaxxer.q2o.Q2Sql.executeUpdate;
import static com.zaxxer.q2o.q2o.initializeTxNone;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sansorm.TestUtils.makeSQLiteDataSource;

public class SQLiteQueryTest {

   public static Closeable prepareSQLiteDatasource(File db) {
      HikariDataSource hds = makeSQLiteDataSource(db);
      initializeTxNone(hds);
      executeUpdate(
         "CREATE TABLE IF NOT EXISTS TargetClassSQL ("
            + "id integer PRIMARY KEY AUTOINCREMENT,"
            + "string text NOT NULL,"
            + "timestamp INTEGER"
            + ')');
      return hds; // to close it properly
   }

   @AfterClass
   public static void tearDown()
   {
      q2o.deinitialize();
   }

   @Test
   public void shouldPerformCRUD() throws IOException {
      try (Closeable ignored = prepareSQLiteDatasource(null)) {
         TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
         assertThat(original.getId()).isNull();
         TargetClassSQL inserted = Q2Obj.insert(original);
         assertThat(inserted).isSameAs(original).as("insertObject() sets generated id");
         Integer idAfterInsert = inserted.getId();
         assertThat(idAfterInsert).isNotNull();

         List<TargetClassSQL> selectedAll = Q2ObjList.fromClause(TargetClassSQL.class, null);
         assertThat(selectedAll).isNotEmpty();

         TargetClassSQL selected = Q2Obj.fromClause(TargetClassSQL.class, "string = ?", "Hi");
         assertThat(selected.getId()).isEqualTo(idAfterInsert);
         assertThat(selected.getString()).isEqualTo("Hi");
         assertThat(selected.getTimestamp().getTime()).isEqualTo(0);

         selected.setString("Hi edited");
         TargetClassSQL updated = Q2Obj.update(selected);
         assertThat(updated).isSameAs(selected).as("updateObject() only set generated id if it was missing");
         assertThat(updated.getId()).isEqualTo(idAfterInsert);
      }
   }

   @Test @Ignore
   public void shouldPerformCRUDAfterReconnect() throws IOException {

      File path = File.createTempFile("sansorm", ".db");
      path.deleteOnExit();

      Integer idAfterInsert;
      try (Closeable ignored = prepareSQLiteDatasource(path)) {
         TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
         assertThat(original.getId()).isNull();
         TargetClassSQL inserted = Q2Obj.insert(original);
         assertThat(inserted).isSameAs(original).as("insertObject() sets generated id");
         idAfterInsert = inserted.getId();
         assertThat(idAfterInsert).isNotNull();
      }

      // reopen database, it is important for this test
      // then select previously inserted object and try to edit it
      try (Closeable ignored = prepareSQLiteDatasource(path)) {
         TargetClassSQL selected = Q2Obj.fromClause(TargetClassSQL.class, "string = ?", "Hi");
         assertThat(selected.getId()).isEqualTo(idAfterInsert);
         assertThat(selected.getString()).isEqualTo("Hi");
         assertThat(selected.getTimestamp().getTime()).isEqualTo(0L);

         selected.setString("Hi edited");
         TargetClassSQL updated = Q2Obj.update(selected);
         assertThat(updated).isSameAs(selected).as("updateObject() only set generated id if it was missing");
         assertThat(updated.getId()).isEqualTo(idAfterInsert);
      }
   }

   @Test
   public void testInsertListNotBatched2() throws IOException {
      // given
      int count = 5;
      Set<TargetClassSQL> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClassSQL(String.valueOf(i), new Date(i)))
         .collect(Collectors.toSet());

      // when
      try (Closeable ignored = prepareSQLiteDatasource(null)) {
         SqlClosure.sqlExecute(c -> {
            Q2ObjList.insertNotBatched(c, toInsert);
            return null;
         });
      }

      // then
      Set<Integer> generatedIds = toInsert.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
      assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
      assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
   }

   @Test
   public void testInsertListBatched() throws IOException {
      // given
      int count = 5;
      String u = UUID.randomUUID().toString();
      Set<TargetClassSQL> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClassSQL(u + String.valueOf(i), new Date(i)))
         .collect(Collectors.toSet());

      // when
      try (Closeable ignored = prepareSQLiteDatasource(null)) {
         SqlClosure.sqlExecute(c -> {
            Q2ObjList.insertBatched(c, toInsert);
            return null;
         });
         List<TargetClassSQL> inserted = Q2ObjList.fromClause(
            TargetClassSQL.class,
            "string in " + Q2Sql.getInClausePlaceholdersForCount(count),
            IntStream.range(0, count).boxed().map(i -> u + String.valueOf(i)).collect(Collectors.toList()).toArray(new Object[]{}));

         // then
         Set<Integer> generatedIds = inserted.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
         assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
         assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
      }
   }
}
