package org.sansorm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;

@Entity // Entity annotation
@Table // no explicit table name
public class TargetClass2 extends BaseClass
{
   @Column // no explicit column name
   private Date someDate; // camelCase

   public TargetClass2()
   {
   }

   public TargetClass2(Date someDate, String string)
   {
      this.someDate = someDate;
      this.string = string;
   }

   public Date getSomeDate()
   {
      return someDate;
   }
}
