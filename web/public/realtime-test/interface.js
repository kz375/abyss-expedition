"use strict";
const esc=value=>String(value??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
let skillSignature="",creatorUnlocked=false;
const nameOfSkill=s=>label(SKILL_NAMES.find(pair=>pair[0]===s[0])||[s[0],s[0]]);
function feedback(target,value,type){const card=$(target+"-card");if(!card||value<.5)return;const node=document.createElement("span");node.className=`floating ${type}`;node.textContent=`${type==="heal"?"+":"−"}${Math.round(value)}`;node.style.left="50%";node.style.top="40%";card.append(node);card.classList.add("hit");setTimeout(()=>{node.remove();card.classList.remove("hit")},650);}
function buttons(items,attribute){return items.map((item,i)=>`<button class="card" ${attribute}="${esc(item.id??i)}" ${item.disabled?"disabled":""}><strong>${esc(label(item.name))}</strong>${item.desc?`<small>${esc(label(item.desc))}</small>`:""}</button>`).join("");}
function modal(title,description,html){$("overlay").hidden=false;$("overlay").innerHTML=`<div class="modal" role="dialog" aria-modal="true" aria-labelledby="dialog-title"><h2 id="dialog-title">${esc(label(title))}</h2><p>${esc(label(description))}</p>${html}</div>`;}
function bind(selector,fn){const revision=game.revision;document.querySelectorAll(selector).forEach(button=>button.onclick=()=>{if(game.revision===revision&&!archiveKind)fn(button);});}
function hasLocalSave(){try{return !!localStorage.getItem(BETA_RUN_KEY)}catch{return false}}
function menu(){
  const previousSeed=$("seed")?.value||"",previousDifficulty=$("difficulty")?.value||"adventurer";
  modal(["选择远征职业","Choose an expedition class"],["种子只决定随机路线；恢复完整进度请使用续局或导入存档","Seeds determine random routes. Use Continue or Import to restore full progress"],`
    <div class="save-actions"><button id="menu-language">${profile.locale==="en"?"简体中文":"English"}</button><button id="menu-warehouse">${tx("仓库与装备","Warehouse & Loadout")}</button><button id="menu-codex">${tx("图鉴","Codex")}</button><button id="menu-achievements">${tx("成就","Achievements")}</button></div>
    ${saveError?`<p class="menu-error" role="alert">${esc(saveError)}</p>`:""}
    <div class="setup"><label>${tx("难度","Difficulty")}<select id="difficulty">${Object.entries(DIFFICULTIES).map(([id,d])=>`<option value="${id}" ${id==="adventurer"?"selected":""}>${esc(label(d.name))}</option>`).join("")}</select></label><label>${tx("路线种子（可留空）","Route seed (optional)")}<input id="seed" maxlength="100" placeholder="${tx("输入种子仅生成新路线","A seed starts a NEW route")}"></label></div>
    <div class="save-actions">${hasLocalSave()?`<button id="resume-run">${tx("继续本地远征","Continue local run")}</button><button id="raw-backup">${tx("下载原始存档备份","Download original save backup")}</button>`:""}<button id="import-menu">${tx("导入 Beta 存档","Import Beta save")}</button></div>
    <div class="cards">${Object.entries(HEROES).filter(([id])=>id!=="creator"||creatorUnlocked).map(([id,h])=>`<button class="card" data-hero="${id}"><strong>${esc(tx(h.name,h.enName))}</strong><small>${tx("基础生命","Base health")} ${h.hp} · ${tx("攻击","Attack")} ${h.attack}<br>${esc(tx(h.passive,h.enPassive))}</small></button>`).join("")}</div>
    ${creatorUnlocked?"":`<div class="secret"><input id="creator-code" placeholder="${tx("已知隐藏口令？在这里输入","Know a secret code? Enter it here")}" aria-label="${tx("隐藏角色口令","Secret character code")}"><button id="unlock-creator">${tx("确认","Confirm")}</button></div>`}<a class="back" href="/">← ${tx("返回正式版","Return to the main game")}</a>`);
  bind("[data-hero]",button=>{if(hasLocalSave()&&!confirm(tx("开始新远征会替换本地续局，确定吗？","Starting a new run replaces the local save. Continue?")))return;newRun(button.dataset.hero,$("seed").value,$("difficulty").value);});
  $("seed").value=previousSeed;$("difficulty").value=previousDifficulty;
  $("menu-language").onclick=()=>$("language").click();$("menu-warehouse").onclick=()=>openArchive("warehouse");$("menu-codex").onclick=()=>openArchive("codex");$("menu-achievements").onclick=()=>openArchive("achievements");
  $("raw-backup")?.addEventListener("click",()=>{try{const raw=localStorage.getItem(BETA_RUN_KEY),url=URL.createObjectURL(new Blob([raw],{type:"application/json"})),a=document.createElement("a");a.href=url;a.download="abyss-original-save-backup.json";a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}catch{saveError=tx("无法读取原存档","Cannot read original save");render();}});
  $("resume-run")?.addEventListener("click",()=>restoreRun());$("import-menu").onclick=()=>$("import-file").click();
  $("unlock-creator")?.addEventListener("click",()=>{if($("creator-code").value.trim().toLowerCase()==="kz"){creatorUnlocked=true;const seed=$("seed").value,d=$("difficulty").value;menu();$("seed").value=seed;$("difficulty").value=d;}});
}
function eventTitle(id){const e=EVENT_CATALOG.find(e=>e[0]===id);return e?[e[1],e[2]]:["事件","Event"];}
function renderScene(){if(!game)return;$("archive-layer").hidden=!archiveKind;applyLocale();render();
  if(game.phase==="menu"){menu();return;}
  if(game.phase==="battle"){$("overlay").hidden=true;return;}
  if(game.phase==="reward"){
    modal(["选择一件遗物","Choose one relic"],["奖励选项和当前阶段已经保存，刷新不会重新抽取","Offers and this reward stage are saved. Reloading does not reroll them"],`<div class="cards">${buttons(game.rewardOffers.map(id=>({id,name:RELIC_BY_ID[id].name,desc:RELIC_BY_ID[id].desc})),"data-relic")}</div>${!game.rewardOffers.length?`<button class="card" id="reward-continue">${tx("全部收集完成，继续","Collection complete — continue")}</button>`:""}`);
    bind("[data-relic]",b=>selectRelic(b.dataset.relic));$("reward-continue")?.addEventListener("click",()=>{if(game.phase!=="reward")return;afterReward();commit();});
  }else if(game.phase==="events"){
    modal(["四个随机事件，选择一个","Four random events — choose one"],["选择后其余三个关闭；事件内的选择均使用按钮","The other three close after selection. All event choices use buttons"],`<div class="cards">${buttons(game.eventOffers.map(id=>({id,name:eventTitle(id)})),"data-event")}</div>`);bind("[data-event]",b=>chooseEvent(b.dataset.event));
  }else if(game.phase==="event"){
    modal(eventTitle(game.eventId),["做出选择；可以离开，本层不能再选其他事件","Choose or leave. You cannot visit another event on this floor"],`<div class="cards">${buttons(eventOptions().map(o=>({name:o.text,disabled:o.enabled===false})),"data-option")}</div>`);bind("[data-option]",b=>selectEventOption(Number(b.dataset.option)));
  }else if(game.phase==="shop"){
    modal(["商店","Merchant"],[`金币 ${game.gold} · 每件商品限购一次`,`Gold ${game.gold} · Each item can be bought once`],`<div class="cards">${buttons(game.shopStock.map(id=>{const s=SHOP.find(s=>s.id===id);return {id,name:s.name,desc:[`${s.price} 金币 · ${shopDescription(id)[0]}`,`${s.price} gold · ${shopDescription(id)[1]}`],disabled:game.bought.includes(id)||game.gold<s.price}}),"data-buy")}</div><button class="card compact" id="shop-leave">${tx("离开商店","Leave shop")}</button>`);bind("[data-buy]",b=>buy(b.dataset.buy));$("shop-leave").onclick=()=>{if(game.phase!=="shop")return;eventResult(["补给完成","Supplies secured"]);commit();};
  }else if(game.phase==="result"){
    modal(["事件结果","Event result"],game.result,`<button class="card compact" id="next-floor">${tx("进入下一层","Enter next floor")}</button>`);$("next-floor").onclick=()=>{if(game.phase!=="result")return;advanceFloor();commit();};
  }else if(game.phase==="trial")renderTrial();
  else if(game.finished){
    const extracted=game.extractedItem?ACCOUNT_ITEM_BY_ID[game.extractedItem]:null;
    modal(game.phase==="victory"?["远征胜利","Expedition complete"]:["远征结束","Expedition ended"],extracted?[`已带回仓库：${extracted.name[0]}`,`Extracted to warehouse: ${extracted.name[1]}`]:["感谢你游玩深渊远征","Thank you for playing Abyss Expedition"],`${runSummary()}<button class="card compact" id="again">${tx("返回职业选择","Choose a new class")}</button>`);$("again").onclick=()=>{game=emptyGame();commit();};
  }
}
function skillDescription(skill,index){const type=skill[3],v=skill[4],cd=(skillCooldown(index)/1000).toFixed(2);const description={damage:[`${v*100}% 攻击`,`${v*100}% attack`],cleave:[`${v*100}% 攻击，短暂破甲`,`${v*100}% attack, brief sunder`],finisher:[`${v*100}% 攻击，低生命目标翻倍`,`${v*100}% attack, double vs low health`],arcane:[`${v*100}% 攻击，引爆灼烧`,`${v*100}% attack, detonates burn`],rewind:["刷新前三个技能","Reset the first three skills"],sunder:[`${v*100}% 攻击，打断`,`${v*100}% attack, interrupt`],burn:[`${v*100}% 攻击，灼烧`,`${v*100}% attack, burn`],multi:[`两次 ${v*100}% 攻击`,`2 × ${v*100}% attack`],poison:[`${v*100}% 攻击，中毒`,`${v*100}% attack, poison`],shield:[`${v} 护盾`,`${v} ward`],smite:[`${v*100}% 攻击，获得 7 护盾`,`${v*100}% attack, gain 7 ward`],judgment:[`${v*100}% 攻击，打断`,`${v*100}% attack, interrupt`],purify:[`清除异常，${v} 护盾`,`Cleanse debuffs, ${v} ward`],soulfire:[`${v*100}% 攻击，灼烧`,`${v*100}% attack, burn`],soulward:[`${v*100}% 攻击，伤害转护盾`,`${v*100}% attack, damage becomes ward`],curse:[`${v*100}% 攻击，诅咒`,`${v*100}% attack, curse`],rend:["五次 100% 攻击","5 × 100% attack"],rewrite:["清除敌方强化，刷新前三技能","Remove enemy buffs; reset first three skills"]};return `${label(description[type])} · ${cd}${tx("秒","s")}`;}
function renderSkills(){const signature=game.heroId+profile.locale;if(signature!==skillSignature){skillSignature=signature;$("skills").replaceChildren();if(game.heroId)hero().skills.forEach((skill,index)=>{const b=document.createElement("button");b.className="skill";b.innerHTML=`<span class="key">${index+1}</span><strong>${esc(nameOfSkill(skill))}</strong><small></small><span class="cool"></span>`;b.onclick=()=>cast(index);$("skills").append(b);});}
  if(!game.heroId)return;[...$("skills").children].forEach((b,index)=>{const left=Math.max(0,game.cd[index]-game.clock);b.disabled=game.phase!=="battle"||game.paused||!!archiveKind||left>0;b.classList.toggle("ready",left===0);b.querySelector(".cool").style.transform=`scaleX(${clamp(left/skillCooldown(index),1)})`;b.querySelector("small").textContent=skillDescription(hero().skills[index],index);});
}
function render(){if(!game)return;const m=game.enemy,active=!!game.heroId;
  $("hero-hp").style.transform=`scaleX(${active?game.hp/game.max:0})`;$("hero-shield").style.transform=`scaleX(${active?game.shield/game.max:0})`;
  $("enemy-hp").style.transform=`scaleX(${m?m.hp/m.max:0})`;$("enemy-shield").style.transform=`scaleX(${m?m.shield/m.max:0})`;
  $("hero-hp-text").textContent=active?`${Math.ceil(game.hp)} / ${Math.ceil(game.max)}`:"—";$("enemy-hp-text").textContent=m?`${Math.ceil(m.hp)} / ${Math.ceil(m.max)}`:"—";
  $("hero-shield-text").textContent=tx("护盾 ","Ward ")+Math.ceil(game.shield||0);$("enemy-shield-text").textContent=tx("护盾 ","Ward ")+Math.ceil(m?.shield||0);
  $("hero-name").textContent=active?tx(hero().name,hero().enName):tx("选择一名英雄","Choose a hero");$("hero-role").textContent=tx("玩家","PLAYER");$("hero-passive").textContent=active?tx(hero().passive,hero().enPassive):"";
  $("enemy-name").textContent=m?monsterName(m):tx("深渊正在等待","The abyss awaits");$("enemy-role").textContent=m?.zone==="BOSS"?tx(`首领 · 阶段 ${m.phase}`,`BOSS · PHASE ${m.phase}`):m?.elite?tx("精英","ELITE"):tx("敌人","HOSTILE");
  $("floor-label").textContent=active?`${tx("第 ","Floor ")}${game.floor+1} / 8 · ${label(difficulty().name)}`:tx("选择职业后开始","Choose a class to start");
  const warned=active&&[m,...game.summons].some(e=>e.telegraph);$("flow").classList.toggle("paused",game.paused||game.phase!=="battle");
  $("flow-text").textContent=game.paused||game.phase!=="battle"?tx("已暂停","Paused"):warned?tx("危险预警 · 时间减速","Danger · Slow time"):tx("实时战斗","Real-time battle");
  $("enemy-card").classList.toggle("charging",!!m?.telegraph);$("player-card").classList.toggle("defeated",game.phase==="defeat");$("enemy-card").classList.toggle("defeated",!!m&&m.hp<=0);
  $("intent").textContent=m?(m.telegraph?`${label(ACTION_NAMES[m.telegraph])} · ${Math.max(0,(m.next-game.clock)/1000).toFixed(1)}${tx("秒后释放：现在防御或打断","s: defend or interrupt now")}`:game.summons.length?tx("护卫在场：先击败护卫才能伤害首领","Guardians active: defeat them before the boss"):tx("下个机制：","Next mechanism: ")+label(ACTION_NAMES[monsterMove(m)])):"";
  $("gold").textContent=tx("金币 ","Gold ")+(game.gold||0);$("relics").textContent=tx("遗物 ","Relics ")+(game.relics?.length||0);
  $("pause").textContent=game.paused?tx("继续","Resume"):tx("暂停","Pause");$("pause").disabled=game.phase!=="battle"||!!archiveKind;
  $("potion").textContent=`${tx("药瓶","Healing Bottle")} (${game.potions||0})`;$("potion").disabled=game.phase!=="battle"||game.paused||!game.potions||!!archiveKind;
  $("export-save").disabled=!active||game.finished;
  $("journal").textContent=active?`${tx("种子","Seed")}: ${game.seed} · ${tx("已击败","Defeated")}: ${game.stats.kills} · ${tx("护卫上限 2","Guardian cap: 2")}`:tx("选择职业或导入存档开始","Choose a class or import a save");
  $("save-status").textContent=saveError||(active&&!game.finished?tx("自动保存中 · 可导出存档跨设备恢复 · 种子不包含进度","Autosaving · Export for cross-device recovery · Seeds do not contain progress"):tx("正式版存档保持不变 · 实时测试版 1.6.0","Main-game saves stay untouched · Real-time Beta 1.6.0"));
  const status=(element,entries)=>{const key=JSON.stringify(entries.map(([n,v])=>[n,Math.ceil(v)]));if(element.dataset.key===key)return;element.dataset.key=key;element.innerHTML=entries.filter(([,v])=>v>0).map(([n,v])=>`<span class="status">${esc(n)} ${Math.ceil(v)}s</span>`).join("");};
  status($("hero-status"),active?Object.entries(game.statuses).map(([k,v])=>[label(ACTION_NAMES[k]||[k,k]),v]):[]);
  status($("enemy-status"),m?[[tx("眩晕","Stun"),m.stunned],[tx("灼烧","Burn"),m.burn],[tx("中毒","Poison"),m.poison],[tx("反击","Counter"),m.counter],[tx("狂暴","Frenzy"),m.rage]]:[]);
  const summons=game.summons||[],signature=summons.map(s=>s.uid).join()+profile.locale;
  if($("summons").dataset.key!==signature){$("summons").dataset.key=signature;$("summons").innerHTML=summons.map(s=>`<div class="summon-row"><span>${esc(monsterName(s))}</span><b></b><i><em></em></i><small></small></div>`).join("");}
  [...$("summons").children].forEach((row,i)=>{const s=summons[i];row.querySelector("b").textContent=`${Math.ceil(s.hp)}/${Math.ceil(s.max)}`;row.querySelector("em").style.transform=`scaleX(${s.hp/s.max})`;row.querySelector("small").textContent=s.telegraph?tx("蓄力：","Charging: ")+label(ACTION_NAMES[s.telegraph]):label(ACTION_NAMES[monsterMove(s)]);});
  renderSkills();
}
function applyLocale(){document.documentElement.lang=profile.locale==="en"?"en":"zh-CN";document.title=tx("深渊远征 · 实时测试版","Abyss Expedition · Real-time Beta");
  $("language").textContent=profile.locale==="en"?"简体中文":"English";$("warehouse").textContent=tx("仓库","Warehouse");$("codex").textContent=tx("图鉴","Codex");$("achievements").textContent=tx("成就","Achievements");
  $("title").textContent=tx("深渊流战 · 八层远征","Abyss Flow · Eight-Floor Expedition");$("eyebrow").textContent=tx("深渊远征 · 实时测试版 1.6.0","ABYSS EXPEDITION · REAL-TIME BETA 1.6.0");
  $("sub").textContent=tx("自主释放技能，敌人独立行动；数字键或点击技能按钮","Cast freely while enemies act independently. Click skills or use keys 1–4");
  $("notice").textContent=tx("独立测试版：职业、怪物、遗物、四选一事件、商店、双核试炼与存档备份。实时数值仍需实战调优","Standalone Beta: classes, monsters, relics, one-of-four events, shops, Twin Cores and save backups. Real-time balance remains under playtesting");
  $("restart").textContent=tx("返回选角色","Character selection");$("export-save").textContent=tx("导出存档","Export save");$("import-save").textContent=tx("导入存档","Import save");
  document.querySelectorAll(".meter b").forEach(b=>b.textContent=tx("生命","Health"));
}
function runSummary(){const fastest=game.stats.floors.length?Math.min(...game.stats.floors.map(f=>f.ms))/1000:0;const rows=[
  [tx("造成伤害","Damage"),Math.round(game.stats.damage)],[tx("承受伤害","Taken"),Math.round(game.stats.taken)],[tx("治疗","Healing"),Math.round(game.stats.healed)],
  [tx("技能次数","Casts"),game.stats.casts],[tx("击败怪物","Defeats"),game.stats.kills],[tx("最快单层（秒）","Fastest floor (s)"),fastest.toFixed(1)]];
  return `<div class="run-summary">${rows.map(([name,value])=>`<span>${name}<b>${value}</b></span>`).join("")}</div><p>${tx("遗物组合","Relic build")}: ${game.relics.map(id=>esc(label(RELIC_BY_ID[id].name))).join(" · ")||"—"}</p><p>${tx("击败记录","Defeat record")}: ${Object.entries(game.killCounts).map(([name,count])=>`${esc(tx(name,MONSTER_EN[name]))} ×${count}`).join(" · ")||"—"}</p><p>${tx("首领阶段记录","Boss phase record")}: ${game.stats.phases.map(p=>`${p.phase} @ ${(p.time/1000).toFixed(1)}s`).join(" · ")||"—"}</p>`;
}
function achievementRows(){return [["first_blood","初入深渊","First Blood"],["elite_hunter","精英猎手","Elite Hunter"],["boss_slayer","深渊征服者","Abyss Conqueror"],["collector","击败十种怪物","Defeat Ten Species"],["full_codex","完整图鉴","Complete Codex"],["no_death","无复活通关","Win Without Revival"],["no_heal","无治疗通关","Win Without Healing"],["all_relics","收集全部遗物","Collect Every Relic"],...Object.entries(HEROES).map(([id,h])=>["class_"+id,h.name+"通关",h.enName+" Victory"]),["boss_19","击败圣遗物守卫","Defeat Relic Guardian"],["boss_20","击败深渊领主","Defeat Abyss Lord"]];}
function openArchive(kind){archiveKind=kind;renderArchive();render();}
function renderArchive(){const layer=$("archive-layer");layer.hidden=false;
  if(archiveKind==="warehouse"){
    const slots={weapon:tx("武器","Weapon"),armor:tx("防具","Armor"),charm:tx("护符","Charm")},active=!!game.heroId&&!game.finished;
    const equipped=ACCOUNT_SLOTS.map(slot=>{const item=ACCOUNT_ITEM_BY_ID[profile.equipped[slot]];return `<div class="archive-row"><strong>${slots[slot]} · ${item?esc(label(item.name)):tx("未装备","Empty")}</strong><small>${item?esc(label(item.desc)):tx("通关后可从仓库装备同槽位物品","Win expeditions to equip an item in this slot")}</small></div>`}).join("");
    const inventory=profile.warehouse.length?profile.warehouse.map(id=>{const item=ACCOUNT_ITEM_BY_ID[id],on=profile.equipped[item.slot]===id;return `<button class="archive-row equipment ${on?"equipped":""}" data-equip="${id}" ${active?"disabled":""}><strong>${on?"✓ ":""}${esc(label(item.name))}</strong><small>${slots[item.slot]} · ${esc(label(item.desc))}</small></button>`}).join(""):`<div class="archive-row"><strong>${tx("仓库为空","Warehouse empty")}</strong><small>${tx("完成八层远征后带回第一件永久装备","Complete an eight-floor expedition to extract your first permanent item")}</small></div>`;
    layer.innerHTML=`<div class="modal archive"><h2>${tx("远征仓库","Expedition Warehouse")}</h2><p>${tx("账号","Account")}: ${esc(profile.accountId)}<br>${active?tx("远征进行中：装备已锁定，下次开局生效","Run active: loadout is locked until the next expedition"):tx("点击仓库物品即可装备或卸下","Select an item to equip or unequip it")}</p><div class="warehouse-slots">${equipped}</div><div class="archive-list">${inventory}</div><button class="card compact" id="archive-close">${tx("返回","Return")}</button></div>`;
    layer.querySelectorAll("[data-equip]").forEach(button=>button.onclick=()=>{if(active)return;const item=ACCOUNT_ITEM_BY_ID[button.dataset.equip],slot=item.slot;profile.equipped[slot]=profile.equipped[slot]===item.id?null:item.id;writeProfile();renderArchive();});
    $("archive-close").onclick=()=>{archiveKind=null;layer.hidden=true;renderScene();};return;
  }
  const rows=archiveKind==="codex"?ROSTER.map((m,index)=>{const count=profile.kills[m.name]||0,module=MONSTER_MODULES[index];return `<div class="archive-row"><strong>${esc(monsterName(m))} · ${count}</strong><small>${tx("技能","Skills")}: ${module.moves.map(a=>label(ACTION_NAMES[a])).join(" / ")}<br>${tx("弱点","Counterplay")}: ${esc(label(module.weakness))}<br>${count?tx("已击败，首胜奖励已领","Defeated; first-defeat reward claimed"):tx("未击败 · 首次击败奖励 10 金币","Undefeated · First defeat grants 10 gold")}</small></div>`}).join(""):achievementRows().map(([id,zh,en])=>`<div class="archive-row"><strong>${profile.achievements[id]?"✓":"○"} ${esc(tx(zh,en))}</strong></div>`).join("");
  layer.innerHTML=`<div class="modal archive"><h2>${archiveKind==="codex"?tx("怪物图鉴","Monster Codex"):tx("成就","Achievements")}</h2><div class="archive-list">${rows}</div><button class="card compact" id="archive-close">${tx("返回","Return")}</button></div>`;
  $("archive-close").onclick=()=>{archiveKind=null;layer.hidden=true;render();};
}
function renderTrial(){const t=game.trial;
  modal(["双核试炼","Trial of Twin Cores"],[`剩余 ${40-t.moves} 步 · 先推箱子，再用箱子把怪物撞向金色核心。怪物不能直接推动`,`Moves left: ${40-t.moves} · Push a box into the monster to drive it onto golden cores. You cannot push the monster directly`],`
    <p>${tx("人：你 · 箱：箱子 · 怪：怪物 · 核：核心。边缘卡住会重置位置","P: player · B: box · M: monster · C: core. Edge traps trigger a reposition")}</p><div class="trial-grid">${Array.from({length:64},(_,c)=>{const type=c===t.player?"player":c===t.monster?"monster":t.boxes.includes(c)?"box":t.cores.includes(c)?"core":c===t.shattered?"shattered":"empty";return `<div class="tile ${type}">${({player:tx("人","P"),monster:tx("怪","M"),box:tx("箱","B"),core:tx("核","C"),shattered:"✦",empty:""})[type]}</div>`}).join("")}</div><div class="directions"><button data-dir="0,-1">↑</button><button data-dir="-1,0">←</button><button data-dir="0,1">↓</button><button data-dir="1,0">→</button><button id="trial-quit">${tx("放弃（按失败结算）","Forfeit (failure penalty)")}</button></div>`);
  bind("[data-dir]",b=>trialMove(...b.dataset.dir.split(",").map(Number)));$("trial-quit").onclick=()=>{if(confirm(tx("放弃将扣当前生命、金币和一件遗物，确定吗？","Forfeit loses current health, gold and one relic. Continue?"))){settleTrial(false);commit();}};
}
function init(){
  if(globalThis.ABYSS_BETA_BUILD!=="1.6.0")throw new Error("Real-time Beta files are from different releases; deploy the complete 1.6.0 asset set");
  // A transformed/animated arena establishes its own fixed-position containing block.
  // Mount dialogs directly on body so all controls remain reachable on short screens.
  document.body.append($("overlay"));
  profile.locale=profile.locale==="en"?"en":"zh";for(const key of ["kills","achievements","progress","claims"])if(!profile[key]||typeof profile[key]!=="object"||Array.isArray(profile[key]))profile[key]={};
  if(typeof profile.accountId!=="string"||!/^[0-9a-f-]{36}$/i.test(profile.accountId))profile.accountId=runId();
  profile.warehouse=[...new Set((Array.isArray(profile.warehouse)?profile.warehouse:[]).filter(id=>Object.hasOwn(ACCOUNT_ITEM_BY_ID,id)))];profile.claims=Object.fromEntries(Object.entries(profile.claims).filter(([run,item])=>/^[0-9a-f-]{36}$/i.test(run)&&(item==="complete"||Object.hasOwn(ACCOUNT_ITEM_BY_ID,item))));profile.equipped=profile.equipped&&typeof profile.equipped==="object"&&!Array.isArray(profile.equipped)?profile.equipped:{};for(const slot of ACCOUNT_SLOTS){const id=profile.equipped[slot];profile.equipped[slot]=id&&profile.warehouse.includes(id)&&Object.hasOwn(ACCOUNT_ITEM_BY_ID,id)&&ACCOUNT_ITEM_BY_ID[id].slot===slot?id:null;}
  profile.kills=Object.fromEntries(Object.entries(profile.kills).filter(([name,count])=>Object.hasOwn(MONSTER_EN,name)&&Number.isInteger(count)&&count>=0));profile.relics=Array.isArray(profile.relics)?profile.relics.filter(id=>Object.hasOwn(RELIC_BY_ID,id)):[];
  writeProfile();
  const toolbar=document.createElement("div");toolbar.className="save-actions";toolbar.innerHTML='<button id="export-save"></button><button id="import-save"></button><input id="import-file" type="file" accept="application/json,.json" hidden>';
  document.querySelector(".below").after(toolbar);const potion=document.createElement("button");potion.id="potion";document.querySelector(".below").append(potion);
  const layer=document.createElement("div");layer.id="archive-layer";layer.className="overlay archive-layer";layer.hidden=true;document.body.append(layer);
  $("export-save").onclick=exportRun;$("import-save").onclick=()=>$("import-file").click();$("import-file").onchange=async event=>{const file=event.target.files[0];event.target.value="";if(!file)return;if(file.size>1e6){saveError=tx("存档文件过大","Save file too large");render();return;}try{const text=await file.text();loadRun(text);if(game.heroId&&!game.finished&&!confirm(tx("导入将替换当前续局，是否继续？","Import replaces the current run. Continue?")))return;restoreRun(text);}catch{saveError=tx("不是兼容的 Beta 存档；原存档没有修改","Not a compatible Beta save; existing save is unchanged");render();}};
  $("pause").onclick=()=>{if(game.phase!=="battle"||archiveKind)return;game.paused=!game.paused;commit();};
  $("restart").onclick=()=>{if(game.heroId&&!game.finished)saveRun();game=emptyGame();archiveKind=null;$("archive-layer").hidden=true;skillSignature="";commit();};
  $("language").onclick=()=>{profile.locale=profile.locale==="en"?"zh":"en";writeProfile();renderScene();if(archiveKind)renderArchive();};$("warehouse").onclick=()=>openArchive("warehouse");$("codex").onclick=()=>openArchive("codex");$("achievements").onclick=()=>openArchive("achievements");$("potion").onclick=usePotion;
  document.addEventListener("keydown",event=>{if(event.repeat||event.ctrlKey||event.metaKey||event.altKey||/INPUT|TEXTAREA|SELECT/.test(event.target?.tagName)||archiveKind)return;const keys={w:[0,-1],ArrowUp:[0,-1],s:[0,1],ArrowDown:[0,1],a:[-1,0],ArrowLeft:[-1,0],d:[1,0],ArrowRight:[1,0]};if(game.phase==="trial"&&keys[event.key]){event.preventDefault();trialMove(...keys[event.key]);}else if(/^[1-4]$/.test(event.key)){event.preventDefault();cast(Number(event.key)-1);}});
  document.addEventListener("visibilitychange",()=>{if(document.hidden&&game.heroId&&!game.finished){game.paused=true;saveRun();render();}});window.addEventListener("pagehide",saveRun);
  game=emptyGame();renderScene();requestAnimationFrame(tick);
}
function tick(now){const elapsed=lastFrame?Math.max(0,now-lastFrame):0;lastFrame=now;if(!document.hidden)step(elapsed);saveElapsed+=Math.min(80,elapsed);if(saveElapsed>=1000){saveElapsed=0;saveRun();}render();requestAnimationFrame(tick);}
init();
