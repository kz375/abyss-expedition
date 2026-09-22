# Abyss Expedition 2D rig specification

Character art, skeletons and actions are separate assets. Replacing a character skin must not require changing action code.

Runtime coordinate rules:

- Canvas: 600 × 900 design units.
- Origin: bottom centre between the feet.
- Positive X points toward the enemy.
- Angles are degrees clockwise.
- Every visual part declares a pivot matching its parent bone.
- Weapons attach to `hand.r`; shields and off-hand props attach to `hand.l`.

Required humanoid parts: `torso`, `head`, paired upper/lower arms, hands, paired thighs/calves and feet. Optional parts such as cape, weapon and effects use sockets and never change the base skeleton.

Common actions are `idle`, `hit` and `death`. Equipment action sets add `slash`, `heavy`, `block`, `cast` or `shoot`.

The initial browser prototype is stored under `web/public/assets/animation/`. It is deliberately a paper-doll rig so joint motion can be tuned before final separated painted skins are produced.
