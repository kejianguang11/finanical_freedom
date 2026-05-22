package com.financial.freedom.ui.holdings;

import com.financial.freedom.data.local.dao.HoldingDao;
import com.financial.freedom.data.remote.PriceService;
import com.financial.freedom.domain.account.AccountManager;
import com.financial.freedom.domain.calculator.BackfillEngine;
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
public final class AddHoldingViewModel_Factory implements Factory<AddHoldingViewModel> {
  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<PriceService> priceServiceProvider;

  private final Provider<BackfillEngine> backfillEngineProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public AddHoldingViewModel_Factory(Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceService> priceServiceProvider, Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.holdingDaoProvider = holdingDaoProvider;
    this.priceServiceProvider = priceServiceProvider;
    this.backfillEngineProvider = backfillEngineProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public AddHoldingViewModel get() {
    return newInstance(holdingDaoProvider.get(), priceServiceProvider.get(), backfillEngineProvider.get(), accountManagerProvider.get());
  }

  public static AddHoldingViewModel_Factory create(Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceService> priceServiceProvider, Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new AddHoldingViewModel_Factory(holdingDaoProvider, priceServiceProvider, backfillEngineProvider, accountManagerProvider);
  }

  public static AddHoldingViewModel newInstance(HoldingDao holdingDao, PriceService priceService,
      BackfillEngine backfillEngine, AccountManager accountManager) {
    return new AddHoldingViewModel(holdingDao, priceService, backfillEngine, accountManager);
  }
}
