# TypeBasedGame 2.16.0-Moonlit-Guard 增量包

针对 2.15.0 用户反馈的返工：背景重做 + Guard/Perfect Guard 持盾动作重做。

## 版本
- 正式战斗：`?v=2.16.0-moonlit-guard`（9 处引用）
- Art Lab：`?v=0.24.0-moonlit-guard`（5 处引用）

## 包含文件（5 个，按项目相对路径）
1. `web/public/realtime-test/index.html` — 缓存版本提升 + 背景新增月亮/远山/远侧塔楼/中央尖塔元素
2. `web/public/realtime-test/style.css` — 末尾追加 "2.16.0: moonlit battlefield" 背景段落
3. `web/public/art-test/index.html` — 缓存版本提升 + 新增 sc-moon 元素
4. `web/public/art-test/style.css` — 末尾追加 "0.24.0: moonlit lab stage" 背景段落
5. `web/public/assets/animation/actions/humanoid-combat-v2.json` — guard / perfect_guard 姿势重做，其余 7 个动作未动

## 应用方法
把上面 5 个文件按相对路径覆盖到项目对应位置即可。本包内文件已与项目文件一致（直接从项目复制）。

## 改了什么
### 背景（2.16.0 / 0.24.0）
- 月亮（含月面纹理）+ 星点夜空
- 两层远山剪影
- 更大更清晰的冷色古堡：新增远侧双塔、中央尖塔、多组暖色窗光
- 月光边缘、石墙细节、更可辨认的月光石路
- 双层漂移雾更清晰；柱体/链条加冷光边缘
- 抬高被压死的暗部，保留暗黑奇幻基调
- Art Lab 三场景保留各自配色（crypt/rift/forge）

### Guard / Perfect Guard 动作
- 旧问题：父子骨骼累计后左盾侧世界角约 -86°、右剑侧约 -89°，视觉上盾像挂在手腕、剑横飞
- 新持盾：盾臂折叠由前臂支撑盾面，盾面覆盖上胸；身体下沉至稳定前后站姿；剑臂收在可反击位置
- Guard：140ms 进入 → 300–520ms 稳定持盾 → 700ms 放松 → 900ms 回正
- Perfect Guard：90ms 接盾 → 175ms 稳定 → 280ms 反击斩 → 430ms 回收
- duration / events / 其余动作结构不变

## 验证
- JSON parse 通过；keyframes 按时间排序；首帧 0、末帧 = duration
- 所有骨骼存在；所有 rotation 在 skeleton limits 内
- 除 guard、perfect_guard 外其余动作与 2.15.0 完全一致
- CSS braces/parens 平衡；HTML div/span 配对
- 未验证：真实浏览器视觉效果（VM 无头 Chromium 无法截图，需在真实浏览器肉眼确认）
