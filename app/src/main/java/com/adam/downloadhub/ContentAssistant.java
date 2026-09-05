package com.adam.downloadhub;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class ContentAssistant {
    public static final class PublishText {
        public final List<String> titles;
        public final String caption;
        public final String hashtags;
        PublishText(List<String> titles,String caption,String hashtags){
            this.titles=titles;this.caption=caption;this.hashtags=hashtags;
        }
    }

    private ContentAssistant(){}

    public static PublishText generate(CreatorProject p,String platform){
        String key=p==null?"custom":clean(p.categoryKey);
        String hook=p==null?"":oneLine(p.hook);
        String body=p==null?"":oneLine(p.body);
        String cta=p==null?"":oneLine(p.cta);
        if(hook.isEmpty())hook=defaultHook(key);

        List<String> titles=new ArrayList<>();
        if("islamic".equals(key)){
            titles.add(trimTitle(hook.replace("🤍","")));
            titles.add(trimTitle("تذكير هادئ لليوم • "+topic(hook)));
            titles.add(trimTitle(topic(hook)+" | رسالة قصيرة نافعة"));
        }else if(isCommercial(key)){
            titles.add(trimTitle(hook));
            titles.add(trimTitle(topic(hook)+" • التفاصيل في ثواني"));
            titles.add(trimTitle("شوف "+topic(hook)+" قبل ما تقرر"));
        }else{
            titles.add(trimTitle(hook));
            titles.add(trimTitle(topic(hook)+" في أقل من دقيقة"));
            titles.add(trimTitle("الفكرة ببساطة: "+topic(hook)));
        }

        String pl=clean(platform).toLowerCase(Locale.ROOT);
        String caption;
        if(pl.contains("youtube")){
            caption=join(body,cta)+"\n\n"+"مشاهدة سريعة من Download Hub Creator Studio.";
        }else if(pl.contains("instagram")||pl.contains("tiktok")||pl.contains("facebook")){
            caption=join(hook,body,cta);
        }else{
            caption=join(hook,body,cta);
        }
        return new PublishText(titles,caption,hashtags(p,key,platform));
    }

    private static String hashtags(CreatorProject p,String key,String platform){
        Set<String> tags=new LinkedHashSet<>();
        String existing=p==null?"":clean(p.hashtags);
        for(String x:existing.split("\\s+")) if(x.startsWith("#")&&x.length()>1)tags.add(sanitizeTag(x));
        add(tags,baseTags(key));
        String pl=clean(platform).toLowerCase(Locale.ROOT);
        if(pl.contains("youtube"))add(tags,"#Shorts #YouTubeShorts");
        else if(pl.contains("instagram"))add(tags,"#Reels #InstagramReels");
        else if(pl.contains("tiktok"))add(tags,"#TikTok #ريلز");
        else if(pl.contains("facebook"))add(tags,"#FacebookReels #Reels");
        else add(tags,"#Reels #Shorts");
        StringBuilder out=new StringBuilder();
        int count=0;
        for(String t:tags){
            if(t.isEmpty())continue;
            if(count++>=12)break;
            if(out.length()>0)out.append(' ');
            out.append(t);
        }
        return out.toString();
    }

    private static String baseTags(String key){
        switch(key){
            case "islamic": return "#اسلاميات #تذكير #ذكر_الله #ريلز_اسلامي #محتوى_نافع";
            case "podcast": return "#بودكاست #Podcast #مقتطفات #حوار";
            case "gaming": return "#Gaming #جيمينج #Highlights #Games";
            case "cars": return "#سيارات #Cars #عربيات #مراجعة_سيارات";
            case "food": return "#Food #مطاعم #اكل #وصفات";
            case "realestate": return "#عقارات #RealEstate #شقق #عقار";
            case "products": return "#منتجات #اعلان #Marketing #Offers";
            case "motivation": return "#تحفيز #Motivation #تطوير_الذات #عادات";
            case "travel": return "#Travel #سفر #رحلات #اماكن";
            case "fitness": return "#Fitness #رياضة #تمارين #Gym";
            case "fashion": return "#Fashion #Beauty #ستايل #موضة";
            case "education": return "#تعليم #Education #معلومة #شرح";
            case "news": return "#اخبار #News #ملخص #تحديث";
            case "business": return "#Business #تسويق #مشاريع #نصائح";
            default:return "#Reels #Shorts #Creator #DownloadHub";
        }
    }

    private static boolean isCommercial(String key){return "products".equals(key)||"food".equals(key)||"realestate".equals(key)||"business".equals(key)||"cars".equals(key);}
    private static String defaultHook(String key){return "islamic".equals(key)?"تذكير هادئ لليوم":"فكرة تستحق المشاهدة";}
    private static String topic(String s){
        String x=oneLine(s).replace("تذكير لطيف","").replace("لحظة هدوء للقلب","").replace("خذها معك اليوم","").replace("🤍","").trim();
        int n=x.indexOf('•');if(n>0)x=x.substring(0,n).trim();
        return x.isEmpty()?"فكرة اليوم":x;
    }
    private static String trimTitle(String s){String x=oneLine(s);return x.length()>72?x.substring(0,69).trim()+"…":x;}
    private static String oneLine(String s){return clean(s).replace('\n',' ').replaceAll("\\s+"," ").trim();}
    private static String join(String... values){StringBuilder b=new StringBuilder();for(String v:values){v=clean(v);if(v.isEmpty())continue;if(b.length()>0)b.append("\n\n");b.append(v);}return b.toString();}
    private static void add(Set<String> set,String text){for(String x:clean(text).split("\\s+"))if(x.startsWith("#"))set.add(sanitizeTag(x));}
    private static String sanitizeTag(String s){return clean(s).replaceAll("[^#\\p{L}\\p{N}_]","");}
    private static String clean(String s){return s==null?"":s.trim();}
}
