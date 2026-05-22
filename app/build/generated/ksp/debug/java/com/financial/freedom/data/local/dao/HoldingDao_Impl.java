package com.financial.freedom.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.financial.freedom.data.local.Converters;
import com.financial.freedom.data.local.entity.Holding;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Long;
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
public final class HoldingDao_Impl implements HoldingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Holding> __insertionAdapterOfHolding;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Holding> __deletionAdapterOfHolding;

  private final EntityDeletionOrUpdateAdapter<Holding> __updateAdapterOfHolding;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByAccountId;

  public HoldingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHolding = new EntityInsertionAdapter<Holding>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `holdings` (`id`,`accountId`,`type`,`symbol`,`name`,`market`,`currency`,`quantity`,`costPrice`,`costDate`,`note`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Holding entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getType());
        statement.bindString(4, entity.getSymbol());
        statement.bindString(5, entity.getName());
        statement.bindString(6, entity.getMarket());
        statement.bindString(7, entity.getCurrency());
        final String _tmp = __converters.bigDecimalToString(entity.getQuantity());
        if (_tmp == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getCostPrice());
        if (_tmp_1 == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, _tmp_1);
        }
        final String _tmp_2 = __converters.localDateToString(entity.getCostDate());
        if (_tmp_2 == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, _tmp_2);
        }
        statement.bindString(11, entity.getNote());
      }
    };
    this.__deletionAdapterOfHolding = new EntityDeletionOrUpdateAdapter<Holding>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `holdings` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Holding entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHolding = new EntityDeletionOrUpdateAdapter<Holding>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `holdings` SET `id` = ?,`accountId` = ?,`type` = ?,`symbol` = ?,`name` = ?,`market` = ?,`currency` = ?,`quantity` = ?,`costPrice` = ?,`costDate` = ?,`note` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Holding entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getType());
        statement.bindString(4, entity.getSymbol());
        statement.bindString(5, entity.getName());
        statement.bindString(6, entity.getMarket());
        statement.bindString(7, entity.getCurrency());
        final String _tmp = __converters.bigDecimalToString(entity.getQuantity());
        if (_tmp == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getCostPrice());
        if (_tmp_1 == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, _tmp_1);
        }
        final String _tmp_2 = __converters.localDateToString(entity.getCostDate());
        if (_tmp_2 == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, _tmp_2);
        }
        statement.bindString(11, entity.getNote());
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteByAccountId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM holdings WHERE accountId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Holding holding, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfHolding.insertAndReturnId(holding);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Holding holding, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHolding.handle(holding);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Holding holding, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHolding.handle(holding);
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
  public Flow<List<Holding>> getAll(final long accountId) {
    final String _sql = "SELECT * FROM holdings WHERE accountId = ? ORDER BY type, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"holdings"}, new Callable<List<Holding>>() {
      @Override
      @NonNull
      public List<Holding> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMarket = CursorUtil.getColumnIndexOrThrow(_cursor, "market");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfCostPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "costPrice");
          final int _cursorIndexOfCostDate = CursorUtil.getColumnIndexOrThrow(_cursor, "costDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<Holding> _result = new ArrayList<Holding>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Holding _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMarket;
            _tmpMarket = _cursor.getString(_cursorIndexOfMarket);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpQuantity;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfQuantity);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpQuantity = _tmp_1;
            }
            final BigDecimal _tmpCostPrice;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCostPrice)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfCostPrice);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpCostPrice = _tmp_3;
            }
            final LocalDate _tmpCostDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfCostDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfCostDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpCostDate = _tmp_5;
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new Holding(_tmpId,_tmpAccountId,_tmpType,_tmpSymbol,_tmpName,_tmpMarket,_tmpCurrency,_tmpQuantity,_tmpCostPrice,_tmpCostDate,_tmpNote);
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
  public Object getAllList(final long accountId,
      final Continuation<? super List<Holding>> $completion) {
    final String _sql = "SELECT * FROM holdings WHERE accountId = ? ORDER BY type, name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Holding>>() {
      @Override
      @NonNull
      public List<Holding> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMarket = CursorUtil.getColumnIndexOrThrow(_cursor, "market");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfCostPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "costPrice");
          final int _cursorIndexOfCostDate = CursorUtil.getColumnIndexOrThrow(_cursor, "costDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<Holding> _result = new ArrayList<Holding>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Holding _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMarket;
            _tmpMarket = _cursor.getString(_cursorIndexOfMarket);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpQuantity;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfQuantity);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpQuantity = _tmp_1;
            }
            final BigDecimal _tmpCostPrice;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCostPrice)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfCostPrice);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpCostPrice = _tmp_3;
            }
            final LocalDate _tmpCostDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfCostDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfCostDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpCostDate = _tmp_5;
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new Holding(_tmpId,_tmpAccountId,_tmpType,_tmpSymbol,_tmpName,_tmpMarket,_tmpCurrency,_tmpQuantity,_tmpCostPrice,_tmpCostDate,_tmpNote);
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
  public Flow<List<Holding>> getByType(final String type, final long accountId) {
    final String _sql = "SELECT * FROM holdings WHERE type = ? AND accountId = ? ORDER BY name";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, type);
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"holdings"}, new Callable<List<Holding>>() {
      @Override
      @NonNull
      public List<Holding> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMarket = CursorUtil.getColumnIndexOrThrow(_cursor, "market");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfCostPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "costPrice");
          final int _cursorIndexOfCostDate = CursorUtil.getColumnIndexOrThrow(_cursor, "costDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<Holding> _result = new ArrayList<Holding>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Holding _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMarket;
            _tmpMarket = _cursor.getString(_cursorIndexOfMarket);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpQuantity;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfQuantity);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpQuantity = _tmp_1;
            }
            final BigDecimal _tmpCostPrice;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCostPrice)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfCostPrice);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpCostPrice = _tmp_3;
            }
            final LocalDate _tmpCostDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfCostDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfCostDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpCostDate = _tmp_5;
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new Holding(_tmpId,_tmpAccountId,_tmpType,_tmpSymbol,_tmpName,_tmpMarket,_tmpCurrency,_tmpQuantity,_tmpCostPrice,_tmpCostDate,_tmpNote);
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
  public Object getById(final long id, final long accountId,
      final Continuation<? super Holding> $completion) {
    final String _sql = "SELECT * FROM holdings WHERE id = ? AND accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Holding>() {
      @Override
      @Nullable
      public Holding call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSymbol = CursorUtil.getColumnIndexOrThrow(_cursor, "symbol");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfMarket = CursorUtil.getColumnIndexOrThrow(_cursor, "market");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfCostPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "costPrice");
          final int _cursorIndexOfCostDate = CursorUtil.getColumnIndexOrThrow(_cursor, "costDate");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final Holding _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
            final String _tmpSymbol;
            _tmpSymbol = _cursor.getString(_cursorIndexOfSymbol);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpMarket;
            _tmpMarket = _cursor.getString(_cursorIndexOfMarket);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpQuantity;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfQuantity);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpQuantity = _tmp_1;
            }
            final BigDecimal _tmpCostPrice;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfCostPrice)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfCostPrice);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpCostPrice = _tmp_3;
            }
            final LocalDate _tmpCostDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfCostDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfCostDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpCostDate = _tmp_5;
            }
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _result = new Holding(_tmpId,_tmpAccountId,_tmpType,_tmpSymbol,_tmpName,_tmpMarket,_tmpCurrency,_tmpQuantity,_tmpCostPrice,_tmpCostDate,_tmpNote);
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
