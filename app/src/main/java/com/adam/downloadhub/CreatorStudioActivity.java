package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class CreatorStudioActivity extends Activity {
    private LinearLayout draftsBox;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
    }

    @Override protected void onResume(){super.onResume();renderDrafts();}

    private View buildUi(){
        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,34));
        scroll.addView(root,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(Ui.topBar(this,"Creator Studio","Create • Edit • Export • Publish",v->finish()));

        LinearLayout hero=Ui.card(this);
        LinearLayout.LayoutParams hp=Ui.matchWrap();hp.setMargins(0,Ui.dp(this,16),0,0);hero.setLayoutParams(hp);
        hero.setBackground(Ui.gradient3(0xFF0B3D77,0xFF112954,0xFF09182E,25,this));
        TextView h=Ui.text(this,"حوّل فكرتك إلى Reel جاهز للنشر",23,Ui.TEXT,true);h.setGravity(Gravity.START);hero.addView(h);
        TextView sub=Ui.text(this,"قوالب فعلية قابلة للتعديل • 9:16 • نصوص • صوت • تصدير • عنوان وهاشتاج تلقائي",13,0xFFC3D7F5,false);sub.setLineSpacing(0,1.25f);LinearLayout.LayoutParams sp=Ui.matchWrap();sp.setMargins(0,Ui.dp(this,8),0,Ui.dp(this,14));hero.addView(sub,sp);
        LinearLayout chips=new LinearLayout(this);chips.setOrientation(LinearLayout.HORIZONTAL);
        TextView one=Ui.chip(this,TemplateCatalog.totalTemplates()+" قالب",Ui.CYAN);chips.addView(one);
        TextView two=Ui.chip(this,"300 لكل قسم",Ui.GREEN);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);cp.setMargins(Ui.dp(this,8),0,0,0);chips.addView(two,cp);
        hero.addView(chips);root.addView(hero);

        TextView quick=Ui.sectionTitle(this,"ابدأ بسرعة");LinearLayout.LayoutParams qp=Ui.matchWrap();qp.setMargins(0,Ui.dp(this,22),0,Ui.dp(this,10));root.addView(quick,qp);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        Button isl=Ui.accent(this,"☾  ريلز إسلامي");isl.setOnClickListener(v->openCategory("islamic"));row.addView(isl,new LinearLayout.LayoutParams(0,Ui.dp(this,58),1f));
        Button all=Ui.primary(this,"▦  كل القوالب");all.setOnClickListener(v->openCategory("islamic"));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,Ui.dp(this,58),1f);ap.setMargins(Ui.dp(this,9),0,0,0);row.addView(all,ap);root.addView(row);

        Button newProject=Ui.secondary(this,"＋  مشروع جديد من ملف أو صورة");
        newProject.setOnClickListener(v->{Intent i=new Intent(this,TemplateEditorActivity.class);i.putExtra("template_id","cinematic-0");i.putExtra("pick_media",true);startActivity(i);});
        LinearLayout.LayoutParams np=Ui.matchWrap();np.height=Ui.dp(this,54);np.setMargins(0,Ui.dp(this,9),0,0);root.addView(newProject,np);

        TextView catTitle=Ui.sectionTitle(this,"الأقسام");LinearLayout.LayoutParams ct=Ui.matchWrap();ct.setMargins(0,Ui.dp(this,22),0,Ui.dp(this,10));root.addView(catTitle,ct);
        for(TemplateCatalog.Category cat:TemplateCatalog.categories())root.addView(categoryCard(cat));

        TextView dtitle=Ui.sectionTitle(this,"مشاريعي");LinearLayout.LayoutParams dt=Ui.matchWrap();dt.setMargins(0,Ui.dp(this,22),0,Ui.dp(this,10));root.addView(dtitle,dt);
        draftsBox=new LinearLayout(this);draftsBox.setOrientation(LinearLayout.VERTICAL);root.addView(draftsBox);

        TextView foot=Ui.text(this,"Download Hub v6 • Creator Studio • Developed by AboAdam",11,Ui.MUTED_2,true);foot.setGravity(Gravity.CENTER);LinearLayout.LayoutParams fp=Ui.matchWrap();fp.setMargins(0,Ui.dp(this,24),0,0);root.addView(foot,fp);
        return scroll;
    }

    private View categoryCard(TemplateCatalog.Category c){
        LinearLayout card=Ui.card(this);card.setPadding(Ui.dp(this,14),Ui.dp(this,13),Ui.dp(this,14),Ui.dp(this,13));
        LinearLayout.LayoutParams p=Ui.matchWrap();p.setMargins(0,0,0,Ui.dp(this,9));card.setLayoutParams(p);
        card.setBackground(Ui.gradient(c.startColor,c.endColor,20,this));
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
        TextView icon=Ui.text(this,c.icon,26,Ui.TEXT,true);icon.setGravity(Gravity.CENTER);row.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,44),Ui.dp(this,44)));
        LinearLayout texts=new LinearLayout(this);texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this,c.name,16,Ui.TEXT,true));
        texts.addView(Ui.text(this,c.description,11,0xFFD1E2FA,false));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f);tp.setMargins(Ui.dp(this,10),0,0,0);row.addView(texts,tp);
        TextView count=Ui.chip(this,"300",Ui.CYAN);row.addView(count);
        card.addView(row);card.setOnClickListener(v->openCategory(c.key));return card;
    }

    private void renderDrafts(){
        if(draftsBox==null)return;draftsBox.removeAllViews();List<CreatorProject> list=DraftStore.list(this);
        if(list.isEmpty()){
            LinearLayout empty=Ui.card(this);empty.addView(Ui.text(this,"لا توجد مسودات بعد — أي تعديل تحفظه سيظهر هنا.",13,Ui.MUTED,false));draftsBox.addView(empty);return;
        }
        int n=Math.min(8,list.size());
        for(int i=0;i<n;i++){
            CreatorProject p=list.get(i);Button b=Ui.secondary(this,"✎  "+p.name+"  •  "+p.durationSec+"ث");b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setOnClickListener(v->{Intent in=new Intent(this,TemplateEditorActivity.class);in.putExtra("draft_id",p.id);startActivity(in);});LinearLayout.LayoutParams lp=Ui.matchWrap();lp.height=Ui.dp(this,52);lp.setMargins(0,0,0,Ui.dp(this,8));draftsBox.addView(b,lp);
        }
    }

    private void openCategory(String key){Intent i=new Intent(this,TemplateLibraryActivity.class);i.putExtra("category",key);startActivity(i);}
}
