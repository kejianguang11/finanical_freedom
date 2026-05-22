package com.financial.freedom.ui.holdings;

import com.financial.freedom.data.local.dao.PriceSnapshotDao;
import com.financial.freedom.data.repository.DepositRepository;
import com.financial.freedom.data.repository.ExchangeRateRepository;
import com.financial.freedom.data.repository.HoldingRepository;
import com.financial.freedom.domain.account.AccountManager;
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
public final class HoldingsViewModel_Factory implements Factory<HoldingsViewModel> {
  private final Provider<DepositRepository> depositRepositoryProvider;

  private final Provider<HoldingRepository> holdingRepositoryProvider;

  private final Provider<ExchangeRateRepository> exchangeRateRepositoryProvider;

  private final Provider<PriceSnapshotDao> priceSnapshotDaoProvider;

  private final Provider<ValuationCalculator> valuationCalculatorProvider;

  private final Provider<InterestCalculator> interestCalculatorProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public HoldingsViewModel_Factory(Provider<DepositRepository> depositRepositoryProvider,
      Provider<HoldingRepository> holdingRepositoryProvider,
      Provider<ExchangeRateRepository> exchangeRateRepositoryProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<InterestCalculator> interestCalculatorProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.depositRepositoryProvider = depositRepositoryProvider;
    this.holdingRepositoryProvider = holdingRepositoryProvider;
    this.exchangeRateRepositoryProvider = exchangeRateRepositoryProvider;
    this.priceSnapshotDaoProvider = priceSnapshotDaoProvider;
    this.valuationCalculatorProvider = valuationCalculatorProvider;
    this.interestCalculatorProvider = interestCalculatorProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public HoldingsViewModel get() {
    return newInstance(depositRepositoryProvider.get(), holdingRepositoryProvider.get(), exchangeRateRepositoryProvider.get(), priceSnapshotDaoProvider.get(), valuationCalculatorProvider.get(), interestCalculatorProvider.get(), accountManagerProvider.get());
  }

  public static HoldingsViewModel_Factory create(
      Provider<DepositRepository> depositRepositoryProvider,
      Provider<HoldingRepository> holdingRepositoryProvider,
      Provider<ExchangeRateRepository> exchangeRateRepositoryProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<ValuationCalculator> valuationCalculatorProvider,
      Provider<InterestCalculator> interestCalculatorProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new HoldingsViewModel_Factory(depositRepositoryProvider, holdingRepositoryProvider, exchangeRateRepositoryProvider, priceSnapshotDaoProvider, valuationCalculatorProvider, interestCalculatorProvider, accountManagerProvider);
  }

  public static HoldingsViewModel newInstance(DepositRepository depositRepository,
      HoldingRepository holdingRepository, ExchangeRateRepository exchangeRateRepository,
      PriceSnapshotDao priceSnapshotDao, ValuationCalculator valuationCalculator,
      InterestCalculator interestCalculator, AccountManager accountManager) {
    return new HoldingsViewModel(depositRepository, holdingRepository, exchangeRateRepository, priceSnapshotDao, valuationCalculator, interestCalculator, accountManager);
  }
}
