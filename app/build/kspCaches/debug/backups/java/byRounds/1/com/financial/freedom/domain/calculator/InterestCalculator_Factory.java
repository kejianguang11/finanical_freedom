package com.financial.freedom.domain.calculator;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class InterestCalculator_Factory implements Factory<InterestCalculator> {
  @Override
  public InterestCalculator get() {
    return newInstance();
  }

  public static InterestCalculator_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static InterestCalculator newInstance() {
    return new InterestCalculator();
  }

  private static final class InstanceHolder {
    private static final InterestCalculator_Factory INSTANCE = new InterestCalculator_Factory();
  }
}
