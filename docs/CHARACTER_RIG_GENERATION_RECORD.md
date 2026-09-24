# Character Rig 生成记录

本文件保存角色战斗 Skin 的来源、用途、生成约束和校验值。最终 PNG 均已储存在 IDEA 项目中，原始菜单立绘没有被覆盖。

## 通用生成规范

- 工具：Codex 内置 ImageGen。
- 用途：dark-fantasy 2D skeletal-rig skin。
- 输出：1086×1448、透明 Alpha、完整全身。
- 姿势：正面 A-pose，双臂与躯干分离，双腿分离并落在同一基线。
- 朝向：头部和视线约向屏幕右侧 25–30°，面向敌人。
- 禁止：文字、水印、地面阴影、身体裁切、额外肢体、武器与法术效果。
- 原菜单立绘只作为身份、服装、材质、配色与绘制风格参考。

## Warrior

- 文件：`web/public/assets/characters/warrior-rig-v1.png`
- 同步正式文件：`web/public/assets/characters/warrior-rig-v2.png`
- 最终编辑要求：保留战士 A-pose、盔甲和身体比例，只把头部与目光转向屏幕右侧敌人；透明背景；武器和盾牌保持独立挂点。
- SHA-256：`46b3c3b28da492a3b2a6409e767ee600ac333fdc768a70dca08c0398e4c11176`

## Mage

- 文件：`web/public/assets/characters/mage-rig-v1.png`
- 最终提示摘要：重制原男性深渊法师为全身透明 A-pose，保留面孔、黑发、炭黑长袍、绿色围巾、皮带和奥术饰品；移除法杖、浮空水晶与法术。
- SHA-256：`1e730dcf536d4d124bc631232e6231dbd0dd093e96bd08bc676af83dce6ec0f9`

## Ranger

- 文件：`web/public/assets/characters/ranger-rig-v1.png`
- 最终提示摘要：重制原兜帽游侠为全身透明 A-pose，保留绿色兜帽面罩、旧皮甲、护臂、腰带和潜行轮廓；移除弓、箭和箭袋。
- SHA-256：`d2613ada610b2ae0ba0a1cc5fe0124890f6e040298a12cab3db651d52514a5a7`

## Paladin

- 文件：`web/public/assets/characters/paladin-rig-v1.png`
- 最终提示摘要：重制原重甲圣骑士为全身透明 A-pose，保留闭合头盔、象牙白与黑色旧重甲、太阳纹章、锁甲和布袍；移除盾牌、钉锤与圣光。
- SHA-256：`6e83914311e04d23bc533d57a48f0f9bc393bd480e9b8871164aef7cf62c8127`

## Necromancer

- 文件：`web/public/assets/characters/necromancer-rig-v1.png`
- 最终提示摘要：重制原死灵法师为全身透明 A-pose，保留苍白面孔、骨鸟头饰、骨质肩饰、黑色旧长袍、仪式封印与腰带；移除法杖、灯笼、灵魂火和召唤手。
- SHA-256：`d35f45ebf2e4ff9a9643c0beff8b5905dcb39501a25afbcaf5deb68c3754e448`

## 独立装备

- Warrior Greatsword：`web/public/assets/animation/skins/warrior-greatsword-v2.png`
- Warrior Shield：`web/public/assets/animation/skins/warrior-shield-v2.png`
- 挂点：`hand_R → weapon_socket`、`hand_L → shield_socket`

以后生成新版角色时必须使用新版本文件名，例如 `mage-rig-v2.png`；确认迁移完成前不得覆盖或删除旧 Skin。

## 正式敌人立绘（2.4.2）

- 生成工具：Codex 内置 ImageGen。
- 统一提示规范：dark-fantasy PC game production cutout、写实厚涂 3D 概念质感、透明 Alpha、完整轮廓、面向屏幕左侧玩家、暖橙色深渊边缘光；禁止场景、UI、文字、水印、投影和裁切。
- Cave Bat：破损皮膜巨翼、黑色毛皮、骨质甲片、琥珀眼和伸展利爪；`web/public/assets/enemies/cave-bat-v1.png`；SHA-256 `d72532319793b9af3b825fd1561d3a6ff9b16fb4fe2cdb1e8b2e2f4380c1ffc1`。
- Shadow Assassin：兜帽黑革与旧钢甲、双弯刃、低身前冲姿势、克制紫色深渊辉光；`web/public/assets/enemies/shadow-assassin-v1.png`；SHA-256 `e9e672aae27e504444d74b2d80c4fafbd43301e07d60cfce0d09e8076cf86012`。
- Abyss Lord：黑曜石裂甲、双角、巨型处刑剑、深红胸腔核心和破损仪式披风；`web/public/assets/enemies/abyss-lord-v1.png`；SHA-256 `356e3fafea8395331829a04c9fba75304fdfb0b218b8d6b8ccaed0bda9b15fe8`。
- Iron Golem 沿用已有正式素材；四类目标全部由 `art-test/actors.js` 数据绑定，正式战斗不再使用剪影。
