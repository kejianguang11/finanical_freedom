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
public final class USStockProvider_Factory implements Factory<USStockProvider> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public USStockProvider_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public USStockProvider get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static USStockProvider_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new USStockProvider_Factory(okHttpClientProvider);
  }

  public static USStockProvider newInstance(OkHttpClient okHttpClient) {
    return new USStockProvider(okHttpClient);
  }
}
