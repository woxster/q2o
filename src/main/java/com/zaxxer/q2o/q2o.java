package com.zaxxer.q2o;

import com.zaxxer.q2o.transaction.TxTransactionManager;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

/** Single point of q2o configuration */
public final class q2o {

   private q2o() {
   }

   /**
    * Use this one if you don't need {@link TransactionManager} tx handling.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @return dataSource that will be used for queries
    */
   public static DataSource initializeTxNone(DataSource dataSource) {
      SqlClosure.setDefaultDataSource(dataSource);
      Q2Obj.q2Object = new Q2Object();
      return dataSource;
   }

   /**
    * Use this one to use simple embedded {@link TransactionManager} implementation for tx handling.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @return dataSource that will be used for queries
    */
   public static DataSource initializeTxSimple(DataSource dataSource) {
      Q2Obj.q2Object = new Q2Object();
      TxTransactionManager txManager = new TxTransactionManager(dataSource);
      return initializeTxCustom(txManager.getTxDataSource(), txManager, txManager);
   }

   /**
    * Use this one if you have custom/provided {@link TransactionManager}, e.g. to run within web app container.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @param txManager the {@link TransactionManager} to use for tx management
    * @param userTx the {@link UserTransaction} to use for tx management together with txManager
    * @return dataSource that will be used for queries
    */
   public static DataSource initializeTxCustom(DataSource dataSource, TransactionManager txManager, UserTransaction userTx) {
      Q2Obj.q2Object = new Q2Object();
      TransactionHelper.setTransactionManager(txManager);
      TransactionHelper.setUserTransaction(userTx);
      return initializeTxNone(dataSource);
   }

   /**
    * To make q2o support spring managed transactions, if available.
    */
   public static DataSource initializeWithSpringTxSupport(DataSource dataSource) {
      Q2Obj.q2Object = new Q2Object();
      Q2Sql.isSpringTxAware = true;
      Q2ObjList.isSpringTxAware = true;
      SqlClosure.isSpringTxAware = true;
      SqlClosure.setDefaultDataSource(dataSource);
      SqlClosure.setDefaultExceptionTranslator(dataSource);
      return dataSource;
   }

   /**
    * You can reset q2o to a fresh state if desired.
    * E.g. if you want to call another initializeXXX method.
    */
   public static void deinitialize() {
      SqlClosure.setDefaultDataSource(null);
      TransactionHelper.setUserTransaction(null);
      TransactionHelper.setTransactionManager(null);
      Q2Obj.q2Object = null;
      Q2Sql.isSpringTxAware = false;
      Q2ObjList.isSpringTxAware = false;
      SqlClosure.isSpringTxAware = false;
      SqlClosure.setDefaultExceptionTranslator(null);
   }

}
