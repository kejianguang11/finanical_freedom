package com.financial.freedom.di;

import com.financial.freedom.data.local.AppDatabase;
import com.financial.freedom.data.local.dao.HoldingDao;
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
public final class DatabaseModule_ProvideHoldingDaoFactory implements Factory<HoldingDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvideHoldingDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public HoldingDao get() {
    return provideHoldingDao(dbProvider.get());
  }

  public static DatabaseModule_ProvideHoldingDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvideHoldingDaoFactory(dbProvider);
  }

  public static HoldingDao provideHoldingDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideHoldingDao(db));
  }
}
