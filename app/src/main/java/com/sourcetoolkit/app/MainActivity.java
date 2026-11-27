package com.sourcetoolkit.app;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.Bundle;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;
public class MainActivity extends Activity {
 static final String PREF="source_toolkit",GAME_URI="game_uri";
 final int BG=0xFF0B0E10,SURFACE=0xFF12171B,LINE=0xFF232B31,BLUE=0xFF2D9CFF,TEXT=0xFFE6EAED,MUTED=0xFF9AA5AD;
 int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.create("sans-serif-condensed",1));return t;}
 GradientDrawable shape(int color,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);if(stroke!=0)d.setStroke(dp(1),stroke);d.setCornerRadius(dp(3));return d;}
 Button button(String s,int color,int textColor){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setTextColor(textColor);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-condensed",1));b.setBackground(shape(color,color==BLUE?0:0xFF38434C));return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);render();}
 void render(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(20),dp(28),dp(20),dp(18));root.setBackgroundColor(BG);setContentView(root);
  TextView brand=text("SOURCE TOOLKIT",25,TEXT,true);brand.setLetterSpacing(.06f);root.addView(brand);
  TextView subtitle=text("SOURCE ENGINE UTILITIES",9,BLUE,false);subtitle.setLetterSpacing(.18f);LinearLayout.LayoutParams subp=new LinearLayout.LayoutParams(-1,-2);subp.topMargin=dp(4);root.addView(subtitle,subp);
  View divider=new View(this);divider.setBackgroundColor(LINE);LinearLayout.LayoutParams divp=new LinearLayout.LayoutParams(-1,dp(1));divp.topMargin=dp(20);divp.bottomMargin=dp(18);root.addView(divider,divp);
  boolean ready=getGameUri()!=null;LinearLayout status=new LinearLayout(this);status.setGravity(Gravity.CENTER_VERTICAL);View dot=new View(this);dot.setBackgroundColor(ready?0xFF73D5AA:0xFFFFB45C);GradientDrawable dotBg=new GradientDrawable();dotBg.setColor(ready?0xFF73D5AA:0xFFFFB45C);dotBg.setCornerRadius(dp(10));dot.setBackground(dotBg);status.addView(dot,new LinearLayout.LayoutParams(dp(7),dp(7)));
  TextView state=text(ready?"游戏目录已连接":"尚未选择游戏目录",12,ready?0xFF73D5AA:0xFFFFB45C,true);LinearLayout.LayoutParams stp=new LinearLayout.LayoutParams(0,-2,1);stp.leftMargin=dp(9);status.addView(state,stp);
  TextView change=text(ready?"更改":"选择目录",12,BLUE,true);status.addView(change);change.setOnClickListener(v->pickGame());root.addView(status);
  Button mods=button("GMod 模组管理",BLUE,BG);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,dp(54));mp.topMargin=dp(24);root.addView(mods,mp);mods.setOnClickListener(v->openMods());
  TextView hint=text("管理已安装模组、创意工坊内容与本地插件",11,MUTED,false);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(8);root.addView(hint,hp);
  View spacer=new View(this);root.addView(spacer,new LinearLayout.LayoutParams(1,dp(30)));
  section(root,"资源工具");
  toolRow(root,"VTF 转换器","PNG / JPG 转换为 Valve Texture Format",()->placeholder("VTF 转换器","VTF 编码器尚未接入。"));
  toolRow(root,"VPK 浏览器","查看归档内容并导出文件",()->placeholder("VPK 浏览器","VPK 浏览与解包功能正在开发。"));
  View fill=new View(this);root.addView(fill,new LinearLayout.LayoutParams(1,0,1));
  View bottom=new View(this);bottom.setBackgroundColor(LINE);root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(1)));
  LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER_VERTICAL);footer.setPadding(0,dp(12),0,0);TextView version=text("SOURCE TOOLKIT  ·  0.1.0",9,0xFF65717A,false);footer.addView(version,new LinearLayout.LayoutParams(0,-2,1));TextView settings=text("设置  ›",12,MUTED,false);footer.addView(settings);settings.setOnClickListener(v->settings());root.addView(footer);
 }
 void section(LinearLayout root,String s){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView bullet=text("▪",12,BLUE,true);row.addView(bullet);TextView label=text("  "+s,10,MUTED,false);label.setLetterSpacing(.12f);row.addView(label);View line=new View(this);line.setBackgroundColor(LINE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(1),1);p.leftMargin=dp(10);row.addView(line,p);root.addView(row);}
 void toolRow(LinearLayout root,String title,String desc,Runnable action){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(2),dp(15),dp(2),dp(15));LinearLayout labels=new LinearLayout(this);labels.setOrientation(1);labels.addView(text(title,15,TEXT,false));TextView d=text(desc,10,MUTED,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(4);labels.addView(d,p);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));row.addView(text("›",22,MUTED,false));row.setOnClickListener(v->action.run());root.addView(row);View line=new View(this);line.setBackgroundColor(LINE);root.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
 Uri getGameUri(){String s=getSharedPreferences(PREF,0).getString(GAME_URI,null);return s==null?null:Uri.parse(s);}
 void pickGame(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,41);}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==41&&c==RESULT_OK&&d!=null){Uri u=d.getData();getContentResolver().takePersistableUriPermission(u,d.getFlags()&3);getSharedPreferences(PREF,0).edit().putString(GAME_URI,u.toString()).apply();render();}}
 void openMods(){if(getGameUri()==null){new AlertDialog.Builder(this).setMessage("请先选择 Garry's Mod 游戏目录。").setPositiveButton("选择目录",(d,w)->pickGame()).setNegativeButton("取消",null).show();return;}startActivity(new Intent(this,GmodModActivity.class));}
 void settings(){new AlertDialog.Builder(this).setTitle("设置").setMessage("固定深色主题。当前最低支持 Android 8.0。").setPositiveButton("更改游戏目录",(d,w)->pickGame()).setNegativeButton("关闭",null).show();}
 void placeholder(String title,String message){new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("确定",null).show();}
}