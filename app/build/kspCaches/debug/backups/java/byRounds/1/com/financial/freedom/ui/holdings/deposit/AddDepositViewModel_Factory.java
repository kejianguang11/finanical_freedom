package com.financial.freedom.ui.holdings.deposit;

import com.financial.freedom.data.local.dao.DepositDao;
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
public final class AddDepositViewModel_Factory implements Factory<AddDepositViewModel> {
  private final Provider<DepositDao> depositDaoProvider;

  private final Provider<BackfillEngine> backfillEngineProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public AddDepositViewModel_Factory(Provider<DepositDao> depositDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.depositDaoProvider = depositDaoProvider;
    this.backfillEngineProvider = backfillEngineProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public AddDepositViewModel get() {
    return newInstance(depositDaoProvider.get(), backfillEngineProvider.get(), accountManagerProvider.get());
  }

  public static AddDepositViewModel_Factory create(Provider<DepositDao> depositDaoProvider,
      Provider<BackfillEngine> backfillEngineProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new AddDepositViewModel_Factory(depositDaoProvider, backfillEngineProvider, accountManagerProvider);
  }

  public static AddDepositViewModel newInstance(DepositDao depositDao,
      BackfillEngine backfillEngine, AccountManager accountManager) {
    return new AddDepositViewModel(depositDao, backfillEngine, accountManager);
  }
}
