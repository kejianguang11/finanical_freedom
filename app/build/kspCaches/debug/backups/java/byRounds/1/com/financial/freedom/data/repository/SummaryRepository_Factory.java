package com.financial.freedom.data.repository;

import com.financial.freedom.data.local.dao.DailyBreakdownItemDao;
import com.financial.freedom.data.local.dao.DailySummaryDao;
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
public final class SummaryRepository_Factory implements Factory<SummaryRepository> {
  private final Provider<DailySummaryDao> summaryDaoProvider;

  private final Provider<DailyBreakdownItemDao> breakdownDaoProvider;

  public SummaryRepository_Factory(Provider<DailySummaryDao> summaryDaoProvider,
      Provider<DailyBreakdownItemDao> breakdownDaoProvider) {
    this.summaryDaoProvider = summaryDaoProvider;
    this.breakdownDaoProvider = breakdownDaoProvider;
  }

  @Override
  public SummaryRepository get() {
    return newInstance(summaryDaoProvider.get(), breakdownDaoProvider.get());
  }

  public static SummaryRepository_Factory create(Provider<DailySummaryDao> summaryDaoProvider,
      Provider<DailyBreakdownItemDao> breakdownDaoProvider) {
    return new SummaryRepository_Factory(summaryDaoProvider, breakdownDaoProvider);
  }

  public static SummaryRepository newInstance(DailySummaryDao summaryDao,
      DailyBreakdownItemDao breakdownDao) {
    return new SummaryRepository(summaryDao, breakdownDao);
  }
}
