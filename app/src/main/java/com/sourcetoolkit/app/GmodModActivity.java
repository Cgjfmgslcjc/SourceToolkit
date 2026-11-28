package com.sourcetoolkit.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.util.*;

public class GmodModActivity extends Activity {
 final int BG=0xFF0B0E10,SURFACE=0xFF12171B,LINE=0xFF232B31,BLUE=0xFF2D9CFF,TEXT=0xFFE6EAED,MUTED=0xFF9AA5AD;
 static final int PICK_GMA=51,PICK_DIR=52;
 DocumentFile addons,disabled;
 LinearLayout list;
 ArrayList<DocumentFile> items=new ArrayList<>();
 HashMap<String,Boolean> enabledByUri=new HashMap<>();
 int selected=-1;

 int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
 TextView text(String s,int z,int c,boolean b){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);if(b)t.setTypeface(Typeface.create("sans-serif-condensed",1));return t;}
 GradientDrawable shape(int c,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(c);if(stroke!=0)d.setStroke(dp(1),stroke);return d;}
 Button button(String s,int c,int tc){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setTextColor(tc);b.setAllCaps(false);b.setTypeface(Typeface.create("sans-serif-condensed",1));b.setBackground(shape(c,c==BLUE?0:0xFF38434C));return b;}

 @Override public void onCreate(Bundle b){
  super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  String s=getSharedPreferences(MainActivity.PREF,0).getString(MainActivity.GAME_URI,null);
  if(s==null){finish();return;}
  DocumentFile root=MainActivity.gameRoot(this);
  addons=folder(root,"addons");disabled=folder(root,"addons_disabled");render();
 }
 DocumentFile folder(DocumentFile p,String n){DocumentFile f=p.findFile(n);return f!=null&&f.isDirectory()?f:p.createDirectory(n);}

 void render(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(12));root.setBackgroundColor(BG);setContentView(root);
  LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
  Button add=button("+ 添加",BLUE,BG);top.addView(add,new LinearLayout.LayoutParams(dp(94),dp(42)));add.setOnClickListener(v->addDialog());
  LinearLayout title=new LinearLayout(this);title.setOrientation(1);title.setPadding(dp(12),0,0,0);title.addView(text("GMod MODS",20,TEXT,true));title.addView(text("ADDON LIBRARY",9,BLUE,false));top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
  Button manage=button("管理",SURFACE,TEXT);top.addView(manage,new LinearLayout.LayoutParams(dp(76),dp(42)));manage.setOnClickListener(v->manage());root.addView(top);
  line(root,14,10);root.addView(text("addons  /  模组列表",10,MUTED,false));
  LinearLayout hdr=new LinearLayout(this);hdr.setPadding(dp(8),dp(10),dp(8),dp(8));hdr.addView(text("图标",10,MUTED,false),new LinearLayout.LayoutParams(dp(52),-2));hdr.addView(text("模组名称",10,MUTED,false),new LinearLayout.LayoutParams(0,-2,1));hdr.addView(text("状态",10,MUTED,false));root.addView(hdr);
  ScrollView sc=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);sc.addView(list);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
  load();line(root,10,0);Button edit=button("编辑 Mod",BLUE,BG);root.addView(edit,new LinearLayout.LayoutParams(-1,dp(48)));
  edit.setOnClickListener(v->{if(selected<0||selected>=items.size()){toast("请先选择一个模组");return;}Intent i=new Intent(this,ModEditorActivity.class);i.putExtra("uri",items.get(selected).getUri().toString());startActivity(i);});
 }
 void line(LinearLayout p,int a,int b){View v=new View(this);v.setBackgroundColor(LINE);LinearLayout.LayoutParams q=new LinearLayout.LayoutParams(-1,dp(1));q.topMargin=dp(a);q.bottomMargin=dp(b);p.addView(v,q);}
 boolean isMod(DocumentFile f){String n=f.getName();return f.isDirectory()||(n!=null&&n.toLowerCase(Locale.ROOT).endsWith(".gma"));}

 void load(){
  items.clear();enabledByUri.clear();list.removeAllViews();selected=-1;
  ArrayList<DocumentFile> local=new ArrayList<>(),workshop=new ArrayList<>();
  collect(addons,true,local,workshop);collect(disabled,false,local,workshop);
  addSection("本地模组",local);addSection("创意工坊模组",workshop);
  if(items.isEmpty()){TextView e=text("暂无模组。点击左上角“添加”导入 GMA 或文件夹。",12,MUTED,false);e.setPadding(dp(12),dp(20),dp(12),dp(20));list.addView(e);}
 }
 void collect(DocumentFile parent,boolean active,ArrayList<DocumentFile> local,ArrayList<DocumentFile> workshop){
  if(parent==null)return;
  for(DocumentFile f:parent.listFiles())if(isMod(f)){
   if(workshopId(f).isEmpty())local.add(f);else workshop.add(f);
   // Enabled state is derived from which root folder contains this addon.
   enabledByUri.put(f.getUri().toString(),active);
  }
 }
 void addSection(String label,ArrayList<DocumentFile> files){
  if(files.isEmpty())return;
  TextView heading=text(label+"  ·  "+files.size(),11,BLUE,true);heading.setPadding(dp(8),dp(14),dp(8),dp(7));list.addView(heading);
  for(DocumentFile f:files)addRow(f,enabledByUri.get(f.getUri().toString())!=null&&enabledByUri.get(f.getUri().toString()));
 }
 void addRow(DocumentFile f,boolean active){
  items.add(f);
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(8),dp(8),dp(8),dp(8));row.setBackgroundColor(SURFACE);
  ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);
  GradientDrawable fallback=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF17334A,0xFF101A22});fallback.setStroke(dp(1),0xFF38434C);image.setImageDrawable(fallback);
  loadIconAsync(f,image);
  row.addView(image,new LinearLayout.LayoutParams(dp(42),dp(42)));
  LinearLayout name=new LinearLayout(this);name.setOrientation(1);name.setPadding(dp(10),0,0,0);
  String title=title(f),id=workshopId(f);
  name.addView(text(id.isEmpty()?title:title+"  ·  "+id,14,TEXT,true));
  String desc=description(f);name.addView(text(desc.isEmpty()?(f.isDirectory()?"文件夹模组":"GMA 归档"):desc,10,MUTED,false));
  row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
  row.addView(text(active?"已启用":"已禁用",10,active?0xFF73D5AA:0xFFFFB36B,false));
  int index=items.size()-1;row.setOnClickListener(v->{selected=index;for(int i=0;i<list.getChildCount();i++){View child=list.getChildAt(i);if(child.getTag()!=null)child.setBackgroundColor(child==row?0xFF1B2A36:SURFACE);}row.setBackgroundColor(0xFF1B2A36);});
  row.setTag("addon-row");list.addView(row);line(list,0,0);
 }


 String title(DocumentFile f){try{JSONObject o=metadata(f);String t=first(o,"title","name","addonTitle","addon_name");if(t.isEmpty()&&o.opt("addon") instanceof JSONObject)t=first(o.optJSONObject("addon"),"title","name");if(!t.isEmpty())return t;}catch(Exception ignored){}String n=f.getName();return n==null?"Unknown":n.replaceAll("(?i)\\.gma$","");}
 String description(DocumentFile f){try{return first(metadata(f),"description","desc","summary");}catch(Exception e){return "";}}
 String workshopId(DocumentFile f){
  try{JSONObject o=metadata(f);String id=first(o,"id","workshop_id","workshopId","publishedfileid");if(id.matches("[0-9]{5,}"))return id;}catch(Exception ignored){}
  String n=f.getName();return n!=null&&n.matches("[0-9]{5,}")?n:"";
 }
 String previewUrl(DocumentFile f){try{String u=first(metadata(f),"preview","preview_url","previewUrl");return u.startsWith("https://")?u:"";}catch(Exception e){return "";}}
 String first(JSONObject o,String... keys){for(String k:keys){String v=o.optString(k,"").trim();if(!v.isEmpty()&&!v.equals("null"))return v;}return "";}

 JSONObject metadata(DocumentFile f)throws Exception{
  if(f.isDirectory()){
   DocumentFile w=findIgnoreCase(f,"workshop.json");if(w!=null){JSONObject o=parseJson(w);if(o.length()>0)return o;}
   DocumentFile a=findIgnoreCase(f,"addon.json");if(a!=null)return parseJson(a);
   return new JSONObject();
  }
  File tmp=cacheGma(f);JSONObject w=null,a=null;
  for(SourceFormats.Entry e:SourceFormats.readGma(tmp)){
   if(e.name.equalsIgnoreCase("workshop.json"))w=new JSONObject(new String(entryBytes(tmp,e),"UTF-8"));
   else if(e.name.equalsIgnoreCase("addon.json"))a=new JSONObject(new String(entryBytes(tmp,e),"UTF-8"));
  }
  if(w!=null&&w.length()>0)return w;return a==null?new JSONObject():a;
 }
 DocumentFile findIgnoreCase(DocumentFile dir,String name){for(DocumentFile f:dir.listFiles())if(f.getName()!=null&&f.getName().equalsIgnoreCase(name))return f;return null;}
 JSONObject parseJson(DocumentFile f)throws Exception{return new JSONObject(read(f));}
 File cacheGma(DocumentFile f)throws Exception{
  File out=new File(getCacheDir(),"gma_"+f.getName().hashCode()+".gma");
  if(!out.exists()||out.length()!=f.length()){InputStream in=getContentResolver().openInputStream(f.getUri());FileOutputStream o=new FileOutputStream(out);byte[] b=new byte[16384];int n;while((n=in.read(b))>0)o.write(b,0,n);o.close();in.close();}
  return out;
 }
 byte[] entryBytes(File f,SourceFormats.Entry e)throws Exception{if(e.size<0||e.size>8*1024*1024)return new byte[0];RandomAccessFile r=new RandomAccessFile(f,"r");try{r.seek(e.offset);byte[] b=new byte[(int)e.size];r.readFully(b);return b;}finally{r.close();}}

 void loadIconAsync(DocumentFile f,ImageView target){
  target.setTag(f.getUri().toString());
  new Thread(()->{try{
   byte[] vtf=null;Bitmap still=null;
   if(f.isDirectory()){
    for(String n:new String[]{"icon.png","icon.jpg","addon.jpg","thumb.jpg"}){DocumentFile x=findIgnoreCase(f,n);if(x!=null){try(InputStream in=getContentResolver().openInputStream(x.getUri())){still=BitmapFactory.decodeStream(in);}if(still!=null)break;}}
    if(still==null){DocumentFile x=findIgnoreCase(f,"icon.vtf");if(x!=null)try(InputStream in=getContentResolver().openInputStream(x.getUri());ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] z=new byte[8192];int n;while((n=in.read(z))!=-1)o.write(z,0,n);vtf=o.toByteArray();}}
   }else{
    File file=cacheGma(f);
    for(SourceFormats.Entry e:SourceFormats.readGma(file)){String n=e.name.toLowerCase(Locale.ROOT);if(n.equals("icon.png")||n.equals("icon.jpg")||n.equals("addon.jpg")||n.equals("thumb.jpg")){byte[] z=entryBytes(file,e);still=BitmapFactory.decodeByteArray(z,0,z.length);if(still!=null)break;}if(n.equals("icon.vtf")||n.equals("materials/icon.vtf"))vtf=entryBytes(file,e);}
   }
   Drawable drawable=null;
   if(vtf!=null){int count=Math.min(VtfDecoder.frameCount(vtf),24);AnimationDrawable animation=new AnimationDrawable();for(int i=0;i<count;i++){Bitmap decoded=VtfDecoder.decode(vtf,i);Bitmap frame=Bitmap.createScaledBitmap(decoded,64,64,true);if(frame!=decoded)decoded.recycle();animation.addFrame(new BitmapDrawable(getResources(),frame),100);}animation.setOneShot(false);drawable=animation;}
   else if(still!=null)drawable=new BitmapDrawable(getResources(),still);
   if(drawable!=null){Drawable result=drawable;runOnUiThread(()->{if(f.getUri().toString().equals(target.getTag())){target.setImageDrawable(result);if(result instanceof AnimationDrawable)((AnimationDrawable)result).start();}});}
   else loadPreviewAsync(f,target);
  }catch(Exception ignored){loadPreviewAsync(f,target);}}).start();
 }
 void loadPreviewAsync(DocumentFile f,ImageView target){
  String url=previewUrl(f);if(url.isEmpty())return;target.setTag(url);
  new Thread(()->{HttpURLConnection c=null;try{
   c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(7000);c.setReadTimeout(10000);c.setInstanceFollowRedirects(true);c.connect();
   Bitmap b=BitmapFactory.decodeStream(c.getInputStream());if(b!=null)runOnUiThread(()->{if(url.equals(target.getTag()))target.setImageBitmap(b);});
  }catch(Exception ignored){}finally{if(c!=null)c.disconnect();}}).start();
 }

 String read(DocumentFile f)throws Exception{InputStream in=getContentResolver().openInputStream(f.getUri());ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))>0)o.write(b,0,n);in.close();return o.toString("UTF-8");}
 void addDialog(){new AlertDialog.Builder(this).setTitle("添加模组").setItems(new String[]{"导入 GMA 文件","导入模组文件夹","新建空白模组"},(d,w)->{if(w==0){Intent i=new Intent(this,GameFolderPickerActivity.class);i.putExtra("mode","file");i.putExtra("extensions","gma");startActivityForResult(i,PICK_GMA);}else if(w==1){Intent i=new Intent(this,GameFolderPickerActivity.class);i.putExtra("mode","folder");i.putExtra("start_path","");startActivityForResult(i,PICK_DIR);}else create();}).show();}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;try{
  if(r==PICK_GMA){Uri u=Uri.parse(d.getStringExtra("selected_uri"));DocumentFile src=docFromUri(u);DocumentFile dst=addons.createFile("application/octet-stream",d.getStringExtra("selected_name"));copy(src,dst);}
  else if(r==PICK_DIR){String path=d.getStringExtra("folder_path");DocumentFile src=MainActivity.gameRoot(this);if(path!=null&&!path.isEmpty())for(String p:path.split("/"))src=src.findFile(p);if(src==null||!src.isDirectory())throw new IOException("所选模组文件夹无效");copyTree(src,addons,src.getName());}
  load();
 }catch(Exception e){toast("导入失败："+e.getMessage());}}
 DocumentFile docFromUri(Uri u){return "file".equalsIgnoreCase(u.getScheme())?DocumentFile.fromFile(new File(u.getPath())):DocumentFile.fromSingleUri(this,u);}
 void create(){EditText e=new EditText(this);e.setHint("模组名称");new AlertDialog.Builder(this).setTitle("新建模组").setView(e).setPositiveButton("创建",(d,w)->{String n=e.getText().toString().trim();if(n.isEmpty())return;DocumentFile f=addons.createDirectory(n);try{DocumentFile j=f.createFile("application/json","addon.json");OutputStream o=getContentResolver().openOutputStream(j.getUri());o.write(("{\"title\":\""+n+"\"}").getBytes("UTF-8"));o.close();}catch(Exception ignored){}load();}).setNegativeButton("取消",null).show();}

 void manage(){new AlertDialog.Builder(this).setTitle("模组管理").setItems(new String[]{"刷新列表","启用/禁用所选模组","删除所选模组"},(d,w)->{if(w==0)load();else if(selected<0||selected>=items.size())toast("请先选择模组");else if(w==1)toggleSelected();else deleteSelected();}).show();}
 void toggleSelected(){
  try{DocumentFile f=items.get(selected);boolean active=enabledByUri.get(f.getUri().toString())!=null&&enabledByUri.get(f.getUri().toString());move(f,active?disabled:addons,f.getName());load();toast(active?"已禁用模组":"已启用模组");}
  catch(Exception e){toast("状态切换失败："+e.getMessage());}
 }
 void deleteSelected(){DocumentFile f=items.get(selected);new AlertDialog.Builder(this).setMessage("确定删除 "+title(f)+"？").setPositiveButton("删除",(d,w)->{delete(f);load();}).setNegativeButton("取消",null).show();}
 void move(DocumentFile src,DocumentFile dst,String n)throws Exception{
  if(src.isDirectory()){DocumentFile d=dst.createDirectory(n);for(DocumentFile c:src.listFiles())move(c,d,c.getName());src.delete();}
  else{DocumentFile out=dst.createFile("application/octet-stream",n);copy(src,out);src.delete();}
 }
 void copy(DocumentFile s,DocumentFile d)throws Exception{InputStream in=getContentResolver().openInputStream(s.getUri());OutputStream out=getContentResolver().openOutputStream(d.getUri());byte[] b=new byte[16384];int n;while((n=in.read(b))>0)out.write(b,0,n);in.close();out.close();}
 void copyTree(DocumentFile s,DocumentFile d,String n)throws Exception{if(s.isDirectory()){DocumentFile x=d.createDirectory(n);for(DocumentFile c:s.listFiles())copyTree(c,x,c.getName());}else copy(s,d.createFile("application/octet-stream",n));}
 void delete(DocumentFile f){if(f.isDirectory())for(DocumentFile c:f.listFiles())delete(c);f.delete();}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
