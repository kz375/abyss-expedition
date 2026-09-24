"use strict";
// Production combat adapter. Character art, skeleton data and actions remain
// independent, so replacing a skin never changes combat rules or animation code.
const COMBAT_RIG_CHARACTERS=Object.freeze({warrior:"warrior-v1",mage:"mage-v1",ranger:"ranger-v1",paladin:"paladin-v1",necromancer:"necromancer-v1"});
globalThis.COMBAT_RIG_CHARACTERS=COMBAT_RIG_CHARACTERS;
const COMBAT_RIG_PARTS=new Set(["pelvis","torso","head","upperArm_L","lowerArm_L","hand_L","upperArm_R","lowerArm_R","hand_R","thigh_L","shin_L","foot_L","thigh_R","shin_R","foot_R"]);
let combatRig=null,combatRigLoad=0,combatRigTimer=0;
const rigFetch=async path=>{const response=await fetch(path);if(!response.ok)throw Error(`Rig asset missing: ${path}`);return response.json();};
function combatRigUrl(path){return new URL(path,document.baseURI).href;}
async function mountCombatRig(heroId,portrait){
  const id=COMBAT_RIG_CHARACTERS[heroId];
  if(!id){unmountCombatRig(portrait);return false;}
  if(combatRig?.heroId===heroId&&combatRig.host?.isConnected)return true;
  const request=++combatRigLoad,character=await rigFetch(`/assets/animation/characters/${id}.json`);
  const [skeleton,skin,actions,locomotion]=await Promise.all([rigFetch(character.skeleton),rigFetch(character.skin),rigFetch(character.actions),rigFetch(character.locomotion)]);
  if(request!==combatRigLoad)return false;
  portrait.querySelectorAll("img,.combat-rig").forEach(node=>node.remove());
  const host=document.createElement("span");host.className=`combat-rig production-rig rig-${heroId} rig-type-${skin.type||"humanoid"}`;host.dataset.hero=heroId;host.dataset.action="idle";host.innerHTML=`<b class="combat-rig-root">${character.parts.filter(part=>COMBAT_RIG_PARTS.has(part)||skin.attachments?.[part]).map(part=>`<i class="combat-rig-part part-${part}" data-part="${part}"></i>`).join("")}</b><span class="combat-rig-fx" aria-hidden="true"></span>`;portrait.append(host);
  const binding=skin.rigTextureBinding||{},size=binding.displaySize||[240,320],offset=binding.offset||[-25,3];host.style.setProperty("--rig-texture",`url(${combatRigUrl(skin.rigTexture)})`);host.style.setProperty("--rig-texture-w",`${size[0]}px`);host.style.setProperty("--rig-texture-h",`${size[1]}px`);host.style.setProperty("--rig-texture-x",`${offset[0]}px`);host.style.setProperty("--rig-texture-y",`${offset[1]}px`);host.dataset.fx=skin.fx||heroId;
  for(const [part,path] of Object.entries(skin.attachments||{}))if(path?.startsWith("/"))host.style.setProperty(`--${part}-texture`,`url(${combatRigUrl(path)})`);
  const bones=new Map(skeleton.bones.map(bone=>[bone.id,bone])),baseAngles=new Map(),joints=new Map([["root",{x:host.clientWidth/2,y:host.clientHeight}]]);
  for(const part of character.parts){const node=host.querySelector(`[data-part="${part}"]`);if(!node)continue;const style=getComputedStyle(node),matrix=style.transform;if(matrix&&matrix!=="none"){const values=matrix.match(/matrix\(([^)]+)\)/)?.[1].split(",").map(Number);if(values)baseAngles.set(part,Math.atan2(values[1],values[0])*180/Math.PI);}if(bones.has(part)){const origin=style.transformOrigin.split(" ").map(parseFloat);joints.set(part,{x:node.offsetLeft+(origin[0]||0),y:node.offsetTop+(origin[1]||0)});}}
  combatRig={heroId,character,skeleton,skin,actions,locomotion,host,bones,baseAngles,joints};host.dataset.ready="true";playCombatRig("idle");return true;
}
function unmountCombatRig(portrait){combatRigLoad++;clearTimeout(combatRigTimer);combatRig=null;portrait?.querySelector(".combat-rig")?.remove();}
function rigWorld(part,pose,cache=new Map()){
  if(!combatRig)return {x:0,y:0,r:0};const socket=combatRig.character.attachments?.[part],resolved=socket?combatRig.skeleton.sockets?.[socket]:part;if(resolved!==part)return rigWorld(resolved,pose,cache);if(cache.has(part))return cache.get(part);const bone=combatRig.bones.get(part);if(!bone)return {x:0,y:0,r:0};const local=pose[part]||{},joint=combatRig.joints.get(part)||{x:0,y:0};let value;
  if(!bone.parent)value={x:local.x||0,y:local.y||0,r:local.r||0,worldX:joint.x+(local.x||0),worldY:joint.y+(local.y||0)};else{const parent=rigWorld(bone.parent,pose,cache),parentJoint=combatRig.joints.get(bone.parent)||{x:0,y:0},angle=parent.r*Math.PI/180,dx=joint.x-parentJoint.x,dy=joint.y-parentJoint.y,worldX=parent.worldX+dx*Math.cos(angle)-dy*Math.sin(angle)+(local.x||0),worldY=parent.worldY+dx*Math.sin(angle)+dy*Math.cos(angle)+(local.y||0);value={x:worldX-joint.x,y:worldY-joint.y,r:parent.r+(local.r||0),worldX,worldY};}cache.set(part,value);return value;
}
function rigTransform(part,pose){const value=rigWorld(part,pose),base=combatRig.baseAngles.get(part)||0;return `translate(${value.x}px,${value.y}px) rotate(${base+value.r}deg)`;}
function rigMerge(body,legs){const flipped=Object.fromEntries(Object.entries(legs||{}).map(([part,value])=>[part,value&&Number.isFinite(value.r)?{...value,r:-value.r}:value])),out={...flipped,...body};for(const part of Object.keys(flipped))if(body[part])out[part]={...flipped[part],...body[part]};return out;}
function playCombatRig(name="idle"){
  if(!combatRig)return false;clearTimeout(combatRigTimer);const action=combatRig.actions.actions[name]||combatRig.actions.actions.idle,duration=action.duration,authored=action.loop||action.hold?action.poses:[...action.poses.slice(0,-1),{...action.poses.at(-2),at:action.duration}],legs=combatRig.locomotion.profiles[name]||[],poses=authored.map((pose,index)=>rigMerge(pose,legs[Math.min(index,legs.length-1)]||{})),frames=poses.map(pose=>({offset:pose.at/action.duration,...pose}));combatRig.host.dataset.action=name;combatRig.host.dataset.playing="true";
  const root=combatRig.host.querySelector(".combat-rig-root"),stage=combatRig.locomotion.stage?.[name]||[],current=getComputedStyle(root).transform;root.getAnimations().forEach(animation=>animation.cancel());root.animate([{offset:0,transform:current==="none"?"none":current},...authored.map((pose,index)=>{const move=stage[Math.min(index,stage.length-1)]||{};return {offset:pose.at/action.duration,transform:`translate(${(move.x||0)*.46}px,${(move.y||0)*.46}px) rotate(${move.r||0}deg) rotateY(${(move.yaw||0)*.45}deg)`,easing:pose.easing||"ease-in-out"};})],{duration,iterations:action.loop?Infinity:1,fill:"forwards"});
  for(const part of combatRig.character.parts){const node=combatRig.host.querySelector(`[data-part="${part}"]`);if(!node)continue;const now=getComputedStyle(node).transform;node.getAnimations().forEach(animation=>animation.cancel());node.animate([{offset:0,transform:now==="none"?rigTransform(part,frames[0]):now},...frames.map(frame=>({offset:frame.offset,transform:rigTransform(part,frame),easing:frame.easing||"ease-in-out"}))],{duration,iterations:action.loop?Infinity:1,fill:"forwards"});}
  if(!action.loop&&!action.hold)combatRigTimer=setTimeout(()=>playCombatRig("idle"),action.duration+180);return true;
}
function combatRigEffect(type,index=0){if(!combatRig)return;const fx=combatRig.host.querySelector(".combat-rig-fx");fx.replaceChildren();fx.dataset.type=type;fx.dataset.skill=String(index);for(let i=0;i<(index===2?9:6);i++){const spark=document.createElement("i");spark.style.setProperty("--i",i);spark.style.setProperty("--a",`${i*(360/(index===2?9:6))}deg`);fx.append(spark);}const wave=document.createElement("b");fx.append(wave);clearTimeout(fx._timer);fx._timer=setTimeout(()=>fx.replaceChildren(),700);}
function playCombatSkill(heroId,index,type){const defensive=["shield","purify","rewind","rewrite"].includes(type),action=defensive?"guard":index===0?"attack_01":index===1?"attack_02":"break_strike";playCombatRig(action);combatRigEffect(type,index);}
globalThis.mountCombatRig=mountCombatRig;globalThis.playCombatRig=playCombatRig;globalThis.playCombatSkill=playCombatSkill;globalThis.unmountCombatRig=unmountCombatRig;
