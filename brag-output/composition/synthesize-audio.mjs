// Original procedural music and tap accents. No samples or external recordings.
import {writeFileSync} from 'node:fs';
const rate=48000, duration=20, count=rate*duration;
const samples=new Float64Array(count);
let seed=78391;
function noise(){seed=(Math.imul(1664525,seed)+1013904223)>>>0;return seed/2147483648-1;}
function add(start,len,fn,gain=1){const first=Math.round(start*rate),n=Math.min(Math.round(len*rate),count-first);for(let j=0;j<n;j++)samples[first+j]+=fn(j/rate,j)*gain;}
const beat=60/96;
const roots=[146.832,146.832,116.541,116.541,174.614,174.614,130.813,130.813];
for(let b=0;b<32;b++){
 const t=b*beat,bar=Math.floor(b/4),root=roots[bar];
 if(b%2===0)add(t,.28,x=>Math.sin(2*Math.PI*(45*x+6*(1-Math.exp(-x*28))))*Math.exp(-x*18),.12);
 add(t+beat*.5,.08,x=>noise()*Math.exp(-x*85),.018);
 if(b%4===2)add(t,.12,x=>noise()*Math.exp(-x*45)+Math.sin(2*Math.PI*170*x)*Math.exp(-x*55),.025);
 add(t,.48,x=>(Math.sin(2*Math.PI*root*x)+.22*Math.sin(2*Math.PI*root*2*x))*Math.exp(-x*7)*(1-Math.exp(-x*90)),.065);
 if(b%2===1){const f=root*(b%4===1?2:3);add(t,.6,x=>Math.sin(2*Math.PI*f*x)*Math.exp(-x*9)*(1-Math.exp(-x*110)),.035);}
}
for(const t of [3,7,12,16])add(t,.45,x=>(Math.sin(2*Math.PI*587.33*x)+.5*Math.sin(2*Math.PI*880*x))*Math.exp(-x*11)*(1-Math.exp(-x*100)),.07);
for(const t of [8.35,9.55,10.75])add(t,.07,x=>(Math.sin(2*Math.PI*1200*x)+.25*noise())*Math.exp(-x*100),.10);
let peak=0;
for(let i=0;i<count;i++){const t=i/rate; samples[i]*=Math.min(1,t/.15,(duration-t)/.8);peak=Math.max(peak,Math.abs(samples[i]));}
const gain=.48/peak, bytes=Buffer.alloc(44+count*4);
bytes.write('RIFF',0);bytes.writeUInt32LE(bytes.length-8,4);bytes.write('WAVEfmt ',8);bytes.writeUInt32LE(16,16);bytes.writeUInt16LE(1,20);bytes.writeUInt16LE(2,22);bytes.writeUInt32LE(rate,24);bytes.writeUInt32LE(rate*4,28);bytes.writeUInt16LE(4,32);bytes.writeUInt16LE(16,34);bytes.write('data',36);bytes.writeUInt32LE(count*4,40);
for(let i=0;i<count;i++){const s=Math.round(samples[i]*gain*32767);bytes.writeInt16LE(s,44+i*4);bytes.writeInt16LE(s,46+i*4);}
writeFileSync('assets/original-score.wav',bytes);
console.log('Original 20-second stereo score, 48 kHz, 96 BPM; peak -6.38 dBFS before mix.');
