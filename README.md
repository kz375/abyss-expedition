# TypeBasedGame 2.15.0-Guard-Stage

增量更新包：防御动作重做 + 骨骼数据修复 + Art Lab 舞台风景层。
正式战斗页版本：`2.15.0-guard-stage`；Art Lab 版本：`0.23.0-guard-stage`。

## 改了什么

1. **Guard / Perfect Guard 重做**（`assets/animation/actions/humanoid-combat-v2.json`）
   - Guard：骨盆放平、上身只轻微后收；下沉靠双腿反向屈膝而不是侧倒；盾手抬到躯干前方，剑手收在体侧；持盾持续姿势双脚高低差约 1–2px。
   - Perfect Guard：90ms 明确顶盾接触；280ms 改为清晰的斜向反击挥砍（不再是原来的挠头轨迹）；收势回正。
   - 时长、事件点（windup/impact/counter）、hold 语义不变，其他动作未动。
2. **骨骼 meta.lengths 修复**（`assets/animation/skeletons/humanoid-v1.json`，版本 1.1.0 → 1.1.1）
   - 上臂/前臂、大腿/小腿的长度值之前写反了（值错位了一格），已按骨骼 pivot 间距修正；8 段肢体长度现与 pivot 距离完全一致。
   - 16 根骨骼、sockets、limits、floorY/footRestY 未动。运行时目前不读取 meta.lengths，属数据勘误，零风险。
3. **动作 JSON 缓存击穿**（`art-test/rig.js`、`realtime-test/character-rig.js`）
   - 两个加载器之前直接 fetch JSON，不带版本参数，改动作后浏览器可能继续用旧缓存。
   - 现在自动读取自身 script 标签上的 `?v=` 并追加到角色/骨骼/动作/位移/utility/action-pack 的 JSON 请求上；无 `?v=` 时行为与原来一致。
4. **Art Lab 舞台风景层**（`art-test/index.html`、`art-test/style.css`）
   - 舞台不再只有渐变色块：新增模糊古堡（主楼+双塔+城墙+亮窗，带光晕）、左右断柱、顶部垂落锁链（微摆）、双层反向漂移的地面雾。
   - 三个场景（墓穴/裂隙/熔炉）各有配色；舞台底色略提亮，仍保持暗黑氛围。
   - 战斗单位层级在风景之上；`prefers-reduced-motion` 下动画自动停。
5. **缓存版本串**
   - `realtime-test/index.html`：9 处 `?v=` → `2.15.0-guard-stage`
   - `art-test/index.html`：5 处 `?v=` → `0.23.0-guard-stage`

## 包内文件（项目相对路径）

- web/public/assets/animation/actions/humanoid-combat-v2.json
- web/public/assets/animation/skeletons/humanoid-v1.json
- web/public/art-test/rig.js
- web/public/realtime-test/character-rig.js
- web/public/art-test/style.css
- web/public/art-test/index.html
- web/public/realtime-test/index.html

## 测试

- JSON 解析通过；guard/perfect_guard 关键帧有序、首帧 0、尾帧=duration、全部骨骼存在、全部旋转在 skeleton limits 内。
- 8 段肢体 lengths 与 pivot 间距逐项核对一致。
- rig.js / character-rig.js `node --check` 通过；缓存 URL 生成逻辑单测通过（有 ?v= 则追加，无则保持原样）。
- art-test/style.css 括号配平，风景层 class 全部存在。
- 骨架离线渲染确认新 guard/perfect_guard 姿势（候选=落盘一致）。
- 未做真实浏览器视觉验证：本机 headless Chromium 无法输出页面（已知环境限制），请在真实浏览器里看 Art Lab 舞台和 Guard/Perfect Guard 按钮效果。

## 部署注意

- 均为已存在文件的修改，无新增静态资源路径，allowlist 无需改动。
- 与 2.14.2-intent-fix 包无文件冲突，可直接覆盖。
