package com.sourcetoolkit.app;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.widget.*;import androidx.documentfile.provider.DocumentFile;import java.util.*;
public class GameFolderPickerActivity extends Activity {
 final int BG=0xFF0B0E10,SURFACE=0xFF12171B,TEXT=0xFFE6EAED,MUTED=0xFF9AA5AD;
 DocumentFile root,current;String path="";LinearLayout list;TextView breadcrumb;
 int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);return t;}
 Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(TEXT);b.setBackgroundColor(0xFF1B2227);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
 String uri=getSharedPreferences(MainActivity.PREF,0).getString(MainActivity.GAME_URI,null);if(uri==null){setResult(RESULT_CANCELED);finish();return;}
 root=DocumentFile.fromTreeUri(this,Uri.parse(uri));if(root==null){setResult(RESULT_CANCELED);finish();return;}
 String start=getIntent().getStringExtra("start_path");if(start!=null)path=start;current=resolve(path);if(current==null||!current.isDirectory()){path="";current=root;}
 LinearLayout base=new LinearLayout(this);base.setOrientation(1);base.setPadding(dp(16),dp(16),dp(16),dp(12));base.setBackgroundColor(BG);setContentView(base);
 base.addView(text("选择游戏内文件夹",19,TEXT));breadcrumb=text("",11,MUTED);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(10);base.addView(breadcrumb,bp);
 LinearLayout nav=new LinearLayout(this);Button up=button("上一级"),choose=button("选择当前目录");nav.addView(up,new LinearLayout.LayoutParams(0,dp(44),1));nav.addView(choose,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(12);base.addView(nav,np);
 up.setOnClickListener(v->goUp());choose.setOnClickListener(v->selectCurrent());ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);lp.topMargin=dp(12);base.addView(scroll,lp);render();}
 DocumentFile resolve(String p){DocumentFile f=root;if(p==null||p.isEmpty())return f;for(String x:p.split("/")){f=f.findFile(x);if(f==null)return null;}return f;}
 void render(){breadcrumb.setText("garrysmod / "+(path.isEmpty()?"（根目录）":path));list.removeAllViews();DocumentFile[] children=current.listFiles();Arrays.sort(children,(a,b)->{if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;return String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName()));});
 for(DocumentFile f:children){if(!f.isDirectory()||f.getName()==null)continue;TextView row=text("📁  "+f.getName()+"     ›",14,TEXT);row.setPadding(dp(10),dp(14),dp(8),dp(14));row.setBackgroundColor(SURFACE);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(1);list.addView(row,rp);row.setOnClickListener(v->{path=path.isEmpty()?f.getName():path+"/"+f.getName();current=f;render();});}
 if(list.getChildCount()==0){TextView empty=text("此目录没有子文件夹",12,MUTED);empty.setPadding(dp(10),dp(18),0,0);list.addView(empty);}}
 void goUp(){if(path.isEmpty())return;int n=path.lastIndexOf('/');path=n<0?"":path.substring(0,n);current=resolve(path);render();}
 void selectCurrent(){Intent data=new Intent();data.putExtra("folder_path",path);setResult(RESULT_OK,data);finish();}
}