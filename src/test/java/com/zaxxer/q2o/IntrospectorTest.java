package com.zaxxer.q2o;

import com.zaxxer.q2o.Introspected;
import com.zaxxer.q2o.Introspector;
import org.junit.Test;
import org.sansorm.TargetClass1;

import static org.assertj.core.api.Assertions.assertThat;

public class IntrospectorTest
{
   @Test
   public void shouldCacheClassMeta()
   {
      Introspected is1 = Introspector.getIntrospected(TargetClass1.class);
      Introspected is2 = Introspector.getIntrospected(TargetClass1.class);
      assertThat(is1).isNotNull();
      assertThat(is1).isSameAs(is2);
   }
}
