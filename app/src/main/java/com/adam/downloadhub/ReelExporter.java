package com.adam.downloadhub;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import com.yausername.ffmpeg.FFmpeg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReelExporter {
    public interface Callback { void onStage(String status); void onSuccess(File file); void onError(String message); }
    private ReelExporter() {}

    private static final class PreparedClip {
        final File file;
        final double durationSec;
        PreparedClip(File f,double d){file=f;durationSec=d;}
    }

    public static void export(Context context, CreatorProject project, Callback cb) {
        try {
            if (project == null) throw new IllegalArgumentException("المشروع غير صالح");
            project.ensureClips();
            if (project.clips.isEmpty()) throw new IllegalArgumentException("أضف فيديو أو صورة قبل التصدير");
            FFmpeg.getInstance().init(context.getApplicationContext());
            File work = new File(context.getCacheDir(), "studio_" + System.currentTimeMillis());
            if (!work.exists() && !work.mkdirs()) throw new IllegalStateException("تعذر تجهيز مساحة العمل");

            int quality = AppPrefs.exportQuality(context);
            int[] size = outputSize(project.aspectRatio, quality);
            List<PreparedClip> prepared = new ArrayList<>();
            int total = project.clips.size();
            for (int i=0;i<total;i++) {
                cb.onStage("تجهيز المقطع " + (i+1) + " من " + total + "…");
                prepared.add(prepareClip(context, project, project.clips.get(i), work, i, size[0], size[1]));
            }

            cb.onStage("دمج الـ Timeline…");
            File base = concatClips(context, prepared, work);
            double totalSec = 0;
            for (PreparedClip p : prepared) totalSec += p.durationSec;
            if (totalSec < .5) throw new IllegalStateException("مدة المشروع غير صالحة");

            File overlay = new File(work,"overlay.png");
            renderOverlay(context,project,overlay,size[0],size[1]);
            File extraAudio = copyUri(context, project.audioUri, work, "music");

            File dir = getOutputDir(context);
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("تعذر إنشاء مجلد Creator");
            String baseName = DownloadUtil.sanitizeFileName(project.name == null ? "Video_Project" : project.name);
            if (baseName.length() > 54) baseName = baseName.substring(0,54);
            File out = new File(dir, baseName + "_" + System.currentTimeMillis() + ".mp4");

            cb.onStage("تطبيق النص والصوت والمؤثرات…");
            int code = run(context, finalArgs(project,base,overlay,extraAudio,out,"libx264",totalSec));
            if (code != 0 || !out.exists() || out.length() < 1024) {
                if (out.exists()) out.delete();
                cb.onStage("إعادة التصدير بوضع التوافق…");
                code = run(context, finalArgs(project,base,overlay,extraAudio,out,"mpeg4",totalSec));
            }
            if (code != 0 || !out.exists() || out.length() < 1024) throw new IllegalStateException("فشل محرك التصدير في إنشاء الفيديو");
            cb.onSuccess(out);
        } catch (Throwable e) {
            String m=e.getMessage();
            cb.onError(m==null||m.trim().isEmpty()?"تعذر تصدير الفيديو":m.trim());
        }
    }

    private static PreparedClip prepareClip(Context c,CreatorProject p,EditorClip clip,File work,int index,int w,int h)throws Exception {
        if(clip==null||clip.uri==null||clip.uri.trim().isEmpty())throw new IllegalArgumentException("مقطع فارغ في الـ Timeline");
        File src=copyUri(c,clip.uri,work,"clip_src_"+index);
        if(src==null||!src.exists()||src.length()<64)throw new IllegalStateException("تعذر قراءة المقطع "+(index+1));
        boolean video=isVideo(c,clip.uri,src);
        long rawDuration=video?mediaDuration(src):Math.max(500,clip.stillDurationMs);
        if(rawDuration<=0)rawDuration=Math.max(1000,p.durationSec*1000L);
        long start=video?Math.max(0,Math.min(rawDuration-100,clip.trimStartMs)):0;
        long end=video&&clip.trimEndMs>start?Math.min(rawDuration,clip.trimEndMs):rawDuration;
        if(end<=start+100)end=Math.min(rawDuration,start+1000);
        double inputSec=Math.max(.25,(end-start)/1000.0);
        double speed=Math.max(.5,Math.min(2.0,clip.speed));
        double outputSec=Math.max(.25,inputSec/speed);
        File out=new File(work,String.format(Locale.US,"timeline_%03d.mp4",index));
        List<String>a=new ArrayList<>();add(a,"-y");
        String transitionFilter=transitionFilter(p,outputSec);
        String scale="scale="+w+":"+h+":force_original_aspect_ratio=increase,crop="+w+":"+h+",setsar=1";
        if(video){
            add(a,"-ss",sec(start),"-i",src.getAbsolutePath(),"-t",String.format(Locale.US,"%.3f",inputSec));
            boolean audio=hasAudio(src);
            if(audio){
                String vf="[0:v]"+scale+",setpts=(PTS-STARTPTS)/"+String.format(Locale.US,"%.3f",speed)+transitionFilter+"[v]";
                String af="[0:a]volume="+String.format(Locale.US,"%.3f",EditorClip.clamp(clip.volume)/100.0)+",atempo="+String.format(Locale.US,"%.3f",speed)+",aresample=44100,asetpts=PTS-STARTPTS"+audioTransitionFilter(p,outputSec)+"[a]";
                add(a,"-filter_complex",vf+";"+af,"-map","[v]","-map","[a]");
            }else{
                add(a,"-f","lavfi","-t",String.format(Locale.US,"%.3f",outputSec),"-i","anullsrc=channel_layout=stereo:sample_rate=44100");
                String vf="[0:v]"+scale+",setpts=(PTS-STARTPTS)/"+String.format(Locale.US,"%.3f",speed)+transitionFilter+"[v]";
                add(a,"-filter_complex",vf,"-map","[v]","-map","1:a:0");
            }
        }else{
            add(a,"-loop","1","-i",src.getAbsolutePath(),"-f","lavfi","-t",String.format(Locale.US,"%.3f",outputSec),"-i","anullsrc=channel_layout=stereo:sample_rate=44100");
            String vf="[0:v]"+scale+",setsar=1"+transitionFilter+"[v]";
            add(a,"-filter_complex",vf,"-map","[v]","-map","1:a:0");
        }
        add(a,"-t",String.format(Locale.US,"%.3f",outputSec),"-r","30","-c:v","mpeg4","-q:v","3","-pix_fmt","yuv420p","-c:a","aac","-b:a","160k","-ar","44100","-ac","2","-movflags","+faststart",out.getAbsolutePath());
        int code=run(c,a);
        if(code!=0||!out.exists()||out.length()<1024)throw new IllegalStateException("فشل تجهيز المقطع "+(index+1));
        return new PreparedClip(out,outputSec);
    }

    private static String transitionFilter(CreatorProject p,double d){
        if(p==null||"None".equalsIgnoreCase(p.transitionStyle)||d<.7)return "";
        double x="Soft".equalsIgnoreCase(p.transitionStyle)?.35:.20;
        double out=Math.max(.05,d-x);
        return ",fade=t=in:st=0:d="+String.format(Locale.US,"%.2f",x)+",fade=t=out:st="+String.format(Locale.US,"%.2f",out)+":d="+String.format(Locale.US,"%.2f",x);
    }

    private static String audioTransitionFilter(CreatorProject p,double d){
        if(p==null||"None".equalsIgnoreCase(p.transitionStyle)||d<.7)return "";
        double x="Soft".equalsIgnoreCase(p.transitionStyle)?.35:.20;
        double out=Math.max(.05,d-x);
        return ",afade=t=in:st=0:d="+String.format(Locale.US,"%.2f",x)+",afade=t=out:st="+String.format(Locale.US,"%.2f",out)+":d="+String.format(Locale.US,"%.2f",x);
    }

    private static File concatClips(Context c,List<PreparedClip> items,File work)throws Exception {
        if(items.size()==1)return items.get(0).file;
        File list=new File(work,"timeline.txt");
        try(BufferedWriter w=new BufferedWriter(new FileWriter(list))){for(PreparedClip p:items){String path=p.file.getAbsolutePath().replace("'","'\\''");w.write("file '");w.write(path);w.write("'\n");}}
        File out=new File(work,"timeline_base.mp4");List<String>a=new ArrayList<>();add(a,"-y","-f","concat","-safe","0","-i",list.getAbsolutePath(),"-c","copy","-movflags","+faststart",out.getAbsolutePath());int code=run(c,a);
        if(code!=0||!out.exists()||out.length()<1024){if(out.exists())out.delete();a.clear();add(a,"-y","-f","concat","-safe","0","-i",list.getAbsolutePath(),"-c:v","mpeg4","-q:v","3","-c:a","aac","-b:a","160k","-ar","44100","-ac","2","-movflags","+faststart",out.getAbsolutePath());code=run(c,a);}
        if(code!=0||!out.exists()||out.length()<1024)throw new IllegalStateException("فشل دمج مقاطع الـ Timeline");return out;
    }

    private static List<String> finalArgs(CreatorProject p,File base,File overlay,File audio,File out,String codec,double totalSec){
        List<String>a=new ArrayList<>();add(a,"-y","-i",base.getAbsolutePath(),"-loop","1","-i",overlay.getAbsolutePath());if(audio!=null)add(a,"-stream_loop","-1","-i",audio.getAbsolutePath());
        String vf="[0:v][1:v]overlay=0:0:format=auto";
        double fadeIn=Math.min(totalSec/3.0,Math.max(0,p.fadeInMs)/1000.0),fadeOut=Math.min(totalSec/3.0,Math.max(0,p.fadeOutMs)/1000.0);
        if(fadeIn>.01)vf+=",fade=t=in:st=0:d="+String.format(Locale.US,"%.3f",fadeIn);
        if(fadeOut>.01)vf+=",fade=t=out:st="+String.format(Locale.US,"%.3f",Math.max(0,totalSec-fadeOut))+":d="+String.format(Locale.US,"%.3f",fadeOut);
        vf+="[v]";
        if(audio!=null){
            String music;
            double vol=Math.max(0,Math.min(100,p.audioVolume))/100.0;
            if(p.audioOffsetMs>=0)music="[2:a]volume="+String.format(Locale.US,"%.3f",vol)+",adelay="+p.audioOffsetMs+"|"+p.audioOffsetMs+",aresample=44100[a1]";
            else music="[2:a]atrim=start="+String.format(Locale.US,"%.3f",Math.abs(p.audioOffsetMs)/1000.0)+",asetpts=PTS-STARTPTS,volume="+String.format(Locale.US,"%.3f",vol)+",aresample=44100[a1]";
            String fc=vf+";[0:a]aresample=44100[a0];"+music+";[a0][a1]amix=inputs=2:duration=first:dropout_transition=2[a]";
            add(a,"-filter_complex",fc,"-map","[v]","-map","[a]");
        }else add(a,"-filter_complex",vf,"-map","[v]","-map","0:a:0");
        add(a,"-t",String.format(Locale.US,"%.3f",totalSec),"-r","30","-c:v",codec);
        if("libx264".equals(codec))add(a,"-preset","veryfast","-crf","21");else add(a,"-q:v","3");
        add(a,"-pix_fmt","yuv420p","-c:a","aac","-b:a","160k","-ar","44100","-ac","2","-movflags","+faststart",out.getAbsolutePath());return a;
    }

    public static void concatAudio(Context context,List<File> files,File out)throws Exception {
        if(files==null||files.isEmpty())throw new IllegalArgumentException("لا توجد ملفات تلاوة للدمج");FFmpeg.getInstance().init(context.getApplicationContext());File parent=out.getParentFile();if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IllegalStateException("تعذر تجهيز مجلد التلاوة");File list=new File(parent==null?context.getCacheDir():parent,"concat_"+System.currentTimeMillis()+".txt");try(BufferedWriter w=new BufferedWriter(new FileWriter(list))){for(File f:files){if(f==null||!f.exists()||f.length()<128)continue;String path=f.getAbsolutePath().replace("'","'\\''");w.write("file '");w.write(path);w.write("'\n");}}
        List<String>a=new ArrayList<>();add(a,"-y","-f","concat","-safe","0","-i",list.getAbsolutePath(),"-vn","-c:a","aac","-b:a","160k",out.getAbsolutePath());int code=run(context,a);try{list.delete();}catch(Exception ignored){}if(code!=0||!out.exists()||out.length()<1024)throw new IllegalStateException("فشل دمج التلاوة");
    }

    public static File getOutputDir(Context c){File root=c.getExternalFilesDir(Environment.DIRECTORY_MOVIES);if(root==null)root=new File(c.getFilesDir(),"movies");return new File(root,"DownloadHub/Creator");}

    private static int[] outputSize(String ratio,int quality){boolean q720=quality==720;if("1:1".equals(ratio))return q720?new int[]{720,720}:new int[]{1080,1080};if("16:9".equals(ratio))return q720?new int[]{1280,720}:new int[]{1920,1080};return q720?new int[]{720,1280}:new int[]{1080,1920};}

    private static void renderOverlay(Context context,CreatorProject p,File out,int w,int h)throws Exception {
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.TRANSPARENT);
        if(p.showText){float scale=Math.max(.6f,Math.min(1.8f,p.textScale/100f));float center=h*Math.max(.10f,Math.min(.90f,p.textYPercent/100f));float panelH=Math.min(h*.52f,680*scale);float top=Math.max(20,center-panelH/2);float bottom=Math.min(h-20,center+panelH/2);Paint panel=new Paint(Paint.ANTI_ALIAS_FLAG);int alpha="Subtitle".equals(p.captionStyle)?80:130;panel.setColor(Color.argb(alpha,3,12,25));c.drawRoundRect(new RectF(w*.065f,top,w*.935f,bottom),Math.max(18,w*.035f),Math.max(18,w*.035f),panel);int color=p.textColor;block(c,p.hook,Math.max(28,w*.075f*scale),color,(int)(w*.09f),(int)(top+panelH*.12f),(int)(w*.82f),true);block(c,p.body,Math.max(20,w*.042f*scale),color,(int)(w*.11f),(int)(top+panelH*.38f),(int)(w*.78f),false);block(c,p.cta,Math.max(18,w*.038f*scale),color,(int)(w*.13f),(int)(top+panelH*.78f),(int)(w*.74f),true);}
        if(AppPrefs.creatorWatermark(context)){TextPaint t=new TextPaint(Paint.ANTI_ALIAS_FLAG);t.setColor(Color.argb(155,255,255,255));t.setTextSize(Math.max(18,w*.026f));t.setTextAlign(Paint.Align.CENTER);c.drawText("Download Hub Studio • AboAdam",w/2f,h*.94f,t);}try(FileOutputStream f=new FileOutputStream(out)){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();
    }

    private static void block(Canvas c,String text,float size,int color,int left,int top,int width,boolean bold){if(text==null||text.trim().isEmpty())return;TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);String x=text.trim();StaticLayout l=StaticLayout.Builder.obtain(x,0,x.length(),p,width).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL).setLineSpacing(5,1.05f).setIncludePad(false).build();c.save();c.translate(left,top);l.draw(c);c.restore();}

    private static File copyUri(Context c,String text,File dir,String prefix)throws Exception {if(text==null||text.trim().isEmpty())return null;Uri u=Uri.parse(text);if("file".equalsIgnoreCase(u.getScheme())){File direct=new File(u.getPath());if(direct.exists()&&direct.length()>0)return direct;}if(u.getScheme()==null){File direct=new File(text);if(direct.exists()&&direct.length()>0)return direct;}ContentResolver cr=c.getContentResolver();String mime=null;try{mime=cr.getType(u);}catch(Exception ignored){}File out=new File(dir,prefix+ext(mime,prefix));try(InputStream in=cr.openInputStream(u);FileOutputStream fo=new FileOutputStream(out)){if(in==null)return null;byte[]buf=new byte[65536];int n;while((n=in.read(buf))>0)fo.write(buf,0,n);}return out;}

    private static boolean isVideo(Context c,String text,File f){try{Uri u=Uri.parse(text);String t=c.getContentResolver().getType(u);if(t!=null&&t.toLowerCase(Locale.ROOT).startsWith("video/"))return true;}catch(Exception ignored){}String n=f.getName().toLowerCase(Locale.ROOT);return n.endsWith(".mp4")||n.endsWith(".webm")||n.endsWith(".mkv")||n.endsWith(".mov")||n.endsWith(".m4v");}
    private static long mediaDuration(File f){MediaMetadataRetriever r=new MediaMetadataRetriever();try{r.setDataSource(f.getAbsolutePath());String s=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);return s==null?0:Long.parseLong(s);}catch(Exception e){return 0;}finally{try{r.release();}catch(Exception ignored){}}}
    private static boolean hasAudio(File f){MediaMetadataRetriever r=new MediaMetadataRetriever();try{r.setDataSource(f.getAbsolutePath());String s=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);return s!=null&&("yes".equalsIgnoreCase(s)||"true".equalsIgnoreCase(s));}catch(Exception e){return false;}finally{try{r.release();}catch(Exception ignored){}}}

    private static String ext(String t,String p){if(t==null)return "music".equals(p)?".m4a":".mp4";t=t.toLowerCase(Locale.ROOT);if(t.contains("png"))return ".png";if(t.contains("jpeg")||t.contains("jpg"))return ".jpg";if(t.contains("webp"))return ".webp";if(t.contains("webm"))return ".webm";if(t.contains("quicktime"))return ".mov";if(t.contains("mpeg"))return t.startsWith("audio")?".mp3":".mp4";if(t.contains("ogg"))return ".ogg";if(t.contains("wav"))return ".wav";return t.startsWith("audio")?".m4a":".mp4";}
    private static String sec(long ms){return String.format(Locale.US,"%.3f",ms/1000.0);}

    private static int run(Context c,List<String> args)throws Exception {String nativeDir=c.getApplicationInfo().nativeLibraryDir;File bin=new File(nativeDir,"libffmpeg.so");if(!bin.exists())throw new IllegalStateException("FFmpeg غير متاح على الجهاز");List<String>cmd=new ArrayList<>();cmd.add(bin.getAbsolutePath());cmd.addAll(args);ProcessBuilder pb=new ProcessBuilder(cmd).redirectErrorStream(true);File deps=new File(c.getNoBackupFilesDir(),"youtubedl-android/packages/ffmpeg/usr/lib");String old=System.getenv("LD_LIBRARY_PATH");pb.environment().put("LD_LIBRARY_PATH",nativeDir+":"+deps.getAbsolutePath()+(old==null?"":":"+old));Process p=pb.start();try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()))){while(r.readLine()!=null){}}return p.waitFor();}
    private static void add(List<String>a,String...v){for(String s:v)a.add(s);}
}
