"use strict";
const $ = id => document.getElementById(id);
let state = null, busy = false, connected = false, paused = false, language = "zh", pollTimer, generation = 0;
const words = {
  zh: {title:"深渊之门",subtitle:"八层深渊。一段属于你的传奇。",chronicle:"远征手记",loading:"火炬正在点燃，深渊之门即将开启…",trial:"双核试炼",puzzleRule:"推箱子撞动守卫，让守卫击碎两个金色核心。WASD / 方向键移动，也可以点击下方方向按钮。",legendPlayer:"◆ 你",legendBox:"▣ 箱子",legendMonster:"♜ 守卫",legendCore:"✦ 核心",quitTrial:"放弃试炼",return:"返回远征",inputLabel:"你的行动",send:"确定 ↵",inputHint:"点击选项即可行动；需要继续时，留空按确定。",ended:"这段旅程已告一段落，你的存档与成就仍在。",gateway:"返回主页",supplies:"行囊与记录",saveNote:"进度会在游戏检查点自动保存。刷新页面可继续当前会话，服务重启后从最近检查点恢复。",backup:"↓ 下载存档备份",pause:"⏸ 暂停并返回",history:"冒险记录",historyNote:"回看本次会话中的战斗、选择与收获。",openHistory:"打开手记 →",storageNote:"存档属于当前浏览器。清除网站数据、使用无痕模式或更换设备前，请下载备份。种子用于查找本档案内的进度，不是跨设备存档码。",footer:"每一次远征，都值得被铭记。",online:"已连接",working:"处理中…",offline:"连接已中断",placeholder:"输入选项、名字或种子…",pauseConfirm:"返回主页后，从最近检查点继续。当前未保存的战斗操作会丢失，确定吗？",quitConfirm:"放弃会按原规则结算失败惩罚，确定吗？",victory:"双核已破碎 · 试炼胜利",defeat:"步数已用尽 · 试炼失败",reconnect:"重新连接",network:"连接暂时中断。恢复连接后会同步当前回合；请勿反复提交。",busy:"服务器暂不可用，请稍后点击重新连接。"},
  en: {title:"The Gateway",subtitle:"Eight floors below. A legend of your own.",chronicle:"Expedition journal",loading:"Lighting the torches. The gateway is opening…",trial:"The Twin Cores",puzzleRule:"Push a crate into the guardian to move it onto both golden cores. Use WASD, arrow keys, or the buttons below.",legendPlayer:"◆ You",legendBox:"▣ Crate",legendMonster:"♜ Guardian",legendCore:"✦ Core",quitTrial:"Abandon trial",return:"Return to expedition",inputLabel:"Your next move",send:"Enter ↵",inputHint:"Choose an action, or type a response. Send an empty line to continue.",ended:"This chapter has ended. Your checkpoints and achievements remain.",gateway:"Return to gateway",supplies:"Provisions & records",saveNote:"Progress saves at game checkpoints. Refresh to rejoin the current session; after a server restart, resume from the last checkpoint.",backup:"↓ Download save backup",pause:"⏸ Pause & return",history:"Your chronicle",historyNote:"Revisit the battles, choices, and discoveries of this session.",openHistory:"Open journal →",storageNote:"Your archive belongs to this browser. Download a backup before clearing site data, using private browsing, or changing devices. A seed looks up progress in this archive; it is not a cross-device save code.",footer:"Every descent writes another legend.",online:"Connected",working:"Working…",offline:"Disconnected",placeholder:"Option, name, or seed…",pauseConfirm:"Return to the gateway and resume from the last checkpoint? Unsaved combat actions will be lost.",quitConfirm:"Abandon the trial and take the normal failure penalty?",victory:"Both cores shattered · Victory",defeat:"No moves left · Defeat",reconnect:"Reconnect",network:"Connection interrupted. Reconnecting will synchronize the current turn; do not repeatedly submit.",busy:"Server unavailable. Please reconnect in a moment."}
};
Object.assign(words.zh, {
  brand:"深渊",brandSubtitle:"远征",fullTitle:"深渊远征",eyebrow:"来自深渊的冒险纪事",seal:"深入深渊",edition:"网页版 · 0.1",trialEyebrow:"双核守卫的考验",fieldNotes:"远征指南",archive:"深渊档案",importBackup:"↑ 导入存档备份",switchLanguage:"切换语言",languageHint:"在游戏主页切换语言",importTooLarge:"备份不能超过 8 MB。",importConfirm:"将切换到导入的档案。请先下载当前档案备份；未保存的行动会丢失。继续？",invalidBackup:"备份无效或不兼容，原档案未改动。",invalidRequest:"输入无效，请检查后重试。",stale:"此回合已变化，已刷新，请重新选择。",requestFailed:"请求失败，最近检查点仍保留。",up:"向上",down:"向下",left:"向左",right:"向右",boardLabel:"八乘八试炼棋盘",gameLabel:"游戏",choicesLabel:"可选行动",movementLabel:"移动",close:"关闭"
});
Object.assign(words.en, {
  brand:"ABYSS",brandSubtitle:"EXPEDITION",fullTitle:"Abyss Expedition",eyebrow:"A CHRONICLE OF THE DEPTHS",seal:"INTO THE ABYSS",edition:"WEB · 0.1",trialEyebrow:"TRIAL OF THE TWIN CORES",fieldNotes:"FIELD NOTES",archive:"THE ARCHIVE",importBackup:"↑ Import save backup",switchLanguage:"Language",languageHint:"Change language at the game gateway",importTooLarge:"Maximum backup size: 8 MB.",importConfirm:"Switch to the imported archive? Back up your current archive first. Unsaved actions will be lost.",invalidBackup:"Backup is invalid or incompatible; your current archive is unchanged.",invalidRequest:"Invalid input. Please check and try again.",stale:"This turn has changed. The page has been refreshed; choose again.",requestFailed:"Request failed. Your last checkpoint is retained.",up:"Up",down:"Down",left:"Left",right:"Right",boardLabel:"Eight by eight trial board",gameLabel:"Game",choicesLabel:"Available choices",movementLabel:"Movement",close:"Close"
});
const t = key => words[language][key] || key;
function languageAvailable() {
  return state?.ready && !state.ended && !state.puzzle && /^(Language|语言)\s/.test(choicesFrom(state.screen).get("5") || "");
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
function notice(message) { $("notice").textContent = message; $("notice").hidden = !message; }
async function api(path, body) {
  const response = await fetch(path, {method:body === undefined ? "GET" : "POST",credentials:"same-origin",cache:"no-store",headers:body === undefined ? {} : {"Content-Type":"text/plain;charset=UTF-8"},body,signal:AbortSignal.timeout(12000)});
  const data = await response.json();
  if (!response.ok) { const error = new Error(data.error || response.statusText); error.status = response.status; throw error; }
  return data;
}
function updateEnabled() {
  const canInput = connected && !busy && state?.ready && !state?.ended;
  document.querySelectorAll("#choices button, #submit, #command, [data-move], #quit-trial, #finish-trial").forEach(el => { el.disabled = !canInput; });
  $("pause").disabled = !connected || busy || !state || state.ended;
  $("restart").disabled = busy;
  $("shell-language").disabled = !connected || busy || !languageAvailable();
  $("connection").textContent = t(!connected ? "offline" : busy || state && !state.ready && !state.ended ? "working" : "online");
}
function choicesFrom(text) {
  const choices = new Map();
  for (const line of text.split("\n")) {
    // Only numbered action lines, never numbers embedded in prose or stat bars.
    if (!/^\s*(?:\[\d+\]|\d+[.)])\s/.test(line)) continue;
    const matches = [...line.matchAll(/(?:^|\s{2,})(?:\[(\d+)\]|(\d+)[.)])\s+(.+?)(?=\s{2,}(?:\[\d+\]|\d+[.)])\s|$)/g)];
    for (const match of matches) choices.set(match[1] || match[2], match[3].trim());
  }
  return choices;
}
function renderText(text) {
  $("screen").replaceChildren();
  const gateway = Math.max(text.lastIndexOf("THE GATEWAY"), text.lastIndexOf("深渊之门"));
  if (gateway >= 0) text = text.substring(gateway);
  const classMenu = /CHOOSE YOUR CHAMPION|THE HIDDEN PATH|选择你的角色|隐藏之路/.test(text);
  let blank = false;
  for (const line of text.split("\n")) {
    if (!classMenu && choicesFrom(line).size) continue;
    if (/^\s*>\s*$/.test(line) || /Type a number, then press Enter\.|输入编号后按回车。/.test(line)) continue;
    if (!line.trim() && blank) continue;
    blank = !line.trim();
    const health = line.match(/^\s*(.+?)\s+\[[#.]+\]\s*(\d+)\/(\d+)(.*)$/);
    if (health) {
      const row = document.createElement("div"); row.className = "health-row";
      const label = document.createElement("span"); label.textContent = health[1].trim();
      const bar = document.createElement("progress"); bar.max = Math.max(1, Number(health[3])); bar.value = Number(health[2]); bar.setAttribute("aria-label", label.textContent);
      const value = document.createElement("span"); value.textContent = `${health[2]} / ${health[3]}${health[4]}`;
      row.append(label, bar, value); $("screen").append(row); continue;
    }
    const row = document.createElement("div");
    const divider = /^\s*[─━═+\-|░▒▓▄☠❉ ]{8,}\s*$/.test(line) && /[─━═\-]/.test(line);
    row.className = divider ? "divider" : "text-line";
    if (/❉|CHOOSE YOUR|THE GATEWAY|深渊之门|选择你的|选择冒险/.test(line)) row.classList.add("heading-line");
    if (!divider) row.textContent = line;
    $("screen").append(row);
  }
  $("screen").scrollTop = 0;
}
function render(next) {
  if (state && next.revision < state.revision) return;
  const changed = !state || state.revision !== next.revision;
  state = next; connected = true;
  if (changed) {
    language = next.language === "zh" ? "zh" : "en";
    translate();
    renderText(next.screen);
    $("history-text").textContent = next.history;
    $("choices").replaceChildren();
    if (!next.puzzle && !next.ended) for (const [number, label] of choicesFrom(next.screen)) {
      const button = document.createElement("button"); button.className = "choice"; button.type = "button";
      const badge = document.createElement("span"); badge.className = "number"; badge.textContent = number;
      const name = document.createElement("span"); name.className = "label"; name.textContent = label;
      button.append(badge, name); button.addEventListener("click", () => send(number)); $("choices").append(button);
    }
    $("screen").hidden = !!next.puzzle;
    $("puzzle").hidden = !next.puzzle;
    $("command-form").hidden = !!next.puzzle || next.ended;
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
  try { state = null; render(await api(restart ? "/api/restart" : "/api/session", "")); notice(""); }
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
$("shell-language").addEventListener("click", () => { if (languageAvailable()) send("5"); });
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
