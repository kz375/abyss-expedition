"use strict";
const RIG_CHARACTER="/assets/animation/characters/warrior-v1.json";
let rigModel=null,rigTimer=null;
async function loadRig(){
  const character=await fetch(RIG_CHARACTER).then(r=>{if(!r.ok)throw Error("Character rig missing");return r.json();});
  const [skeleton,skin,actionSet]=await Promise.all([fetch(character.skeleton).then(r=>r.json()),fetch(character.skin).then(r=>r.json()),fetch(character.actions).then(r=>r.json())]);
  const host=document.getElementById("hero-rig");if(!host)return;
  host.innerHTML=`<b class="rig-root">${character.parts.map(id=>`<i class="rig-part part-${id.replaceAll(".","-")}" data-part="${id}"></i>`).join("")}</b>`;
  const bones=new Map(skeleton.bones.map(bone=>[bone.id,bone])),baseAngles=new Map();
  for(const id of character.parts){const node=host.querySelector(`[data-part="${id}"]`);if(!node)continue;const matrix=getComputedStyle(node).transform;if(matrix&&matrix!=="none"){const values=matrix.match(/matrix\(([^)]+)\)/)?.[1].split(",").map(Number);if(values)baseAngles.set(id,Math.atan2(values[1],values[0])*180/Math.PI);}}
  rigModel={character,skeleton,skin,actionSet,host,bones,baseAngles};globalThis.rigModel=rigModel;host.dataset.bones=String(bones.size);host.dataset.skin=skin.id;playRigAction("idle");
}
function worldPose(id,pose,seen=new Set()){
  if(!rigModel||seen.has(id))return {x:0,y:0,r:0};seen.add(id);const bone=rigModel.bones.get(id),parent=bone?.parent||rigModel.character.attachments?.[id]||null,local=pose[id]||{},up=parent?worldPose(rigModel.skeleton.sockets?.[parent]||parent,pose,seen):{x:0,y:0,r:0};
  return {x:up.x+(local.x||0),y:up.y+(local.y||0),r:up.r+(local.r||0)};
}
function poseTransform(id,pose){const value=worldPose(id,pose),base=rigModel.baseAngles.get(id)||0;return `translate(${value.x}px,${value.y}px) rotate(${base+value.r}deg)`;}
function playRigAction(name){
  if(!rigModel)return;clearTimeout(rigTimer);const action=rigModel.actionSet.actions[name]||rigModel.actionSet.actions.idle;
  rigModel.host.dataset.action=name;const frames=action.poses.map(p=>({offset:p.at/action.duration,...p}));
  const partIds=["root",...rigModel.character.parts];
  partIds.forEach(id=>{const node=id==="root"?rigModel.host.querySelector(".rig-root"):rigModel.host.querySelector(`[data-part="${id}"]`);if(!node)return;node.getAnimations().forEach(animation=>animation.cancel());node.animate(frames.map(frame=>({offset:frame.offset,transform:poseTransform(id,frame)})),{duration:action.duration,iterations:action.loop?Infinity:1,easing:"ease-in-out",fill:"forwards"});});
  if(!action.loop)rigTimer=setTimeout(()=>playRigAction("idle"),action.duration+40);
}
globalThis.playRigAction=playRigAction;
addEventListener("DOMContentLoaded",()=>loadRig().catch(error=>{document.getElementById("hero-rig")?.setAttribute("data-error",error.message);}));
