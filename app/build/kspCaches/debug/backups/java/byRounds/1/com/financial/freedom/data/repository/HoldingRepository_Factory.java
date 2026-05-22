package com.financial.freedom.data.repository;

import com.financial.freedom.data.local.dao.HoldingDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class HoldingRepository_Factory implements Factory<HoldingRepository> {
  private final Provider<HoldingDao> daoProvider;

  public HoldingRepository_Factory(Provider<HoldingDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public HoldingRepository get() {
    return newInstance(daoProvider.get());
  }

  public static HoldingRepository_Factory create(Provider<HoldingDao> daoProvider) {
    return new HoldingRepository_Factory(daoProvider);
  }

  public static HoldingRepository newInstance(HoldingDao dao) {
    return new HoldingRepository(dao);
  }
}
