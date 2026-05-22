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
public final class AStockProvider_Factory implements Factory<AStockProvider> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public AStockProvider_Factory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public AStockProvider get() {
    return newInstance(okHttpClientProvider.get());
  }

  public static AStockProvider_Factory create(Provider<OkHttpClient> okHttpClientProvider) {
    return new AStockProvider_Factory(okHttpClientProvider);
  }

  public static AStockProvider newInstance(OkHttpClient okHttpClient) {
    return new AStockProvider(okHttpClient);
  }
}
