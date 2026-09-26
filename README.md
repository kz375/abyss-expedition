# TypeBasedGame 2.14.1 — Abyss Night

背景重做增量包：整体压暗 2~3 档，暖光冷却/压暗，雾从「亮雾」改成「暗雾」。

## 包含文件（保持项目相对路径）

- `web/public/realtime-test/style.css` —— 末尾追加 `2.14.1: abyss night` 覆盖段
- `web/public/realtime-test/index.html` —— 9 处 `?v=` 升到 `2.14.1-abyss-night`

## 说明

- 纯表现层修改，不碰战斗数值与逻辑。
- 旧版 CSS 规则全部保留，新段在文件末尾以同选择器覆盖，方便回滚：删掉末尾段即回到 2.14.0 效果。
- 详见 `updates/RELEASE_NOTES_2.14.1.md`。
