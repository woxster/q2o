package org.sansorm;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.q2o;
import org.assertj.core.api.Assertions;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Store and retrieve NULL values not primitives as in {@link MySQLDataTypesTest}.
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-27
 */
public class MySQLDataTypesNullTest {

   private static DataSource dataSource;
   private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");;
   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @BeforeClass
   public static void beforeClass() throws SQLException {
//      conn =
//         DriverManager.getConnection("jdbc:mysql://localhost/q2o?" +
//            "user=root&password=opixcntl&generateSimpleParameterMetadata=true");
//      stmnt = conn.createStatement();
//      MysqlDataSource dataSource = new MysqlDataSource();
//      dataSource.setServerName("localhost");
//      dataSource.setDatabaseName("q2o");
//      dataSource.setUser("root");
//      dataSource.setPassword("opixcntl");
//      dataSource.setGenerateSimpleParameterMetadata(true);
//      MySQLTest.dataSource = dataSource;

      dataSource = DataSources.makeMySqlDataSource("q2o", "root", "yxcvbnm");
      q2o.initializeTxNone(dataSource);
      q2o.setMySqlMode(true);

      Q2Sql.executeUpdate(
         "CREATE TABLE DataTypes ("
            + " id INTEGER NOT NULL AUTO_INCREMENT"

            + ", myInteger INTEGER"

            + ", dateToDate DATE"
            + ", sqlDateToDate DATE"
            + ", timestampToDate DATE"

            + ", dateToDateTime DATETIME"
            + ", sqlDateToDateTime DATETIME"
            + ", timeToDateTime DATETIME"
            + ", timestampToDateTime DATETIME"

            + ", dateToTimestamp TIMESTAMP"
            + ", sqlDateToTimestamp TIMESTAMP"
            + ", timestampToTimestamp TIMESTAMP"

            + ", intToYear YEAR"
            + ", sqlDateToYear YEAR"
            + ", stringToYear YEAR"

            // MySQL 8: java.sql.SQLException: Supports only YEAR or YEAR(4) column.
//            + ", intToYear2 YEAR(2)"
//            + ", sqlDateToYear2 YEAR(2)"
//            + ", stringToYear2 YEAR(2)"

            + ", intToTime TIME"
            + ", stringToTime TIME"
            + ", timeToTime TIME"
            + ", timestampToTime TIME"

            + ", stringToChar4 CHAR(4)"

            + ", stringToVarChar4 VARCHAR(4)"

            + ", stringToBinary BINARY(4)"

            + ", stringToVarBinary VARBINARY(4)"
            + ", byteArrayToBinary VARBINARY(4)"
            + ", byteArrayToVarBinary VARBINARY(4)"

            + ", byteToBit8 bit(8)"
            + ", shortToBit16 bit(16)"
            + ", intToBit32 bit(32)"
            + ", longToBit64 bit(64)"
            + ", stringToBit8 bit(8)"
            + ", byteArrayToBit64 bit(64)"

            + ", byteToTinyint TINYINT"
            + ", shortToTinyint TINYINT"
            + ", intToTinyint TINYINT"
            + ", longToTinyint TINYINT"

            + ", byteToSmallint SMALLINT"
            + ", shortToSmallint SMALLINT"
            + ", intToSmallint SMALLINT"
            + ", longToSmallint SMALLINT"

            + ", intToBigint BIGINT"
            + ", longToBigint BIGINT"
            + ", bigintToBigint BIGINT"

            + ", intToInt INT"
            + ", integerToInt INT"
            + ", enumToInt INT"

            + ", intToMediumint MEDIUMINT"

            + ", longToUnsignedInt INT UNSIGNED"

            + ", enumToEnumTypeString ENUM('one', 'two', 'three')"
            + ", enumToEnumTypeOrdinal ENUM('one', 'two', 'three')"

            + ", PRIMARY KEY (id)"
            + " )");
   }

   @AfterClass
   public static void afterClass() throws SQLException {
      try {
         Q2Sql.executeUpdate("drop table DataTypes");
      }
      finally {
         q2o.deinitialize();
      }
   }

   @After
   public void tearDown() throws Exception {
      Q2Sql.executeUpdate("delete from DataTypes");
   }

   public static class DataTypes {
      @Id @GeneratedValue
      int id;

      Integer myInteger;

      Date dateToDate;
      java.sql.Date sqlDateToDate;
      Timestamp timestampToDate;

      Date dateToDateTime;
      java.sql.Date sqlDateToDateTime;
      Time timeToDateTime;
      Timestamp timestampToDateTime;

      Date dateToTimestamp;
      Timestamp timestampToTimestamp;
      java.sql.Date sqlDateToTimestamp;

      Integer intToYear;
      java.sql.Date sqlDateToYear;
      String stringToYear;

      // YEAR(2): No longer supported by MySQL 8
//      Integer intToYear2;
//      java.sql.Date sqlDateToYear2;
//      String stringToYear2;

      Integer intToTime;
      String stringToTime;
      Time timeToTime;
      Timestamp timestampToTime;

      String stringToChar4;
      String stringToVarChar4;
      String stringToBinary;
      String stringToVarBinary;
      byte[] byteArrayToBinary;
      byte[] byteArrayToVarBinary ;

      Byte byteToBit8;
      Short shortToBit16;
      Integer intToBit32;
      Long longToBit64;
      String stringToBit8;
      byte[] byteArrayToBit64;

      Byte byteToTinyint;
      Short shortToTinyint;
      Integer intToTinyint;
      Long longToTinyint;

      Byte byteToSmallint;
      Short shortToSmallint;
      Integer intToSmallint;
      Long longToSmallint;

      Integer intToBigint;
      Long longToBigint;
      BigInteger bigintToBigint;

      Integer intToInt;
      Integer integerToInt;
      Integer intToMediumint;
      Long longToUnsignedInt;

      // CLARIFY Mimic Hibernate? "Hibernate Annotations support out of the box enum type mapping ... the persistence representation, defaulted to ordinal" (Mapping with JPA (Java Persistence Annotations).pdf, "2.2.2.1. Declaring basic property mappings")
      @Enumerated(EnumType.STRING)
      CaseMatched enumToEnumTypeString;

      @Enumerated(EnumType.ORDINAL)
      CaseMatched enumToEnumTypeOrdinal;

      @Enumerated(EnumType.ORDINAL)
      CaseMatched enumToInt;

      public enum CaseMatched {
         one, two, three
      }
   }

   @Test
   public void insertInteger() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.myInteger = 1;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.myInteger, dataTypes1.myInteger);
   }

   /**
    * Surprising.
    * <pre>
    * +----+-----------+------------+
    * | id | myInteger | dateToDate |
    * +----+-----------+------------+
    * |  1 |         0 | 2019-04-01 |
    * +----+-----------+------------+
    * </pre>
    */
   @Test
   public void dateToDATE() throws ParseException {

      DataTypes dataTypes = new DataTypes();
      dataTypes.dateToDate = formatter.parse("2019-04-01 23:59:59.999");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019-04-01 02:00:00.000", formatter.format(dataTypes1.dateToDate));
      assertEquals(dataTypes.dateToDate.getClass(), dataTypes1.dateToDate.getClass());
   }

   /**
    * <pre>
    * +----+-----------+------------+---------------------+
    * | id | myInteger | dateToDate | dateToDateTime      |
    * +----+-----------+------------+---------------------+
    * |  1 |         0 | NULL       | 2019-04-01 21:30:31 |
    * +----+-----------+------------+---------------------+
    * </pre>
    */
   @Test
   public void dateToDATETIME() throws ParseException {

      DataTypes dataTypes = new DataTypes();

      String dateFormatted = "2019-04-01 23:30:30.555";
      dataTypes.dateToDateTime = formatter.parse(dateFormatted); // local time

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);

      assertEquals("2019-04-01 23:30:31.000", formatter.format(dataTypes1.dateToDateTime));
      assertEquals(dataTypes.dateToDateTime.getClass(), dataTypes1.dateToDateTime.getClass());

   }

   /**
    * <pre>
    * +---------------------+
    * | timestampToDateTime |
    * +---------------------+
    * | 1969-12-31 23:00:01 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void timestampToDATETIME() throws ParseException {

      DataTypes dataTypes = new DataTypes();

      dataTypes.timestampToDateTime = Timestamp.valueOf("1970-01-01 00:00:00.999999999");

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);

      assertEquals("1970-01-01 00:00:01.0", dataTypes1.timestampToDateTime.toString());
      assertEquals(dataTypes.timestampToDateTime.getClass(), dataTypes1.timestampToDateTime.getClass());

   }

   /**
    * <pre>
    * +---------------------+
    * | timeToDateTime      |
    * +---------------------+
    * | 2010-10-11 00:00:00 |
    * +---------------------+
    * </pre>
    * <p>
    *     "MySQL converts a time value to a date or date-and-time value by parsing the string value of the time as a date or date-and-time. This is unlikely to be useful. For example, '23:12:31' interpreted as a date becomes '2032-12-31'. Time values not valid as dates become '0000-00-00' or NULL." (MySQL 5.5 Reference Manual, 10.3.5. Conversion Between Date and Time Types).
    * </p>
    */
   @Test
   public void timeToDATETIME() throws ParseException {

      DataTypes dataTypes = new DataTypes();

      dataTypes.timeToDateTime = Time.valueOf("11:10:11");

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);

      assertEquals("02:00:00", dataTypes1.timeToDateTime.toString());
      assertEquals(dataTypes.timeToDateTime.getClass(), dataTypes1.timeToDateTime.getClass());

   }

   /**
    * <pre>
    * mysql> mysql> select * from DataTypes;
    * +----+-----------+------------+----------------+---------------------+
    * | id | myInteger | dateToDate | dateToDateTime | dateToTimestamp     |
    * +----+-----------+------------+----------------+---------------------+
    * |  1 |         0 | NULL       | NULL           | 2019-04-01 22:00:00 |
    * +----+-----------+------------+----------------+---------------------+
    * </pre>
    */
   @Test
   public void dateToTIMESTAMP() throws ParseException {
      DataTypes dataTypes = new DataTypes();
      dataTypes.dateToTimestamp = formatter.parse("2019-04-01 21:59:59.999");

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019-04-01 22:00:00.000", formatter.format(dataTypes1.dateToTimestamp));
      assertEquals(dataTypes.dateToTimestamp.getClass(), dataTypes1.dateToTimestamp.getClass());
   }

   /**
    * <pre>
    * mysql> select * from DataTypes;
    * +----+-----------+----------------------+
    * | id | myInteger | timestampToTimestamp |
    * +----+-----------+----------------------+
    * |  1 |         0 | 2019-04-01 19:51:00  |
    * +----+-----------+----------------------+
    * </pre>
    * <p>
    *     "A DATETIME or TIMESTAMP value can include a trailing fractional seconds part in up to microseconds (6 digits) precision. Although this fractional part is recognized, it is discarded from values stored into DATETIME or TIMESTAMP columns." (MySQL 5.5 Reference Manual, 10.3.1. The DATE, DATETIME, and TIMESTAMP Types)
    * </p>
    */
   @Test
   public void timestampToTIMESTAMP() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.timestampToTimestamp = Timestamp.valueOf("2019-04-01 21:50:59.999");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019-04-01 21:51:00.0", dataTypes1.timestampToTimestamp.toString());
      assertEquals(dataTypes.timestampToTimestamp.getClass(), dataTypes1.timestampToTimestamp.getClass());
   }

   /**
    * Surprising.
    * <pre>
    * +---------------+
    * | sqlDateToDate |
    * +---------------+
    * | 2019-03-31    |
    * +---------------+
    * </pre>
    */
   @Test
   public void sqlDateToDATE() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.sqlDateToDate = java.sql.Date.valueOf("2019-04-01");
      assertEquals("2019-04-01 00:00:00.000", formatter.format(dataTypes.sqlDateToDate));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019-03-31 01:00:00.000", formatter.format(dataTypes1.sqlDateToDate));
      assertEquals(dataTypes.sqlDateToDate.getClass(), dataTypes1.sqlDateToDate.getClass());
   }

   /**
    * <pre>
    * +---------------------+
    * | sqlDateToDateTime   |
    * +---------------------+
    * | 2019-03-31 00:00:00 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void sqlDateToDATETIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.sqlDateToDateTime = java.sql.Date.valueOf("2019-04-01");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019-03-31 01:00:00.000", formatter.format(dataTypes1.sqlDateToDateTime));
      assertEquals(dataTypes.sqlDateToDateTime.getClass(), dataTypes1.sqlDateToDateTime.getClass());
   }

   /**
    * <pre>
    * +---------------------+
    * | sqlDateToTimestamp  |
    * +---------------------+
    * | 2019-03-31 00:00:00 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void sqlDateToTIMESTAMP() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.sqlDateToTimestamp = java.sql.Date.valueOf("2019-04-01");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019-03-31 01:00:00.000", formatter.format(dataTypes1.sqlDateToTimestamp));
      assertEquals(dataTypes.sqlDateToTimestamp.getClass(), dataTypes1.sqlDateToTimestamp.getClass());
   }

   /**
    * <pre>
    * +-----------+
    * | intToYear |
    * +-----------+
    * |      2019 |
    * +-----------+
    * </pre>
    */
   @Test
   public void intToYEAR() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToYear = 2019;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(2019, Optional.ofNullable(dataTypes1.intToYear).orElse(0).intValue());
   }

//   @Test
//   public void intToYEAR2() {
//      DataTypes dataTypes = new DataTypes();
//      dataTypes.intToYear2 = 19;
//      Q2Obj.insert(dataTypes);
//      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
//      assertEquals(19, Optional.ofNullable(dataTypes1.intToYear2).orElse(0).intValue());
//   }

   /**
    * With intToYear of type int there gets 0000 stored when intToYear has not been set but when the object is retrieved from database its intToYear is 2000. With intToYear of type Integer it behaves as expected.
    * <pre>
    * +-----------+
    * | intToYear |
    * +-----------+
    * |      0000 |
    * +-----------+
    * </pre>
    */
   @Test
   public void intToYEARNotSet() {
      DataTypes dataTypes = new DataTypes();
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertNull(dataTypes1.intToYear);
   }

   /**
    * SQLException: Data truncated for column 'sqlDateToYear'
    */
   @Test @Ignore
   public void sqlDateToYEAR() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.sqlDateToYear = java.sql.Date.valueOf("2019-04-01");
      thrown.expectMessage("SQLException: Data truncated for column 'sqlDateToYear'");
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void stringToYEAR4Digits() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToYear = "2019";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019", dataTypes1.stringToYear);
   }

   /**
    * <pre>
    * +--------------+
    * | stringToYear |
    * +--------------+
    * |         2019 |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToYEAR2Digits() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToYear = "19";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("2019", dataTypes1.stringToYear);
   }

   // com.mysql.cj.jdbc.exceptions.MysqlDataTruncation: Data truncation: Incorrect time value: '36000000' for column 'intToTime'
   // Execution in mysql client leads to the same error: ERROR 1292 (22007): Incorrect time value: '36000' for column 'intToTime' at row 1
   @Test @Ignore
   public void intToTIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToTime = 10 * 60 * 60;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.intToTime, dataTypes1.intToTime);
   }

   /**
    * <pre>
    * +--------------+
    * | stringToTime |
    * +--------------+
    * | 10:59:59     |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToTIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToTime = "10:59:59";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      // CLARIFY
      assertEquals("11:59:59", dataTypes1.stringToTime);
      assertNull(dataTypes1.intToTime);
   }

   /**
    * Format is changed.
    * <pre>
    * +--------------+
    * | stringToTime |
    * +--------------+
    * | 10:59:59     |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToTIME2() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToTime = "105959";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      // CLARIFY
      assertEquals("11:59:59", dataTypes1.stringToTime);
      assertNull(dataTypes1.intToTime);
   }

   @Test
   public void timeToTIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.timeToTime = Time.valueOf("11:48:22");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.timeToTime.getTime(), dataTypes1.timeToTime.getTime());
   }

   /**
    * <pre>
    * +-----------------+
    * | timestampToTime |
    * +-----------------+
    * | 23:00:00        |
    * +-----------------+
    * </pre>
    */
   @Test
   public void timestampToTIME() {
      DataTypes dataTypes = new DataTypes();
//      dataTypes.timestampToTime = new Timestamp(1555138024405L);
      dataTypes.timestampToTime = Timestamp.valueOf("1970-1-1 21:59:59.999999999");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("1970-01-01 22:00:00.0", dataTypes1.timestampToTime.toString());
   }

   /**
    * <pre>
    * +-----------------+
    * | timestampToDate |
    * +-----------------+
    * | 1970-01-01      |
    * +-----------------+
    * </pre>
    */
   @Test
   public void timestampToDATE() {
      DataTypes dataTypes = new DataTypes();
//      dataTypes.timestampToTime = new Timestamp(1555138024405L);
      dataTypes.timestampToDate = Timestamp.valueOf("1970-1-1 21:59:59.999999999");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals("1970-01-01 01:00:00.0", dataTypes1.timestampToDate.toString());
   }

   @Test
   public void stringToCHAR4() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToChar4 = "1234";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.stringToChar4, dataTypes1.stringToChar4);
   }

   @Test
   public void stringToVARCHAR4() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToVarChar4 = "1234";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.stringToVarChar4, dataTypes1.stringToVarChar4);
   }

   @Test
   public void stringToBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToBinary = "1234";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.stringToBinary, dataTypes1.stringToBinary);
   }

   @Test
   public void stringToVARBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToVarBinary = "1234";
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.stringToVarBinary, dataTypes1.stringToVarBinary);
   }

   @Test
   public void byteArrayToVARBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.byteArrayToVarBinary = "1234".getBytes();
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertArrayEquals(dataTypes.byteArrayToVarBinary, dataTypes1.byteArrayToVarBinary);
   }

   @Test
   public void byteArrayToBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.byteArrayToBinary = "1".getBytes();
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertArrayEquals(dataTypes.byteArrayToBinary, dataTypes1.byteArrayToBinary);
   }

   @Test
   public void byteToBIT8() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.byteToBit8 = 0b00001000;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.byteToBit8, dataTypes1.byteToBit8);
   }

   @Test @Ignore
   public void byteToBIT8Negative() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.byteToBit8 = -1;
      thrown.expectMessage("MysqlDataTruncation: Data truncation: Data too long for column 'byteToBit8'");
      Q2Obj.insert(dataTypes);
   }

   /**
    * <p>
    * Byte array length must match BIT field size: "If you assign a value to a BIT(M) column that is less than M bits long, the value is padded on the left with zeros." (MySQL 5.5 Reference Manual, 10.2.4. Bit-Value Type). This will mix up indices order as demonstrated here.
    * </p>
    */
   @Test
   public void byteArrayToBIT64() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.byteArrayToBit64 = new byte[]{
         (byte)1, (byte)2, (byte)3, (byte)4,
      };
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      byte[] expected = new byte[]{
            (byte)0,
            (byte)0,
            (byte)0,
            (byte)0,
            (byte)1,
            (byte)2,
            (byte)3,
            (byte)4
         };
      Assertions.assertThat(dataTypes1.byteArrayToBit64).containsExactly(expected);
   }

   @Test @Ignore
   public void stringToBIT8() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.stringToBit8 = "b'11'";
      thrown.expectMessage("MysqlDataTruncation: Data truncation: Data too long for column 'stringToBit8'");
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void shortToBIT16() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.shortToBit16 = Short.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.shortToBit16, dataTypes1.shortToBit16);
   }

   @Test
   public void intToBIT32() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToBit32 = Integer.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.intToBit32, dataTypes1.intToBit32);
   }

   @Test
   public void longToBIT64() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.longToBit64 = Long.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.longToBit64, dataTypes1.longToBit64);
   }

   @Test
   public void byteToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.byteToTinyint = Byte.MIN_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.byteToTinyint, dataTypes1.byteToTinyint);
   }

   @Test
   public void shortToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.shortToTinyint = 127;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.shortToTinyint, dataTypes1.shortToTinyint);
   }

   @Test
   public void intToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToTinyint = 127;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.intToTinyint, dataTypes1.intToTinyint);
   }

   @Test
   public void longToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.longToTinyint = 127L;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.longToTinyint, dataTypes1.longToTinyint);
   }

   @Test
   public void byteToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.byteToSmallint = Byte.MIN_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.byteToSmallint, dataTypes1.byteToSmallint);
   }

   @Test
   public void shortToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.shortToSmallint = Short.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.shortToSmallint, dataTypes1.shortToSmallint);
   }

   @Test
   public void intToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToSmallint = (int)Short.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.intToSmallint, dataTypes1.intToSmallint);
   }

   @Test
   public void longToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.longToSmallint = (long)Short.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.longToSmallint, dataTypes1.longToSmallint);
   }

   @Test
   public void bigintToBIGINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.bigintToBigint = BigInteger.valueOf(Long.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.bigintToBigint, dataTypes1.bigintToBigint);
   }

   @Test
   public void longToBIGINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.longToBigint = Long.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.longToBigint, dataTypes1.longToBigint);
   }

   @Test
   public void intToBIGINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToBigint = Integer.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.intToBigint, dataTypes1.intToBigint);
   }

   @Test
   public void intToINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToInt = Integer.MAX_VALUE;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.intToInt, dataTypes1.intToInt);
   }

   @Test
   public void integerToINTNull() {
      DataTypes dataTypes = new DataTypes();
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.integerToInt, dataTypes1.integerToInt);
   }

   @Test
   public void intToMEDIUMINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.intToMediumint = -8388608;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.intToMediumint, dataTypes1.intToMediumint);
   }

   @Test
   public void longToINT_UNSIGNED() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.longToUnsignedInt = ((long)Integer.MAX_VALUE * 2) + 1;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.longToUnsignedInt, dataTypes1.longToUnsignedInt);
   }

   @Test
   public void enumToEnumTypeString() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.enumToEnumTypeString = DataTypes.CaseMatched.one;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.enumToEnumTypeString, dataTypes1.enumToEnumTypeString);
   }

   @Test
   public void enumToEnumTypeOrdinal() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.enumToEnumTypeOrdinal = DataTypes.CaseMatched.one;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.enumToEnumTypeOrdinal, dataTypes1.enumToEnumTypeOrdinal);
   }

   @Test
   public void enumToInt() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.enumToInt = DataTypes.CaseMatched.one;
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
      assertEquals(dataTypes.enumToInt, dataTypes1.enumToInt);
   }

   /**
    * <pre>
    * +----------------------+
    * | enumToEnumTypeString |
    * +----------------------+
    * | one                  |
    * +----------------------+
    * </pre>
    */
   @Test
   public void enumToEnumTypeStringLettercase() {
      Q2Sql.executeUpdate("insert into datatypes (enumToEnumTypeString) values('OnE')");
      DataTypes dataTypes = Q2Obj.fromClause(DataTypes.class, null);
      assertNotNull(dataTypes.enumToEnumTypeString);
   }

   /*
com.mysql.cj.jdbc.exceptions.MysqlDataTruncation: Data truncation: Incorrect date value: '1553676904544' for column 'dateToDate' at row 1
    */
//   @Test
//   public void insertDateAsMillis() {
//      long ms = new Date().getTime();
//      Q2Sql.executeUpdate("insert into DateToDATE (dateToDate) values(?)", ms);
//   }

   @Test @Ignore
   public void insertDateAsString() {
      Q2Sql.executeUpdate("insert into TestDate (myDate) values(?)", "2019-01-30");
   }

   @Test @Ignore
   public void insertDateAsString2() {
      SimpleDateFormat dateFormat = new SimpleDateFormat();
      dateFormat.applyPattern("yyyy-MM-dd");
      Q2Sql.executeUpdate("insert into TestDate (myDate) values(?)", dateFormat.format(new Date()));
   }

   @Test @Ignore
   public void isMySQL() {
      assertTrue(dataSource instanceof MysqlDataSource);
   }

   /*
com.mysql.cj.jdbc.exceptions.MysqlDataTruncation: Data truncation: Incorrect datetime value: '65895568473997' for column 'myTimestamp' at row 1
    */
//   @Test
//   public void insertTimestamp() {
//      Q2Sql.executeUpdate("insert into TestTimestamp (myTimestamp) values(?)", System.currentTimeMillis());
//   }

   @Test @Ignore
   public void insertTimestampAsString() {
//      SimpleDateFormat dateFormat = new SimpleDateFormat();
//      dateFormat.applyPattern("YYYY-MM-dd HH:mm:ss.SSS");
//      String formatted = dateFormat.format(new Date());
//      System.out.println(formatted);

      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn");
      LocalDateTime localDateTime = LocalDateTime.now();
      String formatted = localDateTime.format(dateTimeFormatter);

      System.out.println(formatted);

      Q2Sql.executeUpdate("insert into TestTimestamp (myTimestamp) values(?)", formatted);
   }

   @Test @Ignore
   public void insertTimestamp() {
      Q2Sql.executeUpdate("insert into TestTimestamp (myTimestamp) values(?)", new Timestamp(System.nanoTime()));
   }
}
