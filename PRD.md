# 资产管理 App 产品设计文档

## 项目概览

| 项目 | 说明 |
|------|------|
| 目标平台 | Android |
| 最低 SDK | API 31+ (Android 12) |
| 核心定位 | 一站式个人财富管理，直观查看资产全貌与变化 |
| 核心原则 | 数据本地存储、可视化呈现、多资产覆盖、多账号隔离 |

---

## 零、账号系统

### 设计原则

- **本地多账号**：单数据库 + `accountId` 列隔离，不依赖服务端
- **PIN 码保护**：每个账号 4 位数字 PIN，切换账号需验证
- **快速切换**：设置页一键切换，无需重新登录

### Account 实体

```kotlin
@Entity
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,          // 昵称，用于显示和区分
    val pinHash: String,           // PIN 的 SHA-256 哈希
    val createdAt: Long            // 创建时间戳
)
```

### 数据隔离规则

所有业务实体新增 `accountId` 列：
- `Deposit.accountId`、`Holding.accountId`
- `PriceSnapshot.accountId`、`Transaction.accountId`
- `DailySummary.accountId`、`DailyBreakdownItem.accountId`
- `ExchangeRate` 为全局共享，不加 accountId

DAO 查询时始终 `WHERE accountId = :currentAccountId`。

### 启动流程

```
App 冷启动
  │
  ├─ 无账号 → WelcomeScreen (创建第一个账号)
  │            ├─ 输入昵称
  │            ├─ 设置 4 位 PIN
  │            └─ 确认 PIN → 创建 Account → 进入首页
  │
  └─ 有账号 → PinUnlockScreen
               ├─ 显示上次使用账号昵称
               ├─ 4 位 PIN 输入
               ├─ 验证通过 → 进入首页
               └─ [切换账号] → 账号列表 → 选账号 → 输 PIN → 首页
```

### 账号管理（设置页）

```
┌─────────────────────────────────┐
│ 当前账号：张三                   │
│ ┌─────────────────────────────┐ │
│ │ 切换账号          >         │ │
│ │ 修改 PIN           >         │ │
│ │ 创建新账号          >         │ │
│ │ 删除当前账号 (红色)   >       │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

- **切换账号**：弹出账号列表 → 选择 → 输 PIN → 切到该账号 → 刷新所有数据
- **修改 PIN**：输入旧 PIN → 输入新 PIN × 2 → 确认
- **创建新账号**：同 WelcomeScreen 流程
- **删除账号**：输 PIN 确认 → 删除该账号所有数据（CASCADE）

### 全局状态

```kotlin
// Hilt 单例，持有当前激活的账号 ID
@Singleton
class AccountManager @Inject constructor(
    private val accountDao: AccountDao
) {
    private val _currentAccountId = MutableStateFlow<Long?>(null)
    val currentAccountId: StateFlow<Long?> = _currentAccountId.asStateFlow()

    suspend fun switchAccount(accountId: Long) { ... }
    suspend fun createAccount(nickname: String, pin: String): Account { ... }
    fun logout() { _currentAccountId.value = null }
}
```

所有 ViewModel 和 Repository 从 `AccountManager.currentAccountId` 获取当前账号 ID，作为 DAO 查询的过滤条件。

### 导航影响

新增两条顶层路由，在 NavHost 中优先判断：

```
NavHost(startDestination = if (hasAccounts) Route.PinUnlock else Route.Welcome) {
    composable<Route.Welcome> { ... }
    composable<Route.PinUnlock> { ... }
    composable<Route.Home> { ... }
    // ... 原有路由
}
```

成功解锁/创建账号后 navigate 到 `Route.Home` 并清除回退栈。

---

## 测试数据一键生成

### 入口

设置页底部新增卡片：**"生成测试数据"**

### 行为

为当前账号写入完整假数据（存在则跳过，避免重复）：

| 类型 | 数量 | 详情 |
|------|------|------|
| 存款 | 3 笔 | 工商定期 CNY 200,000 (2.75%)、招商活期 CNY 50,000 (0.35%)、BoA USD 8,000 (4.5%) |
| A 股 | 2 只 | 贵州茅台 600519 × 100 股 @1680、比亚迪 002594 × 200 股 @245 |
| 美股 | 1 只 | Apple AAPL × 50 股 @$175 |
| 基金 | 2 只 | 易方达蓝筹 005827 × 10000 份 @1.50、天弘沪深300 000961 × 5000 份 @1.20 |
| 黄金 | 1 笔 | 实物金条 50g，总价 ¥22,500 |
| 价格快照 | 每只 × 90 天 | 以成本价为中心，随机 ±15% 波动 |
| 汇率 | 90 天 | USD→CNY 约 7.0-7.3 随机波动 |
| DailySummary | 90 天 | 自动补算生成 |

生成完成后 Toast "测试数据已生成" 并刷新首页。

---

## 技术选型

| 层 | 技术 | 理由 |
|----|------|------|
| 数据存储 | **Room (SQLite)** | 官方推荐、Flow 响应式、适合复杂关联查询 |
| UI 框架 | **Jetpack Compose + Material 3** | 声明式 UI、Material You 动态取色、动画流畅 |
| 架构 | **MVVM + Repository** | 单向数据流：DAO(Flow) → Repository → ViewModel(StateFlow) → Composable |
| 依赖注入 | **Hilt** | 编译期检查、Google 全家桶配合顺畅 |
| 导航 | **Compose Navigation (Type-Safe Routes)** | 官方标配、类型安全 |
| 图表 | **Vico** | Compose-native、Material 3 风格、支持折线/柱状/热力图 |
| 网络 | **Retrofit + OkHttp** | 成熟稳定 |
| JSON | **Kotlinx Serialization** | Kotlin-native、编译期安全 |

---

## 视觉设计

### 风格定位

轻量级现代风（Apple 风格）+ Material You 动态取色。

- 卡片式布局，大圆角，微投影
- 跟随系统壁纸自动生成主题色
- 支持深色/浅色模式自适应
- 涨红跌绿（中国习惯）

### 色彩体系

| 用途 | 颜色 |
|------|------|
| 主色调 | Material You 动态取色（跟随壁纸） |
| 上涨 | #E53935（红色） |
| 下跌 | #43A047（绿色） |
| 卡片背景 | Surface Container（跟随主题） |
| 背景 | Background（跟随主题） |

---

## 导航结构

```
App NavHost (startDestination 动态判断)

├─ Welcome (无账号时)          ← 新建
├─ PinUnlock (有账号时)        ← 解锁
│
└─ 主界面 (解锁后)
   ├─ 🏠 首页  │ 📋 持仓  │ 📊 收益  │ ⚙️ 设置
   │          (中间悬浮 ➕ 按钮)
   ├─ 持仓详情 / 新增存款 / 新增持仓 / 编辑
   └─ 账号管理（设置页内）
```

---

## 一、首页（仪表板）

### 布局

```
┌─────────────────────────────────┐
│  总资产  ¥1,234,567              │
│  今日收益  +3,200  (+0.26%)      │
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 资产走势图 (Vico 折线)        │ │
│ │ 7天 / 30天 / 1年 可切换      │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌──────────┐ ┌──────────┐      │
│ │ 💰 存款   │ │ 📈 股票   │      │
│ │ ¥500,000 │ │ ¥423,000 │      │
│ │ +320 今日 │ │ +2,500 今日│     │
│ └──────────┘ └──────────┘      │
│ ┌──────────┐ ┌──────────┐      │
│ │ 💵 基金   │ │ 🥇 黄金   │      │
│ │ ¥210,000 │ │ ¥101,567 │      │
│ │ +150 今日 │ │ +15 今日  │      │
│ └──────────┘ └──────────┘      │
└─────────────────────────────────┘
```

### 交互

- 点击资产类别卡片 → 跳转持仓列表对应 Tab
- 走势图支持 7天/30天/1年/全部 切换
- 下拉刷新 → 拉取最新价格并重算

---

## 二、持仓管理

### Tab 结构

`[全部] [存款] [股票] [基金] [黄金]`

### 存款卡片

```
┌─────────────────────────────────┐
│ 工商银行定期                     │  ← 左滑删除，点进编辑
│ 本金 ¥200,000    利率 2.75%      │
│ 已产生利息 ¥1,832 (持有 121天)   │
│ 到期 2027/03/15                 │
│ 当前估值 ¥201,832               │
└─────────────────────────────────┘
```

### 股票/基金卡片

```
┌─────────────────────────────────┐
│ 贵州茅台 (600519)                │  ← 左滑删除，点进详情
│ 持有 100 股   成本 ¥1,680        │
│ 当前 ¥1,720                     │
│ 持仓盈亏 +¥4,000 (+2.38%)       │
│ 今日 +¥200 (+0.12%)             │
└─────────────────────────────────┘
```

### 黄金卡片

```
┌─────────────────────────────────┐
│ 实物金条                         │
│ 50 克   买入总价 ¥22,500         │
│ 当前金价 ¥458/g                 │
│ 持仓盈亏 +¥400 (+1.78%)         │
│ 今日 +¥15 (+0.07%)              │
└─────────────────────────────────┘
```

### 新增存款表单

| 字段 | 说明 |
|------|------|
| 存款名称 | 如"工商定期" |
| 银行/平台 | 如"工商银行" |
| 币种 | USD/CNY/JPY/HKD/EUR 等 |
| 本金 | 数值 |
| 年化利率 | 百分比 |
| 存入日期 | 日期选择器 |
| 到期日期 | 日期选择器 |
| 备注 | 选填 |

### 新增股票/ETF 表单

| 字段 | 说明 |
|------|------|
| 代码 | 在线搜索 → 自动填名称 |
| 市场 | A股/美股/港股 |
| 持有数量 | 股数 |
| 成本价 | 每股成本 |
| 买入日期 | 日期选择器 |

### 新增基金表单

| 字段 | 说明 |
|------|------|
| 基金代码 | 在线搜索 → 自动填名称 |
| 持有份额 | 份额数 |
| 成本净值 | 买入时单位净值 |
| 买入日期 | 日期选择器 |

### 新增黄金表单

| 字段 | 说明 |
|------|------|
| 形式 | 实物金条 / 纸黄金 / 黄金 ETF |
| 重量 | 克 |
| 买入总价 | 总花费 |
| 买入日期 | 日期选择器 |

---

## 三、持仓详情页

```
┌─────────────────────────────────┐
│ ← 贵州茅台 (600519)              │
│                                  │
│ ¥1,720.00                        │
│ +20.00 (+1.18%) 今日             │
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 价格走势 (Vico 小面积图)      │ │
│ │ 1月 / 3月 / 1年 切换          │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌────────────┐ ┌────────────┐  │
│ │ 持仓盈亏     │ │ 今日盈亏     │  │
│ │ +¥4,000   │ │ +¥200     │  │
│ │ +2.38%    │ │ +0.12%    │  │
│ └────────────┘ └────────────┘  │
│                                  │
│ 持有 100 股                      │
│ 成本 ¥1,680.00                   │
│ 总成本 ¥168,000                  │
│ 当前市值 ¥172,000                │
│                                  │
│ ▶ 交易记录                       │
│   2025-05-20  买入 100股 @1680   │
│                                  │
│ [编辑]            [删除]         │
└─────────────────────────────────┘
```

---

## 四、收益日历

### 交互

- 顶部 `[日] [周] [月] [年]` 分段控制器
- 涨 = 红色底，跌 = 绿色底
- 每个格子显示当日汇总 ± 数值
- 点击格子 → 浮层展示分类明细

### 日视图（月历）

```
┌─────────────────────────────────┐
│   日      周      月      年     │
│                                  │
│    2026年 5月                    │
│ 一  二  三  四  五  六  日       │
│                   1   2   3      │
│  4   5   6   7   8   9  10      │
│ 11  12  13  14  15  16  17      │
│ 18  19  20 [21] 22  23  24      │
│                                  │
│ ┌──────────────────────────┐    │
│ │ 5月21日           +320.50 │    │
│ │ 存款 +15   股票 +200      │    │
│ │ 基金 +150  黄金 +15       │    │
│ └──────────────────────────┘    │
└─────────────────────────────────┘
```

### 周视图

本周 7 天的柱状图，每天一根柱子，红色涨绿色跌。

### 月视图

月度汇总 + 每日迷你柱状图。

### 年视图

全年热力图（GitHub contributions 风格），颜色深浅表示盈亏大小。

---

## 五、设置页

| 设置项 | 说明 |
|--------|------|
| 账号管理 | 显示当前账号昵称，切换/创建/修改PIN/删除账号 |
| 生成测试数据 | 一键为当前账号生成完整假数据（90天历史） |
| 数据导出 | 导出为 CSV，可选 [保存到文件] 或 [分享] |
| 数据导入 | 系统文件选择器选 CSV → 解析预览 → 确认写入 |
| 汇率基准 | 显示当前汇率，支持手动刷新 |
| 清空数据 | 确认后清空当前账号全部本地数据 |

---

## 六、数据模型

### Room 实体

#### Account（新增）
```kotlin
@Entity
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,          // 昵称
    val pinHash: String,           // SHA-256(PIN)
    val createdAt: Long            // 时间戳
)
```

#### ExchangeRate（全局共享，无 accountId）
```kotlin
@Entity
data class ExchangeRate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromCurrency: String,    // USD
    val toCurrency: String,      // CNY
    val date: LocalDate,
    val rate: BigDecimal
)
```

#### Deposit
```kotlin
@Entity
data class Deposit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,           // 所属账号
    val name: String,
    val bank: String,
    val currency: String,
    val principal: BigDecimal,
    val interestRate: BigDecimal,
    val startDate: LocalDate,
    val maturityDate: LocalDate,
    val status: String,            // active / matured
    val note: String = ""
)
```

#### Holding（股票/基金/黄金统一）
```kotlin
@Entity
data class Holding(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,           // 所属账号
    val type: String,              // STOCK / FUND / GOLD
    val symbol: String,
    val name: String,
    val market: String = "",
    val currency: String,
    val quantity: BigDecimal,
    val costPrice: BigDecimal,
    val costDate: LocalDate,
    val note: String = ""
)
```

#### PriceSnapshot
```kotlin
@Entity
data class PriceSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingId: Long,
    val accountId: Long,           // 所属账号
    val date: LocalDate,
    val unitPrice: BigDecimal,
    val currency: String
)
```

#### Transaction
```kotlin
@Entity
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val holdingId: Long,
    val accountId: Long,           // 所属账号
    val type: String,              // BUY / SELL / DIVIDEND
    val date: LocalDate,
    val price: BigDecimal,
    val quantity: BigDecimal,
    val fee: BigDecimal = BigDecimal.ZERO
)
```

#### DailySummary
```kotlin
@Entity
data class DailySummary(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,           // 所属账号
    val date: LocalDate,
    val totalValueCNY: BigDecimal,
    val dayChange: BigDecimal,
    val dayChangePct: BigDecimal
)
```

#### DailyBreakdownItem
```kotlin
@Entity
data class DailyBreakdownItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,           // 所属账号
    val date: LocalDate,
    val type: String,              // DEPOSIT / STOCK / FUND / GOLD
    val valueCNY: BigDecimal,
    val changeCNY: BigDecimal
)
```

### 实体关系

```
Account ──────────── 1:N → Deposit, Holding, DailySummary, ...
ExchangeRate ─────── 独立实体，全局共享，按日期查询
Deposit ──────────── 独立实体，利息本地计算
Holding ──────────── 1:N → PriceSnapshot（每日价格）
Holding ──────────── 1:N → Transaction（交易记录）
DailySummary ─────── 1:N → DailyBreakdownItem（分类明细）
```

---

## 七、数据源架构

### 统一接口

```kotlin
interface PriceProvider {
    val assetType: String
    suspend fun fetchPrice(symbol: String, date: LocalDate): PriceResult
    suspend fun search(query: String): List<SearchResult>
}
```

### 各提供方

| 提供方 | 数据范围 | 来源 |
|--------|---------|------|
| AStockProvider | A股实时/历史价格 | 新浪财经 / 东方财富 |
| USStockProvider | 美股历史价格 | Alpha Vantage / Finnhub |
| CNFundProvider | 国内基金净值 | 天天基金 |
| GoldProvider | 国际金价 | 金交所 / XAU API |
| ExchangeRateProvider | 汇率历史 | Frankfurter API（免费） |

### 降级策略

所有网络请求失败时，沿用最近一次成功获取的价格，并在界面标注「价格更新于 yyyy-MM-dd」。

---

## 八、离线补算逻辑

### 触发时机

每次 App 冷启动时：

```
1. 查询 DailySummary 最大日期 → lastDate
2. 如果 lastDate < today:
   补算 lastDate+1 到 today 之间的每一天
```

### 按资产类型分别处理

| 资产类型 | 补算方式 | 依赖 |
|----------|---------|------|
| 存款 | 日息 = 本金 × 年化利率 ÷ 365，每日估值精确可算 | 纯本地，不会失败 |
| 股票/基金/黄金 | 拉历史收盘价/净值，计算估值与盈亏 | 网络（失败降级） |
| 汇率 | 拉历史汇率，换算外币存款 | 网络（失败降级） |

### 补算流程

```
补算某一天 (date):
  1. 遍历所有 active 存款:
     估值 = 本金 + 日息 × 持有天数
     换算为 CNY（应用当日汇率）

  2. 遍历所有 Holding:
     拉取当日 PriceSnapshot（或沿用上日价）
     估值 = quantity × unitPrice
     换算为 CNY

  3. 汇总:
     totalValueCNY = 所有资产 CNY 估值之和
     dayChange = totalValueCNY - 上日 totalValueCNY

  4. 写入 DailySummary + DailyBreakdownItem
```

### 补算结果

补算完成后，首页走势图、收益日历的所有历史数据都是完整的，不会出现空白。

---

## 九、CSV 导入导出

### deposit.csv

```csv
name,bank,currency,principal,interest_rate,start_date,maturity_date,status,note
工商定期,工商银行,CNY,200000,2.75,2025-01-15,2027-03-15,active,
BoA Savings,Bank of America,USD,8000,4.5,2024-06-01,2026-06-01,active,
```

### holdings.csv

```csv
type,symbol,name,market,currency,quantity,cost_price,cost_date,note
STOCK,600519,贵州茅台,CN,CNY,100,1680.00,2025-03-01,
FUND,005827,易方达蓝筹精选,CN,CNY,10000,1.50,2024-12-15,
GOLD,XAU,实物金条,,CNY,50,450.00,2024-01-01,50克
```

### transactions.csv（可选）

```csv
date,type,symbol,action,price,quantity,fee
2025-05-20,STOCK,600519,BUY,1720.00,100,5.00
2025-05-21,FUND,005827,BUY,1.55,5000,0
```

### 导入流程

1. 用户通过系统文件选择器选取 CSV 文件
2. 解析并校验格式
3. 弹出预览对话框，展示解析结果（条数、示例数据）
4. 用户确认后批量写入 Room
5. 写入后触发一次全量补算

### 导出流程

1. 用户点击导出
2. 底部弹出选项：`[保存到文件]` 或 `[分享]`
3. 保存到文件 → 系统文件选择器选目录
4. 分享 → 调起系统 Share Sheet

---

## 十、网络数据拉取策略

| 时机 | 操作 |
|------|------|
| App 冷启动 | 检查汇率缓存日期，非当日则拉最新汇率 + 全量价格快照 |
| 新增持仓搜索 | 在线搜索股票/基金代码 |
| 手动下拉刷新 | 拉取最新价格并重算今日 |
| 补算历史 | 按需拉取历史收盘价/汇率 |

所有网络请求失败时静默降级，沿用缓存数据，界面标注更新时间。

---

## 十一、项目结构

```
app/
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt
│   │   ├── dao/
│   │   │   ├── AccountDao.kt          (新增)
│   │   │   ├── DepositDao.kt          (修改：过滤 accountId)
│   │   │   ├── HoldingDao.kt          (修改：过滤 accountId)
│   │   │   ├── DailySummaryDao.kt     (修改：过滤 accountId)
│   │   │   ├── DailyBreakdownItemDao.kt (修改：过滤 accountId)
│   │   │   ├── PriceSnapshotDao.kt    (修改：过滤 accountId)
│   │   │   ├── ExchangeRateDao.kt     (不变)
│   │   │   └── TransactionDao.kt      (修改：过滤 accountId)
│   │   └── entity/
│   │       ├── Account.kt             (新增)
│   │       └── ... (各 Entity 加 accountId)
│   ├── remote/  (不变)
│   ├── repository/
│   │   ├── AccountRepository.kt       (新增)
│   │   └── ... (各 Repository 注入 AccountManager)
│   └── csv/  (不变)
├── domain/
│   ├── account/
│   │   └── AccountManager.kt          (新增：全局账号状态)
│   ├── calculator/  (不变)
│   └── testdata/
│       └── TestDataGenerator.kt       (新增：测试数据生成)
├── ui/
│   ├── auth/
│   │   ├── WelcomeScreen.kt           (新增)
│   │   ├── PinUnlockScreen.kt         (新增)
│   │   └── AccountListScreen.kt       (新增)
│   ├── home/  (修改：accountId 过滤)
│   ├── holdings/  (修改：accountId 过滤)
│   ├── earnings/  (修改：accountId 过滤)
│   ├── settings/
│   │   ├── SettingsScreen.kt          (修改：账号管理+测试数据)
│   │   └── SettingsViewModel.kt       (修改)
│   ├── components/  (不变)
│   ├── navigation/
│   │   ├── AppNavigation.kt           (修改：新增 auth 路由)
│   │   └── Route.kt                   (修改：新增 route)
│   └── theme/  (不变)
├── di/
│   └── AppModule.kt                   (修改：AccountManager)
└── App.kt
```

---

## 十二、阶段 14 已完成修复（2026-05-22）

### 14.1 首页资产分类当日收益 ✅
### 14.2 走势图坐标轴和触摸交互 ✅
### 14.3 子页面底部导航栏 ✅
### 14.4 持仓 Tab 滑动切换 ✅
### 14.5 股票详情页数据修复 ✅
### 14.6 收益视图重构 ✅
### 14.7 统计块尺寸统一 ✅

---

## 十三、新需求（2026-05-22 第二轮）

### 13.1 实时价格拉取体系

**问题诊断**：
- `USStockProvider` 使用 `demo` 作为 Alpha Vantage API Key，不返回真实数据
- App 冷启动后从未主动从网络拉取最新价格：`HomeViewModel` 只读 DB 缓存，`BackfillEngine.getOrFetchPrice()` 只在无本地缓存时才联网，下拉刷新只重算不重拉
- A 股 / 基金 / 黄金 / 汇率 Provider 的 API 对接代码正确，但都没有被主动调用

**修复方案**：

#### 13.1.1 USStockProvider API Key 配置化
- 在 `NetworkModule` 中注入 API Key（通过 `local.properties` 或设置页手动输入）
- 降级方案：若无有效 Key，美股价格使用 Finnhub 免费接口（60次/分钟）

#### 13.1.2 首页实时价格刷新
- 进入首页时，后台异步拉取所有持仓股票/基金/黄金的最新价格
- 拉取成功后更新 `PriceSnapshot` 表，重算今日总资产和日收益，写入 `DailySummary`
- 拉取失败时静默降级，沿用缓存数据，显示「价格更新于 HH:mm」
- 新增 UI 组件 `LastUpdateTime`：显示最后成功刷新时间
- 下拉刷新 = 强制重新拉取 API 价格

#### 13.1.3 今日收益持久化
- 每次实时计算完今日收益后，立即写入 `DailySummary` 和 `DailyBreakdownItem`
- 收益页面从 DB 读取，不再重新计算
- 使用 `INSERT OR REPLACE` 策略（同一天多次刷新会覆盖）

### 13.2 收益周视图重新设计

**问题**：当前周视图显示近 6 周所有数据，信息过多，查找困难。

**新设计**：

```
┌─────────────────────────────────┐
│   日      周      月      年     │
│                                  │
│  2026年 5月              ◀ ▶    │  ← 月份切换
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 第1周  5.1 - 5.3             │ │  ← 4周卡片，按月分组
│ │ ████▌ (日一二三的柱状图)      │ │
│ │ 周收益 +1,200.50             │ │
│ └──────────────────────────────┘ │
│ ┌──────────────────────────────┐ │
│ │ 第2周  5.4 - 5.10            │ │
│ │ ██████████████▌              │ │
│ │ 周收益 +2,800.00             │ │
│ └──────────────────────────────┘ │
│ ┌──────────────────────────────┐ │
│ │ 第3周  5.11 - 5.17           │ │
│ │ ██████▌                      │ │
│ │ 周收益 -450.00               │ │
│ └──────────────────────────────┘ │
│ ┌──────────────────────────────┐ │
│ │ 第4周  5.18 - 5.24           │ │
│ │ ████████████▌                │ │
│ │ 周收益 +3,500.00             │ │
│ └──────────────────────────────┘ │
└─────────────────────────────────┘
```

- 顶部：年份 + 月份 + 左右箭头切换
- 每月显示 4-5 周（按自然周周一~周日分组）
- 每周一张卡片，内含日柱状图 + 周收益汇总
- 点击某周卡片可展开查看每日明细

### 13.3 首页信息层级重构

**问题**：当前首页缺乏视觉重点，存款卡片不显示每笔的当日收益，整体布局无法突出核心信息。

**新设计原则**：
- **今日收益是第一重点**：最大字号 + 红绿强烈对比 + 背景色呼应
- **总资产是第二重点**：大字号但弱于日收益
- **分类卡片是辅助信息**：紧凑布局，清晰标注分类日收益

```
┌─────────────────────────────────┐
│                                  │
│        今日收益 (大标题)          │
│    + ¥ 3,200.50                  │  ← 48sp, 超粗体, 红涨绿跌
│       +0.26%                     │  ← 22sp, 同色
│                                  │
│   总资产  ¥ 1,234,567.00         │  ← 20sp, 弱化但清晰
│                                  │
│ ┌──────────────────────────────┐ │
│ │ 资产走势 (折线图)  7d/30d/1y  │ │
│ └──────────────────────────────┘ │
│                                  │
│ ┌────────┐┌────────┐┌────────┐  │
│ │💰 存款 ││📈 股票 ││💵 基金 │  │  ← 3列网格
│ │502,000 ││423,000 ││210,000 │  │
│ │+320 今 ││+2,500今││+150 今 │  │  ← 每类标注日收益
│ └────────┘└────────┘└────────┘  │
│ ┌────────┐                      │
│ │🥇 黄金  │                      │  ← 第4个单独一行
│ │101,567 │                      │
│ │+15 今日│                      │
│ └────────┘                      │
└─────────────────────────────────┘
```

**关键改动**：
- 今日收益提到最顶部，字号 48sp + ExtraBold
- 总资产弱化为次要信息
- 分类卡片增加每类的今日收益数据
- 存款卡片在持仓列表中也需显示每笔的今日利息收益

### 13.4 存款卡片日收益

**问题**：存款列表不显示每笔存款当日产生的利息，无法感知每日增长。

**修复**：
- 每张存款卡片新增「今日利息」= 本金 × 年化利率 ÷ 365
- 显示格式：`今日 +¥12.33`
- 若币种非 CNY，换算为 CNY 显示

### 13.5 UI 视觉升级

**问题**：当前 UI 过于原生 Material Design，缺乏精致感。

**升级方向**：

#### 13.5.1 卡片风格
- 使用 `CardDefaults.elevatedCardElevation(2.dp)` 替代 `surfaceVariant` 背景
- 卡片圆角统一 16dp（大卡片 20dp）
- 白色/浅灰背景 + 微妙投影（非纯色背景）

#### 13.5.2 排版层级
- 关键数字使用 `fontWeight = FontWeight.ExtraBold` + 大字号
- 涨跌颜色使用 700 weight 字体配对应色
- 标签文本使用 `onSurfaceVariant` + `labelSmall`

#### 13.5.3 间距系统
- 统一使用 4/8/12/16/20/24 dp 的 spacing scale
- 卡片之间 12dp，section 之间 24dp
- 卡片内边距 16dp（小卡片）或 24dp（大卡片）

#### 13.5.4 图标替代
- 资产分类使用 Material Icons 替代 emoji（如 `AccountBalance`、`TrendingUp`、`WaterDrop`）
- 导航栏使用 Outlined/Filled 风格切换

#### 13.5.5 动画微交互
- 数字变化时使用 `animateIntAsState` 过渡
- 卡片点击有涟漪 + 微缩放效果
- 下拉刷新使用 Material 3 内置动画
- 页面切换使用 fade + slide 过渡

---

## 十四、数据一致性修复（2026-05-22 第三轮）

### 14.1 详情页数据空白

**根因**：`HoldingDetailViewModel.loadHolding()` 中对 Room Flow 调用了 `.collect {}`。Room Flow 永不 complete，导致协程永久阻塞在 `.collect {}`，后续设置 `_uiState` 的代码永远不会执行。

**修复**：所有 Room Flow 读操作改为 `.first()`（只取一次快照，不阻塞）。

### 14.2 今日收益不随实时价格更新

**根因**：`HomeViewModel` 流程为 `backfillIfNeeded()` → `fetchLivePrices()` → `refreshData()`。`fetchLivePrices` 拉取最新价格写入 PriceSnapshot 后，`refreshData` 先查 DailySummary 表，发现今日已有记录就直接使用旧数据，不重新计算。

**修复**：`fetchLivePrices` 拉取成功后设置 `needsRecompute = true`，`refreshData` 检查此标记，若为 true 则跳过读取 DailySummary 缓存，强制执行 `computeFromEntities`。

### 14.3 脏数据标记机制

**问题**：新增资产时若指定历史日期（如 30 天前买入股票），已有的 DailySummary 不会自动重算——30 天前的总资产应该包含新买入的资产。

**设计**：

```
新增/编辑/删除资产
  ↓
找到该资产的最早日期（deposit.startDate 或 holding.costDate）
  ↓
markDirty(fromDate, accountId)
  ↓ 删除 fromDate 到 today 的所有 DailySummary + DailyBreakdownItem
  ↓
backfillIfNeeded(accountId)
  ↓ 补算 fromDate 到 today
```

**新增 DAO 方法**：
- `DailySummaryDao.deleteFromDate(date, accountId)` — 删除 >= date 的记录
- `DailyBreakdownItemDao.deleteFromDate(date, accountId)` — 删除 >= date 的记录

**触发时机**：
- 新增存款 → `markDirty(deposit.startDate)`
- 新增持仓 → `markDirty(holding.costDate)`
- 编辑/删除资产 → `markDirty(最早资产的日期)`
- 生成测试数据 → 插入完成后 `markDirty` + `backfillIfNeeded`

### 14.4 价格 API 端到端验证

**验证方法**：在每个 Provider 的 `fetchPrice` 方法中添加 `Log.d` 日志，确认 API 返回真实数据。

**AStockProvider (新浪财经)**：
- URL: `https://hq.sinajs.cn/list=sh600519`
- 期望返回: `var hq_str_sh600519="贵州茅台,1720.00,..."` 
- 验证：解析 `values[3]` 作为当前价

**CNFundProvider (天天基金)**：
- URL: `https://fundgz.1234567.com.cn/js/510300.js`
- 期望返回: `jsonpgz({"fundcode":"510300","name":"...","gsz":"3.95",...})`
- 验证：`gsz` 字段为实时估值

**GoldProvider (新浪 XAU)**：
- URL: `https://hq.sinajs.cn/list=hf_XAU`
- 期望返回: `var hq_str_hf_XAU="2650.50,..."`
- 验证：`values[0]` 为美元/盎司价格

**USStockProvider (Finnhub)**：
- URL: `https://finnhub.io/api/v1/quote?symbol=AAPL&token=...`
- 期望返回: `{"c": 175.50, "h": 176.00, ...}`
- 验证：`c` 字段为当前价

### 14.5 底部导航栏验证

**已有修复**：`FinancialFreedomApp.subRoutes` 包含所有持仓子页面路由。

**验证清单**：
- 首页 → 点击资产卡片 → 持仓列表 → 底部导航可见 ✅
- 持仓 → 点击股票 → 详情页 → 底部导航可见
- 持仓 → 新增存款 → 表单 → 底部导航可见
- 持仓 → 编辑 → 表单 → 底部导航可见

---

## 十五、UI 视觉重设计（2026-05-22 第四轮）

### 15.1 首页布局重构

**当前问题**：
- 今日收益卡片与 PullToRefreshBox 的滚动嵌套导致顶部重叠
- 今日收益卡片使用半透明彩色背景显得突兀
- 总资产字太小，信息层级混乱

**新设计**：

```
┌──────────────────────────────────┐
│  总资产                          │  ← 12sp 灰色标签
│  ¥ 1,234,567                     │  ← 32sp ExtraBold 深色
│  +1,234  +0.12%                  │  ← 20sp Bold 涨跌色
│  最后更新 11:30                  │  ← 10sp 灰色
├──────────────────────────────────┤
│  资产走势          7天｜30天｜1年 │  ← 自定义文字切换，不用 FilterChip
│  ┌────────────────────────────┐  │
│  │     [走势图区域]            │  │  ← 白色卡片，3dp 阴影
│  └────────────────────────────┘  │
├──────────────────────────────────┤
│  资产配置                        │  ← 16sp Bold
│  ┌────────┐┌────────┐┌────────┐  │
│  │💰 存款 ││📈 股票 ││💵 基金 │  │  ← 白色卡片，等高三列
│  │ 50万   ││ 34万   ││ 19万   │  │  ← 数字大且加粗
│  │+37 今日││+12 今日││+5 今日 │  │
│  └────────┘└────────┘└────────┘  │
│  ┌────────────────────────────┐  │
│  │🥇 黄金               ¥4.7万│  │  ← 黄金独占一行
│  │+8 今日                     │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘
```

**关键改动**：
- 去掉 PullToRefreshBox 内的 `verticalScroll`（重复嵌套导致重叠）
- 今日收益不单独用彩色背景卡片，信息直接放在顶部
- 总资产放最上面（最重要），今日收益是它的子信息
- 趋势图时间切换：不用 FilterChip（粗灰边框丑），改用文字按钮

### 15.2 资产卡片等高三列

**当前问题**：数字变 18sp 后，三列卡片因内容长度不同导致高度不一。

**修复方案**：
- Row 加 `Modifier.height(IntrinsicSize.Max)` 强制等高
- 每个柱子内 valueText 固定 `maxLines = 1`，防止换行撑高
- 存款/股票/基金数字最多 10 字符（含逗号），16sp Bold 单行放得下

### 15.3 趋势时间切换器

**当前问题**：`FilterChip` 有粗灰色外框，视觉上很突兀。

**修复方案**：用 Row + 三个可点击 Text 替代：
- 选中项：深色文字 + 底部短横线指示器
- 未选中：灰色文字
- 间距 16dp，整体干净轻量

### 15.4 持仓页去掉「全部」Tab

**当前问题**：全部 tab 混合显示所有类型，列表杂乱。

**修复方案**：
- tabs 改为 `["存款", "股票", "基金", "黄金"]`（四 tab）
- 去掉全部 tab，各类型独立展示
- pagerState pageCount 从 5 改为 4
- defaultTab 映射调整（原全部=0 删除，存款=0，股票=1，基金=2，黄金=3）

### 15.5 整体配色调整

**当前问题**：灰色太多（surfaceVariant），缺乏层次感和质感。

**修复方案**：
- 所有卡片容器用 `surface`（白色）替代 `surfaceVariant`（灰色）
- 卡片阴影提升到 3-4dp，增强层次
- 资产类型色（蓝/红/紫/橙）保留但仅用于图标和数字点缀
- 卡片背景统一白色，通过阴影制造层次
- 页面背景保持浅灰/白，卡片浮在背景上形成自然层次
- 涨跌红绿色保持中国习惯（红涨绿跌）
