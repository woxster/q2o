package com.zaxxer.q2o;

import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 17.12.19
 */
// IMPROVE Let user customize the converter to use.
class ValueToFieldTypeConverter {

   /**
    * Adjust value type to field type when necessary.
    *
    * @return type corrected value
    */
   Object adaptValueToFieldType(@NotNull final AttributeInfo fcInfo, final Object value, final String columnTypeName, final Introspected introspected) {
      try {
         final Class<?> fieldType = fcInfo.getType();
         Object typeCorrectedValue = null;

         if (fcInfo.getConverter() != null) {
            typeCorrectedValue = fcInfo.getConverter().convertToEntityAttribute(value);
         }
         else if (value != null) {
            Class<?> columnType = value.getClass();
            if (fieldType != columnType) {
               // Fix-up column value for enums, integer as boolean, etc.
               if (Integer.class == columnType) {
                  typeCorrectedValue = convertInteger(columnTypeName, fieldType, value, introspected);
               }
               else if (Long.class == columnType) {
                  typeCorrectedValue = convertLong(columnTypeName, fieldType, value);
               }
               else if (Double.class == columnType) {
                  typeCorrectedValue = convertDouble(columnTypeName, fieldType, value);
               }
               else if (BigInteger.class == columnType) {
                  typeCorrectedValue = convertBigInteger(columnTypeName, fieldType, value);
               }
               // // With Sybase ASE it is SybBigDecimal
               else if (BigDecimal.class.isAssignableFrom(columnType)) {
                  typeCorrectedValue = convertBigDecimal(columnTypeName, fieldType, value);
               }
               // With Sybase ASE it is SybTimestamp
               else if (Timestamp.class.isAssignableFrom(columnType)) {
                  typeCorrectedValue = convertTimestamp(columnTypeName, fieldType, value);
               }
               else if (Time.class == columnType) {
                  typeCorrectedValue = convertTime(columnTypeName, fieldType, value);
               }
               else if (java.sql.Date.class == columnType) {
                  typeCorrectedValue = convertSqlDate(columnTypeName, fieldType, value);
               }
               else if (Boolean.class == columnType) {
                  typeCorrectedValue = value;
               }
               else if (byte[].class == columnType) {
                  typeCorrectedValue = convertByteArray(columnTypeName, fieldType, value);
               }
               else if (String.class == columnType && fcInfo.enumConstants == null) {
                  typeCorrectedValue = convertString(columnTypeName, fieldType, value);
               }
               else if (UUID.class == columnType && String.class == fieldType) {
                  typeCorrectedValue = value.toString();
               }
               else if (fieldType.isEnum()) {
                  if (!q2o.isMySqlMode()) {
                     typeCorrectedValue = fcInfo.enumConstants.get(value);
                  }
                  else {
                     //noinspection unchecked
                     typeCorrectedValue = Enum.valueOf((Class) fieldType, (String) value);
                  }
               }
               else if (value instanceof Clob) {
                  typeCorrectedValue = readClob((Clob) value);
               }
               else if ("PGobject".equals(columnType.getSimpleName())
                  && "citext".equalsIgnoreCase(((PGobject) value).getType())) {
                  typeCorrectedValue = ((PGobject) value).getValue();
               }
            }
            else {
               typeCorrectedValue = value;
            }
         }
         return typeCorrectedValue;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   private Object convertString(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = Integer.valueOf(((String) columnValue));
      }
      return columnValue;
   }

   private Object convertInteger(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue, final Introspected introspected) {
      if (fieldType == Boolean.class || fieldType == boolean.class) {
         columnValue = (((Integer) columnValue) != 0);
      }
      else if (fieldType == Date.class) {
         columnValue = new Date((Integer) columnValue);
      }
      else if (fieldType == Byte.class || fieldType == byte.class) { // MySQL TINYINT
         columnValue = ((Integer) columnValue).byteValue();
      }
      else if (fieldType == Short.class || fieldType == short.class) { // MySQL TINYINT
         columnValue = ((Integer) columnValue).shortValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = ((Integer) columnValue).longValue();
      }
      else if (fieldType.isEnum()) {
         columnValue = enumFromNumber(fieldType, (Integer) columnValue);
      }
      return columnValue;
   }

   private Object enumFromNumber(final Class<?> fieldType, Integer ordinal) {
      Object columnValue = null;
      try {
         Object[] values = (Object[]) fieldType.getMethod("values").invoke(null);
         // CLARIFY Deal with NULL and 0?
         if (ordinal != null) {
            if (q2o.isMySqlMode()) {
               // "Values from the list of permissible elements in the column specification are numbered beginning with 1." (MySQL 5.5 Reference Manual, 10.4.4. The ENUM Type).
               ordinal--;
            }
            if (ordinal < values.length) {
               columnValue = values[ordinal];
            }
            else {
               throw new RuntimeException("There is no enum constant with ordinal=" + ordinal + " in " + fieldType.getCanonicalName());
            }
         }
      }
      catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException(e.getMessage(), e);
      }
      return columnValue;
   }

   private Object convertLong(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((Long) columnValue).intValue();
      }
      else if (fieldType == BigInteger.class) { // MYSQL BIGINT
         columnValue = BigInteger.valueOf((Long) columnValue);
      }
      else if (fieldType == Date.class) {
         columnValue = new Date((Long) columnValue);
      }
      else if (fieldType == Timestamp.class) {
         columnValue = new Timestamp((Long) columnValue);
      }
      return columnValue;
   }

   private Object convertDouble(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((Double) columnValue).intValue();
      }
      return columnValue;
   }

   private Object convertBigInteger(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((BigInteger) columnValue).intValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = ((BigInteger) columnValue).longValue();
      }
      else if (fieldType == Double.class || fieldType == double.class) {
         columnValue = ((BigInteger) columnValue).doubleValue();
      }
      return columnValue;
   }

   private Object convertBigDecimal(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == BigInteger.class) {
         columnValue = ((BigDecimal) columnValue).toBigInteger();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((BigDecimal) columnValue).intValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = ((BigDecimal) columnValue).longValue();
      }
      else if (fieldType == Double.class || fieldType == double.class) {
         columnValue = ((BigDecimal) columnValue).doubleValue();
      }
      return columnValue;
   }

   private Object convertTimestamp(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == java.sql.Date.class) {
         columnValue = new java.sql.Date(((Timestamp) columnValue).getTime());
      }
      else if (fieldType == Date.class) {
         columnValue = new Date(((Timestamp) columnValue).getTime());
      }
      else if (fieldType == Time.class) {
         columnValue = Time.valueOf(((Timestamp) columnValue).toLocalDateTime().toLocalTime());
      }
      return columnValue;
   }

   private Object convertTime(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Timestamp.class) {
         columnValue = new Timestamp(((Time) columnValue).getTime());
      }
      else if (fieldType == Date.class) {
         columnValue = new Date(((Timestamp) columnValue).getTime());
      }
      //               else if ("TIME".equals(columnTypeName)) {
      if (fieldType == String.class) {
         columnValue = columnValue.toString();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = Long.valueOf(((Time) columnValue).getTime()).intValue();
      }
      //               }
      return columnValue;
   }

   private Object convertSqlDate(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      // CLARIFY Just in case of MySQL?
      if ("YEAR".equals(columnTypeName)) {
         if (fieldType == String.class) {
            // MySQL 5.5 Reference Manual: "A year in two-digit or four-digit format. The default is four-digit format. In four-digit format, the permissible values are 1901 to 2155, and 0000. In two-digit format, the permissible values are 70 to 69, representing years from 1970 to 2069. MySQL displays YEAR values in YYYY format".
            Calendar cal = Calendar.getInstance();
            cal.setTime(((java.sql.Date) columnValue));
            columnValue = cal.get(Calendar.YEAR) + "";
         }
         else if (fieldType == Integer.class || fieldType == int.class) {
            // MySQL 5.5 Reference Manual: "MySQL ... permits assignment of values to YEAR columns using either strings or numbers"
            Calendar cal = Calendar.getInstance();
            cal.setTime(((java.sql.Date) columnValue));
            columnValue = cal.get(Calendar.YEAR);
         }
      }
      else if (fieldType == Date.class) {
         columnValue = new Date(((java.sql.Date) columnValue).getTime());
      }
      // CLARIFY Should it really be converted?
      else if (fieldType == Timestamp.class) {
         columnValue = new Timestamp(((java.sql.Date) columnValue).getTime());
      }
      return columnValue;
   }

   private Object convertByteArray(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      byte[] v = (byte[]) columnValue;
      if (fieldType == String.class) {
         columnValue = new String(v);
      }
      else if (fieldType == Byte.class || fieldType == byte.class) {
         columnValue = v[0];
      }
      else if (fieldType == Short.class || fieldType == short.class) {
         columnValue = new BigInteger(v).shortValue();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = new BigInteger(v).intValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = new BigInteger(v).longValue();
      }
      return columnValue;
   }

   private String readClob(@NotNull final Clob clob) throws IOException, SQLException {
      try (final Reader reader = clob.getCharacterStream()) {
         final StringBuilder sb = new StringBuilder();
         final char[] cbuf = new char[1024];
         while (true) {
            final int rc = reader.read(cbuf);
            if (rc == -1) {
               break;
            }
            sb.append(cbuf, 0, rc);
         }
         return sb.toString();
      }
   }
}
