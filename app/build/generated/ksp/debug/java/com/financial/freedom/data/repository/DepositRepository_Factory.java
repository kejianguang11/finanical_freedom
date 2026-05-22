package com.financial.freedom.data.repository;

import com.financial.freedom.data.local.dao.DepositDao;
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
public final class DepositRepository_Factory implements Factory<DepositRepository> {
  private final Provider<DepositDao> daoProvider;

  public DepositRepository_Factory(Provider<DepositDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public DepositRepository get() {
    return newInstance(daoProvider.get());
  }

  public static DepositRepository_Factory create(Provider<DepositDao> daoProvider) {
    return new DepositRepository_Factory(daoProvider);
  }

  public static DepositRepository newInstance(DepositDao dao) {
    return new DepositRepository(dao);
  }
}
