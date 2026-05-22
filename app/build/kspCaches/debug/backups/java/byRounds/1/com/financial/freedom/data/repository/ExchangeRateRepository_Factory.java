package com.financial.freedom.data.repository;

import com.financial.freedom.data.local.dao.ExchangeRateDao;
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
public final class ExchangeRateRepository_Factory implements Factory<ExchangeRateRepository> {
  private final Provider<ExchangeRateDao> daoProvider;

  public ExchangeRateRepository_Factory(Provider<ExchangeRateDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public ExchangeRateRepository get() {
    return newInstance(daoProvider.get());
  }

  public static ExchangeRateRepository_Factory create(Provider<ExchangeRateDao> daoProvider) {
    return new ExchangeRateRepository_Factory(daoProvider);
  }

  public static ExchangeRateRepository newInstance(ExchangeRateDao dao) {
    return new ExchangeRateRepository(dao);
  }
}
