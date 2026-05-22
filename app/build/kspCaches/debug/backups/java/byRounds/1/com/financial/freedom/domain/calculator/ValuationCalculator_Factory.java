package com.financial.freedom.domain.calculator;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class ValuationCalculator_Factory implements Factory<ValuationCalculator> {
  private final Provider<InterestCalculator> interestCalculatorProvider;

  public ValuationCalculator_Factory(Provider<InterestCalculator> interestCalculatorProvider) {
    this.interestCalculatorProvider = interestCalculatorProvider;
  }

  @Override
  public ValuationCalculator get() {
    return newInstance(interestCalculatorProvider.get());
  }

  public static ValuationCalculator_Factory create(
      Provider<InterestCalculator> interestCalculatorProvider) {
    return new ValuationCalculator_Factory(interestCalculatorProvider);
  }

  public static ValuationCalculator newInstance(InterestCalculator interestCalculator) {
    return new ValuationCalculator(interestCalculator);
  }
}
