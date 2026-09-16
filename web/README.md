# 深渊远征 · 网页版

这是调用原版 Java 游戏规则的浏览器客户端。战斗、职业、技能、随机数、种子存档、成就、事件与试炼结算都由现有代码执行，不另写一套简化规则。网页新增代码在 `web/`；原版仅在 `GameSession` 增加可注入的试炼展示入口，默认仍打开 Swing 窗口。

## 本机试玩

在项目根目录运行（JDK 17 或以上）：

```sh
java tools/Web.java
```

浏览器打开 <http://127.0.0.1:8080>。也可运行 `web/start-web.command`（Mac）或 `web/start-web.cmd`（Windows）。保持启动终端运行，关闭服务后网页无法继续连接。本机地址只有本机能访问，不是已上线的公网链接。

网页上的按钮对应游戏原有选项；名字和种子可在下方输入。在游戏主页点击右上角“切换语言”（英文界面为 Language），或选择第 5 项。网页、游戏文字、提示与弹窗统一跟随所选语言，设置保存在档案中。冒险记录按语言分别保留并显示，避免切换后混排旧语言记录。中文可输入“列表”查看存档，原 LIST 指令仍可使用。推格子在原游戏随机事件中进入，支持 WASD、方向键和触屏按钮，不需要回车。规则仍为 8×8、40 步、两个核心；核心被击碎后黄色格会消失。

## 存档与更新

- 每个浏览器使用 HttpOnly 随机 Cookie 找到独立服务器档案，文件默认保存在 `web-data/<随机标识>/`。不会读取或覆盖桌面版 `saves/` 或 Application Support 下的存档。
- 刷新网页保留运行中的同一场游戏。服务重启、暂停或会话超时后，从原版最近检查点恢复，未保存的战斗回合不能保证恢复。
- 输入旧种子会查找当前档案的对应存档。种子本身无法从其他人的档案还原进度。
- “下载存档备份”导出 ZIP；“导入备份”验证后切换到独立档案。失败导入不覆盖当前档案。支持本应用导出的 ZIP；上限 8 MB 压缩、16 MB 解压、256 个文件、单个文件 1 MB。
- 导入前先下载当前档案备份。导入会更换当前浏览器指向的档案，原服务器目录保留，但不会自动提供档案切换界面。
- 清除 Cookie、使用无痕窗口或更换设备会建立新档案，可通过导入备份恢复。当前没有账号系统、跨设备自动同步或管理员账号恢复。
- 代码更新时保留数据目录。网页不安装离线缓存，刷新可获得新资源；服务重启后读取检查点。未来变更存档结构必须另做迁移验证，不能假定任意版本兼容。

## 公网部署

需要支持 Java 17 或 Docker、持久化磁盘和 HTTPS 的服务器。这是 Java 后端，不可只把 HTML 上传到静态网页托管。仓库提供 `web/Dockerfile`，但本开发环境未安装 Docker，镜像尚未实际构建验证。

在项目根目录构建：

```sh
docker build -f web/Dockerfile -t abyss-expedition-web .
docker volume create abyss-web-saves
docker run -d --name abyss-web --restart unless-stopped \
  -p 127.0.0.1:8080:8080 \
  -v abyss-web-saves:/data \
  -e ABYSS_PUBLIC_ORIGIN=https://game.example.com \
  -e ABYSS_SECURE_COOKIE=true \
  abyss-expedition-web
```

`game.example.com` 是占位域名，需要换成实际地址。配置 HTTPS 反向代理转发到本机 8080，保留 Host 请求头。生产启动前必须设置实际 `ABYSS_PUBLIC_ORIGIN`；未设置时 API 只接受 localhost/127.0.0.1 的 Host，避免意外对外暴露。默认不信任客户端提供的转发头。

环境变量：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `ABYSS_HOST` | `127.0.0.1`（容器内 `0.0.0.0`） | 监听地址 |
| `PORT` | `8080` | 监听端口 |
| `ABYSS_WEB_ASSETS` | `web/public` | 网页资源目录 |
| `ABYSS_WEB_DATA` | `web-data` | 必须持久化并备份的玩家数据目录 |
| `ABYSS_PUBLIC_ORIGIN` | 空 | 生产 HTTPS 来源，不带末尾斜杠 |
| `ABYSS_SECURE_COOKIE` | `false` | HTTPS 上线时设为 `true` |
| `ABYSS_MAX_PLAYERS` | `8` | 最大同时保留的运行会话 |

每位玩家一个隔离的 Java 进程，上限 128 MB 堆，另有 JVM 原生内存开销。默认是小规模试玩配置，不是大量并发玩家的生产平台。服务器最多保留 1000 个匿名档案；无访问的运行会话 30 分钟后释放，档案保留。结束游戏的会话在释放前也占一个名额。健康检查为 `/healthz`。

正式公开前，在所选服务器验证容器运行、HTTPS、持久化磁盘、重启恢复、内存和预期并发；配置反向代理请求超时、请求大小上限与访问频率限制。多实例部署还需要固定会话路由；本版本按单个服务实例设计。

当前没有部署到外部服务器，也没有创建公网域名或产生托管费用。

## 验证

```sh
java tools/Test.java
java tools/Web.java test
node --check web/public/app.js
```

第一项是原有桌面/规则回归测试。第二项使用临时目录和真实 HTTP/独立 JVM，覆盖开局、隐藏造物主、战斗、语言隔离、重复提交、刷新、暂停恢复、种子存档、备份导入与试炼通信。测试不会使用真实玩家存档。

可选浏览器验证：先启动本机服务，再传入 Chrome 可执行文件：

```sh
node web/tests/browser-test.mjs '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome' http://127.0.0.1:8080
```

该测试使用独立临时 Chrome 配置，检查真实主页/中文/角色/战斗流程、刷新和 390px 布局，随后用测试棋盘检查网页渲染与键盘/触屏控件；棋盘规则与胜利通信由 Java 集成测试验证。没有声称在真实 iPhone、Android、Safari、Firefox 或公网服务器上完成测试。
