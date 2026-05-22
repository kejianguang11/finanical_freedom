package com.financial.freedom.data.csv;

import android.content.Context;
import com.financial.freedom.data.local.dao.DepositDao;
import com.financial.freedom.data.local.dao.HoldingDao;
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
public final class CsvExporter_Factory implements Factory<CsvExporter> {
  private final Provider<Context> contextProvider;

  private final Provider<DepositDao> depositDaoProvider;

  private final Provider<HoldingDao> holdingDaoProvider;

  private final Provider<TransactionDao> transactionDaoProvider;

  public CsvExporter_Factory(Provider<Context> contextProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider,
      Provider<TransactionDao> transactionDaoProvider) {
    this.contextProvider = contextProvider;
    this.depositDaoProvider = depositDaoProvider;
    this.holdingDaoProvider = holdingDaoProvider;
    this.transactionDaoProvider = transactionDaoProvider;
  }

  @Override
  public CsvExporter get() {
    return newInstance(contextProvider.get(), depositDaoProvider.get(), holdingDaoProvider.get(), transactionDaoProvider.get());
  }

  public static CsvExporter_Factory create(Provider<Context> contextProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider,
      Provider<TransactionDao> transactionDaoProvider) {
    return new CsvExporter_Factory(contextProvider, depositDaoProvider, holdingDaoProvider, transactionDaoProvider);
  }

  public static CsvExporter newInstance(Context context, DepositDao depositDao,
      HoldingDao holdingDao, TransactionDao transactionDao) {
    return new CsvExporter(context, depositDao, holdingDao, transactionDao);
  }
}
