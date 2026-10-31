import {spawnSync} from 'node:child_process';
import {readFileSync,writeFileSync,renameSync} from 'node:fs';
import ffmpeg from 'ffmpeg-static';
import probePackage from 'ffprobe-static';
const ffprobe=probePackage.path;
function run(program,args){const r=spawnSync(program,args,{encoding:'utf8',maxBuffer:12*1024*1024,windowsHide:true});if(r.status!==0)throw Error((r.stderr||r.stdout||String(r.error)).slice(-6000));return r;}
function probe(file){return JSON.parse(run(ffprobe,['-v','error','-count_frames','-show_streams','-show_format','-of','json',file]).stdout);}
function audioHash(file){return run(ffmpeg,['-v','error','-i',file,'-map','0:a:0','-c:a','copy','-f','hash','-hash','sha256','-']).stdout.trim();}
const original='../brag-original.mp4', poster='../brag.jpg', temporary='../brag-poster-verified.mp4', final='../brag.mp4';
const before=probe(original);
const v=before.streams.find(s=>s.codec_type==='video');
if(v.width!==1920||v.height!==1080||Number(v.nb_read_frames)!==600||Math.abs(Number(before.format.duration)-20)>.04)throw Error('Original render does not match the 20-second / 600-frame brief.');
run(ffmpeg,['-y','-v','error','-ss','4.8','-i',original,'-frames:v','1','-q:v','2','-update','1',poster]);
run(ffmpeg,['-y','-v','error','-i',original,'-framerate','30','-loop','1','-i',poster,'-filter_complex',"[0:v][1:v]overlay=0:0:enable='lt(t,0.0333333333)':shortest=1[v]",'-map','[v]','-map','0:a:0','-c:v','libx264','-preset','medium','-crf','16','-pix_fmt','yuv420p','-c:a','copy','-frames:v','600','-fps_mode','cfr','-movflags','+faststart',temporary]);
const after=probe(temporary), av=after.streams.find(s=>s.codec_type==='video');
const ba=before.streams.find(s=>s.codec_type==='audio'), aa=after.streams.find(s=>s.codec_type==='audio');
if(av.width!==v.width||av.height!==v.height||av.nb_read_frames!==v.nb_read_frames||Math.abs(Number(after.format.duration)-Number(before.format.duration))>.001)throw Error('Poster replacement changed video timing or dimensions.');
if(!ba||!aa||aa.sample_rate!==ba.sample_rate||aa.channels!==ba.channels||Math.abs(Number(aa.start_time)-Number(ba.start_time))>.001||Math.abs(Number(aa.duration)-Number(ba.duration))>.001)throw Error('Poster replacement changed audio timing.');
const hashBefore=audioHash(original), hashAfter=audioHash(temporary);
if(hashBefore!==hashAfter)throw Error('Audio packets changed during poster replacement.');
renameSync(temporary,final);
run(ffmpeg,['-y','-v','error','-i',final,'-frames:v','1','-update','1','../review/frame-zero.png']);
const similarity=run(ffmpeg,['-i','../review/frame-zero.png','-i',poster,'-lavfi','ssim','-f','null','-']).stderr;
const match=similarity.match(/All:([0-9.]+)/);
if(!match||Number(match[1])<.98)throw Error('Frame zero did not match the selected poster.');
const volume=run(ffmpeg,['-i',final,'-vn','-af','volumedetect','-f','null','-']).stderr;
const report={width:av.width,height:av.height,fps:av.avg_frame_rate,frames:Number(av.nb_read_frames),duration:Number(after.format.duration),videoCodec:av.codec_name,audioCodec:aa.codec_name,sampleRate:Number(aa.sample_rate),channels:aa.channels,audioStart:aa.start_time,audioDuration:aa.duration,audioHash:hashAfter,audioPreserved:true,posterTimestamp:4.8,posterFrameSSIM:Number(match[1]),meanVolume:volume.match(/mean_volume: ([^\n]+)/)?.[1],maxVolume:volume.match(/max_volume: ([^\n]+)/)?.[1],originalPreserved:original};
writeFileSync('../validation.json',JSON.stringify(report,null,2));
writeFileSync('../review/probe-original.json',JSON.stringify(before,null,2));
writeFileSync('../review/probe-final.json',JSON.stringify(after,null,2));
console.log(JSON.stringify(report,null,2));
