# ModernUI for Minecraft 26.2

**维护者**：oioi
**上游项目**：ModernUI / ModernUI-MC，作者 BloCamLimb（许可：LGPL-3.0-or-later）
**构建基线**：MOPELotus/ModernUI-MC 分支 `26.2`
**当前版本**：`3.13.0.9`
**性质说明**：本仓库为第三方维护的 Minecraft 26.2 移植与修订版本，非上游官方发布。

---

## 一、项目说明

上游项目未发布 Minecraft 26.2 版本（其 `master` 分支仍停留在 26.1.2）。本仓库基于上游的 26.2 移植分支构建，并在此基础上修复若干缺陷、重做游戏内操作中心的公告区。

- 支持加载器：Fabric（持续集成同时产出 Forge、NeoForge 构建）
- 下载地址：请见本仓库 Releases 页面
- 问题反馈：请见本仓库 Issues 页面

---

## 二、运行环境要求

以下要求取自各构建文件自身的元数据声明。

| 构建 | 运行环境要求 |
| --- | --- |
| Fabric | Minecraft 26.2（`>=26.2 <26.3`）；Fabric Loader ≥ 0.19.3；Fabric API ≥ 0.153.0+26.2；Java 25；无其它必需前置模组 |
| Forge | Minecraft 26.2；Forge ≥ 65.0.0；无其它必需前置模组 |
| NeoForge | Minecraft 26.2；NeoForge ≥ 26.2.0.0-beta；无其它必需前置模组 |

Fabric 构建已以内置配置实现取代 Forge Config API Port，无需安装额外配置库。

---

## 三、安装

1. 按加载器在 Releases 页面下载对应文件，放入 `.minecraft/mods/`
2. 确认运行环境满足第二节要求
3. 启动客户端，按 `Ctrl + K` 打开「ModernUI 操作中心」

注意事项：

- 同一实例仅可安装一个 ModernUI（模组 ID 均为 `modernui`），不可与官方 26.1.2 构建同时安装
- Fabric 普通版当前校验值：SHA256 `DB6B82A1E854F650F9C3FF881E822D464E07D0E1E341840E8E6EE9693799353A`（可用 `certutil -hashfile <jar> SHA256` 自行比对）

---

## 四、变更内容

配置读写

- 修正配置文件分节（section）内首个键的注释被重复写入的问题
- `NaN` / `Infinity` 不再被视为有效数值，改为回落默认值
- 字符串值完整转义换行、制表符与控制字符，并在读取时正确还原
- 支持 TOML 单引号字面量，值中包含 `#` 时不再被截断
- 无分节配置不再写出非法的空表头 `[]`

稳定性

- 配置目录不可写时不再中断游戏启动，改为记录告警并回退至内存默认值
- 文本采样（NEAREST / LINEAR）判定改用实际绘制原点
- Vulkan 集成字段解析失败改为可诊断异常，避免类加载阶段中断；物理设备特性结构按设备复用

元数据

- 依赖下限调整为与编译目标一致：`fabricloader >= 0.19.3`、`fabric-api >= 0.153.0`

操作中心（ModernUI Center）

- 主页版本号改为运行时读取；公告区展示维护者、原作者、上游仓库与本仓库下载地址
- 更新日志改为读取构建内置文本，离线可用
- 下载入口指向本仓库发布页，不再指向未收录本移植版本的官方 CurseForge / Modrinth 页面
- 除公告区外，操作中心其余页面与上游保持一致，未作改动

---

## 五、构建产物与验证状态

| 文件 | 验证状态 | 说明 |
| --- | --- | --- |
| `ModernUI-Fabric-26.2-3.13.0.9-universal-fixed.jar` | 已验证 | Fabric 普通版。已在 Minecraft 26.2 / Fabric Loader 0.20.0 / Fabric API 0.161.0+26.2 / Java 25 / OpenGL 环境完成启动、世界加载、文本渲染与配置读写的运行验证 |
| `ModernUI-Fabric-26.2-3.13.0.9-universal.jar` | 已验证（等价构建） | 由持续集成从同一源码构建。与上一文件逐条目比对：1845 个 class 文件完全一致，仅 `MANIFEST.MF` 构建时间戳不同。两者择一安装即可 |
| `ModernUI-Fabric-26.2-3.13.0.9-universal-misans.jar` | 未验证 | Fabric 的 MiSans 字体变体。构建通过，未执行运行验证；字体资源需在客户端内下载 |
| `ModernUI-Forge-26.2-3.13.0.9-universal.jar`<br>`ModernUI-Forge-26.2-3.13.0.9-universal-misans.jar` | 未验证 | Forge 构建。构建通过，未执行运行验证 |
| `ModernUI-NeoForge-26.2-3.13.0.9-universal.jar`<br>`ModernUI-NeoForge-26.2-3.13.0.9-universal-misans.jar` | 未验证 | NeoForge 构建。构建通过，未执行运行验证 |

上表「未验证」指未执行运行时测试，不代表已知存在缺陷。除 Fabric 普通版外，其余文件均为持续集成自动构建产物。

---

## 六、从源码构建

需要 JDK 25。

```sh
./gradlew :ModernUI-Fabric:build     # 仅构建 Fabric
./gradlew build                       # 构建 Fabric / Forge / NeoForge
```

产物位于 `fabric/build/libs/`。默认构建普通版；MiSans 变体使用 `-Pfont_variant=misans`。

---

## 七、已知限制

- Minecraft 26.x 的原生 Vulkan 渲染路径未在 26.2 上完成运行验证；使用默认 OpenGL 后端不受影响
- 构建内声明与 `blur`、`tipthescales` 冲突
- 同时安装 `Remove Reloading Screen` 且其配置为 `hideOverlays = "ALL"` 时可能发生崩溃，建议改为 `"RELOADING"`

---

## 八、许可与致谢

- 上游项目：ModernUI / ModernUI-MC，作者 BloCamLimb，许可 LGPL-3.0-or-later
- 26.2 移植基线：MOPELotus/ModernUI-MC 分支 `26.2`
- 本仓库仅进行移植维护与缺陷修复；若上游发布 26.2 正式版本，建议优先使用官方版本
