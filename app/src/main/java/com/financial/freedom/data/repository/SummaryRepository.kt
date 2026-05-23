package com.financial.freedom.data.repository

import com.financial.freedom.data.local.dao.DailyBreakdownItemDao
import com.financial.freedom.data.local.dao.DailySummaryDao
import com.financial.freedom.data.local.entity.DailyBreakdownItem
import com.financial.freedom.data.local.entity.DailySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SummaryRepository @Inject constructor(
    private val summaryDao: DailySummaryDao,
    private val breakdownDao: DailyBreakdownItemDao
) {
    fun getByDateRange(start: LocalDate, end: LocalDate, accountId: Long): Flow<List<DailySummary>> =
        summaryDao.getByDateRange(start, end, accountId)

    suspend fun getListByDateRange(start: LocalDate, end: LocalDate, accountId: Long): List<DailySummary> =
        summaryDao.getListByDateRange(start, end, accountId)

    suspend fun getByDate(date: LocalDate, accountId: Long): DailySummary? =
        summaryDao.getByDate(date, accountId)

    suspend fun getLatestDate(accountId: Long): LocalDate? =
        summaryDao.getLatestDate(accountId)

    suspend fun getEarliestDate(accountId: Long): LocalDate? =
        summaryDao.getEarliestDate(accountId)

    suspend fun getBreakdown(date: LocalDate, accountId: Long): List<DailyBreakdownItem> =
        breakdownDao.getByDate(date, accountId)

    suspend fun getBreakdownsByDateRange(start: LocalDate, end: LocalDate, accountId: Long): List<DailyBreakdownItem> =
        breakdownDao.getByDateRange(start, end, accountId)

    suspend fun saveDailySummary(summary: DailySummary, breakdowns: List<DailyBreakdownItem>) {
        summaryDao.insert(summary)
        breakdownDao.insertAll(breakdowns)
    }

    suspend fun saveTodaySummary(summary: DailySummary, breakdowns: List<DailyBreakdownItem>) {
        summaryDao.insert(summary)
        // Delete old breakdown for today then insert new
        breakdownDao.deleteByDate(summary.date, summary.accountId)
        breakdownDao.insertAll(breakdowns)
    }
}
