package com.financial.freedom.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.financial.freedom.data.local.Converters;
import com.financial.freedom.data.local.entity.DailySummary;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import kotlinx.datetime.LocalDate;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DailySummaryDao_Impl implements DailySummaryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailySummary> __insertionAdapterOfDailySummary;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteByAccountId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteFromDate;

  public DailySummaryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailySummary = new EntityInsertionAdapter<DailySummary>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_summaries` (`id`,`accountId`,`date`,`totalValueCNY`,`dayChange`,`dayChangePct`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailySummary entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        final String _tmp = __converters.localDateToString(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getTotalValueCNY());
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp_1);
        }
        final String _tmp_2 = __converters.bigDecimalToString(entity.getDayChange());
        if (_tmp_2 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp_2);
        }
        final String _tmp_3 = __converters.bigDecimalToString(entity.getDayChangePct());
        if (_tmp_3 == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp_3);
        }
      }
    };
    this.__preparedStmtOfDeleteByAccountId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_summaries WHERE accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteFromDate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_summaries WHERE accountId = ? AND date >= ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final DailySummary summary, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailySummary.insert(summary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<DailySummary> summaries,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailySummary.insert(summaries);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByAccountId(final long accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByAccountId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, accountId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByAccountId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteFromDate(final LocalDate fromDate, final long accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteFromDate.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, accountId);
        _argIndex = 2;
        final String _tmp = __converters.localDateToString(fromDate);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, _tmp);
        }
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteFromDate.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DailySummary>> getByDateRange(final LocalDate start, final LocalDate end,
      final long accountId) {
    final String _sql = "SELECT * FROM daily_summaries WHERE accountId = ? AND date BETWEEN ? AND ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    final String _tmp = __converters.localDateToString(start);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    _argIndex = 3;
    final String _tmp_1 = __converters.localDateToString(end);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp_1);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"daily_summaries"}, new Callable<List<DailySummary>>() {
      @Override
      @NonNull
      public List<DailySummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalValueCNY = CursorUtil.getColumnIndexOrThrow(_cursor, "totalValueCNY");
          final int _cursorIndexOfDayChange = CursorUtil.getColumnIndexOrThrow(_cursor, "dayChange");
          final int _cursorIndexOfDayChangePct = CursorUtil.getColumnIndexOrThrow(_cursor, "dayChangePct");
          final List<DailySummary> _result = new ArrayList<DailySummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailySummary _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final LocalDate _tmpDate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_3 = __converters.stringToLocalDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_3;
            }
            final BigDecimal _tmpTotalValueCNY;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfTotalValueCNY)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfTotalValueCNY);
            }
            final BigDecimal _tmp_5 = __converters.stringToBigDecimal(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpTotalValueCNY = _tmp_5;
            }
            final BigDecimal _tmpDayChange;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfDayChange)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfDayChange);
            }
            final BigDecimal _tmp_7 = __converters.stringToBigDecimal(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpDayChange = _tmp_7;
            }
            final BigDecimal _tmpDayChangePct;
            final String _tmp_8;
            if (_cursor.isNull(_cursorIndexOfDayChangePct)) {
              _tmp_8 = null;
            } else {
              _tmp_8 = _cursor.getString(_cursorIndexOfDayChangePct);
            }
            final BigDecimal _tmp_9 = __converters.stringToBigDecimal(_tmp_8);
            if (_tmp_9 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpDayChangePct = _tmp_9;
            }
            _item = new DailySummary(_tmpId,_tmpAccountId,_tmpDate,_tmpTotalValueCNY,_tmpDayChange,_tmpDayChangePct);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getListByDateRange(final LocalDate start, final LocalDate end, final long accountId,
      final Continuation<? super List<DailySummary>> $completion) {
    final String _sql = "SELECT * FROM daily_summaries WHERE accountId = ? AND date BETWEEN ? AND ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 2;
    final String _tmp = __converters.localDateToString(start);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    _argIndex = 3;
    final String _tmp_1 = __converters.localDateToString(end);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp_1);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailySummary>>() {
      @Override
      @NonNull
      public List<DailySummary> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalValueCNY = CursorUtil.getColumnIndexOrThrow(_cursor, "totalValueCNY");
          final int _cursorIndexOfDayChange = CursorUtil.getColumnIndexOrThrow(_cursor, "dayChange");
          final int _cursorIndexOfDayChangePct = CursorUtil.getColumnIndexOrThrow(_cursor, "dayChangePct");
          final List<DailySummary> _result = new ArrayList<DailySummary>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailySummary _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final LocalDate _tmpDate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_3 = __converters.stringToLocalDate(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_3;
            }
            final BigDecimal _tmpTotalValueCNY;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfTotalValueCNY)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfTotalValueCNY);
            }
            final BigDecimal _tmp_5 = __converters.stringToBigDecimal(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpTotalValueCNY = _tmp_5;
            }
            final BigDecimal _tmpDayChange;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfDayChange)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfDayChange);
            }
            final BigDecimal _tmp_7 = __converters.stringToBigDecimal(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpDayChange = _tmp_7;
            }
            final BigDecimal _tmpDayChangePct;
            final String _tmp_8;
            if (_cursor.isNull(_cursorIndexOfDayChangePct)) {
              _tmp_8 = null;
            } else {
              _tmp_8 = _cursor.getString(_cursorIndexOfDayChangePct);
            }
            final BigDecimal _tmp_9 = __converters.stringToBigDecimal(_tmp_8);
            if (_tmp_9 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpDayChangePct = _tmp_9;
            }
            _item = new DailySummary(_tmpId,_tmpAccountId,_tmpDate,_tmpTotalValueCNY,_tmpDayChange,_tmpDayChangePct);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByDate(final LocalDate date, final long accountId,
      final Continuation<? super DailySummary> $completion) {
    final String _sql = "SELECT * FROM daily_summaries WHERE date = ? AND accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    final String _tmp = __converters.localDateToString(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<DailySummary>() {
      @Override
      @Nullable
      public DailySummary call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfTotalValueCNY = CursorUtil.getColumnIndexOrThrow(_cursor, "totalValueCNY");
          final int _cursorIndexOfDayChange = CursorUtil.getColumnIndexOrThrow(_cursor, "dayChange");
          final int _cursorIndexOfDayChangePct = CursorUtil.getColumnIndexOrThrow(_cursor, "dayChangePct");
          final DailySummary _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final LocalDate _tmpDate;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_2 = __converters.stringToLocalDate(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_2;
            }
            final BigDecimal _tmpTotalValueCNY;
            final String _tmp_3;
            if (_cursor.isNull(_cursorIndexOfTotalValueCNY)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getString(_cursorIndexOfTotalValueCNY);
            }
            final BigDecimal _tmp_4 = __converters.stringToBigDecimal(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpTotalValueCNY = _tmp_4;
            }
            final BigDecimal _tmpDayChange;
            final String _tmp_5;
            if (_cursor.isNull(_cursorIndexOfDayChange)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getString(_cursorIndexOfDayChange);
            }
            final BigDecimal _tmp_6 = __converters.stringToBigDecimal(_tmp_5);
            if (_tmp_6 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpDayChange = _tmp_6;
            }
            final BigDecimal _tmpDayChangePct;
            final String _tmp_7;
            if (_cursor.isNull(_cursorIndexOfDayChangePct)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getString(_cursorIndexOfDayChangePct);
            }
            final BigDecimal _tmp_8 = __converters.stringToBigDecimal(_tmp_7);
            if (_tmp_8 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpDayChangePct = _tmp_8;
            }
            _result = new DailySummary(_tmpId,_tmpAccountId,_tmpDate,_tmpTotalValueCNY,_tmpDayChange,_tmpDayChangePct);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getLatestDate(final long accountId,
      final Continuation<? super LocalDate> $completion) {
    final String _sql = "SELECT MAX(date) FROM daily_summaries WHERE accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalDate>() {
      @Override
      @Nullable
      public LocalDate call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final LocalDate _result;
          if (_cursor.moveToFirst()) {
            final String _tmp;
            if (_cursor.isNull(0)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(0);
            }
            _result = __converters.stringToLocalDate(_tmp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
