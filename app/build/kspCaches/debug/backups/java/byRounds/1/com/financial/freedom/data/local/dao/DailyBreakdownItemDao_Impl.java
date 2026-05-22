package com.financial.freedom.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.financial.freedom.data.local.Converters;
import com.financial.freedom.data.local.entity.DailyBreakdownItem;
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
public final class DailyBreakdownItemDao_Impl implements DailyBreakdownItemDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DailyBreakdownItem> __insertionAdapterOfDailyBreakdownItem;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteByAccountId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByDate;

  private final SharedSQLiteStatement __preparedStmtOfDeleteFromDate;

  public DailyBreakdownItemDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDailyBreakdownItem = new EntityInsertionAdapter<DailyBreakdownItem>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `daily_breakdown_items` (`id`,`accountId`,`date`,`type`,`valueCNY`,`changeCNY`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DailyBreakdownItem entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        final String _tmp = __converters.localDateToString(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        statement.bindString(4, entity.getType());
        final String _tmp_1 = __converters.bigDecimalToString(entity.getValueCNY());
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp_1);
        }
        final String _tmp_2 = __converters.bigDecimalToString(entity.getChangeCNY());
        if (_tmp_2 == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp_2);
        }
      }
    };
    this.__preparedStmtOfDeleteByAccountId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_breakdown_items WHERE accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByDate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_breakdown_items WHERE date = ? AND accountId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteFromDate = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM daily_breakdown_items WHERE accountId = ? AND date >= ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<DailyBreakdownItem> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDailyBreakdownItem.insert(items);
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
  public Object deleteByDate(final LocalDate date, final long accountId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByDate.acquire();
        int _argIndex = 1;
        final String _tmp = __converters.localDateToString(date);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, _tmp);
        }
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
          __preparedStmtOfDeleteByDate.release(_stmt);
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
  public Object getByDate(final LocalDate date, final long accountId,
      final Continuation<? super List<DailyBreakdownItem>> $completion) {
    final String _sql = "SELECT * FROM daily_breakdown_items WHERE date = ? AND accountId = ?";
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
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DailyBreakdownItem>>() {
      @Override
      @NonNull
      public List<DailyBreakdownItem> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfValueCNY = CursorUtil.getColumnIndexOrThrow(_cursor, "valueCNY");
          final int _cursorIndexOfChangeCNY = CursorUtil.getColumnIndexOrThrow(_cursor, "changeCNY");
          final List<DailyBreakdownItem> _result = new ArrayList<DailyBreakdownItem>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyBreakdownItem _item;
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
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final BigDecimal _tmpValueCNY;
            final String _tmp_3;
            if (_cursor.isNull(_cursorIndexOfValueCNY)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getString(_cursorIndexOfValueCNY);
            }
            final BigDecimal _tmp_4 = __converters.stringToBigDecimal(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpValueCNY = _tmp_4;
            }
            final BigDecimal _tmpChangeCNY;
            final String _tmp_5;
            if (_cursor.isNull(_cursorIndexOfChangeCNY)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getString(_cursorIndexOfChangeCNY);
            }
            final BigDecimal _tmp_6 = __converters.stringToBigDecimal(_tmp_5);
            if (_tmp_6 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpChangeCNY = _tmp_6;
            }
            _item = new DailyBreakdownItem(_tmpId,_tmpAccountId,_tmpDate,_tmpType,_tmpValueCNY,_tmpChangeCNY);
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
