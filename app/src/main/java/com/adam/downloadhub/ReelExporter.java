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
    public interface Callback {
        void onStage(String status);
        void onSuccess(File file);
        void onError(String message);
    }

    private ReelExporter(){}

    public static void export(Context context, CreatorProject project, Callback callback) {
        try {
            if(project==null)throw new IllegalArgumentException("المشروع غير صالح");
            callback.onStage("تجهيز مشروع 9:16…");
            FFmpeg.getInstance().init(context.getApplicationContext());

            File work=new File(context.getCacheDir(),"creator_"+System.currentTimeMillis());
            if(!work.exists()&&!work.mkdirs())throw new IllegalStateException("تعذر تجهيز مساحة العمل");
            File background=new File(work,"background.png");
            File overlay=new File(work,"overlay.png");
            renderBackground(project,background);
            renderOverlay(project,overlay);

            File source=copyUri(context,project.sourceUri,work,"source");
            File audio=copyUri(context,project.audioUri,work,"audio");
            boolean sourceVideo=source!=null&&isVideo(context,project.sourceUri);
            boolean sourceImage=source!=null&&!sourceVideo;

            File outDir=getOutputDir(context);
            if(!outDir.exists()&&!outDir.mkdirs())throw new IllegalStateException("تعذر إنشاء مجلد Creator");
            String base=DownloadUtil.sanitizeFileName(project.name==null?"Reel":project.name);
            if(base.length()>54)base=base.substring(0,54);
            File output=new File(outDir,base+"_"+System.currentTimeMillis()+".mp4");

            callback.onStage("تصدير الفيديو بجودة 1080×1920 محسّنة…");
            List<String> args=buildArgs(context,project,background,overlay,source,audio,sourceVideo,sourceImage,output,"libx264");
            int code=runFfmpeg(context,args);
            if(code!=0||!output.exists()||output.length()<1024){
                if(output.exists())output.delete();
                callback.onStage("إعادة التصدير بوضع توافق أعلى…");
                args=buildArgs(context,project,background,overlay,source,audio,sourceVideo,sourceImage,output,"mpeg4");
                code=runFfmpeg(context,args);
            }
            if(code!=0||!output.exists()||output.length()<1024)throw new IllegalStateException("فشل محرك التصدير في إنشاء الفيديو");
            callback.onSuccess(output);
        }catch(Throwable e){
            String m=e.getMessage();
            callback.onError(m==null||m.trim().isEmpty()?"تعذر تصدير الفيديو":m.trim());
        }
    }

    public static File getOutputDir(Context context){
        File root=context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if(root==null)root=new File(context.getFilesDir(),"movies");
        return new File(root,"DownloadHub/Creator");
    }

    private static List<String> buildArgs(Context context,CreatorProject p,File bg,File overlay,File source,File audio,boolean video,boolean image,File out,String codec){
        int seconds=Math.max(6,Math.min(60,p.durationSec));
        List<String> a=new ArrayList<>();
        a.add("-y");
        if(video){a.add("-stream_loop");a.add("-1");a.add("-i");a.add(source.getAbsolutePath());}
        else if(image){a.add("-loop");a.add("1");a.add("-i");a.add(source.getAbsolutePath());}
        else {a.add("-loop");a.add("1");a.add("-i");a.add(bg.getAbsolutePath());}
        a.add("-loop");a.add("1");a.add("-i");a.add(overlay.getAbsolutePath());
        if(audio!=null){a.add("-stream_loop");a.add("-1");a.add("-i");a.add(audio.getAbsolutePath());}

        String filter="[0:v]scale=1080:1920:force_original_aspect_ratio=increase,crop=1080:1920,setsar=1[base];[base][1:v]overlay=0:0:format=auto[v]";
        a.add("-filter_complex");a.add(filter);
        a.add("-map");a.add("[v]");
        if(audio!=null){a.add("-map");a.add("2:a:0");}
        else if(video){a.add("-map");a.add("0:a?");}
        a.add("-t");a.add(String.valueOf(seconds));
        a.add("-r");a.add("30");
        a.add("-c:v");a.add(codec);
        if("libx264".equals(codec)){a.add("-preset");a.add("veryfast");a.add("-crf");a.add("22");}
        else {a.add("-q:v");a.add("4");}
        a.add("-pix_fmt");a.add("yuv420p");
        if(audio!=null||video){a.add("-c:a");a.add("aac");a.add("-b:a");a.add("160k");}
        a.add("-movflags");a.add("+faststart");
        a.add(outputPath(out));
        return a;
    }

    private static String outputPath(File f){return f.getAbsolutePath();}

    private static int runFfmpeg(Context context,List<String> args)throws Exception{
        String nativeDir=context.getApplicationInfo().nativeLibraryDir;
        File binary=new File(nativeDir,"libffmpeg.so");
        if(!binary.exists())throw new IllegalStateException("FFmpeg غير متاح على الجهاز");
        List<String> command=new ArrayList<>();command.add(binary.getAbsolutePath());command.addAll(args);
        ProcessBuilder pb=new ProcessBuilder(command);pb.redirectErrorStream(true);
        File deps=new File(context.getNoBackupFilesDir(),"youtubedl-android/packages/ffmpeg/usr/lib");
        String old=System.getenv("LD_LIBRARY_PATH");
        pb.environment().put("LD_LIBRARY_PATH",nativeDir+":"+deps.getAbsolutePath()+(old==null?"":":"+old));
        Process process=pb.start();
        try(BufferedReader r=new BufferedReader(new InputStreamReader(process.getInputStream()))){while(r.readLine()!=null){}}
        return process.waitFor();
    }

    private static File copyUri(Context c,String uriText,File dir,String prefix)throws Exception{
        if(uriText==null||uriText.trim().isEmpty())return null;
        Uri uri=Uri.parse(uriText);ContentResolver cr=c.getContentResolver();String type=cr.getType(uri);String ext=extension(type,prefix);
        File out=new File(dir,prefix+ext);
        try(InputStream in=cr.openInputStream(uri);FileOutputStream fo=new FileOutputStream(out)){
            if(in==null)return null;byte[] buf=new byte[65536];int n;while((n=in.read(buf))>0)fo.write(buf,0,n);
        }
        return out;
    }

    private static boolean isVideo(Context c,String uriText){
        try{String t=c.getContentResolver().getType(Uri.parse(uriText));return t!=null&&t.toLowerCase(Locale.ROOT).startsWith("video/");}catch(Exception e){return false;}
    }

    private static String extension(String type,String prefix){
        if(type==null)return "audio".equals(prefix)?".m4a":".mp4";
        type=type.toLowerCase(Locale.ROOT);
        if(type.contains("png"))return ".png";if(type.contains("jpeg")||type.contains("jpg"))return ".jpg";if(type.contains("webp"))return ".webp";
        if(type.contains("webm"))return ".webm";if(type.contains("mpeg"))return type.startsWith("audio")?".mp3":".mp4";if(type.contains("ogg"))return ".ogg";if(type.contains("wav"))return ".wav";
        return type.startsWith("audio")?".m4a":".mp4";
    }

    private static void renderBackground(CreatorProject p,File output)throws Exception{
        Bitmap b=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new LinearGradient(0,0,1080,1920,p.startColor,p.endColor,Shader.TileMode.CLAMP));c.drawRect(0,0,1080,1920,paint);paint.setShader(null);
        paint.setColor(Color.argb(45,255,255,255));c.drawCircle(940,220,260,paint);c.drawCircle(120,1630,330,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(Color.argb(40,255,255,255));c.drawRoundRect(new RectF(48,48,1032,1872),44,44,paint);paint.setStyle(Paint.Style.FILL);
        try(FileOutputStream out=new FileOutputStream(output)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }

    private static void renderOverlay(CreatorProject p,File output)throws Exception{
        Bitmap b=Bitmap.createBitmap(1080,1920,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);c.drawColor(Color.TRANSPARENT);
        Paint panel=new Paint(Paint.ANTI_ALIAS_FLAG);panel.setColor(Color.argb(125,3,12,25));c.drawRoundRect(new RectF(70,520,1010,1420),42,42,panel);
        Paint accent=new Paint(Paint.ANTI_ALIAS_FLAG);accent.setColor(Color.argb(235,25,216,255));c.drawRoundRect(new RectF(460,560,620,568),8,8,accent);
        drawBlock(c,p.hook,84,Color.WHITE,105,630,870,true);
        drawBlock(c,p.body,46,Color.rgb(225,236,249),120,890,840,false);
        drawBlock(c,p.cta,42,Color.rgb(118,232,255),135,1265,810,true);
        TextPaint brand=new TextPaint(Paint.ANTI_ALIAS_FLAG);brand.setColor(Color.argb(155,255,255,255));brand.setTextSize(28);brand.setTextAlign(Paint.Align.CENTER);c.drawText("Download Hub • Creator Studio",540,1770,brand);
        try(FileOutputStream out=new FileOutputStream(output)){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();
    }

    private static void drawBlock(Canvas canvas,String text,float size,int color,int left,int top,int width,boolean bold){
        if(text==null||text.trim().isEmpty())return;
        TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.LEFT);p.setFakeBoldText(bold);
        StaticLayout l=StaticLayout.Builder.obtain(text.trim(),0,text.trim().length(),p,width).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.RTL).setLineSpacing(6,1.08f).setIncludePad(false).build();
        canvas.save();canvas.translate(left,top);l.draw(canvas);canvas.restore();
    }
}
