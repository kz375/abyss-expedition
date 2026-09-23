# Character animation architecture

Runtime character presentation is deliberately split into three replaceable layers:

```
Skin JSON → Skeleton JSON → Animation Set JSON
```

`characters/warrior-v1.json` is the binding record. It selects a skin, a skeleton and an action set without embedding artwork or timelines. A future Warrior skin therefore changes only the skin file and its layer assets; it keeps `humanoid-v1` and `humanoid-combat-v1`.

## Current Humanoid contract

- Skeleton: `assets/animation/skeletons/humanoid-v1.json`
- Skin: `assets/animation/skins/warrior-iron-vow-v1.json`
- Binding: `assets/animation/characters/warrior-v1.json`
- Actions: `assets/animation/actions/humanoid-combat-v1.json`

The Humanoid skeleton uses `root → pelvis → torso`, two three-bone arms, two three-bone legs, and head. Attachments do not belong to the body skin:

- `hand_R → weapon_socket → weapon`
- `hand_L → shield_socket → shield`

The first reusable action set contains `idle`, `attack_01`, `attack_02`, `break_strike`, `guard`, `perfect_guard`, `hit`, `break`, and `death`.

## Planned families

- `Humanoid`: Warrior, Shadow Assassin and humanoid bosses.
- `Flying`: Cave Bat and airborne enemies.
- `Heavy`: Iron Golem and giant enemies.

New skins must not add animation code. Bind their image/layer data to an existing family skeleton and choose an existing action set; create a new skeleton only when body topology genuinely differs.
