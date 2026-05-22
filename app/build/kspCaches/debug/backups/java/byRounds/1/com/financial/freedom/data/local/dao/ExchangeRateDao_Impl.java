package com.financial.freedom.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.financial.freedom.data.local.Converters;
import com.financial.freedom.data.local.entity.ExchangeRate;
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
import kotlinx.datetime.LocalDate;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ExchangeRateDao_Impl implements ExchangeRateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ExchangeRate> __insertionAdapterOfExchangeRate;

  private final Converters __converters = new Converters();

  public ExchangeRateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfExchangeRate = new EntityInsertionAdapter<ExchangeRate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `exchange_rates` (`id`,`fromCurrency`,`toCurrency`,`date`,`rate`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ExchangeRate entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getFromCurrency());
        statement.bindString(3, entity.getToCurrency());
        final String _tmp = __converters.localDateToString(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getRate());
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp_1);
        }
      }
    };
  }

  @Override
  public Object insert(final ExchangeRate rate, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExchangeRate.insert(rate);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<ExchangeRate> rates,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfExchangeRate.insert(rates);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRate(final String from, final String to, final LocalDate date,
      final Continuation<? super ExchangeRate> $completion) {
    final String _sql = "SELECT * FROM exchange_rates WHERE fromCurrency = ? AND toCurrency = ? AND date = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, from);
    _argIndex = 2;
    _statement.bindString(_argIndex, to);
    _argIndex = 3;
    final String _tmp = __converters.localDateToString(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ExchangeRate>() {
      @Override
      @Nullable
      public ExchangeRate call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFromCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "fromCurrency");
          final int _cursorIndexOfToCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "toCurrency");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfRate = CursorUtil.getColumnIndexOrThrow(_cursor, "rate");
          final ExchangeRate _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFromCurrency;
            _tmpFromCurrency = _cursor.getString(_cursorIndexOfFromCurrency);
            final String _tmpToCurrency;
            _tmpToCurrency = _cursor.getString(_cursorIndexOfToCurrency);
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
            final BigDecimal _tmpRate;
            final String _tmp_3;
            if (_cursor.isNull(_cursorIndexOfRate)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getString(_cursorIndexOfRate);
            }
            final BigDecimal _tmp_4 = __converters.stringToBigDecimal(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpRate = _tmp_4;
            }
            _result = new ExchangeRate(_tmpId,_tmpFromCurrency,_tmpToCurrency,_tmpDate,_tmpRate);
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
  public Object getLatestRates(final Continuation<? super List<ExchangeRate>> $completion) {
    final String _sql = "SELECT * FROM exchange_rates WHERE date = (SELECT MAX(date) FROM exchange_rates)";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ExchangeRate>>() {
      @Override
      @NonNull
      public List<ExchangeRate> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFromCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "fromCurrency");
          final int _cursorIndexOfToCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "toCurrency");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfRate = CursorUtil.getColumnIndexOrThrow(_cursor, "rate");
          final List<ExchangeRate> _result = new ArrayList<ExchangeRate>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ExchangeRate _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpFromCurrency;
            _tmpFromCurrency = _cursor.getString(_cursorIndexOfFromCurrency);
            final String _tmpToCurrency;
            _tmpToCurrency = _cursor.getString(_cursorIndexOfToCurrency);
            final LocalDate _tmpDate;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfDate)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfDate);
            }
            final LocalDate _tmp_1 = __converters.stringToLocalDate(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpDate = _tmp_1;
            }
            final BigDecimal _tmpRate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfRate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfRate);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpRate = _tmp_3;
            }
            _item = new ExchangeRate(_tmpId,_tmpFromCurrency,_tmpToCurrency,_tmpDate,_tmpRate);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
