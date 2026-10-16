# Modern UI 26.2 修改版

**修改人：oioi**　|　**原作者：BloCamLimb**

- 上游官方仓库：[BloCamLimb/ModernUI-MC](https://github.com/BloCamLimb/ModernUI-MC)
- 本修改版仓库：[poposjj/ModernUI-MC-26.2](https://github.com/poposjj/ModernUI-MC-26.2)
- 下载地址：[GitHub Releases](https://github.com/poposjj/ModernUI-MC-26.2/releases)

---

本包基于上游 ModernUI 3.13.0.x 的 26.2 移植分支构建，属**第三方修改版（非官方发布）**。上游代码版权归 BloCamLimb 所有，遵循 LGPL-3.0-or-later。

## 本次修复

- 配置文件：section 首个键的注释被重复写入
- 配置文件：`NaN` / `Infinity` 不再被当作有效数值接受
- 配置文件：字符串值正确转义换行、制表符与控制字符（读取侧同步还原）
- 配置文件：支持 TOML 单引号字面量，值中的 `#` 不再被截断
- 配置文件：无 section 时不再写出非法空表头 `[]`
- 配置目录不可写时不再打断游戏启动，回退内存默认值
- 文本采样：NEAREST / LINEAR 判定改用实际绘制原点
- Vulkan：字段解析失败改为可诊断异常（不再在类加载期硬崩）；物理设备特性结构按设备复用

## 公告栏

- 主页版本号改为运行时读取，不再写死
- 更新日志改为读取 jar 内置文本，离线可用，不再依赖 GitHub 拉取
- 移除指向官方 CurseForge / Modrinth 的下载链接（本移植版未发布在那里）
