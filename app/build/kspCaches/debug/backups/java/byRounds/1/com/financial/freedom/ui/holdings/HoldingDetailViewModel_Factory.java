package com.financial.freedom.ui.holdings;

import com.financial.freedom.data.local.dao.HoldingDao;
import com.financial.freedom.data.local.dao.PriceSnapshotDao;
import com.financial.freedom.data.local.dao.TransactionDao;
import com.financial.freedom.data.remote.PriceService;
import com.financial.freedom.domain.account.AccountManager;
import com.financial.freedom.domain.calculator.BackfillEngine;
import com.financial.freedom.domain.calculator.ValuationCalculator;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class HoldingDetailViewModel_Factory implements Factory<HoldingDetailViewModel> {
  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<PriceSnapshotDao> priceSnapshotDaoProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  private final Provider<PriceService> priceServiceProvider;

  private final Provider<ValuationCalculator> valuationCalculatorProvider;

  private final Provider<BackfillEngine> backfillEngineProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public HoldingDetailViewModel_Factory(Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<TransactionDao> transactionDaoProvider, Provider<PriceService> priceServiceProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.holdingDaoProvider = holdingDaoProvider;
    this.priceSnapshotDaoProvider = priceSnapshotDaoProvider;
    this.transactionDaoProvider = transactionDaoProvider;
    this.priceServiceProvider = priceServiceProvider;
    this.valuationCalculatorProvider = valuationCalculatorProvider;
    this.backfillEngineProvider = backfillEngineProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public HoldingDetailViewModel get() {
    return newInstance(holdingDaoProvider.get(), priceSnapshotDaoProvider.get(), transactionDaoProvider.get(), priceServiceProvider.get(), valuationCalculatorProvider.get(), backfillEngineProvider.get(), accountManagerProvider.get());
  }

  public static HoldingDetailViewModel_Factory create(Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<TransactionDao> transactionDaoProvider, Provider<PriceService> priceServiceProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new HoldingDetailViewModel_Factory(holdingDaoProvider, priceSnapshotDaoProvider, transactionDaoProvider, priceServiceProvider, valuationCalculatorProvider, backfillEngineProvider, accountManagerProvider);
  }

  public static HoldingDetailViewModel newInstance(HoldingDao holdingDao,
      PriceSnapshotDao priceSnapshotDao, TransactionDao transactionDao, PriceService priceService,
      ValuationCalculator valuationCalculator, BackfillEngine backfillEngine,
      AccountManager accountManager) {
    return new HoldingDetailViewModel(holdingDao, priceSnapshotDao, transactionDao, priceService, valuationCalculator, backfillEngine, accountManager);
  }
}
