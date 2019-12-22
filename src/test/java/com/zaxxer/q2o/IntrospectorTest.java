package com.zaxxer.q2o;

import org.junit.Test;
import org.sansorm.TargetClass1;

import static org.assertj.core.api.Assertions.assertThat;

public class IntrospectorTest
{
   @Test
   public void shouldCacheClassMeta()
   {
      Introspected is1 = Introspected.getInstance(TargetClass1.class);
      Introspected is2 = Introspected.getInstance(TargetClass1.class);
      assertThat(is1).isNotNull();
      assertThat(is1).isSameAs(is2);
   }
}
