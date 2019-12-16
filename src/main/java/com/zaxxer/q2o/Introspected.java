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

import org.jetbrains.annotations.NotNull;
import org.postgresql.util.PGobject;

import javax.persistence.*;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Reader;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

/**
 * An introspected class.
 */
final class Introspected {
   private final Class<?> clazz;
   final List<AttributeInfo> idFcInfos;
   private String delimitedTableName;
   /**
    * Fields in case insensitive lexicographic order
    */
   private final TreeMap<String, List<AttributeInfo>> columnToField;

   private final Map<String, AttributeInfo> propertyToField;
   private final List<AttributeInfo> allFcInfos;
   private List<AttributeInfo> insertableFcInfos;
   private List<AttributeInfo> updatableFcInfos;
   private AttributeInfo selfJoinFCInfo;
   private HashMap<Field, AccessType> fieldsAccessType;
   private final HashMap<String, ArrayList<AttributeInfo>> allFcInfosByTableName = new HashMap<>();
   private final TreeMap<String,Class<?>> tableNameToClassCaseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
   private final HashMap<Class<?>, AttributeInfo> actualTypeToFieldColumnInfo = new HashMap<Class<?>, AttributeInfo>();

   private boolean isGeneratedId;
   private String tableName;

   // We use arrays because iteration is much faster
   private AttributeInfo[] idFieldColumnInfos;
   private String[] idColumnNames;
   private String[] columnTableNames;
   private String[] insertableColumns;
   private String[] updatableColumns;
   private String[] delimitedColumnNames;
   private String[] caseSensitiveColumnNames;
   private String[] delimitedColumnsSansIds;
   private AttributeInfo[] insertableFcInfosArray;
   private AttributeInfo[] updatableFcInfosArray;
   private AttributeInfo[] selectableFcInfos;

   private static final HashSet<Class<?>> jpaAnnotations = new HashSet<>();

   static {
//         jpaAnnotations.add(Access.class);
      jpaAnnotations.add(AssociationOverride.class);
      jpaAnnotations.add(AssociationOverrides.class);
      jpaAnnotations.add(AttributeOverride.class);
      jpaAnnotations.add(AttributeOverrides.class);
      jpaAnnotations.add(Basic.class);
//         jpaAnnotations.add(Cacheable.class);
      jpaAnnotations.add(CollectionTable.class);
      jpaAnnotations.add(Column.class);
      jpaAnnotations.add(ColumnResult.class);
      jpaAnnotations.add(ConstructorResult.class);
      jpaAnnotations.add(Convert.class);
      jpaAnnotations.add(Converter.class);
      jpaAnnotations.add(Converts.class);
      jpaAnnotations.add(DiscriminatorColumn.class);
      jpaAnnotations.add(DiscriminatorValue.class);
      jpaAnnotations.add(ElementCollection.class);
      jpaAnnotations.add(Embeddable.class);
      jpaAnnotations.add(Embedded.class);
      jpaAnnotations.add(EmbeddedId.class);
      jpaAnnotations.add(Entity.class);
      jpaAnnotations.add(EntityListeners.class);
      jpaAnnotations.add(EntityResult.class);
      jpaAnnotations.add(Enumerated.class);
      jpaAnnotations.add(ExcludeDefaultListeners.class);
      jpaAnnotations.add(ExcludeSuperclassListeners.class);
      jpaAnnotations.add(FieldResult.class);
      jpaAnnotations.add(ForeignKey.class);
      jpaAnnotations.add(GeneratedValue.class);
      jpaAnnotations.add(Id.class);
      jpaAnnotations.add(IdClass.class);
      jpaAnnotations.add(Index.class);
      jpaAnnotations.add(Inheritance.class);
      jpaAnnotations.add(JoinColumn.class);
      jpaAnnotations.add(JoinColumns.class);
      jpaAnnotations.add(JoinTable.class);
      jpaAnnotations.add(Lob.class);
      jpaAnnotations.add(ManyToMany.class);
      jpaAnnotations.add(ManyToOne.class);
      jpaAnnotations.add(MapKey.class);
      jpaAnnotations.add(MapKeyClass.class);
      jpaAnnotations.add(MapKeyColumn.class);
      jpaAnnotations.add(MapKeyEnumerated.class);
      jpaAnnotations.add(MapKeyJoinColumn.class);
      jpaAnnotations.add(MapKeyJoinColumns.class);
      jpaAnnotations.add(MapKeyTemporal.class);
      jpaAnnotations.add(MappedSuperclass.class);
      jpaAnnotations.add(MapsId.class);
      jpaAnnotations.add(NamedAttributeNode.class);
      jpaAnnotations.add(NamedEntityGraph.class);
      jpaAnnotations.add(NamedEntityGraphs.class);
      jpaAnnotations.add(NamedNativeQueries.class);
      jpaAnnotations.add(NamedNativeQuery.class);
      jpaAnnotations.add(NamedQueries.class);
      jpaAnnotations.add(NamedQuery.class);
      jpaAnnotations.add(NamedStoredProcedureQueries.class);
      jpaAnnotations.add(NamedStoredProcedureQuery.class);
      jpaAnnotations.add(NamedSubgraph.class);
      jpaAnnotations.add(OneToMany.class);
      jpaAnnotations.add(OneToOne.class);
      jpaAnnotations.add(OrderBy.class);
      jpaAnnotations.add(OrderColumn.class);
      jpaAnnotations.add(PersistenceContext.class);
      jpaAnnotations.add(PersistenceContexts.class);
      jpaAnnotations.add(PersistenceProperty.class);
      jpaAnnotations.add(PersistenceUnit.class);
      jpaAnnotations.add(PersistenceUnits.class);
      jpaAnnotations.add(PostLoad.class);
      jpaAnnotations.add(PostPersist.class);
      jpaAnnotations.add(PostRemove.class);
      jpaAnnotations.add(PostUpdate.class);
      jpaAnnotations.add(PrePersist.class);
      jpaAnnotations.add(PreRemove.class);
      jpaAnnotations.add(PreUpdate.class);
      jpaAnnotations.add(PrimaryKeyJoinColumn.class);
      jpaAnnotations.add(PrimaryKeyJoinColumns.class);
      jpaAnnotations.add(QueryHint.class);
//         jpaAnnotations.add(SecondaryTable.class);
//         jpaAnnotations.add(SecondaryTables.class);
//         jpaAnnotations.add(SequenceGenerator.class);
      jpaAnnotations.add(SqlResultSetMapping.class);
      jpaAnnotations.add(SqlResultSetMappings.class);
      jpaAnnotations.add(StoredProcedureParameter.class);
//         jpaAnnotations.add(Table.class);
//         jpaAnnotations.add(TableGenerator.class);
      jpaAnnotations.add(Temporal.class);
      jpaAnnotations.add(Transient.class);
//         jpaAnnotations.add(UniqueConstraint.class);
      jpaAnnotations.add(Version.class);
   }

   private boolean initialized;

   /**
    * Constructor. Introspect the specified class and cache various annotation data about it.
    *
    * @param clazz the class to introspect
    */
   Introspected(final Class<?> clazz) {

      this.clazz = clazz;
      this.columnToField = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // support both in- and case-sensitive DBs
      this.propertyToField = new HashMap<>();
      this.insertableFcInfos = new ArrayList<>();
      this.updatableFcInfos = new ArrayList<>();
      this.allFcInfos = new ArrayList<>();
      this.idFcInfos = new ArrayList<>();
   }

   Introspected introspect() {
      if (!initialized) {
         extractClassTableName();
         tableNameToClassCaseInsensitive.put(tableName, clazz);

         try {
            for (final Field field : getDeclaredFields()) {
               final int modifiers = field.getModifiers();
               if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers)) {
                  continue;
               }

               final Class<?> fieldClass = field.getDeclaringClass();
               final AttributeInfo fcInfo =
                  fieldsAccessType.get(field) == AccessType.FIELD
                     ? new FieldInfo(field, clazz)
                     : new PropertyInfo(field, clazz);
               if (fcInfo.isToBeConsidered()) {

                  List<AttributeInfo> attributeInfos = columnToField.computeIfAbsent(fcInfo.getCaseSensitiveColumnName(), k -> new ArrayList<>());
                  attributeInfos.add(fcInfo);

                  propertyToField.put(fcInfo.getName(), fcInfo);
                  allFcInfos.add(fcInfo);
                  addToAllFcInfosByTableName(fcInfo);

                  if (fcInfo.isJoinFieldWithSecondTable()) {
                     actualTypeToFieldColumnInfo.put(fcInfo.getActualType(), fcInfo);
                     tableNameToClassCaseInsensitive.putIfAbsent(fcInfo.getTableName(), fcInfo.getActualType());
                  }

                  if (fcInfo.isIdField) {
                     // Is it a problem that Class.getDeclaredFields() claims the fields are returned unordered?  We count on order.
                     idFcInfos.add(fcInfo);
                     isGeneratedId = isGeneratedId || fcInfo.isGeneratedId;
                     if (isGeneratedId && idFcInfos.size() > 1) {
                        throw new IllegalStateException("Cannot have multiple @Id annotations and @GeneratedValue at the same time.");
                     }
                     if (!fcInfo.isGeneratedId) {
                        if (fcInfo.isInsertable() == null || fcInfo.isInsertable()) {
                           insertableFcInfos.add(fcInfo);
                        }
                        if (fcInfo.isUpdatable() == null || fcInfo.isUpdatable()) {
                           updatableFcInfos.add(fcInfo);
                        }
                     }
                  }
                  else {
                     if (fcInfo.isSelfJoinField()) {
                        selfJoinFCInfo = fcInfo;
                     }
                     if (fcInfo.isInsertable() == null || fcInfo.isInsertable()) {
                        insertableFcInfos.add(fcInfo);
                     }
                     if (fcInfo.isUpdatable() == null || fcInfo.isUpdatable()) {
                        updatableFcInfos.add(fcInfo);
                     }
                  }
               }
            }

            precalculateColumnInfos(idFcInfos);

         }
         catch (Exception e) {
            throw new RuntimeException(e);
         }
      }
      initialized = true;
      return this;
   }

   private void addToAllFcInfosByTableName(final AttributeInfo fcInfo) {
      String tableName = fcInfo.getTableName();
      ArrayList<AttributeInfo> attributeInfos = allFcInfosByTableName.computeIfAbsent(tableName, tblName -> new ArrayList<>());
      attributeInfos.add(fcInfo);
   }

   /**
    *
    * @return new entity representing this table
    */
   Object getTableTarget(String tableName) throws IllegalAccessException, InstantiationException {
      Class<?> cls = tableNameToClassCaseInsensitive.get(tableName);
      if (cls != null) {
         Introspected i = Introspector.getIntrospected(cls);
         return i.clazz.newInstance();
      }
      else {
         for (Class<?> c : tableNameToClassCaseInsensitive.values()) {
            Introspected introspected = Introspector.getIntrospected(c);
            if (introspected != this) {
               Object target = introspected.getTableTarget(tableName);
               if (target != null) {
                  return target;
               }
            }
         }
      }
      return null;
   }

   AttributeInfo getFieldColumnInfo(String tableName, String columnName) {
      Class<?> cls = tableNameToClassCaseInsensitive.get(tableName);
      Introspected introspected = Introspector.getIntrospected(cls);
      return introspected.getFieldColumnInfo(columnName);
   }

   AttributeInfo getFieldColumnInfo(Class<?> actualType) {
      AttributeInfo info = actualTypeToFieldColumnInfo.get(actualType);
      if (info != null) {
         return info;
      }
      else {
         for (Class<?> cls : tableNameToClassCaseInsensitive.values()) {
            Introspected introspected = Introspector.getIntrospected(cls);
            if (introspected != this) {
               AttributeInfo fieldColumnInfo = introspected.getFieldColumnInfo(actualType);
               if (fieldColumnInfo != null) {
                  return fieldColumnInfo;
               }
            }
         }
      }
      return null;
   }

   /**
    * Get the {@link AttributeInfo} for the specified column name.
    *
    * @param columnName case insensitive column name without delimiters.
    */
   AttributeInfo getFieldColumnInfo(final String columnName) {
      List<AttributeInfo> attributeInfos = columnToField.get(columnName);
      // If objectFromSelect() did more fields retrieve as are defined on the entity.
      if (attributeInfos != null) {
         for (AttributeInfo attributeInfo : attributeInfos) {
            if (attributeInfo.isSelfJoinField() || !attributeInfo.isJoinColumn) {
               return attributeInfo;
            }
         }
      }
      return null;
   }

   /**
    * Get the declared {@link Field}s for the class, including declared fields from mapped
    * superclasses.
    */
   private Collection<Field> getDeclaredFields() {
      fieldsAccessType = new HashMap<>();
      final LinkedList<Field> declaredFields = new LinkedList<>(Arrays.asList(clazz.getDeclaredFields()));
      analyzeAccessType(declaredFields, clazz);
      for (Class<?> c = clazz.getSuperclass(); c != null; c = c.getSuperclass()) {
         // support fields from MappedSuperclass(es).
         // Do not support ambiguous annotation. Spec says:
         // "A mapped superclass has no separate table defined for it".
         if (c.getAnnotation(MappedSuperclass.class) != null) {
            if (c.getAnnotation(Table.class) == null) {
               final List<Field> df = Arrays.asList(c.getDeclaredFields());
               declaredFields.addAll(df);
               analyzeAccessType(df, c);
            }
            else {
               throw new RuntimeException("Class " + c.getName() + " annotated with @MappedSuperclass cannot also have @Table annotation");
            }
         }
      }
      return declaredFields;
   }

   /**
    * "The default access type of an entity hierarchy is determined by the placement of mapping annotations on the attributes of the entity classes and mapped superclasses of the entity hierarchy that do not explicitly specify an access type. An access type is explicitly specified by means of the Access annotation ... the placement of the mapping annotations on
    * either the persistent fields or persistent properties of the entity class specifies the access type as being either field- or property-based access respectively." (JSR 317: JavaTM Persistence API, Version 2.0, 2.3.1 Default Access Type).
    * <p>
    * "An access type for an individual entity class, mapped superclass, or embeddable class can be specified for that class independent of the default for the entity hierarchy" ... When Access(FIELD) is applied to an entity class it is possible to selectively designate individual attributes within the class for property access ... When Access(PROPERTY) is applied to an entity class it is possible to selectively designate individual attributes within the class for instance variable access. It is not permitted to specify a field as Access(PROPERTY) or a property as Access(FIELD)." (JSR 317: JavaTM Persistence API, Version 2.0, 2.3.2 Explicit Access Type)
    */
   private void analyzeAccessType(final List<Field> declaredFields, final Class<?> cl) {
      if (isExplicitPropertyAccess(cl)) {
         analyzeExplicitPropertyAccess(declaredFields, cl);
      }
      else if (isExplicitFieldAccess(cl)) {
         analyzeExlicitFieldAccess(declaredFields, cl);
      }
      else {
         analyzeDefaultAccess(declaredFields, cl);
      }
   }

   private void analyzeDefaultAccess(final List<Field> declaredFields, final Class<?> cl) {
      declaredFields.forEach(field -> {
         final boolean isDefaultFieldAccess = declaredFields.stream().anyMatch(this::isJpaAnnotated);
         if (isDefaultFieldAccess) {
            fieldsAccessType.put(field, AccessType.FIELD);
         }
         else {
            final Method[] declaredMethods = cl.getDeclaredMethods();
            if (declaredMethods.length != 0) {
               final boolean isDefaultPropertyAccess = Arrays.stream(declaredMethods).anyMatch(this::isJpaAnnotated);
               fieldsAccessType.put(
                  field,
                  isDefaultPropertyAccess ? AccessType.PROPERTY : AccessType.FIELD);
            }
            else {
               // defaults to field access
               fieldsAccessType.put(field, AccessType.FIELD);
            }
         }
      });
   }

   private void analyzeExlicitFieldAccess(final List<Field> declaredFields, final Class<?> cl) {
      declaredFields.forEach(field -> {
         try {
            final PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), cl);
            final Method readMethod = descriptor.getReadMethod();
            final Access accessTypeOnMethod = readMethod.getAnnotation(Access.class);
            final Access accessTypeOnField = field.getDeclaredAnnotation(Access.class);
            if (accessTypeOnMethod == null) {
               if (accessTypeOnField == null || accessTypeOnField.value() == AccessType.FIELD) {
                  fieldsAccessType.put(field, AccessType.FIELD);
               }
               else {
                  throw new RuntimeException("A field can not be of access type property: " + field);
               }
            }
            else if (accessTypeOnMethod.value() == AccessType.PROPERTY) {
               fieldsAccessType.put(field, AccessType.PROPERTY);
            }
            else {
               throw new RuntimeException("A method can not be of access type field: " + readMethod);
            }
         }
         catch (IntrospectionException ignored) {
            fieldsAccessType.put(field, AccessType.FIELD);
//            e.printStackTrace();
         }
      });
   }

   private void analyzeExplicitPropertyAccess(final List<Field> declaredFields, final Class<?> cl) {
      declaredFields.forEach(field -> {
         try {
            final PropertyDescriptor descriptor = new PropertyDescriptor(field.getName(), cl);
            final Method readMethod = descriptor.getReadMethod();
            final Access accessTypeOnMethod = readMethod.getAnnotation(Access.class);
            final Access accessTypeOnField = field.getDeclaredAnnotation(Access.class);
            if (accessTypeOnField == null) {
               if (accessTypeOnMethod == null || accessTypeOnMethod.value() == AccessType.PROPERTY) {
                  fieldsAccessType.put(field, AccessType.PROPERTY);
               }
               else {
                  throw new RuntimeException("A method can not be of access type field: " + readMethod);
               }
            }
            else if (accessTypeOnField.value() == AccessType.FIELD) {
               fieldsAccessType.put(field, AccessType.FIELD);
            }
            else {
               throw new RuntimeException("A field can not be of access type property: " + field);
            }
         }
         catch (IntrospectionException ignored) {
            fieldsAccessType.put(field, AccessType.FIELD);
//            e.printStackTrace();
         }

      });
   }

   /**
    * Get the table name specified by the {@link Table} annotation.
    */
   private void extractClassTableName() {
      String tblname = "";
      final Entity entity = clazz.getAnnotation(Entity.class);
      if (entity != null && !entity.name().isEmpty()) {
         tblname = entity.name();
      }
      else {
         final Table tableAnnotation = clazz.getAnnotation(Table.class);
         if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            tblname = tableAnnotation.name();
         }
      }
      this.delimitedTableName = tblname.isEmpty()
         ? clazz.getSimpleName() // See AttributeInfo#extractTableName()
         : tblname;
      tableName = delimitedTableName.startsWith("\"") || delimitedTableName.endsWith("\"")
         ? delimitedTableName.substring(1, delimitedTableName.length() - 1)
         : delimitedTableName;
   }

   boolean isExplicitFieldAccess(final Class<?> fieldClass) {
      final Access accessType = fieldClass.getAnnotation(Access.class);
      return accessType != null && accessType.value() == AccessType.FIELD;
   }

   boolean isExplicitPropertyAccess(final Class<?> c) {
      final Access accessType = c.getAnnotation(Access.class);
      return accessType != null && accessType.value() == AccessType.PROPERTY;
   }

   boolean isJpaAnnotated(final AccessibleObject fieldOrMethod) {
      final Annotation[] annotations = fieldOrMethod.getDeclaredAnnotations();
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < annotations.length; i++) {
         if (jpaAnnotations.contains(annotations[i].annotationType())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Get the value of the specified field from the specified target object, possibly after applying a
    * {@link AttributeConverter}.
    *
    * @param target the target instance
    * @param fcInfo the {@link AttributeInfo} used to access the field value
    * @return the value of the field from the target object, possibly after applying a {@link AttributeConverter}
    */
   Object get(final Object target, final AttributeInfo fcInfo) {
      if (fcInfo == null) {
         throw new RuntimeException("FieldColumnInfo must not be null. Type is " + target.getClass().getCanonicalName());
      }

      try {
         Object value = fcInfo.getValue(target);
         // Fix-up column value for enums, integer as boolean, etc.
         if (fcInfo.getConverter() != null) {
            value = fcInfo.getConverter().convertToDatabaseColumn(value);
         }
         else if (fcInfo.enumConstants != null && value != null) {
            if (fcInfo.enumType == EnumType.ORDINAL) {
               value = ((Enum<?>) value).ordinal();
               if (q2o.isMySqlMode()) {
                  // "Values from the list of permissible elements in the column specification are numbered beginning with 1." (MySQL 5.5 Reference Manual, 10.4.4. The ENUM Type).
                  value = (int)value + 1;
               }
            }
            else {
               value = ((Enum<?>) value).name();
            }
         }

         return value;
      }
      catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Set a field value of the specified target object.
    *
    * @param target the target instance
    * @param fcInfo the {@link AttributeInfo} used to access the field value
    * @param value  the value to set into the field of the target instance, possibly after applying a
 *               {@link AttributeConverter}
    * @param columnTypeName
    */
   void set(final Object target, @NotNull final AttributeInfo fcInfo, final Object value, final String columnTypeName) {
      // IMPROVE Encapsulate as DefaultTypeConverter and let user customize the converter to use.
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
                  typeCorrectedValue = convertInteger(columnTypeName, fieldType, value);
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
               else if (BigDecimal.class == columnType) {
                  typeCorrectedValue = convertBigDecimal(columnTypeName, fieldType, value);
               }
               else if (Timestamp.class == columnType) {
                  typeCorrectedValue = convertTimestamp(columnTypeName, fieldType, value);
               }
               else if (Time.class == columnType) {
                  typeCorrectedValue = convertTime(columnTypeName, fieldType, value);
               }
               else if (java.sql.Date.class == columnType) {
                  typeCorrectedValue = convertSqlDate(columnTypeName, fieldType, value);
               }
               else if (byte[].class == columnType) {
                  typeCorrectedValue = convertByteArray(columnTypeName, fieldType, value);
               }
               else if (String.class == columnType && fcInfo.enumConstants == null) {
                  typeCorrectedValue = convertString(columnTypeName, fieldType, value);
               }
               else if (java.util.UUID.class == columnType && String.class == fieldType) {
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

         fcInfo.setValue(target, typeCorrectedValue);
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

   private Object convertInteger(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
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
         columnValue = ((Integer)columnValue).longValue();
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
         // TODO NULL und 0 behandeln.
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
         columnValue = new Date((Long)columnValue);
      }
      else if (fieldType == Timestamp.class) {
         columnValue = new Timestamp((Long)columnValue);
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
         columnValue = Time.valueOf(((Timestamp)columnValue).toLocalDateTime().toLocalTime());
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
      // TODO Nur wenn MySQL?
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
         // TODO Cast null?
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

   /**
    * Determines whether this class has join columns.
    *
    * @return true if this class has {@link JoinColumn} annotations
    */
   boolean hasSelfJoinColumn() {
      return selfJoinFCInfo != null;
   }

   /**
    * Determines whether the specified column is a self-join column.
    *
    * @param columnName The column name to check. Requires case sensitive match of name element or property name without delimiters.
    * @return true if the specified column is a self-join column
    */
   boolean isSelfJoinColumn(final String columnName) {
      return selfJoinFCInfo.getCaseSensitiveColumnName().equals(columnName);
   }

   /**
    * Get the name of the self-join column, if one is defined for this class.
    *
    * @return the self-join column name, or null
    */
   String getSelfJoinColumn() {
      return selfJoinFCInfo != null ? selfJoinFCInfo.getColumnName() : null;
   }

   /**
    * @see #getSelfJoinColumn()
    * return the {@link AttributeInfo} of the self-join column, if one is defined for this class.
    */
   AttributeInfo getSelfJoinColumnInfo() {
      return selfJoinFCInfo;
   }

   /**
    * Get all of the columns defined for this introspected class. In case of delimited column names
    * the column name surrounded by delimiters.
    *
    * @return and array of column names
    */
   String[] getColumnNames() {
      return delimitedColumnNames;
   }

   /**
    * Get all of the table names associated with the columns for this introspected class. In case of
    * delimited field names surrounded by delimiters.
    *
    * @return an array of column table names
    */
   String[] getColumnTableNames() {
      return columnTableNames;
   }

   /**
    * Get all of the ID columns defined for this introspected class. In case of delimited field names
    * surrounded by delimiters.
    *
    * @return and array of column names
    */
   String[] getIdColumnNames() {
      return idColumnNames;
   }

   /**
    * Get all of the columns defined for this introspected class, minus the ID columns. In case of
    * delimited field names surrounded by delimiters.
    *
    * @return and array of column names
    */
   String[] getColumnsSansIds() {
      return delimitedColumnsSansIds;
   }

   boolean hasGeneratedId() {
      return isGeneratedId;
   }

   /**
    * Get the insertable column names for this object.
    *
    * @return the insertable columns. In case of delimited column names the names are surrounded
    * by delimiters.
    * @see #getUpdatableColumns()
    */
   String[] getInsertableColumns() {
      return insertableColumns;
   }

   private void precalculateInsertableColumns() {
      insertableFcInfosArray = new AttributeInfo[insertableFcInfos.size()];
      ArrayList<String> uniqueColNames = new ArrayList<>();
      ArrayList<AttributeInfo> uniqueInfos = new ArrayList<>();
      for (int i = 0; i < insertableFcInfos.size(); i++) {
         insertableFcInfosArray[i] = insertableFcInfos.get(i);
         String delimitedColumnName = insertableFcInfos.get(i).getDelimitedColumnName();
         if (!uniqueColNames.contains(delimitedColumnName)) {
            uniqueColNames.add(delimitedColumnName);
            uniqueInfos.add(insertableFcInfos.get(i));
         }
      }
      insertableColumns = new String[uniqueInfos.size()];
      for (int i = 0, j = 0; i < uniqueInfos.size(); i++) {
         insertableColumns[j++] = uniqueInfos.get(i).getDelimitedColumnName();
      }
   }

   /**
    * Get the updatable columns for this object. Column names are unique. For example if there is an @Id field whose name is also used in a name element of a relationship annotation it is only retrieved once.
    *
    * @return The names of the updatable columns. Case sensitive. In case of delimited fields surrounded by delimiters.
    */
   String[] getUpdatableColumns() {
      return updatableColumns;
   }

   private void precalculateUpdatableColumns() {
      updatableFcInfosArray = new AttributeInfo[updatableFcInfos.size()];
      ArrayList<String> uniqueColNames = new ArrayList<>();
      ArrayList<AttributeInfo> uniqueInfos = new ArrayList<>();
      for (int i = 0; i < updatableFcInfos.size(); i++) {
         updatableFcInfosArray[i] = updatableFcInfos.get(i);
         String delimitedColumnName = updatableFcInfos.get(i).getDelimitedColumnName();
         if (!uniqueColNames.contains(delimitedColumnName)) {
            uniqueColNames.add(delimitedColumnName);
            uniqueInfos.add(updatableFcInfos.get(i));
         }
      }
      updatableColumns = new String[uniqueInfos.size()];
      for (int i = 0, j = 0; i < uniqueInfos.size(); i++) {
         updatableColumns[j++] = uniqueInfos.get(i).getDelimitedColumnName();
      }
   }

   /**
    * Is this specified column insertable?
    *
    * @param columnName Same case as in name element or property name without delimeters.
    * @return true if insertable, false otherwise
    */
   boolean isInsertableColumn(final String columnName) {
      // Use index iteration to avoid generating an Iterator as side-effect
      final AttributeInfo[] fcInfos = getInsertableFcInfos();
      for (int i = 0; i < fcInfos.length; i++) {
         if (fcInfos[i].getCaseSensitiveColumnName().equals(columnName)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Is this specified column updatable?
    *
    * @param columnName Same case as in name element or property name without delimeters.
    * @return true if updatable, false otherwise
    */
   boolean isUpdatableColumn(final String columnName) {
      // Use index iteration to avoid generating an Iterator as side-effect
      final AttributeInfo[] fcInfos = getUpdatableFcInfos();
      for (int i = 0; i < fcInfos.length; i++) {
         if (fcInfos[i].getCaseSensitiveColumnName().equals(columnName)) {
            return true;
         }
      }
      return false;
   }

   Object[] getActualIds(final Object target) {
      if (idColumnNames.length == 0) {
         return null;
      }

      try {
         final AttributeInfo[] fcInfos = idFieldColumnInfos;
         final Object[] ids = new Object[idColumnNames.length];
         for (int i = 0; i < fcInfos.length; i++) {
            ids[i] = fcInfos[i].getValue(target);
         }
         return ids;
      }
      catch (IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Get the table name defined for the introspected class.
    *
    * @return a table name
    */
   String getDelimitedTableName() {
      return delimitedTableName;
   }

   String getTableName() {
      return tableName;
   }

   /**
    * Get the delimited column name for the specified property name, or {@code null} if
    * no such property exists.
    * <p>
    * CLARIFY Must be public?
    *
    * @return the delimited column name or {@code null}
    */
   String getColumnNameForProperty(final String propertyName) {
      return Optional.ofNullable(propertyToField.get(propertyName))
         .map(fcInfo -> fcInfo.getDelimitedColumnName())
         .orElse(null);
   }

   private void precalculateColumnInfos(final List<AttributeInfo> idFcInfos) {
      idFieldColumnInfos = new AttributeInfo[idFcInfos.size()];
      idColumnNames = new String[idFcInfos.size()];
      final String[] columnNames = new String[columnToField.size()];
      columnTableNames = new String[columnNames.length];
      caseSensitiveColumnNames = new String[columnNames.length];
      delimitedColumnNames = new String[columnNames.length];
      final String[] columnsSansIds = new String[columnNames.length - idColumnNames.length];
      delimitedColumnsSansIds = new String[columnsSansIds.length];
      selectableFcInfos = new AttributeInfo[allFcInfos.size()];

      int fieldCount = 0, idCount = 0, sansIdCount = 0;

      for (final AttributeInfo fcInfo : allFcInfos) {
         selectableFcInfos[fieldCount] = fcInfo;
         if (!fcInfo.isJoinFieldWithSecondTable()) {
            columnNames[fieldCount] = fcInfo.getColumnName();
            caseSensitiveColumnNames[fieldCount] = fcInfo.getCaseSensitiveColumnName();
            delimitedColumnNames[fieldCount] = fcInfo.getDelimitedColumnName();
            columnTableNames[fieldCount] = fcInfo.delimitedTableName;
            if (!fcInfo.isIdField) {
               columnsSansIds[sansIdCount] = fcInfo.getColumnName();
               delimitedColumnsSansIds[sansIdCount] = fcInfo.getDelimitedColumnName();
               ++sansIdCount;
            }
            else {
               // Is it a problem that Class.getDeclaredFields() claims the fields are returned unordered?  We count on order.
               idColumnNames[idCount] = fcInfo.getDelimitedColumnName();
               idFieldColumnInfos[idCount] = fcInfo;
               ++idCount;
            }
         }
         ++fieldCount;
      }
      precalculateInsertableColumns();
      precalculateUpdatableColumns();
   }

   private static String readClob(@NotNull final Clob clob) throws IOException, SQLException {
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

   String[] getCaseSensitiveColumnNames() {
      return caseSensitiveColumnNames;
   }

   AttributeInfo[] getInsertableFcInfos() {
      return insertableFcInfosArray;
   }

   AttributeInfo getGeneratedIdFcInfo() {
      // If there is a @GeneratedValue annotation only one @Id field can exist.
      return idFieldColumnInfos[0];
   }

   AttributeInfo[] getUpdatableFcInfos() {
      return updatableFcInfosArray;
   }

   /**
    * Fields in same order as supplied by Type inspection
    */
   AttributeInfo[] getSelectableFcInfos() {
      return selectableFcInfos;
   }

   /**
    * @return Any id field regardless of whether it is auto-generated or not.
    */
   List<AttributeInfo> getIdFcInfos() {
      return idFcInfos;
   }

   boolean hasCompositePrimaryKey() {
      return getIdFcInfos().size() > 1;
   }
}
