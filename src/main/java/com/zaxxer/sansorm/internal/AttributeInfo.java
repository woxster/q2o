package com.zaxxer.sansorm.internal;

import javax.persistence.*;
import java.lang.reflect.*;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Column information about a field
 */
abstract class AttributeInfo
{
   protected final Class<?> clazz;
   protected String name;

   final Field field;
   Class<?> type;

   protected boolean isDelimited;
   protected Boolean updatable;
   protected Boolean insertable;
   protected String columnName;
   /** name without delimiter: name as is; delimited name: name as is with delimiters */
   String columnTableName = "";
   EnumType enumType;
   Map<Object, Object> enumConstants;
   protected AttributeConverter converter;
   protected String caseSensitiveColumnName;
   protected boolean isGeneratedId;
   boolean isIdField;
   protected boolean isJoinColumn;
   protected boolean isTransient;
   protected boolean isEnumerated;
   protected boolean isColumnAnnotated;
   protected String delimitedName;
   protected String fullyQualifiedDelimitedName;
   protected boolean toBeConsidered = true;
   protected boolean isJoinColumnsAnnotated;
   protected boolean isOneToManyAnnotated;
   protected boolean isManyToManyAnnotated;
   protected boolean isManyToOneAnnotated;
   protected boolean isOneToOneAnnotated;
   private boolean joinWithSecondTable;

   public AttributeInfo(final Field field, final Class<?> clazz) {
      this.field = field;
      this.clazz = clazz;
      extractFieldName(field);
      if (isToBeConsidered()) {
         adjustType(field.getType());
         extractAnnotations();
         if (toBeConsidered) {
            processFieldAnnotations();
            this.fullyQualifiedDelimitedName =
               columnTableName.isEmpty() ? delimitedName : columnTableName + "." + delimitedName;
         }
      }
   }

   protected abstract void extractFieldName(final Field field);

   private void adjustType(final Class<?> type) {
      if (type == null) {
         throw new IllegalArgumentException("AccessibleObject has to be of type Field or Method.");
      }
      // remap safe conversions
      if (type == Date.class) {
         this.type = Timestamp.class;
      }
      else if (type == int.class) {
         this.type = Integer.class;
      }
      else if (type == long.class) {
         this.type = Long.class;
      }
      else {
         this.type = type;
      }
   }

   private void processFieldAnnotations()
   {
      if (isColumnAnnotated) {
         processColumnAnnotation();
      }
      else  {
         if (isJoinColumn) {
            processJoinColumnAnnotation();
         }
         else {
            if (isIdField) {
               // @Id without @Column annotation, so preserve case of property name.
               setColumnName(name);
            }
            else {
               // CLARIFY Dead code? Never reached by tests.
               setColumnName(name);
            }
         }
      }
      processConvertAnnotation();
   }

   private void extractAnnotations() {
      final Id idAnnotation = extractIdAnnotation();
      if (idAnnotation != null) {
         isIdField = true;
         GeneratedValue generatedAnnotation = extractGeneratedValueAnnotation();
         isGeneratedId = (generatedAnnotation != null);
      }

      final Enumerated enumAnnotation = extractEnumeratedAnnotation();
      if (enumAnnotation != null) {
         isEnumerated = true;
         this.setEnumConstants(enumAnnotation.value());
      }
      final JoinColumn joinColumnAnnotation = extractJoinColumnAnnotation();
      if (joinColumnAnnotation != null) {
         isJoinColumn = true;
      }
      final Transient transientAnnotation = extractTransientAnnotation();
      if (transientAnnotation != null) {
         isTransient = true;
         toBeConsidered = false;
      }
      final Column columnAnnotation = extractColumnAnnotation();
      if (columnAnnotation != null) {
         isColumnAnnotated = true;
      }
      final JoinColumns joinColumns = extractJoinColumnsAnnotation();
      if (joinColumns != null) {
         isJoinColumnsAnnotated = true;
         toBeConsidered = false;
      }
      // relationship annotations can exist without @JoinColumn annotation
      extractRelationship();
   }

   private void extractRelationship() {
      final OneToMany oneToMany = extractOneToManyAnnotation();
      if (oneToMany != null) {
         isOneToManyAnnotated = true;
         toBeConsidered = false;
         initializeJoinWithSecondTable();
      }
      else {
         final ManyToMany manyToMany = extractManyToManyAnnotation();
         if (manyToMany != null) {
            isManyToManyAnnotated = true;
            toBeConsidered = false;
            initializeJoinWithSecondTable();
         }
         else {
            final ManyToOne manyToOne = extractManyToOneAnnotation();
            if (manyToOne != null) {
               isManyToOneAnnotated = true;
               initializeJoinWithSecondTable();
            }
            else {
               final OneToOne oneToOne = extractOneToOneAnnotation();
               if (oneToOne != null) {
                  isOneToOneAnnotated = true;
                  initializeJoinWithSecondTable();
               }
            }
         }
      }
   }

   /**
    * Sets {@link #joinWithSecondTable}
    */
   private void initializeJoinWithSecondTable() {
      // Is also true with @OneToMany fields. Type is Collection then.
      if (type != clazz) {
         if (!Collection.class.isAssignableFrom(type)) {
            joinWithSecondTable = true;
         }
         else {
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            Type typeArg = genericType.getActualTypeArguments()[0];
            if (typeArg != clazz) {
               joinWithSecondTable = true;
            }
         }
      }
   }

   protected abstract OneToOne extractOneToOneAnnotation();

   protected abstract ManyToOne extractManyToOneAnnotation();

   protected abstract ManyToMany extractManyToManyAnnotation();

   protected abstract OneToMany extractOneToManyAnnotation();

   protected abstract JoinColumns extractJoinColumnsAnnotation();

   protected abstract Transient extractTransientAnnotation();

   protected abstract JoinColumn extractJoinColumnAnnotation();

   protected abstract Enumerated extractEnumeratedAnnotation();

   protected abstract GeneratedValue extractGeneratedValueAnnotation();

   protected abstract Id extractIdAnnotation();

   private void processConvertAnnotation()  {
      final Convert convertAnnotation = extractConvertAnnotation();
      if (convertAnnotation != null) {
         final Class<?> converterClass = convertAnnotation.converter();
         if (!AttributeConverter.class.isAssignableFrom(converterClass)) {
            throw new RuntimeException(
               "Convert annotation only supports converters implementing AttributeConverter");
         }
         try {
            setConverter((AttributeConverter) converterClass.newInstance());
         }
         catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
         }
      }
   }

   protected abstract Convert extractConvertAnnotation();

   /**
    * Processes &#64;Column annotated fields.
    */
   private void processColumnAnnotation() {
      final Column columnAnnotation = extractColumnAnnotation();
      final String columnName = columnAnnotation.name();
      setColumnName(columnName);

      this.columnTableName = columnAnnotation.table();
      insertable = columnAnnotation.insertable();
      updatable = columnAnnotation.updatable();
   }

   protected abstract Column extractColumnAnnotation();

   protected void processJoinColumnAnnotation() {
      final JoinColumn joinColumnAnnotation = extractJoinColumnAnnotation();
      setColumnName(joinColumnAnnotation.name());
      insertable = joinColumnAnnotation.insertable();
      updatable = joinColumnAnnotation.updatable();
   }

   private boolean isNotDelimited(final String columnName) {
      return !columnName.startsWith("\"") || !columnName.endsWith("\"");
   }

   private void setColumnName(final String columnName) {
      final String colName = columnName.isEmpty()
         ? name // as per EJB specification, empty name in Column "defaults to the property or field name"
         : columnName;
      if (isNotDelimited(colName)) {
         this.columnName = colName.toLowerCase();
         caseSensitiveColumnName = colName;
         delimitedName = colName;
      }
      else {
         this.columnName = colName.substring(1, colName.length() - 1);
         caseSensitiveColumnName = this.columnName;
         delimitedName = colName;
         isDelimited = true;
      }
   }

   <T extends Enum<?>> void setEnumConstants(final EnumType enumType)
   {
      this.enumType = enumType;
      enumConstants = new HashMap<>();
      @SuppressWarnings("unchecked")
      final T[] enums = (T[]) this.type.getEnumConstants();
      for (T enumConst : enums) {
         Object key = (this.enumType == EnumType.ORDINAL ? enumConst.ordinal() : enumConst.name());
         enumConstants.put(key, enumConst);
      }
   }

   @Override
   public String toString()
   {
      return name + "->" + getColumnName();
   }

   public void setConverter(final AttributeConverter converter) {
      this.converter = converter;
   }

   public AttributeConverter getConverter() {
      return converter;
   }

   boolean isSelfJoinField() {
      return isJoinColumn && type == clazz;
   }

   /** name without delimiter: lower cased; delimited name: name as is without delimiters */
   public String getColumnName() {
      return columnName;
   }

   public String getName() {
      return name;
   }

   /**
    * @return If set &#64;Column name value else property name. In case of delimited fields without delimiters.
    */
   public String getCaseSensitiveColumnName() {
      return caseSensitiveColumnName;
   }

   /**
    *
    * @return case sensitive column name. In case of delimited fields surrounded by delimiters.
    */
   public String getDelimitedColumnName() {
      return delimitedName;
   }

   /**
    *
    * @param tablePrefix Ignored when field has a non empty table element.
    */
   public String getFullyQualifiedDelimitedFieldName(final String ... tablePrefix) {
      return columnTableName.isEmpty() && tablePrefix.length > 0
         ? tablePrefix[0] + "." + fullyQualifiedDelimitedName
         : fullyQualifiedDelimitedName;
   }

   public boolean isDelimited() {
      return isDelimited;
   }

   public boolean isEnumerated() {
      return isEnumerated;
   }

   /**
    *
    * @return null: no @Column annotation. true: @Column annotation. false @Column with updatable = false or join with second table.
    */
   Boolean isUpdatable() {
      // Not as ternary expression or a NPE is thrown?!?
      if (!joinWithSecondTable) {
         return updatable;
      }
      else {
         return false;
      }
   }

   /**
    *
    * @return null: no @Column annotation. true: @Column annotation. false @Column with insertable = false or join with second table.
    */
   Boolean isInsertable() {
      // Not as ternary expression or a NPE is thrown?!?
      if (!joinWithSecondTable) {
         return insertable;
      }
      else {
         return false;
      }
   }

   public abstract Object getValue(final Object target) throws IllegalAccessException, InvocationTargetException;


   protected Object idValueFromParentEntity(final Object obj) throws IllegalAccessException, InvocationTargetException {
      if (obj != null) {
         final Introspected introspected = new Introspected(obj.getClass());
         final AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
         return generatedIdFcInfo.getValue(obj);
      }
      else {
         return null;
      }
   }

   public abstract void setValue(final Object target, final Object value) throws IllegalAccessException;

   boolean isTransient() {
      return isTransient;
   }

   protected Object idValueToParentEntity(final Class<?> clazz, final Object value) throws IllegalAccessException, InstantiationException {
      return idValueToParentEntity(clazz.newInstance(), value);
   }

   protected Object idValueToParentEntity(final Object target, final Object value) throws InstantiationException, IllegalAccessException {
      final Object obj = target.getClass().newInstance();
      final Introspected introspected = new Introspected(obj.getClass());
      final AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
      generatedIdFcInfo.setValue(obj, value);
      return obj;
   }

   /**
    * Can be overridden.
    */
   boolean isToBeConsidered() {
      return !isTransient && toBeConsidered;
   }

   public boolean isJoinFieldWithSecondTable() {
      return joinWithSecondTable;
   }
}
