"use strict";
const RIG_ASSET_ROOT=new URL("../assets/animation/",document.currentScript.src);
const RIG_CHARACTER=new URL("characters/warrior-v1.json",RIG_ASSET_ROOT);
const rigAssetUrl=path=>new URL(path.replace(/^\/assets\/animation\//,""),RIG_ASSET_ROOT);
let rigModel=null,rigTimer=null,pendingRigAction="idle";
async function loadRig(){
  const character=await fetch(RIG_CHARACTER).then(r=>{if(!r.ok)throw Error("Character rig missing");return r.json();});
  const [skeleton,skin,actionSet,locomotion]=await Promise.all([fetch(rigAssetUrl(character.skeleton)).then(r=>{if(!r.ok)throw Error("Skeleton missing");return r.json();}),fetch(rigAssetUrl(character.skin)).then(r=>{if(!r.ok)throw Error("Skin missing");return r.json();}),fetch(rigAssetUrl(character.actions)).then(r=>{if(!r.ok)throw Error("Action set missing");return r.json();}),fetch(rigAssetUrl(character.locomotion)).then(r=>{if(!r.ok)throw Error("Locomotion set missing");return r.json();})]);
  const host=document.getElementById("hero-rig");if(!host)return;
  host.innerHTML=`<b class="rig-root">${character.parts.map(id=>`<i class="rig-part part-${id.replaceAll(".","-")}" data-part="${id}"></i>`).join("")}</b>`;
  const bones=new Map(skeleton.bones.map(bone=>[bone.id,bone])),baseAngles=new Map(),joints=new Map([["root",{x:host.clientWidth/2,y:host.clientHeight}]]);
  for(const id of character.parts){const node=host.querySelector(`[data-part="${id}"]`);if(!node)continue;const style=getComputedStyle(node),matrix=style.transform;if(matrix&&matrix!=="none"){const values=matrix.match(/matrix\(([^)]+)\)/)?.[1].split(",").map(Number);if(values)baseAngles.set(id,Math.atan2(values[1],values[0])*180/Math.PI);}if(bones.has(id)){const origin=style.transformOrigin.split(" ").map(parseFloat);joints.set(id,{x:node.offsetLeft+(origin[0]||0),y:node.offsetTop+(origin[1]||0)});}}
  rigModel={character,skeleton,skin,actionSet,locomotion,host,bones,baseAngles,joints};globalThis.rigModel=rigModel;host.dataset.bones=String(bones.size);host.dataset.skin=skin.id;host.dataset.locomotion=locomotion.id;host.dataset.ready="true";delete host.dataset.error;const status=document.getElementById("rig-action-name");if(status)status.textContent="READY · IDLE";playRigAction(pendingRigAction);
}
function worldPose(id,pose,cache=new Map()){
  if(!rigModel)return {x:0,y:0,r:0};
  const socketTarget=rigModel.character.attachments?.[id],resolved=socketTarget?rigModel.skeleton.sockets?.[socketTarget]:id;
  if(resolved!==id)return worldPose(resolved,pose,cache);
  if(cache.has(id))return cache.get(id);
  const bone=rigModel.bones.get(id);if(!bone)return {x:0,y:0,r:0};
  const local=pose[id]||{},joint=rigModel.joints.get(id)||{x:0,y:0},baseX=joint.x,baseY=joint.y;
  let value;
  if(!bone.parent)value={x:local.x||0,y:local.y||0,r:local.r||0,worldX:baseX+(local.x||0),worldY:baseY+(local.y||0)};
  else{const parent=worldPose(bone.parent,pose,cache),parentJoint=rigModel.joints.get(bone.parent)||{x:0,y:0},angle=parent.r*Math.PI/180,dx=baseX-parentJoint.x,dy=baseY-parentJoint.y,rotX=dx*Math.cos(angle)-dy*Math.sin(angle),rotY=dx*Math.sin(angle)+dy*Math.cos(angle),worldX=parent.worldX+rotX+(local.x||0),worldY=parent.worldY+rotY+(local.y||0);value={x:worldX-baseX,y:worldY-baseY,r:parent.r+(local.r||0),worldX,worldY};}
  cache.set(id,value);return value;
}
function poseTransform(id,pose){const value=worldPose(id,pose),base=rigModel.baseAngles.get(id)||0;return `translate(${value.x}px,${value.y}px) rotate(${base+value.r}deg)`;}
function mergePose(body,legs){const screenLegs=Object.fromEntries(Object.entries(legs).map(([id,value])=>[id,value&&typeof value==="object"&&Number.isFinite(value.r)?{...value,r:-value.r}:value])),merged={...screenLegs,...body};for(const id of new Set([...Object.keys(screenLegs),...Object.keys(body)]))if(screenLegs[id]&&body[id]&&typeof screenLegs[id]==="object"&&typeof body[id]==="object")merged[id]={...screenLegs[id],...body[id]};return merged;}
function playRigAction(name){
  pendingRigAction=name;if(!rigModel)return false;clearTimeout(rigTimer);const action=rigModel.actionSet.actions[name]||rigModel.actionSet.actions.idle;
  rigModel.host.dataset.action=name;rigModel.host.dataset.playing="true";
  const status=document.getElementById("rig-action-name");if(status)status.textContent=name==="idle"?"READY · IDLE":`▶ ${name.replaceAll("_"," ").toUpperCase()} · PLAYING`;
  // Preview ordinary moves at their impact pose. Guard and death own an authored
  // hold pose and stay there until another action is selected.
  const bodyPoses=action.loop||action.hold?action.poses:[...action.poses.slice(0,-1),{...action.poses.at(-2),at:action.duration}],legPoses=rigModel.locomotion.profiles[name]||[];
  const poses=bodyPoses.map((pose,index)=>mergePose(pose,legPoses[Math.min(index,legPoses.length-1)]||{}));
  const frames=poses.map(p=>({offset:p.at/action.duration,...p}));
  const stageNode=rigModel.host.querySelector(".rig-root"),stageTrack=rigModel.locomotion.stage?.[name]||[],stageCurrent=getComputedStyle(stageNode).transform;stageNode.getAnimations().forEach(animation=>animation.cancel());const stageFrames=bodyPoses.map((pose,index)=>{const move=stageTrack[Math.min(index,stageTrack.length-1)]||{};return {offset:pose.at/action.duration,transform:`translate(${move.x||0}px,${move.y||0}px) rotate(${move.r||0}deg)`,easing:pose.easing||"ease-in-out"};}),stageSecond=stageFrames[1]?.offset||.25,stageBlend=Math.min(stageSecond*.45,(rigModel.actionSet.transitionMs||80)/action.duration);stageNode.animate([{offset:0,transform:stageCurrent==="none"?stageFrames[0].transform:stageCurrent,easing:"ease-out"},{...stageFrames[0],offset:stageBlend},...stageFrames.slice(1)],{duration:action.duration,iterations:action.loop?Infinity:1,easing:"linear",fill:"forwards"});
  rigModel.character.parts.forEach(id=>{const node=rigModel.host.querySelector(`[data-part="${id}"]`);if(!node)return;const current=getComputedStyle(node).transform;node.getAnimations().forEach(animation=>animation.cancel());const authored=frames.map(frame=>({offset:frame.offset,transform:poseTransform(id,frame),easing:frame.easing||"ease-in-out"})),second=authored[1]?.offset||.25,blendOffset=Math.min(second*.45,(rigModel.actionSet.transitionMs||80)/action.duration),keyframes=[{offset:0,transform:current==="none"?authored[0].transform:current,easing:"ease-out"},{...authored[0],offset:blendOffset},...authored.slice(1)];node.animate(keyframes,{duration:action.duration,iterations:action.loop?Infinity:1,easing:"linear",fill:"forwards"});});
  if(!action.loop&&!action.hold)rigTimer=setTimeout(()=>playRigAction("idle"),action.duration+720);
  return true;
}
globalThis.playRigAction=playRigAction;
addEventListener("DOMContentLoaded",()=>loadRig().catch(error=>{const host=document.getElementById("hero-rig"),status=document.getElementById("rig-action-name");host?.setAttribute("data-error",error.message);if(status)status.textContent=`RIG ERROR · ${error.message}`;console.error("Art Lab rig failed to load",error);}));
