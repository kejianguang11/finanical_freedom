package com.financial.freedom.data;

import com.financial.freedom.data.local.dao.DepositDao;
import com.financial.freedom.data.local.dao.ExchangeRateDao;
import com.financial.freedom.data.local.dao.HoldingDao;
import com.financial.freedom.data.local.dao.PriceSnapshotDao;
import com.financial.freedom.data.local.dao.TransactionDao;
import com.financial.freedom.domain.account.AccountManager;
import com.financial.freedom.domain.calculator.BackfillEngine;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class TestDataGenerator_Factory implements Factory<TestDataGenerator> {
  private final Provider<DepositDao> depositDaoProvider;

  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<PriceSnapshotDao> priceSnapshotDaoProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  private final Provider<ExchangeRateDao> exchangeRateDaoProvider;

  private final Provider<BackfillEngine> backfillEngineProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public TestDataGenerator_Factory(Provider<DepositDao> depositDaoProvider,
      Provider<HoldingDao> holdingDaoProvider, Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<TransactionDao> transactionDaoProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.depositDaoProvider = depositDaoProvider;
    this.holdingDaoProvider = holdingDaoProvider;
    this.priceSnapshotDaoProvider = priceSnapshotDaoProvider;
    this.transactionDaoProvider = transactionDaoProvider;
    this.exchangeRateDaoProvider = exchangeRateDaoProvider;
    this.backfillEngineProvider = backfillEngineProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public TestDataGenerator get() {
    return newInstance(depositDaoProvider.get(), holdingDaoProvider.get(), priceSnapshotDaoProvider.get(), transactionDaoProvider.get(), exchangeRateDaoProvider.get(), backfillEngineProvider.get(), accountManagerProvider.get());
  }

  public static TestDataGenerator_Factory create(Provider<DepositDao> depositDaoProvider,
      Provider<HoldingDao> holdingDaoProvider, Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<TransactionDao> transactionDaoProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new TestDataGenerator_Factory(depositDaoProvider, holdingDaoProvider, priceSnapshotDaoProvider, transactionDaoProvider, exchangeRateDaoProvider, backfillEngineProvider, accountManagerProvider);
  }

  public static TestDataGenerator newInstance(DepositDao depositDao, HoldingDao holdingDao,
      PriceSnapshotDao priceSnapshotDao, TransactionDao transactionDao,
      ExchangeRateDao exchangeRateDao, BackfillEngine backfillEngine,
      AccountManager accountManager) {
    return new TestDataGenerator(depositDao, holdingDao, priceSnapshotDao, transactionDao, exchangeRateDao, backfillEngine, accountManager);
  }
}
