"use strict";

globalThis.ABYSS_BETA_BUILD = "2.2.18";

const HERO_ART = Object.freeze({
  warrior:"/assets/characters/warrior.png", mage:"/assets/characters/mage.png",
  ranger:"/assets/characters/ranger.png", paladin:"/assets/characters/paladin.png",
  necromancer:"/assets/characters/necromancer.png", creator:"/assets/characters/creator.png"
});

const $ = id => document.getElementById(id);
const clamp = (value, max) => Math.max(0, Math.min(max, value));
const choice = list => list[Math.floor(random() * list.length)];
const BETA_PROFILE_KEY = "abyss-flow-beta-profile-v1";
const BETA_RUN_KEY = "abyss-flow-beta-run-v1";
const emptyProfile=()=>({locale:"zh",kills:{},achievements:{},progress:{},claims:{},warehouse:[],equipped:{weapon:null,armor:null,charm:null}});
const readProfile = () => { try { const value=JSON.parse(localStorage.getItem(BETA_PROFILE_KEY)); return value && typeof value === "object" && !Array.isArray(value) ? value : emptyProfile(); } catch { return emptyProfile(); } };
const profile = readProfile();
const writeProfile = () => { try { localStorage.setItem(BETA_PROFILE_KEY, JSON.stringify(profile)); } catch {} };
const clearRun = () => { try { localStorage.removeItem(BETA_RUN_KEY); } catch {} };
const tx = (zh, en) => profile.locale === "en" ? en : zh;
const MONSTER_EN = {"洞窟蝙蝠":"Cave Bat","深渊猎犬":"Abyss Hound","迷失矿工":"Lost Miner","泥沼史莱姆":"Bog Slime","墓穴鼠":"Crypt Rat","暗影刺客":"Shadow Assassin","诅咒人偶":"Cursed Doll","白骨学者":"Bone Scholar","嗜血水蛭":"Blood Leech","镜像幽灵":"Mirror Wraith","虚空潜行者":"Void Stalker","白骨收割者":"Bone Reaper","恐惧怨灵":"Dread Wraith","疫病携带者":"Plaguebearer","钢铁魔像":"Iron Golem","深渊恶魔":"Abyss Demon","末日先驱":"Doom Harbinger","深渊巨蛇":"Abyss Serpent","饥饿巨像":"Hungry Colossus","圣遗物守卫":"Relic Guardian","深渊领主":"Abyss Lord"};

// Beta conversion of the six existing hero archetypes. These are deliberately isolated
// from the Java save/combat values while real-time pacing is tested.
const HEROES = {
  warrior: {name:"战士", enName:"Warrior", key:"WARRIOR · IRON WILL", hp:220, attack:32, passive:"铁意：每场战斗开始时获得 70 护盾", enPassive:"Iron Will: gain 70 ward at each battle start", skills:[
    ["横扫","快速攻击并积累破势 · 0.72 秒",720,"cleave",1.1], ["破甲斩","打断意图并大量积累破势 · 3 秒",3000,"sunder",2.05], ["处决重斩","破势期间造成爆发伤害 · 5.2 秒",5200,"finisher",1.8]
  ]},
  mage: {name:"法师", enName:"Mage", key:"MAGE · MANA FLOW", hp:188, attack:35, passive:"法力流：技能冷却缩短 15%", enPassive:"Mana Flow: skills recharge 15% faster", skills:[
    ["奥术箭","95% 攻击；对灼烧目标额外爆发 · 0.58 秒",580,"arcane",.95], ["奥术爆发","250% 攻击并灼烧 · 2.7 秒",2700,"burn",2.5], ["秘法屏障","获得 34 护盾 · 5 秒",5000,"shield",34], ["时间回响","刷新前三个技能 · 11 秒",11000,"rewind",0]
  ]},
  ranger: {name:"游侠", enName:"Ranger", key:"RANGER · HUNTER'S FOCUS", hp:214, attack:36, passive:"猎人专注：普通攻击有 15% 几率触发一次额外射击", enPassive:"Hunter's Focus: 15% chance for an extra basic shot", skills:[
    ["速射","100% 攻击 · 0.48 秒",480,"damage",1], ["双重射击","两箭各 140% 攻击 · 3 秒",3000,"multi",1.4], ["毒牙箭","110% 攻击 + 中毒 · 3 秒",3000,"poison",1.1], ["烟幕","获得 35 护盾 · 5 秒",5000,"shield",35]
  ]},
  paladin: {name:"圣骑士", enName:"Paladin", key:"PALADIN · DIVINE AEGIS", hp:226, attack:31, passive:"神圣壁垒：每 4 秒获得 10 护盾", enPassive:"Divine Aegis: gain 10 ward every 4 seconds", skills:[
    ["圣光挥击","95% 攻击并获得 7 护盾 · 0.72 秒",720,"smite",.95], ["神圣审判","220% 攻击并打断 · 3.8 秒",3800,"judgment",2.2], ["守护祷言","获得 55 护盾 · 6 秒",6000,"shield",55], ["圣光净化","清除异常并获得 32 护盾 · 7 秒",7000,"purify",32]
  ]},
  necromancer: {name:"死灵法师", enName:"Necromancer", key:"NECROMANCER · CURSE", hp:200, attack:32, passive:"诅咒：敌人开场 8 秒内承受伤害 +25%", enPassive:"Curse: enemy takes 25% more damage for 8 seconds", skills:[
    ["灵魂火","90% 攻击并灼烧 · 0.68 秒",680,"soulfire",.9], ["灵魂汲取","190% 攻击；伤害转为灵魂护盾 · 3.5 秒",3500,"soulward",1.9], ["骸骨护甲","获得 46 护盾 · 5.5 秒",5500,"shield",46], ["枯萎","80% 攻击并施加诅咒 · 5 秒",5000,"curse",.8]
  ]},
  creator: {name:"造物主", enName:"Creator", key:"CREATOR · EXECUTION", hp:180, attack:31, passive:"执行：敌人低于 10% 最大生命时直接抹除", enPassive:"Execution: erase enemies below 10% maximum health", hidden:true, skills:[
    ["现实切割","100% 攻击 · 0.58 秒",580,"damage",1], ["现实撕裂","五次 100% 攻击 · 4 秒",4000,"rend",1], ["造物屏障","获得 44 护盾 · 5 秒",5000,"shield",44], ["规则重写","清除敌方强化并刷新前三个技能 · 9 秒",9000,"rewrite",0]
  ]}
};

// All 21 current codex creatures are represented in the real-time beta pool.
const ROSTER = [
  ["洞窟蝙蝠","EARLY",245,13,"Savage Bite","bite"],["深渊猎犬","EARLY",265,15,"Savage Bite","bite"],["迷失矿工","EARLY",280,13,"Pickaxe Crush","sunder"],["泥沼史莱姆","EARLY",255,12,"Mire Spit","poison"],["墓穴鼠","EARLY",250,16,"Mire Spit","poison"],
  ["暗影刺客","MID",320,22,"Shadow Combo","double"],["诅咒人偶","MID",335,19,"Cursed Hex","weak"],["白骨学者","MID",310,20,"Soul Bolt","burn"],["嗜血水蛭","MID",345,23,"Blood Drain","drain"],["镜像幽灵","MID",305,24,"Mirror Pulse","curse"],
  ["虚空潜行者","LATE",430,28,"Void Lash","curse"],["白骨收割者","LATE",455,26,"Reaper Sweep","sweep"],["恐惧怨灵","LATE",420,27,"Wraith Howl","weak"],["疫病携带者","LATE",440,25,"Toxic Burst","poison"],["钢铁魔像","LATE",480,25,"Iron Slam","sunder"],
  ["深渊恶魔","DEEP",570,32,"Abyssal Rend","burn"],["末日先驱","DEEP",600,30,"Doom Chant","curse"],["深渊巨蛇","DEEP",555,33,"Serpent Coil","poison"],["饥饿巨像","DEEP",640,31,"Colossus Crash","sweep"],
  ["圣遗物守卫","BOSS",860,40,"Guardian's Wrath","weak"],["深渊领主","BOSS",940,43,"Abyssal Nova","nova"]
].map(([name,zone,hp,attack,special,kind]) => ({name,zone,hp,attack,special,kind}));

const monsterName = m => m ? `${m.elite ? tx("精英 ", "Elite ") : ""}${tx(m.baseName || m.name.replace("精英 ", ""), MONSTER_EN[m.baseName || m.name.replace("精英 ", "")] || m.name)}` : "";
