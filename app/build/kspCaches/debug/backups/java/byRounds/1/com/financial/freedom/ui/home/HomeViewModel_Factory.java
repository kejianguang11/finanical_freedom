package com.financial.freedom.ui.home;

import com.financial.freedom.data.local.dao.DailyBreakdownItemDao;
import com.financial.freedom.data.local.dao.DepositDao;
import com.financial.freedom.data.local.dao.ExchangeRateDao;
import com.financial.freedom.data.local.dao.HoldingDao;
import com.financial.freedom.data.local.dao.PriceSnapshotDao;
import com.financial.freedom.data.remote.PriceService;
import com.financial.freedom.data.repository.SummaryRepository;
import com.financial.freedom.domain.account.AccountManager;
import com.financial.freedom.domain.calculator.BackfillEngine;
import com.financial.freedom.domain.calculator.InterestCalculator;
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<SummaryRepository> summaryRepositoryProvider;

  private final Provider<DepositDao> depositDaoProvider;

  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<PriceSnapshotDao> priceSnapshotDaoProvider;

  private final Provider<ExchangeRateDao> exchangeRateDaoProvider;

  private final Provider<DailyBreakdownItemDao> breakdownDaoProvider;

  private final Provider<BackfillEngine> backfillEngineProvider;

  private final Provider<ValuationCalculator> valuationCalculatorProvider;

  private final Provider<InterestCalculator> interestCalculatorProvider;

  private final Provider<PriceService> priceServiceProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public HomeViewModel_Factory(Provider<SummaryRepository> summaryRepositoryProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider,
      Provider<DailyBreakdownItemDao> breakdownDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<InterestCalculator> interestCalculatorProvider,
      Provider<PriceService> priceServiceProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.summaryRepositoryProvider = summaryRepositoryProvider;
    this.depositDaoProvider = depositDaoProvider;
    this.holdingDaoProvider = holdingDaoProvider;
    this.priceSnapshotDaoProvider = priceSnapshotDaoProvider;
    this.exchangeRateDaoProvider = exchangeRateDaoProvider;
    this.breakdownDaoProvider = breakdownDaoProvider;
    this.backfillEngineProvider = backfillEngineProvider;
    this.valuationCalculatorProvider = valuationCalculatorProvider;
    this.interestCalculatorProvider = interestCalculatorProvider;
    this.priceServiceProvider = priceServiceProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(summaryRepositoryProvider.get(), depositDaoProvider.get(), holdingDaoProvider.get(), priceSnapshotDaoProvider.get(), exchangeRateDaoProvider.get(), breakdownDaoProvider.get(), backfillEngineProvider.get(), valuationCalculatorProvider.get(), interestCalculatorProvider.get(), priceServiceProvider.get(), accountManagerProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<SummaryRepository> summaryRepositoryProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider,
      Provider<DailyBreakdownItemDao> breakdownDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<InterestCalculator> interestCalculatorProvider,
      Provider<PriceService> priceServiceProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new HomeViewModel_Factory(summaryRepositoryProvider, depositDaoProvider, holdingDaoProvider, priceSnapshotDaoProvider, exchangeRateDaoProvider, breakdownDaoProvider, backfillEngineProvider, valuationCalculatorProvider, interestCalculatorProvider, priceServiceProvider, accountManagerProvider);
  }

  public static HomeViewModel newInstance(SummaryRepository summaryRepository,
      DepositDao depositDao, HoldingDao holdingDao, PriceSnapshotDao priceSnapshotDao,
      ExchangeRateDao exchangeRateDao, DailyBreakdownItemDao breakdownDao,
      BackfillEngine backfillEngine, ValuationCalculator valuationCalculator,
      InterestCalculator interestCalculator, PriceService priceService,
      AccountManager accountManager) {
    return new HomeViewModel(summaryRepository, depositDao, holdingDao, priceSnapshotDao, exchangeRateDao, breakdownDao, backfillEngine, valuationCalculator, interestCalculator, priceService, accountManager);
  }
}
