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
public final class HKStockProvider_Factory implements Factory<HKStockProvider> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public HKStockProvider_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public HKStockProvider get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static HKStockProvider_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new HKStockProvider_Factory(okHttpClientProvider);
  }

  public static HKStockProvider newInstance(OkHttpClient okHttpClient) {
    return new HKStockProvider(okHttpClient);
  }
}
