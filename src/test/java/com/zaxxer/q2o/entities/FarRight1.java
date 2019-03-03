package com.zaxxer.q2o.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "FAR_RIGHT_TABLE")
public class FarRight1 {
   private int id;
   private String type;

   @Id
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @Column(name = "type")
   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   @Override
   public String toString() {
      return "FarRight1{" +
         "id=" + id +
         ", type='" + type + '\'' +
         '}';
   }
}
