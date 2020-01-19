package com.zaxxer.q2o;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Defers closing of a connection's statements until closing the connection. A solution for MySQL to support reading Lobs not only until a statement is closed but until the end of a transaction.
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.01.20
 */
public class Q2ODataSource implements InvocationHandler {

   private final DataSource dataSource;

   public Q2ODataSource(DataSource dataSource)
   {
      this.dataSource = dataSource;
   }

   @Override
   public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
   {
      Object ret;
      if ("getConnection".equals(method.getName())) {
         ret = method.invoke(dataSource, args);
         ret = ConnectionProxy.wrap((Connection) ret);

      }
      else {
         ret = method.invoke(dataSource, args);
      }
      return ret;
   }

   static DataSource wrap(final DataSource dataSource)
   {
      Q2ODataSource handler = new Q2ODataSource(dataSource);
      return (DataSource) Proxy.newProxyInstance(Q2ODataSource.class.getClassLoader(), new Class[] { DataSource.class }, handler);
   }
}
