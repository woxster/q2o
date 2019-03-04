package com.zaxxer.q2o;

import java.sql.SQLException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-04
 */
interface SQLExceptionTranslator<T extends RuntimeException> {
   T translate(String task, String sql, SQLException ex);
}
