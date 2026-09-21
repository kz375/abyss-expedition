// Isolated local browser; never opens or controls the user's normal Chrome profile.
import {spawn} from 'node:child_process';
import {mkdtemp,readFile,writeFile} from 'node:fs/promises';
import {createServer} from 'node:http';
import {tmpdir} from 'node:os';
import {join,resolve,extname} from 'node:path';
import assert from 'node:assert/strict';
const root=resolve('web/public'),temp=await mkdtemp(join(tmpdir(),'abyss-beta-qa-'));
const server=createServer(async(req,res)=>{try{const pathname=decodeURIComponent(new URL(req.url,'http://localhost').pathname),path=resolve(root,'.'+pathname);if(!path.startsWith(root+'/'))throw Error();const file=pathname.endsWith('/')?path+'/index.html':path;res.setHeader('Content-Type',({'.js':'text/javascript','.css':'text/css','.html':'text/html','.png':'image/png'})[extname(file)]||'application/octet-stream');res.end(await readFile(file));}catch{res.writeHead(404);res.end();}});
await new Promise((ok,fail)=>{server.once('error',fail);server.listen(0,'127.0.0.1',ok)});
const origin=process.argv[3]||`http://127.0.0.1:${server.address().port}`;
const chrome=spawn(process.argv[2]||'/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',['--headless=new','--no-first-run','--no-default-browser-check',`--user-data-dir=${temp}`,'--remote-debugging-port=0','about:blank'],{stdio:'ignore'});
const delay=ms=>new Promise(r=>setTimeout(r,ms));let socket;let checks=0;const errors=[];
function check(value,message){assert.ok(value,message);checks++;console.log('PASS '+message);}
try{
  let port;for(let i=0;i<100;i++){try{port=Number((await readFile(join(temp,'DevToolsActivePort'),'utf8')).split('\n')[0]);break}catch{await delay(100)}}assert.ok(port,'Chrome starts');
  const targets=await(await fetch(`http://127.0.0.1:${port}/json`)).json();socket=new WebSocket(targets.find(t=>t.type==='page').webSocketDebuggerUrl);await new Promise((ok,fail)=>{socket.onopen=ok;socket.onerror=fail});
  let serial=0;const pending=new Map();socket.onmessage=e=>{const m=JSON.parse(e.data);if(m.id){const p=pending.get(m.id);pending.delete(m.id);m.error?p.reject(Error(JSON.stringify(m.error))):p.resolve(m.result)}else if(m.method==='Runtime.exceptionThrown')errors.push(m.params.exceptionDetails)};
  const command=(method,params={})=>new Promise((resolve,reject)=>{const id=++serial;pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}));});
  const evaluate=async expression=>{const r=await command('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(r.exceptionDetails)throw Error(JSON.stringify(r.exceptionDetails));return r.result.value};
  const wait=async expression=>{for(let i=0;i<100;i++){if(await evaluate(expression))return;await delay(60)}throw Error('Timed out: '+expression+' '+JSON.stringify(errors))};
  const click=async selector=>{await evaluate(`document.querySelector(${JSON.stringify(selector)}).scrollIntoView({block:"center"})`);await delay(350);const p=await evaluate(`(()=>{const e=document.querySelector(${JSON.stringify(selector)}),r=e.getBoundingClientRect(),x=r.x+r.width/2,y=r.y+r.height/2;return {x,y,reachable:e.contains(document.elementFromPoint(x,y))}})()`);assert.ok(p.reachable,'click target is visible and not covered: '+selector);await command('Input.dispatchMouseEvent',{type:'mousePressed',x:p.x,y:p.y,button:'left',clickCount:1});await delay(100);await command('Input.dispatchMouseEvent',{type:'mouseReleased',x:p.x,y:p.y,button:'left',clickCount:1});};
  await command('Runtime.enable');await command('Page.enable');await command('Emulation.setDeviceMetricsOverride',{width:1280,height:960,deviceScaleFactor:1,mobile:false});await command('Page.navigate',{url:origin+'/realtime-test/'});await wait('typeof game!=="undefined" && game.phase==="menu"');
  check(await evaluate('globalThis.ABYSS_BETA_BUILD==="1.7.0"'),'all scripts use Beta build 1.7.0');
  check(await evaluate('document.querySelectorAll("[data-hero]").length===5'),'five visible classes, Creator hidden');
  check(await evaluate('/^[0-9a-f-]{36}$/i.test(profile.accountId)'),'player receives a persistent account identity');
  await click('#menu-warehouse');check(await evaluate('archiveKind==="warehouse"&&$("archive-layer").innerText.includes(profile.accountId)'),'empty account warehouse opens with its account id');await click('#archive-close');
  check(await evaluate('new Set(Object.values(HEROES).map(h=>JSON.stringify(h.skills.map(s=>s[3])))).size===6'),'six classes expose distinct skill kits');
  check(await evaluate('Object.values(HEROES).flatMap(h=>h.skills).every(s=>!["heal","cleanse","drain"].includes(s[3]))'),'no class retains an active healing skill');
  await command('Emulation.setDeviceMetricsOverride',{width:390,height:667,deviceScaleFactor:1,mobile:true});await delay(800);
  await writeFile(join(temp,'character-selection.png'),Buffer.from((await command('Page.captureScreenshot',{format:'png'})).data,'base64'));
  check(await evaluate('(()=>{const r=$("overlay").getBoundingClientRect();return Math.abs(r.top)<1&&Math.abs(r.bottom-innerHeight)<1})()'),'character selection overlay covers the viewport '+await evaluate('JSON.stringify({overlay:$("overlay").getBoundingClientRect().toJSON(),height:innerHeight})'));
  for(const [width,height] of [[320,568],[390,667],[844,390],[1280,720]]){
    await command('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:width<900});
    await click('#menu-language');await click('#menu-language');
    await click('[data-hero="necromancer"]');
    check(await evaluate('game.heroId==="necromancer"&&game.phase==="battle"'),`last visible class starts at ${width}x${height}`);
    await evaluate('$("restart").click();clearRun();renderScene()');
  }
  await command('Emulation.setDeviceMetricsOverride',{width:1280,height:960,deviceScaleFactor:1,mobile:false});
  await evaluate('$("seed").value="1234";$("seed").focus()');await command('Input.dispatchKeyEvent',{type:'keyDown',key:'1'});check(await evaluate('game.phase==="menu"'),'typing digits does not cast');
  await evaluate('$("creator-code").value="kz";$("unlock-creator").click()');check(await evaluate('document.querySelectorAll("[data-hero]").length===6'),'secret unlock works');
  await evaluate('Object.defineProperty(crypto,"randomUUID",{value:undefined,configurable:true})');
  for(const id of ['warrior','mage','ranger','paladin','necromancer','creator'])for(const mode of ['explorer','adventurer','nightmare','ultra']){
    await evaluate(`$("seed").value="selection-matrix";$("difficulty").value=${JSON.stringify(mode)}`);
    await click(`[data-hero="${id}"]`);
    check(await evaluate(`game.heroId===${JSON.stringify(id)}&&game.difficulty===${JSON.stringify(mode)}&&game.seed==="selection-matrix"&&validRun(game)`),`select ${id}/${mode} without randomUUID`);
    await evaluate('$("restart").click();clearRun();renderScene()');
  }
  await click('[data-hero="warrior"]');await wait('game.phase==="battle"');check(await evaluate('$("overlay").hidden'),'class selection closes and battle starts');
  check(await evaluate('/药瓶|Healing Bottle/.test($("potion").textContent)'),'healing bottle replaces active healing skills');
  const before=await evaluate('game.stats.casts');await click('#skills button');check(await evaluate('game.stats.casts')>before,'physical pointer click works across animation frames');
  await evaluate('$("pause").click()');const clock=await evaluate('game.clock');await delay(150);check(await evaluate('game.clock')===clock,'pause freezes simulation clock');
  await evaluate('game.enemy.hp=0;checkEnd()');const picks=await evaluate('JSON.stringify(game.rewardOffers)');await command('Page.reload');await wait('typeof game!=="undefined"&&game.phase==="menu"');await click('#resume-run');check(await evaluate('game.phase==="reward"'),'reload restores reward stage '+await evaluate('JSON.stringify({phase:game.phase,error:saveError,valid:validRun(JSON.parse(localStorage.getItem(BETA_RUN_KEY)).run)})'));check(await evaluate('JSON.stringify(game.rewardOffers)')===picks,'reward choices do not reroll');
  await evaluate('$("codex").click()');check(await evaluate('!$("archive-layer").hidden'),'codex opens separately');await click('#archive-close');check(await evaluate('game.phase==="reward"&&!$("overlay").hidden'),'closing codex preserves reward controls');
  await click('[data-relic]');check(await evaluate('game.phase==="events"&&document.querySelectorAll("[data-event]").length===4'),'reward leads to four selectable events');
  await evaluate('profile.locale="en";writeProfile();renderScene()');const english=await evaluate('document.body.innerText.replace("简体中文", "")');check(!/[\u3400-\u9fff]/.test(english),'English events and battle UI contain no Chinese text');
  await evaluate('game.eventOffers=["oracle","shop","trial","camp"];commit()');await click('[data-event="oracle"]');await click('[data-option="0"]');check(await evaluate('game.phase==="result"&&$("overlay").innerText.includes("Floor")'),'oracle result remains visible');await click('#next-floor');check(await evaluate('game.floor===1&&game.phase==="battle"'),'next floor advances once');
  await evaluate('game.floor=7;beginBattle({index:20,elite:false});spawnSummon();spawnSummon();game.paused=true;commit()');
  check(await evaluate('$("summons").children.length===2'),'two named summons display health bars');
  await writeFile(join(temp,'desktop.png'),Buffer.from((await command('Page.captureScreenshot',{format:'png'})).data,'base64'));
  await command('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});await delay(150);
  await writeFile(join(temp,'mobile.png'),Buffer.from((await command('Page.captureScreenshot',{format:'png'})).data,'base64'));
  check(await evaluate('document.documentElement.scrollWidth<=390'),'mobile has no horizontal overflow '+await evaluate('JSON.stringify({width:document.documentElement.scrollWidth,offenders:[...document.querySelectorAll("body *")].filter(e=>e.getBoundingClientRect().right>391).map(e=>[e.tagName,e.id,e.className,e.getBoundingClientRect().right])})'));
  check(await evaluate('(()=>{const s=$("skills").getBoundingClientRect(),f=$("enemy-card").getBoundingClientRect();return s.top>=f.bottom})()'),'mobile skills do not overlap the enemy card');
  check(await evaluate('(()=>{const s=$("summons").getBoundingClientRect(),i=$("intent").getBoundingClientRect();return s.bottom<=i.top})()'),'summon bars do not overlap enemy intent');
  await writeFile(join(temp,'mobile.png'),Buffer.from((await command('Page.captureScreenshot',{format:'png'})).data,'base64'));
  await evaluate('startTrial();commit()');check(await evaluate('document.querySelectorAll(".tile").length===64'),'trial renders 8 by 8 grid');await command('Page.reload');await wait('typeof game!=="undefined"&&game.phase==="menu"');await evaluate('restoreRun()');check(await evaluate('game.phase==="trial"&&document.querySelectorAll(".tile").length===64'),'trial restores after refresh');
  await evaluate('settleTrial(false);commit();profile.locale="zh";renderScene()');check(await evaluate('$("overlay").innerText.includes("当前生命")'),'trial penalty is current HP, matching formal rules');
  await evaluate('finish(false)');await command('Page.reload');await wait('typeof game!=="undefined"&&game.phase==="menu"');check(await evaluate('!$("resume-run")'),'defeated runs cannot be continued');
  await evaluate('localStorage.setItem(BETA_RUN_KEY,JSON.stringify({heroId:"warrior",version:1}));renderScene()');
  await click('#resume-run');check(await evaluate('game.phase==="menu"&&!!document.querySelector(".menu-error")&&hasLocalSave()'),'incompatible saves show a visible error and remain recoverable');
  await evaluate('localStorage.setItem(BETA_RUN_KEY,"original-save");globalThis.originalSet=Storage.prototype.setItem;Storage.prototype.setItem=function(){throw new Error("Quota exceeded")};globalThis.originalConfirm=confirm;globalThis.confirm=()=>true;renderScene()');
  await click('[data-hero="warrior"]');check(await evaluate('game.phase==="menu"&&document.querySelector(".menu-error").textContent.includes("备份")&&localStorage.getItem(BETA_RUN_KEY)==="original-save"'),'backup failure is visible without overwriting the old save');
  await evaluate('Storage.prototype.setItem=originalSet;globalThis.confirm=originalConfirm;clearRun();renderScene();globalThis.originalGet=Storage.prototype.getItem;Storage.prototype.getItem=function(){throw new Error("Storage blocked")};Storage.prototype.setItem=function(){throw new Error("Storage blocked")}');
  await click('[data-hero="mage"]');check(await evaluate('game.heroId==="mage"&&game.phase==="battle"&&saveError.length>0'),'blocked storage allows play and reports that progress is unsaved');
  await evaluate('Storage.prototype.getItem=originalGet;Storage.prototype.setItem=originalSet;saveRun();openArchive("codex");restoreRun()');
  check(await evaluate('archiveKind===null&&$("archive-layer").hidden&&game.paused'),'restoring a run closes stale archive layers and pauses combat');
  await evaluate('finish(true)');check(await evaluate('profile.warehouse.length>=1&&profile.warehouse.length<=2&&game.extractedItems.length===profile.warehouse.length&&profile.warehouse.every(validGear)'),'victory extracts valid difficulty-scaled gear to the account warehouse');
  await click('#again');await click('#menu-warehouse');check(await evaluate('document.querySelectorAll("[data-equip]").length===profile.warehouse.length'),'all extracted items appear in warehouse');await click('[data-equip]');
  const equipped=await evaluate('profile.equipped[profile.warehouse[0].slot]||Object.values(profile.equipped).find(Boolean)');await click('#archive-close');await click('[data-hero="warrior"]');check(await evaluate(`Object.values(game.loadout).some(item=>item?.id===${JSON.stringify(equipped)})`),'equipped warehouse item is snapshotted into the next run');
  const firstAccount=await evaluate('profile.accountId');await evaluate('game.finished=true;localStorage.clear()');await command('Page.reload');await wait('typeof game!=="undefined"&&game.phase==="menu"');check(await evaluate(`profile.accountId!==${JSON.stringify(firstAccount)}&&profile.warehouse.length===0`),'a separate browser profile receives an independent empty account');
  check(errors.length===0,'no browser runtime exceptions: '+JSON.stringify(errors));console.log(`${checks} browser checks passed. Screenshots: ${temp}`);
}finally{socket?.close();chrome.kill();await new Promise(r=>server.close(r));}
