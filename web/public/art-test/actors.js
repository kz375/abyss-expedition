"use strict";

// Character assets are data only. The VFX/action engine in lab.js never needs
// to know which hero or monster is installed here.
globalThis.ART_TEST_ACTORS = Object.freeze({
  defaultHero:"warrior",
  defaultTarget:"golem",
  heroes:Object.freeze({
    warrior:Object.freeze({
      zh:"战士 · 铁誓", en:"Warrior · Iron Vow",
      role:"TEST HERO · WARRIOR", art:"/assets/characters/warrior.png",
      kind:"warrior", scale:1.04, anchor:"50% 100%"
    })
  }),
  targets:Object.freeze({
    golem:Object.freeze({zh:"钢铁魔像 · 实装素材",en:"Iron Golem · Production Art",kind:"golem",art:"/assets/enemies/iron-golem-test.png",scale:1.08,anchor:"50% 100%"}),
    bat:Object.freeze({zh:"洞窟蝙蝠 · 剪影",en:"Cave Bat · Silhouette",kind:"bat"}),
    assassin:Object.freeze({zh:"暗影刺客 · 剪影",en:"Shadow Assassin · Silhouette",kind:"assassin"}),
    boss:Object.freeze({zh:"深渊领主 · 剪影",en:"Abyss Lord · Silhouette",kind:"boss",scale:1.12,anchor:"50% 100%"})
  })
});
