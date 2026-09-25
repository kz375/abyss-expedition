"use strict";

// Character assets are data only. The VFX/action engine in lab.js never needs
// to know which hero or monster is installed here.
const SHARED_RIG_CHARACTERS=Object.freeze({warrior:"warrior-v1",mage:"mage-v1",ranger:"ranger-v1",paladin:"paladin-v1",necromancer:"necromancer-v1",creator:"creator-v1"});
globalThis.ART_TEST_ACTORS = Object.freeze({
  defaultHero:"warrior",
  defaultTarget:"golem",
  rigCharacters:SHARED_RIG_CHARACTERS,
  heroes:Object.freeze({
    warrior:Object.freeze({
      zh:"战士 · 铁誓", en:"Warrior · Iron Vow",
      role:"HUMANOID · IRON WILL", character:"warrior-v1", fx:"physical",
      kind:"warrior", scale:1.04, anchor:"50% 100%"
    }),
    mage:Object.freeze({zh:"法师 · 深渊织法者",en:"Mage · Abyss Weaver",role:"HUMANOID · ARCANE",character:"mage-v1",fx:"arcane",kind:"mage",scale:1.04,anchor:"50% 100%"}),
    ranger:Object.freeze({zh:"游侠 · 暗林猎手",en:"Ranger · Gloam Hunter",role:"HUMANOID · VENOM",character:"ranger-v1",fx:"venom",kind:"ranger",scale:1.04,anchor:"50% 100%"}),
    paladin:Object.freeze({zh:"圣骑士 · 残阳壁垒",en:"Paladin · Dying Sun",role:"HEAVY · HOLY",character:"paladin-v1",fx:"holy",kind:"paladin",scale:1.08,anchor:"50% 100%"}),
    necromancer:Object.freeze({zh:"死灵法师 · 白骨司祭",en:"Necromancer · Bone Prelate",role:"HUMANOID · SOUL",character:"necromancer-v1",fx:"soul",kind:"necromancer",scale:1.04,anchor:"50% 100%"}),
    creator:Object.freeze({zh:"造物主 · 现实编织者",en:"Creator · Reality Weaver",role:"HUMANOID · REALITY",character:"creator-v1",fx:"reality",kind:"creator",scale:1.04,anchor:"50% 100%"})
  }),
  targets:Object.freeze({
    golem:Object.freeze({zh:"钢铁魔像 · 实装素材",en:"Iron Golem · Production Art",kind:"golem",art:"/assets/enemies/iron-golem-test.png",scale:1.08,anchor:"50% 100%"}),
    bat:Object.freeze({zh:"洞窟蝙蝠 · 实装素材",en:"Cave Bat · Production Art",kind:"bat",art:"/assets/enemies/cave-bat-v1.png",scale:1.04,anchor:"50% 100%"}),
    assassin:Object.freeze({zh:"暗影刺客 · 实装素材",en:"Shadow Assassin · Production Art",kind:"assassin",art:"/assets/enemies/shadow-assassin-v1.png",scale:1.02,anchor:"50% 100%"}),
    boss:Object.freeze({zh:"深渊领主 · 实装素材",en:"Abyss Lord · Production Art",kind:"boss",art:"/assets/enemies/abyss-lord-v1.png",scale:1.1,anchor:"50% 100%"})
  })
});
