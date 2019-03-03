package com.zaxxer.q2o;

import java.sql.SQLException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
interface Query<T> {
   T execute() throws SQLException;
}
