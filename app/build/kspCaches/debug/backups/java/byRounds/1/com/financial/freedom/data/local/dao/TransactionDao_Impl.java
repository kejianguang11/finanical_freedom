package com.financial.freedom.data.local.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
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
import com.financial.freedom.data.local.entity.Transaction;
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
public final class TransactionDao_Impl implements TransactionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Transaction> __insertionAdapterOfTransaction;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Transaction> __deletionAdapterOfTransaction;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByAccountId;

  public TransactionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfTransaction = new EntityInsertionAdapter<Transaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `transactions` (`id`,`holdingId`,`accountId`,`type`,`date`,`price`,`quantity`,`fee`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Transaction entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getHoldingId());
        statement.bindLong(3, entity.getAccountId());
        statement.bindString(4, entity.getType());
        final String _tmp = __converters.localDateToString(entity.getDate());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getPrice());
        if (_tmp_1 == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp_1);
        }
        final String _tmp_2 = __converters.bigDecimalToString(entity.getQuantity());
        if (_tmp_2 == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp_2);
        }
        final String _tmp_3 = __converters.bigDecimalToString(entity.getFee());
        if (_tmp_3 == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, _tmp_3);
        }
      }
    };
    this.__deletionAdapterOfTransaction = new EntityDeletionOrUpdateAdapter<Transaction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `transactions` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Transaction entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteByAccountId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM transactions WHERE accountId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Transaction transaction,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfTransaction.insertAndReturnId(transaction);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Transaction transaction,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfTransaction.handle(transaction);
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
  public Flow<List<Transaction>> getByHolding(final long holdingId, final long accountId) {
    final String _sql = "SELECT * FROM transactions WHERE holdingId = ? AND accountId = ? ORDER BY date DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, holdingId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"transactions"}, new Callable<List<Transaction>>() {
      @Override
      @NonNull
      public List<Transaction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfHoldingId = CursorUtil.getColumnIndexOrThrow(_cursor, "holdingId");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfDate = CursorUtil.getColumnIndexOrThrow(_cursor, "date");
          final int _cursorIndexOfPrice = CursorUtil.getColumnIndexOrThrow(_cursor, "price");
          final int _cursorIndexOfQuantity = CursorUtil.getColumnIndexOrThrow(_cursor, "quantity");
          final int _cursorIndexOfFee = CursorUtil.getColumnIndexOrThrow(_cursor, "fee");
          final List<Transaction> _result = new ArrayList<Transaction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Transaction _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpHoldingId;
            _tmpHoldingId = _cursor.getLong(_cursorIndexOfHoldingId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpType;
            _tmpType = _cursor.getString(_cursorIndexOfType);
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
            final BigDecimal _tmpPrice;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfPrice)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfPrice);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpPrice = _tmp_3;
            }
            final BigDecimal _tmpQuantity;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfQuantity)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfQuantity);
            }
            final BigDecimal _tmp_5 = __converters.stringToBigDecimal(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpQuantity = _tmp_5;
            }
            final BigDecimal _tmpFee;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfFee)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfFee);
            }
            final BigDecimal _tmp_7 = __converters.stringToBigDecimal(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpFee = _tmp_7;
            }
            _item = new Transaction(_tmpId,_tmpHoldingId,_tmpAccountId,_tmpType,_tmpDate,_tmpPrice,_tmpQuantity,_tmpFee);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
