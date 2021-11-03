package org.sansorm.testutils;

import com.zaxxer.q2o.Q2Sql;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.01.20
 */
public class TableCreatorH2 {
   public static void dropTables()
   {
      Q2Sql.executeUpdate("DROP TABLE IF EXISTS FAR_RIGHT1_TABLE");
      Q2Sql.executeUpdate("DROP TABLE IF EXISTS RIGHT1_TABLE");
      Q2Sql.executeUpdate("DROP TABLE IF EXISTS MIDDLE1_TABLE");
      Q2Sql.executeUpdate("DROP TABLE IF EXISTS LEFT1_TABLE");
   }

   public static void createTables()
   {
      Q2Sql.executeUpdate(
         " CREATE TABLE FAR_RIGHT1_TABLE ("
            + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
            + ", type VARCHAR(128)"
            + ")");

      Q2Sql.executeUpdate(
         " CREATE TABLE RIGHT1_TABLE ("
            + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
            + ", type VARCHAR(128)"
            + ", farRightId INTEGER"
            + ", CONSTRAINT RIGHT1_TABLE_cnst1 FOREIGN KEY(farRightId) REFERENCES FAR_RIGHT1_TABLE (id)"
            + ")");

      Q2Sql.executeUpdate(
         " CREATE TABLE MIDDLE1_TABLE ("
            + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
            + ", type VARCHAR(128)"
            + ", rightId INTEGER"
            + ", CONSTRAINT MIDDLE1_TABLE_cnst1 FOREIGN KEY(rightId) REFERENCES RIGHT1_TABLE (id)"
            + ")");

      Q2Sql.executeUpdate(
         "CREATE TABLE LEFT1_TABLE ("
            + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
            + ", type VARCHAR(128)"
            + ", middleId INTEGER"
            + ", CONSTRAINT LEFT1_TABLE_cnst1 FOREIGN KEY(middleId) REFERENCES MIDDLE1_TABLE (id)"
            + ")");
   }

   public static void truncateTables()
   {
      Q2Sql.executeUpdate("truncate table LEFT1_TABLE");
      Q2Sql.executeUpdate("truncate table MIDDLE1_TABLE");
      Q2Sql.executeUpdate("truncate table RIGHT1_TABLE");
      Q2Sql.executeUpdate("truncate table FAR_RIGHT1_TABLE");
   }
}
