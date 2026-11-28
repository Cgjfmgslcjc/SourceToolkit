package com.sourcetoolkit.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import android.graphics.Typeface;
import androidx.documentfile.provider.DocumentFile;
import org.json.JSONObject;
import java.io.*;
import java.util.*;

public class ModEditorActivity extends Activity {
    private DocumentFile original;
    private DocumentFile workspace;
    private File workDir;
    private boolean gmaMode;
    private String currentPath="";
    private LinearLayout list, header;
    private TextView pathLabel;
    private final int BG=0xFF0B0E10, SURFACE=0xFF12171B, TEXT=0xFFE6EAED, BLUE=0xFF2D9CFF, MUTED=0xFF9AA5AD;

    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView label(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setTextColor(TEXT);b.setAllCaps(false);b.setBackgroundColor(SURFACE);return b;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        String raw=getIntent().getStringExtra("uri");
        if(raw==null){fail("没有收到模组路径");return;}
        Uri uri=Uri.parse(raw);
        original=fromUri(uri);
        if(original==null||!original.exists()){fail("无法访问所选模组");return;}
        gmaMode=original.isFile() || (original.getName()!=null&&original.getName().toLowerCase(Locale.ROOT).endsWith(".gma"));
        if(gmaMode){if(!prepareGma()){return;}}
        else workspace=original;
        render();
    }

    private DocumentFile fromUri(Uri u){
        if("file".equalsIgnoreCase(u.getScheme()))return DocumentFile.fromFile(new File(u.getPath()));
        DocumentFile d=DocumentFile.fromTreeUri(this,u);
        if(d==null)d=DocumentFile.fromSingleUri(this,u);
        return d;
    }
    private void fail(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();finish();}

    private boolean prepareGma(){
        try{
            File source=new File(getCacheDir(),"source_edit_"+System.currentTimeMillis()+".gma");
            try(InputStream in=getContentResolver().openInputStream(original.getUri());OutputStream out=new FileOutputStream(source)){copy(in,out);}
            List<SourceFormats.Entry> entries=SourceFormats.readGma(source);
            workDir=new File(getCacheDir(),"gma_workspace_"+System.currentTimeMillis());
            if(!workDir.mkdirs())throw new IOException("无法创建临时编辑目录");
            for(SourceFormats.Entry e:entries){
                File target=safeChild(workDir,e.name);
                File parent=target.getParentFile();if(parent!=null&&!parent.exists()&&!parent.mkdirs())throw new IOException("无法创建目录："+e.name);
                byte[] bytes=SourceFormats.readGmaEntry(source,e);
                try(FileOutputStream out=new FileOutputStream(target)){out.write(bytes);}
            }
            workspace=DocumentFile.fromFile(workDir);
            return true;
        }catch(Exception e){fail("GMA 解包失败："+e.getMessage());return false;}
    }
    private File safeChild(File root,String name)throws IOException{
        String clean=name.replace('\\','/');
        if(clean.startsWith("/")||clean.contains("../")||clean.equals(".."))throw new IOException("GMA 包含非法路径："+name);
        File f=new File(root,clean);
        String base=root.getCanonicalPath()+File.separator;
        if(!f.getCanonicalPath().startsWith(base))throw new IOException("非法文件路径");
        return f;
    }

    private void render(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(14),dp(16),dp(10));root.setBackgroundColor(BG);setContentView(root);
        TextView title=label(gmaMode?"GMA ARCHIVE EDITOR":"MOD FOLDER EDITOR",19,TEXT);title.setTypeface(Typeface.DEFAULT_BOLD);root.addView(title);
        root.addView(label(original.getName()==null?"模组":original.getName(),12,BLUE));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(0);
        Button file=button("新建文件"),folder=button("新建文件夹"),save=button(gmaMode?"重新打包并保存":"刷新");
        actions.addView(file,new LinearLayout.LayoutParams(0,dp(42),1));actions.addView(folder,new LinearLayout.LayoutParams(0,dp(42),1));actions.addView(save,new LinearLayout.LayoutParams(0,dp(42),1));root.addView(actions);
        file.setOnClickListener(v->create(false));folder.setOnClickListener(v->create(true));
        save.setOnClickListener(v->{if(gmaMode)saveGma();else{load();toast("文件夹模组已直接保存");}});
        Button up=button("返回上级目录");root.addView(up,new LinearLayout.LayoutParams(-1,dp(38)));up.setOnClickListener(v->goUp());
        pathLabel=label("/",11,MUTED);pathLabel.setPadding(0,dp(8),0,dp(8));root.addView(pathLabel);
        ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        load();
    }

    private DocumentFile current(){
        DocumentFile d=workspace;
        if(!currentPath.isEmpty())for(String p:currentPath.split("/")){if(d==null)return null;d=d.findFile(p);}
        return d;
    }
    private void load(){
        if(list==null)return;list.removeAllViews();pathLabel.setText("/"+currentPath);
        DocumentFile dir=current();if(dir==null){toast("目录不存在");goUp();return;}
        DocumentFile[] files=dir.listFiles();
        Arrays.sort(files,(a,b)->{if(a.isDirectory()!=b.isDirectory())return a.isDirectory()?-1:1;return String.valueOf(a.getName()).compareToIgnoreCase(String.valueOf(b.getName()));});
        for(DocumentFile f:files){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(4),dp(6),dp(4));row.setBackgroundColor(SURFACE);
            TextView name=label((f.isDirectory()?"📁 ":"📄 ")+f.getName(),13,TEXT);row.addView(name,new LinearLayout.LayoutParams(0,dp(44),1));
            Button more=button("⋮");row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(40)));more.setOnClickListener(v->fileMenu(f));
            row.setOnClickListener(v->{if(f.isDirectory()){currentPath=currentPath.isEmpty()?f.getName():currentPath+"/"+f.getName();load();}else if(isText(f.getName()))editText(f);else showInfo(f);});
            list.addView(row);View line=new View(this);line.setBackgroundColor(0xFF232B31);list.addView(line);
        }
        if(files.length==0)list.addView(label("此目录为空",12,MUTED));
    }
    private boolean isText(String n){String x=n==null?"":n.toLowerCase(Locale.ROOT);return x.endsWith(".lua")||x.endsWith(".txt")||x.endsWith(".json")||x.endsWith(".vmt")||x.endsWith(".cfg")||x.endsWith(".ini")||x.endsWith(".xml")||x.endsWith(".md")||x.endsWith(".nut")||x.endsWith(".vdf");}
    private void goUp(){if(currentPath.isEmpty())return;int i=currentPath.lastIndexOf('/');currentPath=i<0?"":currentPath.substring(0,i);load();}
    private void showInfo(DocumentFile f){new AlertDialog.Builder(this).setTitle(f.getName()).setMessage("类型："+(f.isDirectory()?"文件夹":"文件")+"\n大小："+(f.isFile()?f.length()+" bytes":"—")+"\n\n该文件类型暂不支持内置文本编辑，可通过更多菜单删除或重命名。").setPositiveButton("确定",null).show();}
    private void fileMenu(DocumentFile f){new AlertDialog.Builder(this).setTitle(f.getName()).setItems(new String[]{"重命名","删除"},(d,w)->{if(w==0)rename(f);else new AlertDialog.Builder(this).setMessage("确定删除 "+f.getName()+"？").setPositiveButton("删除",(x,y)->{deleteTree(f);load();}).setNegativeButton("取消",null).show();}).show();}
    private void rename(DocumentFile f){EditText e=new EditText(this);e.setSingleLine(true);e.setText(f.getName());new AlertDialog.Builder(this).setTitle("重命名").setView(e).setPositiveButton("保存",(d,w)->{String n=e.getText().toString().trim();if(!n.isEmpty()&&!f.renameTo(n))toast("重命名失败");load();}).setNegativeButton("取消",null).show();}
    private void deleteTree(DocumentFile f){if(f.isDirectory())for(DocumentFile c:f.listFiles())deleteTree(c);f.delete();}
    private void create(boolean dir){EditText e=new EditText(this);e.setSingleLine(true);e.setHint(dir?"文件夹名称（如 lua/autorun）":"文件名称（如 autorun.lua）");new AlertDialog.Builder(this).setTitle(dir?"新建文件夹":"新建文件").setView(e).setPositiveButton("创建",(d,w)->{String n=e.getText().toString().trim();if(n.isEmpty()||n.contains("/")||n.contains("\\\\")){toast("名称无效");return;}DocumentFile p=current();DocumentFile made=dir?p.createDirectory(n):p.createFile(mime(n),n);if(made==null)toast("创建失败");load();}).setNegativeButton("取消",null).show();}
    private String mime(String n){return n.toLowerCase(Locale.ROOT).endsWith(".json")?"application/json":n.toLowerCase(Locale.ROOT).endsWith(".lua")?"text/plain":"application/octet-stream";}
    private void editText(DocumentFile f){
        try{String content;try(InputStream in=getContentResolver().openInputStream(f.getUri());ByteArrayOutputStream out=new ByteArrayOutputStream()){copy(in,out);content=out.toString("UTF-8");}
            EditText e=new EditText(this);e.setText(content);e.setGravity(Gravity.TOP|Gravity.LEFT);e.setMinLines(12);e.setTypeface(Typeface.MONOSPACE);e.setTextSize(12);
            new AlertDialog.Builder(this).setTitle(f.getName()).setView(e).setPositiveButton("保存",(d,w)->{try(OutputStream out=getContentResolver().openOutputStream(f.getUri(),"wt")){out.write(e.getText().toString().getBytes("UTF-8"));toast("已保存");}catch(Exception ex){toast("保存失败："+ex.getMessage());}}).setNegativeButton("取消",null).show();
        }catch(Exception ex){toast("读取失败："+ex.getMessage());}
    }
    private void saveGma(){
        ProgressDialog progress=ProgressDialog.show(this,"正在打包","请稍候…",true,false);
        new Thread(()->{String error=null;try{
            JSONObject meta=new JSONObject();File addon=new File(workDir,"addon.json");if(addon.isFile())try(FileInputStream in=new FileInputStream(addon)){byte[] b=new byte[(int)Math.min(addon.length(),1024*1024)];int n=in.read(b);if(n>0)meta=new JSONObject(new String(b,0,n,"UTF-8"));}
            String title=meta.optString("title",stripGma(original.getName()));String desc=meta.optString("description","");String author=meta.optString("author","");
            File built=new File(getCacheDir(),"rebuilt_"+System.currentTimeMillis()+".gma");
            SourceFormats.writeGma(workDir,built,title,desc,author,meta.optLong("version",1));
            try(InputStream in=new FileInputStream(built);OutputStream out=getContentResolver().openOutputStream(original.getUri(),"wt")){copy(in,out);}
            built.delete();
        }catch(Exception e){error=e.getMessage();}
        final String err=error;runOnUiThread(()->{progress.dismiss();if(err==null){toast("GMA 已重新打包并保存");finish();}else toast("打包失败："+err);});
        }).start();
    }
    private String stripGma(String n){return n==null?"Addon":n.replaceAll("(?i)\\.gma$","");}
    private void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.flush();}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override public void onBackPressed(){if(!currentPath.isEmpty()){goUp();return;}super.onBackPressed();}
    @Override protected void onDestroy(){super.onDestroy();if(gmaMode&&workDir!=null)deleteTemp(workDir);}
    private void deleteTemp(File f){if(f.isDirectory()){File[] a=f.listFiles();if(a!=null)for(File x:a)deleteTemp(x);}f.delete();}
}
