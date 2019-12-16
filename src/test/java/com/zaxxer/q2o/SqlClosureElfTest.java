package com.zaxxer.q2o;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class SqlClosureElfTest {
   @Test
   public void getInClausePlaceholdersByItems()
   {
      assertThat(Q2Sql.getInClausePlaceholders()).isEqualTo(" ('s0me n0n-ex1st4nt v4luu') ");
      assertThat(Q2Sql.getInClausePlaceholders(0)).isEqualTo(" (?) ");
      assertThat(Q2Sql.getInClausePlaceholders("1")).isEqualTo(" (?) ");
      assertThat(Q2Sql.getInClausePlaceholders("1","2","3","4","5")).isEqualTo(" (?,?,?,?,?) ");
   }

   @Test
   public void getInClausePlaceholdersByCount() {
      assertThat(Q2Sql.getInClausePlaceholdersForCount(0)).isEqualTo(" ('s0me n0n-ex1st4nt v4luu') ");
      assertThat(Q2Sql.getInClausePlaceholdersForCount(1)).isEqualTo(" (?) ");
      assertThat(Q2Sql.getInClausePlaceholdersForCount(5)).isEqualTo(" (?,?,?,?,?) ");
      assertThatIllegalArgumentException().isThrownBy(() -> Q2Sql.getInClausePlaceholdersForCount(-1));
   }
}
