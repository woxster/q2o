package org.sansorm.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.q2o.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sansorm.TestUtils;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class VariousTest extends GeneralTestConfigurator {

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      dataSource = TestUtils.getHikariDataSource(true, dataSource);
      if (withSpringTx) {
         q2o.initializeWithSpringTxSupport(dataSource);
      }
      else {
         q2o.initializeTxNone(dataSource);
      }
   }

   @Override
   @After
   public void tearDown() throws Exception {
      Q2Sql.executeUpdate("drop table TargetClassSQL");
      super.tearDown();
      ((HikariDataSource)dataSource).close();
   }

   public HikariDataSource createTables(File db) {
      if (database == Database.h2) {
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS TargetClassSQL ("
               + "id integer NOT NULL IDENTITY PRIMARY KEY,"
               + "string text NOT NULL,"
               + "timestamp INTEGER"
               + ')');
      }
      else if (database == Database.mysql){
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS TargetClassSQL ("
               + "id integer PRIMARY KEY AUTO_INCREMENT,"
               + "string text NOT NULL,"
               + "timestamp INTEGER"
               + ')');
      }
      else if (database == Database.sqlite) {
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS TargetClassSQL ("
               + "id integer PRIMARY KEY AUTOINCREMENT,"
               + "string text NOT NULL,"
               + "timestamp INTEGER"
               + ')');
      }
      return (HikariDataSource)dataSource;
   }

   @Test
   public void shouldPerformCRUD() throws IOException {
      createTables(null);
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

   @Test @Ignore
   public void shouldPerformCRUDAfterReconnect() throws IOException {

      File path = File.createTempFile("sansorm", ".db");
      path.deleteOnExit();

      Integer idAfterInsert;
      createTables(path);
      TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
      assertThat(original.getId()).isNull();
      TargetClassSQL inserted = Q2Obj.insert(original);
      assertThat(inserted).isSameAs(original).as("insertObject() sets generated id");
      idAfterInsert = inserted.getId();
      assertThat(idAfterInsert).isNotNull();

      // reopen database, it is important for this test
      // then select previously inserted object and try to edit it
      try (Closeable ignored = createTables(path)) {
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

      createTables(null);
      // when
      SqlClosure.sqlExecute(c -> {
         Q2ObjList.insertNotBatched(c, toInsert);
         return null;
      });

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

      createTables(null);
      // when
      Closeable ignored = createTables(null);
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
