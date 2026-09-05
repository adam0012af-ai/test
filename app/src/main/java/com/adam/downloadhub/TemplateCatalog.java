package com.adam.downloadhub;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TemplateCatalog {
    public static final int TEMPLATES_PER_CATEGORY = 300;

    public static final class Category {
        public final String key, name, icon, description;
        public final int startColor, endColor;
        public final boolean featured;
        Category(String key,String name,String icon,String description,int startColor,int endColor,boolean featured){
            this.key=key;this.name=name;this.icon=icon;this.description=description;
            this.startColor=startColor;this.endColor=endColor;this.featured=featured;
        }
    }

    private static final List<Category> CATEGORIES = Collections.unmodifiableList(Arrays.asList(
            c("islamic","ريلز إسلامي","☾","أذكار • تذكير • أدعية • الجمعة • رمضان",0xFF0D6B63,0xFF0A3245,true),
            c("podcast","Podcast","🎙","مقاطع كلام • اقتباسات • حوار • Highlights",0xFF6147FF,0xFF171B45,true),
            c("gaming","Gaming","🎮","Highlights • Wins • Reactions • Tips",0xFF235BFF,0xFF081A42,true),
            c("cars","سيارات","◆","مراجعات • عروض • مواصفات • قبل/بعد",0xFF167CFF,0xFF10243C,true),
            c("food","مطاعم وFood","◉","أطباق • عروض • Menu • وصفات",0xFFFF7A38,0xFF442112,true),
            c("realestate","عقارات","⌂","شقق • فلل • جولات • عروض",0xFF00A8A8,0xFF102E3A,true),
            c("products","Products & Ads","▣","إعلانات • عروض • إطلاق منتج • UGC",0xFFFF3F83,0xFF3A1630,true),
            c("motivation","Motivation","⚡","تحفيز • نجاح • عادات • تطوير ذات",0xFFFFB12C,0xFF412A08,false),
            c("quotes","Quotes","❝","اقتباسات • أفكار • رسائل قصيرة",0xFF8E69FF,0xFF251844,false),
            c("travel","Travel","✈","رحلات • أماكن • Tips • Moments",0xFF00A9E8,0xFF0B2944,false),
            c("fitness","Fitness","◇","تمارين • نتائج • نصائح • تحديات",0xFF27C875,0xFF0D3325,false),
            c("fashion","Fashion & Beauty","✦","Looks • Beauty • Style • Showcase",0xFFE965C3,0xFF38162F,false),
            c("education","Education","✎","شرح سريع • معلومة • خطوات • Tips",0xFF22A7FF,0xFF112B46,false),
            c("news","News","▤","خبر سريع • ملخص • تحديث • عاجل",0xFFED4B5B,0xFF3C1319,false),
            c("business","Business","▥","مشاريع • تسويق • نصائح • نتائج",0xFF22C1A2,0xFF10332F,false),
            c("beforeafter","Before / After","↔","تحول • مقارنة • نتيجة • تجديد",0xFFFF8D42,0xFF3B2314,false),
            c("slideshow","Photo Slideshow","▧","صور • ذكريات • مناسبات • Showcase",0xFF8C6CFF,0xFF231A43,false),
            c("cinematic","Cinematic","◈","سينمائي • Mood • Slow • Story",0xFF466DFF,0xFF11182E,false),
            c("music","Music & Lyrics","♫","موسيقى • كلمات • Beat • Visualizer",0xFF12B9FF,0xFF1F1546,false),
            c("seasonal","مناسبات وSeasonal","✺","رمضان • عيد • عروض • مناسبات",0xFFDA8B2F,0xFF3D2510,false)
    ));

    private TemplateCatalog(){}

    private static Category c(String key,String name,String icon,String description,int a,int b,boolean featured){
        return new Category(key,name,icon,description,a,b,featured);
    }

    public static List<Category> categories(){ return CATEGORIES; }

    public static Category category(String key){
        for(Category c:CATEGORIES) if(c.key.equals(key)) return c;
        return CATEGORIES.get(0);
    }

    public static int totalTemplates(){ return CATEGORIES.size()*TEMPLATES_PER_CATEGORY; }

    public static List<ReelTemplate> templates(String categoryKey,String query,int offset,int limit){
        Category cat=category(categoryKey);
        String q=query==null?"":query.trim().toLowerCase(Locale.ROOT);
        List<ReelTemplate> out=new ArrayList<>();
        int skipped=0;
        for(int i=0;i<TEMPLATES_PER_CATEGORY;i++){
            ReelTemplate t=make(cat,i);
            if(!q.isEmpty()){
                String hay=(t.name+" "+t.hook+" "+t.body+" "+t.hashtags).toLowerCase(Locale.ROOT);
                if(!hay.contains(q)) continue;
            }
            if(skipped<offset){skipped++;continue;}
            out.add(t);
            if(limit>0&&out.size()>=limit)break;
        }
        return out;
    }

    public static ReelTemplate byId(String id){
        if(id==null)return make(CATEGORIES.get(0),0);
        for(Category c:CATEGORIES){
            String prefix=c.key+"-";
            if(id.startsWith(prefix)){
                try{
                    int index=Integer.parseInt(id.substring(prefix.length()));
                    if(index>=0&&index<TEMPLATES_PER_CATEGORY)return make(c,index);
                }catch(Exception ignored){}
            }
        }
        return make(CATEGORIES.get(0),0);
    }

    private static ReelTemplate make(Category cat,int index){
        String[] topics=topics(cat.key);
        String[] bodies=bodies(cat.key);
        String[] calls=ctas(cat.key);
        int topicIndex=index%10;
        int bodyIndex=(index/10)%10;
        int styleIndex=(index/100)%3;
        String topic=topics[topicIndex];
        String body=bodies[bodyIndex];
        String[] layouts={"Center Focus","Headline + Story","Minimal Cards"};
        String[] motions={"Smooth Zoom","Beat Cuts","Soft Float"};
        String[] captions={"Bold Highlight","Clean Subtitle","Word Focus"};
        int shift=(bodyIndex*7+topicIndex*11+styleIndex*19)%34;
        int start=shift(cat.startColor,shift);
        int end=shift(cat.endColor,-shift/2);
        int duration=12+((topicIndex+bodyIndex+styleIndex)%4)*3;
        String name=cat.name+" • "+topic+" #"+(index+1);
        String hook=hook(cat.key,topic,styleIndex);
        String cta=calls[(topicIndex+styleIndex)%calls.length];
        String hashtags=hashtags(cat.key,topic,index);
        return new ReelTemplate(cat.key+"-"+index,cat.key,cat.name,name,hook,body,cta,hashtags,
                layouts[styleIndex],motions[(styleIndex+bodyIndex)%3],captions[(styleIndex+topicIndex)%3],
                start,end,duration,"islamic".equals(cat.key));
    }

    private static String hook(String key,String topic,int style){
        if("islamic".equals(key)){
            String[] p={"تذكير لطيف 🤍","لحظة هدوء للقلب","خذها معك اليوم"};
            return p[style]+"\n"+topic;
        }
        String[] p={"شوف الفكرة دي","في ثواني هتعرف","محتوى يستاهل الوقفة"};
        return p[style]+"\n"+topic;
    }

    private static String[] topics(String key){
        switch(key){
            case "islamic": return a("ابدأ يومك بذكر الله","لا تنس أذكار الصباح","أكثر من الاستغفار","حافظ على الصلاة في وقتها","اجعل لك وردًا من القرآن","صلِّ على النبي ﷺ","الدعاء باب أمل","أحسن إلى والديك","يوم الجمعة فرصة للخير","اختم يومك بذكر الله");
            case "podcast": return a("فكرة غيرت نظرتي","أهم درس من التجربة","سؤال لازم تسأله لنفسك","الحقيقة التي لا يقولها أحد","دقيقة من الحوار","نصيحة للمبتدئين","القرار الأصعب","موقف علّمني كثيرًا","فكرة تستحق النقاش","الخلاصة في 20 ثانية");
            case "gaming": return a("أفضل لقطة اليوم","لحظة الفوز","رد فعل مستحيل","الحركة التي قلبت المباراة","نصيحة ترفع مستواك","أقوى Combo","قبل النهاية بثانية","تحدي جديد","Top Play","الخطأ الذي يجب تتجنبه");
            case "cars": return a("تفصيلة هتعجبك في العربية","صوت المحرك","مراجعة سريعة","3 مميزات مهمة","هل تستحق السعر؟","قبل وبعد التعديل","جولة داخلية","تجربة الطريق","معلومة قبل الشراء","عرض السيارة اليوم");
            case "food": return a("الطبق الأكثر طلبًا","شوف القرمشة","من المطبخ للطاولة","عرض اليوم","وصفة في ثواني","سر الطعم","اختيار الشيف","وجبة تستحق التجربة","قبل وبعد التقديم","اطلبها كده");
            case "realestate": return a("جولة داخل الشقة","التفصيلة التي تفرق","فرصة عقارية","المساحة في 20 ثانية","قبل ما تشتري","إطلالة الوحدة","تقسيمة عملية","موقع مميز","سعر مقابل قيمة","معاينة سريعة");
            case "products": return a("المنتج في 15 ثانية","ليه الناس بتحبه؟","عرض لفترة محدودة","قبل وبعد الاستخدام","3 أسباب للشراء","فتح وتجربة","مشكلة وحل","تفصيلة صغيرة فرقها كبير","استخدام سريع","الأكثر طلبًا");
            case "motivation": return a("ابدأ حتى لو بخطوة","الاستمرار أقوى من الحماس","ركز على تقدمك","يوم صعب لا يعني نهاية","ابنِ عادة صغيرة","خليك ثابت","ابدأ من مكانك","ركز على ما تستطيع","كل يوم فرصة","لا تنتظر الوقت المثالي");
            case "quotes": return a("جملة تستحق الحفظ","رسالة لليوم","فكرة قصيرة","اقرأها مرتين","كلام في وقته","رسالة هادئة","فكرة من سطر واحد","وقفة مع النفس","كلمة بسيطة","احتفظ بهذه الفكرة");
            case "travel": return a("مكان لازم تشوفه","يوم كامل في دقائق","3 نصائح للرحلة","المنظر يستاهل","قبل ما تسافر","أفضل وقت للزيارة","تجربة محلية","لقطة من الطريق","مكان هادي","خطة سريعة لليوم");
            case "fitness": return a("تمرين اليوم","صحح الحركة دي","3 أخطاء شائعة","تحدي 30 ثانية","قبل وبعد التمرين","روتين سريع","حركة للمبتدئين","ركز على الفورم","خطوة نحو هدفك","نصيحة تدريب");
            case "fashion": return a("لوك اليوم","تفصيلة تغير اللوك","قبل وبعد","3 تنسيقات","اختيار اليوم","ستايل بسيط","تجربة سريعة","ألوان ماشية مع بعض","خطوة Beauty","النتيجة النهائية");
            case "education": return a("معلومة في 20 ثانية","افهمها ببساطة","3 خطوات فقط","خطأ شائع","مثال سريع","احفظ القاعدة دي","شرح مختصر","اختبار سريع","معلومة مهمة","الخلاصة هنا");
            case "news": return a("ملخص سريع","أهم ما حدث","التفاصيل في ثواني","تحديث جديد","الخبر بالأرقام","ماذا يعني هذا؟","3 نقاط مهمة","آخر التطورات","الصورة الكاملة","خلاصة الموضوع");
            case "business": return a("فكرة مشروع","درس تسويق","خطأ يكلفك","3 خطوات للنمو","كيف تبدأ؟","فكرة للمبيعات","نصيحة لصاحب مشروع","راقب الرقم ده","خدمة أفضل","الخلاصة التجارية");
            case "beforeafter": return a("شوف الفرق","النتيجة تتكلم","قبل وبعد","تحول كامل","من البداية للنهاية","اللمسة الأخيرة","فرق التفاصيل","النتيجة النهائية","مرحلة بمرحلة","شوف التغيير");
            case "slideshow": return a("لحظات تستحق الحفظ","ذكريات اليوم","أفضل الصور","قصة في صور","من البداية للنهاية","تفاصيل صغيرة","لحظة جميلة","مجموعة اليوم","صور تحكي القصة","ذكريات لا تنسى");
            case "cinematic": return a("مشهد يحكي بدون كلام","لحظة سينمائية","Mood اليوم","تفاصيل بطيئة","حكاية قصيرة","ضوء وحركة","لحظة تستحق الإعادة","من زاوية مختلفة","مشهد هادئ","نهاية سينمائية");
            case "music": return a("عيش اللحظة مع الإيقاع","المقطع المفضل","كلمات على Beat","Drop اللحظة","Visual للّحن","مقطع يستحق الإعادة","إحساس الأغنية","Beat Edit","لحظة موسيقية","خلي الصورة تتكلم");
            default: return a("رمضان كريم","كل عام وأنتم بخير","عرض المناسبة","تهنئة بسيطة","لحظة احتفال","دعوة خاصة","خصم المناسبة","ذكرى جميلة","استقبال الموسم","نهاية الموسم");
        }
    }

    private static String[] bodies(String key){
        switch(key){
            case "islamic": return a("خصص دقائق قليلة للذكر وسط يومك المزدحم.","اجعل التذكير سببًا لعمل خير اليوم.","الهدوء يبدأ حين ترتب قلبك ووقتك على ما ينفع.","شارك التذكير بلطف دون مبالغة أو ضغط على الآخرين.","راجع النص الديني قبل النشر إذا أضفت آية أو حديثًا.","اختر كلمات واضحة وبسيطة تليق بالمحتوى الهادئ.","اجعل الريل قصيرًا ومريحًا للعين والصوت.","يمكنك استبدال الخلفية وإضافة تلاوة أو صوت مرخص لديك.","استخدم خطًا عربيًا واضحًا واترك مساحة آمنة للنص.","اختم برسالة خير قصيرة يمكن للقارئ تطبيقها اليوم.");
            case "podcast": return a("ابدأ بأقوى جملة من الحوار ثم اترك التفاصيل تكمل الفكرة.","اختصر المقطع على نقطة واحدة واضحة حتى لا يتشتت المشاهد.","استخدم Captions واضحة لأن كثيرًا من الناس يشاهدون بدون صوت.","ضع السؤال في البداية والإجابة في المنتصف والخلاصة في النهاية.","قص التوقفات الطويلة وحافظ على الإيقاع الطبيعي للكلام.","أبرز الكلمات المهمة بلون مختلف دون ازدحام الشاشة.","اختر لقطة فيها تعبير واضح لتكون بداية أقوى.","أضف اسم المتحدث بشكل بسيط ثم اترك التركيز للكلام.","قسّم الفكرة الطويلة إلى سلسلة أجزاء قصيرة.","اختم بسؤال يشجع الجمهور على النقاش.");
            case "gaming": return a("ابدأ باللحظة الأقوى ثم ارجع سريعًا لبداية اللقطة.","استخدم Zoom خفيف على نقطة الفوز مع صوت مناسب.","اكتب النتيجة أو التحدي بوضوح من أول ثانية.","اجعل القص سريعًا ومتزامنًا مع الحركة.","أظهر Reaction صغير إذا كان متوفرًا ثم ركز على اللعب.","استخدم Caption قصير بدل فقرات طويلة.","كرّر اللحظة الحاسمة مرة بطيئة في النهاية.","وضح Tip واحد يمكن للمشاهد تطبيقه فورًا.","استخدم 9:16 مع Crop يتابع منطقة اللعب المهمة.","اختم بالنتيجة أو السؤال عن أفضل لقطة.");
            case "cars": return a("ابدأ بلقطة خارجية قوية ثم انتقل للتفاصيل الداخلية.","اعرض الميزة الرئيسية مع رقم أو معلومة قصيرة.","قارن نقطة واحدة بوضوح بدل عرض مواصفات كثيرة.","استخدم صوت المحرك الأصلي إذا كان واضحًا ومناسبًا.","أظهر السعر أو العرض في النهاية بشكل مرتب.","ركز على التصميم والإضاءة والتفاصيل القريبة.","استخدم انتقالات نظيفة حتى يبقى شكل السيارة هو البطل.","في قبل/بعد اجعل زاوية التصوير متقاربة للمقارنة.","أضف CTA للتواصل أو الحجز إذا كان المحتوى تجاريًا.","اكتب الموديل والسنة بشكل ثابت وواضح.");
            case "food": return a("ابدأ بأقرب لقطة للطبق ثم اكشف الشكل كاملًا.","استخدم خطوات قصيرة من التحضير حتى التقديم.","أظهر السعر والعرض في Card بسيطة في النهاية.","اجعل النص بعيدًا عن الطبق حتى لا يغطي التفاصيل.","لقطة السكب أو التقطيع مناسبة كبداية جذابة.","استخدم ألوان دافئة مع انتقالات سريعة وناعمة.","ضع اسم الطبق والمكونات الرئيسية فقط.","اختم بعنوان الفرع أو طريقة الطلب لو كان إعلانًا.","اجعل مدة اللقطات قصيرة للحفاظ على الشهية والإيقاع.","استخدم موسيقى مرخصة لديك أو الصوت الأصلي للمطبخ.");
            case "realestate": return a("ابدأ بأوسع لقطة ثم تحرك بين الغرف بترتيب منطقي.","اكتب المساحة وعدد الغرف في البداية بدون زحام.","أظهر نقطة تميز واحدة مثل الإطلالة أو الموقع.","استخدم حركة هادئة حتى يستطيع المشاهد رؤية التفاصيل.","في النهاية ضع السعر أو وسيلة التواصل إذا رغبت.","رتب الجولة: استقبال ثم غرف ثم مطبخ ثم إطلالة.","استخدم Safe Zones حتى لا تغطي أزرار المنصة البيانات.","أضف اسم المنطقة بوضوح في أول ثوانٍ.","قارن السعر بالقيمة بمعلومة مختصرة قابلة للتحقق.","اختم بدعوة للمعاينة بدل نص طويل.");
            case "products": return a("ابدأ بالمشكلة ثم أظهر المنتج كحل عملي.","اعرض الفائدة الأساسية قبل التفاصيل التقنية.","استخدم قبل/بعد إذا كانت النتيجة قابلة للمقارنة بوضوح.","أظهر المنتج في استخدام حقيقي بدل لقطة ثابتة فقط.","اكتب العرض والسعر بوضوح وتجنب نص صغير جدًا.","اختم بزر أو CTA واضح للطلب أو معرفة المزيد.","استخدم 3 لقطات: المنتج، الاستخدام، النتيجة.","أبرز ميزة واحدة في كل مشهد بدل جمع كل المزايا.","دع الحركة بسيطة كي لا تشتت عن المنتج.","يمكنك حفظ Brand Kit لتطبيق الشعار والألوان بضغطة.");
            case "motivation": return a("ابدأ بجملة قصيرة ثم أعط المشاهد فكرة واحدة قابلة للتطبيق.","اجعل الإيقاع هادئًا والنص كبيرًا وواضحًا.","استخدم صور تقدم أو عمل بدل لقطات لا علاقة لها بالرسالة.","ركز على خطوة صغيرة يمكن تنفيذها اليوم.","لا تملأ الشاشة بالنص؛ جملة واحدة في كل لحظة أفضل.","استخدم نهاية تشجع على الاستمرار لا على المقارنة بالآخرين.","قسّم الرسالة إلى بداية وسبب وخطوة عملية.","حافظ على صوت واضح إذا أضفت Voice Over.","اختر لون Highlight للكلمة الأهم فقط.","اختم بتذكير بسيط للمشاهد أن يبدأ الآن.");
            case "quotes": return a("ضع الاقتباس في المنتصف واترك مساحة تنفس حوله.","استخدم خلفية بسيطة كي يبقى النص هو العنصر الأساسي.","قسّم الجملة الطويلة إلى سطرين أو ثلاثة فقط.","أضف اسم المصدر يدويًا إذا كان الاقتباس منسوبًا لشخص.","استخدم حركة دخول خفيفة بدل مؤثرات كثيرة.","اختر موسيقى هادئة مرخصة لديك إن احتاج التصميم.","استخدم Contrast قوي بين النص والخلفية.","ضع كلمة واحدة بلون مختلف لإبراز المعنى.","اجعل المدة كافية للقراءة بدون بطء زائد.","اختم بسؤال قصير مرتبط بالفكرة.");
            case "travel": return a("ابدأ بأجمل لقطة للمكان ثم اعرض تفاصيل اليوم.","اكتب اسم المكان والمدينة في أول ثانية.","أضف Tip واحد مفيد مثل التوقيت أو طريقة الوصول.","استخدم Map لقطة ثابتة فقط إذا كانت لديك حقوق استخدامها.","رتب المشاهد من الوصول إلى التجربة ثم النهاية.","استخدم ألوان طبيعية ولا تبالغ في الفلاتر.","أظهر الطعام أو الشارع أو النشاط المحلي بجانب المنظر.","اجعل CTA سؤالًا عن المكان التالي الذي يريد الجمهور رؤيته.","لخص الميزانية أو المدة في Card واحدة إذا احتجت.","احفظ نسخة 9:16 وأخرى 4:5 من نفس المشروع.");
            case "fitness": return a("اعرض الحركة كاملة ثم نقطة التصحيح المهمة.","استخدم عداد أو توقيت بسيط للتحديات القصيرة.","وضح أن المحتوى عام ولا يستبدل نصيحة مختص عند الحاجة.","ركز على الفورم والزوايا الواضحة بدل مؤثرات كثيرة.","استخدم قبل/بعد فقط عندما تكون المقارنة عادلة وواضحة.","قسّم الروتين إلى أسماء تمارين قصيرة.","اجعل الموسيقى أقل من الصوت لو فيه شرح.","أضف عدد التكرارات أو الزمن في مكان ثابت.","اختم بتذكير بالراحة المناسبة حسب الخطة الشخصية.","احفظ Template خاص بروتينك لاستخدامه كل أسبوع.");
            case "fashion": return a("ابدأ بالنتيجة النهائية ثم ارجع لخطوات اللوك.","استخدم انتقالات Match Cut بين الإطلالات.","اكتب أسماء القطع أو الخطوات باختصار.","اختر خلفية محايدة لو التفاصيل والألوان كثيرة.","اجعل كل Look له ثانيتان أو ثلاث ليراها المشاهد.","استخدم Split Screen للمقارنة بين اختيارين.","في Beauty اعرض الخطوات بوضوح بدون مبالغة في الفلاتر.","اختم باللوك النهائي واسم الستايل.","استخدم Brand Kit إذا كان المحتوى لصفحة تجارية.","صدّر 9:16 للريل و4:5 للمنشور من نفس المشروع.");
            case "education": return a("ابدأ بالسؤال ثم قدم الإجابة في خطوات قصيرة.","استخدم مثالًا واحدًا قبل إضافة تفاصيل أكثر.","ضع المصطلح الرئيسي بخط كبير ومعناه تحته.","استخدم أرقام 1 2 3 عند الشرح المتسلسل.","اختم بخلاصة يمكن حفظها أو تصويرها Screenshot.","حافظ على Caption واضح ومتزامن مع الكلام.","قسم الفكرة الطويلة إلى سلسلة بدل Reel مزدحم.","استخدم لون Highlight للكلمات الأساسية فقط.","أضف مصدرًا عندما تعرض معلومة تحتاج مرجعًا.","اختم بسؤال بسيط لاختبار الفهم.");
            case "news": return a("ابدأ بعنوان واضح ثم اعرض النقاط المؤكدة فقط.","فرّق بصريًا بين الخبر والرأي داخل التصميم.","اكتب التاريخ إذا كان الخبر سريع التغير.","استخدم مصدرًا واضحًا عندما يكون متاحًا.","قسّم الملخص إلى 3 نقاط بدل فقرة طويلة.","تجنب الصور المضللة أو القديمة غير المرتبطة بالحدث.","استخدم أسلوبًا هادئًا بدون مؤثرات مبالغ فيها.","اختم بما هو معروف الآن وما زال غير مؤكد.","إذا تغير الخبر لاحقًا أنشئ Update بدل تعديل المعنى بصمت.","اجعل Cover يحمل عنوانًا قصيرًا وواضحًا.");
            case "business": return a("ابدأ بالمشكلة التجارية ثم اعرض خطوة عملية للحل.","استخدم رقمًا واحدًا مهمًا بدل جدول مزدحم.","اعرض مثالًا واقعيًا مبسطًا قبل الخلاصة.","ضع CTA مناسبًا مثل الحجز أو التواصل أو حفظ الفيديو.","استخدم Brand Kit حتى تبدو السلسلة كلها بهوية واحدة.","ركز على فائدة العميل لا على وصف المنتج فقط.","اجعل كل Reel يجيب سؤالًا واحدًا محددًا.","استخدم Before/After للنتائج القابلة للقياس بوضوح.","اختبر أكثر من Hook لنفس المحتوى.","اختم بخطوة تالية واضحة للمشاهد.");
            case "beforeafter": return a("حافظ على نفس الزاوية قدر الإمكان حتى تكون المقارنة عادلة.","ابدأ بالنتيجة ثانية واحدة ثم اعرض البداية.","استخدم Split Screen لو المقارنة تحتاج رؤية اللحظتين معًا.","ضع أسماء Before وAfter بوضوح.","لا تستخدم فلاتر مختلفة تجعل المقارنة مضللة.","أضف المدة أو الخطوات إذا كانت مهمة لفهم النتيجة.","استخدم انتقال Swipe بسيط بين المرحلتين.","ركز على تفصيلة واحدة في كل مقارنة.","اختم بلقطة نهائية كاملة للنتيجة.","احفظ القالب لاستخدام نفس أسلوب المقارنة مستقبلًا.");
            case "slideshow": return a("اختر الصور بترتيب يحكي قصة واضحة.","اجعل مدة كل صورة مناسبة للقراءة والمشاهدة.","استخدم Ken Burns خفيف بدل Zoom مبالغ فيه.","ضع عنوانًا واحدًا في البداية وتفاصيل قليلة أثناء الصور.","زامن التغييرات مع Beat إذا أضفت موسيقى.","استخدم انتقالًا موحدًا ليبقى الشكل احترافيًا.","رتب الصور من بداية الحدث إلى نهايته.","أضف تاريخ أو مكان للذكريات إذا رغبت.","اجعل الصورة الأقوى Cover للمشروع.","اختم بصورة نهائية ورسالة قصيرة.");
            case "cinematic": return a("استخدم قصات أبطأ ومساحات هادئة بين التفاصيل.","ركز على الضوء والحركة بدل كثرة النصوص.","ضع عنوانًا قصيرًا ثم دع المشاهد يتكلم بصريًا.","استخدم Speed Ramp فقط عندما يخدم الحركة.","حافظ على تدرج ألوان متناسق بين اللقطات.","استخدم أصوات المكان لو كانت تضيف إحساسًا للمشهد.","اختر Transition بسيط مثل Fade أو Match Cut.","اجعل كل لقطة لها بداية ونهاية واضحة.","استخدم Crop 9:16 مع متابعة العنصر الأساسي.","اختم بلقطة هادئة تصلح كCover أيضًا.");
            case "music": return a("ضع الكلمات المهمة متزامنة مع الإيقاع قدر الإمكان.","استخدم Highlight كلمة بكلمة لو كان النص قصيرًا.","اختر Visualizer بسيط لا يغطي المحتوى الأساسي.","زامن Cuts على Beat Points لنتيجة أنظف.","استخدم نسخة صوت تملك حق استخدامها أو مرخصة لك.","اجعل النص بعيدًا عن أزرار المنصة في Safe Zone.","استخدم Loop سلس لو المقطع قصير.","اختر Cover يحمل اسم المقطع بشكل واضح.","استخدم حركة نصية واحدة متسقة بدل عدة أنماط.","صدّر بجودة مناسبة للصوت والفيديو معًا.");
            default: return a("استخدم ألوان المناسبة مع هوية الصفحة.","ضع تاريخ المناسبة أو العرض عند الحاجة.","اجعل الرسالة قصيرة وواضحة وقابلة للتعديل.","استخدم صورك أو فيديوهاتك الخاصة داخل Slots القالب.","أضف Logo أو Brand Kit بضغطة واحدة.","احفظ نسخة بدون نص إذا احتجت إعادة الاستخدام.","استخدم 9:16 للريل وStory مع Safe Zones.","غيّر الموسيقى أو الصوت بما يناسب المناسبة وحقوق الاستخدام.","اجعل CTA واضحًا إذا كان هناك عرض أو حجز.","صدّر نسخة نهائية واحتفظ بالمشروع كDraft للتعديل لاحقًا.");
        }
    }

    private static String[] ctas(String key){
        if("islamic".equals(key)) return a("شارك الخير بلطف","احفظ التذكير وارجع له لاحقًا","اجعل لك دقيقة هادئة اليوم");
        if("products".equals(key)||"food".equals(key)||"realestate".equals(key)||"business".equals(key)) return a("اعرف التفاصيل الآن","احفظ العرض وتواصل عند الحاجة","شاركها مع شخص مهتم");
        return a("احفظ الفيديو","شارك رأيك","تابع للمزيد");
    }

    private static String hashtags(String key,String topic,int index){
        String base;
        switch(key){
            case "islamic": base="#اسلاميات #تذكير #ذكر_الله #ريلز_اسلامي #محتوى_نافع";break;
            case "podcast": base="#بودكاست #Podcast #مقتطفات #حوار #Reels";break;
            case "gaming": base="#Gaming #جيمينج #Highlights #Games #Shorts";break;
            case "cars": base="#سيارات #Cars #مراجعة_سيارات #عربيات #Reels";break;
            case "food": base="#Food #مطاعم #اكل #وصفات #Reels";break;
            case "realestate": base="#عقارات #RealEstate #شقق #عقار #Reels";break;
            case "products": base="#منتجات #اعلان #Marketing #Offers #Reels";break;
            case "motivation": base="#تحفيز #تطوير_الذات #Motivation #عادات #Shorts";break;
            case "quotes": base="#اقتباسات #Quotes #رسائل #افكار #Reels";break;
            case "travel": base="#Travel #سفر #رحلات #اماكن #Reels";break;
            case "fitness": base="#Fitness #تمارين #رياضة #Gym #Reels";break;
            case "fashion": base="#Fashion #Beauty #ستايل #موضة #Reels";break;
            case "education": base="#تعليم #معلومة #Education #شرح #Shorts";break;
            case "news": base="#اخبار #ملخص #News #تحديث #Reels";break;
            case "business": base="#Business #تسويق #مشاريع #نصائح #Reels";break;
            case "beforeafter": base="#BeforeAfter #قبل_وبعد #Transformation #Reels #نتيجة";break;
            case "slideshow": base="#Slideshow #صور #ذكريات #PhotoReel #Reels";break;
            case "cinematic": base="#Cinematic #فيديو #Mood #FilmLook #Reels";break;
            case "music": base="#Music #موسيقى #Lyrics #Beat #Reels";break;
            default: base="#مناسبات #Seasonal #تهنئة #عروض #Reels";break;
        }
        return base+" #DH"+((index%9)+1);
    }

    private static int shift(int color,int amount){
        int r=Math.max(0,Math.min(255,Color.red(color)+amount));
        int g=Math.max(0,Math.min(255,Color.green(color)+amount/2));
        int b=Math.max(0,Math.min(255,Color.blue(color)+amount/3));
        return Color.rgb(r,g,b);
    }

    private static String[] a(String...v){return v;}
}
