package com.financial.freedom.di;

import com.financial.freedom.data.local.AppDatabase;
import com.financial.freedom.data.local.dao.DailySummaryDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class DatabaseModule_ProvideDailySummaryDaoFactory implements Factory<DailySummaryDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideDailySummaryDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DailySummaryDao get() {
    return provideDailySummaryDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDailySummaryDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideDailySummaryDaoFactory(dbProvider);
  }

  public static DailySummaryDao provideDailySummaryDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDailySummaryDao(db));
  }
}
