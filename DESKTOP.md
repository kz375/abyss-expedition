# 电脑版：macOS / Windows / Linux

## 两种运行方式

- **独立桌面版**：运行 `abyss.desktop.DesktopLauncher`，或构建后运行 `dist/AbyssExpedition.jar`。使用独立窗口、输入框及确定按钮，不依赖终端中文字库和终端输入编码。菜单仍使用数字和回车，玩法不变。
- **IDEA / 终端版**：原有 `gameBody` 入口保留，原存档位置不变。

桌面窗口有字号选择、可滚动输出和关闭确认。输出记录有长度上限，仅影响屏幕历史，不影响游戏存档。关闭窗口只保证恢复最后成功保存的检查点，不是战斗中途即时保存。
推格子仍用 WASD / 方向键，窗口根据显示器可用空间调整；系统负责 HiDPI 缩放。特别小的屏幕会缩小画面，不能保证文字在任意分辨率都清晰。
中文采用 Java 逻辑字体回退。Linux 精简系统如果缺少中文字库，需安装系统的 CJK 字体；不能保证无字体的系统显示中文。

## 构建（开发者需要 JDK 17+，不需要 IDEA）

在项目根目录执行：

```sh
java tools/Build.java
java tools/Test.java
java tools/Package.java
```

第一条生成通用 `dist/AbyssExpedition.jar` 及三种启动脚本。运行该 JAR 的电脑需要 Java 17 或更新的桌面运行环境。
`Test.java` 运行隔离的回归与兼容测试。`Package.java` 使用本机 `jpackage`，生成 `releases/desktop-*/` 下带 Java 运行时的应用目录。玩家无需另装 Java。必须分发整个生成的应用目录，不能只复制其中的启动文件。

已提供 `.github/workflows/desktop.yml`，把项目放入 GitHub 后可在三种系统上自动测试和构建。本地添加此文件不会自动触发远程任务；当前没有上传项目或实际运行该工作流。下载 CI 产物后，macOS/Linux 启动文件可能需要恢复执行权限。

macOS 生成 `.app`，Windows 生成带 `.exe` 的目录，Linux 生成应用目录。**必须在相应系统、相应 CPU 架构的 JDK 上分别构建**；macOS 构建出的包不是 Windows/Linux 安装包。未配置开发者签名、公证或系统安装器。
应用包必须遵守其构建 JDK 支持的系统版本；不承诺 Windows XP/7、所有旧 macOS 或所有 Linux 发行版。ChromeOS、Android 桌面不在原生保证范围内。

## 桌面版存档

| 系统 | 默认目录 |
| --- | --- |
| macOS | `~/Library/Application Support/AbyssExpedition` |
| Windows | `%APPDATA%\AbyssExpedition`（缺少变量时使用用户目录的 `AppData/Roaming`） |
| Linux | `$XDG_DATA_HOME/abyss-expedition`，未设或不是绝对路径时使用 `~/.local/share/abyss-expedition` |

可通过 `java -Dabyss.saveDir="自选目录" -jar dist/AbyssExpedition.jar` 指定位置。桌面窗口启动时也显示实际目录。
首次运行桌面版时，如果工作目录存在旧 `saves` 且目标目录尚不存在，会先复制旧档，保留原文件；不会合并或覆盖已存在的目标目录。符号链接或复制失败会停止启动，原文件保持不变。
从 IDEA 迁移最直接的方法是在项目根目录执行上面的 JAR 命令；双击发行目录的脚本时工作目录不同，可能无法自动发现项目中的旧档。也可显式指定原 `saves` 的绝对路径。不要在迁移期间运行另一份旧版游戏。
跨电脑复制完整存档目录，包括种子文件、成就和语言设置。种子数字本身不包含进度。

## 验收范围

本轮本机结果：JDK 17 目标编译、311 项游戏回归断言、87 项桌面兼容断言通过；使用生成的 macOS 应用内置运行时重复执行同样的 398 项断言也通过。通用 JAR 无图形环境的退出提示已验证。
macOS Apple Silicon 成品位于每次 `Package.java` 输出的新 `releases/desktop-*/AbyssExpedition.app` 目录，约 76 MB；应使用最新构建，不要继续打开历史目录里的旧版应用。Windows/Linux 原生应用包尚未在对应系统构建或验收；也未执行本机 GUI 的逐项鼠标、输入法操作验收。

主页入口修订后：读取存档固定为选项 2，无当前存档时也显示；语言固定为选项 5。此次验证 316 项回归断言及 87 项桌面断言通过。

自动化检查覆盖：原有回归测试、桌面输入的 UTF-8/空行/EOF、跨系统路径计算、小屏幕尺寸、旧存档复制及不覆盖、通用 JAR 构建。
macOS/Windows/Linux 的图形桌面、输入法、双显示器和系统安全提示仍需逐平台人工验收。生成应用包不等于已完成所有设备测试。
此前发现的存档异常恢复风险不属于本轮平台适配修复范围。

平台打包限制依据：[Oracle jpackage 文档](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-overview.html)。字体回退依据：[Java 逻辑字体说明](https://docs.oracle.com/javase/tutorial/2d/text/fonts.html)。
