"use strict";

const $ = id => document.getElementById(id);
const clamp = (value, max) => Math.max(0, Math.min(max, value));
const choice = list => list[Math.floor(Math.random() * list.length)];
const BETA_PROFILE_KEY = "abyss-flow-beta-profile-v1";
const readProfile = () => { try { return JSON.parse(localStorage.getItem(BETA_PROFILE_KEY)) || {locale:"zh",kills:{},achievements:{}}; } catch { return {locale:"zh",kills:{},achievements:{}}; } };
const profile = readProfile();
const writeProfile = () => { try { localStorage.setItem(BETA_PROFILE_KEY, JSON.stringify(profile)); } catch {} };
const tx = (zh, en) => profile.locale === "en" ? en : zh;
const MONSTER_EN = {"洞窟蝙蝠":"Cave Bat","深渊猎犬":"Abyss Hound","迷失矿工":"Lost Miner","泥沼史莱姆":"Bog Slime","墓穴鼠":"Crypt Rat","暗影刺客":"Shadow Assassin","诅咒人偶":"Cursed Doll","白骨学者":"Bone Scholar","嗜血水蛭":"Blood Leech","镜像幽灵":"Mirror Wraith","虚空潜行者":"Void Stalker","白骨收割者":"Bone Reaper","恐惧怨灵":"Dread Wraith","疫病携带者":"Plaguebearer","钢铁魔像":"Iron Golem","深渊恶魔":"Abyss Demon","末日先驱":"Doom Harbinger","深渊巨蛇":"Abyss Serpent","饥饿巨像":"Hungry Colossus","圣遗物守卫":"Relic Guardian","深渊领主":"Abyss Lord"};
const EVENT_EN = {"篝火":["Campfire","Rest: recover 30% maximum health"],"预言家":["Oracle","See the next floor's enemy"],"赌徒":["Gambler","Roll for 15–55 gold"],"熔炉":["Forge","Forge a weapon: all damage +6%"],"神龛":["Shrine","Gain 65 ward"],"低语之井":["Whispering Well","Cleanse debuffs and recover 18% maximum health"],"古老图书馆":["Ancient Library","All skill cooldowns -5%"],"流浪商队":["Wandering Caravan","Gain 28 gold and recover 12% maximum health"],"宝箱":["Chest","Open it: gain 42 gold"],"医师":["Healer","Recover 24% maximum health"],"清泉":["Spring","Recover 16% maximum health and gain 28 ward"],"冒险者":["Adventurer","Gain 20 gold and 30 ward"],"收藏家":["Curator","Receive a random unowned relic"],"裂隙":["Rift","All damage +12%"],"祭坛":["Altar","Lose 8% current health; all damage +10%"],"神像":["Idol","Gain 75 ward"]};
const monsterName = monster => monster?.elite ? `${tx("精英 ","Elite ")}${MONSTER_EN[monster.baseName || monster.name.replace("精英 ","")] || monster.baseName || monster.name}` : (MONSTER_EN[monster?.name] || monster?.name || "");

// Beta conversion of the six existing hero archetypes. These are deliberately isolated
// from the Java save/combat values while real-time pacing is tested.
const HEROES = {
  warrior: {name:"战士", enName:"Warrior", key:"WARRIOR · IRON WILL", hp:220, attack:32, passive:"铁意：每场战斗开始时获得 70 护盾", enPassive:"Iron Will: gain 70 ward at each battle start", skills:[
    ["斩击","100% 攻击 · 0.65 秒",650,"damage",1], ["破甲斩","240% 攻击 · 3 秒",3000,"sunder",2.4], ["铁壁","获得 48 护盾 · 6 秒",6000,"shield",48], ["战地包扎","恢复 30 生命 · 8 秒",8000,"heal",30]
  ]},
  mage: {name:"法师", enName:"Mage", key:"MAGE · MANA FLOW", hp:188, attack:35, passive:"法力流：技能冷却缩短 15%", enPassive:"Mana Flow: skills recharge 15% faster", skills:[
    ["奥术箭","100% 攻击 · 0.58 秒",580,"damage",1], ["奥术爆发","270% 攻击 + 灼烧 · 2.55 秒",2550,"burn",2.7], ["秘法屏障","获得 37 护盾 · 5 秒",5000,"shield",37], ["星火修复","恢复 25 生命 · 6.8 秒",6800,"heal",25]
  ]},
  ranger: {name:"游侠", enName:"Ranger", key:"RANGER · HUNTER'S FOCUS", hp:214, attack:36, passive:"猎人专注：普通攻击有 15% 几率触发一次额外射击", enPassive:"Hunter's Focus: 15% chance for an extra basic shot", skills:[
    ["速射","100% 攻击 · 0.48 秒",480,"damage",1], ["双重射击","两箭各 140% 攻击 · 3 秒",3000,"multi",1.4], ["毒牙箭","110% 攻击 + 中毒 · 3 秒",3000,"poison",1.1], ["烟幕","获得 35 护盾 · 5 秒",5000,"shield",35]
  ]},
  paladin: {name:"圣骑士", enName:"Paladin", key:"PALADIN · DIVINE AEGIS", hp:226, attack:31, passive:"神圣壁垒：每 4 秒获得 10 护盾", enPassive:"Divine Aegis: gain 10 ward every 4 seconds", skills:[
    ["圣光挥击","100% 攻击 · 0.68 秒",680,"damage",1], ["神圣审判","215% 攻击、治疗并眩晕 · 3.6 秒",3600,"judgment",2.15], ["守护祷言","获得 50 护盾 · 6 秒",6000,"shield",50], ["净化","恢复 28 并清除异常 · 7 秒",7000,"cleanse",28]
  ]},
  necromancer: {name:"死灵法师", enName:"Necromancer", key:"NECROMANCER · CURSE", hp:200, attack:32, passive:"诅咒：敌人开场 8 秒内承受伤害 +25%", enPassive:"Curse: enemy takes 25% more damage for 8 seconds", skills:[
    ["灵魂火","100% 攻击 · 0.63 秒",630,"damage",1], ["灵魂汲取","200% 攻击、吸取一半生命并中毒 · 3.4 秒",3400,"drain",2], ["骸骨护甲","获得 42 护盾 · 5.5 秒",5500,"shield",42], ["枯萎","造成 80% 攻击并施加诅咒 · 5 秒",5000,"curse",.8]
  ]},
  creator: {name:"造物主", enName:"Creator", key:"CREATOR · EXECUTION", hp:180, attack:31, passive:"执行：敌人低于 10% 最大生命时直接抹除", enPassive:"Execution: erase enemies below 10% maximum health", hidden:true, skills:[
    ["现实切割","100% 攻击 · 0.58 秒",580,"damage",1], ["现实撕裂","五次 100% 攻击 · 4 秒",4000,"rend",1], ["造物屏障","获得 44 护盾 · 5 秒",5000,"shield",44], ["重构","恢复 32 生命 · 7 秒",7000,"heal",32]
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

let game;
function newGame() {
  return {hero:null,floor:0,plan:[],hp:0,max:0,shield:0,enemy:null,enemyHp:0,enemyMax:0,enemyShield:0,
    paused:true,finished:false,choice:false,last:performance.now(),nextEnemy:0,enemyCount:0,cd:[],gold:0,relics:[],
    power:1, cooldownMultiplier:1, burn:0, poison:0, playerPoison:0, weak:0, curse:0, sunder:0, stunned:0, paladinPulse:0, eventUsed:false,archiveOpen:false,archivePaused:false,summons:[],bossPhase:1,telegraph:false,stats:{damage:0,taken:0,healed:0,casts:0}};
}
function makePlan() {
  const byZone = zone => ROSTER.filter(monster => monster.zone === zone);
  return [choice(byZone("EARLY")),choice(byZone("EARLY")),elite(choice(byZone("MID"))),choice(byZone("MID")),elite(choice(byZone("LATE"))),choice(byZone("LATE")),elite(choice(byZone("DEEP"))),choice(ROSTER.filter(monster => monster.zone === "BOSS"))];
}
function elite(monster) { return {...monster, baseName:monster.name, name:`精英 ${monster.name}`, elite:true}; }
function log(text) { $("journal").textContent=`[${new Date().toLocaleTimeString("zh-CN",{hour:"2-digit",minute:"2-digit",second:"2-digit"})}] ${text}`; }
function show(html) { $("overlay").innerHTML=html; $("overlay").hidden=false; }
function hideOverlay() { $("overlay").hidden=true; }
function startChoice() {
  const cards = Object.entries(HEROES).filter(([id]) => id !== "creator" || game.creatorUnlocked).map(([id,hero]) => heroCard(id,hero)).join("");
  const secret = game.creatorUnlocked ? "" : `<div class="secret"><input id="creator-code" aria-label="隐藏角色口令" placeholder="隐藏角色口令"><button id="unlock-creator">确认</button></div>`;
  show(`<div class="modal"><h2>选择远征职业</h2><p>这是完整实时测试版。普通职业直接选择；隐藏角色仍需使用原正式远征中的口令解锁。</p><div class="cards">${cards}</div>${secret}<a class="back" href="/">← 返回正式远征</a></div>`);
  document.querySelectorAll("[data-hero]").forEach(button => button.onclick=() => startRun(button.dataset.hero));
  $("unlock-creator")?.addEventListener("click", () => { if ($("creator-code").value.trim().toLowerCase() === "kz") { game.creatorUnlocked=true; startChoice(); } });
}
function heroCard(id, hero) { return `<button class="card" data-hero="${id}"><span class="tag">${hero.key}</span><strong>${tx(hero.name,hero.enName)}</strong><small>${tx("生命","Health")} ${hero.hp} · ${tx("攻击","Attack")} ${hero.attack}<br>${hero.skills.map(skill => skill[0]).join(" / ")}<br>${tx(hero.passive,hero.enPassive)}</small></button>`; }
function startRun(id) {
  game.hero=HEROES[id]; game.max=game.hero.hp; game.hp=game.max; game.plan=makePlan(); game.paused=false;
  game.paladinPulse=performance.now()+4000; hideOverlay(); beginFloor(); log(`${game.hero.name} 踏入深渊，第 1 层敌人出现。`);
}
function beginFloor() {
  const base=game.plan[game.floor]; game.enemy={...base};
  const floorScale=1+(game.floor*.13)+(game.enemy.elite?.26:0);
  game.enemyMax=Math.round(base.hp*floorScale); game.enemyHp=game.enemyMax; game.enemyShield=0; game.enemyCount=0; game.summons=[]; game.bossPhase=1;
  game.burn=0; game.poison=0; game.stunned=0; game.telegraph=false; game.nextEnemy=performance.now()+1300;
  if(game.resetCooldowns) game.cd=[];
  $("enemy-card").classList.remove("defeated"); $("enemy-name").textContent=monsterName(game.enemy);
  if (game.hero === HEROES.warrior) game.shield=clamp(game.shield+70,game.max);
  if (game.hero === HEROES.necromancer) game.curse=8;
}
function physical(amount) { return Math.max(1, amount * game.power * (game.hp/game.max<.35 ? 1+(game.lowHealthPower||0) : 1) * (game.weak > 0 ? .8 : 1)); }
function skillCooldown(cooldown) { return cooldown * game.cooldownMultiplier * (game.hero === HEROES.mage ? .85 : 1); }
function enemyDamage(amount) { return Math.max(1, amount * (game.sunder > 0 ? 1.25 : 1)); }
function damageEnemy(amount, visual=true) {
  amount *= game.curse > 0 ? 1.25 : 1;
  if (game.summons.length) { const summon=game.summons[0]; summon.hp-=amount; game.stats.damage+=amount; if(summon.hp<=0){game.summons.shift();game.enemy.attack*=.97;game.enemyMax=Math.max(1,Math.round(game.enemyMax*.97));game.enemyHp=Math.min(game.enemyHp,game.enemyMax);log(tx("召唤物被击败，Boss 永久削弱 3%。","Summon defeated: Boss permanently weakened by 3%."));} if(visual)float("enemy",amount,"damage"); return; }
  const absorbed=Math.min(game.enemyShield,amount); game.enemyShield-=absorbed; game.enemyHp-=amount-absorbed;
  game.stats.damage+=amount-absorbed;
  if(game.enemy.zone==="BOSS"){const ratio=game.enemyHp/game.enemyMax;const phase=ratio<=.33?3:ratio<=.66?2:1;if(phase>game.bossPhase){game.bossPhase=phase;game.enemy.attack*=1.15;spawnSummon();log(tx(`Boss 进入第 ${phase} 阶段，属性提高 15%。`,`Boss enters phase ${phase}: attributes rise 15%.`));}}
  if (visual) float("enemy",amount,"damage");
}
function damageHero(amount, visual=true) {
  const absorbed=Math.min(game.shield,amount); game.shield-=absorbed; game.hp-=amount-absorbed;
  game.stats.taken+=amount-absorbed; if(amount-absorbed>0&&game.thorns) damageEnemy((amount-absorbed)*game.thorns,false);
  if (visual) float("player",amount,"damage");
}
function heal(amount) { const before=game.hp; game.hp=clamp(game.hp+amount*(game.healPower||1),game.max); game.stats.healed+=game.hp-before; float("player",game.hp-before,"heal"); }
function spawnSummon(){if(game.summons.length>=2)return;const base=choice(ROSTER.filter(monster=>monster.zone!=="BOSS"));const max=Math.round(base.hp*.5*(1+game.floor*.1));game.summons.push({name:base.name,hp:max,max});}
function float(target, amount, type) {
  const card=$(target+"-card"), value=document.createElement("span");
  card.classList.remove("hit"); void card.offsetWidth; card.classList.add("hit");
  value.className=`floating ${type}`; value.textContent=`${type === "heal" ? "+" : "−"}${Math.max(1,Math.round(amount))}`; value.style.left="50%"; value.style.top="42%"; card.append(value);
  setTimeout(()=>{card.classList.remove("hit");value.remove();},720); $("impact").classList.remove("show"); void $("impact").offsetWidth; $("impact").classList.add("show");
}
function cast(index, now) {
  if (!game.hero || game.paused || game.finished || game.choice || now < game.cd[index]) return;
  const skill=game.hero.skills[index], [name,,cooldown,type,value]=skill; game.cd[index]=now+skillCooldown(cooldown); game.stats.casts++;
  if (type === "damage") { damageEnemy(physical(game.hero.attack*value)); log(`你施放「${name}」。`); if (game.hero===HEROES.ranger && index===0 && Math.random()<.15) damageEnemy(physical(game.hero.attack*value)); }
  if (type === "sunder") { damageEnemy(physical(game.hero.attack*value)); game.stunned=Math.max(game.stunned,1.3); log(`「${name}」击碎敌方防御并造成眩晕。`); }
  if (type === "burn") { damageEnemy(physical(game.hero.attack*value)); game.burn=5; log(`「${name}」施加灼烧。`); }
  if (type === "multi") { damageEnemy(physical(game.hero.attack*value)); damageEnemy(physical(game.hero.attack*value)); log(`「${name}」连续命中两次。`); }
  if (type === "poison") { damageEnemy(physical(game.hero.attack*value)); game.poison=6; log(`「${name}」施加中毒。`); }
  if (type === "shield") { game.shield=clamp(game.shield+value,game.max); log(`你施放「${name}」，获得 ${value} 护盾。`); }
  if (type === "heal") { heal(value); log(`你施放「${name}」，恢复生命。`); }
  if (type === "judgment") { damageEnemy(physical(game.hero.attack*value)); heal(18); game.stunned=2; log(`「${name}」治疗你并眩晕敌人。`); }
  if (type === "cleanse") { heal(value); game.playerPoison=game.weak=game.sunder=0; log(`「${name}」清除了自身异常。`); }
  if (type === "drain") { const dealt=physical(game.hero.attack*value); damageEnemy(dealt); heal(dealt*.5); game.poison=5; log(`「${name}」吸取生命并施加中毒。`); }
  if (type === "curse") { damageEnemy(physical(game.hero.attack*value)); game.curse=8; log(`「${name}」使敌人进入诅咒。`); }
  if (type === "rend") { for(let hit=0;hit<5;hit++) damageEnemy(physical(game.hero.attack)); log(`「${name}」连续撕裂现实五次。`); }
  if (game.hero === HEROES.creator && game.enemyHp/game.enemyMax < .1) { game.enemyHp=0; log("执行触发：敌人低于 10% 生命，被直接抹除。 "); }
  checkEnd();
}
function enemyAction(now) {
  if (now < game.nextEnemy || game.finished || game.choice) return;
  if (game.stunned>0) { game.nextEnemy=now+700; return; }
  const enemy=game.enemy; game.enemyCount++; const special=game.enemyCount%3===0;
  if(special && !game.telegraph){game.telegraph=true;game.enemyCount--;game.nextEnemy=now+1100;log(tx(`危险预警：${monsterName(enemy)} 正在蓄力「${enemy.special}」。立即防御、净化或打断！`,`Danger: ${monsterName(enemy)} is charging ${enemy.special}. Defend, cleanse, or interrupt now!`));return;}
  game.telegraph=false;
  let multiplier=special?1.3:1, hits=1;
  if (special && enemy.kind === "double") { multiplier=.82; hits=2; }
  if (special && enemy.kind === "shield") { game.enemyShield=clamp(game.enemyShield+35,game.enemyMax); log(`${enemy.name} 使用「${enemy.special}」，获得 35 护盾。`); game.nextEnemy=now+900; return; }
  if (special && enemy.zone === "BOSS") { spawnSummon(); damageHero(enemyDamage(enemy.attack*multiplier)); log(tx(`${monsterName(enemy)} 召唤护卫并发动 ${enemy.special}。`,`${monsterName(enemy)} summons a guardian and uses ${enemy.special}.`)); }
  else if (special && enemy.kind === "drain") { const before=game.hp; damageHero(enemyDamage(enemy.attack*multiplier)); const dealt=before-game.hp; game.enemyHp=clamp(game.enemyHp+dealt*.5,game.enemyMax); log(`${enemy.name} 使用「${enemy.special}」，并吸取生命。`); }
  else { for(let hit=0;hit<hits;hit++) damageHero(enemyDamage(enemy.attack*multiplier)); log(`${enemy.name}${special?`使用「${enemy.special}」`:"发动攻击"}。`); }
  if (special) applyEnemyEffect(enemy.kind); game.nextEnemy=now+(special?2200:Math.max(1050,1600-game.floor*35)); checkEnd();
}
function applyEnemyEffect(kind) {
  if (kind === "poison") game.playerPoison=Math.max(game.playerPoison,5);
  if (kind === "burn") game.playerPoison=Math.max(game.playerPoison,4);
  if (kind === "weak") game.weak=Math.max(game.weak,5);
  if (kind === "curse" || kind === "nova") game.sunder=Math.max(game.sunder,5);
  if (kind === "sunder" || kind === "sweep") game.sunder=Math.max(game.sunder,4);
  if (kind === "nova") game.enemyShield=clamp(game.enemyShield+28,game.enemyMax);
}
function runStats(){return `<div class="run-summary"><span>${tx("造成伤害","Damage")} <b>${Math.round(game.stats.damage)}</b></span><span>${tx("承受伤害","Taken")} <b>${Math.round(game.stats.taken)}</b></span><span>${tx("治疗","Healing")} <b>${Math.round(game.stats.healed)}</b></span><span>${tx("技能次数","Casts")} <b>${game.stats.casts}</b></span></div>`;}
function checkEnd() {
  if (game.enemyHp<=0 && !game.choice && !game.finished) { game.enemyHp=0; game.choice=true; game.paused=true; $("enemy-card").classList.add("defeated"); recordKill(game.enemy); game.gold+=18+game.floor*7+(game.enemy.elite?18:0); log(`${monsterName(game.enemy)} ${tx("被击败。获得金币，选择远征奖励。","was defeated. Gain gold and choose a relic.")}`); rewardChoice(); }
  if (game.hp<=0 && !game.finished) { game.hp=0; game.finished=true; game.paused=true; $("player-card").classList.add("defeated"); log("你被深渊击退。本次 Beta 远征结束。"); show(`<div class="modal"><h2>${tx("远征结束","Expedition Ended")}</h2><p>${tx("这不会影响正式游戏、成就或存档。","This never affects the main game, achievements, or saves.")}</p>${runStats()}<button class="card" id="again"><strong>${tx("重新开始测试","Restart Test")}</strong><small>${tx("重新选择职业与八层路线。","Choose a class and eight-floor route again.")}</small></button><a class="back" href="/">← ${tx("返回正式远征","Return to Main Expedition")}</a></div>`); $("again").onclick=reset; }
}
const ACHIEVEMENTS=[
  ["first_blood","初入深渊","First Blood","击败任意一只怪物","Defeat any monster"],
  ["elite_hunter","精英猎手","Elite Hunter","击败一只精英怪","Defeat an elite monster"],
  ["boss_slayer","深渊征服者","Abyss Conqueror","击败最终 Boss","Defeat the final boss"],
  ["collector","怪物学者","Monster Scholar","击败 10 种不同怪物","Defeat 10 different monsters"],
  ["full_codex","深渊档案员","Abyss Archivist","击败全部 21 种怪物","Defeat all 21 monsters"]
];
function unlockAchievement(id) { if (!profile.achievements[id]) { profile.achievements[id]=true; writeProfile(); log(tx("成就解锁：","Achievement unlocked: ")+(ACHIEVEMENTS.find(item=>item[0]===id)?.[profile.locale === "en" ? 2 : 1]||id)); } }
function recordKill(monster) {
  const key=monster.baseName || monster.name.replace("精英 ",""); profile.kills[key]=(profile.kills[key]||0)+1; writeProfile();
  unlockAchievement("first_blood"); if(monster.elite) unlockAchievement("elite_hunter"); if(monster.zone==="BOSS") unlockAchievement("boss_slayer");
  const unique=Object.keys(profile.kills).length; if(unique>=10) unlockAchievement("collector"); if(unique>=ROSTER.length) unlockAchievement("full_codex");
}
function openArchive(kind) {
  if(!game) return; game.archivePaused=game.paused; game.archiveOpen=true; game.paused=true;
  if(kind==="codex") { const rows=ROSTER.map(monster=>{const count=profile.kills[monster.name]||0, known=count>0; return `<div class="archive-row ${known?"known":"unknown"}"><strong>${known?monsterName(monster):"???"}</strong><small>${known?`${tx("击败","Defeated")} ${count}`:tx("尚未击败","Not yet defeated")}</small></div>`;}).join(""); show(`<div class="modal archive"><h2>${tx("怪物图鉴","Monster Codex")}</h2><p>${tx("已记录 21 种深渊生物的击败次数。","Track defeats across all 21 abyss creatures.")}</p><div class="archive-list">${rows}</div><button class="card archive-close">${tx("返回","Return")}</button></div>`); }
  else { const rows=ACHIEVEMENTS.map(([id,zh,en,zhDesc,enDesc])=>`<div class="archive-row ${profile.achievements[id]?"known":"unknown"}"><strong>${profile.achievements[id]?tx(zh,en):"???"}</strong><small>${profile.achievements[id]?tx(zhDesc,enDesc):tx("尚未解锁","Locked")}</small></div>`).join(""); show(`<div class="modal archive"><h2>${tx("成就","Achievements")}</h2><p>${tx("实时 Beta 的成就保存在当前浏览器。","Beta achievements are saved in this browser.")}</p><div class="archive-list">${rows}</div><button class="card archive-close">${tx("返回","Return")}</button></div>`); }
  document.querySelector(".archive-close").onclick=()=>{hideOverlay();game.archiveOpen=false;game.paused=game.archivePaused;};
}
const RELICS=[
  ["战栗之刃","所有伤害 +12%",()=>game.power*=1.12],["活力结晶","生命上限 +26，并恢复 26",()=>{game.max+=26;heal(26)}],["月影护符","获得 55 护盾",()=>game.shield=clamp(game.shield+55,game.max)],
  ["迅捷刻印","所有技能冷却 -10%",()=>shortenCooldowns(.9)],["炽焰核心","灼烧伤害提高",()=>game.power*=1.06],["掠夺者印记","获得 35 金币",()=>game.gold+=35],
  ["荆棘冠冕","每次受击反弹 12% 伤害",()=>game.thorns=(game.thorns||0)+.12],["不屈徽章","低于 35% 生命时所有伤害 +20%",()=>game.lowHealthPower=(game.lowHealthPower||0)+.2],["时砂","每场战斗开始时所有技能立即就绪",()=>game.resetCooldowns=true],["愈合符文","治疗效果 +35%",()=>game.healPower=(game.healPower||1)*1.35]
];
function shortenCooldowns(factor) {
  const now=performance.now(); game.cooldownMultiplier*=factor; game.cd=game.cd.map(end=>now+(end-now)*factor);
}
function rewardChoice() {
  const picks=[...RELICS].sort(()=>Math.random()-.5).slice(0,3);
  show(`<div class="modal"><h2>第 ${game.floor+1} 层完成</h2><p>每次胜利选择一件本次远征的遗物。随后会触发一次简短事件并推进下一层。</p><div class="cards">${picks.map(([name,desc],i)=>`<button class="card" data-relic="${i}"><span class="tag">RELIC</span><strong>${name}</strong><small>${desc}</small></button>`).join("")}</div></div>`);
  document.querySelectorAll("[data-relic]").forEach(button=>button.onclick=()=>takeRelic(picks[Number(button.dataset.relic)]));
}
function takeRelic(relic) {
  relic[2](); game.relics.push(relic[0]); hideOverlay();
  if (game.floor===7) { game.finished=true; log("深渊领主倒下。八层实时远征测试完成！"); show(`<div class="modal"><h2>${tx("深渊远征完成","Abyss Expedition Complete")}</h2><p>${tx("你完成了完整八层实时测试。","You completed the full eight-floor real-time test.")}</p>${runStats()}<button class="card" id="again"><strong>${tx("再次远征","Expedition Again")}</strong><small>${tx("重置测试数据，尝试新的敌人路线。","Reset the test and try a new route.")}</small></button><a class="back" href="/">← ${tx("返回正式远征","Return to Main Expedition")}</a></div>`); $("again").onclick=reset; return; }
  eventChoice();
}
const EVENTS=[
  ["篝火","休息：恢复 30% 最大生命",()=>heal(game.max*.3)],
  ["预言家","预见下一层：显示即将遭遇的怪物",()=>log(`预言家看见下一层：${game.plan[game.floor+1].name}。`)],
  ["赌徒","掷骰：获得 15–55 金币",()=>{const gold=15+Math.floor(Math.random()*41);game.gold+=gold;log(`赌徒给了你 ${gold} 金币。`)}],
  ["熔炉","锻造武器：所有伤害 +6%",()=>game.power*=1.06],
  ["神龛","获得 65 护盾",()=>game.shield=clamp(game.shield+65,game.max)],
  ["低语之井","净化异常并恢复 18% 最大生命",()=>{game.playerPoison=game.weak=game.sunder=0;heal(game.max*.18)}],
  ["古老图书馆","所有技能冷却额外缩短 5%",()=>shortenCooldowns(.95)],
  ["流浪商队","补给：获得 28 金币并恢复 12% 最大生命",()=>{game.gold+=28;heal(game.max*.12)}],
  ["宝箱","打开宝箱：获得 42 金币",()=>game.gold+=42],
  ["医师","治疗：恢复 24% 最大生命",()=>heal(game.max*.24)],
  ["清泉","清泉祝福：恢复 16% 最大生命并获得 28 护盾",()=>{heal(game.max*.16);game.shield=clamp(game.shield+28,game.max)}],
  ["冒险者","交换情报：获得 20 金币，下一场开局获得 30 护盾",()=>{game.gold+=20;game.shield=clamp(game.shield+30,game.max)}],
  ["收藏家","古物交易：随机获得一件未持有遗物",()=>{const pool=RELICS.filter(relic=>!game.relics.includes(relic[0]));if(pool.length){const relic=choice(pool);relic[2]();game.relics.push(relic[0]);}}],
  ["裂隙","穿越裂隙：下一场敌人受到的伤害 +12%",()=>game.power*=1.12],
  ["祭坛","献祭：失去 8% 当前生命，所有伤害 +10%",()=>{game.hp=Math.max(1,game.hp*.92);game.power*=1.10}],
  ["神像","守护祝福：获得 75 护盾",()=>game.shield=clamp(game.shield+75,game.max)]
];
function eventChoice() {
  const events=[...EVENTS].sort(()=>Math.random()-.5).slice(0,4); game.eventUsed=false;
  show(`<div class="modal"><h2>${tx("层间事件","Interfloor Events")}</h2><p>${tx("随机出现四个事件，只能选择其中一个；其余事件会立即关闭。","Four events appear at random. Choose one; the other three immediately close.")}</p><div class="cards">${events.map(([name,desc],i)=>`<button class="card" data-event="${i}"><span class="tag">EVENT</span><strong>${tx(name,EVENT_EN[name]?.[0]||name)}</strong><small>${tx(desc,EVENT_EN[name]?.[1]||desc)}</small></button>`).join("")}</div></div>`);
  document.querySelectorAll("[data-event]").forEach(button=>button.onclick=()=>{
    if (game.eventUsed) return; game.eventUsed=true;
    document.querySelectorAll("[data-event]").forEach(card=>card.disabled=true);
    const event=events[Number(button.dataset.event)]; event[2](); game.floor++; game.choice=false; game.paused=false; hideOverlay(); beginFloor(); log(`${tx(event[0],EVENT_EN[event[0]]?.[0]||event[0])} ${tx("结束。进入第 ","ends. Enter floor ")}${game.floor+1}${tx(" 层。",".")}`);
  });
}
function renderStatus(container, list) { container.replaceChildren(); list.filter(([text])=>text).forEach(([text,bad])=>{const item=document.createElement("span");item.className=`status${bad?" bad":""}`;item.textContent=text;container.append(item);}); }
function renderSkills(now) {
  $("skills").replaceChildren(); if (!game.hero) return;
  game.hero.skills.forEach((skill,index)=>{const [name,description,cooldown]=skill, left=Math.max(0,(game.cd[index]||0)-now), button=document.createElement("button"), cover=document.createElement("span");
    button.className=`skill${left===0?" ready":""}`; button.disabled=game.paused||game.finished||game.choice||left>0; button.innerHTML=`<span class="key">${index+1}</span><strong>${name}</strong><small>${description}</small>`; cover.className="cool";cover.style.transform=`scaleX(${left/cooldown})`;button.append(cover);button.onclick=()=>cast(index,performance.now());$("skills").append(button);
  });
}
function render(now) {
  const enemy=game.enemy;
  $("hero-hp").style.transform=`scaleX(${game.max?clamp(game.hp/game.max,1):0})`; $("hero-shield").style.transform=`scaleX(${game.max?clamp(game.shield/game.max,1):0})`;
  $("enemy-hp").style.transform=`scaleX(${game.enemyMax?clamp(game.enemyHp/game.enemyMax,1):0})`; $("enemy-shield").style.transform=`scaleX(${game.enemyMax?clamp(game.enemyShield/game.enemyMax,1):0})`;
  $("hero-hp-text").textContent=game.max?`${Math.ceil(game.hp)} / ${game.max}`:"—"; $("hero-shield-text").textContent=game.shield?`护盾 ${Math.ceil(game.shield)}`:"无护盾";
  $("enemy-hp-text").textContent=game.enemyMax?`${Math.ceil(game.enemyHp)} / ${game.enemyMax}`:"—"; $("enemy-shield-text").textContent=game.enemyShield?`护盾 ${Math.ceil(game.enemyShield)}`:"无护盾";
  $("summons").replaceChildren(...game.summons.map(summon=>{const row=document.createElement("div");row.className="summon-row";row.innerHTML=`<span>${tx("召唤护卫","Summoned Guardian")}</span><b>${Math.max(0,Math.ceil(summon.hp))}/${summon.max}</b><i><em style="transform:scaleX(${clamp(summon.hp/summon.max,1)})"></em></i>`;return row;}));
  $("hero-name").textContent=game.hero?tx(game.hero.name,game.hero.enName):tx("选择一名英雄","Choose a hero"); $("hero-role").textContent=game.hero?.key||"PLAYER"; $("hero-passive").textContent=game.hero?tx(game.hero.passive,game.hero.enPassive):tx("职业被动将在这里显示","Your class passive appears here");
  $("enemy-name").textContent=enemy?monsterName(enemy):tx("深渊正在等待","The abyss awaits"); $("enemy-role").textContent=enemy?`${enemy.zone}${enemy.elite?" · ELITE":""}`:"HOSTILE";
  $("floor-label").textContent=game.hero?`第 ${Math.min(game.floor+1,8)} / 8 层 · ${enemy?.name||"准备中"}`:"选择职业后开始";
  $("flow").classList.toggle("paused",game.paused); if(game.hero&&!game.paused&&!game.finished&&!game.choice) $("flow-text").textContent=game.stunned>0?`敌人眩晕 ${game.stunned.toFixed(1)} 秒`:`实时进行中 · ${Math.max(0,(game.nextEnemy-now)/1000).toFixed(1)} 秒后敌人行动`;
  $("gold").textContent=`金币 ${game.gold}`; $("relics").textContent=`遗物 ${game.relics.length}`;
  renderStatus($("hero-status"),[[game.shield?`🛡 护盾 ${Math.ceil(game.shield)}`:"",false],[game.playerPoison?`☠ 中毒 ${game.playerPoison.toFixed(1)}s`:"",true],[game.weak?`↓ 虚弱 ${game.weak.toFixed(1)}s`:"",true],[game.sunder?`⌁ 破甲 ${game.sunder.toFixed(1)}s`:"",true]]);
  renderStatus($("enemy-status"),[[game.enemyShield?`🛡 护盾 ${Math.ceil(game.enemyShield)}`:"",false],[game.burn?`🔥 灼烧 ${game.burn.toFixed(1)}s`:"",true],[game.poison?`☠ 中毒 ${game.poison.toFixed(1)}s`:"",true],[game.curse?`✦ 诅咒 ${game.curse.toFixed(1)}s`:"",true],[game.stunned?`✧ 眩晕 ${game.stunned.toFixed(1)}s`:"",true]]);
  $("intent").textContent=enemy ? (game.summons.length?tx("召唤物存在：必须先击败护卫才能伤害 Boss。","Guardians active: defeat them before damaging the Boss."):(game.stunned>0?tx("敌人被眩晕，无法行动。","Enemy is stunned."):`${tx("意图","Intent")}：${game.enemyCount%3===2?enemy.special:tx("普通攻击","Basic attack")}`)) : tx("敌人尚未苏醒。","No enemy has awakened.");
  renderSkills(now);
}
function applyLocale() {
  document.documentElement.lang=profile.locale === "en" ? "en" : "zh-CN";
  $("language").textContent=profile.locale === "en" ? "中文" : "EN"; $("codex").textContent=tx("图鉴","Codex"); $("achievements").textContent=tx("成就","Achievements");
  $("title").textContent=tx("深渊流战 · 八层远征测试","Abyss Flow Combat · Eight-Floor Test");
  $("sub").textContent=tx("完整独立测试版：所有战斗均为实时冷却制。它不读取、不修改正式远征存档。","A standalone real-time cooldown test. It never reads or changes the main expedition save.");
  $("notice").textContent=tx("测试目标：职业手感、怪物意图、技能冷却、异常状态、遗物/事件抉择与八层推关节奏。","Testing class feel, enemy intent, cooldowns, status effects, relic/event choices, and eight-floor pacing.");
  if (!game.hero) startChoice();
}
function tick(now) {
  const delta=Math.min(80,now-game.last)/1000; game.last=now;
  if (!game.paused && !game.finished && !game.choice) {
    game.shield=Math.max(0,game.shield-delta*1.6); game.enemyShield=Math.max(0,game.enemyShield-delta*1.2);
    for (const key of ["burn","poison","playerPoison","weak","curse","sunder","stunned"]) game[key]=Math.max(0,game[key]-delta);
    if (game.burn>0) damageEnemy(delta*5*game.power,false); if (game.poison>0) damageEnemy(delta*4,false); if (game.playerPoison>0) damageHero(delta*4,false);
    if (game.hero===HEROES.paladin && now>=game.paladinPulse) { game.shield=clamp(game.shield+10,game.max);game.paladinPulse=now+4000; }
    enemyAction(now); checkEnd();
  }
  render(now); requestAnimationFrame(tick);
}
function reset() { game=newGame(); $("player-card").classList.remove("defeated");$("enemy-card").classList.remove("defeated");$("pause").textContent="暂停";startChoice();render(performance.now()); }
$("pause").onclick=()=>{if(!game.hero||game.finished||game.choice)return;game.paused=!game.paused;$("pause").textContent=game.paused?"继续":"暂停";$("flow-text").textContent=game.paused?"已暂停 · 双方行动冻结":"实时进行中";};
$("restart").onclick=reset;
$("language").onclick=()=>{profile.locale=profile.locale === "en" ? "zh" : "en";writeProfile();applyLocale();};
$("codex").onclick=()=>openArchive("codex");
$("achievements").onclick=()=>openArchive("achievements");
document.addEventListener("keydown",event=>{if(!event.repeat&&"1234".includes(event.key))cast(Number(event.key)-1,performance.now());});
reset(); applyLocale(); requestAnimationFrame(tick);
