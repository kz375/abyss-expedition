# TypeBasedGame 2.25.0-scenefirst

打破"卡片墙"：场景优先的三页面重构。

## 事件详情：场景优先双栏
- 左 38%：大场景（篝火/废墟/角色剪影）+ 底部标题
- 右 62%：决策条（88px 高）
- 决策条：编号 | 图标 | 标题 | 代价/收益（右）
- 顶部固定 HUD（右上），不嵌在卡里
- 描述只留一句，重复的删掉

## 四选一：横排入口牌
- 4 张窄高牌（300px 高），横排
- 每张：弱化场景背景 + 大图标 + 名字 + 类别
- 无两行描述，Hover 背景放大+图标放大
- 顶部只有 2px 分类色线，不整卡染色

## 遗物：祭坛悬浮式
- 中央祭坛光圈（呼吸动画）
- 3 个遗物悬浮（84px 符印，上下浮动）
- Hover：图标放大 1.3x + 光晕 + 显示详细说明
- 不再是"三个表单选项"

## 统一视觉
- 背景统一深黑绿 #0b0f0c
- 分类色只用于：图标、2px 边框线、Glow、标签

## 文件清单
- web/public/realtime-test/runtime.js（沿用）
- web/public/realtime-test/interface.js（新布局）
- web/public/realtime-test/style.css
- web/public/realtime-test/index.html（?v=2.25.0-scenefirst）
