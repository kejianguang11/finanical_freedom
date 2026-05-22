package com.financial.freedom.ui.holdings;

import com.financial.freedom.data.local.dao.HoldingDao;
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
public final class EditHoldingViewModel_Factory implements Factory<EditHoldingViewModel> {
  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<BackfillEngine> backfillEngineProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public EditHoldingViewModel_Factory(Provider<HoldingDao> holdingDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.holdingDaoProvider = holdingDaoProvider;
    this.backfillEngineProvider = backfillEngineProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public EditHoldingViewModel get() {
    return newInstance(holdingDaoProvider.get(), backfillEngineProvider.get(), accountManagerProvider.get());
  }

  public static EditHoldingViewModel_Factory create(Provider<HoldingDao> holdingDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new EditHoldingViewModel_Factory(holdingDaoProvider, backfillEngineProvider, accountManagerProvider);
  }

  public static EditHoldingViewModel newInstance(HoldingDao holdingDao,
      BackfillEngine backfillEngine, AccountManager accountManager) {
    return new EditHoldingViewModel(holdingDao, backfillEngine, accountManager);
  }
}
