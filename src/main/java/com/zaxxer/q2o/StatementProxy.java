package com.zaxxer.q2o;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * A non-closeable Statement.
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.01.20
 */
class StatementProxy implements InvocationHandler {
   private final Statement statement;

   public StatementProxy(Statement statement)
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

   static Statement wrap(Statement statement)
   {
      StatementProxy statementProxy = new StatementProxy(statement);
      return (Statement) Proxy.newProxyInstance(StatementProxy.class.getClassLoader(), new Class[]{Statement.class}, statementProxy);
   }
}
