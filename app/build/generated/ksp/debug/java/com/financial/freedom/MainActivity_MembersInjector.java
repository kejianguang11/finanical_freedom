package com.financial.freedom;

import com.financial.freedom.domain.account.AccountManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<AccountManager> accountManagerProvider;

  public MainActivity_MembersInjector(Provider<AccountManager> accountManagerProvider) {
    this.accountManagerProvider = accountManagerProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<AccountManager> accountManagerProvider) {
    return new MainActivity_MembersInjector(accountManagerProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectAccountManager(instance, accountManagerProvider.get());
  }

  @InjectedFieldSignature("com.financial.freedom.MainActivity.accountManager")
  public static void injectAccountManager(MainActivity instance, AccountManager accountManager) {
    instance.accountManager = accountManager;
  }
}
