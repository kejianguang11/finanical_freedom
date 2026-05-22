# Financial Freedom

一站式个人财富管理 Android App，离线优先 + 多资产覆盖 + 多账号隔离。

## 功能

- **资产管理** — 存款、股票（A股/美股）、基金、黄金，统一估值
- **收益日历** — 日 / 周 / 月 / 年 多维度收益视图，GitHub 风格热力图
- **走势图表** — 资产走势折线图，支持 7天 / 30天 / 1年 切换
- **多账号隔离** — 本地 PIN 码保护，数据完全隔离
- **离线优先** — SQLite 本地存储，离线补算，网络失败自动降级
- **CSV 导入导出** — 批量导入持仓数据，导出备份
- **实时价格** — A股 / 美股 / 基金 / 黄金 / 汇率 自动拉取

## 技术栈

| 层 | 技术 |
|----|------|
| UI | Jetpack Compose + Material 3 |
| 数据 | Room (SQLite) |
| 架构 | MVVM + Repository |
| DI | Hilt |
| 导航 | Compose Navigation (Type-Safe) |
| 图表 | Vico |
| 网络 | Retrofit + OkHttp + Kotlinx Serialization |

## 数据源

| 资产 | 来源 |
|------|------|
| A股 | 新浪财经 |
| 美股 | Finnhub |
| 基金 | 天天基金 |
| 黄金 | 新浪 XAU |
| 汇率 | Frankfurter API |

## 构建

```bash
# Android Studio 打开项目，或命令行：
./gradlew assembleDebug
```

最低 SDK 31 (Android 12)，目标 SDK 35。

## 项目结构

```
app/
├── data/
│   ├── local/       # Room Entity + DAO + Database
│   ├── remote/      # 网络层 (Retrofit Provider)
│   ├── repository/  # 数据仓库
│   └── csv/         # CSV 导入导出
├── domain/
│   ├── account/     # 账号管理 (AccountManager)
│   ├── calculator/  # 估值 / 利息计算
│   └── testdata/    # 测试数据生成
├── ui/
│   ├── auth/        # 欢迎页 / PIN 解锁
│   ├── home/        # 首页仪表板
│   ├── holdings/    # 持仓管理
│   ├── earnings/    # 收益日历
│   ├── settings/    # 设置页
│   ├── components/  # 共享 UI 组件
│   ├── navigation/  # 路由定义
│   └── theme/       # 主题配色
└── di/              # Hilt 依赖注入
```

## License

MIT
