package com.zaxxer.q2o.converters;

import javax.persistence.AttributeConverter;
import java.sql.Timestamp;
import java.util.Date;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 20.12.19
 */
public class DateTimestampConverter<X,Y> implements AttributeConverter<Date, Timestamp> {
   @Override
   public Timestamp convertToDatabaseColumn(final Date attribute) {
      return attribute != null ? new Timestamp(attribute.getTime())
                                 : null;
   }

   @Override
   public Date convertToEntityAttribute(final Timestamp dbData) {
      return dbData != null ? new Date(dbData.getTime())
                              : null;
   }
}
