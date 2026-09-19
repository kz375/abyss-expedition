// Optional browser integration test with an isolated headless Chrome profile.
// node web/tests/browser-test.mjs /absolute/path/to/chrome http://127.0.0.1:8080
import { spawn } from 'node:child_process';
import { mkdtemp, readFile, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import assert from 'node:assert/strict';

const executable = process.argv[2];
const origin = process.argv[3] || 'http://127.0.0.1:8080';
if (!executable) throw new Error('Pass an absolute Chrome executable path');
const profile = await mkdtemp(join(tmpdir(), 'abyss-web-chrome-'));
const chrome = spawn(executable, ['--headless=new', '--no-first-run', '--no-default-browser-check', `--user-data-dir=${profile}`, '--remote-debugging-port=0', 'about:blank'], {stdio:'ignore'});
let socket;
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
try {
  let port;
  for (let i = 0; i < 100; i++) {
    try { port = Number((await readFile(join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]); break; } catch { await delay(100); }
  }
  assert.ok(port, 'Chrome starts');
  const targets = await (await fetch(`http://127.0.0.1:${port}/json`)).json();
  socket = new WebSocket(targets.find(t => t.type === 'page').webSocketDebuggerUrl);
  await new Promise((resolve, reject) => { socket.onopen = resolve; socket.onerror = reject; });
  const pending = new Map(); let serial = 0; const errors = [];
  socket.onmessage = event => {
    const message = JSON.parse(event.data);
    if (message.id) { const item = pending.get(message.id); if (item) { pending.delete(message.id); message.error ? item.reject(new Error(JSON.stringify(message.error))) : item.resolve(message.result); } }
    else if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text);
  };
  const command = (method, params = {}) => new Promise((resolve, reject) => {
    const id = ++serial; pending.set(id, {resolve,reject}); socket.send(JSON.stringify({id,method,params}));
  });
  const evaluate = async expression => {
    const result = await command('Runtime.evaluate', {expression,returnByValue:true,awaitPromise:true});
    if (result.exceptionDetails) throw new Error(JSON.stringify(result.exceptionDetails));
    return result.result.value;
  };
  const waitFor = async expression => {
    for (let i = 0; i < 120; i++) { if (await evaluate(expression)) return; await delay(100); }
    throw new Error(`Timed out: ${expression}\n${await evaluate('document.body.innerText')}`);
  };
  const send = async value => {
    const revision = await evaluate('state?.revision');
    if (await evaluate('document.getElementById("command-form").hidden')) {
      const clicked = await evaluate(`(() => {
        const button = [...document.querySelectorAll('#choices button')].find(item => item.querySelector('.number')?.textContent === ${JSON.stringify(value)});
        if (!button) return false; button.click(); return true;
      })()`);
      assert.equal(clicked, true, `clickable choice ${value} exists`);
    } else {
      await waitFor('document.getElementById("submit").disabled === false');
      await evaluate(`document.getElementById('command').value = ${JSON.stringify(value)}; document.getElementById('command-form').requestSubmit();`);
    }
    await waitFor(`(state?.ready && !busy && state?.revision !== ${JSON.stringify(revision)}) || !document.getElementById("ended").hidden`);
  };
  await command('Runtime.enable'); await command('Page.enable');
  await command('Emulation.setDeviceMetricsOverride', {width:1280,height:960,deviceScaleFactor:1,mobile:false});
  await command('Page.navigate', {url:origin});
  await waitFor('document.getElementById("choices")?.children.length === 6 && !document.getElementById("submit").disabled');
  assert.equal(await evaluate('document.querySelectorAll(".choice").length'), 6, 'main menu buttons including the monster codex');
  assert.equal(await evaluate('document.getElementById("choices").innerText.includes("Monster Codex")'), true, 'Monster Codex is selectable from the gateway');
  assert.equal(await evaluate('document.getElementById("command-form").hidden'), true, 'numbered gateway choices do not require typing');
  assert.equal(/[\u3400-\u9fff]/.test(await evaluate('document.body.innerText')), false, 'English screen contains no Chinese UI');
  await evaluate('document.getElementById("shell-language").click()');
  await waitFor('document.querySelectorAll(".choice").length === 2 && !document.getElementById("submit").disabled');
  assert.equal(await evaluate('document.body.innerText.includes("简体中文")'), true, 'Chinese language name stays Chinese');
  assert.equal(await evaluate('document.body.innerText.includes("Simplified Chinese")'), false, 'English translation does not replace the Chinese language name');
  await send('2');
  await waitFor('document.getElementById("screen").innerText.includes("深渊之门")');
  assert.equal(/[A-Za-z]{3,}/.test(await evaluate('document.body.innerText')), false, 'Chinese page, branding and menu contain no English UI');
  await evaluate('document.getElementById("show-history").click()');
  assert.equal(/[A-Za-z]{3,}/.test(await evaluate('document.getElementById("history-text").innerText')), false, 'Chinese journal contains only Chinese history');
  await evaluate('document.getElementById("close-history").click()');
  await send('6'); await send('1');
  assert.equal(await evaluate('document.documentElement.lang'), 'en', 'switching back synchronizes the active English interface');
  await send('6'); await send('2');
  await command('Page.reload');
  await waitFor('document.documentElement.lang === "zh-CN" && document.querySelectorAll(".choice").length === 6 && !document.getElementById("submit").disabled');
  assert.equal(/[A-Za-z]{3,}/.test(await evaluate('document.body.innerText')), false, 'selected language survives refresh');
  const shot = await command('Page.captureScreenshot', {format:'png'});
  await writeFile(join(profile, 'desktop.png'), Buffer.from(shot.data, 'base64'));
  await send('1'); await send('8675309'); await send('网页测试<script>alert(1)</script>'); await send('2');
  await waitFor('document.querySelectorAll(".choice").length === 5');
  await send('kz');
  await waitFor('document.querySelectorAll(".choice").length === 1');
  assert.match(await evaluate('document.getElementById("choices").innerText'), /6.*造物主/s);
  await send('6'); await send('1');
  await waitFor('document.getElementById("choices").innerText.includes("普通攻击")');
  assert.equal(await evaluate('document.querySelectorAll(".health-row.player-health").length >= 1'), true, 'player health uses the aligned green combat row');
  assert.equal(await evaluate('document.querySelectorAll(".health-row.enemy-health").length >= 1'), true, 'enemy health uses the aligned red combat row');
  assert.equal(await evaluate('document.getElementById("shell-language").disabled'), true, 'language shortcut cannot submit combat action 5');
  await evaluate('globalThis.dialogText=""; globalThis.confirm=message=>{globalThis.dialogText=message;return false};document.getElementById("pause").click()');
  assert.equal(/[A-Za-z]{3,}/.test(await evaluate('globalThis.dialogText')), false, 'Chinese pause dialog is monolingual');
  assert.equal(await evaluate('errorText({status:503})'), '服务器暂不可用，请稍后点击重新连接。', 'server errors localized instead of exposing bilingual payload');
  assert.equal(await evaluate('document.querySelectorAll("#screen script").length'), 0, 'player text is not executable HTML');
  await command('Page.reload');
  await waitFor('document.getElementById("choices")?.innerText.includes("普通攻击")');
  await command('Emulation.setDeviceMetricsOverride', {width:390,height:844,deviceScaleFactor:1,mobile:true});
  assert.equal(await evaluate('document.documentElement.scrollWidth <= window.innerWidth'), true, 'mobile page does not overflow');
  const mobile = await command('Page.captureScreenshot', {format:'png'});
  await writeFile(join(profile, 'mobile.png'), Buffer.from(mobile.data, 'base64'));
  // Deterministic protocol fixtures cover the state combinations real random play may miss.
  await evaluate('paused=true;clearTimeout(pollTimer)');
  const translations = await readFile(new URL('../../src/abyss/ui/WorldText.java', import.meta.url), 'utf8');
  for (const pair of translations.matchAll(/Map\.entry\(("(?:[^"\\]|\\.)*"), ("(?:[^"\\]|\\.)*")\)/g)) {
    const en = JSON.parse(pair[1]), zh = JSON.parse(pair[2]);
    if (!en.includes('1. ') || !en.includes('2. ')) continue;
    for (const text of [en,zh]) {
      const expected = [...text.matchAll(/(\d+)\. /g)].map(m=>m[1]);
      assert.deepEqual(await evaluate(`[...choicesFrom(${JSON.stringify(text)}).keys()]`), expected, `all event buttons: ${text}`);
    }
  }
  await evaluate(`globalThis.fixture=(hp,shield)=>'你 [##########] '+hp+'/100  护盾 '+shield+'\\n深渊领主 [##########] 300/300\\n召唤物：\\n1. 洞窟蝙蝠 [##########] 50/100  护盾 20\\n[1] 普通攻击\\n[2] 技能';
    healthSnapshot.clear();renderText(fixture(100,40));globalThis.heroRow=document.querySelector('.player-health');`);
  assert.deepEqual(await evaluate('[...choicesFrom(fixture(100,40)).keys()]'), ['1','2'], 'summon health is never an action');
  assert.equal(await evaluate('document.querySelectorAll(".summon-health").length'),1,'summon HP is visible below the boss');
  assert.equal(await evaluate('document.querySelector(".player-health .shield-fill").getBoundingClientRect().width>0'),true,'full HP still shows ward');
  await evaluate('renderText(fixture(70,0))');
  assert.equal(await evaluate('heroRow===document.querySelector(".player-health")'),true,'health DOM reused between turns');
  assert.equal(await evaluate('heroRow.querySelector(".health-fill").getAnimations().length>0'),true,'HP transition actually runs');
  assert.equal(await evaluate('heroRow.querySelector(".shield-trail").getAnimations().length>0'),true,'breaking shield leaves a trail');
  await evaluate('renderText(fixture(45,10))');
  await delay(950);
  assert.equal(await evaluate('getComputedStyle(heroRow.querySelector(".health-fill")).transform'), 'matrix(0.45, 0, 0, 1, 0, 0)', 'rapid damage settles at exact final HP');
  assert.equal(await evaluate('getComputedStyle(heroRow.querySelector(".shield-fill")).transform'), 'matrix(0.1, 0, 0, 1, 0, 0)', 'ward settles at exact final value');
  await command('Emulation.setEmulatedMedia',{features:[{name:'prefers-reduced-motion',value:'reduce'}]});
  await evaluate('renderText(fixture(65,30))');
  assert.equal(await evaluate('heroRow.querySelector(".health-fill").getAnimations().length'),0,'reduced motion is respected');
  await command('Emulation.setEmulatedMedia',{features:[]});
  await evaluate('renderText("生命 [#####.....] 50/100")');
  assert.equal(await evaluate('document.querySelectorAll(".player-health").length'),1,'status HP is green, not enemy red');
  await evaluate('renderText("Elite Shadow Assassin[######################] 1085/1085  Shield 97")');
  assert.equal(await evaluate('document.querySelectorAll(".enemy-health").length'),1,'long enemy names without padding still render a health row');
  assert.equal(await evaluate('document.getElementById("screen").innerText.includes("[######################]")'),false,'long enemy health text is replaced by the meter');
  for (const width of [320,390,768,851,1024,1280]) {
    await command('Emulation.setDeviceMetricsOverride',{width,height:844,deviceScaleFactor:1,mobile:width<600});
    await evaluate('renderText(fixture(100,40))');
    assert.equal(await evaluate('document.documentElement.scrollWidth<=innerWidth'),true,`page fits ${width}px`);
    assert.equal(await evaluate('document.querySelector(".health-row").scrollWidth<=document.querySelector(".health-row").clientWidth'),true,`health row fits ${width}px`);
    assert.notEqual(await evaluate('getComputedStyle(document.documentElement).overflowY'),'hidden','document scrolling enabled');
  }
  const meters = await command('Page.captureScreenshot',{format:'png'});
  await writeFile(join(profile,'shield-desktop.png'),Buffer.from(meters.data,'base64'));
  await evaluate(`renderText('你发现一座古老神龛。1. 献祭 15 生命  2. 离开')`);
  assert.equal(await evaluate('document.getElementById("screen").innerText.includes("你发现一座古老神龛。")'),true,'event narration is retained');
  // Render an actual protocol-shaped puzzle snapshot and test key & touch controls separately from mechanics (covered by Java tests).
  await evaluate(`state = {...state, ready:true, puzzle:{size:8,moves:4,limit:40,player:10,monster:27,boxes:[18,36],targets:[21,45],shattered:-1,finished:false,won:false},revision:state.revision+1}; const example=state; state=null; render(example); paused=true; clearTimeout(pollTimer);`);
  assert.equal(await evaluate('document.querySelectorAll(".cell").length'), 64, '64 puzzle cells');
  assert.equal(await evaluate('document.querySelectorAll(".cell.core").length'), 2, 'two visible cores');
  await evaluate(`const p={...state.puzzle,targets:[45],shattered:21};renderPuzzle(p);`);
  assert.equal(await evaluate('document.querySelectorAll(".cell.core").length'), 1, 'shattered yellow tile removed');
  await evaluate(`globalThis.capturedMove=null; send=async value=>{globalThis.capturedMove=value}; document.dispatchEvent(new KeyboardEvent('keydown',{key:'ArrowLeft',bubbles:true}));`);
  assert.equal(await evaluate('globalThis.capturedMove'), 'LEFT', 'arrow key without Enter');
  await evaluate(`document.querySelector('[data-move="DOWN"]').click()`);
  assert.equal(await evaluate('globalThis.capturedMove'), 'DOWN', 'touch direction button');
  // The real-time beta is intentionally separate from saves, but its full browser flow must remain usable.
  await command('Page.navigate', {url:`${origin}/realtime-test/`});
  await waitFor('document.querySelectorAll("[data-hero]").length === 5');
  await evaluate('document.querySelector("[data-hero=warrior]").click()');
  await waitFor('document.querySelectorAll("#skills .skill").length === 4');
  const betaEnemyHp = await evaluate('document.getElementById("enemy-hp-text").textContent');
  await evaluate('document.querySelector("#skills .skill").click()');
  await waitFor(`document.getElementById("enemy-hp-text").textContent !== ${JSON.stringify(betaEnemyHp)}`);
  assert.equal(await evaluate('document.querySelector("#skills .skill").disabled'), true, 'real-time beta applies an ability cooldown');
  await evaluate('document.getElementById("pause").click()');
  assert.equal(await evaluate('document.getElementById("pause").textContent'), '继续', 'real-time beta can pause both sides');
  assert.deepEqual(errors, [], 'no browser exceptions');
  console.log(`PASS: Chrome main menu, Chinese, hidden Creator, combat, refresh, mobile layout, puzzle rendering and controls. Screenshots: ${profile}`);
} finally {
  socket?.close(); chrome.kill('SIGTERM');
}
