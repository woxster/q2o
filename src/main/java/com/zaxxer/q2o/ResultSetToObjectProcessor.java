package com.zaxxer.q2o;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 17.12.19
 */
class ResultSetToObjectProcessor<T> {
   private final ResultSet resultSet;
   /**
    * the provided target ({@link #process(Object)}) for a single row result or the
    * target for the currently processed row in a multiple row result.
    */
   private T target;
   private final Set<String> ignoredColumns;
   private ResultSetMetaData metaData;
   private Introspected introspected;
   /**
    * scope is the currently processed row.
    */
   private HashMap<String, Object> tableNameToEntitiesInCurrentRow;
   private HashMap<String, Object> tableNameToEntities;
   private int currentRow = 0;
   /**
    * scope is the currently processed column if its a join column.
    */
   private Object currentEntity;
   /**
    * scope is the currently processed join column if its type is an entity that is
    * first seen in the current row and must be therefore created.
    */
   private AtomicBoolean isNewEntity;
   /**
    * scope is the currently processed column if its a join column.
    */
   private AttributeInfo parentInfo;
   private Object currentParent;
   /**
    * the per row created targets in a multi row result
    */
   private List<T> targets;
   private int colIdx;
   private static final DatabaseValueToFieldType DATABASE_VALUE_TO_FIELD_TYPE = new DatabaseValueToFieldType();

   /**
    * @param resultSet With next() already been called on. To be compatible with Spring
    *                  JDBC.
    */
   ResultSetToObjectProcessor(final ResultSet resultSet, final Set<String> ignoredColumns) {
      this.resultSet = resultSet;
      this.ignoredColumns = ignoredColumns;
   }

   T forTestOnly(final T target) throws SQLException {
      this.target = target;

      metaData = resultSet.getMetaData();
      introspected = Introspected.getInstance(target.getClass());
      tableNameToEntitiesInCurrentRow = new HashMap<>();
      tableNameToEntitiesInCurrentRow.putIfAbsent(introspected.getTableName().toUpperCase(), target);
      tableNameToEntities = new HashMap<>();

      // do ... while collides with usage within org.springframework.jdbc.core.RowMapper
//         do {
      if (currentRow > 0) {
//                  tableNameToEntitiesInCurrentRow.keySet().removeIf(tableName -> !tableName.equalsIgnoreCase(introspected.getTableName()));

         tableNameToEntities = tableNameToEntitiesInCurrentRow;
         tableNameToEntitiesInCurrentRow = new HashMap<>();
      }
      for (colIdx = metaData.getColumnCount(); colIdx > 0; colIdx--) {
         processColumn(colIdx);
      }

//               if (currentRow > 0) {
//                  for (Map.Entry<Object, AttributeInfo> entry : entityTojoinField.entrySet()) {
//                     AttributeInfo info = entry.getValue();
//                     Object trgt = tableNameToEntitiesInCurrentRow.remove(info.getOwnerClassTableName().toUpperCase());
//                     Object value = introspected.get(trgt, info);
//                     Object entity = entry.getKey();
//                     if (info.getType() == Collection.class) {
//                        ((Collection) value).add(entity);
//                     }
//                     else {
//                        introspected.set(trgt, info, entity);
//                     }
//                  }
////                  tableNameToEntitiesInCurrentRow = new HashMap<>();
//                  entityTojoinField = new HashMap<>();
//               }

      currentRow++;
//         } while (resultSet.next());

      return currentRow > 0 ? target : null;
   }

   T process(final T target) throws SQLException {
      this.target = target;
      metaData = resultSet.getMetaData();
      introspected = Introspected.getInstance(target.getClass());
      tableNameToEntitiesInCurrentRow = new HashMap<>();
      tableNameToEntitiesInCurrentRow.putIfAbsent(introspected.getTableName().toUpperCase(), target);
      tableNameToEntities = new HashMap<>();

      for (colIdx = metaData.getColumnCount(); colIdx > 0; colIdx--) {
         processColumn(colIdx);
      }

      return target;
   }

   List<T> process(final Class<T> targetClass) throws SQLException {

      metaData = resultSet.getMetaData();
      introspected = Introspected.getInstance(targetClass);
      targets = new ArrayList<>();

      do {
         try {
            target = targetClass.newInstance();
            tableNameToEntitiesInCurrentRow = new HashMap<>();
            tableNameToEntitiesInCurrentRow.put(introspected.getTableName().toUpperCase(), target);
         }
         catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
         }

         for (colIdx = metaData.getColumnCount(); colIdx > 0; colIdx--) {
            processColumn(colIdx);
         }

         targets.add(target);
         currentRow++;

      } while (resultSet.next());

      return targets;
   }

   private void processColumn(final int colIdx) throws SQLException {
      final String columnName = metaData.getColumnName(colIdx);
      // To make names in ignoredColumns independend from database case sensitivity. Otherwise you have to write database dependent code.
      if (OrmBase.isIgnoredColumn(ignoredColumns, columnName)) {
         return;
      }
      String tableName = null;
      AttributeInfo fcInfo = null;
      tableName = Optional.ofNullable(metaData.getTableName(colIdx)).orElse("");
//      if (tableName.isEmpty() || tableName.equalsIgnoreCase(introspected.getTableName())) {
//         fcInfo = !tableName.isEmpty()
//            ? introspected.getFieldColumnInfo(tableName, columnName)
//            : introspected.getFieldColumnInfo(columnName);
//      }

      if (!tableName.isEmpty()) {
         fcInfo = introspected.getFieldColumnInfo(tableName, columnName);
         if (fcInfo == null) {
            // OneToOneTest.flattenedTableJoin()
            fcInfo = introspected.getFieldColumnInfo(columnName);
         }
      }
      else {
         fcInfo = introspected.getFieldColumnInfo(columnName);
      }

      Object columnValue = getObject(colIdx, fcInfo);

      // tableName is empty when aliases as in "SELECT (t.string_from_number + 1) as string_from_number " were used. See org.sansorm.QueryTest.testConverterLoad().
      if (tableName.isEmpty() || tableName.equalsIgnoreCase(introspected.getTableName())) {

         Object parent = tableNameToEntitiesInCurrentRow.computeIfAbsent(introspected.getTableName().toUpperCase(), tbl -> {
            try {
               return introspected.getTableTarget(introspected.getTableName());
            }
            catch (IllegalAccessException | InstantiationException e) {
               throw new RuntimeException(e);
            }
         });
         // If objectFromSelect() does more fields retrieve as are defined on the entity then fcInfo is null.
         if (fcInfo != null
            && (!fcInfo.isIdField || !fcInfo.getType().isPrimitive() || columnValue != null)) {
            // Do not call fcInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
            try {
               Object typeCorrectedValue = DATABASE_VALUE_TO_FIELD_TYPE.adaptValueToFieldType(fcInfo, columnValue, metaData, introspected, colIdx);
               fcInfo.setValue(parent, typeCorrectedValue);
            }
            catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
         }
      }
      else {
         processColumnOfJoinedTable(columnName, columnValue, tableName);
      }
   }

   @Nullable
   private Object getObject(final int colIdx, final AttributeInfo fcInfo) throws SQLException
   {
      Object columnValue = null;
      // fcInfo is null in case of a database field but no corresponding entity field.
      if (fcInfo != null) {
         if (!q2o.isMySqlMode() || !Blob.class.isAssignableFrom(fcInfo.getType())) {
            if (!Clob.class.isAssignableFrom(fcInfo.getType())) {
               columnValue = resultSet.getObject(colIdx);
            }
            else {
               // Sybase: In case of the value is null resultSet.getObject(colIdx) returns the string "null".
               columnValue = resultSet.getClob(colIdx);
            }
         }
         else {
            // MySQL: resultSet.getObject(colIdx) returns column name as String only.
            columnValue = resultSet.getBlob(colIdx);
         }
      }
      return columnValue;
   }

   /**
    * Called for every table of a joined table.
    */
   private void processColumnOfJoinedTable(
      final String columnName,
      final Object columnValue,
      final String tableName) throws SQLException {

      isNewEntity = new AtomicBoolean(false);
      currentEntity = tableNameToEntitiesInCurrentRow.computeIfAbsent(tableName.toUpperCase(), tableNameUpperCased -> {
         try {
            isNewEntity.set(true);
            return introspected.getTableTarget(tableNameUpperCased);
         }
         catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException(e);
         }
      });
      // currentEntity is null if target does not correspond with an actual table. See com.zaxxer.q2o.internal.JoinOneToOneSeveralTablesTest.flattenedTableJoin().
      currentEntity = currentEntity == null ? target : currentEntity;

      Class<?> currentTargetClass = currentEntity.getClass();
      AttributeInfo currentTargetInfo = Introspected.getInstance(currentTargetClass).getFieldColumnInfo(columnName);
      // Do not call currentTargetInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
      if (currentTargetInfo != null && (
         !currentTargetInfo.isIdField
            || !currentTargetInfo.getType().isPrimitive()
            || columnValue != null)
      ) {
         if (!(currentTargetInfo.getType().isPrimitive() && columnValue == null)) {
            try {
               Object typeCorrectedValue = DATABASE_VALUE_TO_FIELD_TYPE.adaptValueToFieldType(currentTargetInfo, columnValue, metaData, introspected, colIdx);
               currentTargetInfo.setValue(currentEntity, typeCorrectedValue);
            }
            catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
         }
         // parentInfo is null if target does not correspond with an actual table. See com.zaxxer.q2o.internal.JoinOneToOneSeveralTablesTest.flattenedTableJoin().
         parentInfo = introspected.getFieldColumnInfo(currentTargetClass);
         if (parentInfo != null) {
            currentParent = tableNameToEntitiesInCurrentRow.computeIfAbsent(parentInfo.getOwnerClassTableName().toUpperCase(), tbln -> {
               try {
                  return parentInfo.getOwnerClazz().newInstance();
               }
               catch (InstantiationException | IllegalAccessException e) {
                  throw new RuntimeException(e);
               }
            });
            // Do not call currentTargetInfo.setValue() directly. AttributeInfo#setValue() does not apply type conversion (e. g. identity fields of type BigInteger to integer)!
            if (!parentInfo.isOneToManyAnnotated) {
               try {
                  Object typeCorrectedValue = DATABASE_VALUE_TO_FIELD_TYPE.adaptValueToFieldType(parentInfo, currentEntity, metaData, introspected, colIdx);
                  parentInfo.setValue(currentParent, typeCorrectedValue);
               }
               catch (IllegalAccessException e) {
                  throw new RuntimeException(e);
               }
            }
            else if (parentInfo.getType() == Collection.class) {
               setManyToOneField();
            }
         }
      }
   }

   private void setManyToOneField() {
      try {
         Object value = parentInfo.getValue(currentParent);
         if (value == null) {
            Collection collection = new ArrayList();
            collection.add(currentEntity);
            Object typeCorrecteValue = DATABASE_VALUE_TO_FIELD_TYPE.adaptValueToFieldType(parentInfo, collection, metaData, introspected, colIdx);
            parentInfo.setValue(currentParent, typeCorrecteValue);

//            String parentTableName = parentInfo.getOwnerClassTableName().toUpperCase();
//            Object parentEntity = tableNameToEntities.get(parentTableName);
//            if (parentEntity != null) {
//               Collection c = (Collection) FieldValueToDatabaseType.get(parentEntity, parentInfo);
//               c.add(currentEntity);
//            }
         }
      }
      catch (IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }
}
