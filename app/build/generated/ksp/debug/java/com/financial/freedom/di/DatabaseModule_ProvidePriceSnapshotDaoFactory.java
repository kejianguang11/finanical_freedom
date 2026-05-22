package com.financial.freedom.di;

import com.financial.freedom.data.local.AppDatabase;
import com.financial.freedom.data.local.dao.PriceSnapshotDao;
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
public final class DatabaseModule_ProvidePriceSnapshotDaoFactory implements Factory<PriceSnapshotDao> {
  private final Provider<AppDatabase> dbProvider;

  public DatabaseModule_ProvidePriceSnapshotDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PriceSnapshotDao get() {
    return providePriceSnapshotDao(dbProvider.get());
  }

  public static DatabaseModule_ProvidePriceSnapshotDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new DatabaseModule_ProvidePriceSnapshotDaoFactory(dbProvider);
  }

  public static PriceSnapshotDao providePriceSnapshotDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePriceSnapshotDao(db));
  }
}
