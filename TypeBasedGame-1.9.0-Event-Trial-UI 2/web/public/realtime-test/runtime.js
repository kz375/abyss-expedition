"use strict";
// All deadlines use simulation milliseconds. Pauses, hidden tabs and save/restore
// freeze the same clock, including cooldowns, telegraphs and summoned enemies.
let game=null, lastFrame=0, saveElapsed=0, saveError="", archiveKind=null;
const label=pair=>tx(pair[0],pair[1]);
const clone=value=>JSON.parse(JSON.stringify(value));
const hashSeed=text=>{let n=2166136261;for(const c of text){n^=c.charCodeAt(0);n=Math.imul(n,16777619);}return n>>>0||1;};
function random(){let x=game.rng;x^=x<<13;x^=x>>>17;x^=x<<5;game.rng=x>>>0;return game.rng/4294967296;}
function draw(pool,n){const copy=[...pool],out=[];while(copy.length&&out.length<n)out.push(copy.splice(Math.floor(random()*copy.length),1)[0]);return out;}
function emptyGame(){return {phase:"menu",revision:0,paused:true,clock:0,heroId:null,finished:false};}
function hero(){return HEROES[game.heroId];}
function difficulty(){return DIFFICULTIES[game.difficulty];}
const ACCOUNT_SLOTS=["weapon","armor","charm"];
const integerGearStats=new Set(["attack","max","defense","ward","startPotions"]);
function affixDef(slot,id){return GEAR_AFFIXES[slot]?.find(a=>a.id===id);}
function gearEffects(item){const out={};if(!item||!validGear(item))return out;const base=GEAR_BASE_BY_ID[item.baseId];out[base.main]=(out[base.main]||0)+base.value*item.scale;for(const roll of item.affixes){const def=affixDef(item.slot,roll.id);out[def.stat]=(out[def.stat]||0)+roll.value;}for(const key of Object.keys(out))if(integerGearStats.has(key))out[key]=Math.round(out[key]);return out;}
function validGear(item){
  if(!item||typeof item!=="object"||Array.isArray(item)||typeof item.id!=="string"||!/^[0-9a-f-]{36}$/i.test(item.id))return false;
  if(!Object.hasOwn(GEAR_BASE_BY_ID,item.baseId)||!Object.hasOwn(GEAR_QUALITIES,item.quality)||!ACCOUNT_SLOTS.includes(item.slot))return false;
  const base=GEAR_BASE_BY_ID[item.baseId],rule=Object.hasOwn(LOOT_RULES,item.sourceDifficulty)?LOOT_RULES[item.sourceDifficulty]:null;if(base.slot!==item.slot||!Array.isArray(item.affixes)||new Set(item.affixes.map(a=>a?.id)).size!==item.affixes.length)return false;
  if(item.quality==="legacy")return item.sourceDifficulty==="adventurer"&&item.scale===1&&item.affixes.length===0;
  if(!rule||!rule.qualities.some(([quality])=>quality===item.quality)||typeof item.scale!=="number"||!Number.isFinite(item.scale)||item.scale<rule.scale[0]||item.scale>rule.scale[1]||item.affixes.length<rule.affixes[0]||item.affixes.length>rule.affixes[1])return false;
  return item.affixes.every(roll=>{const def=affixDef(item.slot,roll?.id);if(!def)return false;const rawMin=def.range[0]*item.scale,rawMax=def.range[1]*item.scale,min=def.integer?Math.max(1,Math.round(rawMin)):rawMin,max=def.integer?Math.max(min,Math.round(rawMax)):rawMax;return typeof roll.value==="number"&&Number.isFinite(roll.value)&&roll.value>=min-1e-9&&roll.value<=max+1e-9&&(!def.integer||Number.isInteger(roll.value));});
}
function legacyGear(baseId){const base=GEAR_BASE_BY_ID[baseId];return base?{id:runId(),baseId,slot:base.slot,quality:"legacy",sourceDifficulty:"adventurer",scale:1,affixes:[]}:null;}
function migrateProfileGear(target){
  const old=Array.isArray(target.warehouse)?target.warehouse:[],converted=[],byBase={};
  for(const value of old){const item=typeof value==="string"?legacyGear(value):value;if(validGear(item)&&!converted.some(g=>g.id===item.id)){converted.push(clone(item));(byBase[item.baseId]??=item.id);}}
  const equipped=target.equipped&&typeof target.equipped==="object"&&!Array.isArray(target.equipped)?target.equipped:{};target.warehouse=converted;target.equipped={};
  for(const slot of ACCOUNT_SLOTS){let id=equipped[slot];if(typeof id==="string"&&GEAR_BASE_BY_ID[id])id=byBase[id];const item=converted.find(g=>g.id===id);target.equipped[slot]=item?.slot===slot?id:null;}
  const claims=target.claims&&typeof target.claims==="object"&&!Array.isArray(target.claims)?target.claims:{};target.claims=Object.fromEntries(Object.entries(claims).filter(([id])=>/^[0-9a-f-]{36}$/i.test(id)).map(([id,value])=>[id,Array.isArray(value)&&value.every(x=>typeof x==="string")?value:"complete"]));return target;
}
function accountLoadout(){return Object.fromEntries(ACCOUNT_SLOTS.map(slot=>{const id=profile.equipped?.[slot],item=profile.warehouse?.find(g=>g.id===id);return [slot,item?.slot===slot?clone(item):null];}));}
function weightedQuality(rule){let roll=random()*rule.qualities.reduce((n,[,weight])=>n+weight,0);for(const [quality,weight] of rule.qualities){roll-=weight;if(roll<0)return quality;}return rule.qualities.at(-1)[0];}
function generateGear(mode=game.difficulty){const rule=LOOT_RULES[mode],base=choice(GEAR_BASES),scale=rule.scale[0]+random()*(rule.scale[1]-rule.scale[0]),count=rule.affixes[0]+Math.floor(random()*(rule.affixes[1]-rule.affixes[0]+1));return {id:runId(),baseId:base.id,slot:base.slot,quality:weightedQuality(rule),sourceDifficulty:mode,scale,affixes:draw(GEAR_AFFIXES[base.slot],count).map(def=>{const low=def.range[0]*scale,high=def.range[1]*scale;if(def.integer){const min=Math.max(1,Math.round(low)),max=Math.max(min,Math.round(high));return {id:def.id,value:min+Math.floor(random()*(max-min+1))};}return {id:def.id,value:low+random()*(high-low)};})};}
function runId(){
  if(typeof globalThis.crypto?.randomUUID==="function")return crypto.randomUUID();
  const bytes=new Uint8Array(16);
  if(typeof globalThis.crypto?.getRandomValues==="function")crypto.getRandomValues(bytes);
  else for(let i=0;i<bytes.length;i++)bytes[i]=Math.floor(Math.random()*256);
  // This identifier deduplicates local progress; it is not an authentication token.
  bytes[6]=(bytes[6]&15)|64;bytes[8]=(bytes[8]&63)|128;
  const hex=[...bytes].map(b=>b.toString(16).padStart(2,"0")).join("");
  return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
}
function newRun(id,seed,mode){
  if(!HEROES[id]||!DIFFICULTIES[mode])return;
  // Keep the previous local record recoverable when starting a fresh run.
  let old=null;
  try{old=localStorage.getItem(BETA_RUN_KEY);}catch{}
  if(old)try{localStorage.setItem(BETA_RUN_KEY+"-backup",old);}catch{saveError=tx("旧存档备份失败，请先下载原始存档备份，再释放浏览器存储空间","Previous-save backup failed. Download the original backup, then free browser storage");renderScene();return;}
  const initial=Math.round(HEROES[id].hp*DIFFICULTIES[mode].hero);
  game={version:2,id:runId(),revision:0,seed:String(seed).trim().slice(0,100)||String(Date.now()),rng:1,difficulty:mode,heroId:id,
    phase:"battle",paused:false,finished:false,clock:0,floor:0,plan:[],hp:initial,baseMax:initial,max:initial,initialHp:initial,
    baseAttack:Math.round(HEROES[id].attack*DIFFICULTIES[mode].hero),baseDefense:0,shield:0,cd:[0,0,0,0],pulse:4000,
    gold:35,potions:1,relics:[],statuses:{poison:0,burn:0,weak:0,sunder:0},enemy:null,summons:[],nextEntity:0,
    floorStart:0,trialUsed:false,trial:null,returnFromBattle:false,rewardOffers:[],eventOffers:[],eventId:null,eventUsed:false,
    rewardReturn:"events",result:["",""],shopStock:[],bought:[],killCounts:{},revives:0,phoenixUsed:false,
    loadout:accountLoadout(),extractedItems:[],
    stats:{damage:0,taken:0,healed:0,casts:0,kills:0,floors:[],phases:[],startedAt:Date.now()}};
  game.rng=hashSeed(game.seed);
  reconcile();game.hp=game.max;game.potions+=effects().startPotions;
  game.plan=["EARLY","EARLY","MID","MID","LATE","LATE","DEEP","BOSS"].map((zone,i)=>({index:ROSTER.indexOf(choice(ROSTER.filter(m=>m.zone===zone))),elite:[2,4,6].includes(i)}));
  beginBattle(game.plan[0]);commit();
}
function effects(){const e={attack:game.baseAttack||0,defense:game.baseDefense||0,max:game.baseMax||0,power:1,cooldown:1,ward:0,thorns:0,low:0,healing:1,burn:1,leech:0,crit:0,potion:1,goldBonus:0,bossPower:0,elitePower:0,damageReduction:0,startPotions:0,reset:false,phoenix:false,poison:false};
  for(const item of Object.values(game.loadout||{})){const stats=gearEffects(item);for(const key of ["attack","defense","max","ward","low","leech","crit","goldBonus","bossPower","elitePower","damageReduction","startPotions"])e[key]+=stats[key]||0;e.cooldown*=1-(stats.cooldownReduction||0);e.burn*=1+(stats.burnBonus||0);e.potion*=1+(stats.potionBonus||0);}
  for(const item of (game.relics||[]).map(id=>RELIC_BY_ID[id])){if(!item)continue;for(const key of ["attack","defense","max","ward","thorns","low","leech","crit","goldBonus"])e[key]+=item[key]||0;for(const key of ["power","cooldown","healing","burn","potion"])e[key]*=item[key]||1;for(const key of ["reset","phoenix","poison"])e[key]||=!!item[key];}
  e.cooldown=Math.max(.6,e.cooldown*(game.heroId==="mage"?.85:1));e.crit=Math.min(.5,e.crit);e.leech=Math.min(.2,e.leech);e.damageReduction=Math.min(.3,e.damageReduction);return e;
}
function reconcile(){game.max=Math.max(game.initialHp,effects().max);game.hp=clamp(game.hp,game.max);game.shield=clamp(game.shield,game.max);}
function gainGold(value){const gain=Math.floor(value*difficulty().gold*(1+effects().goldBonus));game.gold+=gain;return gain;}
function heal(value){if(game.hp<=0)return 0;const actual=Math.max(0,Math.min(game.max-game.hp,value*effects().healing));game.hp+=actual;game.stats.healed+=actual;if(actual>.5)feedback("player",actual,"heal");return actual;}
function addWard(value){game.shield=clamp(game.shield+value,game.max);}
function grantRelic(id){if(!RELIC_BY_ID[id]||game.relics.includes(id))return false;const r=RELIC_BY_ID[id];game.relics.push(id);reconcile();if(r.max)heal(r.max);if(r.gold)game.gold+=r.gold;return true;}
function makeEnemy(index,elite=false,minion=false){const base=ROSTER[index],d=difficulty(),scale=(1+game.floor*.13)*(elite?1.35:1);
  const max=Math.round(minion?base.hp*d.hp*(1+game.floor*.1)*.5:Math.max(base.hp*d.hp*scale,game.initialHp*1.1));
  return {uid:++game.nextEntity,index,name:base.name,elite,minion,zone:minion?"SUMMON":base.zone,max,hp:max,shield:0,
    attack:base.attack*d.attack*(elite?1.2:1)*(minion?.55:1),defense:base.zone==="BOSS"?4:elite?3:0,speed:MONSTER_MODULES[index].speed,
    next:game.clock+(minion?2400:1600),count:0,rotation:0,phase:1,phaseMax:max,weakened:0,telegraph:null,stunned:0,counter:0,rage:0,poison:0,burn:0,curse:game.heroId==="necromancer"?8:0};
}
function beginBattle(plan,ambush=false){
  game.enemy=makeEnemy(plan.index,plan.elite);game.summons=[];game.phase="battle";game.paused=false;game.returnFromBattle=ambush;
  game.phoenixUsed=false;game.floorStart=game.clock;game.statuses={poison:0,burn:0,weak:0,sunder:0};
  if(effects().reset)game.cd=[0,0,0,0];game.pulse=game.clock+4000;addWard(effects().ward+(game.heroId==="warrior"?70:0));
  game.result=[""," "];
}
function spawnSummon(){if(game.summons.length>=2)return;const index=Math.floor(random()*ROSTER.length);game.summons.push(makeEnemy(index,random()<.05,true));}
function target(){return game.summons[0]||game.enemy;}
function damageHero(amount,reflect=true,continuous=false){if(game.hp<=0)return 0;const e=effects(),incoming=(continuous?Math.max(0,amount):Math.max(1,amount-e.defense))*(game.statuses.sunder>0?1.25:1)*(1-e.damageReduction),absorbed=Math.min(game.shield,incoming);game.shield-=absorbed;
  const dealt=Math.min(game.hp,incoming-absorbed);game.hp-=dealt;game.stats.taken+=dealt;if(dealt>.5)feedback("player",dealt,"damage");
  if(reflect&&dealt&&effects().thorns)damageEnemy(dealt*effects().thorns,false,false);
  if(game.hp<=0&&effects().phoenix&&!game.phoenixUsed){game.phoenixUsed=true;game.revives++;game.hp=game.max*.4;game.stats.healed+=game.hp;}
  return dealt;
}
function damageEnemy(amount,direct=true,leech=true){const m=target();if(!m||m.hp<=0)return 0;
  amount=Math.max(0,amount)*(m.curse>0?1.25:1);if(direct)amount=Math.max(1,amount-m.defense);
  const absorbed=Math.min(m.shield,amount);m.shield-=absorbed;const dealt=Math.min(m.hp,amount-absorbed);m.hp-=dealt;game.stats.damage+=dealt;
  if(direct&&dealt>.5)feedback("enemy",dealt,"damage");if(leech&&effects().leech)heal(dealt*effects().leech);
  if(direct&&m.counter>0&&m.hp>0)damageHero(m.attack*.6,false);
  if(game.heroId==="creator"&&m.hp>0&&m.hp/m.max<.1){game.stats.damage+=m.hp;m.hp=0;}
  if(m.hp<=0&&m.minion){recordKill(m);game.summons=game.summons.filter(e=>e!==m);const boss=game.enemy;if(boss.zone==="BOSS"){
    boss.weakened++;const ratio=boss.hp/boss.max;boss.max=Math.max(1,boss.max*.97);boss.hp=ratio*boss.max;boss.attack*=.97;boss.defense*=.97;boss.speed*=.97;boss.shield*=.97;
  }}else if(m.hp>0&&m.zone==="BOSS")advanceBoss(m);
  return dealt;
}
function advanceBoss(m){const phase=m.hp/m.max<=.33?3:m.hp/m.max<=.66?2:1;while(m.phase<phase){m.phase++;const ratio=m.hp/m.max;m.max*=1.15;m.hp=ratio*m.max;m.attack*=1.15;m.defense*=1.15;m.speed*=1.15;m.shield*=1.15;game.stats.phases.push({floor:game.floor+1,phase:m.phase,time:game.clock});spawnSummon();}}
function skillCooldown(index){const ms=hero().skills[index][2]*effects().cooldown;return index===1&&game.heroId==="paladin"?Math.max(2800,ms):ms;}
function cast(index){if(game.phase!=="battle"||game.paused||archiveKind||!Number.isInteger(index)||index<0||index>3||game.cd[index]>game.clock)return false;
  const [, , ,type,value]=hero().skills[index],e=effects();game.cd[index]=game.clock+skillCooldown(index);game.stats.casts++;
  const hit=(multiplier=1)=>{const foe=target(),hunt=1+(foe?.zone==="BOSS"?e.bossPower:0)+(foe?.elite?e.elitePower:0);return damageEnemy(e.attack*value*multiplier*e.power*hunt*(game.hp/game.max<.35?1+e.low:1)*(game.statuses.weak>0?.8:1)*(random()<e.crit?1.5:1));};
  const m=target();let dealt=0;
  if(!["shield","rewind","purify","rewrite"].includes(type))dealt=hit(type==="finisher"&&m.hp/m.max<.35?2:1);
  if(type==="damage"&&game.heroId==="ranger"&&random()<.15)hit();
  if(type==="multi")hit();if(type==="rend")for(let n=0;n<4;n++)hit();
  if(type==="arcane"&&m.burn>0)damageEnemy(e.attack*.75*e.power);
  if(type==="cleave"&&m.hp>0)m.curse=Math.max(m.curse,1.5);
  if(type==="smite")addWard(7);
  if(type==="soulward")addWard(dealt*.45);
  if(["sunder","judgment"].includes(type)&&m.hp>0){m.stunned=type==="judgment"?1:1.3;m.telegraph=null;m.next=Math.max(m.next,game.clock+1800);}
  if(type==="shield")addWard(value);
  if(type==="purify"){game.statuses={poison:0,burn:0,weak:0,sunder:0};addWard(value);}
  if(type==="rewind")for(let i=0;i<3;i++)game.cd[i]=game.clock;
  if(type==="rewrite"){m.shield=0;m.counter=0;m.rage=0;m.telegraph=null;for(let i=0;i<3;i++)game.cd[i]=game.clock;}
  if(type==="burn")m.burn=5;if(type==="soulfire")m.burn=Math.max(m.burn,3.5);if(type==="poison"||index===0&&e.poison)m.poison=6;
  if(type==="curse")m.curse=8;
  // Ultra Nightmare: each player basic attack calls one random species, 5% elite.
  if(index===0&&game.difficulty==="ultra"&&game.enemy.zone==="BOSS"&&game.enemy.hp>0)spawnSummon();
  checkEnd();saveRun();return true;
}
function monsterMove(m){let moves=MONSTER_MODULES[m.index].moves;
  if(m.zone==="BOSS"&&m.phase===2)moves=m.index===19?["shield","summon","sunder"]:["summon","dispel","burn"];
  if(m.zone==="BOSS"&&m.phase===3)moves=m.index===19?["counter","charge","sweep"]:["nova","rage","drain"];
  return moves[m.rotation%moves.length];
}
function performMove(m,action){if(m.hp<=0||game.hp<=0)return;const attack=m.attack*(m.rage>0?1.3:1);
  if(action==="shield")m.shield=clamp(m.shield+m.max*.12,m.max);
  else if(action==="heal")m.hp=clamp(m.hp+m.max*.1,m.max);
  else if(action==="counter")m.counter=3;
  else if(action==="rage")m.rage=6;
  else if(action==="summon"){if(!m.minion)spawnSummon();else m.shield=clamp(m.shield+m.max*.1,m.max);}
  else{
    if(action==="dispel")game.shield*=.25;
    const damage=damageHero(attack*(action==="charge"?1.9:action==="nova"?1.5:action==="double"?.7:1.15));
    if(action==="double"&&game.hp>0)damageHero(attack*.7);
    if(action==="drain"&&m.hp>0)m.hp=clamp(m.hp+damage*.5,m.max);
    if(action==="poison")game.statuses.poison=6;if(action==="burn")game.statuses.burn=5;
    if(action==="weak")game.statuses.weak=5;
    if(["sunder","curse","sweep","nova"].includes(action))game.statuses.sunder=5;
    if(action==="sweep")game.shield*=.65;
  }
}
function enemyStep(m){if(m.hp<=0||m.stunned>0||game.clock<m.next)return;
  if(m.telegraph){const action=m.telegraph;m.telegraph=null;performMove(m,action);m.rotation++;m.count++;m.next=game.clock+2200/m.speed;}
  else if(m.count%3===2){m.telegraph=monsterMove(m);m.next=game.clock+1100;}
  else{damageHero(m.attack*(m.rage>0?1.3:1));m.count++;m.next=game.clock+1900/m.speed;}
}
function step(ms){if(!game||game.phase!=="battle"||game.paused||archiveKind||game.finished)return;
  // Slow the shared simulation during telegraphs: player input remains immediate.
  const slow=[game.enemy,...game.summons].some(m=>m.telegraph&&m.stunned<=0);const dt=Math.min(80,Math.max(0,ms))*(slow?.55:1),s=dt/1000;game.clock+=dt;
  const m=target(),burnTime=Math.min(s,m.burn),poisonTime=Math.min(s,m.poison),heroPoisonTime=Math.min(s,game.statuses.poison),heroBurnTime=Math.min(s,game.statuses.burn);
  for(const k of Object.keys(game.statuses))game.statuses[k]=Math.max(0,game.statuses[k]-s);
  game.shield=Math.max(0,game.shield-s*1.6);
  for(const m of [game.enemy,...game.summons]){m.shield=Math.max(0,m.shield-s*1.2);for(const k of ["stunned","rage","counter","poison","burn","curse"])m[k]=Math.max(0,m[k]-s);}
  if(burnTime>0)damageEnemy(burnTime*5*effects().power*effects().burn,false);if(poisonTime>0&&target()===m)damageEnemy(poisonTime*4,false);
  if(heroPoisonTime>0)damageHero(heroPoisonTime*4,false,true);if(heroBurnTime>0)damageHero(heroBurnTime*5,false,true);
  checkEnd();if(game.phase!=="battle")return;
  if(game.heroId==="paladin"&&game.clock>=game.pulse){addWard(10);game.pulse=game.clock+4000;}
  for(const entity of [game.enemy,...game.summons]){enemyStep(entity);checkEnd();if(game.phase!=="battle")break;}
}
function recordKill(m){game.stats.kills++;game.killCounts[m.name]=(game.killCounts[m.name]||0)+1;}
function syncProfile(){
  if(!game.heroId)return;profile.progress||={};const old=profile.progress[game.id]||{kills:{}};
  for(const [name,count] of Object.entries(game.killCounts)){const delta=Math.max(0,count-(old.kills[name]||0));if(delta){if(!profile.kills[name])game.gold+=10;profile.kills[name]=(profile.kills[name]||0)+delta;}}
  // Ledger makes save refresh/re-import idempotent on this device.
  profile.progress[game.id]={kills:{...old.kills,...Object.fromEntries(Object.entries(game.killCounts).map(([k,v])=>[k,Math.max(v,old.kills[k]||0)]))}};
  const a=profile.achievements;a.first_blood=Object.keys(profile.kills).length>0;a.collector=Object.keys(profile.kills).length>=10;a.full_codex=ROSTER.every(m=>profile.kills[m.name]>0);
  if(game.enemy?.elite&&game.enemy.hp<=0)a.elite_hunter=true;
  profile.relics=[...new Set([...(profile.relics||[]),...game.relics])];a.all_relics=RELICS.every(r=>profile.relics.includes(r.id));
  if(game.phase==="victory"){a.boss_slayer=true;a["class_"+game.heroId]=true;if(!game.revives)a.no_death=true;if(!game.stats.healed)a.no_heal=true;a["boss_"+game.enemy.index]=true;}
}
function checkEnd(){if(game.phase!=="battle")return;
  if(game.hp<=0){game.hp=0;finish(false);return;}
  if(game.enemy.hp<=0){game.enemy.hp=0;recordKill(game.enemy);gainGold(18+game.floor*7+(game.enemy.elite?18:0));
    if(!game.returnFromBattle)game.stats.floors.push({floor:game.floor+1,ms:game.clock-game.floorStart});
    game.rewardReturn=game.returnFromBattle?"advance":game.floor===7?"victory":"events";openReward();commit();
  }
}
function extractAccountItems(){profile.claims||={};profile.warehouse||=[];if(Object.hasOwn(profile.claims,game.id)){const ids=profile.claims[game.id];game.extractedItems=Array.isArray(ids)?ids.map(id=>profile.warehouse.find(g=>g.id===id)).filter(Boolean).map(clone):[];return game.extractedItems;}const rule=LOOT_RULES[game.difficulty],count=rule.count[0]+Math.floor(random()*(rule.count[1]-rule.count[0]+1)),items=Array.from({length:count},()=>generateGear());profile.warehouse.push(...items);profile.claims[game.id]=items.map(item=>item.id);game.extractedItems=clone(items);return game.extractedItems;}
function finish(win){game.phase=win?"victory":"defeat";game.finished=true;game.paused=true;if(win)extractAccountItems();syncProfile();writeProfile();clearRun();commit();}
function openReward(all=false){game.phase="reward";game.paused=true;game.rewardOffers=draw(RELICS.filter(r=>!game.relics.includes(r.id)).map(r=>r.id),all?RELICS.length:3);}
function selectRelic(id){if(game.phase!=="reward"||!game.rewardOffers.includes(id))return;grantRelic(id);game.rewardOffers=[];afterReward();commit();}
function afterReward(){if(game.rewardReturn==="victory")return finish(true);if(game.rewardReturn==="advance")return advanceFloor();if(game.rewardReturn==="trial"){heal(game.max);return eventResult(["核心破碎，生命已恢复，遗物已领取","Cores shattered. Health restored and relic claimed"]);}openEvents();}
function openEvents(){game.phase="events";game.paused=true;game.eventUsed=false;game.eventId=null;game.eventOffers=draw(EVENT_CATALOG.filter(e=>e[0]!=="trial"||!game.trialUsed).map(e=>e[0]),4);}
function chooseEvent(id){if(game.phase!=="events"||game.eventUsed||!game.eventOffers.includes(id))return;game.eventUsed=true;game.eventId=id;game.phase=id==="shop"?"shop":"event";if(id==="shop"){game.shopStock=draw(SHOP.map(s=>s.id),3);game.bought=[];}commit();}
function advanceFloor(){game.floor++;game.returnFromBattle=false;beginBattle(game.plan[game.floor]);}
function eventResult(result){game.result=result;game.phase="result";if(game.hp<=0)finish(false);}
function eventBattle(){const pool=ROSTER.map((m,i)=>({m,i})).filter(({m})=>m.zone===["EARLY","EARLY","MID","MID","LATE","LATE","DEEP","BOSS"][game.floor]);beginBattle({index:choice(pool).i,elite:true},true);}
function spendGold(cost){if(game.gold<cost)return false;game.gold-=cost;return true;}
function spendHealth(cost){if(game.hp<=cost)return false;game.hp-=cost;game.stats.taken+=cost;return true;}
function randomRelic(){const pool=RELICS.filter(r=>!game.relics.includes(r.id));if(pool.length)grantRelic(choice(pool).id);else gainGold(40);}
function eventOptions(){const f=game.floor+1,g=game.gold;const leave={text:["离开","Leave"],run:()=>eventResult(["继续向深渊前进","Continue into the abyss"])};
  const opt=(zh,en,run,enabled=true)=>({text:[zh,en],run,enabled});
  const done=(run,text=["事件已结算","Event resolved"])=>()=>{run();if(game.phase==="event")eventResult(text);};
  const stat=n=>game.baseAttack+=n;
  const options={
    camp:[opt("休息：恢复 35% 生命","Rest: heal 35%",done(()=>heal(game.max*.35))),opt("训练：失去 12 生命，攻击 +2","Train: pay 12 health; attack +2",done(()=>{if(spendHealth(12))stat(2)}),game.hp>12),opt("调查余烬","Investigate embers",done(()=>{const roll=random();if(roll<.45){heal(22);addWard(15)}else if(roll<.8){if(spendHealth(10))stat(3)}else{game.potions++;game.baseDefense++}}))],
    shrine:[opt("献祭 15 生命：攻击 +3","Offer 15 health: attack +3",done(()=>{if(spendHealth(15))stat(3)}),game.hp>15),opt("祈祷：恢复 20 生命","Pray: heal 20",done(()=>heal(20)))],
    chest:[opt("开箱：70% 金币，30% 精英伏击","Open: 70% gold, 30% elite ambush",done(()=>{if(random()<.7)gainGold(25+f*8);else eventBattle()}))],
    healer:[opt("12 金币：药瓶 +1","12 gold: +1 healing bottle",done(()=>{if(spendGold(12))game.potions++}),g>=12),opt("冒险祝福：防御或损失生命换金币","Risk blessing: defense or lose health for gold",done(()=>{if(random()<.5)game.baseDefense+=2;else{damageHero(18,false);gainGold(35)}}))],
    spring:[opt("饮用：55% 增强生命，否则受到 16 伤害","Drink: 55% vitality, otherwise take 16 damage",done(()=>{if(random()<.55){game.baseMax+=5;reconcile();heal(40)}else damageHero(16,false)}))],
    adventurer:[opt("救人：60% 金币与药瓶，否则精英伏击","Rescue: 60% gold and bottle, else elite ambush",done(()=>{if(random()<.6){gainGold(30+f*6);game.potions++}else eventBattle()}))],
    gambler:[15,40,70].map(stake=>opt(`下注 ${stake} 金币`,`Bet ${stake} gold`,()=>{if(g<stake)return;const a=1+Math.floor(random()*6),b=1+Math.floor(random()*6);if(a>b)gainGold(stake);if(a<b)spendGold(stake);eventResult([`你 ${a} 点，对手 ${b} 点：${a>b?"获胜":a<b?"失败":"平局"}`,`You rolled ${a}, opponent ${b}: ${a>b?"win":a<b?"loss":"tie"}`]);},g>=stake)),
    library:[opt("研读：攻击 +1","Study: attack +1",done(()=>stat(1))),opt("冥想：防御 +1","Meditate: defense +1",done(()=>game.baseDefense++))],
    well:[opt("投入 25 金币：随机祝福","Offer 25 gold: random blessing",done(()=>{if(!spendGold(25))return;const r=random();if(r<.45)randomRelic();else if(r<.7)stat(2);else if(r<.85)game.baseDefense+=2;else{game.baseMax+=20;reconcile();heal(20)}}),g>=25)],
    cards:[opt("25 金币抽牌：金币、补给或诅咒","Draw for 25 gold: coins, supplies or curse",done(()=>{if(!spendGold(25))return;const r=random();if(r<.45)gainGold(60);else if(r<.8){game.potions++;addWard(20)}else damageHero(14,false)}),g>=25)],
    oracle:[1,2].map(n=>opt(`预见后 ${n} 层`,`See ${n} floor(s) ahead`,()=>{const p=game.plan[game.floor+n];const m=p?ROSTER[p.index]:null;eventResult(m?[`第 ${game.floor+n+1} 层：${m.name}${p.elite?"（精英）":""}`,`Floor ${game.floor+n+1}: ${MONSTER_EN[m.name]}${p.elite?" (Elite)":""}`]:["前方已是终点","No more floors lie ahead"])})),
    curator:[opt(`${55+f*10} 金币：三选一遗物`,`${55+f*10} gold: choose one of three relics`,()=>{if(!spendGold(55+f*10))return;game.rewardReturn="advance";openReward()},g>=55+f*10&&game.relics.length<RELICS.length)],
    rift:[opt("进入裂隙：挑战精英","Enter: challenge an elite",eventBattle)],
    forge:[opt("失去 12 生命：攻击 +4","Lose 12 health: attack +4",done(()=>{if(spendHealth(12))stat(4)}),game.hp>12),opt("失去 12 生命：防御 +4","Lose 12 health: defense +4",done(()=>{if(spendHealth(12))game.baseDefense+=4}),game.hp>12)],
    altar:[opt("30 金币：生命上限 +14，恢复 20","30 gold: max health +14; heal 20",done(()=>{if(spendGold(30)){game.baseMax+=14;reconcile();heal(20)}}),g>=30),opt(`获得 ${30+f*3} 护盾`,`Gain ${30+f*3} ward`,done(()=>addWard(30+f*3)))],
    caravan:[opt(`${20+f*3} 金币：药瓶与护盾`,`${20+f*3} gold: bottle and ward`,done(()=>{if(spendGold(20+f*3)){game.potions++;addWard(18+f*3)}}),g>=20+f*3)],
    idol:[opt(`献祭 10 生命：${45+f*5} 基础金币`,`Offer 10 health: ${45+f*5} base gold`,done(()=>{if(spendHealth(10))gainGold(45+f*5)}),game.hp>10)],
    stalker:[opt("追踪神秘身影：双核试炼","Follow the shadow: Trial of Twin Cores",startTrial,!game.trialUsed)],
    trial:[opt("进入双核试炼：40 步，破坏两个核心","Enter Twin Cores: 40 moves, destroy two cores",startTrial,!game.trialUsed)]
  };return [...(options[game.eventId]||[]),leave];
}
function selectEventOption(i){if(game.phase!=="event")return;const option=eventOptions()[i];if(!option||option.enabled===false)return;option.run();commit();}
function shopDescription(id){const stat=game.difficulty==="ultra"?6:game.difficulty==="nightmare"?4:2;return {potion:["药瓶 +1","Healing bottle +1"],weapon:[`攻击 +${stat}`,`Attack +${stat}`],armor:[`防御 +${stat}`,`Defense +${stat}`],ward:["护盾 +25","Ward +25"],tonic:["生命上限 +12，恢复 20","Max health +12; heal 20"],smoke:["药瓶 +1，护盾 +12","Bottle +1; ward +12"]}[id];}
function buy(id){if(game.phase!=="shop"||!game.shopStock.includes(id)||game.bought.includes(id))return;const item=SHOP.find(s=>s.id===id);if(!spendGold(item.price))return;const stat=game.difficulty==="ultra"?6:game.difficulty==="nightmare"?4:2;
  if(id==="potion"||id==="smoke")game.potions++;if(id==="weapon")game.baseAttack+=stat;if(id==="armor")game.baseDefense+=stat;if(id==="ward")addWard(25);if(id==="smoke")addWard(12);if(id==="tonic"){game.baseMax+=12;reconcile();heal(20)}game.bought.push(id);commit();}
function usePotion(){if(game.phase!=="battle"||game.paused||archiveKind||game.potions<1||game.hp>=game.max)return;game.potions--;heal(game.max*.3*effects().potion);commit();}
// Twin Cores matches the Java board's push rules; rotations/mirrors preserve a
// known solution so random layouts do not require lucky edge teleports to win.
function startTrial(){if(game.trialUsed)return;game.trialUsed=true;game.trial=generateTrial();game.phase="trial";
}
function generateTrial(){
  for(let attempt=0;attempt<6;attempt++){const used=new Set(),drawCell=limit=>{const pool=[];for(let y=1;y<=limit;y++)for(let x=1;x<=limit;x++)if(!used.has(y*8+x))pool.push(y*8+x);const c=choice(pool);used.add(c);return c;};
    const monster=drawCell(5),t={monster,player:drawCell(6),boxes:[drawCell(6),drawCell(6)],cores:[drawCell(6),drawCell(6)],moves:0,shattered:-1};
    const path=solveTrial(t,6000);if(path&&path.length>=12)return t;
  }
  const turns=Math.floor(random()*4),mirror=random()<.5,transform=cell=>{let x=cell%8,y=Math.floor(cell/8);if(mirror)x=7-x;for(let i=0;i<turns;i++)[x,y]=[7-y,x];return y*8+x;};
  return {player:transform(25),monster:transform(27),boxes:[transform(26),transform(49)],cores:[transform(29),transform(45)],moves:0,shattered:-1};
}
// Bounded A*: excludes random relocations, so every accepted board has a
// <=40-move solution independent of favorable teleport rolls.
function solveTrial(initial,budget=18000){const dirs=[[0,-1],[0,1],[-1,0],[1,0]],dist=(a,b)=>Math.abs(a%8-b%8)+Math.abs(Math.floor(a/8)-Math.floor(b/8));
  const key=t=>[t.player,t.monster,...[...t.boxes].sort((a,b)=>a-b),...t.cores].join(",");
  const h=t=>!t.cores.length?0:Math.min(...t.cores.map(c=>dist(t.monster,c)))+(t.cores.length===2?dist(...t.cores):0);
  const heap=[],seen=new Map();const push=n=>{heap.push(n);let i=heap.length-1;while(i>0){const p=(i-1)>>1;if(heap[p].f<=n.f)break;heap[i]=heap[p];i=p;}heap[i]=n;};
  const pop=()=>{const first=heap[0],tail=heap.pop();if(heap.length){let i=0;while(i*2+1<heap.length){let child=i*2+1;if(child+1<heap.length&&heap[child+1].f<heap[child].f)child++;if(heap[child].f>=tail.f)break;heap[i]=heap[child];i=child;}heap[i]=tail;}return first;};
  push({t:initial,path:[],f:h(initial)});seen.set(key(initial),0);
  while(heap.length&&budget-->0){const {t,path}=pop();if(path.length>seen.get(key(t)))continue;if(!t.cores.length)return path;
    for(let d=0;d<4;d++){const [dx,dy]=dirs[d],next=tileOffset(t.player,dx,dy);if(next<0||next===t.monster)continue;const n={player:next,monster:t.monster,boxes:[...t.boxes],cores:[...t.cores]},b=t.boxes.indexOf(next);
      if(b>=0){const beyond=tileOffset(next,dx,dy);if(beyond<0||t.boxes.includes(beyond))continue;if(beyond===t.monster){const after=tileOffset(t.monster,dx,dy);if(after<0||edge(after)||t.boxes.includes(after))continue;n.monster=after;n.cores=n.cores.filter(c=>c!==after);}else if(edge(beyond))continue;n.boxes[b]=beyond;}
      const length=path.length+1,f=length+h(n),k=key(n);if(f>40||(seen.get(k)??Infinity)<=length)continue;seen.set(k,length);push({t:n,path:[...path,d],f});
    }
  }return null;
}
function tileOffset(cell,dx,dy){const x=cell%8+dx,y=Math.floor(cell/8)+dy;return x<0||x>7||y<0||y>7?-1:y*8+x;}
function edge(cell){return cell%8===0||cell%8===7||Math.floor(cell/8)===0||Math.floor(cell/8)===7;}
function freeTile(t,central=false){const cells=[];for(let y=1;y<=(central?5:6);y++)for(let x=1;x<=(central?5:6);x++){const c=y*8+x;if(c!==t.player&&c!==t.monster&&!t.boxes.includes(c)&&!t.cores.includes(c))cells.push(c);}return choice(cells);}
function trialMove(dx,dy){if(game.phase!=="trial"||archiveKind||Math.abs(dx)+Math.abs(dy)!==1)return false;const t=clone(game.trial),next=tileOffset(t.player,dx,dy);if(next<0||next===t.monster)return false;
  const box=t.boxes.indexOf(next);t.shattered=-1;
  if(box>=0){let beyond=tileOffset(next,dx,dy);if(beyond<0||t.boxes.includes(beyond))return false;if(beyond===t.monster){const after=tileOffset(t.monster,dx,dy);if(after<0||t.boxes.includes(after))return false;t.monster=after;t.boxes[box]=beyond;t.player=next;
      if(edge(after))t.monster=freeTile(t,true);else if(t.cores.includes(after)){t.shattered=after;t.cores=t.cores.filter(c=>c!==after);}
    }else{t.boxes[box]=beyond;t.player=next;if(edge(beyond)){const center=[36,28,35,27].find(c=>c!==t.player&&c!==t.monster&&!t.boxes.includes(c)&&!t.cores.includes(c));t.boxes[box]=center??freeTile(t);}}
  }else t.player=next;
  t.moves++;game.trial=t;if(!t.cores.length)settleTrial(true);else if(t.moves>=40)settleTrial(false);commit();return true;
}
function settleTrial(win){if(game.phase!=="trial")return;
  if(win){game.gold+=Math.floor(game.gold*.02);game.rewardReturn="trial";openReward(true);if(!game.rewardOffers.length){game.gold+=40;afterReward();}}
  else{const lost=game.relics.length?choice(game.relics):null;if(lost)game.relics=game.relics.filter(r=>r!==lost);reconcile();const before=game.hp;game.hp=Math.min(game.max,Math.max(game.initialHp,game.hp-Math.max(1,Math.ceil(game.hp*.1))));game.stats.healed+=Math.max(0,game.hp-before);game.gold-=Math.ceil(game.gold*.05);game.result=lost?[`试炼失败：当前生命 -10%（不低于职业初始值），金币 -5%，失去${RELIC_BY_ID[lost].name[0]}`,`Trial failed: current health -10% (initial-health floor), gold -5%, lost ${RELIC_BY_ID[lost].name[1]}`]:["试炼失败：当前生命 -10%（不低于职业初始值），金币 -5%","Trial failed: current health -10% (initial-health floor), gold -5%"] ;game.phase="result";}
}
function validRun(s){
  const finite=(n,min,max)=>typeof n==="number"&&Number.isFinite(n)&&n>=min&&n<=max;
  if(!s||s.version!==2||typeof s.id!=="string"||!/^[0-9a-f-]{36}$/i.test(s.id)||!Object.hasOwn(HEROES,s.heroId)||!Object.hasOwn(DIFFICULTIES,s.difficulty)||typeof s.seed!=="string"||s.seed.length>100)return false;
  if(!["battle","reward","events","event","shop","result","trial"].includes(s.phase)||s.finished||!finite(s.hp,.000001,1e7)||!finite(s.max,1,1e7)||s.hp>s.max||!finite(s.clock,0,1e12)||!Number.isInteger(s.floor)||s.floor<0||s.floor>7)return false;
  if(!Array.isArray(s.plan)||s.plan.length!==8||s.plan.some(p=>!Number.isInteger(p.index)||!ROSTER[p.index]))return false;
  if(!Array.isArray(s.relics)||new Set(s.relics).size!==s.relics.length||s.relics.some(id=>!RELIC_BY_ID[id])||!Array.isArray(s.cd)||s.cd.length!==4||s.cd.some(n=>!finite(n,0,1e12)))return false;
  if(s.loadout!=null&&(!s.loadout||ACCOUNT_SLOTS.some(slot=>s.loadout[slot]!=null&&(!validGear(s.loadout[slot])||s.loadout[slot].slot!==slot))))return false;
  if(s.extractedItems!=null&&(!Array.isArray(s.extractedItems)||s.extractedItems.some(item=>!validGear(item))))return false;
  if(!s.stats||!Array.isArray(s.stats.floors)||!Array.isArray(s.stats.phases)||!s.killCounts||typeof s.killCounts!=="object")return false;
  if(["paused","finished","trialUsed","returnFromBattle","eventUsed","phoenixUsed"].some(k=>typeof s[k]!=="boolean"))return false;
  if(s.stats.floors.some(f=>!f||!Number.isInteger(f.floor)||!finite(f.floor,1,8)||!finite(f.ms,0,1e12))||s.stats.phases.some(p=>!p||!Number.isInteger(p.floor)||!finite(p.floor,1,8)||!Number.isInteger(p.phase)||!finite(p.phase,2,3)||!finite(p.time,0,1e12)))return false;
  if(s.relics.some(id=>!Object.hasOwn(RELIC_BY_ID,id))||s.rewardOffers?.some(id=>!Object.hasOwn(RELIC_BY_ID,id)))return false;
  if(Object.entries(s.killCounts).some(([name,n])=>!MONSTER_EN[name]||!Number.isInteger(n)||n<0||n>10000))return false;
  for(const key of ["damage","taken","healed","casts","kills"])if(!finite(s.stats[key],0,1e12))return false;
  for(const key of ["baseMax","initialHp","baseAttack","baseDefense","gold","potions","shield","rng","pulse","nextEntity","floorStart","revision","revives"])if(!finite(s[key],0,1e12))return false;
  const validEnemy=m=>m&&Number.isInteger(m.index)&&ROSTER[m.index]&&m.name===ROSTER[m.index].name&&["hp","max","attack","defense","speed","next","count","rotation","phase","weakened","shield","stunned","counter","rage","poison","burn","curse"].every(k=>finite(m[k],0,1e12))&&m.max>0&&m.hp<=m.max&&m.speed>0&&["uid","count","rotation","phase","weakened"].every(k=>Number.isInteger(m[k]))&&m.phase>=1&&m.phase<=3&&(!m.telegraph||typeof m.telegraph==="string"&&Object.hasOwn(ACTION_NAMES,m.telegraph));
  if(!validEnemy(s.enemy)||!Array.isArray(s.summons)||s.summons.length>2||s.summons.some(m=>!validEnemy(m)))return false;
  if(s.enemy.zone!==ROSTER[s.enemy.index].zone||s.enemy.minion!==false||s.summons.some(m=>m.zone!=="SUMMON"||m.minion!==true))return false;
  if(s.phase==="battle"&&s.enemy.hp<=0||s.rng<=0||!Number.isInteger(s.rng)||s.rng>4294967295)return false;
  if(!s.statuses||["poison","burn","weak","sunder"].some(k=>!finite(s.statuses[k],0,60)))return false;
  if(!Array.isArray(s.rewardOffers)||s.rewardOffers.some(id=>!RELIC_BY_ID[id])||!Array.isArray(s.eventOffers)||s.eventOffers.some(id=>!EVENT_CATALOG.some(e=>e[0]===id)))return false;
  if(!["events","advance","victory","trial"].includes(s.rewardReturn)||!Array.isArray(s.result)||s.result.length!==2||s.result.some(x=>typeof x!=="string"||x.length>2000))return false;
  if(["event","shop"].includes(s.phase)&&!EVENT_CATALOG.some(e=>e[0]===s.eventId))return false;
  if(!Array.isArray(s.shopStock)||!Array.isArray(s.bought)||[...s.shopStock,...s.bought].some(id=>!SHOP.some(i=>i.id===id)))return false;
  if(s.phase==="trial"){const t=s.trial,cell=n=>Number.isInteger(n)&&n>=0&&n<64;if(!t||!cell(t.player)||!cell(t.monster)||!Array.isArray(t.boxes)||t.boxes.length!==2||t.boxes.some(c=>!cell(c))||!Array.isArray(t.cores)||!t.cores.length||t.cores.length>2||t.cores.some(c=>!cell(c))||!Number.isInteger(t.moves)||t.moves<0||t.moves>=40)return false;if(new Set([t.player,t.monster,...t.boxes]).size!==4)return false;}
  return true;
}
function saveEnvelope(){return {format:"abyss-beta",version:2,run:game,profile};}
function saveRun(){if(!game?.heroId||game.finished)return;syncProfile();try{localStorage.setItem(BETA_RUN_KEY,JSON.stringify(saveEnvelope()));writeProfile();saveError="";}catch{saveError=tx("保存失败：请立即导出备份（浏览器存储不可用或已满）","Save failed: export a backup now (storage unavailable or full)");} }
function migrateRunGear(run){if(!run||typeof run!=="object")return run;if(run.loadout&&typeof run.loadout==="object")for(const slot of ACCOUNT_SLOTS){const value=run.loadout[slot];if(typeof value==="string"){const item=legacyGear(value);run.loadout[slot]=item?.slot===slot?item:null;}}if(run.extractedItem!==undefined){run.extractedItems=[];delete run.extractedItem;}run.extractedItems??=[];return run;}
function loadRun(text){const data=JSON.parse(text);if(data.format!=="abyss-beta"||data.version!==2)throw new Error("Incompatible or invalid Beta save");migrateRunGear(data.run);if(!validRun(data.run))throw new Error("Incompatible or invalid Beta save");return clone(data.run);}
function restoreRun(text){try{const source=text||localStorage.getItem(BETA_RUN_KEY),parsed=JSON.parse(source),restored=loadRun(source),p=parsed.profile;
    profile.claims||={};profile.warehouse||=[];profile.equipped||={weapon:null,armor:null,charm:null};
    if(p&&typeof p==="object"){
      for(const m of ROSTER){const n=p.kills?.[m.name];if(Number.isInteger(n)&&n>=0&&n<=1e7)profile.kills[m.name]=Math.max(profile.kills[m.name]||0,n);}
      if(p.achievements&&typeof p.achievements==="object")for(const [id,value] of Object.entries(p.achievements))if(/^[a-z0-9_]{1,40}$/.test(id)&&value===true)profile.achievements[id]=true;
      profile.relics=[...new Set([...(Array.isArray(profile.relics)?profile.relics:[]),...(Array.isArray(p.relics)?p.relics.filter(id=>Object.hasOwn(RELIC_BY_ID,id)):[])])];
      const imported=migrateProfileGear(clone(p)),known=new Set(profile.warehouse.map(item=>item.id));for(const item of imported.warehouse)if(!known.has(item.id)){profile.warehouse.push(item);known.add(item.id);}
      for(const [run,items] of Object.entries(imported.claims))profile.claims[run]=items;
      if(typeof p.accountId==="string"&&/^[0-9a-f-]{36}$/i.test(p.accountId))profile.accountId=p.accountId;
      for(const slot of ACCOUNT_SLOTS){const id=imported.equipped[slot],item=profile.warehouse.find(g=>g.id===id);if(item?.slot===slot)profile.equipped[slot]=id;}
    }
    profile.progress||={};const prior=profile.progress[restored.id]?.kills||{};profile.progress[restored.id]={kills:{...prior,...Object.fromEntries(Object.entries(restored.killCounts).map(([k,v])=>[k,Math.max(v,prior[k]||0)]))}};
    game=restored;game.loadout||={weapon:null,armor:null,charm:null};game.extractedItems??=[];game.paused=true;archiveKind=null;commit();return true;
  }catch{saveError=tx("存档损坏或格式不兼容；旧版本请保留备份，不能安全猜测奖励阶段","Invalid or incompatible save; keep old backups. Reward state cannot be safely guessed");renderScene();return false;}}
function commit(){game.revision++;if(game.heroId&&!game.finished)saveRun();renderScene();}
function exportRun(){if(!game.heroId||game.finished)return;saveRun();const blob=new Blob([JSON.stringify(saveEnvelope(),null,2)],{type:"application/json"}),url=URL.createObjectURL(blob),a=document.createElement("a");a.href=url;a.download=`abyss-beta-${game.seed.replace(/[^a-zA-Z0-9_-]/g,"_")}-floor-${game.floor+1}.json`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
