package com.zaxxer.q2o;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * A non-closeable Statement.
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.01.20
 */
class PreparedStatementProxy implements InvocationHandler {
   private final PreparedStatement statement;

   public PreparedStatementProxy(PreparedStatement statement)
   {
      this.statement = statement;
   }

   @Override
   public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
   {
      Object ret = null;
      if (!"close".equals(method.getName())) {
         ret = method.invoke(statement, args);
      }
      if (ret instanceof ResultSet) {
         ret = ResultSetProxy.wrap((ResultSet) ret);
      }
      return ret;
   }

   static PreparedStatement wrap(PreparedStatement statement)
   {
      PreparedStatementProxy statementProxy = new PreparedStatementProxy(statement);
      return (PreparedStatement) Proxy.newProxyInstance(PreparedStatementProxy.class.getClassLoader(), new Class[]{PreparedStatement.class}, statementProxy);
   }
}
