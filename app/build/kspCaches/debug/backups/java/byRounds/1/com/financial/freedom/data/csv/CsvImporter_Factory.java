package com.financial.freedom.data.csv;

import android.content.Context;
import com.financial.freedom.data.local.dao.DepositDao;
import com.financial.freedom.data.local.dao.HoldingDao;
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
public final class CsvImporter_Factory implements Factory<CsvImporter> {
  private final Provider<Context> contextProvider;

  private final Provider<DepositDao> depositDaoProvider;

  private final Provider<HoldingDao> holdingDaoProvider;

  public CsvImporter_Factory(Provider<Context> contextProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider) {
    this.contextProvider = contextProvider;
    this.depositDaoProvider = depositDaoProvider;
    this.holdingDaoProvider = holdingDaoProvider;
  }

  @Override
  public CsvImporter get() {
    return newInstance(contextProvider.get(), depositDaoProvider.get(), holdingDaoProvider.get());
  }

  public static CsvImporter_Factory create(Provider<Context> contextProvider,
      Provider<DepositDao> depositDaoProvider, Provider<HoldingDao> holdingDaoProvider) {
    return new CsvImporter_Factory(contextProvider, depositDaoProvider, holdingDaoProvider);
  }

  public static CsvImporter newInstance(Context context, DepositDao depositDao,
      HoldingDao holdingDao) {
    return new CsvImporter(context, depositDao, holdingDao);
  }
}
