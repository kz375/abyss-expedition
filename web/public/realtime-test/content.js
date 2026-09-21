"use strict";
// Content IDs, not translated labels, are persisted in Beta saves.
const DIFFICULTIES = {
  explorer: {name:["探索者","Explorer"], hp:.85, attack:.85, hero:1, gold:.85},
  adventurer: {name:["冒险者","Adventurer"], hp:1, attack:1, hero:1, gold:1},
  nightmare: {name:["噩梦","Nightmare"], hp:1.3, attack:1.2, hero:1, gold:1.5},
  ultra: {name:["超级噩梦","Ultra Nightmare"], hp:4, attack:2.4, hero:3.7, gold:2}
};
const SKILL_NAMES = [
  ["横扫","Cleave"],["破甲斩","Armor Break"],["铁壁","Iron Wall"],["处决重斩","Execution Blow"],
  ["奥术箭","Arcane Bolt"],["奥术爆发","Arcane Burst"],["秘法屏障","Arcane Barrier"],["时间回响","Temporal Echo"],
  ["速射","Quick Shot"],["双重射击","Double Shot"],["毒牙箭","Venom Arrow"],["烟幕","Smoke Screen"],
  ["圣光挥击","Holy Strike"],["神圣审判","Judgment"],["守护祷言","Prayer"],["圣光净化","Radiant Purge"],
  ["灵魂火","Soul Fire"],["灵魂汲取","Soul Drain"],["骸骨护甲","Bone Armor"],["枯萎","Wither"],
  ["现实切割","Reality Cut"],["现实撕裂","Reality Rend"],["造物屏障","Creation Ward"],["规则重写","Rewrite Rules"]
];
const RELICS = [
  {id:"blade",name:["战栗之刃","Tremor Blade"],desc:["伤害 +12%","Damage +12%"],power:1.12},
  {id:"vital",name:["活力结晶","Vital Crystal"],desc:["生命上限 +26，恢复 26","Max health +26; heal 26"],max:26},
  {id:"moon",name:["月影护符","Moon Charm"],desc:["每场开局护盾 +55","Start each battle with 55 ward"],ward:55},
  {id:"haste",name:["迅捷刻印","Swift Sigil"],desc:["冷却 -10%；总减免最多 40%","Cooldown -10%; total reduction capped at 40%"],cooldown:.9},
  {id:"flame",name:["炽焰核心","Flame Core"],desc:["灼烧伤害 +50%","Burn damage +50%"],burn:1.5},
  {id:"plunder",name:["掠夺者印记","Raider Mark"],desc:["立即获得 35 金币","Gain 35 gold once"],gold:35},
  {id:"thorns",name:["荆棘冠冕","Thorn Crown"],desc:["反弹实际失血的 12%","Reflect 12% of health damage taken"],thorns:.12},
  {id:"resolve",name:["不屈徽章","Resolve Emblem"],desc:["生命低于 35% 时伤害 +20%","Damage +20% below 35% health"],low:.2},
  {id:"sand",name:["时砂","Time Sand"],desc:["每场开局重置冷却","Reset cooldowns at battle start"],reset:true},
  {id:"healing",name:["愈合符文","Healing Rune"],desc:["治疗效果 +35%","Healing +35%"],healing:1.35},
  {id:"ruby",name:["血红宝石","Blood Ruby"],desc:["攻击 +5","Attack +5"],attack:5},
  {id:"iron",name:["钢铁印记","Iron Sigil"],desc:["防御 +4","Defense +4"],defense:4},
  {id:"vial",name:["月之瓶","Moon Vial"],desc:["生命上限 +30，恢复 30","Max health +30; heal 30"],max:30},
  {id:"fang",name:["吸血獠牙","Vampire Fang"],desc:["实际伤害的 15% 转为治疗","Heal 15% of actual damage dealt"],leech:.15},
  {id:"mail",name:["荆棘甲","Thorn Mail"],desc:["反弹实际失血的 20%","Reflect 20% of health damage taken"],thorns:.2},
  {id:"clover",name:["幸运四叶草","Lucky Clover"],desc:["暴击率 +10%","Critical chance +10%"],crit:.1},
  {id:"gourd",name:["炼金葫芦","Alchemist Gourd"],desc:["药水治疗 +60%","Potion healing +60%"],potion:1.6},
  {id:"gold",name:["黄金神像","Gold Idol"],desc:["战斗与事件金币 +30%","Battle and event gold +30%"],goldBonus:.3},
  {id:"standard",name:["战旗","Battle Standard"],desc:["每场开局护盾 +30","Start each battle with 30 ward"],ward:30},
  {id:"chrono",name:["时光护符","Chrono Charm"],desc:["实时适配：冷却 -15%；总减免最多 40%","Real-time conversion: cooldown -15%; total capped at 40%"],cooldown:.85},
  {id:"phoenix",name:["凤凰羽毛","Phoenix Feather"],desc:["每场复活一次，恢复 40% 生命","Revive once per battle at 40% health"],phoenix:true},
  {id:"mask",name:["狂战士面具","Berserker Mask"],desc:["伤害 +12%","Damage +12%"],power:1.12},
  {id:"serpent",name:["蛇之戒","Serpent Ring"],desc:["普攻附加 3 秒中毒","Basic attacks apply 3 seconds of poison"],poison:true}
];
const RELIC_BY_ID=Object.fromEntries(RELICS.map(r=>[r.id,r]));
// Account equipment persists outside expeditions. Each new run snapshots these
// three slots so warehouse changes cannot alter an active battle.
const ACCOUNT_ITEMS=[
  {id:"iron_blade",slot:"weapon",name:["远征铁刃","Expedition Ironblade"],desc:["攻击 +4","Attack +4"],attack:4},
  {id:"ember_staff",slot:"weapon",name:["余烬法杖","Ember Staff"],desc:["灼烧伤害 +25%","Burn damage +25%"],burn:1.25},
  {id:"hunter_bow",slot:"weapon",name:["猎渊长弓","Abyss Hunter Bow"],desc:["暴击率 +8%","Critical chance +8%"],crit:.08},
  {id:"bastion_plate",slot:"armor",name:["堡垒胸甲","Bastion Plate"],desc:["生命上限 +18，防御 +2","Max health +18; defense +2"],max:18,defense:2},
  {id:"bone_mail",slot:"armor",name:["白骨锁甲","Bone Mail"],desc:["每场战斗获得 24 护盾","Gain 24 ward each battle"],ward:24},
  {id:"mist_cloak",slot:"armor",name:["雾行斗篷","Mistwalker Cloak"],desc:["技能冷却 -6%","Skill cooldown -6%"],cooldown:.94},
  {id:"blood_charm",slot:"charm",name:["血契护符","Blood-Pact Charm"],desc:["实际伤害的 5% 转为生命","Heal 5% of actual damage"],leech:.05},
  {id:"lucky_coin",slot:"charm",name:["归途金币","Homebound Coin"],desc:["金币收益 +15%","Gold gain +15%"],goldBonus:.15},
  {id:"hourglass",slot:"charm",name:["裂隙沙漏","Rift Hourglass"],desc:["技能冷却 -5%","Skill cooldown -5%"],cooldown:.95}
];
const ACCOUNT_ITEM_BY_ID=Object.fromEntries(ACCOUNT_ITEMS.map(item=>[item.id,item]));
// Each species has its own rotation, tempo and counterplay, also used by summons.
const MONSTER_MODULES = [
  {moves:["bite","double"],speed:1.15,weakness:["蓄力时眩晕","Stun during wind-up"]},
  {moves:["rage","bite"],speed:1.05,weakness:["狂暴时用护盾","Ward through frenzy"]},
  {moves:["sunder","charge"],speed:.8,weakness:["打断重击","Interrupt the heavy strike"]},
  {moves:["poison","shield"],speed:.8,weakness:["净化毒素","Cleanse poison"]},
  {moves:["double","poison"],speed:1.25,weakness:["低血量时爆发","Burst before poison stacks"]},
  {moves:["counter","double"],speed:1.2,weakness:["反击姿态时停止直伤","Avoid direct hits during counter stance"]},
  {moves:["weak","dispel"],speed:.9,weakness:["驱散后再加盾","Ward after its dispel"]},
  {moves:["burn","heal"],speed:.85,weakness:["打断治疗","Interrupt healing"]},
  {moves:["drain","poison"],speed:.9,weakness:["护盾阻止吸血","Ward prevents life steal"]},
  {moves:["counter","shield"],speed:1,weakness:["用持续伤害绕过反击","Damage over time bypasses counters"]},
  {moves:["dispel","charge"],speed:1.15,weakness:["留眩晕阻止蓄力","Save a stun for the charge"]},
  {moves:["sweep","double"],speed:.95,weakness:["横扫前减伤","Defend before its sweep"]},
  {moves:["weak","curse"],speed:1.1,weakness:["保留净化","Save a cleanse"]},
  {moves:["poison","burn"],speed:.9,weakness:["连续净化与治疗","Cleanse and sustain"]},
  {moves:["shield","sunder"],speed:.7,weakness:["护盾衰减后爆发","Burst after ward decay"]},
  {moves:["burn","rage","charge"],speed:1.05,weakness:["打断狂暴蓄力","Interrupt the enraged charge"]},
  {moves:["summon","curse","dispel"],speed:.85,weakness:["先清理召唤物","Clear summoned enemies first"]},
  {moves:["poison","drain","double"],speed:1.2,weakness:["净化后压制治疗","Cleanse and deny healing"]},
  {moves:["heal","sweep","rage"],speed:.75,weakness:["打断自疗","Interrupt self-healing"]},
  {moves:["shield","charge"],speed:.85,weakness:["优先清护卫，打断蓄力","Clear guardians, then interrupt charges"]},
  {moves:["burn","nova"],speed:1,weakness:["为第三阶段保留净化","Save cleansing for phase three"]}
];
const ACTION_NAMES={bite:["撕咬","Bite"],double:["连击","Combo"],rage:["狂暴","Frenzy"],sunder:["破甲","Sunder"],charge:["蓄力重击","Charged Strike"],poison:["毒雾","Venom"],shield:["屏障","Barrier"],counter:["反击姿态","Counter Stance"],weak:["虚弱诅咒","Weakening Hex"],dispel:["驱散","Dispel"],burn:["烈焰","Flame"],heal:["复苏","Recovery"],drain:["汲取","Drain"],sweep:["横扫","Sweep"],curse:["侵蚀","Corruption"],summon:["召唤","Summon"],nova:["深渊新星","Abyssal Nova"]};
const EVENT_CATALOG=[
  ["camp","篝火","Campfire"],["shop","商店","Merchant"],["shrine","古老神龛","Ancient Shrine"],["chest","上锁宝箱","Locked Chest"],
  ["healer","流浪医师","Wandering Healer"],["spring","神秘清泉","Mystic Spring"],["adventurer","受困冒险者","Trapped Adventurer"],
  ["gambler","骰子赌徒","Dice Gambler"],["library","古老图书馆","Ancient Library"],["well","低语之井","Whispering Well"],
  ["cards","纸牌骗子","Card Sharp"],["oracle","预言家","Oracle"],["curator","遗物收藏家","Relic Curator"],["rift","裂隙之门","Rift Gate"],
  ["forge","遗忘熔炉","Forgotten Forge"],["altar","回声祭坛","Echoing Altar"],["caravan","月光商队","Moonlit Caravan"],
  ["idol","饥饿神像","Starved Idol"],["stalker","神秘追踪者","Mysterious Stalker"],["trial","双核试炼","Trial of Twin Cores"]
];
const SHOP=[
  {id:"potion",name:["药瓶","Healing Bottle"],price:18}, {id:"weapon",name:["磨刀石","Whetstone"],price:32},
  {id:"armor",name:["锁子甲","Chainmail"],price:35}, {id:"ward",name:["屏障","Barrier"],price:24},
  {id:"tonic",name:["活力药剂","Vitality Tonic"],price:38}, {id:"smoke",name:["烟幕弹","Smoke Bomb"],price:28}
];
