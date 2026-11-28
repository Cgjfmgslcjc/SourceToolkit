package com.sourcetoolkit.app;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.view.*;import android.widget.*;import androidx.documentfile.provider.DocumentFile;import java.util.*;
public class GameFolderPickerActivity extends Activity {
 final int BG=0xFF0B0E10,SURFACE=0xFF12171B,TEXT=0xFFE6EAED,MUTED=0xFF9AA5AD,BLUE=0xFF2D9CFF;
 DocumentFile root,current;String path="",mode="folder";Set<String> extensions=new HashSet<>(),chosen=new LinkedHashSet<>();LinearLayout list;TextView breadcrumb;
 int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);return t;}
 Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(TEXT);b.setBackgroundColor(0xFF1B2227);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
 root=MainActivity.gameRoot(this);if(root==null||!root.isDirectory()){setResult(RESULT_CANCELED);finish();return;}
 mode=getIntent().getStringExtra("mode");if(mode==null)mode="folder";
 String ext=getIntent().getStringExtra("extensions");if(ext!=null)for(String x:ext.split(","))extensions.add(x.trim().toLowerCase(Locale.ROOT));
 path=getIntent().getStringExtra("start_path");if(path==null)path="";current=resolve(path);if(current==null||!current.isDirectory()){path="";current=root;}
 LinearLayout base=new LinearLayout(this);base.setOrientation(1);base.setPadding(dp(16),dp(16),dp(16),dp(12));base.setBackgroundColor(BG);setContentView(base);
 base.addView(text(mode.equals("folder")?"选择文件夹":"选择文件",19,TEXT));breadcrumb=text("",11,MUTED);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(10);base.addView(breadcrumb,bp);
 LinearLayout nav=new LinearLayout(this);Button up=button("上一级"),choose=button(mode.equals("folder")?"选择当前目录":mode.equals("multi")?"完成选择":"返回");nav.addView(up,new LinearLayout.LayoutParams(0,dp(44),1));nav.addView(choose,new LinearLayout.LayoutParams(0,dp(44),1));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(12);base.addView(nav,np);
 up.setOnClickListener(v->goUp());choose.setOnClickListener(v->{if(mode.equals("folder"))selectCurrent();else if(mode.equals("multi"))finishMulti();else setResult(RESULT_CANCELED);if(!mode.equals("file"))finish();});
 ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);lp.topMargin=dp(12);base.addView(scroll,lp);render();}
 DocumentFile resolve(String p){DocumentFile f=root;if(p==null||p.isEmpty())return f;for(String x:p.split("/")){f=f.findFile(x);if(f==null)return null;}return f;}
 String relative(DocumentFile f){return path.isEmpty()?f.getName():path+"/"+f.getName();}
 boolean accepted(DocumentFile f){if(f.isDirectory())return true;if(extensions.isEmpty())return true;String n=f.getName();if(n==null)return false;n=n.toLowerCase(Locale.ROOT);for(String x:extensions)if(n.endsWith(x.startsWith(".")?x:"."+x))return true;return false;}
 void render(){breadcrumb.setText("garrysmod / "+(path.isEmpty()?"（根目录）":path));list.removeAllViews();DocumentFile[] children=current.listFiles();Arrays.sort(children,(a,b)->{if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;return String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName()));});
 for(DocumentFile f:children){if(!accepted(f))continue;LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(12),dp(8),dp(12));row.setBackgroundColor(SURFACE);TextView label=text((f.isDirectory()?"📁  ":"▤  ")+f.getName(),14,TEXT);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));
 if(mode.equals("multi")&&!f.isDirectory()){CheckBox cb=new CheckBox(this);cb.setButtonTintList(android.content.res.ColorStateList.valueOf(BLUE));String u=f.getUri().toString();cb.setChecked(chosen.contains(u));cb.setOnCheckedChangeListener((v,on)->{if(on)chosen.add(u);else chosen.remove(u);});row.addView(cb);}
 LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=dp(1);list.addView(row,rp);
 row.setOnClickListener(v->{if(f.isDirectory()){path=relative(f);current=f;render();}else if(mode.equals("file"))selectFile(f);else if(mode.equals("multi")){String u=f.getUri().toString();if(chosen.contains(u))chosen.remove(u);else chosen.add(u);render();}});
 if(mode.equals("folder")&&f.isDirectory())row.setOnLongClickListener(v->{path=relative(f);current=f;selectCurrent();return true;});
 }
 if(list.getChildCount()==0){TextView empty=text("此目录没有符合条件的内容",12,MUTED);empty.setPadding(dp(10),dp(18),0,0);list.addView(empty);}}
 void goUp(){if(path.isEmpty())return;int n=path.lastIndexOf('/');path=n<0?"":path.substring(0,n);current=resolve(path);render();}
 void selectCurrent(){Intent d=new Intent();d.putExtra("folder_path",path);setResult(RESULT_OK,d);finish();}
 void selectFile(DocumentFile f){Intent d=new Intent();d.putExtra("selected_uri",f.getUri().toString());d.putExtra("selected_name",f.getName());setResult(RESULT_OK,d);finish();}
 void finishMulti(){Intent d=new Intent();d.putStringArrayListExtra("selected_uris",new ArrayList<>(chosen));setResult(RESULT_OK,d);}
}
