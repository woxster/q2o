package com.zaxxer.q2o.entities;

import javax.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "RIGHT_TABLE")
public class Right1 {
   private int id;
   private String type;
   private int farRightId;
   private FarRight1 farRight1;

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
      return "Right1{" +
         "id=" + id +
         ", type='" + type + '\'' +
         ", farRightId=" + farRightId +
         ", farRight1=" + farRight1 +
         '}';
   }

   public int getFarRightId() {
      return farRightId;
   }

   public void setFarRightId(int farRightId) {
      this.farRightId = farRightId;
   }

   @OneToOne
   @JoinColumn(name = "farRightId")
   public FarRight1 getFarRight1() {
      return farRight1;
   }

   public void setFarRight1(FarRight1 right) {
      this.farRight1 = right;
   }
}
