# 开发任务拆解

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
| 16 - 数据一致性 & Bug 修复 | **active** | 5 个子任务（详见下方） |

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
| **合计** | **78** |
