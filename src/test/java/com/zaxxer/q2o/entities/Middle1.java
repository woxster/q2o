package com.zaxxer.q2o.entities;

import javax.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "MIDDLE_TABLE")
public class Middle1 {
   private int id;
   private String type;
   private int rightId;
   private Right1 right;

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

   @OneToOne
   @JoinColumn(name = "rightId")
   public Right1 getRight() {
      return right;
   }

   public void setRight(Right1 right) {
      this.right = right;
   }

   @Column(name = "rightId")
   public int getRightId() {
      return rightId;
   }

   public void setRightId(int rightId) {
      this.rightId = rightId;
   }

   @Override
   public String toString() {
      return "Middle1{" +
         "id=" + id +
         ", type='" + type + '\'' +
         ", rightId=" + rightId +
         ", right=" + right +
         '}';
   }
}
