# 扶摇阁 — 开发任务拆解

> 执行原则：按阶段顺序执行，每个任务完成后打勾 `[x]`。依赖关系严格遵守，不可跳过前置任务。

## 当前进度

**已完成：阶段 0-13（全部完成）**

| 阶段 | 进度 | 说明 |
|------|------|------|
| 0 - 脚手架 | done | Gradle / Theme / Manifest / App |
| 1 - 数据层 | done | 8 Entity + 7 DAO + AppDatabase v2 + TypeConverter |
| 2 - DI | done | DatabaseModule + NetworkModule |
| 3 - 领域层 | done | InterestCalculator + ValuationCalculator + BackfillEngine |
| 4 - Repository | done | 4 个 Repository + AccountManager |
| 5 - 网络层 | done | 5 个 Provider 全部对接真实 API |
| 6 - 补算引擎 | done | 启动触发 → 逐日计算 → 写入 Summary |
| 7 - 导航框架 | done | 4 Tab + FAB + BottomSheet + Auth 路由 |
| 8 - UI 页面 | done | 12 Screen + 7 ViewModel 全部数据绑定 |
| 9 - 共享组件 | done | AssetCard、TrendChart、CalendarView、FlowRow 布局 |
| 10 - CSV | done | CsvExporter + CsvImporter + 预览 + 分享 |
| 11 - 集成打磨 | done | 错误处理、空状态、分享导出、搜索功能 |
| 12 - 账号系统 | done | Account 实体、PIN 解锁、账号切换、数据隔离、动态启动路由、级联删除、PIN 修改 |
| 13 - 测试数据 | done | TestDataGenerator ✅、设置页一键生成 ✅ |
| 14 - Bug 修复 | done | 7 项修复（详见下方） ✅ |
| 15 - 实时价格 & 视觉升级 | done | 6 个子任务（详见下方） ✅ |
| 16 - 数据一致性 & Bug 修复 | done | 5 个子任务 ✅ |
| 17 - UI 视觉重设计 | done | 5 个子任务 ✅ |
| 18 - 数据正确性修复 | done | 7 个子任务 ✅ |
| 19 - 资产体系扩展 | done | 现金 + 应收 + 负债 + 存款到期 ✅ |
| 20 - 更名 & 日期选择器 & Modified Dietz | pending | 7 个子任务 |
| 21 - 全 App 连续滑动体系 | done | 8 个子任务（v3 核心架构） ✅ |
| 22 - 导航优化 & 首页视觉升级 | done | 5 个子任务 ✅ |
| 23 - 统一CSV & 名称回填 & 现金扣款 | pending | 6 个子任务 |
| 24 - 自动搜索 & 港股搜索 & 黄金简化 & 多笔交易 | done | 8 个子任务 |
| 25 - 持仓分组重设计 | pending | 7 个子任务（v17 核心特性） |
| 26 - 一键重算收益 & 显示倍率 | done | 已实现 ✅ |
| 27 - 现金/信用 v22 重设计 | done | 已实现 ✅ |
| 28 - 黄金 v23 重设计 | done | 单一资产多次买入 ✅（被 v24 替代） |
| 29 - 黄金 v24 两层重设计 | pending | 6 个子任务（概览卡片 + 独立详情 + 减仓 + 删除） |
| 30 - 黄金走势图坐标轴标注 | in_progress | Y轴价格标签 + 网格线 + X轴日期（GoldDetailChart + GoldChartWithBuyMarkers） |
| 31 - 年度收益/总资产切换 | done | 3 个子任务（年视图 pill 切换 + 数据模型 + UI） ✅ |

---

## 阶段 14：Bug 修复（2026-05-22）

> 修复 7 个已知问题，详见 PRD 第十二章。

### 14.1 首页资产分类日收益 +0 修复
- [x] `HomeViewModel.kt`：新增 `breakdownChange()` 方法，`computeFromEntities` 按资产类型分别计算日变化
- [x] `HomeScreen.kt`：AssetCard 显示分类日收益，涨红色跌绿色

### 14.2 走势图坐标轴和触摸交互
- [x] `TrendChart.kt`：重写组件，添加 Y 轴数值标签（自动格式化万/千）、X 轴日期标签、网格线
- [x] `TrendChart.kt`：添加 `detectTapGestures` 触摸交互，点击拐点弹出 tooltip（日期/金额/日变化/百分比）

### 14.3 子页面底部导航栏恢复
- [x] `FinancialFreedomApp.kt`：将 `HoldingDetail`、`AddDeposit`、`EditDeposit`、`AddHolding`、`EditHolding` 加入 `subRoutes`
- [x] `showBottomBar` 覆盖 tabRoutes + subRoutes，`showFab` 仅覆盖 tabRoutes

### 14.4 持仓 Tab 滑动切换
- [x] `HoldingsScreen.kt`：使用 `HorizontalPager` + `rememberPagerState` 实现左右滑动切换 Tab

### 14.5 股票详情页数据修复
- [x] `HoldingDetailViewModel.kt`：通过 `getByHoldingAndDate` 获取今日/昨日价格，正确计算 todayChange 和 todayChangePct
- [x] `HoldingDetailScreen.kt`：价格图表正确计算日变化，空数据时显示占位提示

### 14.6 收益视图重构（日/周/月/年）
- [x] `EarningsViewModel.kt`：周视图展示近6周（每周区间如 "5.5-5.11"），月视图展示12个月汇总，年视图展示多年汇总
- [x] `EarningsScreen.kt`：周视图改为卡片式（周区间+柱状图），月视图改为12月列表，年视图改为年度卡片

### 14.7 统计块尺寸统一
- [x] `CalendarView.kt`：日历格子固定 48dp 高度，数字使用紧凑格式（≥10000 显示"万"）
- [x] 所有视图块状元素使用固定尺寸，不因数字位数变化而拉伸

---

## 阶段 15：实时价格 & 视觉升级（2026-05-22）

> 依赖：阶段 0-14 全部完成
> 执行顺序：15.1 → 15.2 → 15.3 → 15.4 → 15.5 → 15.6

### 15.1 实时价格拉取体系

- [x] `USStockProvider.kt`：改用 Finnhub 免费接口（60次/分钟），移除 Alpha Vantage demo key
- [x] `HomeViewModel.kt`：新增 `fetchLivePrices()` 方法，启动时异步拉取所有持仓最新价格
  - 遍历 holdings → 调 `PriceService.fetchPrice()` → 写入 `PriceSnapshot`
  - 拉取成功 → 重算 `DailySummary` → `INSERT OR REPLACE` 写入 DB
  - 拉取失败 → 降级用缓存 → 更新 `lastUpdateTime` 状态
- [x] `HomeUiState`：新增 `lastUpdateTime: String?`（格式 HH:mm）
- [x] `DailySummary.kt` + `DailyBreakdownItem.kt`：添加 unique index 支持 upsert
- [x] `DailyBreakdownItemDao.kt`：新增 `deleteByDate` 方法
- [x] `SummaryRepository.kt`：新增 `saveTodaySummary` 方法
- [x] `AppDatabase.kt`：version bump 到 3

### 15.2 首页信息层级重构

- [x] `HomeScreen.kt`：重新设计布局
  - 今日收益提到最顶部，40sp ExtraBold + 涨跌色背景
  - 总资产弱化为 18sp 常规字重
  - 分类卡片改为 3+1 网格（存款/股票/基金 一行三个，黄金单独一行）
  - 新增 `LastUpdateTime` 组件（显示「价格更新于 HH:mm」）
- [x] `HomeViewModel.kt`：`refresh()` 方法改为真正的网络拉取 + 重算
- [x] `HomeScreen.kt`：所有卡片使用 Material Icons 替代 emoji

### 15.3 收益周视图按月分组

- [x] `EarningsViewModel.kt`：新增 `currentViewMonth`、`changeViewMonth()`、`toggleWeekExpanded()`
- [x] `EarningsScreen.kt`：周视图重构为按月分组 + 月份导航箭头 + 每周卡片 + 点击展开明细
- [x] `EarningsUiState`：新增 `currentViewMonth`、`weekExpandedIndex` 字段

### 15.4 存款卡片显示日收益

- [x] `HoldingsViewModel.kt`：`DepositDisplay` 新增 `todayInterest` 字段
- [x] `HoldingsScreen.kt`：`DepositCard` 新增今日利息行，红涨绿跌着色

### 15.5 UI 视觉升级

- [x] `FinancialColors.kt`：扩充配色（upBg/downBg + 四类资产色）
- [x] `HomeScreen.kt`：卡片使用 `ElevatedCard` + Material Icons
- [x] `HoldingsScreen.kt`：DepositCard/HoldingCard 使用 `ElevatedCard` + 微投影
- [x] `EarningsScreen.kt`：所有卡片统一 `ElevatedCard`
- [x] 全局 spacing 统一：卡片间 10-12dp，section 间 16-20dp，卡片内 14-16dp
- [x] 导航栏已使用 Outlined/Filled 双状态切换

### 15.6 底部导航栏验证

- [x] `FinancialFreedomApp.kt`：`subRoutes` 已包含 `HoldingDetail`、`AddDeposit`、`EditDeposit`、`AddHolding`、`EditHolding`
- [x] `showBottomBar` = `currentRoute in tabRoutes || currentRoute in subRoutes` ✅
- [x] `showFab` 仅 `currentRoute in tabRoutes` ✅

---

## 阶段 16：数据一致性 & Bug 修复（2026-05-22 第三轮）

> 依赖：阶段 0-15 全部完成
> 执行顺序：16.1 → 16.2 → 16.3 → 16.4 → 16.5

### 16.1 详情页数据空白修复

- [x] `HoldingDetailViewModel.kt`：将 `transactionDao.getByHolding(...).collect {}` 改为 `.first()` 避免永久阻塞
- [x] `HoldingDetailViewModel.kt`：将 `priceSnapshotDao.getByHoldingAndDateRange(...).collect {}` 改为 `.first()`
- [x] `HoldingDetailViewModel.kt`：优化价格获取逻辑，getByHoldingAndDate 失败时用 getLatest 降级

### 16.2 今日收益实时更新

- [x] `HomeViewModel.kt`：`fetchLivePrices()` 成功后设置 `needsRecompute = true`
- [x] `HomeViewModel.kt`：`refreshData()` 检查 `needsRecompute`，若为 true 则强制调用 `computeFromEntities` 不读缓存
- [x] 今日 DailySummary 在 `computeFromEntities` 中持久化到 DB

### 16.3 脏数据标记与补算

- [x] `DailySummaryDao.kt`：新增 `deleteFromDate(date, accountId)` — 删除 >= date 的记录
- [x] `DailyBreakdownItemDao.kt`：新增 `deleteFromDate(date, accountId)` — 删除 >= date 的记录
- [x] `BackfillEngine.kt`：新增 `markDirtyAndBackfill(fromDate, accountId)` 方法
- [x] 新增资产/编辑资产/删除资产后触发 `markDirtyAndBackfill`
- [x] `TestDataGenerator.kt`：插入数据后调用 `markDirtyAndBackfill` 而非简单的 `backfillIfNeeded`

### 16.4 价格 API 日志验证

- [x] 各 Provider 添加 `Log.d(TAG, ...)` 输出 API URL 和返回数据摘要
- [x] `HomeViewModel.fetchLivePrices()` 添加汇总日志

### 16.5 底部导航栏确认

- [x] `FinancialFreedomApp.kt`：review 确认 subRoutes 覆盖完整
- [x] 实测验证底部导航在子页面可见

### 0.1 创建 Android 项目
- [x] 用 Android Studio 创建新项目，包名 `com.financial.freedom`
- [x] minSdk = 31，targetSdk = 35，compileSdk = 35
- [x] 启用 Compose，Kotlin 2.0+

### 0.2 Gradle 依赖
- [x] `build.gradle.kts`（project 级别）添加 Hilt 插件、Kotlin Serialization 插件
- [x] `build.gradle.kts`（app 级别）添加以下依赖：

| 类别 | 依赖 |
|------|------|
| Compose | BOM、Material 3、Icons Extended、Navigation |
| Room | runtime、compiler（KSP）、ktx |
| Hilt | hilt-android、hilt-compiler（KSP）、hilt-navigation-compose |
| Network | Retrofit、OkHttp、Kotlinx Serialization converter |
| 图表 | Vico（compose-m3） |
| 序列化 | Kotlinx Serialization JSON |
| 其他 | kotlinx-datetime |

### 0.3 主题配置
- [x] `Theme.kt`：Material 3 + `dynamicColor`（Material You 跟随壁纸）
- [x] `Color.kt`：定义涨红色 `#E53935`、跌绿色 `#43A047`
- [x] `Type.kt`：默认字体配置
- [x] 深色/浅色模式自适应

### 0.4 Application 类
- [x] 创建 `App.kt`，添加 `@HiltAndroidApp` 注解
- [x] `AndroidManifest.xml` 注册 Application、添加 INTERNET 权限

---

## 阶段 1：数据层（Room）

### 1.1 TypeConverter
- [x] `Converters.kt`：`LocalDate` ↔ `String`、`BigDecimal` ↔ `String`

### 1.2 Entity（7 个）

按 PRD 第六章定义：

- [x] `ExchangeRate.kt` — fromCurrency, toCurrency, date, rate
- [x] `Deposit.kt` — name, bank, currency, principal, interestRate, startDate, maturityDate, status, note
- [x] `Holding.kt` — type(STOCK/FUND/GOLD), symbol, name, market, currency, quantity, costPrice, costDate, note
- [x] `PriceSnapshot.kt` — holdingId, date, unitPrice, currency
- [x] `Transaction.kt` — holdingId, type(BUY/SELL/DIVIDEND), date, price, quantity, fee
- [x] `DailySummary.kt` — date, totalValueCNY, dayChange, dayChangePct
- [x] `DailyBreakdownItem.kt` — date, type, valueCNY, changeCNY

### 1.3 DAO（6 个）

- [x] `DepositDao.kt`
  - `getAll(): Flow<List<Deposit>>`
  - `getById(id: Long): Deposit?`
  - `insert(deposit: Deposit): Long`
  - `update(deposit: Deposit)`
  - `delete(deposit: Deposit)`

- [x] `HoldingDao.kt`
  - `getAll(): Flow<List<Holding>>`
  - `getByType(type: String): Flow<List<Holding>>`
  - `getById(id: Long): Holding?`
  - `insert(holding: Holding): Long`
  - `update(holding: Holding)`
  - `delete(holding: Holding)`

- [x] `DailySummaryDao.kt`
  - `getByDateRange(start: LocalDate, end: LocalDate): Flow<List<DailySummary>>`
  - `getByDate(date: LocalDate): DailySummary?`
  - `getLatestDate(): LocalDate?`
  - `insert(summary: DailySummary)`
  - `insertAll(summaries: List<DailySummary>)`

- [x] `DailyBreakdownItemDao.kt`
  - `getByDate(date: LocalDate): List<DailyBreakdownItem>`
  - `insertAll(items: List<DailyBreakdownItem>)`

- [x] `PriceSnapshotDao.kt`
  - `getByHoldingAndDateRange(holdingId: Long, start: LocalDate, end: LocalDate): Flow<List<PriceSnapshot>>`
  - `getLatest(holdingId: Long): PriceSnapshot?`
  - `insert(snapshot: PriceSnapshot)`
  - `insertAll(snapshots: List<PriceSnapshot>)`

- [x] `ExchangeRateDao.kt`
  - `getRate(from: String, to: String, date: LocalDate): ExchangeRate?`
  - `getLatestRates(): List<ExchangeRate>`
  - `insert(rate: ExchangeRate)`
  - `insertAll(rates: List<ExchangeRate>)`

- [x] `TransactionDao.kt`
  - `getByHolding(holdingId: Long): Flow<List<Transaction>>`
  - `insert(transaction: Transaction)`
  - `delete(transaction: Transaction)`

### 1.4 AppDatabase
- [x] `AppDatabase.kt`：声明所有 Entity，暴露所有 DAO，配置 TypeConverter
- [x] 版本号 = 1，暂不需要 migration

---

## 阶段 2：依赖注入

### 2.1 Hilt Module
- [x] `DatabaseModule.kt` — 提供 AppDatabase、各 DAO 实例
- [x] `NetworkModule.kt` — 提供 Retrofit、OkHttpClient（暂无具体 Service，先搭壳）
- [x] `RepositoryModule.kt` — 绑定各 Repository 实现

---

## 阶段 3：领域层

### 3.1 类型定义
- [x] `PriceResult.kt` — data class（price: BigDecimal, currency: String, date: LocalDate）
- [x] `SearchResult.kt` — data class（symbol: String, name: String, market: String, type: String）
- [x] `AssetType.kt` — enum（DEPOSIT, STOCK, FUND, GOLD）

### 3.2 InterestCalculator
- [x] 日息 = 本金 × 年化利率 ÷ 365
- [x] 累计利息 = 日息 × 持有天数
- [x] 持有天数 = (today - startDate) 或 (maturityDate - startDate)，以较早者为准
- [x] 单测：验证日息计算、累计利息、到期后不再计息

### 3.3 ValuationCalculator
- [x] `calcDepositValueCNY(deposit, exchangeRate)` → 估算 CNY 估值
- [x] `calcHoldingValueCNY(holding, currentPrice, exchangeRate?)` → 估算 CNY 估值
- [x] 单测：覆盖各币种换算、零利率、边界情况

---

## 阶段 4：Repository 层

### 4.1 DepositRepository
- [x] 封装 DepositDao 的 CRUD
- [x] 返回 `Flow<List<DepositWithValuation>>`（包含实时计算的估值）

### 4.2 HoldingRepository
- [x] 封装 HoldingDao 的 CRUD
- [x] 返回 `Flow<List<HoldingWithValuation>>`（包含当前价格与盈亏）

### 4.3 SummaryRepository
- [x] `getDailySummaries(start, end): Flow<List<DailySummary>>`
- [x] `getBreakdown(date): List<DailyBreakdownItem>`
- [x] `getLatestDate(): LocalDate?`
- [x] `saveDailySummary(summary, breakdowns)`

### 4.4 ExchangeRateRepository
- [x] 先从本地缓存查询汇率
- [x] 缓存未命中或过期 → 调网络层拉取 → 写入缓存
- [x] 降级：网络失败时使用最近缓存

---

## 阶段 5：网络层

### 5.1 PriceProvider 接口
- [x] `suspend fun fetchPrice(symbol, date): PriceResult?`
- [x] `suspend fun fetchHistory(symbol, start, end): List<PriceResult>`
- [x] `suspend fun search(query): List<SearchResult>`

### 5.2 AStockProvider（A股）
- [x] 对接新浪财经/东方财富免费接口
- [x] 支持搜索（代码或名称）
- [x] 支持拉取历史日线

### 5.3 USStockProvider（美股）
- [x] 对接 Alpha Vantage 或 Finnhub
- [x] 支持搜索
- [x] 支持拉取历史日线

### 5.4 CNFundProvider（国内基金）
- [x] 对接天天基金接口
- [x] 拉取每日净值
- [x] 支持搜索

### 5.5 GoldProvider（黄金）
- [x] 对接国际金价 API（XAU）
- [x] 返回 CNY/g 价格

### 5.6 ExchangeRateProvider（汇率）
- [x] 对接 Frankfurter API（免费）
- [x] 支持历史汇率查询
- [x] 拉取 USD/JPY/HKD/EUR → CNY

### 5.7 PriceService（统一入口）
- [x] 组合所有 Provider
- [x] `getPrice(type, symbol, date)` → 路由到对应 Provider
- [x] `searchAll(query)` → 并发搜索所有市场

---

## 阶段 6：补算引擎（核心）

### 6.1 BackfillEngine
- [x] `backfillIfNeeded()` — App 启动时调用
- [x] 查询 `DailySummary` 最后日期 lastDate
- [x] 如果没有记录（首次使用），从最早资产日期开始补算
- [x] 如果 lastDate < today，补算 lastDate+1 到 yesterday
- [x] 今天的数据通过实时计算展示，不写入 DailySummary

### 6.2 单日补算
- [x] `backfillDay(date)`：
  1. 遍历所有 active 存款 → 计算 CNY 估值（需要当日汇率）
  2. 遍历所有 Holding → 拉当日价格 → 计算 CNY 估值
  3. 汇总 totalValueCNY，对比上日计算 dayChange
  4. 按资产类型拆分写入 DailyBreakdownItem
  5. 写入 DailySummary

### 6.3 降级处理
- [x] 拉不到当日价格的 Holding → 沿用上一日价格 → 标注 stale
- [x] 拉不到当日汇率 → 沿用上一日汇率 → 标注 stale

---

## 阶段 7：导航框架

### 7.1 Route 定义
- [x] 定义 sealed class Route：
  - `Home`、`Holdings`、`Earnings`、`Settings`
  - `HoldingDetail(holdingId: Long)`
  - `AddDeposit`、`AddStock`、`AddFund`、`AddGold`
  - `EditDeposit(id)`、`EditHolding(id)`

### 7.2 Navigation 实现
- [x] `AppNavigation.kt`：NavHost + 各路由 composable
- [x] 底部导航栏 `BottomNavBar.kt`（🏠首页 / 📋持仓 / 📊收益 / ⚙️设置）
- [x] 中间悬浮 ➕ 按钮，点击弹出资产类型选择 BottomSheet
- [x] `App.kt` 的 `setContent` 组装 Scaffold + NavHost

---

## 阶段 8：UI 页面

### 8.1 首页（HomeScreen + HomeViewModel）
- [x] ViewModel：
  - 从 SummaryRepository 获取 totalValueCNY、todayChange
  - 从各 Repository 获取分类汇总
  - 从 SummaryRepository 获取走势图数据（7天/30天/1年）
- [x] UI：
  - 总资产大数字 + 今日收益（红涨绿跌）
  - Vico 折线走势图（7d/30d/1y 切换）
  - 4 张资产分类卡片（网格布局），点击跳转持仓对应 Tab
  - 下拉刷新

### 8.2 持仓列表（HoldingsScreen + HoldingsViewModel）
- [x] ViewModel：按类型过滤，获取列表数据
- [x] UI：
  - `[全部] [存款] [股票] [基金] [黄金]` Tab 栏
  - 存款卡片：名称、本金、利率、累计利息、到期日、估值
  - 股票/基金卡片：名称、代码、持有量、成本价、当前价、盈亏
  - 黄金卡片：形式、重量、买入价、当前价、盈亏
  - 左滑删除（SwipeToDismiss）
  - 点击跳转详情

### 8.3 新增/编辑表单
- [x] `AddDepositScreen.kt` — 存款表单（7 字段）
- [x] `EditDepositScreen.kt` — 复用表单，预填数据
- [x] `AddHoldingScreen.kt` — 股票/基金/黄金 统一表单，含在线搜索
- [x] `EditHoldingScreen.kt` — 复用表单，预填数据
- [x] 所有表单含输入校验

### 8.4 持仓详情（DetailScreen + DetailViewModel）
- [x] ViewModel：获取单个 Holding + 价格历史 + 交易记录
- [x] UI：
  - 当前价格 + 今日涨跌
  - Vico 小面积走势图（1月/3月/1年 切换）
  - 持仓盈亏 + 今日盈亏 双卡片
  - 持有信息（数量、成本、市值）
  - 交易记录可折叠列表
  - 编辑 / 删除 按钮

### 8.5 收益日历（EarningsScreen + EarningsViewModel）
- [x] ViewModel：获取指定时间范围的 DailySummary + Breakdown
- [x] 日视图：
  - 月历网格，每格显示汇总 ± 金额
  - 涨红底跌绿底
  - 点击格子弹出 BottomSheet 展示分类明细
- [x] 周视图：Vico 柱状图，7 天
- [x] 月视图：月度汇总 + 每日迷你柱
- [x] 年视图：热力图（GitHub contributions 风格）
- [x] 顶部分段控制器切换

### 8.6 设置页（SettingsScreen + SettingsViewModel）
- [x] 数据导出 → BottomSheet 选「保存到文件」或「分享」
- [x] 数据导入 → SAF 文件选择器 → 解析预览 → 确认写入
- [x] 当前汇率展示 + 手动刷新按钮
- [x] 各数据源状态展示
- [x] 清空数据（二次确认）
- [x] 关于信息

---

## 阶段 9：共享组件

### 9.1 AssetCard
- [x] 可复用卡片组件，接受 title、subtitle、value、change、onClick
- [x] 红涨绿跌颜色逻辑
- [x] 大圆角 + 微投影 Material 3 卡片

### 9.2 TrendChart
- [x] 封装 Vico LineChart，接受 `List<Pair<LocalDate, BigDecimal>>`
- [x] 支持日/周/月/年数据缩放
- [x] 跟随 Material You 主题色

### 9.3 CalendarView
- [x] 自定义 Compose 月历组件（不依赖第三方）
- [x] 每格显示数字 + 收益金额
- [x] 红涨绿跌背景色
- [x] 可左右滑动切换月份
- [x] 点击回调

### 9.4 HeatmapView
- [x] 年视图热力图组件
- [x] N×53 格（类似 GitHub contributions）
- [x] 颜色深浅表示盈亏大小

### 9.5 SearchSheet
- [x] 搜索 BottomSheet
- [x] 输入关键词 → 实时显示搜索结果
- [x] 选中结果回填表单

---

## 阶段 10：CSV 导入导出

### 10.1 CsvExporter
- [x] 查询所有 Deposit → 生成 deposit.csv
- [x] 查询所有 Holding → 生成 holdings.csv
- [x] 查询所有 Transaction → 生成 transactions.csv
- [x] 通过 SAF 写入文件
- [x] 通过 ShareSheet 分享

### 10.2 CsvImporter
- [x] 通过 SAF 选择文件
- [x] 解析 CSV 头，校验字段完整性
- [x] 预览对话框（显示条数和前 3 行数据）
- [x] 确认后批量写入 Room
- [x] 写入完成后触发全量补算

---

## 阶段 11：集成与打磨

### 11.1 App 启动流程
- [x] `App.kt` 内 `LaunchedEffect` → 调 `BackfillEngine.backfillIfNeeded()`
- [x] 补算完成后刷新 UI

### 11.2 错误处理
- [x] 网络请求失败 Toast 提示
- [x] CSV 解析失败具体报错
- [x] 数据写入失败回滚

### 11.3 体验打磨
- [x] 页面切换动画（共享元素 / fade）
- [x] 下拉刷新动画
- [x] 空状态占位图（暂无持仓 / 暂无收益记录）
- [x] 删除确认对话框

### 11.4 自测
- [x] 新增存款 → 首页显示 → 持仓列表显示 → 收益日历显示
- [x] 新增股票 → 价格拉取成功 → 盈亏计算正确 → 走势图正常
- [x] 关闭 App 3 天再打开 → 补算正确 → 收益日历完整
- [x] CSV 导出 → 文件内容正确
- [x] CSV 导入 → 预览正确 → 写入成功
- [x] 深色模式切换正常

---

## 阶段 12：账号系统

### 12.1 Account 实体与 DAO
- [x] `Account.kt` — Entity（id, nickname, pinHash, createdAt）
- [x] `AccountDao.kt`
  - `getAll(): Flow<List<Account>>`
  - `getById(id: Long): Account?`
  - `insert(account: Account): Long`
  - `update(account: Account)`
  - `delete(account: Account)`

### 12.2 现有 Entity 加 accountId
- [x] `Deposit.kt` 加 `accountId: Long`
- [x] `Holding.kt` 加 `accountId: Long`
- [x] `PriceSnapshot.kt` 加 `accountId: Long`
- [x] `Transaction.kt` 加 `accountId: Long`
- [x] `DailySummary.kt` 加 `accountId: Long`
- [x] `DailyBreakdownItem.kt` 加 `accountId: Long`
- [x] DB 版本升级到 2（destructive migration）

### 12.3 DAO 查询加 accountId 过滤
- [x] `DepositDao` — 所有查询加 `WHERE accountId = :accountId`
- [x] `HoldingDao` — 所有查询加 `WHERE accountId = :accountId`
- [x] `DailySummaryDao` — 所有查询加 `WHERE accountId = :accountId`
- [x] `DailyBreakdownItemDao` — 所有查询加 `WHERE accountId = :accountId`
- [x] `PriceSnapshotDao` — 所有查询加 `WHERE accountId = :accountId`
- [x] `TransactionDao` — 所有查询加 `WHERE accountId = :accountId`

### 12.4 AccountManager（全局状态）
- [x] `AccountManager.kt` — Hilt @Singleton
  - `currentAccountId: StateFlow<Long?>`
  - `suspend fun switchAccount(accountId: Long)`
  - `suspend fun createAccount(nickname: String, pin: String): Account`
  - `fun logout()`
  - PIN 验证逻辑
- [x] `deleteAccount()` 级联删除该账号所有数据（deposits, holdings, summaries 等）

### 12.5 Auth UI 页面
- [x] `WelcomeScreen.kt` — 首次使用创建账号
- [x] `PinUnlockScreen.kt` — 冷启动 PIN 解锁（含切换账号、创建新账号）
- [x] `AccountListScreen.kt` — 账号列表（PIN 验证弹窗）

### 12.6 导航更新
- [x] `Route.kt` 新增 `Welcome`、`PinUnlock`、`AccountList`
- [x] `AppNavigation.kt` startDestination 动态判断（无账号→Welcome，有账号→PinUnlock）
- [x] `FinancialFreedomApp.kt` 用 AccountManager 控制显示 auth 还是主界面

### 12.7 ViewModel / Repository 适配
- [x] 所有 ViewModel 注入 AccountManager，查询时传 accountId
- [x] `BackfillEngine` 过滤当前账号数据
- [x] `CsvExporter` / `CsvImporter` 按 accountId 导出导入

### 12.8 设置页账号管理
- [x] 账号管理卡片（当前昵称、切换/创建/PIN修改/删除）
- [x] 切换账号：弹出账号列表 → 选号 → 输 PIN
- [x] 修改 PIN：旧 PIN → 新 PIN × 2
- [x] 删除账号：PIN 确认 → 级联删除所有关联数据

---

## 阶段 13：测试数据生成

### 13.1 TestDataGenerator
- [x] `TestDataGenerator.kt` — Hilt @Singleton
  - 生成 3 笔存款 + 2 股票 + 1 基金 + 1 黄金
  - 每只资产生成 90 天随机价格快照（±15% 波动）
  - 生成汇率（USD→CNY 7.15, HKD→CNY 0.92）
  - 写入完成后触发 BackfillEngine 补算 90 天 DailySummary

### 13.2 设置页入口
- [x] 设置页「一键生成测试数据」卡片
- [x] 点击 → 生成 → Toast 完成（含防重复生成提示）

---

## 执行顺序图

```
12.1 → 12.2 → 12.3           (Account 实体 + 现有表加列)
         ↓
12.4                         (AccountManager)
         ↓
12.5 → 12.6                  (Auth UI + 导航)
         ↓
12.7 → 12.8                  (ViewModel 适配 + 设置页)
         ↓
13.1 → 13.2                  (测试数据)
```

---

---

## 阶段 17：UI 视觉重设计（2026-05-22 第四轮）

> 依赖：阶段 0-16 全部完成
> 执行顺序：17.1 → 17.2 → 17.3 → 17.4 → 17.5

### 17.1 首页布局重构

- [x] `HomeScreen.kt`：去掉内层 `verticalScroll`（PullToRefreshBox 已自带滚动，嵌套导致顶部重叠）
- [x] `HomeScreen.kt`：信息层级重新排列
  - 顶部：总资产（32sp ExtraBold）+ 今日收益（20sp Bold 涨跌色）+ 更新时间
  - 去掉今日收益的半透明彩色背景卡片
  - 走势图区域：白色 ElevatedCard
  - 资产配置区域：白色 ElevatedCard 三列等高
- [x] `HomeScreen.kt`：PullToRefreshBox + Column 无嵌套滚动

### 17.2 资产卡片等高三列

- [x] `HomeScreen.kt`：AssetCard Row 加 `Modifier.height(IntrinsicSize.Max)` 强制三个卡片等高
- [x] `HomeScreen.kt`：AssetCard 内 value 限制 `maxLines = 1`，防止换行

### 17.3 趋势时间切换器重做

- [x] `HomeScreen.kt`：删除 `FilterChip` 组件
- [x] `HomeScreen.kt`：用 Row + 三个 Text 实现文字切换（选中深色+底部指示线，未选中灰色）
- [x] 间距 16dp，干净轻量

### 17.4 持仓页去掉「全部」Tab

- [x] `HoldingsScreen.kt`：tabs 改为 `["存款", "股票", "基金", "黄金"]`
- [x] `HoldingsScreen.kt`：pagerState pageCount 从 5 改为 4
- [x] `HoldingsScreen.kt`：page 映射调整（0=存款, 1=股票, 2=基金, 3=黄金）
- [x] `HomeScreen.kt`：onNavigateToHoldings 的 tab 参数重新映射
- [x] `HoldingsScreen.kt`：去掉 `AllHoldings` composable 和对应的 page case

### 17.5 整体配色调整

- [x] `HoldingsScreen.kt`：所有卡片 `surfaceVariant` → `surface`，阴影 3dp+
- [x] `HomeScreen.kt`：资产卡片白色 + 类型色图标点缀
- [x] 全局减少灰色使用，白色卡片 + 阴影制造层次
- [x] 涨跌色保持红涨绿跌

---

## 阶段 18：数据正确性修复（2026-05-22 第五轮）

> 依赖：阶段 0-17 全部完成
> 执行顺序：18.1 → 18.2 → 18.3 → 18.4 → 18.5 → 18.6 → 18.7
> 详见 PRD 第十六章

### 18.1 BackfillEngine 对接 fetchHistory 获取真实历史价格

**根因**：`getOrFetchPrice()` 调用 `fetchPrice()`，所有 Provider 的 `fetchPrice()` 都忽略 `date` 参数，永远返回实时价。`fetchHistory()` 已实现但从未被调用。

- [x] `BackfillEngine.kt`：`backfillRange()` 预拉取历史数据阶段，对每个持仓调用 `fetchHistory(start, end)` 批量获取并写入 PriceSnapshot 缓存
- [x] `BackfillEngine.kt`：`getOrFetchPrice()` 仅对「今天」尝试网络拉取实时价；历史日期若无缓存则跳过（不污染历史数据）
- [x] `GoldProvider.kt`：`fetchHistory()` 对接东方财富历史金价 K 线 API（secid=118.AU9999）
- [x] `HKStockProvider.kt`：`fetchHistory()` 对接东方财富港股历史 K 线 API（secid=116.xxxxx）
- [x] `AStockProvider` / `CNFundProvider`：`fetchHistory()` 已有正确实现，无需改动

### 18.2 修复测试数据与真实价格不连续

**根因**：测试数据生成随机游走价格，与真实市场价格差异巨大。`fetchLivePrices()` 拉取真实价后，今日总值与昨日 DailySummary（用随机游走价算的）对比 → 虚假巨幅变动（如茅台 -31%）。

- [x] `HomeViewModel.kt`：`computeFromEntities()` 改为用昨日的真实 PriceSnapshot 计算昨日总值，不再依赖 DailySummary 中的历史汇总值
- [x] 昨日股票/基金/黄金总值 = 各持仓 quantity × yesterdayPrice × rate 求和（yesterdayPrice 来自 getByHoldingAndDate 或 getLatestBefore）
- [ ] `HomeViewModel.kt`：`fetchLivePrices()` 增加拉取「昨日收盘价」逻辑（computeFromEntities 的修复已解决核心问题，此项可选）
- [ ] `TestDataGenerator.kt`：生成测试数据后，可选地用 `fetchHistory` 拉取真实历史价格覆盖随机数据
- [ ] 新增数据标记：测试数据标记 `dataSource` 字段（后续迭代）

### 18.3 删除黄金缓存强制清除逻辑

**根因**：`HomeViewModel.clearGoldPriceCache()` 每次启动删除所有黄金快照，导致回填前打开详情页无数据，回填后全部同一实时价。

- [x] `HomeViewModel.kt`：删除 `clearGoldPriceCache()` 方法及其在 `init` 中的调用
- [x] `HomeViewModel.kt`：删除 `cacheCleared` 标志位
- [ ] `DatabaseModule.kt`：添加 migration，一次性清理旧的黄金价格缓存（若需要，后续迭代）

### 18.4 修复 HoldingDetailViewModel 前一日价格查询

**根因**：`getPrevious()` 取第二新快照，若同日期有多条快照则可能取到同一天的数据。应取 `date < today` 的最新快照。

- [x] `PriceSnapshotDao.kt`：新增 `getLatestBefore(holdingId, date, accountId)` 查询方法
- [x] `HoldingDetailViewModel.kt`：`renderWithCache()` 中 `prevPrice` 改用 `getLatestBefore(h.id, today, accountId)` 替代 `getPrevious()`
- [x] `HoldingsViewModel.kt`：`toHoldingDisplay()` 中同样改用 `getLatestBefore` 获取昨日价

### 18.5 趋势图增加百分比标注

**根因**：`TrendChart` Y 轴只显示绝对金额，用户需要看到涨跌百分比。

- [x] `TrendChart.kt`：新增 `showPercentage` 参数，百分比模式下 Y 轴和 Tooltip 显示相对于首日的涨跌百分比
- [x] `TrendChart.kt`：新增 `formatPctLabel()` 函数格式化百分比标签
- [x] `HoldingDetailScreen.kt`：图表卡片标题栏增加 ¥ / % 切换按钮（金色），默认百分比模式

### 18.6 港股历史数据对接

**根因**：`HKStockProvider.fetchHistory()` 返回空列表，港股无趋势图。

- [x] `HKStockProvider.kt`：实现 `fetchHistory()`，对接东方财富港股 K 线 API（secid=116.xxxxx）
- [ ] `HKStockProvider.kt`：实现 `search()` 方法（后续迭代）

### 18.7 端到端验证

- [x] APK 编译通过（`./gradlew assembleDebug` 成功）
- [ ] 生成测试数据 → 打开茅台详情页 → 确认趋势图非平线、今日涨跌合理（<±10%）
- [ ] 打开黄金详情页 → 确认有趋势图、非空白
- [ ] 打开腾讯详情页 → 确认有趋势图
- [ ] 首页日变动幅度合理（不是 -31% 这种极端值）
- [ ] 下拉刷新后数据保持一致
- [ ] **无法自动验证**：无 Android 设备/模拟器连接，需用户手动安装 APK 测试

---

## 阶段 19：资产体系扩展（现金 + 应收 + 负债 + 存款到期）

> 依赖：阶段 0-18 全部完成
> 执行顺序：19.1 → 19.2 → 19.3 → 19.4 → 19.5 → 19.6 → 19.7

### 19.1 数据层新增

- [x] `CashTransaction.kt` — Entity（id, accountId, date, amount, type, note）
- [x] `Receivable.kt` — Entity（id, accountId, name, amount, date, expectedDate, note）
- [x] `Debt.kt` — Entity（id, accountId, name, amount, date, interestRate, note）
- [x] `CashTransactionDao.kt` — CRUD + getByDateRange
- [x] `ReceivableDao.kt` — CRUD + getAll
- [x] `DebtDao.kt` — CRUD + getAll
- [x] `AppDatabase.kt` — 注册新 Entity + DAO，version bump 到 5

### 19.2 现金余额管理

- [x] `CashRepository.kt` — 封装流水 CRUD，提供 `getBalance(): BigDecimal`（求和所有流水）
- [x] `CashViewModel.kt` — 余额展示 + 流水列表 + 入金/出金弹窗
- [x] `CashScreen.kt` — 余额卡片 + 流水列表 + 入金/出金按钮
- [x] 入金/出金弹窗：金额输入 + 备注 → 写入 CashTransaction + 刷新

### 19.3 应收款管理

- [x] `ReceivableRepository.kt` — 封装 CRUD
- [x] `CreditViewModel.kt`（复用，管理应收 + 负债）
- [x] `CreditScreen.kt` — 应收款区域 + 负债区域上下分区
- [x] 新增/编辑/删除应收款弹窗

### 19.4 负债管理

- [x] `DebtRepository.kt` — 封装 CRUD
- [x] 新增/编辑/删除负债弹窗
- [x] 应收净额 = 应收合计 - 负债合计（底部固定显示）

### 19.5 存款到期自动处理

- [x] `BackfillEngine.kt`：每日回填时检测 `maturityDate <= today && status == "active"` 的存款
  - 更新 status → "matured"
  - 自动生成 CashTransaction（本金 + 累计利息，type = DEPOSIT_MATURITY）
  - 更新 status → "settled"
- [x] `DepositDao.kt`：新增 `getMaturedList(accountId)` / `getSettledList(accountId)` / `getInactiveFlow`
- [x] `HoldingsViewModel.kt`：存款 Tab 拆分为两个子 Tab（持有中 / 已到期）
  - 持有中：status=active
  - 已到期：status=matured 或 settled
- [x] 已到期存款卡片：显示本息合计、到期日期、赎回状态

### 19.6 首页重构为净资产体系

- [x] `HomeViewModel.kt`：净资产 = 现金余额 + 存款(active) + 持仓市值 + 应收款 - 负债
- [x] `HomeScreen.kt`：净资产卡片（资产/负债分区，2×3 网格点击跳转）
- [x] `HomeScreen.kt`：今日收益明细卡片（按资产类型着色）
- [x] `HomeUiState.kt`：新增 cashBalance, receivablesTotal, debtsTotal, netWorth 字段
- [x] 净资产各分项点击跳转对应 Tab（现金→CashScreen，存款→存款Tab，持仓→持仓Tab，应收/负债→CreditScreen）

### 19.7 持仓页 Tab 重构

- [x] `HoldingsScreen.kt`：顶层 Tab 改为 `[持仓, 存款, 现金, 信用]`
  - 持仓 Tab 内：子 Tab `[股票, 基金, 黄金]`
  - 存款 Tab 内：子 Tab `[持有中, 已到期]`
  - 现金 Tab：跳转 CashScreen
  - 信用 Tab：跳转 CreditScreen
- [x] `AppNavigation.kt`：新增 Cash、Credit 路由
- [x] `FinancialFreedomApp.kt`：subRoutes 新增 Cash/Credit，底部导航适配

---

## 统计

| 阶段 | 任务数 |
|------|--------|
| 0 - 脚手架 | 4 |
| 1 - 数据层 | 4 |
| 2 - 依赖注入 | 1 |
| 3 - 领域层 | 3 |
| 4 - Repository | 4 |
| 5 - 网络层 | 7 |
| 6 - 补算引擎 | 3 |
| 7 - 导航框架 | 2 |
| 8 - UI 页面 | 6 |
| 9 - 共享组件 | 5 |
| 10 - CSV | 2 |
| 11 - 集成打磨 | 4 |
| 12 - 账号系统 | 8 |
| 13 - 测试数据 | 2 |
| 14 - Bug 修复 | 7 |
| 15 - 实时价格 & 视觉升级 | 6 |
| 16 - 数据一致性 & Bug 修复 | 5 |
| 17 - UI 视觉重设计 | 5 |
| 18 - 数据正确性修复 | 7 |
| 19 - 资产体系扩展 | 7 |
| 20 - 更名 & 日期选择器 & Modified Dietz | 7 |
| 21 - 全 App 连续滑动体系 | 8 |
| 22 - 导航优化 & 首页视觉升级 | 5 |
| 23 - 统一CSV & 名称回填 & 现金扣款 | 6 |
| **合计** | **125** |

---

## 阶段 20：更名 & 日期选择器 & Modified Dietz（2026-05-22）

> 依赖：阶段 0-19 全部完成
> 执行顺序：20.1 → 20.2 → 20.3 → 20.4 → 20.5 → 20.6 → 20.7
> 详见 PRD 第十七～十九章、UI_DESIGN.md 6.6

### 20.1 应用更名

- [ ] `strings.xml`：`app_name` 从「财富自由」改为「扶摇阁」
- [ ] `PRD.md` / `TASKS.md` / `UI_DESIGN.md` / `README.md` / `CLAUDE.md`：项目名称统一改为扶摇阁

### 20.2 EditDepositScreen 日期选择器

- [ ] `EditDepositScreen.kt`：存入日期 + 到期日期接入 DatePickerDialog（参照 AddDepositScreen 实现）

### 20.3 EditHoldingScreen 日期选择器

- [ ] `EditHoldingScreen.kt`：买入日期接入 DatePickerDialog（参照 AddHoldingScreen 实现）

### 20.4 CreditScreen.ReceivableDialog 日期选择器

- [ ] `CreditScreen.kt`：ReceivableDialog 预计归还日接入 DatePickerDialog

### 20.5 数据模型：DailySummary 新增 netInflow

- [ ] `DailySummary.kt`：新增 `netInflow: BigDecimal` 字段
- [ ] `AppDatabase.kt`：version bump 到 6
- [ ] `DailyBreakdownItem.kt`：新增 `contribution: BigDecimal` 字段（当日入金本金）

### 20.6 BackfillEngine / HomeViewModel 剔除入金计算

- [ ] `BackfillEngine.kt`：`backfillDay()` 中 dayChange = todayTotal - yesterdayTotal - netInflow
- [ ] `HomeViewModel.kt`：`computeFromEntities()` 同步修正
- [ ] `HomeUiState`：新增 `cumulativeContributions`、`cumulativeReturn`、`cumulativeReturnPct` 字段

### 20.7 HomeScreen 累计收益展示

- [ ] `HomeScreen.kt`：今日收益卡片底部加累计投入/收益/收益率行（参照 UI_DESIGN.md 6.4）

---

## 阶段 21：全 App 连续滑动体系（v3 核心架构）

> 依赖：阶段 0-19 全部完成（20 可并行）
> 执行顺序：21.1 → 21.2 → 21.3 → 21.4 → 21.5 → 21.6 → 21.7 → 21.8
> 详见 UI_DESIGN.md 第六、七、十二章

### 21.1 全局 Pager 骨架

- [x] 新建 `MainPagerScreen.kt`：10 页 `HorizontalPager`，`userScrollEnabled = true`
- [x] 定义页面枚举 `PagerPage`（HOME=0, STOCK=1, FUND=2, GOLD=3, DEPOSIT_ACTIVE=4, DEPOSIT_MATURED=5, CASH=6, CREDIT=7, EARNINGS=8, SETTINGS=9）
- [x] `BottomNavBar` 改为接收 `currentPage: Int`，点击触发 `animateScrollToPage(anchor)`
- [x] 底部导航高亮映射：page 0→首页, 1-7→持仓, 8→收益, 9→设置
- [x] `AppNavigation.kt`：详情页（HoldingDetail、AddHolding 等）保留 NavHost overlay，主页面切换改为 Pager 滑动

### 21.2 Section 指示器组件

- [x] 新建 `SectionIndicator` (集成在 MainPagerScreen.kt)
- [x] 参数：`categoryName: String`、`subItems: List<String>`、`currentIndex: Int`
- [x] 渲染：分类标签 12sp + 子项名称列表 15sp + 圆点指示器（8dp 金色/灰色）
- [x] 仅在投资段（page 1-3）和存款段（page 4-5）显示
- [x] 其他 page 不显示 SectionIndicator

### 21.3 首页嵌入 Pager

- [x] `HomeScreen` 嵌入 Pager page 0
- [x] 资产卡片点击改为 `scrollToPage(n)`（投资→1, 存款→4, 现金→6, 信用→7）
- [x] 总资产 + 今日收益 + 4 资产卡片（v6 设计）

### 21.4 投资段（股票/基金/黄金）重构

- [x] 将 `HoldingsScreen` 的股票/基金/黄金三页拆入 Pager pages 1-3
- [x] 每页：SectionIndicator + LazyColumn + FAB
- [x] 共享 `HoldingsViewModel`，数据按 page 位置过滤
- [x] FAB 点击 → NavHost overlay `AddHoldingScreen(type)`

### 21.5 存款段（持有中/已到期）重构

- [x] 将存款持有中/已到期拆入 Pager pages 4-5
- [x] 共享 `HoldingsViewModel` 存款数据
- [x] FAB 点击 → NavHost overlay `AddDepositScreen`

### 21.6 现金 + 信用嵌入 Pager

- [x] `CashScreen` 嵌入 Pager page 6（不再独立 navigate）
- [x] `CreditScreen` 嵌入 Pager page 7（不再独立 navigate）
- [x] 添加/编辑操作仍走 NavHost overlay

### 21.7 收益 + 设置嵌入 Pager

- [x] `EarningsScreen` 嵌入 Pager page 8
- [x] `SettingsScreen` 嵌入 Pager page 9

### 21.8 收尾动画与边界

- [x] Pager 页面切换动画（`animateScrollToPage` 流畅过渡）
- [x] 边界处理：page 0 不可左滑，page 9 不可右滑（`beyondViewportPageCount = 0`）
- [ ] `prefers-reduced-motion` 检查（开启时 duration=0）→ 非关键，后续补充
- [ ] 全链路滑动测试：首页→股票→基金→黄金→持有中→已到期→现金→信用→收益→设置

### 滑动链路验证清单

```
[x] 首页右滑 → 投资·股票
[x] 投资·股票右滑 → 投资·基金
[x] 投资·基金右滑 → 投资·黄金
[x] 投资·黄金右滑 → 存款·持有中
[x] 存款·持有中右滑 → 存款·已到期
[x] 存款·已到期右滑 → 现金
[x] 现金右滑 → 信用
[x] 信用右滑 → 收益
[x] 收益右滑 → 设置
[ ] 设置不能右滑（边界）
[ ] 首页不能左滑（边界）
[ ] 底部导航点击「持仓」→ 跳到投资·股票(page 1)
[ ] 底部导航点击「收益」→ 跳到收益(page 8)
[ ] 首页资产网格点「现金」→ 跳到现金(page 6)
```

---

## 阶段 22：导航优化 & 首页视觉升级（2026-05-22）

> 依赖：阶段 0-21 全部完成
> 执行顺序：22.1 → 22.2 → 22.3 → 22.4 → 22.5
> 详见 PRD 第二十章、UI_DESIGN.md 6.3-6.5

### 22.1 Category 快速跳转导航条

- [ ] `MainPagerScreen.kt`：新增 `CategoryNavStrip` 组件
  - 4 个分类 chip：投资 / 存款 / 现金 / 信用
  - 当前分类金色文字 + 2dp 底部指示线，其他灰色
  - 仅在 page 1-7 显示
  - 映射：投资→page 1, 存款→page 4, 现金→page 6, 信用→page 7
  - 接受 `onCategoryClick: (Int) -> Unit` 回调，调用 `animateScrollToPage`

### 22.2 SectionIndicator 可点击

- [ ] `MainPagerScreen.kt`：`SectionIndicator` 子项名称改为可点击
  - 股票/基金/黄金 → 点击跳转到对应 page (1/2/3)
  - 持有中/已到期 → 点击跳转到对应 page (4/5)
  - 当前项保持金色高亮

### 22.3 首页资产配置占比条

- [ ] `HomeScreen.kt`：新增 `AllocationBar` 组件
  - 堆叠水平条：各段宽度 ∝ 该类资产占总资产比例
  - 段颜色：投资=gold, 存款=deposit, 现金=cash, 应收=receivable
  - 下方标签：类别名称 + 百分比
  - 最小段宽 4dp，无数据类别不显示
  - 容器白色 ElevatedCard，16dp 圆角

### 22.4 日期选择器补全

- [ ] `CreditScreen.kt`：`DebtDialog` 新增日期字段 + DatePickerDialog
- [ ] `AddHoldingScreen.kt`：替换 emoji 📅 为 Material Icon `DateRange`
- [ ] `AddHoldingScreen.kt`：统一 clickable 模式（直接放 modifier 上，与 Edit 页面一致）

### 22.5 编译验证

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 无编译错误和警告

---

## 阶段 23：统一CSV & 名称回填 & 现金扣款（2026-05-22）

> 依赖：阶段 0-22 全部完成
> 执行顺序：23.1 → 23.2 → 23.3 → 23.4 → 23.5 → 23.6
> 详见 PRD 第二十二章

### 23.1 统一 CSV 导出

- [ ] `CsvExporter.kt`：新增 `exportAll(accountId, uri)` 方法
  - 查询所有 Deposit + Holding，写入单一 `assets.csv`
  - 列：type,name,bank,symbol,market,currency,principal,quantity,cost_price,interest_rate,start_date,end_date,note
  - 存款填充：name,bank,currency,principal,interest_rate,start_date,end_date
  - 股票/基金填充：symbol,market,currency,quantity,cost_price,start_date（name 留空）
  - 黄金填充：symbol=XAU,currency,quantity,cost_price,start_date（name 留空）
- [ ] 删除旧方法：`exportDeposits()`, `exportHoldings()`, `exportTransactions()`
- [ ] `SettingsViewModel.kt`：合并为 `exportAssets(uri)` 单方法
- [ ] `SettingsScreen.kt`：导出入口合并为单按钮"导出资产 → assets.csv"

### 23.2 统一 CSV 导入

- [ ] `CsvImporter.kt`：新增 `importAll(uri, accountId)` 方法
  - 按 `type` 列分发：DEPOSIT→插入 Deposit，STOCK/FUND/GOLD→插入 Holding
  - GOLD 自动设置 name="黄金", symbol="XAU"
  - STOCK/FUND name 留空（后续 API 回填）
- [ ] 删除旧方法：`importDeposits()`, `importHoldings()`
- [ ] `SettingsViewModel.kt`：合并为 `importAssets(uri)` 单方法
- [ ] `SettingsScreen.kt`：导入预览对话框适配新格式

### 23.3 名称自动回填

- [ ] `HoldingDao.kt`：新增 `updateName(id, name)` 方法
- [ ] `HomeViewModel.kt`：`fetchLivePrices()` 拉取价格后，检查 `holding.name` 为空则用 API 返回名称填充
- [ ] `BackfillEngine.kt`：拉取历史价格时同样回填空名称

### 23.4 AddDepositScreen 现金扣除

- [ ] `AddDepositScreen.kt`：备注后、保存前增加 Switch "从现金中扣除"
- [ ] `AddDepositViewModel.kt`：注入 `CashTransactionDao`
  - `save()` 新增 `deductFromCash: Boolean` 参数
  - true 时创建 CashTransaction(type=ASSET_PURCHASE, amount=-principal, date=startDate)

### 23.5 AddHoldingScreen 现金扣除

- [ ] `AddHoldingScreen.kt`：备注后、保存前增加 Switch "从现金中扣除"
- [ ] `AddHoldingViewModel.kt`：注入 `CashTransactionDao`
  - `save()` 新增 `deductFromCash: Boolean` 参数
  - true 时扣除金额=quantity×cost_price（黄金直接=cost_price）
  - 创建 CashTransaction(type=ASSET_PURCHASE, amount=负数)
- [ ] `CashTransaction.kt` 实体 type 字段文档更新（增加 ASSET_PURCHASE 类型）

### 23.6 编译验证

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 无编译错误和警告

---

## 阶段 24：自动搜索 & 港股搜索 & 黄金简化 & 多笔交易（2026-05-22）

> 依赖：阶段 0-23 全部完成
> 执行顺序：24.1 → 24.2 → 24.3 → 24.4 → 24.5 → 24.6 → 24.7 → 24.8
> 详见 PRD 第二十三章、UI_DESIGN.md 第十三章

### 24.1 存款去名称

- [x] `AddDepositScreen.kt`：删除"存款名称"输入框，保存时 `name = bank`
- [x] `EditDepositScreen.kt`：同步删除名称字段，编辑时不再显示
- [x] 存款卡片：标题改为只显示银行名（原先显示名称 + 银行）
- [x] `Deposit.kt` 实体不修改（保留 `name` 列，值自动等于 `bank`）

### 24.2 股票/基金代码输入自动搜索

- [x] `AddHoldingScreen.kt`：代码输入框改造
  - 删除搜索图标按钮
  - 输入文本变化 → 300ms 去抖 → 自动调用 `viewModel.search(query)`
  - 搜索中显示 `CircularProgressIndicator`（16dp，在输入框右侧）
  - 搜索结果以下拉列表形式覆盖在输入框下方（`DropdownMenu` 或自定义 `Popup`）
  - 最多显示 8 条结果，超出可滚动
  - 选中某条 → 自动填入 symbol、name、market → 下拉消失
  - 点击输入框外区域或清空输入 → 下拉消失
  - 无结果时显示"未找到匹配结果"
- [x] `AddHoldingViewModel.kt`：新增 `search()` 方法，调用 `PriceService.searchAll()`
  - 暴露 `searchResults: StateFlow<List<SearchResult>>` + `isSearching: StateFlow<Boolean>`
- [x] 搜索仅对股票和基金类型触发，黄金类型不搜索

### 24.3 港股搜索实现

- [x] `HKStockProvider.kt`：实现 `search()` 方法
  - 对接东方财富港股搜索 API
  - 返回 `SearchResult(symbol, name, market="HK", type="STOCK")`
- [x] `PriceService.kt`：`searchAll()` 中加入 `hkStockProvider.search(query)` 调用

### 24.4 黄金表单简化

- [x] `AddHoldingScreen.kt`：黄金表单改造
  - 删除代码、名称、市场字段
  - 保留：克数、单价（元/克）、购买日期
  - 买入总价实时预览
  - 保存时：`symbol = "XAU"`, `name = "黄金"`, `market = ""`, `costPrice = 克数 × 单价`
- [x] `EditHoldingScreen.kt`：黄金编辑同步改造（从 costPrice 反推单价展示）

### 24.5 Holding 实体新增 status 字段

- [x] `Holding.kt`：新增 `val status: String = "active"`（active / closed）
- [x] `AppDatabase.kt`：version bump 到 7，添加 MIGRATION_6_7
- [x] DAO 查询：列表查询默认过滤 `status = "active"`
- [x] `DatabaseModule.kt`：注册 MIGRATION_6_7

### 24.6 持仓详情页加仓/减仓按钮

- [x] `HoldingDetailScreen.kt`：交易记录上方新增 `[+ 加仓]` `[- 减仓]` 按钮
- [x] 仅非黄金类型且 status=active 时显示
- [x] 已清仓显示"已清仓"标签

### 24.7 加仓/减仓弹窗

- [x] `PositionDialogs.kt`：`AddPositionDialog` 组件
  - 当前持仓信息 + 买入数量/价格/日期输入
  - 实时预览：加仓后股数、加权成本均价、总成本
  - 可选 Switch "从现金中扣除"
  - 输入校验：数量 > 0
- [x] `PositionDialogs.kt`：`ReducePositionDialog` 组件
  - 当前持仓信息 + 卖出数量/价格/日期输入
  - 实时预览：剩余股数、实现盈亏
  - 可选 Switch "收入计入现金"
  - 全部卖出时二次确认弹窗
- [x] `HoldingDetailViewModel.kt`：新增 `addPosition()` / `reducePosition()` 方法
  - 注入 `TransactionDao`、`CashTransactionDao`
  - 加权平均成本计算 + 实现盈亏计算 + 现金流水

### 24.8 编译验证

- [x] `./gradlew assembleDebug` 编译通过
- [x] 无编译错误和警告

---

## 阶段 25：持仓分组重设计（v17 — 2026-05-23）

> 依赖：阶段 0-24 全部完成
> 执行顺序：25.1 → 25.2 → 25.3 → 25.4 → 25.5 → 25.6 → 25.7
> 详见 PRD 第二十五章、UI_DESIGN.md 第十四章

### 25.1 数据层：Display 数据类 + 分组逻辑

- [ ] `HoldingsViewModel.kt`：新增 Display 数据类
  - `BankGroupDisplay`（bank, depositCount, totalPrincipal, totalCurrentValue, todayTotalInterest, weightedProgress, nearestMaturity, currency）
  - `HoldingGroupDisplay`（symbol, name, market, type, totalQuantity, avgCost, currentPrice, totalPnL, totalPnLPct, todayChange, isUp, marketValue, buyRecords: List<BuyRecordDisplay>）
  - `BuyRecordDisplay`（transactionId, date, type, quantity, price, cost, currentValue, pnl, pnlPct, isUp）
- [ ] `HoldingsViewModel.kt`：新增分组方法
  - `toBankGroupList(deposits, today, accountId)` → 按 bank 分组，聚合统计
  - `toHoldingGroupList(holdings, today, accountId)` → 按 symbol 分组，聚合 + 拉取 Transaction
- [ ] `HoldingsUiState`：新增字段
  - `bankGroups: List<BankGroupDisplay>`
  - `maturedBankGroups: List<BankGroupDisplay>`
  - `stockGroups: List<HoldingGroupDisplay>`
  - `fundGroups: List<HoldingGroupDisplay>`
  - 保留原有 flat lists 供 SectionIndicator 汇总用

### 25.2 首页金额完整展示（去掉"万"）

- [ ] `HomeScreen.kt`：重写 `formatMoneyShort()` → 输出完整数字（带千位分隔符，如 ¥420,000）
- [ ] `HomeScreen.kt`：重写 `formatNetWorthShort()` → 同上
- [ ] `HomeScreen.kt`：更新 `formatAllocationValue()` 添加 `¥` 前缀
- [ ] 验证：首页 2×2 网格卡片全部显示完整数字

### 25.3 UI：CategoryNavStrip + SectionIndicator 汇总行

- [ ] `MainPagerScreen.kt`：注入 `HoldingsViewModel` 获取汇总数据
- [ ] `MainPagerScreen.kt`：`CategoryNavStrip` 新增汇总行
  - chip 行下方显示「当前分类总计 + 今日」
  - 随 currentPage 切换：投资总计 / 存款总计 / 现金余额 / 应收净额
- [ ] `MainPagerScreen.kt`：`SectionIndicator` 新增子类汇总行
  - 圆点下方显示「子类总市值 + 今日涨跌」
  - 随 currentPage 切换：股票市值 / 基金市值 / 黄金市值 / 持有中估值 / 已到期本息

### 25.4 UI：存款分组（银行组卡片 + 银行详情页）

- [ ] `HoldingsPages.kt`：新增 `BankGroupCard` composable
  - 银行名 + 存单数、本金合计、当前估值、今日利息
  - 加权进度条、最近到期日、右侧箭头
  - 左侧 4dp 蓝色条
- [ ] `HoldingsPages.kt`：`ActiveDepositsPage` 改为使用 `BankGroupCard`（数据源 `state.bankGroups`）
- [ ] `HoldingsPages.kt`：`MaturedDepositsPage` 改为使用 `BankGroupCard`（数据源 `state.maturedBankGroups`，灰色调）
- [ ] 新建 `BankDepositsScreen.kt` + `BankDepositsViewModel.kt`
  - 顶部汇总卡片 + 存单列表
  - 每张存单显示完整信息 + 编辑/删除按钮
  - 顶部栏 [+] 快速添加存单到该银行
- [ ] `Route.kt`：新增 `BankDeposits(bankName: String, status: String)` 路由
- [ ] `AppNavigation.kt`：注册路由 + 导航逻辑（`onDepositClick` → `BankDeposits` 替代 `EditDeposit`）

### 25.5 UI：股票/基金分组（标的组卡片）

- [ ] `HoldingsPages.kt`：新增 `HoldingGroupCard` composable
  - 折叠态：名称、代码、市场标签、当前价、今日涨跌、总持仓信息、盈亏
  - 「▶ N 笔买入记录 [展开]」按钮
  - 展开态：买入记录列表（每笔：日期、数量、价格、成本、当前市值、盈亏）
  - [+ 加仓] [- 减仓] 按钮（展开态底部）
  - 左侧 4dp 类型色条
- [ ] `HoldingsPages.kt`：`StockPage` 改为使用 `HoldingGroupCard`（数据源 `state.stockGroups`）
- [ ] `HoldingsPages.kt`：`FundPage` 改为使用 `HoldingGroupCard`（数据源 `state.fundGroups`，紫色条）
- [ ] `GoldPage` 保持不变

### 25.6 导航 & 数据流调整

- [ ] `MainPagerScreen.kt`：`onDepositClick` 改为传递 bank + status 而非 depositId
- [ ] `AppNavigation.kt`：适配新的存款点击回调（bank → BankDepositsScreen）
- [ ] `HoldingsViewModel.kt`：确保原有 flat lists 仍可正常获取（用于 SectionIndicator 汇总）
- [ ] 边界处理：空银行组、空标的组、无买入记录时的占位提示

### 25.7 编译验证 & 收尾

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 无编译错误和警告
- [ ] 数据流验证：银行分组正确、股票分组正确、汇总数据准确

---

## 阶段 26：一键重算收益 + 显示倍率

### 26.1 基础设施：DisplaySettings

- [ ] 新建 `com.financial.freedom.domain.settings.DisplaySettings` 单例
  - 读写 SharedPreferences（key `display_multiplier`），默认 1.0
  - 暴露 `StateFlow<BigDecimal>` 供各 ViewModel 收集
  - 方法 `setMultiplier(value: BigDecimal)` + `getMultiplier(): BigDecimal`
- [ ] Hilt 模块注册 `DisplaySettings` 为 `@Singleton`

### 26.2 集中化金额格式化

- [ ] 新建 `com.financial.freedom.ui.common.FormatUtils.kt`
  - `formatMoney(value: BigDecimal, multiplier: BigDecimal = BigDecimal.ONE): String`
  - 逻辑：`value * multiplier` → 千分位 + 2 位小数
  - 配套 `formatMoneyShort`（0 位小数）、`formatSignedChange`、`parseMoneyValue`
- [ ] 重构 `HomeViewModel.formatMoney` 引用为 `FormatUtils.formatMoney`
- [ ] 重构 `HomeScreen.formatMoneyShort` 引用为 `FormatUtils.formatMoneyShort`
- [ ] 重构 `HoldingsViewModel.formatMoney` 引用为 `FormatUtils.formatMoney`
- [ ] 重构 `EarningsScreen.formatMoney` 引用为 `FormatUtils.formatMoney`

### 26.3 一键重算收益 — ViewModel

- [ ] `SettingsViewModel` 新增状态：
  - `showRecalcConfirm: Boolean`
  - `recalcDone: Boolean`
- [ ] `SettingsViewModel` 新增方法：
  - `showRecalcConfirm()`
  - `dismissRecalcConfirm()`
  - `recalculateReturns()` — 调用 `BackfillEngine.markDirtyAndBackfill(fromDate, accountId)`
- [ ] 注入 `BackfillEngine`（如尚未注入）
- [ ] 查找最早资产日期（从 Deposit + Holding + CashTransaction 等取 min date）

### 26.4 一键重算收益 — UI

- [ ] `SettingsScreen` 新增「一键重算收益」卡片项
  - 位于「生成测试数据」和「清空数据」之间
  - 标题：「一键重算收益」，副标题：「删除所有历史收益汇总并重新计算」
- [ ] `SettingsScreen` 新增确认弹窗（AlertDialog）
  - 标题「确认重算收益」，警告说明，取消/确认按钮
  - 确认后调用 `viewModel.recalculateReturns()` + Toast 提示

### 26.5 显示倍率 — ViewModel

- [ ] `SettingsViewModel` 注入 `DisplaySettings`
- [ ] `SettingsUiState` 新增字段 `displayMultiplier: BigDecimal = BigDecimal.ONE`
- [ ] `SettingsViewModel` 新增方法 `setDisplayMultiplier(value: BigDecimal)`
- [ ] `SettingsViewModel.init` 收集 `DisplaySettings.multiplierFlow`

### 26.6 显示倍率 — UI

- [ ] `SettingsScreen` 新增「显示倍率」卡片
  - 位于「汇率基准」和「清空数据」之间
  - 标题「显示倍率」，副标题说明文字
  - 三个 `FilterChip` 或分段按钮：10%、50%、100%
  - 当前选中项高亮（primary container 色）
  - 点击后立即调用 `viewModel.setDisplayMultiplier()`

### 26.7 各 ViewModel 应用倍率

- [ ] `HomeViewModel` 收集 `DisplaySettings.multiplierFlow`，传入 `formatMoney`
- [ ] `HoldingsViewModel` 收集 `DisplaySettings.multiplierFlow`，传入 `formatMoney`
- [ ] `EarningsViewModel` 收集 `DisplaySettings.multiplierFlow`，传入 `formatMoney`

### 26.8 编译验证 & 收尾

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 无编译错误和警告
- [ ] 功能验证：重算收益后每日汇总数据正确
- [ ] 功能验证：切换倍率后各页面金额变化正确
- [ ] 功能验证：倍率设置为 50% 后重启 App 仍然生效


## v21 资产增涨爽感系统（2026-05-24）

| # | 任务 | 复杂度 | 状态 |
|---|------|--------|------|
| 1 | 数字格式化：总资产整数，其他保留小数 | 低 | ⏳ |
| 2 | 周视图：未来周视觉弱化处理 | 低 | ⏳ |
| 3 | 首页总资产 count-up 数字滚动动画 | 低 | ⏳ |
| 4 | 正收益金色脉冲光晕 | 中 | ⏳ |
| 5 | 连续增长标识（Streak Badge） | 中 | ⏳ |
| 6 | 环比数据行（较上月变化） | 中 | ⏳ |
| 7 | 存款利息呼吸动画 | 低 | ⏳ |
| 8 | 存入动作正反馈（Toast 展示新总资产） | 中 | ⏳ |
| 9 | 空状态情感化引导页 | 低 | ⏳ |

### 设计文档
- UI_DESIGN.md Section 18：资产增涨爽感系统完整设计稿
- UI_DESIGN.md Section 6.5：数字格式化规则
- UI_DESIGN.md Section 8.2：周视图未来周处理
- PRD.md Section 二十五：资产增涨爽感系统需求描述

## 阶段 27：现金/信用页面 v22 重设计（2026-05-24）

> 依赖：阶段 0-26 全部完成
> 设计文档：UI_DESIGN.md 7.6、7.7、7.10、7.11 | PRD.md 现金 Tab、信用 Tab

### 27.1 数据层 — Entity 变更

- [ ] `Receivable` 移除 `expectedDate` 字段，新增 `status: String = "未还"`
- [ ] `Debt` 移除 `interestRate` 字段，新增 `status: String = "未还"`
- [ ] `CashTransaction` 新增 `type` 值：`LEND`（借钱出金）、`REPAY`（还款入金）
- [ ] `CashTransaction` 新增 `relatedId: Long?` 字段（关联 Receivable/Debt id）
- [ ] Room 数据库版本号 +1，编写 Migration（ALTER TABLE 删旧列加新列）
- [ ] DAO 更新：ReceivableDao / DebtDao 新增 `updateStatus(id, status)` 方法
- [ ] DAO 更新：CashTransactionDao 查询支持新 type

### 27.2 现金页面 — UI 重设计

- [ ] `CashScreen.kt` 余额大号居中（32sp Serif Bold），移除旧 ElevatedCard 包裹
- [ ] 入金/出金按钮紧凑排列在余额下方
- [ ] 流水区域默认折叠为「最近流水 ▼」，展开显示最近 5 条
- [ ] 展开后底部「查看全部流水 →」入口
- [ ] 每条流水卡片：日期和类型同行（`·` 分隔），备注在下行
- [ ] `CashDialog` 日期默认今天，保留金额 + 备注字段

### 27.3 信用页面 — UI 重设计

- [ ] `CreditScreen.kt` 应收/应付双折叠区（独立展开收起）
- [ ] 折叠头部：左边线色条 + 标题 + 总金额 + 笔数·状态
- [ ] 展开后每张卡片：姓名 + 金额 + 日期·状态同行 + 编辑按钮
- [ ] 净资产行底部常驻：正数绿色「净应收」、负数红色「净负债」
- [ ] 移除所有 `expectedDate`、`interestRate` 相关 UI
- [ ] `ReceivableDialog` 重设计：姓名、金额、日期、☑从现金账户扣除
- [ ] `DebtDialog` 重设计：来源、金额、日期、☑入现金账户

### 27.4 信用页面 — 交互逻辑

- [ ] 卡片点击弹出选项菜单：编辑 / 已归还(已还) / 删除
- [ ] 「已归还」标记应收为"已还" + 自动生成 REPAY 入金流水
- [ ] 「已还」标记应付为"已还" + 自动生成 REPAY 出金流水
- [ ] 新增应收勾选「从现金扣除」→ 自动生成 LEND 出金流水
- [ ] 新增应付勾选「入现金账户」→ 自动生成 LEND 入金流水

### 27.5 ViewModel 变更

- [ ] `CashViewModel` 支持 LEND/REPAY 类型流水展示和生成
- [ ] `CreditViewModel` 新增 `markReceivableRepaid(id)` / `markDebtPaid(id)` 方法
- [ ] `CreditViewModel` 更新 `addReceivable` / `addDebt` 签名（移除旧参数，新增 deductFromCash 等）
- [ ] `CreditViewModel` 应收/应付合计只统计"未还"状态的条目

### 27.6 编译验证 & 收尾

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 无编译错误和警告
- [ ] 安装到设备验证：现金余额英雄区 + 折叠流水
- [ ] 安装到设备验证：信用双折叠 + 净资产 + 已归还入金
- [ ] 安装到设备验证：新增应收勾选从现金扣除 → 现金减少
- [ ] 提交 GitHub

## 阶段 27b：信用页面术语简化（2026-05-24）

> 依赖：阶段 27 全部完成
> 设计文档：UI_DESIGN.md 7.7、7.11 | PRD.md 信用 Tab
> 目标：应收/应付 → 借出/负债，大白话命名，核心操作可见化

### 27b.1 UI 术语全面更新

- [ ] `CreditScreen.kt`：SectionHeader 标题 应收→借出、应付→负债
- [ ] `CreditScreen.kt`：卡片操作按钮 [编辑]→[对方已还款]/[我已还清] + ··· 菜单
- [ ] `CreditScreen.kt`：ReceivableDialog 标题「新增应收款」→「新增借出」，字段「对方姓名」→「借款人」
- [ ] `CreditScreen.kt`：DebtDialog 标题「新增应付」→「新增负债」，字段「来源（银行/个人）」→「债权人」
- [ ] `CreditScreen.kt`：复选框文本更新：从现金账户扣除→现金同步扣减，入现金账户→现金同步到账
- [ ] `CreditScreen.kt`：底部净头寸行：净应收→净借出，净负债不变
- [ ] `CreditScreen.kt`：空状态提示文本更新

### 27b.2 编译验证

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装到设备验证：新术语显示正确
- [ ] 提交 GitHub

## 阶段 28：黄金 v23 重设计 — 单一资产多次买入（2026-05-24）

> 依赖：阶段 0-27 全部完成
> 设计文档：UI_DESIGN.md 7.3 | PRD.md 黄金卡片、新增黄金买入

### 28.1 数据层 — 加仓模式

- [ ] 黄金不再每个买入创建 Holding，改为 1 个 Holding（汇总） + N 个 Transaction
- [ ] `GoldViewModel`（新建）：管理黄金汇总状态、买入列表、新增/编辑/删除买入
- [ ] 新增买入时：创建 Transaction(type=BUY) + 更新 Holding(quantity += 克数, costPrice = 加权均价)
- [ ] 编辑买入时：更新 Transaction + 重算 Holding
- [ ] 删除买入时：删除 Transaction + 重算 Holding（若最后一笔则删除 Holding）
- [ ] 勾选从现金扣除 → 自动生成 CashTransaction

### 28.2 黄金主页 — 聚合卡片 UI

- [ ] 替换现有 GoldPage 为单手聚合卡片布局
- [ ] 卡片显示：实时金价、涨跌、持有克数、均价、成本、市值、盈亏
- [ ] 买入记录默认折叠，展开显示每笔：日期 + 单价 + 克数 + 编辑按钮
- [ ] [+ 买入黄金] 按钮
- [ ] 卡片内嵌迷你走势图（30天），标注买入点

### 28.3 新增/编辑买入弹窗

- [ ] 日期（DatePickerDialog）+ 单价 + 克数
- [ ] 自动计算买入总价 = 单价 × 克数
- [ ] ☐ 从现金账户扣除
- [ ] 编辑模式：修改日期/单价/克数，或删除该笔买入

### 28.4 走势图详情页 — 买入点标注

- [ ] 价格-时间折线图（Y轴=价格，X轴=时间）
- [ ] 买入点标注：醒目圆点标记（金色 #B7930A），位置 = 买入日期 × 买入单价
- [ ] 点击买入标记 → tooltip：日期、单价、克数、总价、vs 当前盈亏
- [ ] 30天/90天/1年范围切换
- [ ] 下方买入明细列表 + 底部汇总

### 28.5 编译验证 & 收尾

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装到设备验证：黄金聚合卡片 + 买入记录折叠
- [ ] 安装到设备验证：新增买入 + 自动重算均价
- [ ] 安装到设备验证：走势图买入点标注 + tooltip
- [ ] 提交 GitHub

## 阶段 29：黄金 v24 两层重设计（2026-05-24）

> 依赖：阶段 0-28 全部完成
> 设计文档：UI_DESIGN.md 7.3 | PRD.md 黄金卡片
> 设计原则：外层紧凑概览卡片（像股票）+ 点击进入独立详情页 + 支持减仓和删除全部持仓

### 29.1 GoldPage 精简 — 紧凑概览卡片

- [ ] `GoldScreen.kt`：替换内联详情为紧凑概览卡片
  - Au 徽章 + 名称 + 克数 + 市值（大字）
  - 今日涨跌 + 持仓盈亏（并排，涨跌色）
  - 成本均价 + 总成本
  - 迷你走势图（40dp 高）+ "点击查看 >" 入口
- [ ] 移除内联的完整走势图、买入记录展开列表、编辑/删除操作
- [ ] 卡片点击 → `onCardClick` → 导航至 `GoldDetailScreen`
- [ ] FAB 保留，点击弹出买入对话框（同 v23）
- [ ] 空状态保留（无持仓时居中引导）

### 29.2 GoldDetailScreen — 独立详情页

- [ ] 新建 `GoldDetailScreen.kt` + `GoldDetailViewModel.kt`
- [ ] TopAppBar：返回箭头 + "黄金 (XAU)" + 右侧删除图标
- [ ] 市值 Hero：大号市值居中 + 今日涨跌
- [ ] 走势图（30 天）+ 买入点标注 + 1月/3月/1年切换
- [ ] 盈亏双卡（持仓盈亏 + 今日涨跌）
- [ ] 持仓信息三列（持仓量 / 成本均价 / 总成本）
- [ ] 买入记录列表（每笔：序号 + 日期 + 单价 × 克数 = 总价 + 编辑/删除菜单）
- [ ] GoldDetailViewModel 复用 GoldViewModel 的数据逻辑（fetch、render、CRUD）

### 29.3 加仓/减仓按钮

- [ ] 详情页添加 [+ 加仓] [− 减仓] 按钮（黄金不再限制加减仓）
- [ ] 加仓：复用现有 GoldPurchaseDialog 逻辑
- [ ] 减仓：新增 GoldReduceDialog
  - 卖出克数 + 卖出单价 + 卖出日期
  - 实时预览：剩余克数、实现盈亏
  - ☐ 收入计入现金
  - 减仓克数 > 持仓 → 报错
  - 减仓克数 = 持仓 → 全部清仓二次确认
- [ ] GoldDetailViewModel 新增 `reducePosition()` 方法
  - 创建 Transaction(type=SELL)
  - quantity -= 卖出克数
  - 若 quantity = 0 → status = "closed"

### 29.4 删除全部持仓

- [ ] 详情页右上角删除图标 + 底部红色"删除全部黄金持仓"按钮
- [ ] 二次确认弹窗
- [ ] 确认后：删除 Holding + 所有 Transaction + 所有 PriceSnapshot
- [ ] 删除后自动返回 GoldPage（显示空状态）

### 29.5 导航 & 路由

- [ ] `Route.kt`：新增 `GoldDetail` 路由
- [ ] `AppNavigation.kt`：注册 `GoldDetail` → `GoldDetailScreen`
- [ ] `GoldPage` 的 `onHoldingClick` 回调改为 navigate 到 `GoldDetail`
- [ ] 确保返回栈正确（GoldDetail → back → GoldPage）

### 29.6 编译验证 & 收尾

- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装到设备验证：GoldPage 紧凑卡片 + 点击进入详情
- [ ] 安装到设备验证：详情页走势图 + 买入记录 + 加减仓
- [ ] 安装到设备验证：减仓弹窗 + 删除全部持仓
- [ ] 安装到设备验证：空状态 → 添加 → 卡片出现
- [ ] 提交 GitHub

---

## 阶段 30：黄金走势图坐标轴标注（2026-05-24）

> 黄金走势图目前只画曲线和标记点，缺少 Y 轴价格标签和 X 轴日期，用户无法读取具体价格。

### 30.1 GoldDetailChart 坐标轴
- [ ] 重构为 Row（Y轴标签 Column + Canvas）
- [ ] Y 轴：3-4 个价格标签（元/克），格式 `¥1,234`
- [ ] 横向网格线（半透明）
- [ ] X 轴：底部日期标签（智能采样，避免重叠）

### 30.2 GoldChartWithBuyMarkers 坐标轴
- [ ] 同样添加 Y 轴价格标签 + 网格线 + X 轴日期

### 30.3 编译验证
- [ ] `./gradlew assembleDebug` 编译通过
- [ ] 安装到设备验证走势图价格标签
- [ ] 提交 GitHub

---

## 阶段 31：年度收益/总资产切换（2026-05-24）

> 年视图增加收益/总资产双模式 pill 切换。收益模式看投资波动，总资产模式看年末资产总值 + 较上年变化。数据在 loadYearView 时一次性加载，切换不重新查询数据库。

### 31.1 数据模型
- [x] `YearEarning` 新增字段：`yearEndTotalValue`, `yearOverYearChange`, `yearOverYearChangePct`
- [x] `EarningsUiState` 新增 `yearViewMode: YearViewMode` 枚举（EARNINGS / TOTAL_ASSETS）

### 31.2 ViewModel
- [x] `loadYearView()` 同时查询年末 `DailySummary.totalValueCNY`，计算同比变化
- [x] 新增 `selectYearViewMode(mode)` 方法切换模式

### 31.3 UI
- [x] `YearEarningsView` 新增顶部 pill 切换（收益 / 总资产）
- [x] 收益模式：保留现有卡片（年度总收益 + 涨/跌/活跃天数）
- [x] 总资产模式：卡片显示年末总资产 + 较上年变化额 + 变化率
- [x] 日/周/月视图不修改

### 31.4 编译验证
- [x] `./gradlew assembleDebug` 编译通过
- [x] 安装到设备验证年视图切换
- [ ] 提交 GitHub
