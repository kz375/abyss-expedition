"use strict";
// Pure combat rules. Runtime owns state mutations; UI/VFX only consume emitted events.
globalThis.CombatCore=Object.freeze({
  PRESSURE_MAX:100,
  PERFECT_GUARD_MS:260,
  HOLD_GUARD_MS:300,
  HOLD_WARD:12,
  GUARD_COOLDOWN_MS:1400,
  BROKEN_MS:3600,
  PLAYER_BREAK_GAIN:.72,
  intents:Object.freeze({
    bite:{windup:850,kind:"attack"},sunder:{windup:1150,kind:"heavy"},poison:{windup:1000,kind:"special"},
    double:{windup:900,kind:"combo"},weak:{windup:1050,kind:"special"},burn:{windup:1050,kind:"special"},
    drain:{windup:1200,kind:"heavy"},curse:{windup:1050,kind:"special"},sweep:{windup:1250,kind:"heavy"},
    shield:{windup:700,kind:"defend"},heal:{windup:900,kind:"defend"},counter:{windup:750,kind:"defend"},
    rage:{windup:750,kind:"defend"},summon:{windup:1100,kind:"special"},dispel:{windup:950,kind:"special"},
    charge:{windup:1400,kind:"heavy"},nova:{windup:1500,kind:"ultimate"}
  }),
  create(mode="expedition"){return {mode,pressure:0,tier:0,encounter:0,guard:false,guardStarted:-1,guardTapUntil:-1,guardCooldownUntil:0,guardWardGranted:false,wave:1,events:[],sequence:0,phantomAt:7000};},
  pressure(deltaMs,mode){return deltaMs/(mode==="endless"?760:380);},
  tier(value){return value>=85?4:value>=65?3:value>=40?2:value>=18?1:0;},
  playerPower(value){return 1+value*.004;},
  breakPower(value){return 1+value*.002;},
  enemyPower(value){return 1+value*.003;},
  enemySpeed(value){return 1+value*.004;},
  rewardPower(value){return 1+value*.006;},
  guardReduction(elapsedMs){return Math.max(.2,.65-Math.max(0,elapsedMs-this.HOLD_GUARD_MS)*.00011);},
  intent(action,pressure){const base=this.intents[action]||{windup:900,kind:"attack"};return {...base,windup:Math.max(520,base.windup/this.enemySpeed(pressure))};},
  emit(combat,type,payload={}){const event={id:++combat.sequence,type,...payload};combat.events.push(event);if(combat.events.length>24)combat.events.splice(0,combat.events.length-24);return event;}
});
