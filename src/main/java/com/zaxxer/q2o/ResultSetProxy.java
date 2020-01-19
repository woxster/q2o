package com.zaxxer.q2o;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.01.20
 */
public class ResultSetProxy implements InvocationHandler {

   private final ResultSet resultSet;

   public ResultSetProxy(ResultSet resultSet)
   {
      this.resultSet = resultSet;
   }

   @Override
   public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
   {
      Object ret = null;
      if (!"close".equals(method.getName())) {
         ret = method.invoke(resultSet, args);
      }
      return ret;
   }

   static ResultSet wrap(ResultSet resultSet)
   {
      ResultSetProxy proxy = new ResultSetProxy(resultSet);
      return (ResultSet) Proxy.newProxyInstance(ResultSetProxy.class.getClassLoader(), new Class[]{ResultSet.class}, proxy);
   }
}
