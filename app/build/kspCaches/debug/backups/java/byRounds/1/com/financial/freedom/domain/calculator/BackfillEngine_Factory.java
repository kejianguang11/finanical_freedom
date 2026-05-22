package com.financial.freedom.domain.calculator;

import com.financial.freedom.data.local.dao.DailyBreakdownItemDao;
import com.financial.freedom.data.local.dao.DailySummaryDao;
import com.financial.freedom.data.local.dao.DepositDao;
import com.financial.freedom.data.local.dao.ExchangeRateDao;
import com.financial.freedom.data.local.dao.HoldingDao;
import com.financial.freedom.data.local.dao.PriceSnapshotDao;
import com.financial.freedom.data.remote.PriceService;
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
public final class BackfillEngine_Factory implements Factory<BackfillEngine> {
  private final Provider<DepositDao> depositDaoProvider;

  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<DailySummaryDao> summaryDaoProvider;

  private final Provider<DailyBreakdownItemDao> breakdownDaoProvider;

  private final Provider<ExchangeRateDao> exchangeRateDaoProvider;

  private final Provider<PriceSnapshotDao> priceSnapshotDaoProvider;

  private final Provider<PriceService> priceServiceProvider;

  private final Provider<ValuationCalculator> valuationCalculatorProvider;

  private final Provider<InterestCalculator> interestCalculatorProvider;

  public BackfillEngine_Factory(Provider<DepositDao> depositDaoProvider,
      Provider<HoldingDao> holdingDaoProvider, Provider<DailySummaryDao> summaryDaoProvider,
      Provider<DailyBreakdownItemDao> breakdownDaoProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<PriceService> priceServiceProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<InterestCalculator> interestCalculatorProvider) {
    this.depositDaoProvider = depositDaoProvider;
    this.holdingDaoProvider = holdingDaoProvider;
    this.summaryDaoProvider = summaryDaoProvider;
    this.breakdownDaoProvider = breakdownDaoProvider;
    this.exchangeRateDaoProvider = exchangeRateDaoProvider;
    this.priceSnapshotDaoProvider = priceSnapshotDaoProvider;
    this.priceServiceProvider = priceServiceProvider;
    this.valuationCalculatorProvider = valuationCalculatorProvider;
    this.interestCalculatorProvider = interestCalculatorProvider;
  }

  @Override
  public BackfillEngine get() {
    return newInstance(depositDaoProvider.get(), holdingDaoProvider.get(), summaryDaoProvider.get(), breakdownDaoProvider.get(), exchangeRateDaoProvider.get(), priceSnapshotDaoProvider.get(), priceServiceProvider.get(), valuationCalculatorProvider.get(), interestCalculatorProvider.get());
  }

  public static BackfillEngine_Factory create(Provider<DepositDao> depositDaoProvider,
      Provider<HoldingDao> holdingDaoProvider, Provider<DailySummaryDao> summaryDaoProvider,
      Provider<DailyBreakdownItemDao> breakdownDaoProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<PriceService> priceServiceProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<InterestCalculator> interestCalculatorProvider) {
    return new BackfillEngine_Factory(depositDaoProvider, holdingDaoProvider, summaryDaoProvider, breakdownDaoProvider, exchangeRateDaoProvider, priceSnapshotDaoProvider, priceServiceProvider, valuationCalculatorProvider, interestCalculatorProvider);
  }

  public static BackfillEngine newInstance(DepositDao depositDao, HoldingDao holdingDao,
      DailySummaryDao summaryDao, DailyBreakdownItemDao breakdownDao,
      ExchangeRateDao exchangeRateDao, PriceSnapshotDao priceSnapshotDao, PriceService priceService,
      ValuationCalculator valuationCalculator, InterestCalculator interestCalculator) {
    return new BackfillEngine(depositDao, holdingDao, summaryDao, breakdownDao, exchangeRateDao, priceSnapshotDao, priceService, valuationCalculator, interestCalculator);
  }
}
