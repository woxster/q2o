package com.zaxxer.q2o;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.DataSources;
import org.sansorm.testutils.Database;

import javax.persistence.*;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.sansorm.DataSourcesPrivate.getSybaseDataSource;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 11.01.20
 */
@RunWith(Parameterized.class)
public class LobTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Parameterized.Parameters(name = "autocommit={0}, userTx={1} database={2}")
   public static Collection<Object[]> data() {
      Object[][] params = {
         {true, true, Database.h2Server}, {true, true, Database.mysql}
//         {true, true, Database.mysql}
//         {true, true, Database.sybase}
//         {true, true, Database.h2Server}
//         {true, true, Database.sqlite} // java.sql.Blob not supported.
      };
      return Arrays.asList(params);
   }
   private DataSource dataSource;

   @Parameterized.Parameter(0)
   public boolean withAutoCommit;

   @Parameterized.Parameter(1)
   public boolean withUserTx;

   @Parameterized.Parameter(2)
   public Database database;

   @Before // not @BeforeClass to have fresh table in each test, also sde
   public void setUp() throws IOException, SQLException
   {
      switch (database) {
         case h2Server:
            dataSource = DataSources.getH2ServerDataSource(/*autoCommit=*/withAutoCommit);
            break;
         case mysql:
            dataSource = DataSources.getMySqlDataSource("q2o", "root", "yxcvbnm");
            break;
         case sqlite:
            dataSource = DataSources.getSqLiteDataSource(null);
            break;
         case sybase:
            dataSource = getSybaseDataSource();
            break;
      }

      if (withUserTx) {
         dataSource = q2o.initializeTxSimple(dataSource);
      }
      else {
         q2o.initializeTxNone(dataSource);
      }

      if (database == Database.mysql) {
         q2o.setMySqlMode(true);
      }

      dropTable();

      switch (database) {
         case h2Server:
         case sqlite:
            Q2Sql.executeUpdate(
               "create table MYLOB (ID IDENTITY, MYBLOB BLOB, MYCLOB CLOB)");
            break;
         case mysql:
            Q2Sql.executeUpdate(
               "create table MYLOB (" +
                  "ID INTEGER NOT NULL AUTO_INCREMENT" +
                  ", MYBLOB BLOB" +
                  ", MYCLOB TEXT" +
                  ", PRIMARY KEY (id)" +
                  ")");
            break;
         case sybase:
            // com.sybase.jdbc4.jdbc.SybSQLException: The 'CREATE TABLE' command is not allowed within a multi-statement transaction in the 'opixcntl' database.
            Connection con = dataSource.getConnection();
            Q2Sql.executeUpdate(
               con,
               "create table MYLOB (ID NUMERIC(8,0) IDENTITY, MYBLOB IMAGE NULL, MYCLOB TEXT NULL)");
            con.close();
            break;
      }
   }

   private void dropTable() throws SQLException
   {
      if (database != Database.sybase) {
         Q2Sql.executeUpdate("drop table if exists MYLOB");
      }
      else {
         // com.sybase.jdbc4.jdbc.SybSQLException: The 'DROP TABLE' command is not allowed within a multi-statement transaction in the 'opixcntl' database.
         Connection con = dataSource.getConnection();
         Q2Sql.executeUpdate(
            con,
            "if object_id('MYLOB') != null drop table MYLOB");
         con.close();
      }
   }

   @After
   public void tearDown() throws SQLException
   {
      dropTable();
      q2o.deinitialize();
   }

   @Entity
   @Table(name = "MYLOB")
   static class MyLob {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "ID")
      int id;
      @Column(name = "MYBLOB")
      Blob myBlob;
      @Column(name = "MYCLOB")
      Clob myClob;
   }

   @Test
   public void readBlob() throws SQLException, IOException
   {
      insertImageAsBytes();

      TransactionHelper.beginOrJoinTransaction();

      List<MyLob> myLobs = Q2ObjList.fromRawClause(MyLob.class, "where ID=1");
      // MySQL: no locator object but instead all data are already transfered over the net.
      // H2: locator object.
      long length = myLobs.get(0).myBlob.length();
      byte[] retrievedImg = myLobs.get(0).myBlob.getBytes(1, (int) length);

      TransactionHelper.commit();

      assertEquals(37032, length);
      assertEquals(37032, retrievedImg.length);
   }

   @Test
   public void blob() throws SQLException, IOException
   {
      int copiedBytes = 0;
      try {
         TransactionHelper.beginOrJoinTransaction();

         Connection con = dataSource.getConnection();
         Blob blob = con.createBlob();
         MyLob myLob = new MyLob();
         myLob.myBlob = blob;

         // Must be executed before the insert statement is executed or data will not be stored.
         if (database == Database.sybase) {
            // To circumvent Bug: SQLException: JZ037: Value of offset/position/start should be in the range [1, len] where len is length of Large Object[LOB].
            blob.setBytes(1, new byte[]{0});
         }
         // MySQL: Not a real Locator Object: Data are hold in-memory, what is not the aim.
         OutputStream outputStream = blob.setBinaryStream(1);
         File img = new File("src/test/resources/image.png");
         FileInputStream imgInputStream = new FileInputStream(img);
         copiedBytes = IOUtils.copy(imgInputStream, outputStream);
         outputStream.close();
         imgInputStream.close();

         Q2Obj.insert(myLob);

         TransactionHelper.commit();
      }
      catch (SQLException | IOException e) {
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(37032, copiedBytes);

      long length = 0;
      byte[] retrievedImg = new byte[0];
      try {
         TransactionHelper.beginOrJoinTransaction();

         MyLob myLobRetrieved = Q2Obj.byId(MyLob.class, 1);
         // MySQL: no locator object but instead all data are already transfered over the net.
         // H2: locator object.
         length = myLobRetrieved.myBlob.length();
         retrievedImg = myLobRetrieved.myBlob.getBytes(1, (int) length);

         TransactionHelper.commit();
      }
      catch (SQLException e) {
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(37032, length);
      assertEquals(37032, retrievedImg.length);

   }

   @Test
   public void blobRawJdbc() throws SQLException, IOException
   {
      int rows = 0;
      try {
         TransactionHelper.beginOrJoinTransaction();

         Connection con = dataSource.getConnection();
         Blob blob = con.createBlob();
         MyLob myLob = new MyLob();
         myLob.myBlob = blob;

         // Must be executed before the insert statement is executed or data will not be stored.
         if (database == Database.sybase) {
            // To circumvent Bug: SQLException: JZ037: Value of offset/position/start should be in the range [1, len] where len is length of Large Object[LOB].
            blob.setBytes(1, new byte[]{0});
         }
         // MySQL: Not a real Locator Object: Data are hold in-memory, what is not the aim.
         OutputStream outputStream = blob.setBinaryStream(1);
         File img = new File("src/test/resources/image.png");
         FileInputStream imgInputStream = new FileInputStream(img);
         int copiedBytes = IOUtils.copy(imgInputStream, outputStream);
         outputStream.close();
         imgInputStream.close();

         PreparedStatement stmnt = con.prepareStatement("insert into MYLOB (MYBLOB) values (?)");
         stmnt.setBlob(1, blob);
         rows = stmnt.executeUpdate();

         TransactionHelper.commit();
      }
      catch (SQLException | IOException e) {
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(1, rows);

      long length = 0;
      try {
         TransactionHelper.beginOrJoinTransaction();

         ResultSet rs;
         if (database == Database.mysql) {
            // select ID, sonst in MySQL: SQLException: Emulated BLOB locators must come from a ResultSet with only one table selected, and all primary keys selected
            // "'MYBLOB' MYBLOB": "you must use a column alias with the value of the column to the actual name of the Blob", connector-j-8.0-en.a4.pdf, 35.
            rs = Q2Sql.executeQuery("select ID, 'MYBLOB' MYBLOB from MYLOB where ID = 1");
   //         rs = Q2Sql.executeQuery("SELECT MyLob.id,'myblob' myblob,MyLob.myClob FROM MyLob MyLob WHERE  id=1");
         }
         else {
            rs = Q2Sql.executeQuery("select MYBLOB from MYLOB where ID = 1");
         }
         rs.next();
         // MySQL without emulateLocators=true: not a real locator object but instead all data are already transfered over the net. With emulateLocators=true a locator object.
         // H2: locator object.
         Blob blobRetrieved = rs.getBlob("MYBLOB");
         // MySQL with "... MYBLOB myBlobAliasName ..." instead of "... 'MYBLOB' myBlobAliasName ..." and emulateLocators=true: "SQLException: Not a valid escape sequence".
         // MySQL Blob is only valid until ResultSet is closed.
         length = blobRetrieved.length();

         rs.close();

         TransactionHelper.commit();
      }
      catch (SQLException e) {
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(37032, length);
   }

   private void insertImageAsBytes() throws SQLException, IOException
   {
      File img = new File("src/test/resources/image.png");
      FileInputStream imgInputStream = new FileInputStream(img);

      Connection con = dataSource.getConnection();
      PreparedStatement ps = con.prepareStatement("insert into MYLOB (MYBLOB) values (?)");
      // SQLite: java.sql.SQLFeatureNotSupportedException
//      ps.setBinaryStream(1, imgInputStream);
      ps.setBytes(1, IOUtils.toByteArray(imgInputStream));
      ps.executeUpdate();
      ps.close();
      con.close();

      imgInputStream.close();
   }
}
