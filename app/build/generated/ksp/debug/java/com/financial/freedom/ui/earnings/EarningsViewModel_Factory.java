package com.financial.freedom.ui.earnings;

import com.financial.freedom.data.repository.SummaryRepository;
import com.financial.freedom.domain.account.AccountManager;
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
public final class EarningsViewModel_Factory implements Factory<EarningsViewModel> {
  private final Provider<SummaryRepository> summaryRepositoryProvider;

  private final Provider<AccountManager> accountManagerProvider;

  public EarningsViewModel_Factory(Provider<SummaryRepository> summaryRepositoryProvider,
      Provider<AccountManager> accountManagerProvider) {
    this.summaryRepositoryProvider = summaryRepositoryProvider;
    this.accountManagerProvider = accountManagerProvider;
  }

  @Override
  public EarningsViewModel get() {
    return newInstance(summaryRepositoryProvider.get(), accountManagerProvider.get());
  }

  public static EarningsViewModel_Factory create(
      Provider<SummaryRepository> summaryRepositoryProvider,
      Provider<AccountManager> accountManagerProvider) {
    return new EarningsViewModel_Factory(summaryRepositoryProvider, accountManagerProvider);
  }

  public static EarningsViewModel newInstance(SummaryRepository summaryRepository,
      AccountManager accountManager) {
    return new EarningsViewModel(summaryRepository, accountManager);
  }
}
