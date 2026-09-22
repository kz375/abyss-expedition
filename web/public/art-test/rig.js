"use strict";
const RIG_CHARACTER="/assets/animation/characters/warrior-initial.json";
let rigModel=null,rigTimer=null;
async function loadRig(){
  const character=await fetch(RIG_CHARACTER).then(r=>{if(!r.ok)throw Error("Character rig missing");return r.json();});
  const [skeleton,actionSet]=await Promise.all([fetch(character.skeleton).then(r=>r.json()),fetch(character.actions).then(r=>r.json())]);
  const host=document.getElementById("hero-rig");if(!host)return;
  host.innerHTML=`<b class="rig-root">${character.parts.map(id=>`<i class="rig-part part-${id.replaceAll(".","-")}" data-part="${id}"></i>`).join("")}</b>`;
  rigModel={character,skeleton,actionSet,host};playRigAction("idle");
}
function poseTransform(value={}){return `translate(${value.x||0}px,${value.y||0}px) rotate(${value.r||0}deg)`;}
function playRigAction(name){
  if(!rigModel)return;clearTimeout(rigTimer);const action=rigModel.actionSet.actions[name]||rigModel.actionSet.actions.idle;
  rigModel.host.dataset.action=name;const frames=action.poses.map(p=>({offset:p.at/action.duration,...p}));
  const partIds=new Set(frames.flatMap(frame=>Object.keys(frame).filter(key=>key!=="at"&&key!=="offset")));
  partIds.forEach(id=>{const node=id==="root"?rigModel.host.querySelector(".rig-root"):rigModel.host.querySelector(`[data-part="${id}"]`);if(!node)return;node.getAnimations().forEach(animation=>animation.cancel());node.animate(frames.map(frame=>({offset:frame.offset,transform:poseTransform(frame[id])})),{duration:action.duration,iterations:action.loop?Infinity:1,easing:"ease-in-out",fill:"forwards"});});
  if(!action.loop)rigTimer=setTimeout(()=>playRigAction("idle"),action.duration+40);
}
globalThis.playRigAction=playRigAction;
addEventListener("DOMContentLoaded",()=>loadRig().catch(error=>{document.getElementById("hero-rig")?.setAttribute("data-error",error.message);}));
