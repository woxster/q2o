package com.zaxxer.q2o.entities;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Table;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.05.18
 */
@Table(name = "\"Test Class\"")
public class DelimitedFields {
   @javax.persistence.Id
   @GeneratedValue
   public
   int Id;
   @Column(name = "\"Delimited field name\"")
   String delimitedFieldName = "delimited field value";
   @Column(name = "Default_Case")
   public
   String defaultCase = "default case value";
}
