package com.financial.freedom.domain.account;

import android.content.Context;
import com.financial.freedom.data.local.dao.AccountDao;
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao;
import com.financial.freedom.data.local.dao.DailySummaryDao;
import com.financial.freedom.data.local.dao.DepositDao;
import com.financial.freedom.data.local.dao.HoldingDao;
import com.financial.freedom.data.local.dao.PriceSnapshotDao;
import com.financial.freedom.data.local.dao.TransactionDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AccountManager_Factory implements Factory<AccountManager> {
  private final Provider<AccountDao> accountDaoProvider;

  private final Provider<DepositDao> depositDaoProvider;

  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<PriceSnapshotDao> priceSnapshotDaoProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  private final Provider<DailySummaryDao> dailySummaryDaoProvider;

  private final Provider<DailyBreakdownItemDao> dailyBreakdownItemDaoProvider;

  private final Provider<Context> contextProvider;

  public AccountManager_Factory(Provider<AccountDao> accountDaoProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<TransactionDao> transactionDaoProvider,
      Provider<DailySummaryDao> dailySummaryDaoProvider,
      Provider<DailyBreakdownItemDao> dailyBreakdownItemDaoProvider,
      Provider<Context> contextProvider) {
    this.accountDaoProvider = accountDaoProvider;
    this.depositDaoProvider = depositDaoProvider;
    this.holdingDaoProvider = holdingDaoProvider;
    this.priceSnapshotDaoProvider = priceSnapshotDaoProvider;
    this.transactionDaoProvider = transactionDaoProvider;
    this.dailySummaryDaoProvider = dailySummaryDaoProvider;
    this.dailyBreakdownItemDaoProvider = dailyBreakdownItemDaoProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public AccountManager get() {
    return newInstance(accountDaoProvider.get(), depositDaoProvider.get(), holdingDaoProvider.get(), priceSnapshotDaoProvider.get(), transactionDaoProvider.get(), dailySummaryDaoProvider.get(), dailyBreakdownItemDaoProvider.get(), contextProvider.get());
  }

  public static AccountManager_Factory create(Provider<AccountDao> accountDaoProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider,
      Provider<PriceSnapshotDao> priceSnapshotDaoProvider,
      Provider<TransactionDao> transactionDaoProvider,
      Provider<DailySummaryDao> dailySummaryDaoProvider,
      Provider<DailyBreakdownItemDao> dailyBreakdownItemDaoProvider,
      Provider<Context> contextProvider) {
    return new AccountManager_Factory(accountDaoProvider, depositDaoProvider, holdingDaoProvider, priceSnapshotDaoProvider, transactionDaoProvider, dailySummaryDaoProvider, dailyBreakdownItemDaoProvider, contextProvider);
  }

  public static AccountManager newInstance(AccountDao accountDao, DepositDao depositDao,
      HoldingDao holdingDao, PriceSnapshotDao priceSnapshotDao, TransactionDao transactionDao,
      DailySummaryDao dailySummaryDao, DailyBreakdownItemDao dailyBreakdownItemDao,
      Context context) {
    return new AccountManager(accountDao, depositDao, holdingDao, priceSnapshotDao, transactionDao, dailySummaryDao, dailyBreakdownItemDao, context);
  }
}
