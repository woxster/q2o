package com.zaxxer.q2o;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-04
 */
class SQLExceptionTranslatorSpring implements SQLExceptionTranslator<DataAccessException> {

   SQLErrorCodeSQLExceptionTranslator translator;

   public SQLExceptionTranslatorSpring(DataSource dataSource) {
      translator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
   }

   @Override
   public DataAccessException translate(final String task, final String sql, final SQLException ex) {
      return translator.translate(task, sql, ex);
   }
}
