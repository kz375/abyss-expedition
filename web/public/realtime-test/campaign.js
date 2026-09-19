"use strict";

const $ = id => document.getElementById(id);
const clamp = (value, max) => Math.max(0, Math.min(max, value));
const choice = list => list[Math.floor(Math.random() * list.length)];

// Beta conversion of the six existing hero archetypes. These are deliberately isolated
// from the Java save/combat values while real-time pacing is tested.
const HEROES = {
  warrior: {name:"战士", key:"WARRIOR · IRON WILL", hp:220, attack:32, passive:"铁意：每场战斗开始时获得 70 护盾", skills:[
    ["斩击","100% 攻击 · 0.65 秒",650,"damage",1], ["破甲斩","240% 攻击 · 3 秒",3000,"sunder",2.4], ["铁壁","获得 48 护盾 · 6 秒",6000,"shield",48], ["战地包扎","恢复 30 生命 · 8 秒",8000,"heal",30]
  ]},
  mage: {name:"法师", key:"MAGE · MANA FLOW", hp:188, attack:35, passive:"法力流：技能冷却缩短 15%", skills:[
    ["奥术箭","100% 攻击 · 0.58 秒",580,"damage",1], ["奥术爆发","270% 攻击 + 灼烧 · 2.55 秒",2550,"burn",2.7], ["秘法屏障","获得 37 护盾 · 5 秒",5000,"shield",37], ["星火修复","恢复 25 生命 · 6.8 秒",6800,"heal",25]
  ]},
  ranger: {name:"游侠", key:"RANGER · HUNTER'S FOCUS", hp:214, attack:36, passive:"猎人专注：普通攻击有 15% 几率触发一次额外射击", skills:[
    ["速射","100% 攻击 · 0.48 秒",480,"damage",1], ["双重射击","两箭各 140% 攻击 · 3 秒",3000,"multi",1.4], ["毒牙箭","110% 攻击 + 中毒 · 3 秒",3000,"poison",1.1], ["烟幕","获得 35 护盾 · 5 秒",5000,"shield",35]
  ]},
  paladin: {name:"圣骑士", key:"PALADIN · DIVINE AEGIS", hp:226, attack:31, passive:"神圣壁垒：每 4 秒获得 10 护盾", skills:[
    ["圣光挥击","100% 攻击 · 0.68 秒",680,"damage",1], ["神圣审判","215% 攻击、治疗并眩晕 · 3.6 秒",3600,"judgment",2.15], ["守护祷言","获得 50 护盾 · 6 秒",6000,"shield",50], ["净化","恢复 28 并清除异常 · 7 秒",7000,"cleanse",28]
  ]},
  necromancer: {name:"死灵法师", key:"NECROMANCER · CURSE", hp:200, attack:32, passive:"诅咒：敌人开场 8 秒内承受伤害 +25%", skills:[
    ["灵魂火","100% 攻击 · 0.63 秒",630,"damage",1], ["灵魂汲取","200% 攻击、吸取一半生命并中毒 · 3.4 秒",3400,"drain",2], ["骸骨护甲","获得 42 护盾 · 5.5 秒",5500,"shield",42], ["枯萎","造成 80% 攻击并施加诅咒 · 5 秒",5000,"curse",.8]
  ]},
  creator: {name:"造物主", key:"CREATOR · EXECUTION", hp:180, attack:31, passive:"执行：敌人低于 10% 最大生命时直接抹除", hidden:true, skills:[
    ["现实切割","100% 攻击 · 0.58 秒",580,"damage",1], ["现实撕裂","五次 100% 攻击 · 4 秒",4000,"rend",1], ["造物屏障","获得 44 护盾 · 5 秒",5000,"shield",44], ["重构","恢复 32 生命 · 7 秒",7000,"heal",32]
  ]}
};

// All 21 current codex creatures are represented in the real-time beta pool.
const ROSTER = [
  ["洞窟蝙蝠","EARLY",60,12,"Savage Bite","bite"],["深渊猎犬","EARLY",74,14,"Savage Bite","bite"],["迷失矿工","EARLY",80,12,"Pickaxe Crush","sunder"],["泥沼史莱姆","EARLY",73,11,"Mire Spit","poison"],["墓穴鼠","EARLY",66,15,"Mire Spit","poison"],
  ["暗影刺客","MID",100,21,"Shadow Combo","double"],["诅咒人偶","MID",108,18,"Cursed Hex","weak"],["白骨学者","MID",96,19,"Soul Bolt","burn"],["嗜血水蛭","MID",110,22,"Blood Drain","drain"],["镜像幽灵","MID",92,23,"Mirror Pulse","curse"],
  ["虚空潜行者","LATE",150,27,"Void Lash","curse"],["白骨收割者","LATE",162,25,"Reaper Sweep","sweep"],["恐惧怨灵","LATE",146,26,"Wraith Howl","weak"],["疫病携带者","LATE",155,24,"Toxic Burst","poison"],["钢铁魔像","LATE",178,24,"Iron Slam","sunder"],
  ["深渊恶魔","DEEP",200,33,"Abyssal Rend","burn"],["末日先驱","DEEP",212,31,"Doom Chant","curse"],["深渊巨蛇","DEEP",195,34,"Serpent Coil","poison"],["饥饿巨像","DEEP",238,32,"Colossus Crash","sweep"],
  ["圣遗物守卫","BOSS",420,40,"Guardian's Wrath","weak"],["深渊领主","BOSS",470,43,"Abyssal Nova","nova"]
].map(([name,zone,hp,attack,special,kind]) => ({name,zone,hp,attack,special,kind}));

let game;
function newGame() {
  return {hero:null,floor:0,plan:[],hp:0,max:0,shield:0,enemy:null,enemyHp:0,enemyMax:0,enemyShield:0,
    paused:true,finished:false,choice:false,last:performance.now(),nextEnemy:0,enemyCount:0,cd:[],gold:0,relics:[],
    power:1, burn:0, poison:0, playerPoison:0, weak:0, curse:0, sunder:0, stunned:0, paladinPulse:0, eventUsed:false};
}
function makePlan() {
  const byZone = zone => ROSTER.filter(monster => monster.zone === zone);
  return [choice(byZone("EARLY")),choice(byZone("EARLY")),elite(choice(byZone("MID"))),choice(byZone("MID")),choice(byZone("MID")),elite(choice(byZone("LATE"))),choice(byZone("LATE")),choice(ROSTER.filter(monster => monster.zone === "BOSS"))];
}
function elite(monster) { return {...monster, name:`精英 ${monster.name}`, elite:true}; }
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
function heroCard(id, hero) { return `<button class="card" data-hero="${id}"><span class="tag">${hero.key}</span><strong>${hero.name}</strong><small>生命 ${hero.hp} · 攻击 ${hero.attack}<br>${hero.skills.map(skill => skill[0]).join(" / ")}<br>${hero.passive}</small></button>`; }
function startRun(id) {
  game.hero=HEROES[id]; game.max=game.hero.hp; game.hp=game.max; game.plan=makePlan(); game.paused=false;
  game.paladinPulse=performance.now()+4000; hideOverlay(); beginFloor(); log(`${game.hero.name} 踏入深渊，第 1 层敌人出现。`);
}
function beginFloor() {
  const base=game.plan[game.floor]; game.enemy={...base};
  const floorScale=1+(game.floor*.13)+(game.enemy.elite?.26:0);
  game.enemyMax=Math.round(base.hp*floorScale); game.enemyHp=game.enemyMax; game.enemyShield=0; game.enemyCount=0;
  game.burn=0; game.poison=0; game.stunned=0; game.nextEnemy=performance.now()+1300;
  $("enemy-card").classList.remove("defeated"); $("enemy-name").textContent=base.name;
  if (game.hero === HEROES.warrior) game.shield=clamp(game.shield+70,game.max);
  if (game.hero === HEROES.necromancer) game.curse=8;
}
function physical(amount) { return Math.max(1, amount * game.power * (game.weak > 0 ? .8 : 1)); }
function enemyDamage(amount) { return Math.max(1, amount * (game.sunder > 0 ? 1.25 : 1)); }
function damageEnemy(amount, visual=true) {
  amount *= game.curse > 0 ? 1.25 : 1;
  const absorbed=Math.min(game.enemyShield,amount); game.enemyShield-=absorbed; game.enemyHp-=amount-absorbed;
  if (visual) float("enemy",amount,"damage");
}
function damageHero(amount, visual=true) {
  const absorbed=Math.min(game.shield,amount); game.shield-=absorbed; game.hp-=amount-absorbed;
  if (visual) float("player",amount,"damage");
}
function heal(amount) { const before=game.hp; game.hp=clamp(game.hp+amount,game.max); float("player",game.hp-before,"heal"); }
function float(target, amount, type) {
  const card=$(target+"-card"), value=document.createElement("span");
  card.classList.remove("hit"); void card.offsetWidth; card.classList.add("hit");
  value.className=`floating ${type}`; value.textContent=`${type === "heal" ? "+" : "−"}${Math.max(1,Math.round(amount))}`; value.style.left="50%"; value.style.top="42%"; card.append(value);
  setTimeout(()=>{card.classList.remove("hit");value.remove();},720); $("impact").classList.remove("show"); void $("impact").offsetWidth; $("impact").classList.add("show");
}
function cast(index, now) {
  if (!game.hero || game.paused || game.finished || game.choice || now < game.cd[index]) return;
  const skill=game.hero.skills[index], [name,,cooldown,type,value]=skill; game.cd[index]=now+cooldown;
  if (type === "damage") { damageEnemy(physical(game.hero.attack*value)); log(`你施放「${name}」。`); if (game.hero===HEROES.ranger && index===0 && Math.random()<.15) damageEnemy(physical(game.hero.attack/3)); }
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
  let multiplier=special?1.3:1, hits=1;
  if (special && enemy.kind === "double") { multiplier=.82; hits=2; }
  if (special && enemy.kind === "shield") { game.enemyShield=clamp(game.enemyShield+35,game.enemyMax); log(`${enemy.name} 使用「${enemy.special}」，获得 35 护盾。`); game.nextEnemy=now+900; return; }
  if (special && enemy.kind === "drain") { const before=game.hp; damageHero(enemyDamage(enemy.attack*multiplier)); const dealt=before-game.hp; game.enemyHp=clamp(game.enemyHp+dealt*.5,game.enemyMax); log(`${enemy.name} 使用「${enemy.special}」，并吸取生命。`); }
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
function checkEnd() {
  if (game.enemyHp<=0 && !game.choice && !game.finished) { game.enemyHp=0; game.choice=true; game.paused=true; $("enemy-card").classList.add("defeated"); game.gold+=18+game.floor*7+(game.enemy.elite?18:0); log(`${game.enemy.name} 被击败。获得金币，选择远征奖励。`); rewardChoice(); }
  if (game.hp<=0 && !game.finished) { game.hp=0; game.finished=true; game.paused=true; $("player-card").classList.add("defeated"); log("你被深渊击退。本次 Beta 远征结束。"); show(`<div class="modal"><h2>远征结束</h2><p>这不会影响正式游戏、成就或存档。你可以立即再试一次不同职业与遗物组合。</p><button class="card" id="again"><strong>重新开始测试</strong><small>重新选择职业与八层路线。</small></button><a class="back" href="/">← 返回正式远征</a></div>`); $("again").onclick=reset; }
}
const RELICS=[
  ["战栗之刃","所有伤害 +12%",()=>game.power*=1.12],["活力结晶","生命上限 +26，并恢复 26",()=>{game.max+=26;heal(26)}],["月影护符","获得 55 护盾",()=>game.shield=clamp(game.shield+55,game.max)],
  ["迅捷刻印","所有技能冷却 -10%",()=>game.cd=game.cd.map(end=>performance.now()+(end-performance.now())*.9)],["炽焰核心","灼烧伤害提高",()=>game.power*=1.06],["掠夺者印记","获得 35 金币",()=>game.gold+=35]
];
function rewardChoice() {
  const picks=[...RELICS].sort(()=>Math.random()-.5).slice(0,3);
  show(`<div class="modal"><h2>第 ${game.floor+1} 层完成</h2><p>每次胜利选择一件本次远征的遗物。随后会触发一次简短事件并推进下一层。</p><div class="cards">${picks.map(([name,desc],i)=>`<button class="card" data-relic="${i}"><span class="tag">RELIC</span><strong>${name}</strong><small>${desc}</small></button>`).join("")}</div></div>`);
  document.querySelectorAll("[data-relic]").forEach(button=>button.onclick=()=>takeRelic(picks[Number(button.dataset.relic)]));
}
function takeRelic(relic) {
  relic[2](); game.relics.push(relic[0]); hideOverlay();
  if (game.floor===7) { game.finished=true; log("深渊领主倒下。八层实时远征测试完成！"); show(`<div class="modal"><h2>深渊远征完成</h2><p>你完成了完整八层实时测试。这个版本已覆盖职业、21 怪物池、精英、双 Boss、异常、遗物与连续推关基础循环。</p><button class="card" id="again"><strong>再次远征</strong><small>重置测试数据，尝试新的敌人路线。</small></button><a class="back" href="/">← 返回正式远征</a></div>`); $("again").onclick=reset; return; }
  eventChoice();
}
function eventChoice() {
  const events=[
    ["篝火","休息：恢复 30% 最大生命",()=>heal(game.max*.3)],
    ["预言家","预见下一层：显示即将遭遇的怪物",()=>log(`预言家看见下一层：${game.plan[game.floor+1].name}。`)],
    ["赌徒","掷骰：获得 15–55 金币",()=>{const gold=15+Math.floor(Math.random()*41);game.gold+=gold;log(`赌徒给了你 ${gold} 金币。`)}]
  ];
  show(`<div class="modal"><h2>层间事件</h2><p>测试版中的事件会暂停战斗；选择后进入下一层。</p><div class="cards">${events.map(([name,desc],i)=>`<button class="card" data-event="${i}"><span class="tag">EVENT</span><strong>${name}</strong><small>${desc}</small></button>`).join("")}</div></div>`);
  document.querySelectorAll("[data-event]").forEach(button=>button.onclick=()=>{const event=events[Number(button.dataset.event)]; event[2](); game.floor++; game.choice=false; game.paused=false; hideOverlay(); beginFloor(); log(`${event[0]}结束。进入第 ${game.floor+1} 层。`);});
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
  $("hero-name").textContent=game.hero?.name||"选择一名英雄"; $("hero-role").textContent=game.hero?.key||"PLAYER"; $("hero-passive").textContent=game.hero?.passive||"职业被动将在这里显示";
  $("enemy-name").textContent=enemy?.name||"深渊正在等待"; $("enemy-role").textContent=enemy?`${enemy.zone}${enemy.elite?" · ELITE":""}`:"HOSTILE";
  $("floor-label").textContent=game.hero?`第 ${Math.min(game.floor+1,8)} / 8 层 · ${enemy?.name||"准备中"}`:"选择职业后开始";
  $("flow").classList.toggle("paused",game.paused); if(game.hero&&!game.paused&&!game.finished&&!game.choice) $("flow-text").textContent=game.stunned>0?`敌人眩晕 ${game.stunned.toFixed(1)} 秒`:`实时进行中 · ${Math.max(0,(game.nextEnemy-now)/1000).toFixed(1)} 秒后敌人行动`;
  $("gold").textContent=`金币 ${game.gold}`; $("relics").textContent=`遗物 ${game.relics.length}`;
  renderStatus($("hero-status"),[[game.shield?`🛡 护盾 ${Math.ceil(game.shield)}`:"",false],[game.playerPoison?`☠ 中毒 ${game.playerPoison.toFixed(1)}s`:"",true],[game.weak?`↓ 虚弱 ${game.weak.toFixed(1)}s`:"",true],[game.sunder?`⌁ 破甲 ${game.sunder.toFixed(1)}s`:"",true]]);
  renderStatus($("enemy-status"),[[game.enemyShield?`🛡 护盾 ${Math.ceil(game.enemyShield)}`:"",false],[game.burn?`🔥 灼烧 ${game.burn.toFixed(1)}s`:"",true],[game.poison?`☠ 中毒 ${game.poison.toFixed(1)}s`:"",true],[game.curse?`✦ 诅咒 ${game.curse.toFixed(1)}s`:"",true],[game.stunned?`✧ 眩晕 ${game.stunned.toFixed(1)}s`:"",true]]);
  $("intent").textContent=enemy ? (game.stunned>0?"敌人被眩晕，无法行动。":`意图：${game.enemyCount%3===2?enemy.special:"普通攻击"}`) : "敌人尚未苏醒。";
  renderSkills(now);
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
document.addEventListener("keydown",event=>{if(!event.repeat&&"1234".includes(event.key))cast(Number(event.key)-1,performance.now());});
reset(); requestAnimationFrame(tick);
