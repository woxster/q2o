package com.zaxxer.q2o;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 20.12.19
 */
class SqlTypesResolver {
   static Map<Integer, String> codeToName = new HashMap<Integer, String>();
   static {
      for (Field field : Types.class.getFields()) {
         try {
            codeToName.put((Integer)field.get(null), field.getName());
         }
         catch (IllegalAccessException e) {
            e.printStackTrace();
         }
      }
   }
}
