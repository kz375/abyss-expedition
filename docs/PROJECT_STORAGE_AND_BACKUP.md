# TypeBasedGame 项目储存与备份规则

## 唯一正式工作目录

所有可被游戏引用的源码、美术、角色模型、骨骼、动画、特效和配置，必须保存在：

`/Users/kevinz/IdeaProjects/TypeBasedGame`

游戏代码不得引用 `.codex/generated_images`、系统临时目录、下载目录或绝对用户路径。

## 项目内位置

- 桌面版源码：`src/`
- 桌面版测试：`tests/`
- Web 正式战斗：`web/public/realtime-test/`
- 美术馆：`web/public/art-test/`
- 角色菜单立绘：`web/public/assets/characters/*.png`
- 可绑定战斗 Skin：`web/public/assets/characters/*-rig-v*.png`
- 正式敌人立绘：`web/public/assets/enemies/*.png`
- Character Config：`web/public/assets/animation/characters/`
- Skeleton：`web/public/assets/animation/skeletons/`
- Animation Set：`web/public/assets/animation/actions/`
- Skin 与独立装备：`web/public/assets/animation/skins/`
- 架构和生成记录：`docs/`
- 历史增量包：`updates/`
- 完整源码备份：`backups/`

## 当前角色资源

| 角色 | 菜单立绘 | 战斗 Skin | Character Config | Skeleton |
|---|---|---|---|---|
| Warrior | `warrior.png` | `warrior-rig-v2.png` | `warrior-v1.json` | `humanoid-v1.json` |
| Mage | `mage.png` | `mage-rig-v1.png` | `mage-v1.json` | `humanoid-v1.json` |
| Ranger | `ranger.png` | `ranger-rig-v1.png` | `ranger-v1.json` | `humanoid-v1.json` |
| Paladin | `paladin.png` | `paladin-rig-v1.png` | `paladin-v1.json` | `humanoid-v1.json`（Heavy 兼容层） |
| Necromancer | `necromancer.png` | `necromancer-rig-v1.png` | `necromancer-v1.json` | `humanoid-v1.json` |

## 备份约定

- `updates/` 保存每个版本的独立增量包，旧包不得删除。
- `backups/` 保存可以恢复当前工作状态的完整源码包。
- 完整源码包包含 IDEA 配置、源码、测试、Web 文件、角色资源、文档、工具、存档数据和全部更新包。
- `.git/` 不放入 ZIP；Git 历史由仓库自身保存。
- `build/`、`dist/`、`releases/` 不放入源码备份，它们属于可重新生成的编译产物。

## 当前外部引用审计

2026-09-23 检查结果：正式源码和配置中没有 `.codex/generated_images`、`/var/folders` 或 `file:///` 引用。ImageGen 输出已经复制到项目的 `web/public/assets/characters/` 与 `web/public/assets/enemies/`，游戏不依赖生成临时目录。
