package com.zaxxer.q2o.entities;

import javax.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity(name = "LEFT_TABLE")
public class Left2 {
   private int id;
   private String type;
   private Right2 right;

   @Id
   @GeneratedValue
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @OneToOne
   @JoinColumn(name = "id")
   public Right2 getRight() {
      return right;
   }

   public void setRight(Right2 right) {
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

   @Override
   public String toString() {
      return "Left{" +
         "id=" + id +
         ", type='" + type + '\'' +
         ", right=" + right +
         '}';
   }
}
