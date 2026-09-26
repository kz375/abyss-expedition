# 2.14.0 Skeleton / Scenery / UI — Release Notes

版本：2.14.0（增量更新，基于 2.13.0 Paladin Action Pack）

## 改了什么

**骨骼：humanoid-v1 → v1.1.0（纯兼容升级）**
- 16 根骨骼的 id / parent / pivot 与 2.13.0 一字不差；limits（关节安全范围）沿用 2.13.0 调好的值；continuity 契约不变
- 新增 `version` 字段；新增 `meta`：左右镜像对、16 根骨骼长度（由 pivot 实测）、地面高度 floorY=850 / 脚底静止高度 footRestY=845
- sockets 从 4 个扩展到 7 个：新增 `belt_socket`（腰带/药瓶挂点）、`pauldron_L/R`（肩甲挂点），原有 weapon/shield/back/head_fx 四个 socket 不变
- 本次没有加 neck、没有拆分 spine（留到 2.15.0 在 Art Lab 预览验证后再动）

**战斗背景（只改 style.css，没动 DOM）**
- 远景雾带（`.abyss-scenery:after`）+ 近地面雾（`.foreground-rocks:before`），两层反向缓慢漂移，纵深感
- 废墟柱顶加火把光点（呼吸闪烁），城堡像有人住
- 路面上缘加暖色磨损高光，人物站位更实
- 删除已废弃的 `.abyss-gate` 相关 CSS（该元素早就不在 DOM 里，之前靠 display:none 藏着）

**UI**
- Intent 倒计时：armed 状态下数字跳动；剩余 < 1.2 秒进入 urgent 红色警示
- 技能图标按效果家族着色（holy/arcane/fire/ice/poison/soul/soulfire/reality/physical），读技能更快
- 查过全部 6 职业 21 个技能类型，skillIcon 映射无缺口，本次没动
- 护盾保持淡蓝 overlay 盖在 HP 条上（已符合要求），没动

**缓存版本**：index.html 里 9 处 `?v=2.13.0-paladin-rig` → `?v=2.14.0-skeleton-scenery-ui`

## 兼容性

- 运行时只读 bones / limits / sockets / continuity，新增字段旧代码自动忽略
- 6 角色皮肤、现有全部动作包无需任何修改

## 文件清单（本包只含这 4 个修改过的文件）

- web/public/assets/animation/skeletons/humanoid-v1.json
- web/public/realtime-test/style.css
- web/public/realtime-test/interface.js
- web/public/realtime-test/index.html

## 检查情况（诚实版）

- 通过：JSON 解析 + 骨骼一致性校验（bones/limits 与 2.13.0 完全一致）、interface.js 的 node 语法检查、CSS 大括号配平、无伪元素冲突检查
- 没跑：完整游戏规则测试、Web/HTTP 测试、真实浏览器测试、桌面回归测试——这台工作机跑不了 Mac 上的那套，需要你在 IDEA / Mac 上按老流程跑一遍再发布
