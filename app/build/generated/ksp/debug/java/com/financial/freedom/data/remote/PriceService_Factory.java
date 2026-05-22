package com.financial.freedom.data.remote;

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
public final class PriceService_Factory implements Factory<PriceService> {
  private final Provider<AStockProvider> aStockProvider;

  private final Provider<USStockProvider> usStockProvider;

  private final Provider<HKStockProvider> hkStockProvider;

  private final Provider<CNFundProvider> cnFundProvider;

  private final Provider<GoldProvider> goldProvider;

  private final Provider<ExchangeRateProvider> exchangeRateProvider;

  public PriceService_Factory(Provider<AStockProvider> aStockProvider,
      Provider<USStockProvider> usStockProvider, Provider<HKStockProvider> hkStockProvider,
      Provider<CNFundProvider> cnFundProvider, Provider<GoldProvider> goldProvider,
      Provider<ExchangeRateProvider> exchangeRateProvider) {
    this.aStockProvider = aStockProvider;
    this.usStockProvider = usStockProvider;
    this.hkStockProvider = hkStockProvider;
    this.cnFundProvider = cnFundProvider;
    this.goldProvider = goldProvider;
    this.exchangeRateProvider = exchangeRateProvider;
  }

  @Override
  public PriceService get() {
    return newInstance(aStockProvider.get(), usStockProvider.get(), hkStockProvider.get(), cnFundProvider.get(), goldProvider.get(), exchangeRateProvider.get());
  }

  public static PriceService_Factory create(Provider<AStockProvider> aStockProvider,
      Provider<USStockProvider> usStockProvider, Provider<HKStockProvider> hkStockProvider,
      Provider<CNFundProvider> cnFundProvider, Provider<GoldProvider> goldProvider,
      Provider<ExchangeRateProvider> exchangeRateProvider) {
    return new PriceService_Factory(aStockProvider, usStockProvider, hkStockProvider, cnFundProvider, goldProvider, exchangeRateProvider);
  }

  public static PriceService newInstance(AStockProvider aStockProvider,
      USStockProvider usStockProvider, HKStockProvider hkStockProvider,
      CNFundProvider cnFundProvider, GoldProvider goldProvider,
      ExchangeRateProvider exchangeRateProvider) {
    return new PriceService(aStockProvider, usStockProvider, hkStockProvider, cnFundProvider, goldProvider, exchangeRateProvider);
  }
}
