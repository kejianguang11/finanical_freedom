package com.financial.freedom.data.remote;

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
public final class ExchangeRateProvider_Factory implements Factory<ExchangeRateProvider> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public ExchangeRateProvider_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public ExchangeRateProvider get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static ExchangeRateProvider_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new ExchangeRateProvider_Factory(okHttpClientProvider);
  }

  public static ExchangeRateProvider newInstance(OkHttpClient okHttpClient) {
    return new ExchangeRateProvider(okHttpClient);
  }
}
