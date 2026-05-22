package com.financial.freedom.ui.settings;

import android.content.Context;
import com.financial.freedom.data.TestDataGenerator;
import com.financial.freedom.data.csv.CsvExporter;
import com.financial.freedom.data.csv.CsvImporter;
import com.financial.freedom.data.local.AppDatabase;
import com.financial.freedom.data.local.dao.ExchangeRateDao;
import com.financial.freedom.domain.account.AccountManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<Context> contextProvider;

  private final Provider<CsvExporter> csvExporterProvider;

  private final Provider<CsvImporter> csvImporterProvider;

  private final Provider<ExchangeRateDao> exchangeRateDaoProvider;

  private final Provider<AppDatabase> databaseProvider;

  private final Provider<AccountManager> accountManagerProvider;

  private final Provider<TestDataGenerator> testDataGeneratorProvider;

  public SettingsViewModel_Factory(Provider<Context> contextProvider,
      Provider<CsvExporter> csvExporterProvider, Provider<CsvImporter> csvImporterProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider, Provider<AppDatabase> databaseProvider,
      Provider<AccountManager> accountManagerProvider,
      Provider<TestDataGenerator> testDataGeneratorProvider) {
    this.contextProvider = contextProvider;
    this.csvExporterProvider = csvExporterProvider;
    this.csvImporterProvider = csvImporterProvider;
    this.exchangeRateDaoProvider = exchangeRateDaoProvider;
    this.databaseProvider = databaseProvider;
    this.accountManagerProvider = accountManagerProvider;
    this.testDataGeneratorProvider = testDataGeneratorProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(contextProvider.get(), csvExporterProvider.get(), csvImporterProvider.get(), exchangeRateDaoProvider.get(), databaseProvider.get(), accountManagerProvider.get(), testDataGeneratorProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<Context> contextProvider,
      Provider<CsvExporter> csvExporterProvider, Provider<CsvImporter> csvImporterProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider, Provider<AppDatabase> databaseProvider,
      Provider<AccountManager> accountManagerProvider,
      Provider<TestDataGenerator> testDataGeneratorProvider) {
    return new SettingsViewModel_Factory(contextProvider, csvExporterProvider, csvImporterProvider, exchangeRateDaoProvider, databaseProvider, accountManagerProvider, testDataGeneratorProvider);
  }

  public static SettingsViewModel newInstance(Context context, CsvExporter csvExporter,
      CsvImporter csvImporter, ExchangeRateDao exchangeRateDao, AppDatabase database,
      AccountManager accountManager, TestDataGenerator testDataGenerator) {
    return new SettingsViewModel(context, csvExporter, csvImporter, exchangeRateDao, database, accountManager, testDataGenerator);
  }
}
