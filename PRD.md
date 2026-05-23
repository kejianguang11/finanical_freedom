# 扶摇阁 — 资产管理 App 产品设计文档

## 项目概览

| 项目 | 说明 |
|------|------|
| 应用名称 | **扶摇阁** |
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
┌──────────────────────────────────────────┐
│                                          │
│  净资产                                   │
│  ¥ 586,320                               │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  资产                              │  │
│  │  ┌──────────┐ ┌──────────┐ ┌────┐ │  │
│  │  │ 现金     │ │ 存款     │ │持仓│ │  │
│  │  │ ¥45,000  │ │ ¥120,500 │ │45万│ │  │
│  │  └──────────┘ └──────────┘ └────┘ │  │
│  │  ┌──────────┐ ┌──────────┐        │  │
│  │  │ 应收     │ │ 负债     │        │  │
│  │  │ ¥30,000  │ │ -¥60,000 │        │  │
│  │  └──────────┘ └──────────┘        │  │
│  │  ──────────────────────────────── │  │
│  │  点击各卡片跳转对应 Tab            │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  今日收益  +1,250.30  +0.21%       │  │
│  │                                    │  │
│  │  A股      +820.00  ████████████    │  │
│  │  美股     +280.30  ████            │  │
│  │  基金      +95.50  █▌              │  │
│  │  存款利息  +34.50  ▌               │  │
│  │  黄金      +20.00  ▏               │  │
│  │                                    │  │
│  │  现金流入/流出不计入收益            │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │  近期收益趋势   7天  30天  1年     │  │
│  │  [折线图]                          │  │
│  └────────────────────────────────────┘  │
│                                          │
│  [首页]  [持仓]  [收益]  [我的]           │
└──────────────────────────────────────────┘
```

说明：
- 净资产卡片按**资产 / 负债**分区，资产在上、负债在下，中间分割线
- 2×3 网格布局，每个格子够手指点击
- 今日收益明细**按金额降序**排列，附带迷你条形图直观对比

### 净资产公式

```
净资产 = 现金余额 + 存款(active) + 持仓市值 + 应收款 - 负债
```

### 交互

- 点击净资产各分项 → 跳转持仓对应 Tab
- 走势图支持 7天/30天/1年/全部 切换
- 下拉刷新 → 拉取最新价格并重算

---

## 二、持仓管理

### Tab 结构

`[持仓] [存款] [现金] [信用]`

| Tab | 内容 | 子结构 |
|-----|------|--------|
| 持仓 | 股票 + 基金 + 黄金 | 子 Tab：`[股票] [基金] [黄金]` |
| 存款 | 定期存款 | 子 Tab：`[持有中] [已到期]` |
| 现金 | 现金余额 + 流水 | 无子 Tab |
| 信用 | 应收款 + 负债 | 页面内上下分区 |

### 存款

存款 Tab 内分两个子 Tab：**持有中 / 已到期**。

#### 持有中存款卡片

```
┌─────────────────────────────────┐
│ 工商银行定期                     │  ← 左滑删除，点进编辑
│ 本金 ¥200,000    利率 2.75%      │
│ 已产生利息 ¥1,832 (持有 121天)   │
│ 到期 2027/03/15                 │
│ 当前估值 ¥201,832               │
│ 今日利息 +¥15.07                 │
└─────────────────────────────────┘
```

#### 到期存款处理（全自动）

```
到期日当天 BackfillEngine 自动检测
    │
    ▼
存款 status → "matured"
    │
    ▼
本金 + 累计利息 → 自动转入现金余额
  （生成一条现金流水，备注"XX存款到期入账"）
    │
    ▼
存款 status → "settled"
    │
    ▼
首页不再显示该存款，现金余额自动增加
```

存款三态：`active`（持有中）→ `matured`（已到期，待处理）→ `settled`（已赎回，仅历史记录）

#### 已到期存款卡片

```
┌─────────────────────────────────┐
│ 工行定期                已到期 ✓ │
│ 本金 ¥50,000    利率 3.25%       │
│ 持有 90 天                      │
│ 总利息 ¥401    本息 ¥50,401      │
│ 2026-03-01 自动赎回到现金        │
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

### 现金 Tab

```
┌─────────────────────────────────────┐
│                                     │
│   ┌───────────────────────────┐     │
│   │   现金余额                 │     │
│   │   ¥ 45,000.00             │     │
│   └───────────────────────────┘     │
│                                     │
│   资金流水                           │
│                                     │
│   ┌─────────────────────────────┐   │
│   │ 2026-05-20  手动存入  +10000│   │
│   │ 2026-05-15  手动取出   -2000│   │
│   │ 2026-03-01  工行定期到期入账  │   │
│   │             本息 +50,401    │   │
│   │ 2026-01-10  手动存入   +5000│   │
│   └─────────────────────────────┘   │
│                                     │
│   [+ 手动入金]  [- 手动出金]         │
└─────────────────────────────────────┘
```

- 余额汇总展示
- 每次操作（手动加减、存款到期入账）记一条流水
- 流水包含：日期、类型、金额、备注

### 信用 Tab（应收款 + 负债）

```
┌─────────────────────────────────────┐
│                                     │
│  ┌─ 应收款 ────────────── [+] ─┐   │
│  │  张三              ¥20,000  │   │
│  │  2026-02-10 借出            │   │
│  │  预计 2026-08 归还          │   │
│  │  ─────────────────────     │   │
│  │  王五              ¥10,000  │   │
│  │  合计应收  ¥30,000          │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─ 负债 ──────────────── [+] ─┐   │
│  │  招行信用卡       ¥35,000   │   │
│  │  李四             ¥25,000   │   │
│  │  合计负债  ¥60,000          │   │
│  └─────────────────────────────┘   │
│                                     │
│  应收净额  -¥30,000                 │
└─────────────────────────────────────┘
```

- 应收款和负债上下分区，各自独立管理
- 每项可新增、编辑、删除
- 底部显示应收净额（应收 - 负债）

### 新增存款表单

| 字段 | 说明 |
|------|------|
| 银行/平台 | 如"工商银行"（作为存款唯一标识，不再单独填名称） |
| 币种 | USD/CNY/JPY/HKD/EUR 等 |
| 本金 | 数值 |
| 年化利率 | 百分比 |
| 存入日期 | 日期选择器 |
| 到期日期 | 日期选择器 |
| 备注 | 选填 |

> **v16 变更**：去掉"存款名称"字段，银行/平台名称即作为存款标识。Deposit 实体 `name` 字段废弃，值等于 `bank`。

### 新增股票/ETF 表单

| 字段 | 说明 |
|------|------|
| 代码 | **输入时自动搜索**（A股+美股+港股），下拉显示匹配结果，选中后自动补全名称和市场 |
| 名称 | 选中文搜索后自动填入，也可手动修改 |
| 市场 | 选中后自动填入（CN/US/HK），也可手动修改 |
| 持有数量 | 股数 |
| 成本价 | 每股成本 |
| 买入日期 | 日期选择器 |

> **v16 变更**：搜索从手动点搜索图标改为**输入时自动搜索**（300ms 去抖）。搜索结果聚合 A 股（东方财富）、美股（Finnhub）、港股（新增东方财富港股搜索 API）三个市场。选中某条结果后，代码、名称、市场一并自动补全。

### 新增基金表单

| 字段 | 说明 |
|------|------|
| 基金代码 | **输入时自动搜索**，下拉显示匹配结果，选中后自动补全名称 |
| 基金名称 | 选中后自动填入，也可手动修改 |
| 持有份额 | 份额数 |
| 成本净值 | 买入时单位净值 |
| 买入日期 | 日期选择器 |

> **v16 变更**：基金搜索改为和股票一样的自动搜索交互，不再需要手动点搜索按钮。

### 新增黄金表单

| 字段 | 说明 |
|------|------|
| 克数 | 黄金重量（克） |
| 单价 | 每克价格（元/克） |
| 购买日期 | 日期选择器 |

> **v16 变更**：黄金不再填写代码、名称、形式。只需克数 + 单价 + 购买日期。**买入总价 = 克数 × 单价**，自动计算展示，无需用户手填。

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
| 一键重算收益 | 删除所有每日收益汇总，从最早资产日期重新回填计算，不影响原始数据 |
| 显示倍率 | 可选 10%/50%/100%，所有页面金额显示乘以所选倍率，默认 100%，持久化存储 |

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

#### CashTransaction（新增 — 现金流水）

```kotlin
@Entity
data class CashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val date: LocalDate,
    val amount: BigDecimal,          // 正数=入金，负数=出金
    val type: String,                // MANUAL / DEPOSIT_MATURITY（存款到期）
    val note: String = ""
)
```

#### Receivable（新增 — 应收款）

```kotlin
@Entity
data class Receivable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,                // 对方姓名
    val amount: BigDecimal,
    val date: LocalDate,             // 借出日期
    val expectedDate: LocalDate?,    // 预计归还日，可选
    val note: String = ""
)
```

#### Debt（新增 — 负债）

```kotlin
@Entity
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val name: String,                // 来源（银行/个人）
    val amount: BigDecimal,
    val date: LocalDate,
    val interestRate: BigDecimal?,   // 可选利率
    val note: String = ""
)
```

### 实体关系

```
Account ──────────── 1:N → Deposit, Holding, DailySummary, CashTransaction, ...
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

## 二十四、一键备份与恢复

### 24.0 设计目标

用户填写测试数据后，一键导出完整快照。改代码或清数据后，一键恢复，无需重新手填。

### 24.1 备份格式

**JSON**（非 CSV），因为：
- 保留完整类型信息（BigDecimal、LocalDate 等）
- 单文件包含所有表，便于管理
- 人类可读，便于调试

```json
{
  "version": 1,
  "exportedAt": "2026-05-22T15:30:00",
  "accountId": 1,
  "data": {
    "deposits": [...],
    "holdings": [...],
    "priceSnapshots": [...],
    "transactions": [...],
    "dailySummaries": [...],
    "dailyBreakdownItems": [...],
    "cashTransactions": [...],
    "receivables": [...],
    "debts": [...],
    "exchangeRates": [...]
  }
}
```

### 24.2 备份范围

| 表 | 过滤条件 |
|----|---------|
| Deposit | `accountId = current` |
| Holding | `accountId = current` |
| PriceSnapshot | `accountId = current` |
| Transaction | `accountId = current` |
| DailySummary | `accountId = current` |
| DailyBreakdownItem | `accountId = current` |
| CashTransaction | `accountId = current` |
| Receivable | `accountId = current` |
| Debt | `accountId = current` |
| ExchangeRate | 全局（无 accountId 过滤） |

### 24.3 备份流程

1. 用户在设置页点击「一键备份」
2. 系统文件选择器（SAF `ACTION_CREATE_DOCUMENT`）选择保存位置
3. 从 Room 读取当前账号所有数据
4. 序列化为 JSON，写入选中文件
5. Toast "备份完成，共 N 条数据"

### 24.4 恢复流程

1. 用户在设置页点击「一键恢复」
2. **二次确认弹窗**：警告将覆盖当前账号所有数据
3. 系统文件选择器（SAF `ACTION_OPEN_DOCUMENT`）选择备份文件
4. 读取 JSON → 解析校验
5. **预览弹窗**：显示备份日期、各表记录数
6. 用户点「确认恢复」
7. 清空当前账号所有数据 → 逐表写入备份数据
8. 触发 `markDirtyAndBackfill` 重算历史
9. Toast "恢复完成，已刷新数据" → 自动返回首页

### 24.5 安全约束

| 规则 | 说明 |
|------|------|
| 版本校验 | `version` 字段不匹配 → 拒绝恢复 |
| accountId 校验 | 备份的 accountId 必须与当前账号一致 |
| 二次确认 | 恢复前弹窗警告，防止误操作 |
| 事务包裹 | 清空 + 写入在同一 Room 事务中，失败则回滚 |

### 24.6 实现要点

- 新增 `BackupManager`（`@Singleton`），注入所有 DAO + `AppDatabase`（用于 `runInTransaction`）
- 使用 `kotlinx.serialization` 序列化各 Entity（需给 Entity 加 `@Serializable`）
- SettingsViewModel 新增 `backup()` 和 `restore(uri)` 方法
- SettingsScreen 新增两个入口卡片

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

## 十六、数据正确性 Bug 分析（2026-05-22 第五轮）

### 16.0 核心问题概述

用户反馈三个典型问题：
1. **黄金页面点进去没有价格趋势图**（空白或平线）
2. **股票/基金当日变动幅度严重失真**（如茅台 600519 显示今日 -31%，绝不可能）
3. **趋势图缺少百分比标注**（只能看到绝对价格，看不到涨跌百分比）

经完整代码审查，发现 **7 个关联 Bug**，根因集中在「回填引擎不使用历史数据 API」和「测试数据与真实价格不连续」。

---

### 16.1 【关键】BackfillEngine 不使用 fetchHistory，全部日期用实时价格

**文件**：`BackfillEngine.kt:214`（`getOrFetchPrice` 方法）

**问题**：
```kotlin
// 当前代码（错误）
val result = priceService.fetchPrice(type, symbol, market, date)
```

`getOrFetchPrice` 调用的是 `fetchPrice()`，但所有 Provider 的 `fetchPrice()` 都**不接受/不使用 `date` 参数**：

| Provider | fetchPrice 实际行为 | fetchHistory（已实现但从未调用） |
|----------|--------------------|-------------------------------|
| AStockProvider | 新浪 API 返回实时价，忽略 date | 东方财富 K线 API，返回真实历史日线 |
| CNFundProvider | 天天基金返回实时估值，忽略 date | 东方财富净值 API，返回真实历史净值 |
| GoldProvider | 东方财富 AU9999 实时价，忽略 date | 无（逐日调 fetchPrice，全部同价） |
| HKStockProvider | 新浪港股实时价，忽略 date | 空列表（未实现） |
| USStockProvider | Finnhub 实时报价，忽略 date | Finnhub candle API（未验证） |

**影响**：
- 回填引擎逐日补算时，每一天拿到的都是**今天此刻的实时价**
- 所有历史 PriceSnapshot 都是同一价格 → **所有持仓的趋势图都是平线**
- 当 App 跨天打开两次，两次回填会给同一天写入不同价格（每天的实时价），造成数据混乱

**修复方向**：
- `BackfillEngine.getOrFetchPrice` 对历史日期应优先调用 `fetchHistory(start, end)` 批量拉取
- 仅对「今天」使用 `fetchPrice` 获取实时价
- `GoldProvider.fetchHistory` 需对接东方财富历史金价 API
- `HKStockProvider.fetchHistory` 需对接港股历史数据

---

### 16.2 【关键】测试数据随机价格与真实市场价格不连续

**文件**：`TestDataGenerator.kt:226-255`、`HomeViewModel.kt:155-200`

**问题链路**：
```
1. TestDataGenerator 为茅台生成 90 天随机游走价格
   起点 ¥1680，dailyVolatility=1.5%，90天后可能随机到 ¥2100 左右

2. App 冷启动 → backfillIfNeeded() 用随机游走价格回填历史 DailySummary

3. fetchLivePrices() 拉取今天真实茅台价格（如 ¥1450）

4. computeFromEntities() 重算今日：
   - 今日总资产 = 200股 × ¥1450（真实价）= ¥290,000
   - 昨日总资产（来自 DailySummary）= 200股 × ¥2100（随机游走）= ¥420,000
   - dayChangePct = (290000 - 420000) / 420000 = -30.95%
```

**这就是用户看到的「茅台今日 -31%」的根本原因。**

**修复方向**：
- 生成测试数据后，立即用 `fetchHistory` 拉取真实历史价格覆盖随机数据
- 或者：`fetchLivePrices` 拉取今日价格后，同时拉取昨日真实收盘价，用真实昨日价算今日变动
- 长期方案：用户添加真实持仓后，不允许测试数据与真实数据混合

---

### 16.3 【关键】HomeViewModel 销毁黄金缓存导致数据空白

**文件**：`HomeViewModel.kt:88-91, 113-125`

**问题**：
```kotlin
if (!cacheCleared) {
    clearGoldPriceCache(accountId)  // 删除所有黄金 PriceSnapshot！
    cacheCleared = true
}
```

每次 App 启动都删除黄金的所有价格快照。注释说「旧黄金缓存可能价格偏高」，但实际是因为之前 GoldProvider 的 API 有问题。现在的 GoldProvider 修复后，这个清除操作反而导致：

1. 黄金快照全部被删
2. BackfillEngine 重新回填 → 调用 GoldProvider.fetchPrice → 忽略 date → 所有天同一价格
3. 如果用户在回填完成前打开黄金详情页 → `priceHistory` 为空 → 显示「暂无价格数据」
4. 即使回填完成 → 所有快照都是同一实时价 → 趋势图平线

**修复方向**：
- 删除 `clearGoldPriceCache` 的自动调用
- 改为数据库 migration 一次性清理，或仅在用户手动「刷新数据」时清理
- 修复 GoldProvider.fetchHistory 后自然不需要此 workaround

---

### 16.4 【中等】趋势图 Y 轴不显示百分比

**文件**：`TrendChart.kt:272-280`

**问题**：`TrendChart` 的 Y 轴标签使用 `formatYLabel()`，只格式化绝对数值（如 `1.5k`、`2.3万`）。在持仓详情页的价格趋势图中，用户需要看到：
- 每个数据点的价格（绝对值）
- **价格相对于首日/昨日的涨跌百分比**

当前只有点击 Tooltip 才能看到百分比，非点击状态下完全不可见。

**修复方向**：
- 为 `TrendChart` 增加可选的「副 Y 轴」显示百分比
- 或在图表区域叠加涨跌百分比标签
- Tooltip 保持现有交互，但增加默认显示的百分比信息

---

### 16.5 【中等】HoldingDetailViewModel 取「前一日价格」逻辑不严谨

**文件**：`HoldingDetailViewModel.kt:110-118`

**问题**：
```kotlin
val prevPrice = yesterdaySnapshot?.unitPrice                           // 查昨日快照
    ?: priceSnapshotDao.getPrevious(h.id, accountId)?.unitPrice        // fallback: 第二新
    ?: currentPrice                                                     // fallback: 当前价
```

`getPrevious()` 的 SQL：
```sql
SELECT * FROM price_snapshots WHERE holdingId=:id ORDER BY date DESC LIMIT 1 OFFSET 1
```

这个查询取的是「按日期降序的第二条记录」。如果同一天有多条快照（如重复拉取导致 INSERT OR REPLACE 失败、或不同时间拉取），第二新的快照可能仍然是**今天**的快照（而非昨天的）。

此时 `prevPrice == currentPrice`，`todayChange = 0`——但这只是静默错误。更隐蔽的情况是：如果历史回填时某天插入失败，`getPrevious` 可能跳到更早的日期，导致前一日价格偏差巨大。

**修复方向**：
- 改为查询 `date < today` 的最新一条快照，确保拿到的是真正的「前一日」价格
- `getPrevious` 应改为 `getLatestBefore(date: LocalDate)`: `SELECT * FROM price_snapshots WHERE holdingId=:id AND date < :date ORDER BY date DESC LIMIT 1`

---

### 16.6 【低】HKStockProvider.fetchHistory 返回空列表

**文件**：`HKStockProvider.kt:55-58`

```kotlin
override suspend fun fetchHistory(...): List<PriceResult> {
    return emptyList()  // 港股历史数据暂不支持
}
```

港股（如腾讯 00700）完全无法获取历史价格，趋势图永远无数据。

**修复方向**：
- 对接新浪港股历史 API 或东方财富港股 K 线 API

---

### 16.7 【低】USStockProvider 同样存在 date 参数忽略问题

**文件**：`USStockProvider.kt`

与 AStockProvider/GoldProvider 同理，`fetchPrice` 调 Finnhub quote 端点，永远返回实时价，不使用 date 参数。`fetchHistory` 虽已实现但从未被 BackfillEngine 调用。

---

### 16.8 修复优先级

| 优先级 | Bug | 影响范围 | 修复复杂度 |
|--------|-----|---------|-----------|
| P0 | 16.1 Backfill 不用 fetchHistory | 全部资产趋势图 | 高（需改 BackfillEngine + 所有 Provider） |
| P0 | 16.2 测试数据与真实价格不连续 | 日变动严重失真 | 中（需改 HomeViewModel 计算逻辑） |
| P0 | 16.3 黄金缓存清除 | 黄金无趋势图 | 低（删代码即可） |
| P1 | 16.5 前一日价格逻辑 | 详情页今日涨跌不准 | 低（改 SQL 查询） |
| P1 | 16.4 趋势图无百分比 | 用户体验 | 中（需改 TrendChart 组件） |
| P2 | 16.6 港股无历史 | 港股趋势图 | 中（对接新 API） |
| P2 | 16.7 美股 date 忽略 | 美股趋势图 | 中 |

### 16.9 修复记录（2026-05-22）

| Bug | 状态 | 改动文件 |
|-----|------|---------|
| 16.1 Backfill 不用 fetchHistory | ✅ 已修复 | BackfillEngine.kt, GoldProvider.kt, HKStockProvider.kt |
| 16.2 测试数据与真实价格不连续 | ✅ 已修复 | HomeViewModel.kt (computeFromEntities 重写) |
| 16.3 黄金缓存清除 | ✅ 已修复 | HomeViewModel.kt (删除 clearGoldPriceCache) |
| 16.4 趋势图无百分比 | ✅ 已修复 | TrendChart.kt, HoldingDetailScreen.kt |
| 16.5 前一日价格逻辑 | ✅ 已修复 | PriceSnapshotDao.kt, HoldingDetailViewModel.kt, HoldingsViewModel.kt |
| 16.6 港股无历史 | ✅ 已修复 | HKStockProvider.kt |
| 16.7 美股 date 忽略 | ⏳ 待修复 | USStockProvider.kt（影响较小，后续迭代） |

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

---

## 十七、应用更名

**扶摇阁** — 取自庄子《逍遥游》「抟扶摇而上者九万里」，寓意资产如大鹏展翅，乘风直上。

| 改动项 | 说明 |
|--------|------|
| 应用显示名 | `strings.xml` 中 `app_name` → 扶摇阁 |
| 文档标题 | PRD.md / TASKS.md / UI_DESIGN.md / README.md 项目名 → 扶摇阁 |
| 包名 | `com.financial.freedom` 保持不变（内部标识，非用户可见） |

---

## 十八、日期选择器修复

### 18.0 问题概述

用户反馈：新增的编辑页面和弹窗中，日期字段只能手动输入，点击无反应，没有弹出日历选择器。

**根因**：`EditDepositScreen`、`EditHoldingScreen`、`CreditScreen.ReceivableDialog` 三个页面的日期字段只用了普通 `OutlinedTextField`，没有接入 Material 3 `DatePickerDialog`。而 `AddDepositScreen` 和 `AddHoldingScreen` 已正确实现，需统一。

### 18.1 需修复的页面

| 页面 | 日期字段 | 当前状态 | 修复方案 |
|------|---------|---------|---------|
| `EditDepositScreen` | 存入日期、到期日期 | 纯文本输入 | 接入 DatePickerDialog |
| `EditHoldingScreen` | 买入日期 | 纯文本输入 | 接入 DatePickerDialog |
| `CreditScreen.ReceivableDialog` | 预计归还日 | 纯文本输入 | 接入 DatePickerDialog |

### 18.2 修复方案

参照 `AddDepositScreen` 的实现模式：
- 日期字段设为 `readOnly = true`
- 外层包裹 `clickable`（indication=null, interactionSource=MutableInteractionSource）触发 DatePicker
- `rememberDatePickerState()` + `DatePickerDialog` 弹窗
- 确认后将 `selectedDateMillis` 转为 `LocalDate` 并更新状态

---

## 二十、导航优化 & 首页视觉升级（2026-05-22）

### 20.0 问题概述

用户反馈三个问题：
1. **跨分类导航困难**：从股票(page 1) 滑到存款(page 4) 需经过基金、黄金，中间 3 次滑动
2. **首页布局单调**：仅 4 张卡片纵向排列，缺乏视觉层次
3. **日期选择器不完整**：DebtDialog 无日期字段，AddHoldingScreen 使用 emoji 图标

### 20.1 Category 快速跳转导航条

在 Pager 上方新增分类导航条 `[投资] [存款] [现金] [信用]`：
- 仅在 page 1-7 显示
- 点击对应分类 → `animateScrollToPage(该分类第一页)`
- 当前分类金色高亮 + 底部指示线，其他灰色
- 映射：投资→1, 存款→4, 现金→6, 信用→7

### 20.2 SectionIndicator 可点击

子项名称（股票/基金/黄金、持有中/已到期）改为可点击，点击直接跳转到该子页。

### 20.3 首页资产配置占比条

在资产卡片上方新增堆叠占比条：
- 按投资/存款/现金/信用占比显示彩色分段
- 每段颜色对应该资产类型色
- 下方标注类别名称 + 百分比
- 容器为白色 ElevatedCard，16dp 圆角

### 20.4 日期选择器补全

- `DebtDialog`：新增日期字段 + DatePickerDialog
- `AddHoldingScreen`：替换 emoji 📅 为 Material Icon `DateRange`

---

## 二十一、累计收益率（Modified Dietz）

详见 UI_DESIGN.md 第 6.6 节。核心要点：

- **今日涨跌**：分母用昨日总资产，剔除当天新入金本金
- **累计收益率**：Modified Dietz 分母，消除入金时机对收益率的扭曲
- **数据模型**：`DailySummary` 新增 `netInflow` 字段
- **UI**：今日收益卡片底部加累计投入/收益/收益率一行
- **走势图**：数据不变（走势图展示每日总资产，不涉及入金拆分）

---

## 二十二、统一 CSV & 名称回填 & 现金扣款（2026-05-22）

### 22.0 问题概述

1. CSV 导入导出分散在 3 个文件（deposits/holdings/transactions），使用不便
2. 股票/基金/黄金的名称需手动填写，实际可通过 API 自动获取
3. 新增资产时不会自动从现金扣除，导致净值虚高

### 22.1 统一 CSV 格式

所有资产合并到单个 `assets.csv`，列定义如下：

```
type,name,bank,symbol,market,currency,principal,quantity,cost_price,interest_rate,start_date,end_date,note
```

不同资产类型使用不同字段子集：

| 列 | 存款 | 股票 | 基金 | 黄金 |
|----|------|------|------|------|
| type | DEPOSIT | STOCK | FUND | GOLD |
| name | 存款名称 | 留空(API回填) | 留空(API回填) | 留空(固定"黄金") |
| bank | 银行名 | - | - | - |
| symbol | - | 代码 | 代码 | XAU |
| market | - | CN/US/HK | CN | - |
| principal | 本金 | - | - | - |
| quantity | - | 股数 | 份额 | 克数 |
| cost_price | - | 成本价 | 成本净值 | 买入总价 |
| interest_rate | 年化利率 | - | - | - |

### 22.2 名称自动回填

- `HoldingDao` 新增 `updateName(id, name)` 方法
- `HomeViewModel.fetchLivePrices()` 拉取实时价格后，检查 name 为空则从 API 结果回填
- `BackfillEngine` 拉取历史价格时同步回填空名称
- 黄金导入时 name 自动 = "黄金"，symbol = "XAU"

### 22.3 从现金扣除选项

新增资产表单增加 toggle "从现金中扣除"（默认关闭）：
- 存款：扣除金额 = 本金
- 股票/基金：扣除金额 = quantity × cost_price  
- 黄金：扣除金额 = cost_price（买入总价）
- 创建 `CashTransaction(type=ASSET_PURCHASE, amount=负数)`

### 22.4 清理

- 删除旧独立导出方法（exportDeposits/exportHoldings/exportTransactions）
- 删除旧独立导入方法（importDeposits/importHoldings）
- SettingsScreen 导出入口合并为单按钮

---

## 二十三、自动搜索 & 港股搜索 & 黄金简化 & 多笔交易（2026-05-22）

### 23.0 问题概述

1. **存款名称多余**：银行/平台已经能标识存款，再填"名称"重复
2. **搜索交互繁琐**：股票/基金需手动点搜索图标 → 弹窗 → 输入 → 选结果，步骤太多
3. **港股搜索缺失**：HKStockProvider.search() 返回空列表，用户无法搜索港股
4. **黄金表单冗余**：黄金不需要代码/名称/形式，用户只需知道克数和单价
5. **多笔交易不支持**：同一只股票今天加仓、明天减仓，目前只能创建多个独立持仓

### 23.1 存款去名称

- `AddDepositScreen` 去掉"存款名称"输入框
- `Deposit.name` 保存时自动设为 `bank` 的值
- `EditDepositScreen` 同步去掉名称字段
- 存款卡片展示：银行名即标题，不再显示单独的"名称"
- 数据库不做迁移（`name` 列保留，值等于 `bank`）

### 23.2 股票/基金输入时自动搜索

**交互流程**：

```
用户在代码输入框输入 "600"
  ↓ (300ms 去抖)
自动调用 PriceService.searchAll(query) → 并发搜索 A股+美股+港股
  ↓
下拉列表出现在输入框下方（覆盖在其他内容之上）
  ├─ 600519  贵州茅台  A股
  ├─ 600030  中信证券  A股
  └─ ...
  ↓ 用户点击某一项
代码、名称、市场 自动填入表单对应字段
下拉列表消失
```

**规格**：
- 去抖 300ms，避免每次按键都发请求
- 搜索中显示 loading 指示器（输入框右侧小转圈）
- 结果列表最多显示 8 条，超出可滚动
- 点击列表外区域或清空输入框 → 列表消失
- 网络失败时静默降级，不弹错误提示
- 用户仍可手动输入（不选择搜索结果），此时名称需手填

### 23.3 港股搜索实现

`HKStockProvider.search()` 对接东方财富港股搜索 API：

- URL: `https://searchadapter.eastmoney.com/api/suggest/get?input={query}&type=14&token=d&market=HK`
- 或复用 AStockProvider 的搜索逻辑，追加港股市场过滤参数
- 返回 `SearchResult(type="STOCK", market="HK")`
- `PriceService.searchAll()` 中新增 `hkStockProvider.search(query)` 调用

### 23.4 黄金表单简化

**旧字段**：形式（实物/纸黄金/ETF）、代码、名称、重量、买入总价、买入日期

**新字段**：

| 字段 | 说明 |
|------|------|
| 克数 | 黄金重量（克），数字输入 |
| 单价 | 每克价格（元/克），数字输入 |
| 购买日期 | DatePickerDialog |

**自动计算**：
- 买入总价 = 克数 × 单价
- 实时显示在表单底部：`买入总价：¥12,345.00`
- 保存时 `Holding.costPrice = 克数 × 单价`（存储总价作为 costPrice）
- `Holding.symbol = "XAU"`, `Holding.name = "黄金"`, `Holding.type = "GOLD"`, `Holding.market = ""`

**金价实时参考**：表单底部可选展示当前国际金价（元/克），作为填单价的参考：
```
当前参考金价 ¥458.50/克（上海金交所 AU9999）
更新于 15:30
```

### 23.5 股票/基金多笔交易（加仓/减仓）

**现状分析**：
- `Transaction` 实体已支持多笔记录（holdingId + type + date + price + quantity）
- `HoldingDetailScreen` 已展示交易记录列表
- **缺失**：无 UI 入口创建加仓/减仓交易

**设计方案**：

#### 23.5.1 持仓详情页新增操作按钮

```
┌─────────────────────────────────┐
│ ← 贵州茅台 (600519)              │
│                                  │
│ ¥1,720.00                        │
│ +20.00 (+1.18%) 今日             │
│                                  │
│ [价格走势图]                      │
│                                  │
│ ┌────────────┐ ┌────────────┐  │
│ │ 持仓盈亏     │ │ 今日盈亏     │  │
│ │ +¥4,000   │ │ +¥200     │  │
│ └────────────┘ └────────────┘  │
│                                  │
│ 持有 100 股 · 成本 ¥1,680        │
│ 总成本 ¥168,000 · 市值 ¥172,000  │
│                                  │
│ ┌──────────┐ ┌──────────┐       │
│ │  + 加仓   │ │  - 减仓   │       │  ← 新增两个按钮
│ └──────────┘ └──────────┘       │
│                                  │
│ ▶ 交易记录                       │
│   2025-06-01  买入 50股 @1750   │  ← 加仓记录
│   2025-05-20  买入 100股 @1680  │  ← 初始买入
│                                  │
│ [编辑]            [删除]         │
└─────────────────────────────────┘
```

#### 23.5.2 加仓弹窗

```
┌─────────────────────────────────┐
│  加仓 — 贵州茅台 (600519)        │
│                                  │
│  当前持仓 100 股 · 成本均价 ¥1,680│  ← 参考信息
│                                  │
│  买入数量 (股)   [           ]   │
│  买入价格 (元)   [           ]   │
│  买入日期        [ 2026-06-01 ]  │
│                                  │
│  加仓后：                        │  ← 实时预览
│  持有 150 股                     │
│  成本均价 ¥1,703.33             │  ← 加权平均
│  总成本 ¥255,500                │
│                                  │
│  从现金中扣除 [ Switch ]         │
│                                  │
│  [取消]              [确认加仓]   │
└─────────────────────────────────┘
```

#### 23.5.3 减仓弹窗

```
┌─────────────────────────────────┐
│  减仓 — 贵州茅台 (600519)        │
│                                  │
│  当前持仓 100 股 · 成本均价 ¥1,680│
│                                  │
│  卖出数量 (股)   [           ]   │
│  卖出价格 (元)   [           ]   │
│  卖出日期        [ 2026-06-05 ]  │
│                                  │
│  减仓后：                        │
│  剩余 60 股                      │
│  实现盈亏 +¥5,200 (+38.2%)       │  ← 卖出盈亏
│  剩余持仓成本均价 ¥1,680        │
│                                  │
│  收入计入现金 [ Switch ]          │  ← 卖出金额自动入金
│                                  │
│  [取消]              [确认减仓]   │
└─────────────────────────────────┘
```

#### 23.5.4 数据模型影响

- **Holding 表不变**：`quantity` 和 `costPrice` 在每次加仓/减仓后更新
  - 加仓：`costPrice = (原quantity × 原costPrice + 新quantity × 新price) / (原quantity + 新quantity)`
  - 减仓：`costPrice` 不变（FIFO 或平均成本），`quantity -= 卖出数量`
- **Transaction 表**：每次加仓/减仓各生成一条记录
  - 加仓：`type=BUY, price=买入价, quantity=买入量`
  - 减仓：`type=SELL, price=卖出价, quantity=卖出量`
- **CashTransaction**：如果开启"从现金扣除"或"收入计入现金"，自动生成对应流水
- **新增 DAO 方法**：`TransactionDao.getByHolding(holdingId)` 已存在

#### 23.5.5 边界条件

| 场景 | 处理 |
|------|------|
| 减仓数量 > 当前持仓 | 报错"卖出数量不能超过持仓数量" |
| 减仓数量 = 当前持仓 | 全部清仓 → Holding.quantity = 0，标记 status = "closed" |
| 加仓数量 ≤ 0 | 报错"买入数量必须大于 0" |
| 已清仓的持仓 | 不显示加仓/减仓按钮，仅保留历史记录可查看 |

#### 23.5.6 Holding 新增 status 字段

```kotlin
// Holding.kt 新增字段
val status: String = "active"  // active / closed
```

- `active`：正常持有中，可加仓减仓
- `closed`：已全部卖出，仅历史记录
- 首页/持仓列表默认过滤 `status = "active"`

---

## 二十五、持仓分组重设计（v17 — 2026-05-23）

### 25.0 设计目标

**问题**：同一平台的存款分散为多张卡片，同一只股票的多次买入各自独立展示。信息碎片化，无法快速了解在某银行/某股票上的总敞口。

**方案**：
- **存款**：按 `bank` 字段分组，同银行存单合并为银行组卡片；点击进入银行详情，逐条查看存单
- **股票/基金**：按 `symbol` 字段分组，同代码持仓合并为标的组卡片；卡片内可展开查看每笔买入记录及盈亏
- **黄金**：保持扁平（通常只有 1-2 笔）

### 25.1 存款分组

#### 银行组卡片（替代现有 DepositCard）

同银行（`bank` 字段相同）的存单合并为一张银行组卡片：
- 银行名 + 存单数
- 本金合计、当前估值（本金 + 累计利息）
- 今日利息合计
- 加权平均进度条
- 最近到期日

点击 → 进入 `BankDepositsScreen`，展示该银行下所有存单明细。

#### 银行详情页

- 顶部汇总卡片（本金合计 + 累计利息 + 当前估值 + 今日利息）
- 存单列表：每张存单显示本金、利率、日期、进度条、累计利息、今日利息
- 支持编辑/删除单张存单
- 支持快速添加存单到该银行

### 25.2 股票/基金分组

#### 标的组卡片（替代现有 HoldingCard）

同 symbol 的持仓合并为一张标的组卡片：
- 名称、代码、市场标签
- 当前价、今日涨跌
- 持有总量、成本均价（加权平均）、持仓盈亏、市值
- **可展开区域**：显示每笔买入记录（日期、数量、价格、该笔盈亏）
- 展开后显示 [+ 加仓] [- 减仓] 按钮

点击卡片主体 → 进入 `HoldingDetailScreen`（现有）。

#### 黄金

保持扁平，不分组。

### 25.3 三级数据汇总

每一层入口都展示 **总价值 + 当日盈亏**：

| 层级 | 位置 | 内容 |
|------|------|------|
| Level 0 | 首页 2×2 网格 | 投资/存款/现金/信用 各类总值 + 今日 |
| Level 1 | CategoryNavStrip 汇总行 | 当前分类总计 + 今日（如"投资总计 ¥423,000 +¥2,500"） |
| Level 2 | SectionIndicator 子类汇总 | 当前子类总计 + 今日（如"股票市值 ¥358,000 +¥2,350"） |
| Level 3 | 分组卡片列表 | 各银行/标的的总值 + 今日 |

### 25.4 金额展示

首页及所有汇总数字使用**完整数字**（如 ¥420,000），不再使用"万"缩写。

### 25.5 新增/变更页面

| 页面 | 说明 |
|------|------|
| `BankDepositsScreen` | **新增**：银行存单详情页 |
| `ActiveDepositsPage` | **改**：扁平列表 → 银行组卡片列表 |
| `MaturedDepositsPage` | **改**：同上 |
| `StockPage` | **改**：扁平列表 → 标的组卡片（可展开） |
| `FundPage` | **改**：同上 |
| `GoldPage` | **不变** |
| `MainPagerScreen` | **改**：CategoryNavStrip + SectionIndicator 增加汇总行 |
| `HomeScreen` | **改**：金额完整展示（去掉"万"） |

### 25.6 数据层变更

| 变更 | 说明 |
|------|------|
| `HoldingsUiState` 新增字段 | `bankGroups`、`stockGroups`、`fundGroups`、`categorySummaries` |
| 新增 Display 数据类 | `BankGroupDisplay`、`HoldingGroupDisplay`、`BuyRecordDisplay` |
| DAO 补充查询 | `DepositDao.getByBankAndStatus()`、`TransactionDao` 已有 `getByHolding` |
| ViewModel 分组逻辑 | 在 `toDepositDisplay` / `toHoldingDisplay` 之后按 bank/symbol 分组聚合 |
- `AppDatabase` version bump 到 7
