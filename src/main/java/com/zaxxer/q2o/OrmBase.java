/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.q2o;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OrmBase
 */
class OrmBase
{
   private static final Map<String, String> csvCache;

   static {
      csvCache = new ConcurrentHashMap<>();
   }

   protected OrmBase() {
      // protected constructor
   }

   protected static void populateStatementParameters(final PreparedStatement stmt, final Object... args) throws SQLException
   {
      final ParameterMetaData parameterMetaData = stmt.getParameterMetaData();
      final int paramCount = parameterMetaData.getParameterCount();
      if (paramCount > 0 && args.length < paramCount) {
         throw new RuntimeException("Too few parameters supplied for query");
      }

      for (int colIdx = paramCount; colIdx > 0; colIdx--) {
         final int parameterType = parameterMetaData.getParameterType(colIdx);
         final Object object = mapSqlType(args[colIdx - 1], parameterType);
         stmt.setObject(colIdx, object, parameterType);
      }
   }

   // public static <T> String getColumnsCsv(final Class<T> clazz, final String... tablePrefix)

   /**
    *
    * @see #getColumnsCsvExclude(Class, String...)
    */
   protected static <T> String getColumnsCsv(final Class<T> clazz, final String... tablePrefix)
   {
      final String cacheKey = (tablePrefix == null || tablePrefix.length == 0 ? clazz.getName() : tablePrefix[0] + clazz.getName());
      return csvCache.computeIfAbsent(cacheKey, key -> {
        final StringBuilder sb = new StringBuilder();

        final Introspected introspected = Introspector.getIntrospected(clazz);
        final AttributeInfo[] selectableFields = introspected.getSelectableFcInfos();
        for (AttributeInfo selectableField : selectableFields) {
           if (!selectableField.isJoinFieldWithSecondTable()) {
              sb.append(selectableField.getFullyQualifiedDelimitedFieldName(tablePrefix)).append(',');
           }
        }

        return sb.deleteCharAt(sb.length() - 1).toString();
      });
   }

   /**
    * @param excludeColumns Case as in name element or property name. In case of delimited column names provide name without delimiters.
    * @return Selectable columns. Comma separated. In case of delimited column names the column names are surrounded by delimiters.
    */
   protected static <T> String getColumnsCsvExclude(final Class<T> clazz, final String... excludeColumns)
   {
      final Set<String> excludes = new HashSet<>(Arrays.asList(excludeColumns));
      final StringBuilder sb = new StringBuilder();

      final Introspected introspected = Introspector.getIntrospected(clazz);
      final AttributeInfo[] selectableFields = introspected.getSelectableFcInfos();
      for (AttributeInfo selectableField : selectableFields) {
         if (!excludes.contains(selectableField.getCaseSensitiveColumnName())) {
            sb.append(selectableField.getFullyQualifiedDelimitedFieldName()).append(',');
         }
      }

      return sb.deleteCharAt(sb.length() - 1).toString();
   }

   protected static Object mapSqlType(final Object object, final int sqlType)
   {
      switch (sqlType) {
      case Types.TIMESTAMP:
         if (object instanceof Timestamp) {
            return object;
         }
         if (object instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) object).getTime());
         }
         break;
      case Types.DECIMAL:
         if (object instanceof BigInteger) {
            return new BigDecimal(((BigInteger) object));
         }
         break;
      case Types.SMALLINT:
         if (object instanceof Boolean) {
            return (((Boolean) object) ? (short) 1 : (short) 0);
         }
         break;
      default:
         break;
      }

      return object;
   }

   /**
    * Case insensitive comparison.
    */
   protected static boolean isIgnoredColumn(final Set<String> ignoredColumns, final String columnName) {
      return ignoredColumns.stream().anyMatch(s -> s.equalsIgnoreCase(columnName));
   }

   protected static <T> String idsAsInClause(Class<T> clazz, List<T> objects) {
      Introspected introspected = Introspector.getIntrospected(clazz);
      List<AttributeInfo> idFcInfos = introspected.getIdFcInfos();
      StringBuilder ids = new StringBuilder();
      if (!introspected.hasCompositePrimaryKey()) {
         idsAsInClauseSinglePrimaryKey(objects, idFcInfos.get(0), ids);
      }
      else {
         idsAsInClauseCompositePrimaryKey(objects, idFcInfos, ids);
      }
      return ids.toString();
   }

   private static <T> void idsAsInClauseCompositePrimaryKey(final List<T> objects, final List<AttributeInfo> idFcInfos, final StringBuilder ids) {
      ids.append("(");
      objects.forEach(obj -> {
         ids.append(" (");
         idFcInfos.forEach(info -> {
            try {
               Object value = info.getValue(obj);
               String name = info.getDelimitedColumnName();
               String delimiter = info.getType() != String.class ? "" : "'";
               ids.append(" ").append(name).append("=").append(delimiter).append(value).append(delimiter).append(" AND");
            }
            catch (IllegalAccessException | InvocationTargetException e) {
               throw new RuntimeException(e);
            }
         });
         ids.setLength(ids.length() - 4);
         ids.append(") OR");
      });
      ids.setLength(ids.length() - 3);
      ids.append(")");
   }

   private static <T> void idsAsInClauseSinglePrimaryKey(final List<T> objects, final AttributeInfo idFcInfo, final StringBuilder ids) {
      ids.append(idFcInfo.getDelimitedColumnName()).append(" ").append("IN (");
      String delimiter = idFcInfo.getType() != String.class ? "" : "'";
      objects.forEach(obj -> {
         try {
            Object value = idFcInfo.getValue(obj);
            ids.append(delimiter).append(value).append(delimiter).append(",");
         }
         catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
         }
      });
      ids.setLength(ids.length() - 1);
      ids.append(")");
   }
}
