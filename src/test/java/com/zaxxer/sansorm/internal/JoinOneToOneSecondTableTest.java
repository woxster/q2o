package com.zaxxer.sansorm.internal;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.sansorm.testutils.DummyConnection;
import org.sansorm.testutils.DummyParameterMetaData;
import org.sansorm.testutils.DummyResultSet;
import org.sansorm.testutils.DummyStatement;

import javax.persistence.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 04.05.18
 */

public class JoinOneToOneSecondTableTest {

   @Entity @Table
   public static class Left {
      private int id;
      private String type;
      private Right right;

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @OneToOne @JoinColumn(name = "id")
      public Right getRight() {
         return right;
      }

      public void setRight(Right right) {
         this.right = right;
      }

      // TODO Support properties/fields with no or only @Basic annotation
      @Column(name = "type")
      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }
   }

   public static class Right {
      private int id;

      @Id
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }
   }

   @Test
   public void setValue() throws NoSuchFieldException, IllegalAccessException {
      Left left = new Left();
      Field rightField = Left.class.getDeclaredField("right");
      PropertyInfo rightInfo = new PropertyInfo(rightField, Left.class);
      assertTrue(rightInfo.isJoinColumn);
      rightInfo.setValue(left, 1);
      assertNotNull(left.getRight());
      assertEquals(1, left.getRight().getId());
   }

   @Test
   public void getValue() throws NoSuchFieldException, InvocationTargetException, IllegalAccessException {
      Left left = new Left();
      Right right = new Right();
      right.setId(1);
      left.setRight(right);
      Field rightField = Left.class.getDeclaredField("right");
      PropertyInfo rightInfo = new PropertyInfo(rightField, Left.class);
      int rightId = (int) rightInfo.getValue(left);
      assertEquals(1, rightId);
   }

   @Test
   public void introspect() {
      Introspected introspected = new Introspected(Left.class);

      AttributeInfo[] selectableFcInfos = introspected.getSelectableFcInfos();
      Assertions.assertThat(selectableFcInfos).extracting("name").containsExactly("id", "type", "right");

      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      Assertions.assertThat(insertableFcInfos).extracting("name", "insertable").containsExactly(Tuple.tuple("type", true));

      Assertions.assertThat(introspected.getInsertableColumns()).hasSize(1).contains("type");
      Assertions.assertThat(introspected.getUpdatableColumns()).hasSize(1).contains("type");

      String columnsCsv = OrmReader.getColumnsCsv(Left.class);
      assertEquals("id,type", columnsCsv);
   }

   /**
    * Currently no graphs are persisted.
    */
   @Test
   public void insertObject() throws SQLException {
      final String[] fetchedSql = new String[1];
      final String[] params = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql, String[] idColNames) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return JoinOneToOneSecondTableTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        if (param == 1) {
                           return Types.VARCHAR;
                        }
                        throw new RuntimeException("To many parameters");
                     }
                  };
               }

               @Override
               public ResultSet getGeneratedKeys() {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() {
                        return true;
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return 123;
                     }
                  };
               }

               @Override
               public void setObject(final int parameterIndex, final Object x, final int targetSqlType) throws SQLException {
                  params[0] = (String) x;
               }
            };
         }
      };
      Left left = new Left();
      left.setType("left");
      Right right = new Right();
      left.setRight(right);
      OrmWriter.insertObject(con, left);
      assertEquals("INSERT INTO Left(type) VALUES (?)", fetchedSql[0]);
      assertEquals(123, left.getId());
      assertEquals(0, right.getId());
      assertEquals("left", params[0]);
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
