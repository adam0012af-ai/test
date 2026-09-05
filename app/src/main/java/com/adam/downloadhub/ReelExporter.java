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
import android.net.Uri;
import android.os.Environment;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;

import com.yausername.ffmpeg.FFmpeg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReelExporter {
    public interface Callback {void onStage(String status);void onSuccess(File file);void onError(String message);}
    private ReelExporter(){}

    public static void export(Context context,CreatorProject project,Callback cb){
        try{
            if(project==null)throw new IllegalArgumentException("المشروع غير صالح");
            cb.onStage("تجهيز مشروع 9:16…");FFmpeg.getInstance().init(context.getApplicationContext());
            File work=new File(context.getCacheDir(),"creator_"+System.currentTimeMillis());if(!work.exists()&&!work.mkdirs())throw new IllegalStateException("تعذر تجهيز مساحة العمل");
            File bg=new File(work,"background.png"),overlay=new File(work,"overlay.png");renderBackground(project,bg);renderOverlay(context,project,overlay);
            File source=copyUri(context,project.sourceUri,work,"source"),audio=copyUri(context,project.audioUri,work,"audio");boolean video=source!=null&&isVideo(context,project.sourceUri),image=source!=null&&!video;
            File dir=getOutputDir(context);if(!dir.exists()&&!dir.mkdirs())throw new IllegalStateException("تعذر إنشاء مجلد Creator");String base=DownloadUtil.sanitizeFileName(project.name==null?"Reel":project.name);if(base.length()>54)base=base.substring(0,54);File out=new File(dir,base+"_"+System.currentTimeMillis()+".mp4");
            int quality=AppPrefs.exportQuality(context);cb.onStage("تصدير "+quality+"p…");int code=run(context,args(project,bg,overlay,source,audio,video,image,out,"libx264",quality));
            if(code!=0||!out.exists()||out.length()<1024){if(out.exists())out.delete();cb.onStage("إعادة التصدير بوضع توافق أعلى…");code=run(context,args(project,bg,overlay,source,audio,video,image,out,"mpeg4",quality));}
            if(code!=0||!out.exists()||out.length()<1024)throw new IllegalStateException("فشل محرك التصدير في إنشاء الفيديو");cb.onSuccess(out);
        }catch(Throwable e){String m=e.getMessage();cb.onError(m==null||m.trim().isEmpty()?"تعذر تصدير الفيديو":m.trim());}
    }

    public static File getOutputDir(Context c){File root=c.getExternalFilesDir(Environment.DIRECTORY_MOVIES);if(root==null)root=new File(c.getFilesDir(),"movies");return new File(root,"DownloadHub/Creator");}

    private static List<String> args(CreatorProject p,File bg,File overlay,File source,File audio,boolean video,boolean image,File out,String codec,int quality){
        int sec=Math.max(6,Math.min(60,p.durationSec)),w=quality==720?720:1080,h=quality==720?1280:1920;List<String>a=new ArrayList<>();a.add("-y");
        if(video){add(a,"-stream_loop","-1","-i",source.getAbsolutePath());}else if(image){add(a,"-loop","1","-i",source.getAbsolutePath());}else add(a,"-loop","1","-i",bg.getAbsolutePath());
        add(a,"-loop","1","-i",overlay.getAbsolutePath());if(audio!=null)add(a,"-stream_loop","-1","-i",audio.getAbsolutePath());
        add(a,"-filter_complex","[0:v]scale="+w+":"+h+":force_original_aspect_ratio=increase,crop="+w+":"+h+",setsar=1[base];[1:v]scale="+w+":"+h+"[ov];[base][ov]overlay=0:0:format=auto[v]","-map","[v]");
        if(audio!=null)add(a,"-map","2:a:0");else if(video)add(a,"-map","0:a?");add(a,"-t",String.valueOf(sec),"-r","30","-c:v",codec);
        if("libx264".equals(codec))add(a,"-preset","veryfast","-crf","22");else add(a,"-q:v","4");add(a,"-pix_fmt","yuv420p");if(audio!=null||video)add(a,"-c:a","aac","-b:a","160k");add(a,"-movflags","+faststart",out.getAbsolutePath());return a;
    }

    private static int run(Context c,List<String> args)throws Exception{String nativeDir=c.getApplicationInfo().nativeLibraryDir;File bin=new File(nativeDir,"libffmpeg.so");if(!bin.exists())throw new IllegalStateException("FFmpeg غير متاح على الجهاز");List<String>cmd=new ArrayList<>();cmd.add(bin.getAbsolutePath());cmd.addAll(args);ProcessBuilder pb=new ProcessBuilder(cmd).redirectErrorStream(true);File deps=new File(c.getNoBackupFilesDir(),"youtubedl-android/packages/ffmpeg/usr/lib");String old=System.getenv("LD_LIBRARY_PATH");pb.environment().put("LD_LIBRARY_PATH",nativeDir+":"+deps.getAbsolutePath()+(old==null?"":":"+old));Process p=pb.start();try(BufferedReader r=new BufferedReader(new InputStreamReader(p.getInputStream()))){while(r.readLine()!=null){}}return p.waitFor();}
    private static void add(List<String>a,String...v){for(String s:v)a.add(s);}

    private static File copyUri(Context c,String text,File dir,String prefix)throws Exception{if(text==null||text.trim().isEmpty())return null;Uri u=Uri.parse(text);ContentResolver cr=c.getContentResolver();File out=new File(dir,prefix+ext(cr.getType(u),prefix));try(InputStream in=cr.openInputStream(u);FileOutputStream fo=new FileOutputStream(out)){if(in==null)return null;byte[]buf=new byte[65536];int n;while((n=in.read(buf))>0)fo.write(buf,0,n);}return out;}
    private static boolean isVideo(Context c,String text){try{String t=c.getContentResolver().getType(Uri.parse(text));return t!=null&&t.toLowerCase(Locale.ROOT).startsWith("video/");}catch(Exception e){return false;}}
    private static String ext(String t,String p){if(t==null)return"audio".equals(p)?".m4a":".mp4";t=t.toLowerCase(Locale.ROOT);if(t.contains("png"))return".png";if(t.contains("jpeg")||t.contains("jpg"))return".jpg";if(t.contains("webp"))return".webp";if(t.contains("webm"))return".webm";if(t.contains("mpeg"))return t.startsWith("audio")?".mp3":".mp4";if(t.contains("ogg"))return".ogg";if(t.contains("wav"))return".wav";return t.startsWith("audio")?".m4a":".mp4";}

    private static void renderBackground(CreatorProject p,File out)throws Exception{Bitmap b=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setShader(new LinearGradient(0,0,1080,1920,p.startColor,p.endColor,Shader.TileMode.CLAMP));c.drawRect(0,0,1080,1920,paint);paint.setShader(null);paint.setColor(Color.argb(45,255,255,255));c.drawCircle(940,220,260,paint);c.drawCircle(120,1630,330,paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(Color.argb(40,255,255,255));c.drawRoundRect(new RectF(48,48,1032,1872),44,44,paint);try(FileOutputStream f=new FileOutputStream(out)){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();}
    private static void renderOverlay(Context context,CreatorProject p,File out)throws Exception{Bitmap b=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.TRANSPARENT);Paint panel=new Paint(Paint.ANTI_ALIAS_FLAG);panel.setColor(Color.argb(125,3,12,25));c.drawRoundRect(new RectF(70,520,1010,1420),42,42,panel);panel.setColor(Color.argb(235,25,216,255));c.drawRoundRect(new RectF(460,560,620,568),8,8,panel);block(c,p.hook,84,Color.WHITE,105,630,870,true);block(c,p.body,46,Color.rgb(225,236,249),120,890,840,false);block(c,p.cta,42,Color.rgb(118,232,255),135,1265,810,true);if(AppPrefs.creatorWatermark(context)){TextPaint t=new TextPaint(Paint.ANTI_ALIAS_FLAG);t.setColor(Color.argb(165,255,255,255));t.setTextSize(28);t.setTextAlign(Paint.Align.CENTER);c.drawText("Download Hub • AboAdam",540,1770,t);}try(FileOutputStream f=new FileOutputStream(out)){b.compress(Bitmap.CompressFormat.PNG,100,f);}b.recycle();}
    private static void block(Canvas c,String text,float size,int color,int left,int top,int width,boolean bold){if(text==null||text.trim().isEmpty())return;TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setFakeBoldText(bold);String x=text.trim();StaticLayout l=StaticLayout.Builder.obtain(x,0,x.length(),p,width).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.RTL).setLineSpacing(6,1.08f).setIncludePad(false).build();c.save();c.translate(left,top);l.draw(c);c.restore();}
}
