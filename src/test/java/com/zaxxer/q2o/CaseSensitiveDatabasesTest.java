package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.CaseSensitiveDatabasesClass;
import com.zaxxer.q2o.entities.InsertObjectH2;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.sansorm.TestUtils;
import org.sansorm.testutils.*;

import javax.persistence.*;
import java.sql.*;
import java.util.HashSet;

import static com.zaxxer.q2o.Q2Obj.countFromClause;
import static org.junit.Assert.*;
import static org.sansorm.TestUtils.makeH2DataSource;

/**
 * See Issue #22: <a href="https://github.com/brettwooldridge/SansOrm/issues/22">Problem with upper case column names</a>
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.03.18
 */
public class CaseSensitiveDatabasesTest {

   @After
   public void tearDown()
   {
      q2o.deinitialize();
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();


   @Test
   public void getColumnsCsv() {
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      String cols = OrmReader.getColumnsCsv(TestClass.class);
      // Preserve field order!!!
      assertEquals("TestClass.\"Delimited Field Name\",TestClass.Default_Case", cols);
   }

   @Test
   public void getColumnsCsvTableName() {
      class TestClass {
         @Column(name = "\"Delimited Field Name\"", table = "\"Delimited table name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case", table = "Default_Case_Table")
         String defaultCase;
      }
      String cols = OrmReader.getColumnsCsv(TestClass.class);
      // Preserve field order!!!
      assertEquals(cols, "\"Delimited table name\".\"Delimited Field Name\",Default_Case_Table.Default_Case");
   }

   @Test
   public void getColumnsCsvExclude() {
      String cols = Q2Sql.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "Delimited Field Name");
      // Preserve field order!!!
      assertEquals("Test_Class.Id,Test_Class.Default_Case", cols);
      cols = Q2Sql.getColumnsCsvExclude(CaseSensitiveDatabasesClass.class, "Default_Case");
      // Preserve field order!!!
      assertEquals("Test_Class.Id,Test_Class.\"Delimited Field Name\"", cols);
   }

   @Test
   public void getColumnsCsvExcludeWithTableName() {
      @Table(name = "TEST")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"", table = "Default_Table_Name")
         String delimitedFieldName;
         @Column(name = "Default_Case", table = "\"DELIMITED_TABLE_NAME\"")
         String defaultCase;
         @Column
         String excluded;
      }
      String cols = Q2Sql.getColumnsCsvExclude(TestClass.class, "excluded");
      // Preserve field order!!!
      assertEquals("Default_Table_Name.\"Delimited Field Name\",\"DELIMITED_TABLE_NAME\".Default_Case", cols);
   }

   @Test
   public void insertObject() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql, String[] columnNames) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }

               @Override
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     private boolean next = true;
                     @Override
                     public boolean next() {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return "auto-generated id";
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.insertObject(con, new TestClass());
      assertEquals("INSERT INTO Test_Class(\"Delimited Field Name\",Default_Case) VALUES (?,?)", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
   }

   @Test
   public void insertObjectGeneratedValue() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue @Column(name = "\"Id\"")
         String id;
         @Column(name = "\"Delimited field name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql, String[] columnNames) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }

               @Override
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     private boolean next = true;
                     @Override
                     public boolean next() {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return "123";
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.insertObject(con, new TestClass());
      assertEquals("INSERT INTO Test_Class(\"Delimited field name\",Default_Case) VALUES (?,?)", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
      assertEquals("123", obj.id);
   }

   @Test
   public void objectById() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default_case value";
      String idValue = "Id value";
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public ResultSet executeQuery() {
                  return new DummyResultSet() {
                     private boolean next = true;
                     @Override
                     public boolean next() {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }

                     @Override
                     public ResultSetMetaData getMetaData() {
                        return new DummyResultSetMetaData() {
                           @Override
                           public int getColumnCount() {
                              return 3;
                           }

                           @Override
                           public String getColumnName(int column) {
                              return   column == 1 ? "Delimited Field Name" :
                                       column == 2 ? "default_case" :
                                       column == 3 ? "id"
                                                   : null;
                           }

                           @Override
                           public String getTableName(final int column) {
                              return "Test_Class";
                           }
                        };
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return   columnIndex == 1 ? delimitedFieldValue :
                                 columnIndex == 2 ? defaultCaseValue :
                                 columnIndex == 3 ? idValue
                                                  : null;
                     }
                  };
               }
            };
         }
      };
      CaseSensitiveDatabasesClass obj = OrmReader.objectById(con, CaseSensitiveDatabasesClass.class, "xyz");
      // Preserve field order!!!
      assertEquals("SELECT Test_Class.Id,Test_Class.\"Delimited Field Name\",Test_Class.Default_Case FROM Test_Class Test_Class WHERE  Id=?", fetchedSql[0]);
      assertEquals(idValue, obj.getId());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
   }

   @Test
   public void updateObject() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.updateObject(con, new TestClass());
      assertEquals("UPDATE Test_Class SET \"Delimited Field Name\"=?,Default_Case=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
   }

   @Test
   public void updateObjectGeneratedId() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }

               @Override
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     private boolean next = true;
                     @Override
                     public boolean next() {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return "123";
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.updateObject(con, new TestClass());
      assertEquals("UPDATE Test_Class SET \"Delimited Field Name\"=?,Default_Case=? WHERE id=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
   }

   @Test
   public void updateObjectGeneratedDelimitedId() throws SQLException {
      String upperCaseValue = "delimited field value";
      int defaultCaseValue = 1;
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue @Column(name = "\"Id\"")
         int id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
         int myInt = defaultCaseValue;
         @Column
         int myInt2 = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return   param == 1 ? Types.INTEGER :
                                 param == 2 ? Types.VARCHAR :
                                 param == 3 ? Types.INTEGER
                                            : Types.INTEGER;
                     }
                  };
               }

               @Override
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     private boolean next = true;
                     @Override
                     public boolean next() {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return 123;
                     }
                  };
               }
            };
         }
      };
      TestClass obj = OrmWriter.updateObject(con, new TestClass());
      assertEquals("UPDATE Test_Class SET \"Delimited Field Name\"=?,Default_Case=?,myInt2=? WHERE \"Id\"=?", fetchedSql[0]);
      assertEquals(defaultCaseValue, obj.myInt);
      assertEquals(upperCaseValue, obj.delimitedFieldName);
      assertEquals(123, obj.id);
   }

   @Test
   public void deleteObject() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id
         String Id = "xyz";
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      final String[] fetchedId = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public int executeUpdate() {
                  return 1;
               }
               @Override
               public void setObject(int parameterIndex, Object x, int targetSqlType) {
                  fetchedId[0] = (String) x;
               }

            };
         }
      };
      assertEquals(1, OrmWriter.deleteObject(con, new TestClass()));
      assertEquals("DELETE FROM Test_Class WHERE Id=?", fetchedSql[0]);
      assertEquals("xyz", fetchedId[0]);
   }

   @Test
   public void deleteObjectById() throws SQLException {
      String upperCaseValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Id
         String Id = "xyz";
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = upperCaseValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      final String[] fetchedId = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public int executeUpdate() {
                  return 1;
               }

               @Override
               public void setObject(int parameterIndex, Object x, int targetSqlType) {
                  fetchedId[0] = (String) x;
               }
            };
         }
      };
      assertEquals(1, OrmWriter.deleteObjectById(con, TestClass.class, "xyz"));
      assertEquals("DELETE FROM Test_Class WHERE Id=?", fetchedSql[0]);
      assertEquals("xyz", fetchedId[0]);
   }

   @Test
   public void statementToObject() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      String idValue = "Id value";

      final String[] fetchedSql = new String[1];

      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public ResultSet executeQuery() {
                  return new DummyResultSet() {
                     private boolean next = true;
                     @Override
                     public boolean next() {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }

                     @Override
                     public ResultSetMetaData getMetaData() {
                        return new DummyResultSetMetaData() {
                           @Override
                           public int getColumnCount() {
                              return 3;
                           }

                           @Override
                           public String getColumnName(int column) {
                              return   column == 1 ? "Delimited Field Name" :
                                       column == 2 ? "default_case" :
                                       column == 3 ? "id"
                                                   : null;
                           }

                           @Override
                           public String getTableName(final int column) {
                              return "Test_Class";
                           }
                        };
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return   columnIndex == 1 ? delimitedFieldValue :
                                 columnIndex == 2 ? defaultCaseValue :
                                 columnIndex == 3 ? idValue
                                                  : null;
                     }
                  };
               }
            };
         }
      };
      PreparedStatement pstmnt = con.prepareStatement("select * from Test_Class where Id = ?");
      CaseSensitiveDatabasesClass obj = Q2Obj.fromStatement(pstmnt, CaseSensitiveDatabasesClass.class, "xyz");
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      assertEquals(idValue, obj.getId());
   }

   @Test
   public void deleteObjectNoIdProvided() throws SQLException {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default Case")
         String defaultCase = defaultCaseValue;
      }
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
            };
         }
      };
      thrown.expectMessage("No id columns provided");
      OrmWriter.deleteObject(con, new TestClass());
   }

   @Test
   public void countObjectsFromClause() throws SQLException {
      class TestClass {
         @Id @Column(name = "Id")
         String id;
      }

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      String idValue = "Id value";

      final String[] fetchedSql = new String[1];

      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return CaseSensitiveDatabasesTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }
               @Override
               public ResultSet executeQuery() {
                  return new DummyResultSet() {
                     private boolean next = true;
                     @Override
                     public boolean next() {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }
                     @Override
                     public Object getObject(int columnIndex) {
                        return 123;
                     }
                  };
               }
            };
         }
      };
      int count = countFromClause(con, TestClass.class, "where \"Delimited Field Name\" = null");
      assertEquals("SELECT COUNT(TestClass.Id) FROM TestClass TestClass where \"Delimited Field Name\" = null", fetchedSql[0]);
      assertEquals(123, count);
   }

   @Test
   public void insertObjectH2() {

      q2o.initializeTxNone(TestUtils.makeH2DataSource());
      try {
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         InsertObjectH2 obj = Q2Obj.insert(new InsertObjectH2());
         assertEquals(1, obj.Id);
         obj = Q2Obj.byId(InsertObjectH2.class, obj.Id);
         assertNotNull(obj);
         int count = Q2Obj.countFromClause(InsertObjectH2.class, "\"Delimited field name\" = 'delimited field value'");
         assertEquals(1, count);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectH2GeneratedId() {

      q2o.initializeTxNone(TestUtils.makeH2DataSource());
      try {
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         InsertObjectH2 obj = new InsertObjectH2();
         obj = Q2Obj.insert(obj);
         obj.defaultCase = "changed";
         obj = Q2Obj.update(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectH2GeneratedDelimitedId() {

      @Table(name = "\"Test Class\"")
      class TestClass {
         @Id @GeneratedValue @Column(name = "\"Id\"")
         int id;
         @Column(name = "\"Delimited field name\"")
         String delimitedFieldName = "delimited field value";
         @Column(name = "Default_Case")
         String defaultCase = "default case value";
      }

      try {
         JdbcDataSource dataSource = makeH2DataSource();
         q2o.initializeTxNone(dataSource);
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "\"Id\" INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         TestClass obj = new TestClass();
         obj = Q2Obj.insert(obj);
         obj.defaultCase = "changed";
         obj = Q2Obj.update(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void resultSetToObjectIgnoredColumns() throws SQLException {

      @Table(name = "TEST")
      class ResultSetToObjectClass {
         @Id @Column(name = "Id")
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
         @Column
         String ignoredCol;
      }

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      String idValue = "Id value";
      String ignoredColValue = "ignored col";

      DummyResultSet rs = new DummyResultSet() {
         private boolean next = true;
         @Override
         public boolean next() {
            if (next) {
               next = false;
               return true;
            }
            else {
               return false;
            }
         }

         @Override
         public ResultSetMetaData getMetaData() {
            return new DummyResultSetMetaData() {
               @Override
               public int getColumnCount() {
                  return 4;
               }

               @Override
               public String getColumnName(int column) {
                  return   column == 1 ? "Delimited Field Name" :
                           column == 2 ? "default_case" :
                           column == 3 ? "ignoredcol" :
                           column == 4 ? "ID"
                                       : null;
               }

               @Override
               public String getTableName(final int column) {
                  return "TEST";
               }
            };
         }

         @Override
         public Object getObject(int columnIndex) {
            return   columnIndex == 1 ? delimitedFieldValue :
                     columnIndex == 2 ? defaultCaseValue :
                     columnIndex == 3 ? ignoredColValue :
                     columnIndex == 4 ? idValue
                                      : null;
         }
      };

//      Introspected introspected = new Introspected(ResultSetToObjectClass.class);
      Introspector.getIntrospected(ResultSetToObjectClass.class);

      HashSet<String> ignoredCols = new HashSet<>();
      ignoredCols.add("ignoredCol");
      ResultSetToObjectClass obj = Q2Obj.fromResultSet(rs, new ResultSetToObjectClass(), ignoredCols);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(idValue, obj.id);
      assertEquals(null, obj.ignoredCol);
   }

   @Test
   public void resultSetToObjectDelimitedId() throws SQLException {

      @Table(name = "TEST")
      class ResultSetToObjectClass {
         @Id @Column(name = "\"Id\"")
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
         @Column
         String ignoredCol;
      }

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      String idValue = "Id value";
      String ignoredColValue = "ignored col";

      DummyResultSet rs = new DummyResultSet() {
         private boolean next = true;
         @Override
         public boolean next() {
            if (next) {
               next = false;
               return true;
            }
            else {
               return false;
            }
         }

         @Override
         public ResultSetMetaData getMetaData() {
            return new DummyResultSetMetaData() {
               @Override
               public int getColumnCount() {
                  return 4;
               }

               @Override
               public String getColumnName(int column) {
                  return   column == 1 ? "Delimited Field Name" :
                           column == 2 ? "default_case" :
                           column == 3 ? "ignoredcol" :
                           column == 4 ? "Id"
                                       : null;
               }

               @Override
               public String getTableName(final int column) {
                  return "TEST";
               }
            };
         }

         @Override
         public Object getObject(int columnIndex) {
            return   columnIndex == 1 ? delimitedFieldValue :
                     columnIndex == 2 ? defaultCaseValue :
                     columnIndex == 3 ? ignoredColValue :
                     columnIndex == 4 ? idValue
                                      : null;
         }
      };

//      Introspected introspected = new Introspected(ResultSetToObjectClass.class);
      Introspector.getIntrospected(ResultSetToObjectClass.class);

      HashSet<String> ignoredCols = new HashSet<>();
//      ignoredCols.add("ignoredCol");
      ResultSetToObjectClass obj = Q2Obj.fromResultSet(rs, new ResultSetToObjectClass(), ignoredCols);
      assertEquals(delimitedFieldValue, obj.delimitedFieldName);
      assertEquals(defaultCaseValue, obj.defaultCase);
      assertEquals(idValue, obj.id);
      assertEquals(ignoredColValue, obj.ignoredCol);
   }

   // ######### Utility methods ######################################################

   private int getParameterCount(String s) {
      int count = 0;
      for (Byte b : s.getBytes()) {
         if ((int)b == '?') {
            count++;
         }
      }
      return count;
   }
}
