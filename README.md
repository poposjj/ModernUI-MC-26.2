# ModernUI · Minecraft 26.2 修改版（Fabric）

- **修改人**：oioi
- **原作者**：BloCamLimb — 官方仓库 [BloCamLimb/ModernUI-MC](https://github.com/BloCamLimb/ModernUI-MC) · [BloCamLimb/ModernUI](https://github.com/BloCamLimb/ModernUI)
- **当前版本**：`3.13.0.9`（基于上游 26.2 移植分支构建，目标 Minecraft **26.2** / Fabric）
- **下载地址**：[Releases](https://github.com/poposjj/ModernUI-MC-26.2/releases)
- **许可**：LGPL-3.0-or-later（继承上游）。**本仓库是第三方修改版，不是官方发布**；上游代码版权归 BloCamLimb 所有。

> 官方上游并未发布 26.2 版本（其 `master` 仍停在 26.1.2）。本仓库基于上游的 26.2 移植分支（[MOPELotus/ModernUI-MC](https://github.com/MOPELotus/ModernUI-MC) 分支 `26.2`）构建，并额外修复了若干问题、重做了操作中心的公告栏。

---

## 安装

1. 从 [Releases](https://github.com/poposjj/ModernUI-MC-26.2/releases) 下载 `ModernUI-Fabric-26.2-3.13.0.9-universal-fixed.jar`
2. 放进 `.minecraft/mods/`
3. 前置要求：**Minecraft 26.2** · **Fabric Loader ≥ 0.19.3** · **Fabric API ≥ 0.153.0+26.2** · **Java 25**

> 只安装一个 ModernUI jar（mod id 均为 `modernui`，不要与官方 26.1.2 的包同时放）。
> 游戏内按 **Ctrl + K** 打开「ModernUI 操作中心」。

---

## 本修改版改了什么

### 配置读写（`SimpleConfigSpec`）

| 问题 | 修复 |
|---|---|
| 每个 section 首个键的注释被写入两次 | 去掉重复输出 |
| `NaN` / `Infinity` 被当作有效数值 | 非有限值回落默认值 |
| 字符串值不转义换行 / 制表 / 控制字符 | 完整转义与解码（`\n \r \t \b \f \" \\ \uXXXX`） |
| 不支持 TOML 单引号字面量，值内 `#` 被截断 | 同时支持 `"…"` 与 `'…'` |
| 无 section 时写出非法空表头 `[]` | 空 section 不写表头 |

### 稳定性

| 问题 | 修复 |
|---|---|
| 配置目录不可写会中断游戏启动 | 改为告警并回退内存默认值 |
| 文本采样在坐标重写前判定，可能残留抖动 | 判定改用实际绘制原点 |
| `NativeVulkanIntegration` 字段解析失败即类加载期硬崩 | 改为惰性解析 + 可诊断异常（`NativeVulkanIntegration$Offsets`） |
| Vulkan 物理设备特性结构重复分配 | 按设备复用（并注明为何不提前释放） |
| 依赖下限声明过松（`>=0.15.0` / `>=0.90.7`） | 改为与编译目标一致（`>=0.19.3` / `>=0.153.0`） |

### 操作中心「公告栏」

| 问题 | 修复 |
|---|---|
| 主页写死 `Modern UI 3.13.0` 并列出上游 3.13.0 的卖点 | 改为运行时读取版本，显示修改人 / 原作者 / 官方仓库 / 本修改版仓库与下载地址 |
| 更新日志从 `raw.githubusercontent.com` 联网拉取（国内常超时，内容还是上游的） | 改为读取 jar 内置公告（`assets/modernui/changelogs.md`），离线可用 |
| 下载链接指向官方 CurseForge / Modrinth（本移植版并未发布在那里） | 改为指向本仓库 Releases |

除公告栏外，操作中心其余页面（偏好设置、高级选项等）与上游保持一致，未作改动。

---

## 从源码构建

需要 **JDK 25**：

```sh
./gradlew :ModernUI-Fabric:build     # 只构建 Fabric
./gradlew build                       # 构建 Fabric / Forge / NeoForge
```

产物位于 `fabric/build/libs/`。默认构建普通版；MiSans 版使用 `-Pfont_variant=misans`。

---

## 致谢与声明

- 上游项目：**ModernUI** / **ModernUI-MC** by **BloCamLimb**（LGPL-3.0-or-later）
- 26.2 移植基础：[MOPELotus/ModernUI-MC](https://github.com/MOPELotus/ModernUI-MC) 分支 `26.2`
- 本仓库仅做移植维护与缺陷修复，非官方发布；若上游发布 26.2 正式版，请优先使用官方版本。
