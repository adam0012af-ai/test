package com.adam.downloadhub;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class TemplateLibraryActivity extends Activity {
    private String categoryKey;
    private EditText search;
    private LinearLayout listBox;
    private TextView counter;
    private int shown=0;
    private String lastQuery="";
    private static final int PAGE=24;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        categoryKey=getIntent().getStringExtra("category");
        if(categoryKey==null)categoryKey="islamic";
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        setContentView(buildUi());
        reload();
    }

    private View buildUi(){
        TemplateCatalog.Category cat=TemplateCatalog.category(categoryKey);
        LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);page.setBackground(Ui.gradient(Ui.BG,Ui.BG_2,0,this));
        LinearLayout top=Ui.topBar(this,cat.icon+"  "+cat.name,"300 قالب فعلي قابل للتعديل",v->finish());top.setPadding(Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,16),Ui.dp(this,8));page.addView(top);

        LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.HORIZONTAL);controls.setPadding(Ui.dp(this,16),Ui.dp(this,6),Ui.dp(this,16),Ui.dp(this,10));
        search=Ui.input(this,"ابحث داخل 300 قالب…",false);controls.addView(search,new LinearLayout.LayoutParams(0,Ui.dp(this,50),1f));
        Button go=Ui.primary(this,"بحث");go.setOnClickListener(v->reload());LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(Ui.dp(this,82),Ui.dp(this,50));gp.setMargins(Ui.dp(this,8),0,0,0);controls.addView(go,gp);page.addView(controls);

        counter=Ui.text(this,"",12,Ui.CYAN,true);counter.setPadding(Ui.dp(this,18),0,Ui.dp(this,18),Ui.dp(this,8));page.addView(counter);

        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        listBox=new LinearLayout(this);listBox.setOrientation(LinearLayout.VERTICAL);listBox.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),Ui.dp(this,28));scroll.addView(listBox,new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT));page.addView(scroll,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1f));
        return page;
    }

    private void reload(){lastQuery=search==null?"":search.getText().toString().trim();shown=0;if(listBox!=null)listBox.removeAllViews();loadMore();}

    private void loadMore(){
        List<ReelTemplate> items=TemplateCatalog.templates(categoryKey,lastQuery,shown,PAGE);
        if(items.isEmpty()&&shown==0){
            LinearLayout empty=Ui.card(this);empty.addView(Ui.text(this,"مفيش نتيجة للكلمة دي داخل القسم. جرّب كلمة تانية.",13,Ui.MUTED,false));listBox.addView(empty);counter.setText("0 نتيجة");return;
        }
        for(ReelTemplate t:items)listBox.addView(templateCard(t));shown+=items.size();
        int total=countMatches();counter.setText("ظاهر "+Math.min(shown,total)+" من "+total+" قالب");
        removeLoadButton();
        if(shown<total){Button more=Ui.secondary(this,"عرض قوالب أكثر  ＋");more.setTag("load_more");more.setOnClickListener(v->loadMore());LinearLayout.LayoutParams lp=Ui.matchWrap();lp.height=Ui.dp(this,52);lp.setMargins(0,Ui.dp(this,4),0,0);listBox.addView(more,lp);}
    }

    private int countMatches(){return TemplateCatalog.templates(categoryKey,lastQuery,0,TemplateCatalog.TEMPLATES_PER_CATEGORY).size();}
    private void removeLoadButton(){for(int i=listBox.getChildCount()-1;i>=0;i--){View v=listBox.getChildAt(i);if("load_more".equals(v.getTag())){listBox.removeViewAt(i);break;}}}

    private View templateCard(ReelTemplate t){
        LinearLayout card=Ui.card(this);LinearLayout.LayoutParams cp=Ui.matchWrap();cp.setMargins(0,0,0,Ui.dp(this,10));card.setLayoutParams(cp);card.setBackground(Ui.gradient(t.startColor,t.endColor,21,this));
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=Ui.text(this,t.name,15,Ui.TEXT,true);head.addView(name,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f));
        TextView dur=Ui.chip(this,t.durationSec+"ث",Ui.CYAN);head.addView(dur);card.addView(head);
        TextView hook=Ui.text(this,t.hook,18,Ui.TEXT,true);hook.setGravity(Gravity.CENTER);hook.setPadding(0,Ui.dp(this,14),0,Ui.dp(this,8));card.addView(hook);
        TextView body=Ui.text(this,t.body,12,0xFFD6E5F8,false);body.setGravity(Gravity.CENTER);body.setMaxLines(3);card.addView(body);
        LinearLayout meta=new LinearLayout(this);meta.setOrientation(LinearLayout.HORIZONTAL);meta.setGravity(Gravity.CENTER);LinearLayout.LayoutParams mp=Ui.matchWrap();mp.setMargins(0,Ui.dp(this,12),0,0);
        meta.addView(Ui.chip(this,t.layout,Ui.CYAN));TextView motion=Ui.chip(this,t.motion,Ui.GREEN);LinearLayout.LayoutParams m=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);m.setMargins(Ui.dp(this,6),0,0,0);meta.addView(motion,m);card.addView(meta,mp);
        Button use=Ui.accent(this,"استخدم وعدّل القالب");use.setOnClickListener(v->openEditor(t.id));LinearLayout.LayoutParams up=Ui.matchWrap();up.height=Ui.dp(this,50);up.setMargins(0,Ui.dp(this,14),0,0);card.addView(use,up);
        return card;
    }

    private void openEditor(String id){Intent i=new Intent(this,TemplateEditorActivity.class);i.putExtra("template_id",id);startActivity(i);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
