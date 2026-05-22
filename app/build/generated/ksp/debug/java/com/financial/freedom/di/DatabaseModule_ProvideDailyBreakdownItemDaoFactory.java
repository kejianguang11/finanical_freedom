package com.financial.freedom.di;

import com.financial.freedom.data.local.AppDatabase;
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao;
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
public final class DatabaseModule_ProvideDailyBreakdownItemDaoFactory implements Factory<DailyBreakdownItemDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideDailyBreakdownItemDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public DailyBreakdownItemDao get() {
    return provideDailyBreakdownItemDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideDailyBreakdownItemDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideDailyBreakdownItemDaoFactory(dbProvider);
  }

  public static DailyBreakdownItemDao provideDailyBreakdownItemDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideDailyBreakdownItemDao(db));
  }
}
