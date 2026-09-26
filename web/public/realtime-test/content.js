"use strict";
// Content IDs, not translated labels, are persisted in Beta saves.
const DIFFICULTIES = {
  explorer: {name:["探索者","Explorer"], hp:.92, attack:.9, speed:.96, hero:1, gold:.85},
  adventurer: {name:["冒险者","Adventurer"], hp:1.18, attack:1.12, speed:1.03, hero:1, gold:1},
  nightmare: {name:["噩梦","Nightmare"], hp:1.58, attack:1.42, speed:1.1, hero:1, gold:1.5},
  ultra: {name:["超级噩梦","Ultra Nightmare"], hp:4.8, attack:3.15, speed:1.16, hero:3.7, gold:2}
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
// Account loot is generated once at extraction. Difficulty controls quantity,
// quality, affix count and roll strength; stored rolls never reroll on reload.
const GEAR_BASES=[
  {id:"iron_blade",slot:"weapon",name:["远征铁刃","Expedition Ironblade"],main:"attack",value:4},
  {id:"ember_staff",slot:"weapon",name:["余烬法杖","Ember Staff"],main:"burnBonus",value:.18},
  {id:"hunter_bow",slot:"weapon",name:["猎渊长弓","Abyss Hunter Bow"],main:"crit",value:.055},
  {id:"bastion_plate",slot:"armor",name:["堡垒胸甲","Bastion Plate"],main:"max",value:18},
  {id:"bone_mail",slot:"armor",name:["白骨锁甲","Bone Mail"],main:"ward",value:22},
  {id:"mist_cloak",slot:"armor",name:["雾行斗篷","Mistwalker Cloak"],main:"defense",value:2},
  {id:"blood_charm",slot:"charm",name:["血契护符","Blood-Pact Charm"],main:"leech",value:.04},
  {id:"lucky_coin",slot:"charm",name:["归途金币","Homebound Coin"],main:"goldBonus",value:.12},
  {id:"hourglass",slot:"charm",name:["裂隙沙漏","Rift Hourglass"],main:"cooldownReduction",value:.045}
];
const GEAR_BASE_BY_ID=Object.fromEntries(GEAR_BASES.map(item=>[item.id,item]));
const GEAR_QUALITIES={common:["普通","Common"],fine:["精良","Fine"],rare:["稀有","Rare"],epic:["史诗","Epic"],legendary:["传说","Legendary"],legacy:["传承","Legacy"]};
const LOOT_RULES={
  explorer:{count:[1,1],scale:[.7,.9],affixes:[1,1],qualities:[["common",75],["fine",25]]},
  adventurer:{count:[1,2],scale:[.9,1.1],affixes:[1,2],qualities:[["common",25],["fine",50],["rare",25]]},
  nightmare:{count:[2,2],scale:[1.1,1.4],affixes:[2,3],qualities:[["fine",25],["rare",50],["epic",25]]},
  ultra:{count:[3,3],scale:[1.4,1.8],affixes:[3,4],qualities:[["rare",30],["epic",50],["legendary",20]]}
};
const GEAR_AFFIXES={
  weapon:[
    {id:"attack",name:["锋锐","Keen"],stat:"attack",range:[2,5],integer:true},
    {id:"crit",name:["精准","Precise"],stat:"crit",range:[.02,.05]},
    {id:"burn",name:["焚烧","Scorching"],stat:"burnBonus",range:[.08,.18]},
    {id:"boss",name:["弑首","Bossbane"],stat:"bossPower",range:[.06,.15]},
    {id:"execute",name:["处决","Executioner"],stat:"low",range:[.06,.14]}
  ],
  armor:[
    {id:"max",name:["强健","Stalwart"],stat:"max",range:[8,18],integer:true},
    {id:"defense",name:["坚固","Fortified"],stat:"defense",range:[1,3],integer:true},
    {id:"ward",name:["守护","Warded"],stat:"ward",range:[10,24],integer:true},
    {id:"reduction",name:["不屈","Resolute"],stat:"damageReduction",range:[.03,.08]},
    {id:"bottle",name:["炼金","Alchemical"],stat:"potionBonus",range:[.1,.3]}
  ],
  charm:[
    {id:"cooldown",name:["迅捷","Swift"],stat:"cooldownReduction",range:[.025,.065]},
    {id:"leech",name:["饮血","Blooddrinking"],stat:"leech",range:[.02,.055]},
    {id:"gold",name:["寻宝","Treasure-Seeking"],stat:"goldBonus",range:[.08,.2]},
    {id:"bottle",name:["补给","Provisioned"],stat:"startPotions",range:[1,1],integer:true},
    {id:"elite",name:["猎魔","Elite Hunter"],stat:"elitePower",range:[.06,.15]}
  ]
};
// Each species has its own rotation, tempo and counterplay, also used by summons.
const MONSTER_MODULES = [
  {moves:["bite","pounce","double"],speed:1.15,weakness:["扑击前精准防御","Perfect Guard the pounce"]},
  {moves:["rage","pounce","bleed"],speed:1.05,weakness:["狂暴后防住裂伤","Ward through the frenzy bleed"]},
  {moves:["sunder","charge","quake"],speed:.8,weakness:["打断重击或震地","Interrupt the crush or quake"]},
  {moves:["poison","shield","hexburst"],speed:.8,weakness:["净化后阻止咒爆","Cleanse before the hex burst"]},
  {moves:["double","bleed","pounce"],speed:1.25,weakness:["精准格挡快速连段","Perfect Guard its quick chain"]},
  {moves:["counter","shadowstep","barrage"],speed:1.2,weakness:["反击姿态时停止直伤","Avoid direct hits during counter stance"]},
  {moves:["weak","dispel","hexburst"],speed:.9,weakness:["驱散后保留净化","Save cleansing after its dispel"]},
  {moves:["burn","heal","fortify"],speed:.85,weakness:["打断治疗与堡垒","Interrupt recovery and fortify"]},
  {moves:["drain","bleed","poison"],speed:.9,weakness:["护盾阻止汲取与裂伤","Ward prevents drain and bleed"]},
  {moves:["counter","shadowstep","shield"],speed:1,weakness:["用持续伤害绕过反击","Damage over time bypasses counters"]},
  {moves:["dispel","shadowstep","charge"],speed:1.15,weakness:["识破暗影步后打断蓄力","Read the step, then interrupt charge"]},
  {moves:["sweep","barrage","bleed"],speed:.95,weakness:["横扫前减伤","Defend before its sweep"]},
  {moves:["weak","curse","hexburst"],speed:1.1,weakness:["咒爆前清除异常","Cleanse before hex burst"]},
  {moves:["poison","burn","barrage"],speed:.9,weakness:["连续净化并防住爆发","Cleanse and guard the burst"]},
  {moves:["fortify","sunder","quake"],speed:.7,weakness:["打断堡垒后的震地","Interrupt the post-fortify quake"]},
  {moves:["burn","rage","pounce","charge"],speed:1.05,weakness:["打断狂暴蓄力","Interrupt the enraged charge"]},
  {moves:["summon","curse","hexburst","dispel"],speed:.85,weakness:["先清理召唤物","Clear summoned enemies first"]},
  {moves:["poison","drain","barrage","bleed"],speed:1.2,weakness:["净化后压制汲取","Cleanse and deny drain"]},
  {moves:["heal","quake","fortify","rage"],speed:.75,weakness:["打断自疗与震地","Interrupt recovery and quake"]},
  {moves:["fortify","charge","barrage"],speed:.85,weakness:["优先清护卫，打断蓄力","Clear guardians, then interrupt charges"]},
  {moves:["burn","hexburst","nova"],speed:1,weakness:["为第三阶段保留净化","Save cleansing for phase three"]}
];
const ACTION_NAMES={bite:["撕咬","Bite"],double:["连击","Combo"],rage:["狂暴","Frenzy"],sunder:["破甲","Sunder"],charge:["蓄力重击","Charged Strike"],poison:["毒雾","Venom"],shield:["屏障","Barrier"],counter:["反击姿态","Counter Stance"],weak:["虚弱诅咒","Weakening Hex"],dispel:["驱散","Dispel"],burn:["烈焰","Flame"],heal:["复苏","Recovery"],drain:["汲取","Drain"],sweep:["横扫","Sweep"],curse:["侵蚀","Corruption"],summon:["召唤","Summon"],nova:["深渊新星","Abyssal Nova"],pounce:["扑袭","Pounce"],barrage:["碎骨连射","Bone Barrage"],bleed:["裂伤","Rending Wound"],fortify:["深渊堡垒","Abyssal Fortify"],quake:["震地冲击","Seismic Crash"],shadowstep:["暗影步","Shadowstep"],hexburst:["咒印爆发","Hexburst"]};
/* ===== 2.18.0: data-driven event system =====
   Each event: id / category / weight / floorRange / rare.
   Categories: safe, trade, arcane, risk, trial.
   floorRange: [minFloor, maxFloor] (1-based). */
const EVENT_CATALOG=[
  {id:"camp",name:["篝火","Campfire"],category:"safe",weight:10,floorRange:[1,8]},
  {id:"shop",name:["商店","Merchant"],category:"trade",weight:9,floorRange:[1,8]},
  {id:"shrine",name:["古老神龛","Ancient Shrine"],category:"arcane",weight:8,floorRange:[1,8]},
  {id:"chest",name:["上锁宝箱","Locked Chest"],category:"risk",weight:8,floorRange:[2,8]},
  {id:"healer",name:["流浪医师","Wandering Healer"],category:"safe",weight:9,floorRange:[1,7]},
  {id:"spring",name:["神秘清泉","Mystic Spring"],category:"arcane",weight:7,floorRange:[1,8]},
  {id:"adventurer",name:["受困冒险者","Trapped Adventurer"],category:"risk",weight:7,floorRange:[2,8]},
  {id:"gambler",name:["骰子赌徒","Dice Gambler"],category:"trade",weight:7,floorRange:[2,8]},
  {id:"library",name:["古老图书馆","Ancient Library"],category:"arcane",weight:8,floorRange:[1,8]},
  {id:"well",name:["低语之井","Whispering Well"],category:"arcane",weight:7,floorRange:[2,8]},
  {id:"cards",name:["纸牌骗子","Card Sharp"],category:"risk",weight:6,floorRange:[3,8]},
  {id:"oracle",name:["预言家","Oracle"],category:"arcane",weight:7,floorRange:[1,7]},
  {id:"curator",name:["遗物收藏家","Relic Curator"],category:"trade",weight:6,floorRange:[3,8]},
  {id:"rift",name:["裂隙之门","Rift Gate"],category:"risk",weight:6,floorRange:[3,8],highRisk:true},
  {id:"forge",name:["遗忘熔炉","Forgotten Forge"],category:"risk",weight:7,floorRange:[2,8]},
  {id:"altar",name:["回声祭坛","Echoing Altar"],category:"arcane",weight:7,floorRange:[1,8]},
  {id:"caravan",name:["月光商队","Moonlit Caravan"],category:"trade",weight:7,floorRange:[2,8]},
  {id:"idol",name:["饥饿神像","Starved Idol"],category:"risk",weight:6,floorRange:[3,8]},
  {id:"stalker",name:["神秘追踪者","Mysterious Stalker"],category:"trial",weight:2,floorRange:[2,7],rare:true},
  {id:"trial",name:["双核试炼","Trial of Twin Cores"],category:"trial",weight:5,floorRange:[2,7]}
];
/* Floor-based category weight multipliers: [safe, trade, arcane, risk, trial] */
const EVENT_FLOOR_WEIGHTS={
  1:{safe:1.6,trade:1.0,arcane:0.9,risk:0.5,trial:0.8},
  2:{safe:1.4,trade:1.1,arcane:1.0,risk:0.7,trial:1.0},
  3:{safe:1.1,trade:1.3,arcane:1.3,risk:0.9,trial:1.0},
  4:{safe:1.0,trade:1.3,arcane:1.3,risk:1.0,trial:1.0},
  5:{safe:0.9,trade:1.2,arcane:1.2,risk:1.2,trial:1.0},
  6:{safe:0.7,trade:1.0,arcane:1.1,risk:1.4,trial:1.0},
  7:{safe:0.6,trade:0.9,arcane:1.0,risk:1.6,trial:1.0},
  8:{safe:0.5,trade:0.8,arcane:0.9,risk:1.8,trial:0.5}
};
const STORY_CHAPTERS=[
  {title:["序章 · 被遗忘的名字","PROLOGUE · THE FORGOTTEN NAME"],body:["七年前，深渊吞没了北境远征军，也从所有史书里抹去了他们的名字。今晚，刻着你名字的黑色信函出现在门前：想知道他们为何消失，就独自走到第八层。","Seven years ago the abyss swallowed the northern expedition—and erased every name from history. Tonight, a black letter bearing your name appeared at the door: descend alone to the eighth floor if you want the truth."]},
  {title:["第一幕 · 墙后的呼吸","ACT I · BREATH BEHIND THE WALL"],body:["石墙内传来整齐的呼吸声。失踪者没有死去；某种东西让他们在墙后继续做着同一个梦。你在裂缝中找到一枚远征军徽记。","Measured breathing echoes inside the stone. The lost did not die; something keeps them dreaming behind the walls. In a crack, you find the expedition's crest."]},
  {title:["第二幕 · 无火的营地","ACT II · THE FIRELESS CAMP"],body:["营地仍保持撤退前的模样，唯独篝火从未燃烧过。桌上的日志写着：深渊不是地下城，而是一段正在寻找宿主的记忆。","The camp remains exactly as it was before the retreat, except its fire was never lit. A journal reads: the abyss is not a dungeon, but a memory searching for a host."]},
  {title:["第三幕 · 回声借用了你的声音","ACT III · THE ECHO WEARS YOUR VOICE"],body:["从这一层开始，回声会提前说出你的选择。它知道你的招式、恐惧，甚至知道你还未经历的失败。信函上的墨迹正在变成你的笔迹。","From this floor onward, the echo speaks your choices before you make them. It knows your skills, your fear, even failures you have not lived. The letter's ink is becoming your handwriting."]},
  {title:["第四幕 · 双核契约","ACT IV · COVENANT OF THE TWIN CORES"],body:["两枚核心维持着深渊的循环：一枚保存死者，一枚重写来者。击碎它们能打开前路，也会让被遗忘者真正迎来死亡。","Twin cores sustain the abyssal cycle: one preserves the dead, the other rewrites those who enter. Breaking them opens the path—and grants the forgotten their first true death."]},
  {title:["第五幕 · 最后一名记录者","ACT V · THE LAST CHRONICLER"],body:["你遇见远征军最后的记录者。他只剩影子，却认得你：七年前，是未来的你把队伍引到了这里。深渊正在把因果折成一个封闭的圆。","You meet the expedition's last chronicler, now only a shadow. He recognizes you: seven years ago, your future self led them here. The abyss is folding cause and effect into a closed circle."]},
  {title:["第六幕 · 王座之前","ACT VI · BEFORE THE THRONE"],body:["所有岔路最终汇向同一座王座。你终于明白，所谓领主不是统治深渊的人，而是每一轮选择留下的总和。王座正在等待新的名字。","Every path converges on a single throne. The Lord is not one who rules the abyss, but the sum of every choice left behind. The throne is waiting for a new name."]},
  {title:["终幕 · 第八层","FINALE · THE EIGHTH FLOOR"],body:["门后没有宝藏，只有七年前尚未发生的清晨。击败守门者，你可以斩断循环；接受它，你将成为下一封黑色信函的书写者。","Beyond the door lies no treasure, only a morning from seven years ago that has not happened yet. Defeat the keeper to sever the cycle—or become the author of the next black letter."]}
];
const STORY_ENDINGS={
  warrior:["你把剑插进王座，所有被抹去的名字重新浮现在铁刃上。黎明到来时，世人终于记起了那支远征军。","You drive your sword into the throne. Every erased name returns along the blade. At dawn, the world remembers the lost expedition."],
  mage:["你没有摧毁循环，而是改写了它的第一行。从此，深渊仍会做梦，却再也不能借走任何人的名字。","You do not destroy the cycle; you rewrite its first line. The abyss still dreams, but it can never steal another name."],
  ranger:["你带着最后一封信离开，并烧毁了通往深渊的地图。多年后，仍有人在无月之夜听见第八层传来的弓弦声。","You leave with the final letter and burn every map to the abyss. Years later, bowstrings can still be heard from the eighth floor on moonless nights."],
  paladin:["圣光没有净化深渊，而是照亮了被困其中的人。你守在门前，直到最后一个灵魂走回清晨。","The light does not cleanse the abyss; it reveals those trapped within. You guard the gate until the final soul walks back into morning."],
  necromancer:["死者拒绝再次被遗忘。他们跟随你穿过大门，而深渊第一次发现：记忆也可以反过来吞噬主人。","The dead refuse to be forgotten again. They follow you through the gate, and the abyss learns that memory can devour its master."],
  creator:["你把圆环拆成无数条可能的道路。每一条都通往不同的黎明，而没有任何一个你再需要写下那封信。","You break the circle into countless possible roads. Each reaches a different dawn, and no version of you ever needs to write the letter again."]
};
const EVENT_PRESENTATION={
  camp:{icon:"♨",tone:"safe",scene:"campfire",desc:["余烬尚暖，盔甲上的霜正缓缓融化。你只有片刻喘息。","The embers still breathe, thawing the frost from your armor. You have only a moment to rest."]},
  shop:{icon:"⚖",tone:"trade",scene:"merchant",desc:["灯笼下，商人把货物一件件推到你面前——价格已经写好。","Under a hooded lantern, the merchant lays out the wares. Every price is final."]},
  shrine:{icon:"◇",tone:"arcane",scene:"altar",desc:["无名神像俯视着你。石座上的旧血仍未完全干涸。","A nameless figure watches from the stone. The old blood on its altar has not yet dried."]},
  chest:{icon:"▣",tone:"risk",scene:"relic",desc:["锁舌里传来细小抓挠声。里面也许是财富，也许不是。","Something scratches softly behind the lock. It may be treasure. It may not."]},
  healer:{icon:"✚",tone:"safe",scene:"refuge",desc:["医师没有问你的名字，只用指尖敲了敲空药瓶。","The healer asks no name, only taps a finger against an empty bottle."]},
  spring:{icon:"≈",tone:"arcane",scene:"refuge",desc:["银色泉水倒映出一张并不属于你的脸。","The silver water reflects a face that is not yours."]},
  adventurer:{icon:"⚑",tone:"risk",scene:"ruins",desc:["求救声从坍塌的石柱后传来，远处也响起了脚步。","A cry comes from behind the collapsed pillars. Footsteps answer in the distance."]},
  gambler:{icon:"⚄",tone:"trade",scene:"merchant",desc:["骨骰在杯中作响。赌徒笑着为你留出一个位置。","Bone dice rattle in a cup. The gambler smiles and makes room for you."]},
  library:{icon:"▤",tone:"arcane",scene:"library",desc:["书页自行翻动，墨迹像虫群一样重新排列。","Pages turn by themselves, their ink rearranging like a swarm."]},
  well:{icon:"◉",tone:"arcane",scene:"oracle",desc:["井底有人用你的声音许诺力量。","Something at the bottom promises power in your own voice."]},
  cards:{icon:"♠",tone:"risk",scene:"merchant",desc:["三张牌面朝下。骗子的袖口比他的笑容更可疑。","Three cards lie face down. The sharp's sleeves are less honest than his smile."]},
  oracle:{icon:"☽",tone:"arcane",scene:"oracle",desc:["预言家闭着眼，却准确地望向你下一步会站的位置。","The oracle's eyes are closed, yet she watches the place where you will stand next."]},
  curator:{icon:"✦",tone:"trade",scene:"relic",desc:["玻璃柜中，每件遗物都在轻轻呼吸。","Inside the glass cases, every relic seems to breathe."]},
  rift:{icon:"⌁",tone:"risk",scene:"rift",desc:["裂隙另一侧传来磨刀声。门已经为你打开。","A blade is being sharpened beyond the rift. The way is already open."]},
  forge:{icon:"⚒",tone:"risk",scene:"forge",desc:["熔炉需要的不是煤。铁砧正在等待你的代价。","This forge does not burn coal. The anvil waits for your price."]},
  altar:{icon:"△",tone:"arcane",scene:"altar",desc:["你的脚步在祭坛上重复了三次，最后一次不是回声。","Your footstep repeats three times across the altar. The last is not an echo."]},
  caravan:{icon:"✧",tone:"trade",scene:"merchant",desc:["月光商队从不回头，也从不为同一个人停留两次。","The moonlit caravan never turns back, and never stops twice for the same traveler."]},
  idol:{icon:"♜",tone:"risk",scene:"altar",desc:["神像张着空洞的嘴。它闻到了活人的温度。","The idol's hollow mouth hangs open. It has tasted the warmth of the living."]},
  stalker:{icon:"◌",tone:"trial",scene:"rift",desc:["那道身影总与你隔着七步。它在等你跟上。","The silhouette remains seven steps ahead. It is waiting for you to follow."]},
  trial:{icon:"⬡",tone:"trial",scene:"trial",desc:["两枚核心在石盘深处共鸣。四十步之内，只有一次答案。","Twin cores resonate beneath the stone board. Within forty moves, there is only one answer."]}
};
const SHOP=[
  {id:"potion",name:["药瓶","Healing Bottle"],price:18}, {id:"weapon",name:["磨刀石","Whetstone"],price:32},
  {id:"armor",name:["锁子甲","Chainmail"],price:35}, {id:"ward",name:["屏障","Barrier"],price:24},
  {id:"tonic",name:["活力药剂","Vitality Tonic"],price:38}, {id:"smoke",name:["烟幕弹","Smoke Bomb"],price:28}
];
