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
import com.financial.freedom.data.local.entity.PriceSnapshot;
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
public final class PriceSnapshotDao_Impl implements PriceSnapshotDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PriceSnapshot> __insertionAdapterOfPriceSnapshot;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteByAccountId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByHoldingId;

  public PriceSnapshotDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPriceSnapshot = new EntityInsertionAdapter<PriceSnapshot>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `price_snapshots` (`id`,`holdingId`,`accountId`,`date`,`unitPrice`,`currency`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PriceSnapshot entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getHoldingId());
        statement.bindLong(3, entity.getAccountId());
        final String _tmp = __converters.localDateToString(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getUnitPrice());
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp_1);
        }
        statement.bindString(6, entity.getCurrency());
      }
    };
    this.__preparedStmtOfDeleteByAccountId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM price_snapshots WHERE accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByHoldingId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM price_snapshots WHERE holdingId = ? AND accountId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final PriceSnapshot snapshot, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPriceSnapshot.insert(snapshot);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<PriceSnapshot> snapshots,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPriceSnapshot.insert(snapshots);
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
  public Object deleteByHoldingId(final long holdingId, final long accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByHoldingId.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, holdingId);
        _argIndex = 2;
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
          __preparedStmtOfDeleteByHoldingId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PriceSnapshot>> getByHoldingAndDateRange(final long holdingId,
      final LocalDate start, final LocalDate end, final long accountId) {
    final String _sql = "SELECT * FROM price_snapshots WHERE holdingId = ? AND accountId = ? AND date BETWEEN ? AND ? ORDER BY date ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 4);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, holdingId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    _argIndex = 3;
    final String _tmp = __converters.localDateToString(start);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    _argIndex = 4;
    final String _tmp_1 = __converters.localDateToString(end);
    if (_tmp_1 == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp_1);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"price_snapshots"}, new Callable<List<PriceSnapshot>>() {
      @Override
      @NonNull
      public List<PriceSnapshot> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHoldingId = CursorUtil.getColumnIndexOrThrow(_cursor, "holdingId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfUnitPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "unitPrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final List<PriceSnapshot> _result = new ArrayList<PriceSnapshot>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PriceSnapshot _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHoldingId;
            _tmpHoldingId = _cursor.getLong(_cursorIndexOfHoldingId);
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
            final BigDecimal _tmpUnitPrice;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfUnitPrice)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfUnitPrice);
            }
            final BigDecimal _tmp_5 = __converters.stringToBigDecimal(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpUnitPrice = _tmp_5;
            }
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            _item = new PriceSnapshot(_tmpId,_tmpHoldingId,_tmpAccountId,_tmpDate,_tmpUnitPrice,_tmpCurrency);
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
  public Object getByHoldingAndDate(final long holdingId, final LocalDate date,
      final long accountId, final Continuation<? super PriceSnapshot> $completion) {
    final String _sql = "SELECT * FROM price_snapshots WHERE holdingId = ? AND date = ? AND accountId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, holdingId);
    _argIndex = 2;
    final String _tmp = __converters.localDateToString(date);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, _tmp);
    }
    _argIndex = 3;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PriceSnapshot>() {
      @Override
      @Nullable
      public PriceSnapshot call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHoldingId = CursorUtil.getColumnIndexOrThrow(_cursor, "holdingId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfUnitPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "unitPrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final PriceSnapshot _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHoldingId;
            _tmpHoldingId = _cursor.getLong(_cursorIndexOfHoldingId);
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
            final BigDecimal _tmpUnitPrice;
            final String _tmp_3;
            if (_cursor.isNull(_cursorIndexOfUnitPrice)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getString(_cursorIndexOfUnitPrice);
            }
            final BigDecimal _tmp_4 = __converters.stringToBigDecimal(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpUnitPrice = _tmp_4;
            }
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            _result = new PriceSnapshot(_tmpId,_tmpHoldingId,_tmpAccountId,_tmpDate,_tmpUnitPrice,_tmpCurrency);
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
  public Object getLatest(final long holdingId, final long accountId,
      final Continuation<? super PriceSnapshot> $completion) {
    final String _sql = "SELECT * FROM price_snapshots WHERE holdingId = ? AND accountId = ? ORDER BY date DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, holdingId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PriceSnapshot>() {
      @Override
      @Nullable
      public PriceSnapshot call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHoldingId = CursorUtil.getColumnIndexOrThrow(_cursor, "holdingId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfUnitPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "unitPrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final PriceSnapshot _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHoldingId;
            _tmpHoldingId = _cursor.getLong(_cursorIndexOfHoldingId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
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
            final BigDecimal _tmpUnitPrice;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUnitPrice)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfUnitPrice);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpUnitPrice = _tmp_3;
            }
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            _result = new PriceSnapshot(_tmpId,_tmpHoldingId,_tmpAccountId,_tmpDate,_tmpUnitPrice,_tmpCurrency);
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
  public Object getPrevious(final long holdingId, final long accountId,
      final Continuation<? super PriceSnapshot> $completion) {
    final String _sql = "SELECT * FROM price_snapshots WHERE holdingId = ? AND accountId = ? ORDER BY date DESC LIMIT 1 OFFSET 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, holdingId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PriceSnapshot>() {
      @Override
      @Nullable
      public PriceSnapshot call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHoldingId = CursorUtil.getColumnIndexOrThrow(_cursor, "holdingId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfUnitPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "unitPrice");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final PriceSnapshot _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHoldingId;
            _tmpHoldingId = _cursor.getLong(_cursorIndexOfHoldingId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
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
            final BigDecimal _tmpUnitPrice;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfUnitPrice)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfUnitPrice);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpUnitPrice = _tmp_3;
            }
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            _result = new PriceSnapshot(_tmpId,_tmpHoldingId,_tmpAccountId,_tmpDate,_tmpUnitPrice,_tmpCurrency);
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
