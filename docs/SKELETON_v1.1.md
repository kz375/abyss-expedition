# humanoid-v1 → v1.1.0（兼容性升级说明）

## 升级原则

只做加法，不动现有结构。16 根骨骼的 `id / parent / pivot` 与 v1.0（2.13.0）
逐字节语义一致；`limits` 沿用 2.13.0 的关节安全范围；`continuity` 契约不变。
旧运行时、旧皮肤、旧动作包无需任何修改即可继续工作。

## 新增字段

- `version`: "1.1.0"
- `sockets` 新增：
  - `belt_socket` → pelvis（腰带 / 药瓶 / 饰品挂点）
  - `pauldron_L` → upperArm_L，`pauldron_R` → upperArm_R（肩甲挂点）
- `meta`：
  - `mirror`: 6 对左右镜像骨骼（动画镜像 / 对称调试用）
  - `lengths`: 每根骨骼长度 = 该关节到父关节 pivot 的实测距离（单位：canvas px）

    | bone | len | bone | len |
    |---|---|---|---|
    | root | 0 | pelvis | 200.0 |
    | torso | 180.0 | head | 170.0 |
    | upperArm_L/R | 96.0 | lowerArm_L | 134.2 |
    | lowerArm_R | 132.0 | hand_L/R | 120.9 |
    | thigh_L/R | 40.0 | shin_L | 105.5 |
    | shin_R | 106.1 | foot_L | 90.1 |
    | foot_R | 90.6 | | |

    （左右手臂/腿的微小不对称是原画 pivot 自带的，已如实记录，未"修正"）
  - `floorY`: 850（= origin y，地面线）
  - `footRestY`: 845（脚底静止高度，来自原 pivot）
  - `facing`: "front"

## 给 2.15.0 的预留

`neck` 骨骼、spine 拆分等多段脊柱结构本次有意不做。
等 Art Lab 里预览验证通过、动作迁移方案定下来再动，避免直接改正式战斗。
