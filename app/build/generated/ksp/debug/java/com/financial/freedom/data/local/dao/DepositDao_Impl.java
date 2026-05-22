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
import com.financial.freedom.data.local.entity.Deposit;
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
public final class DepositDao_Impl implements DepositDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Deposit> __insertionAdapterOfDeposit;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<Deposit> __deletionAdapterOfDeposit;

  private final EntityDeletionOrUpdateAdapter<Deposit> __updateAdapterOfDeposit;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByAccountId;

  public DepositDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDeposit = new EntityInsertionAdapter<Deposit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `deposits` (`id`,`accountId`,`name`,`bank`,`currency`,`principal`,`interestRate`,`startDate`,`maturityDate`,`status`,`note`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Deposit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getBank());
        statement.bindString(5, entity.getCurrency());
        final String _tmp = __converters.bigDecimalToString(entity.getPrincipal());
        if (_tmp == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getInterestRate());
        if (_tmp_1 == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp_1);
        }
        final String _tmp_2 = __converters.localDateToString(entity.getStartDate());
        if (_tmp_2 == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, _tmp_2);
        }
        final String _tmp_3 = __converters.localDateToString(entity.getMaturityDate());
        if (_tmp_3 == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, _tmp_3);
        }
        statement.bindString(10, entity.getStatus());
        statement.bindString(11, entity.getNote());
      }
    };
    this.__deletionAdapterOfDeposit = new EntityDeletionOrUpdateAdapter<Deposit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `deposits` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Deposit entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfDeposit = new EntityDeletionOrUpdateAdapter<Deposit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `deposits` SET `id` = ?,`accountId` = ?,`name` = ?,`bank` = ?,`currency` = ?,`principal` = ?,`interestRate` = ?,`startDate` = ?,`maturityDate` = ?,`status` = ?,`note` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Deposit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getAccountId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getBank());
        statement.bindString(5, entity.getCurrency());
        final String _tmp = __converters.bigDecimalToString(entity.getPrincipal());
        if (_tmp == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp);
        }
        final String _tmp_1 = __converters.bigDecimalToString(entity.getInterestRate());
        if (_tmp_1 == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp_1);
        }
        final String _tmp_2 = __converters.localDateToString(entity.getStartDate());
        if (_tmp_2 == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, _tmp_2);
        }
        final String _tmp_3 = __converters.localDateToString(entity.getMaturityDate());
        if (_tmp_3 == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, _tmp_3);
        }
        statement.bindString(10, entity.getStatus());
        statement.bindString(11, entity.getNote());
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteByAccountId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM deposits WHERE accountId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final Deposit deposit, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfDeposit.insertAndReturnId(deposit);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final Deposit deposit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfDeposit.handle(deposit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final Deposit deposit, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfDeposit.handle(deposit);
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
  public Flow<List<Deposit>> getAll(final long accountId) {
    final String _sql = "SELECT * FROM deposits WHERE accountId = ? ORDER BY startDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"deposits"}, new Callable<List<Deposit>>() {
      @Override
      @NonNull
      public List<Deposit> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfMaturityDate = CursorUtil.getColumnIndexOrThrow(_cursor, "maturityDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<Deposit> _result = new ArrayList<Deposit>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Deposit _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBank;
            _tmpBank = _cursor.getString(_cursorIndexOfBank);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpPrincipal;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPrincipal)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPrincipal);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpPrincipal = _tmp_1;
            }
            final BigDecimal _tmpInterestRate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfInterestRate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfInterestRate);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpInterestRate = _tmp_3;
            }
            final LocalDate _tmpStartDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_5;
            }
            final LocalDate _tmpMaturityDate;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfMaturityDate)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfMaturityDate);
            }
            final LocalDate _tmp_7 = __converters.stringToLocalDate(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpMaturityDate = _tmp_7;
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new Deposit(_tmpId,_tmpAccountId,_tmpName,_tmpBank,_tmpCurrency,_tmpPrincipal,_tmpInterestRate,_tmpStartDate,_tmpMaturityDate,_tmpStatus,_tmpNote);
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
      final Continuation<? super List<Deposit>> $completion) {
    final String _sql = "SELECT * FROM deposits WHERE accountId = ? ORDER BY startDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Deposit>>() {
      @Override
      @NonNull
      public List<Deposit> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfMaturityDate = CursorUtil.getColumnIndexOrThrow(_cursor, "maturityDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<Deposit> _result = new ArrayList<Deposit>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Deposit _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBank;
            _tmpBank = _cursor.getString(_cursorIndexOfBank);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpPrincipal;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPrincipal)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPrincipal);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpPrincipal = _tmp_1;
            }
            final BigDecimal _tmpInterestRate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfInterestRate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfInterestRate);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpInterestRate = _tmp_3;
            }
            final LocalDate _tmpStartDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_5;
            }
            final LocalDate _tmpMaturityDate;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfMaturityDate)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfMaturityDate);
            }
            final LocalDate _tmp_7 = __converters.stringToLocalDate(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpMaturityDate = _tmp_7;
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new Deposit(_tmpId,_tmpAccountId,_tmpName,_tmpBank,_tmpCurrency,_tmpPrincipal,_tmpInterestRate,_tmpStartDate,_tmpMaturityDate,_tmpStatus,_tmpNote);
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
  public Object getActiveList(final long accountId,
      final Continuation<? super List<Deposit>> $completion) {
    final String _sql = "SELECT * FROM deposits WHERE accountId = ? AND status = 'active' ORDER BY startDate DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Deposit>>() {
      @Override
      @NonNull
      public List<Deposit> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfMaturityDate = CursorUtil.getColumnIndexOrThrow(_cursor, "maturityDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final List<Deposit> _result = new ArrayList<Deposit>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Deposit _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBank;
            _tmpBank = _cursor.getString(_cursorIndexOfBank);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpPrincipal;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPrincipal)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPrincipal);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpPrincipal = _tmp_1;
            }
            final BigDecimal _tmpInterestRate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfInterestRate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfInterestRate);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpInterestRate = _tmp_3;
            }
            final LocalDate _tmpStartDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_5;
            }
            final LocalDate _tmpMaturityDate;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfMaturityDate)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfMaturityDate);
            }
            final LocalDate _tmp_7 = __converters.stringToLocalDate(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpMaturityDate = _tmp_7;
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _item = new Deposit(_tmpId,_tmpAccountId,_tmpName,_tmpBank,_tmpCurrency,_tmpPrincipal,_tmpInterestRate,_tmpStartDate,_tmpMaturityDate,_tmpStatus,_tmpNote);
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
  public Object getById(final long id, final long accountId,
      final Continuation<? super Deposit> $completion) {
    final String _sql = "SELECT * FROM deposits WHERE id = ? AND accountId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    _argIndex = 2;
    _statement.bindLong(_argIndex, accountId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Deposit>() {
      @Override
      @Nullable
      public Deposit call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfAccountId = CursorUtil.getColumnIndexOrThrow(_cursor, "accountId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfBank = CursorUtil.getColumnIndexOrThrow(_cursor, "bank");
          final int _cursorIndexOfCurrency = CursorUtil.getColumnIndexOrThrow(_cursor, "currency");
          final int _cursorIndexOfPrincipal = CursorUtil.getColumnIndexOrThrow(_cursor, "principal");
          final int _cursorIndexOfInterestRate = CursorUtil.getColumnIndexOrThrow(_cursor, "interestRate");
          final int _cursorIndexOfStartDate = CursorUtil.getColumnIndexOrThrow(_cursor, "startDate");
          final int _cursorIndexOfMaturityDate = CursorUtil.getColumnIndexOrThrow(_cursor, "maturityDate");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfNote = CursorUtil.getColumnIndexOrThrow(_cursor, "note");
          final Deposit _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpAccountId;
            _tmpAccountId = _cursor.getLong(_cursorIndexOfAccountId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpBank;
            _tmpBank = _cursor.getString(_cursorIndexOfBank);
            final String _tmpCurrency;
            _tmpCurrency = _cursor.getString(_cursorIndexOfCurrency);
            final BigDecimal _tmpPrincipal;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfPrincipal)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfPrincipal);
            }
            final BigDecimal _tmp_1 = __converters.stringToBigDecimal(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpPrincipal = _tmp_1;
            }
            final BigDecimal _tmpInterestRate;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfInterestRate)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfInterestRate);
            }
            final BigDecimal _tmp_3 = __converters.stringToBigDecimal(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'java.math.BigDecimal', but it was NULL.");
            } else {
              _tmpInterestRate = _tmp_3;
            }
            final LocalDate _tmpStartDate;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfStartDate)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfStartDate);
            }
            final LocalDate _tmp_5 = __converters.stringToLocalDate(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpStartDate = _tmp_5;
            }
            final LocalDate _tmpMaturityDate;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfMaturityDate)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfMaturityDate);
            }
            final LocalDate _tmp_7 = __converters.stringToLocalDate(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.LocalDate', but it was NULL.");
            } else {
              _tmpMaturityDate = _tmp_7;
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final String _tmpNote;
            _tmpNote = _cursor.getString(_cursorIndexOfNote);
            _result = new Deposit(_tmpId,_tmpAccountId,_tmpName,_tmpBank,_tmpCurrency,_tmpPrincipal,_tmpInterestRate,_tmpStartDate,_tmpMaturityDate,_tmpStatus,_tmpNote);
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
