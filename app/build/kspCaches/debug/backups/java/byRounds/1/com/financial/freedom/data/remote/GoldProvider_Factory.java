package com.financial.freedom.data.remote;

import com.financial.freedom.data.local.dao.ExchangeRateDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

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
public final class GoldProvider_Factory implements Factory<GoldProvider> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<ExchangeRateDao> exchangeRateDaoProvider;

  public GoldProvider_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.exchangeRateDaoProvider = exchangeRateDaoProvider;
  }

  @Override
  public GoldProvider get() {
    return newInstance(okHttpClientProvider.get(), exchangeRateDaoProvider.get());
  }

  public static GoldProvider_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<ExchangeRateDao> exchangeRateDaoProvider) {
    return new GoldProvider_Factory(okHttpClientProvider, exchangeRateDaoProvider);
  }

  public static GoldProvider newInstance(OkHttpClient okHttpClient,
      ExchangeRateDao exchangeRateDao) {
    return new GoldProvider(okHttpClient, exchangeRateDao);
  }
}
