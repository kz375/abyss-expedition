"use strict";
const $ = id => document.getElementById(id);
let state = null, busy = false, connected = false, paused = false, language = "zh", pollTimer, generation = 0;
let healthSnapshot = new Map();
const words = {
  zh: {title:"深渊之门",subtitle:"八层深渊 一段属于你的传奇",chronicle:"远征手记",loading:"火炬正在点燃，深渊之门即将开启…",trial:"双核试炼",puzzleRule:"推箱子撞动守卫，让守卫击碎两个金色核心。WASD / 方向键移动，也可以点击下方方向按钮。",legendPlayer:"◆ 你",legendBox:"▣ 箱子",legendMonster:"♜ 守卫",legendCore:"✦ 核心",quitTrial:"放弃试炼",return:"返回远征",inputLabel:"你的行动",send:"确定 ↵",inputHint:"只有名字、种子和隐藏口令需要输入。",ended:"这段旅程已告一段落，你的存档与成就仍在。",gateway:"返回主页",supplies:"行囊与记录",saveNote:"进度会在游戏检查点自动保存。刷新页面可继续当前会话，服务重启后从最近检查点恢复。",backup:"↓ 下载存档备份",pause:"⏸ 暂停并返回",history:"冒险记录",historyNote:"回看本次会话中的战斗、选择与收获。",openHistory:"打开手记 →",storageNote:"存档属于当前浏览器。清除网站数据、使用无痕模式或更换设备前，请下载备份。种子用于查找本档案内的进度，不是跨设备存档码。",footer:"每一次远征，都值得被铭记。",online:"已连接",working:"处理中…",offline:"连接已中断",placeholder:"输入名字、种子或隐藏口令…",pauseConfirm:"返回主页后，从最近检查点继续。当前未保存的战斗操作会丢失，确定吗？",quitConfirm:"放弃会按原规则结算失败惩罚，确定吗？",victory:"双核已破碎 · 试炼胜利",defeat:"步数已用尽 · 试炼失败",reconnect:"重新连接",network:"连接暂时中断。恢复连接后会同步当前回合；请勿反复提交。",busy:"服务器暂不可用，请稍后点击重新连接。",continue:"继续",browseSaves:"查看保留存档",randomSeed:"随机生成种子"},
  en: {title:"The Gateway",subtitle:"Eight floors below  A legend of your own",chronicle:"Expedition journal",loading:"Lighting the torches. The gateway is opening…",trial:"The Twin Cores",puzzleRule:"Push a crate into the guardian to move it onto both golden cores. Use WASD, arrow keys, or the buttons below.",legendPlayer:"◆ You",legendBox:"▣ Crate",legendMonster:"♜ Guardian",legendCore:"✦ Core",quitTrial:"Abandon trial",return:"Return to expedition",inputLabel:"Your next move",send:"Enter ↵",inputHint:"Only a name, seed, or hidden passphrase needs typing.",ended:"This chapter has ended. Your checkpoints and achievements remain.",gateway:"Return to gateway",supplies:"Provisions & records",saveNote:"Progress saves at game checkpoints. Refresh to rejoin the current session; after a server restart, resume from the last checkpoint.",backup:"↓ Download save backup",pause:"⏸ Pause & return",history:"Your chronicle",historyNote:"Revisit the battles, choices, and discoveries of this session.",openHistory:"Open journal →",storageNote:"Your archive belongs to this browser. Download a backup before clearing site data, using private browsing, or changing devices. A seed looks up progress in this archive; it is not a cross-device save code.",footer:"Every descent writes another legend.",online:"Connected",working:"Working…",offline:"Disconnected",placeholder:"Name, seed, or hidden passphrase…",pauseConfirm:"Return to the gateway and resume from the last checkpoint? Unsaved combat actions will be lost.",quitConfirm:"Abandon the trial and take the normal failure penalty?",victory:"Both cores shattered · Victory",defeat:"No moves left · Defeat",reconnect:"Reconnect",network:"Connection interrupted. Reconnecting will synchronize the current turn; do not repeatedly submit.",busy:"Server unavailable. Please reconnect in a moment.",continue:"Continue",browseSaves:"Browse retained saves",randomSeed:"Generate random seed"}
};
Object.assign(words.zh, {
  brand:"深渊",brandSubtitle:"远征",fullTitle:"深渊远征",eyebrow:"来自深渊的冒险纪事",seal:"深入深渊",edition:"网页版 · 0.1",trialEyebrow:"双核守卫的考验",fieldNotes:"远征指南",archive:"深渊档案",importBackup:"↑ 导入存档备份",switchLanguage:"切换语言",languageHint:"在游戏主页切换语言",importTooLarge:"备份不能超过 8 MB。",importConfirm:"将切换到导入的档案。请先下载当前档案备份；未保存的行动会丢失。继续？",invalidBackup:"备份无效或不兼容，原档案未改动。",invalidRequest:"输入无效，请检查后重试。",stale:"此回合已变化，已刷新，请重新选择。",requestFailed:"请求失败，最近检查点仍保留。",up:"向上",down:"向下",left:"向左",right:"向右",boardLabel:"八乘八试炼棋盘",gameLabel:"游戏",choicesLabel:"可选行动",movementLabel:"移动",close:"关闭",hiddenLabel:"隐藏角色口令",hiddenPlaceholder:"输入口令后点击确定",hiddenHint:"普通角色请直接点击上方按钮。",seedLabel:"种子档案",seedPlaceholder:"输入已有种子以恢复进度",seedHint:"也可以点击下方按钮查看存档或生成新种子。"
});
Object.assign(words.en, {
  brand:"ABYSS",brandSubtitle:"EXPEDITION",fullTitle:"Abyss Expedition",eyebrow:"A CHRONICLE OF THE DEPTHS",seal:"INTO THE ABYSS",edition:"WEB · 0.1",trialEyebrow:"TRIAL OF THE TWIN CORES",fieldNotes:"FIELD NOTES",archive:"THE ARCHIVE",importBackup:"↑ Import save backup",switchLanguage:"Language",languageHint:"Change language at the game gateway",importTooLarge:"Maximum backup size: 8 MB.",importConfirm:"Switch to the imported archive? Back up your current archive first. Unsaved actions will be lost.",invalidBackup:"Backup is invalid or incompatible; your current archive is unchanged.",invalidRequest:"Invalid input. Please check and try again.",stale:"This turn has changed. The page has been refreshed; choose again.",requestFailed:"Request failed. Your last checkpoint is retained.",up:"Up",down:"Down",left:"Left",right:"Right",boardLabel:"Eight by eight trial board",gameLabel:"Game",choicesLabel:"Available choices",movementLabel:"Movement",close:"Close",hiddenLabel:"Hidden character passphrase",hiddenPlaceholder:"Enter a passphrase, then confirm",hiddenHint:"Choose a normal character with the buttons above.",seedLabel:"Seed archive",seedPlaceholder:"Enter an existing seed to restore progress",seedHint:"Or use the buttons below to browse saves or create a new seed."
});
words.zh.thanks = "感谢你踏入深渊";
words.en.thanks = "Thank you for venturing into the abyss";
words.zh.edition = "版本 · 1.2.6";
words.en.edition = "VERSION · 1.2.6";
words.zh.versionLabel = "版本";
words.en.versionLabel = "VERSION";
words.zh.releaseVersion = "1.2.6";
words.en.releaseVersion = "1.2.6";
words.zh.releaseNote = "护盾动画、事件交互与战斗修复";
words.en.releaseNote = "Smooth wards, event choices, and combat fixes";
const t = key => words[language][key] || key;
function languageAvailable() {
  return state?.ready && !state.ended && !state.puzzle && [...choicesFrom(state.screen).entries()].some(([, label]) => /^(Language|语言)\s/.test(label));
}
function languageChoiceNumber() {
  return [...choicesFrom(state?.screen || "").entries()].find(([, label]) => /^(Language|语言)\s/.test(label))?.[0];
}
function errorText(error, fallback = "network") {
  return t(({400:"invalidRequest",401:"reconnect",403:"requestFailed",409:"stale",410:"reconnect",413:"importTooLarge",500:"requestFailed",503:"busy"})[error.status] || fallback);
}
function translate() {
  document.documentElement.lang = language === "zh" ? "zh-CN" : "en";
  document.title = t("fullTitle");
  document.querySelector('meta[name="description"]').content = t("subtitle");
  document.querySelectorAll("[data-i18n]").forEach(el => { el.textContent = t(el.dataset.i18n); });
  document.querySelectorAll("[data-i18n-aria]").forEach(el => { el.setAttribute("aria-label", t(el.dataset.i18nAria)); });
  $("shell-language").title = t(languageAvailable() ? "switchLanguage" : "languageHint");
  $("command").placeholder = t("placeholder");
  updateEnabled();
}
function updateCommandHelp(classSelection = false, seedPrompt = false) {
  let label = t("inputLabel"), placeholder = t("placeholder"), hint = t("inputHint");
  if (classSelection) ({label, placeholder, hint} = {label:t("hiddenLabel"), placeholder:t("hiddenPlaceholder"), hint:t("hiddenHint")});
  else if (seedPrompt) ({label, placeholder, hint} = {label:t("seedLabel"), placeholder:t("seedPlaceholder"), hint:t("seedHint")});
  document.querySelector('label[for="command"]').textContent = label;
  $("command").placeholder = placeholder;
  $("input-hint").textContent = hint;
}
function notice(message) { $("notice").textContent = message; $("notice").hidden = !message; }
async function api(path, body) {
  const response = await fetch(path, {method:body === undefined ? "GET" : "POST",credentials:"same-origin",cache:"no-store",headers:body === undefined ? {} : {"Content-Type":"text/plain;charset=UTF-8"},body,signal:AbortSignal.timeout(12000)});
  const data = await response.json();
  if (!response.ok) { const error = new Error(data.error || response.statusText); error.status = response.status; throw error; }
  return data;
}
function updateEnabled() {
  const canInput = connected && !busy && state?.ready && !state?.ended;
  document.querySelectorAll("#choices button, #submit, #command, [data-move], #quit-trial, #finish-trial").forEach(el => { el.disabled = !canInput || el.dataset.locked === "true"; });
  $("pause").disabled = !connected || busy || !state || state.ended;
  $("restart").disabled = busy;
  $("shell-language").disabled = !connected || busy || !languageAvailable();
  $("connection").textContent = t(!connected ? "offline" : busy || state && !state.ready && !state.ended ? "working" : "online");
}
// Console padding disappears when a long enemy name reaches the label width, producing
// "Elite Shadow Assassin[#####]". Accept both that form and the normal spaced form.
const healthLine = /^\s*(.*?)\s*\[([#.]+)\]\s*(\d+)\/(\d+)(.*)$/;
function choiceLine(line) {
  // A numbered summon HP row is state, never an action.
  if (healthLine.test(line)) return {prefix:line, choices:[]};
  const start = line.match(/^\s*(?=\[\d+\]\s|\d+[.)]\s)/)
    || line.match(/(?:^|[\s。！？.!?:：])(?=1\.\s+)/);
  if (!start) return {prefix:line, choices:[]};
  const offset = start.index + start[0].length;
  const choices = [...line.slice(offset).matchAll(/(?:^|\s+)(?:\[(\d+)\]|(\d+)[.)])\s+(.+?)(?=\s+(?:\[\d+\]|\d+[.)])\s|$)/g)]
    .map(match => [match[1] || match[2], match[3].trim()]);
  return {prefix:line.slice(0, offset).trimEnd(), choices};
}
function choicesFrom(text) {
  return new Map(text.split("\n").flatMap(line => choiceLine(line).choices));
}
function isClassSelection(text) { return /CHOOSE YOUR CHAMPION|选择你的角色/.test(text); }
function isSeedPrompt(text) { return /(?:Seed|种子)\s*>\s*$/m.test(text); }
function needsContinue(text) { return /Press Enter to (?:enter|return)|按回车(?:进入|返回)/.test(text); }
function addChoice(number, label, action, variant = "", locked = false) {
  const button = document.createElement("button"); button.className = "choice"; button.type = "button";
  if (variant) button.classList.add(variant);
  if (locked) { button.dataset.locked = "true"; button.setAttribute("aria-disabled", "true"); }
  if (number) { const badge = document.createElement("span"); badge.className = "number"; badge.textContent = number; button.append(badge); }
  const name = document.createElement("span"); name.className = "label"; name.textContent = label;
  button.append(name); button.addEventListener("click", action); $("choices").append(button);
}
function isEventScreen(text) {
  return /(?:篝火|神龛|宝箱|医师|泉水|冒险者|赌徒|图书馆|低语之井|牌手|预言家|收藏家|裂隙|熔炉|祭坛|商队|神像|Campfire|Shrine|Chest|Healer|Spring|Adventurer|Gambler|Library|Well|Card Sharp|Oracle|Curator|Rift|Forge|Altar|Caravan|Idol)/.test(text)
    && !/(?:ENCOUNTER:|遭遇战|ELITE|精英战|BOSS|首领战)/.test(text);
}
function isCriticalLine(line) { return /CRITICAL HIT|暴击/.test(line); }
function basicDamage(line) {
  return line.match(/^You attack for (\d+) damage\.$/) || line.match(/^你发动普通攻击，造成 (\d+) 点伤害。$/);
}
function incomingDamage(line) {
  return line.match(/^(.+?) attacks you for (\d+) damage\.$/) || line.match(/^(.+?)攻击了你，造成 (\d+) 点伤害。$/);
}
function combatEvent(row, icon, value, kind, label = "") {
  row.className = `text-line combat-event ${kind}`;
  const glyph = document.createElement("span"); glyph.className = "combat-icon"; glyph.textContent = icon;
  const text = document.createElement("span"); text.className = "combat-label"; text.textContent = label;
  const number = document.createElement("strong"); number.className = "combat-number"; number.textContent = value;
  row.append(glyph, text, number);
}
function animateMeter(layer, fraction, previous, duration, delay = 0) {
  const target = `scaleX(${fraction})`;
  const from = previous == null ? target : getComputedStyle(layer).transform;
  layer.getAnimations().forEach(animation => animation.cancel());
  layer.style.transform = target;
  if (previous != null && previous !== fraction && !matchMedia("(prefers-reduced-motion: reduce)").matches) {
    layer.animate([{transform:from === "none" ? `scaleX(${previous})` : from}, {transform:target}],
      {duration, delay, easing:"cubic-bezier(.22,.7,.22,1)", fill:"backwards"});
  }
}
function pulseHealth(entry, kind) {
  if (matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  entry.row.classList.remove("health-hit", "shield-break");
  void entry.row.offsetWidth;
  entry.row.classList.add(kind);
  entry.row.addEventListener("animationend", () => entry.row.classList.remove(kind), {once:true});
}
function damageBurst(entry, amount) {
  if (!amount || matchMedia("(prefers-reduced-motion: reduce)").matches) return;
  const burst = document.createElement("span");
  burst.className = "damage-burst";
  burst.textContent = `-${amount}`;
  burst.setAttribute("aria-hidden", "true");
  entry.meter.append(burst);
  burst.addEventListener("animationend", () => burst.remove(), {once:true});
}
function renderHealth(health, seen) {
  const name = health[1].trim();
  const player = /^(You|你|Health|生命)$/.test(name);
  const key = `${player ? "hero" : "enemy"}:${name}`;
  const current = Number(health[3]), maximum = Math.max(1, Number(health[4]));
  const shield = Number((health[5].match(/(?:Shield|护盾)\s+(\d+)/) || [, "0"])[1]);
  let entry = healthSnapshot.get(key);
  if (!entry) {
    const row = document.createElement("div");
    row.className = `health-row ${player ? "player-health" : "enemy-health"}${/^\d+\.\s/.test(name) ? " summon-health" : ""}`;
    const label = document.createElement("span"); label.textContent = name; label.title = name;
    const meter = document.createElement("span"); meter.className = "health-meter";
    meter.setAttribute("role", "meter"); meter.setAttribute("aria-label", name); meter.setAttribute("aria-valuemin", "0");
    const layers = {};
    for (const kind of ["damage-trail", "health-fill", "shield-trail", "shield-fill"]) {
      layers[kind] = document.createElement("span"); layers[kind].className = kind;
      layers[kind].setAttribute("aria-hidden", "true"); meter.append(layers[kind]);
    }
    const value = document.createElement("span"); value.className = "health-value";
    row.append(label, meter, value); entry = {row, meter, value, layers};
  }
  const hp = Math.max(0, Math.min(1, current / maximum));
  const ward = Math.max(0, Math.min(1, shield / maximum));
  if (entry.hp != null && hp < entry.hp) {
    pulseHealth(entry, "health-hit");
    damageBurst(entry, Math.max(1, Math.round((entry.hp - hp) * maximum)));
  }
  if (entry.ward != null && ward === 0 && entry.ward > 0) pulseHealth(entry, "shield-break");
  // Both tracks use max HP as their scale. A full HP bar never hides the shield.
  for (const [kind, fraction, old, duration, delay] of [
    ["health-fill", hp, entry.hp, 360, 0], ["damage-trail", hp, entry.hp, 620, hp < entry.hp ? 160 : 0],
    ["shield-fill", ward, entry.ward, 320, 0], ["shield-trail", ward, entry.ward, 540, ward < entry.ward ? 100 : 0]
  ]) if (old !== fraction) animateMeter(entry.layers[kind], fraction, old, duration, delay);
  entry.meter.setAttribute("aria-valuemax", String(maximum));
  entry.meter.setAttribute("aria-valuenow", String(current));
  entry.meter.setAttribute("aria-valuetext", `${current} / ${maximum}; ${language === "zh" ? "护盾" : "Shield"} ${shield}`);
  entry.value.textContent = `${health[3]} / ${health[4]}${health[5]}`;
  entry.hp = hp; entry.ward = ward; seen.set(key, entry);
  return entry.row;
}
function renderText(text) {
  const fragment = document.createDocumentFragment(), seen = new Map();
  // A new encounter must not inherit the last same-named enemy's animation.
  if (/>> (?:BOSS |ELITE )?ENCOUNTER:|❉ (?:首领战|精英战|遭遇战) ·/.test(text)) healthSnapshot.clear();
  const gateway = Math.max(text.lastIndexOf("THE GATEWAY"), text.lastIndexOf("深渊之门"));
  if (gateway >= 0) text = text.substring(gateway);
  const classMenu = /CHOOSE YOUR CHAMPION|THE HIDDEN PATH|选择你的角色|隐藏之路/.test(text);
  let blank = false;
  const lines = text.split("\n");
  for (let index = 0; index < lines.length; index++) {
    let line = lines[index];
    const actions = choiceLine(line);
    if (!classMenu && actions.choices.length) {
      line = actions.prefix;
      if (!line.trim()) continue;
    }
    if (/^\s*>\s*$/.test(line) || /Type a number, then press Enter\.|输入编号后按回车。/.test(line)) continue;
    if (!line.trim() && blank) continue;
    blank = !line.trim();
    const health = line.match(healthLine);
    if (health) {
      if (/^(Health|生命)$/.test(health[1].trim())) {
        const ward = (lines[index + 1] || "").match(/(?:Shield|护盾)\s+\d+/);
        if (ward) health[5] += `  ${ward[0]}`;
      }
      fragment.append(renderHealth(health, seen)); continue;
    }
    const row = document.createElement("div");
    if (/^\s*(Summoned foes:|召唤物：)\s*$/.test(line)) {
      row.className = "summon-heading"; row.textContent = line.trim(); fragment.append(row); continue;
    }
    const critical = index > 0 && isCriticalLine(lines[index - 1]);
    const outgoing = basicDamage(line), incoming = incomingDamage(line);
    if (isCriticalLine(line) && (basicDamage(lines[index + 1] || "") || incomingDamage(lines[index + 1] || ""))) continue;
    if (outgoing) { combatEvent(row, critical ? "✦" : "⚔", outgoing[1], critical ? "critical-hit" : "player-hit"); fragment.append(row); continue; }
    if (incoming) { combatEvent(row, critical ? "✦" : "☠", incoming[2], critical ? "critical-hit enemy-hit" : "enemy-hit", incoming[1]); fragment.append(row); continue; }
    const divider = /^\s*[─━═+\-|░▒▓▄☠❉ ]{8,}\s*$/.test(line) && /[─━═\-]/.test(line);
    row.className = divider ? "divider" : "text-line";
    if (/❉|CHOOSE YOUR|THE GATEWAY|深渊之门|选择你的|选择冒险/.test(line)) row.classList.add("heading-line");
    if (!divider) row.textContent = line;
    fragment.append(row);
  }
  healthSnapshot = seen;
  $("screen").replaceChildren(fragment);
  $("screen").scrollTop = 0;
}
function render(next) {
  if (state && next.revision < state.revision) return;
  const changed = !state || state.revision !== next.revision;
  const screenChanged = !state || state.screen !== next.screen || state.language !== next.language;
  state = next; connected = true;
  if (changed) {
    language = next.language === "zh" ? "zh" : "en";
    translate();
    if (screenChanged) renderText(next.screen);
    $("history-text").textContent = next.history;
    $("choices").replaceChildren();
    const numberedChoices = choicesFrom(next.screen);
    const classSelection = isClassSelection(next.screen);
    const eventScreen = isEventScreen(next.screen);
    const seedPrompt = isSeedPrompt(next.screen);
    $("choices").classList.toggle("class-choices", classSelection);
    $("choices").classList.toggle("event-choices", eventScreen);
    if (!next.puzzle && !next.ended) {
      for (const [number, label] of numberedChoices) {
        const locked = /\s\[(?:closed|chosen|已关闭|已选择)\]$/.test(label);
        addChoice(number, label, () => send(number), classSelection ? "class-choice" : eventScreen ? "event-choice" : "", locked);
      }
      if (!numberedChoices.size && needsContinue(next.screen)) addChoice("", t("continue"), () => send(""));
      if (seedPrompt) {
        addChoice("", t("browseSaves"), () => send(language === "zh" ? "列表" : "LIST"));
        addChoice("", t("randomSeed"), () => send(""));
      }
    }
    $("screen").hidden = !!next.puzzle;
    $("puzzle").hidden = !next.puzzle;
    const requiresTyping = !numberedChoices.size || classSelection;
    $("command-form").hidden = !!next.puzzle || next.ended || !requiresTyping;
    updateCommandHelp(classSelection, seedPrompt);
    $("ended").hidden = !next.ended;
    $("restart").textContent = t("gateway");
    if (next.puzzle) renderPuzzle(next.puzzle);
  }
  updateEnabled();
}
function renderPuzzle(p) {
  document.querySelectorAll(".puzzle-result").forEach(el => el.remove());
  $("moves").textContent = `${p.limit - p.moves} / ${p.limit}`;
  $("board").replaceChildren();
  for (let i = 0; i < p.size * p.size; i++) {
    const cell = document.createElement("div"); cell.className = "cell"; cell.setAttribute("role", "gridcell");
    const edge = i % p.size === 0 || i % p.size === p.size - 1 || i < p.size || i >= p.size * (p.size - 1);
    if (edge) cell.classList.add("edge");
    let name = language === "zh" ? "空地" : "Empty";
    if (p.targets.includes(i)) { cell.classList.add("core"); cell.textContent = "✦"; name = t("legendCore"); }
    if (p.boxes.includes(i)) { cell.classList.add("box"); cell.textContent = "▣"; name = t("legendBox"); }
    if (p.monster === i) { cell.classList.add("monster"); cell.textContent = "♜"; name = t("legendMonster"); }
    if (p.player === i) { cell.classList.add("player"); cell.textContent = "◆"; name = t("legendPlayer"); }
    if (p.shattered === i) cell.classList.add("broken");
    cell.setAttribute("aria-label", `${Math.floor(i / p.size) + 1},${i % p.size + 1}: ${name}`);
    $("board").append(cell);
  }
  $("finish-trial").hidden = !p.finished; $("quit-trial").hidden = p.finished;
  document.querySelector(".dpad").hidden = p.finished;
  if (p.finished) { const result = document.createElement("p"); result.className = "puzzle-result"; result.textContent = t(p.won ? "victory" : "defeat"); $("board").after(result); }
  else document.querySelectorAll(".puzzle-result").forEach(el => el.remove());
}
async function connect(restart = false) {
  ++generation;
  busy = true; paused = false; updateEnabled(); clearTimeout(pollTimer);
  try { state = null; healthSnapshot.clear(); render(await api(restart ? "/api/restart" : "/api/session", "")); notice(""); }
  catch (e) { disconnected(errorText(e, "busy")); }
  finally { busy = false; updateEnabled(); schedulePoll(); }
}
function disconnected(message) {
  connected = false; notice(message); updateEnabled();
  $("ended").hidden = false; $("restart").textContent = t("reconnect");
}
function schedulePoll() { clearTimeout(pollTimer); if (!paused && connected) pollTimer = setTimeout(poll, document.hidden ? 5000 : state?.ready || state?.ended ? 1800 : 120); }
async function poll() {
  if (busy) { schedulePoll(); return; }
  const expectedGeneration = generation;
  try { const next = await api("/api/state"); if (generation === expectedGeneration) render(next); }
  catch (e) { if (generation === expectedGeneration) disconnected(errorText(e)); }
  finally { schedulePoll(); }
}
async function send(text) {
  if (!state?.ready || state.ended || busy || !connected) return;
  busy = true; updateEnabled(); clearTimeout(pollTimer);
  try {
    render(await api("/api/input", `${state.revision}\n${text}`));
    $("command").value = ""; notice("");
  } catch (e) {
    if (e.status === 409) { notice(errorText(e)); try { render(await api("/api/state")); } catch { disconnected(t("network")); } }
    else disconnected(errorText(e));
  } finally { busy = false; updateEnabled(); schedulePoll(); }
}
$("command-form").addEventListener("submit", e => { e.preventDefault(); send($("command").value); });
$("restart").addEventListener("click", () => connect(connected && state?.ended));
$("shell-language").addEventListener("click", () => { const choice = languageChoiceNumber(); if (choice) send(choice); });
$("show-history").addEventListener("click", () => $("history-dialog").showModal());
$("close-history").addEventListener("click", () => $("history-dialog").close());
$("pause").addEventListener("click", async () => {
  if (busy || !confirm(t("pauseConfirm"))) return;
  busy = true; paused = true; ++generation; updateEnabled(); clearTimeout(pollTimer);
  try { await api("/api/pause", ""); await connect(); }
  catch (e) { disconnected(errorText(e)); }
  finally { busy = false; updateEnabled(); }
});
$("quit-trial").addEventListener("click", () => { if (confirm(t("quitConfirm"))) send("QUIT"); });
$("finish-trial").addEventListener("click", () => send(""));
$("import-backup").addEventListener("click", () => { if (!busy) $("backup-file").click(); });
$("backup-file").addEventListener("change", async () => {
  const file = $("backup-file").files[0]; $("backup-file").value = "";
  if (!file || busy) return;
  if (file.size > 8000000) { notice(t("importTooLarge")); return; }
  if (!confirm(t("importConfirm"))) return;
  busy = true; ++generation; clearTimeout(pollTimer); updateEnabled();
  try {
    const response = await fetch("/api/import", {method:"POST",credentials:"same-origin",body:file,signal:AbortSignal.timeout(30000)});
    const data = await response.json();
    if (!response.ok) { const error = new Error(); error.status = response.status; throw error; }
    state = null; render(data); notice("");
  } catch (e) { notice(e.status === 400 ? t("invalidBackup") : errorText(e)); }
  finally { busy = false; updateEnabled(); schedulePoll(); }
});
document.querySelectorAll("[data-move]").forEach(button => button.addEventListener("click", () => send(button.dataset.move)));
document.addEventListener("keydown", e => {
  if (!state?.puzzle || state.puzzle.finished || $("history-dialog").open || e.ctrlKey || e.metaKey || e.altKey || /INPUT|TEXTAREA/.test(e.target.tagName)) return;
  const move = {ArrowUp:"UP",ArrowDown:"DOWN",ArrowLeft:"LEFT",ArrowRight:"RIGHT",w:"UP",s:"DOWN",a:"LEFT",d:"RIGHT"}[e.key.length === 1 ? e.key.toLowerCase() : e.key];
  if (move) { e.preventDefault(); if (!e.repeat) send(move); }
});
document.addEventListener("visibilitychange", () => { if (!document.hidden && connected && !busy) { clearTimeout(pollTimer); poll(); } });
translate(); connect();
