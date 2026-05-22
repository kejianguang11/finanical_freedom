package com.financial.freedom.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.financial.freedom.data.local.dao.AccountDao;
import com.financial.freedom.data.local.dao.AccountDao_Impl;
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao;
import com.financial.freedom.data.local.dao.DailyBreakdownItemDao_Impl;
import com.financial.freedom.data.local.dao.DailySummaryDao;
import com.financial.freedom.data.local.dao.DailySummaryDao_Impl;
import com.financial.freedom.data.local.dao.DepositDao;
import com.financial.freedom.data.local.dao.DepositDao_Impl;
import com.financial.freedom.data.local.dao.ExchangeRateDao;
import com.financial.freedom.data.local.dao.ExchangeRateDao_Impl;
import com.financial.freedom.data.local.dao.HoldingDao;
import com.financial.freedom.data.local.dao.HoldingDao_Impl;
import com.financial.freedom.data.local.dao.PriceSnapshotDao;
import com.financial.freedom.data.local.dao.PriceSnapshotDao_Impl;
import com.financial.freedom.data.local.dao.TransactionDao;
import com.financial.freedom.data.local.dao.TransactionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ExchangeRateDao _exchangeRateDao;

  private volatile DepositDao _depositDao;

  private volatile HoldingDao _holdingDao;

  private volatile PriceSnapshotDao _priceSnapshotDao;

  private volatile TransactionDao _transactionDao;

  private volatile DailySummaryDao _dailySummaryDao;

  private volatile DailyBreakdownItemDao _dailyBreakdownItemDao;

  private volatile AccountDao _accountDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `nickname` TEXT NOT NULL, `pinHash` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exchange_rates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `fromCurrency` TEXT NOT NULL, `toCurrency` TEXT NOT NULL, `date` TEXT NOT NULL, `rate` TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_exchange_rates_fromCurrency_toCurrency_date` ON `exchange_rates` (`fromCurrency`, `toCurrency`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `deposits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `name` TEXT NOT NULL, `bank` TEXT NOT NULL, `currency` TEXT NOT NULL, `principal` TEXT NOT NULL, `interestRate` TEXT NOT NULL, `startDate` TEXT NOT NULL, `maturityDate` TEXT NOT NULL, `status` TEXT NOT NULL, `note` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `holdings` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `type` TEXT NOT NULL, `symbol` TEXT NOT NULL, `name` TEXT NOT NULL, `market` TEXT NOT NULL, `currency` TEXT NOT NULL, `quantity` TEXT NOT NULL, `costPrice` TEXT NOT NULL, `costDate` TEXT NOT NULL, `note` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `price_snapshots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `holdingId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `date` TEXT NOT NULL, `unitPrice` TEXT NOT NULL, `currency` TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_price_snapshots_holdingId_date` ON `price_snapshots` (`holdingId`, `date`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `holdingId` INTEGER NOT NULL, `accountId` INTEGER NOT NULL, `type` TEXT NOT NULL, `date` TEXT NOT NULL, `price` TEXT NOT NULL, `quantity` TEXT NOT NULL, `fee` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_summaries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `date` TEXT NOT NULL, `totalValueCNY` TEXT NOT NULL, `dayChange` TEXT NOT NULL, `dayChangePct` TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_summaries_date_accountId` ON `daily_summaries` (`date`, `accountId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_breakdown_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `date` TEXT NOT NULL, `type` TEXT NOT NULL, `valueCNY` TEXT NOT NULL, `changeCNY` TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_breakdown_items_date_accountId_type` ON `daily_breakdown_items` (`date`, `accountId`, `type`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'aa94a62da71f01c84b1e6b438efba5b5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `accounts`");
        db.execSQL("DROP TABLE IF EXISTS `exchange_rates`");
        db.execSQL("DROP TABLE IF EXISTS `deposits`");
        db.execSQL("DROP TABLE IF EXISTS `holdings`");
        db.execSQL("DROP TABLE IF EXISTS `price_snapshots`");
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `daily_summaries`");
        db.execSQL("DROP TABLE IF EXISTS `daily_breakdown_items`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAccounts = new HashMap<String, TableInfo.Column>(4);
        _columnsAccounts.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("nickname", new TableInfo.Column("nickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("pinHash", new TableInfo.Column("pinHash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAccounts.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAccounts = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAccounts = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAccounts = new TableInfo("accounts", _columnsAccounts, _foreignKeysAccounts, _indicesAccounts);
        final TableInfo _existingAccounts = TableInfo.read(db, "accounts");
        if (!_infoAccounts.equals(_existingAccounts)) {
          return new RoomOpenHelper.ValidationResult(false, "accounts(com.financial.freedom.data.local.entity.Account).\n"
                  + " Expected:\n" + _infoAccounts + "\n"
                  + " Found:\n" + _existingAccounts);
        }
        final HashMap<String, TableInfo.Column> _columnsExchangeRates = new HashMap<String, TableInfo.Column>(5);
        _columnsExchangeRates.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExchangeRates.put("fromCurrency", new TableInfo.Column("fromCurrency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExchangeRates.put("toCurrency", new TableInfo.Column("toCurrency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExchangeRates.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExchangeRates.put("rate", new TableInfo.Column("rate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExchangeRates = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExchangeRates = new HashSet<TableInfo.Index>(1);
        _indicesExchangeRates.add(new TableInfo.Index("index_exchange_rates_fromCurrency_toCurrency_date", true, Arrays.asList("fromCurrency", "toCurrency", "date"), Arrays.asList("ASC", "ASC", "ASC")));
        final TableInfo _infoExchangeRates = new TableInfo("exchange_rates", _columnsExchangeRates, _foreignKeysExchangeRates, _indicesExchangeRates);
        final TableInfo _existingExchangeRates = TableInfo.read(db, "exchange_rates");
        if (!_infoExchangeRates.equals(_existingExchangeRates)) {
          return new RoomOpenHelper.ValidationResult(false, "exchange_rates(com.financial.freedom.data.local.entity.ExchangeRate).\n"
                  + " Expected:\n" + _infoExchangeRates + "\n"
                  + " Found:\n" + _existingExchangeRates);
        }
        final HashMap<String, TableInfo.Column> _columnsDeposits = new HashMap<String, TableInfo.Column>(11);
        _columnsDeposits.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("bank", new TableInfo.Column("bank", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("principal", new TableInfo.Column("principal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("interestRate", new TableInfo.Column("interestRate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("startDate", new TableInfo.Column("startDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("maturityDate", new TableInfo.Column("maturityDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDeposits.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDeposits = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDeposits = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDeposits = new TableInfo("deposits", _columnsDeposits, _foreignKeysDeposits, _indicesDeposits);
        final TableInfo _existingDeposits = TableInfo.read(db, "deposits");
        if (!_infoDeposits.equals(_existingDeposits)) {
          return new RoomOpenHelper.ValidationResult(false, "deposits(com.financial.freedom.data.local.entity.Deposit).\n"
                  + " Expected:\n" + _infoDeposits + "\n"
                  + " Found:\n" + _existingDeposits);
        }
        final HashMap<String, TableInfo.Column> _columnsHoldings = new HashMap<String, TableInfo.Column>(11);
        _columnsHoldings.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("symbol", new TableInfo.Column("symbol", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("market", new TableInfo.Column("market", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("quantity", new TableInfo.Column("quantity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("costPrice", new TableInfo.Column("costPrice", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("costDate", new TableInfo.Column("costDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHoldings.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHoldings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHoldings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHoldings = new TableInfo("holdings", _columnsHoldings, _foreignKeysHoldings, _indicesHoldings);
        final TableInfo _existingHoldings = TableInfo.read(db, "holdings");
        if (!_infoHoldings.equals(_existingHoldings)) {
          return new RoomOpenHelper.ValidationResult(false, "holdings(com.financial.freedom.data.local.entity.Holding).\n"
                  + " Expected:\n" + _infoHoldings + "\n"
                  + " Found:\n" + _existingHoldings);
        }
        final HashMap<String, TableInfo.Column> _columnsPriceSnapshots = new HashMap<String, TableInfo.Column>(6);
        _columnsPriceSnapshots.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceSnapshots.put("holdingId", new TableInfo.Column("holdingId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceSnapshots.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceSnapshots.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceSnapshots.put("unitPrice", new TableInfo.Column("unitPrice", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPriceSnapshots.put("currency", new TableInfo.Column("currency", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPriceSnapshots = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPriceSnapshots = new HashSet<TableInfo.Index>(1);
        _indicesPriceSnapshots.add(new TableInfo.Index("index_price_snapshots_holdingId_date", true, Arrays.asList("holdingId", "date"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoPriceSnapshots = new TableInfo("price_snapshots", _columnsPriceSnapshots, _foreignKeysPriceSnapshots, _indicesPriceSnapshots);
        final TableInfo _existingPriceSnapshots = TableInfo.read(db, "price_snapshots");
        if (!_infoPriceSnapshots.equals(_existingPriceSnapshots)) {
          return new RoomOpenHelper.ValidationResult(false, "price_snapshots(com.financial.freedom.data.local.entity.PriceSnapshot).\n"
                  + " Expected:\n" + _infoPriceSnapshots + "\n"
                  + " Found:\n" + _existingPriceSnapshots);
        }
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(8);
        _columnsTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("holdingId", new TableInfo.Column("holdingId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("price", new TableInfo.Column("price", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("quantity", new TableInfo.Column("quantity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("fee", new TableInfo.Column("fee", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.financial.freedom.data.local.entity.Transaction).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsDailySummaries = new HashMap<String, TableInfo.Column>(6);
        _columnsDailySummaries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummaries.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummaries.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummaries.put("totalValueCNY", new TableInfo.Column("totalValueCNY", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummaries.put("dayChange", new TableInfo.Column("dayChange", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailySummaries.put("dayChangePct", new TableInfo.Column("dayChangePct", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailySummaries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailySummaries = new HashSet<TableInfo.Index>(1);
        _indicesDailySummaries.add(new TableInfo.Index("index_daily_summaries_date_accountId", true, Arrays.asList("date", "accountId"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoDailySummaries = new TableInfo("daily_summaries", _columnsDailySummaries, _foreignKeysDailySummaries, _indicesDailySummaries);
        final TableInfo _existingDailySummaries = TableInfo.read(db, "daily_summaries");
        if (!_infoDailySummaries.equals(_existingDailySummaries)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_summaries(com.financial.freedom.data.local.entity.DailySummary).\n"
                  + " Expected:\n" + _infoDailySummaries + "\n"
                  + " Found:\n" + _existingDailySummaries);
        }
        final HashMap<String, TableInfo.Column> _columnsDailyBreakdownItems = new HashMap<String, TableInfo.Column>(6);
        _columnsDailyBreakdownItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyBreakdownItems.put("accountId", new TableInfo.Column("accountId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyBreakdownItems.put("date", new TableInfo.Column("date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyBreakdownItems.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyBreakdownItems.put("valueCNY", new TableInfo.Column("valueCNY", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDailyBreakdownItems.put("changeCNY", new TableInfo.Column("changeCNY", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDailyBreakdownItems = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDailyBreakdownItems = new HashSet<TableInfo.Index>(1);
        _indicesDailyBreakdownItems.add(new TableInfo.Index("index_daily_breakdown_items_date_accountId_type", true, Arrays.asList("date", "accountId", "type"), Arrays.asList("ASC", "ASC", "ASC")));
        final TableInfo _infoDailyBreakdownItems = new TableInfo("daily_breakdown_items", _columnsDailyBreakdownItems, _foreignKeysDailyBreakdownItems, _indicesDailyBreakdownItems);
        final TableInfo _existingDailyBreakdownItems = TableInfo.read(db, "daily_breakdown_items");
        if (!_infoDailyBreakdownItems.equals(_existingDailyBreakdownItems)) {
          return new RoomOpenHelper.ValidationResult(false, "daily_breakdown_items(com.financial.freedom.data.local.entity.DailyBreakdownItem).\n"
                  + " Expected:\n" + _infoDailyBreakdownItems + "\n"
                  + " Found:\n" + _existingDailyBreakdownItems);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "aa94a62da71f01c84b1e6b438efba5b5", "4d04c1b5ebde0767dab6b5932fe08520");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "accounts","exchange_rates","deposits","holdings","price_snapshots","transactions","daily_summaries","daily_breakdown_items");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `accounts`");
      _db.execSQL("DELETE FROM `exchange_rates`");
      _db.execSQL("DELETE FROM `deposits`");
      _db.execSQL("DELETE FROM `holdings`");
      _db.execSQL("DELETE FROM `price_snapshots`");
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `daily_summaries`");
      _db.execSQL("DELETE FROM `daily_breakdown_items`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ExchangeRateDao.class, ExchangeRateDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DepositDao.class, DepositDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HoldingDao.class, HoldingDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PriceSnapshotDao.class, PriceSnapshotDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(TransactionDao.class, TransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DailySummaryDao.class, DailySummaryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DailyBreakdownItemDao.class, DailyBreakdownItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AccountDao.class, AccountDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ExchangeRateDao exchangeRateDao() {
    if (_exchangeRateDao != null) {
      return _exchangeRateDao;
    } else {
      synchronized(this) {
        if(_exchangeRateDao == null) {
          _exchangeRateDao = new ExchangeRateDao_Impl(this);
        }
        return _exchangeRateDao;
      }
    }
  }

  @Override
  public DepositDao depositDao() {
    if (_depositDao != null) {
      return _depositDao;
    } else {
      synchronized(this) {
        if(_depositDao == null) {
          _depositDao = new DepositDao_Impl(this);
        }
        return _depositDao;
      }
    }
  }

  @Override
  public HoldingDao holdingDao() {
    if (_holdingDao != null) {
      return _holdingDao;
    } else {
      synchronized(this) {
        if(_holdingDao == null) {
          _holdingDao = new HoldingDao_Impl(this);
        }
        return _holdingDao;
      }
    }
  }

  @Override
  public PriceSnapshotDao priceSnapshotDao() {
    if (_priceSnapshotDao != null) {
      return _priceSnapshotDao;
    } else {
      synchronized(this) {
        if(_priceSnapshotDao == null) {
          _priceSnapshotDao = new PriceSnapshotDao_Impl(this);
        }
        return _priceSnapshotDao;
      }
    }
  }

  @Override
  public TransactionDao transactionDao() {
    if (_transactionDao != null) {
      return _transactionDao;
    } else {
      synchronized(this) {
        if(_transactionDao == null) {
          _transactionDao = new TransactionDao_Impl(this);
        }
        return _transactionDao;
      }
    }
  }

  @Override
  public DailySummaryDao dailySummaryDao() {
    if (_dailySummaryDao != null) {
      return _dailySummaryDao;
    } else {
      synchronized(this) {
        if(_dailySummaryDao == null) {
          _dailySummaryDao = new DailySummaryDao_Impl(this);
        }
        return _dailySummaryDao;
      }
    }
  }

  @Override
  public DailyBreakdownItemDao dailyBreakdownItemDao() {
    if (_dailyBreakdownItemDao != null) {
      return _dailyBreakdownItemDao;
    } else {
      synchronized(this) {
        if(_dailyBreakdownItemDao == null) {
          _dailyBreakdownItemDao = new DailyBreakdownItemDao_Impl(this);
        }
        return _dailyBreakdownItemDao;
      }
    }
  }

  @Override
  public AccountDao accountDao() {
    if (_accountDao != null) {
      return _accountDao;
    } else {
      synchronized(this) {
        if(_accountDao == null) {
          _accountDao = new AccountDao_Impl(this);
        }
        return _accountDao;
      }
    }
  }
}
