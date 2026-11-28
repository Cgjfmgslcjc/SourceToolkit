package com.sourcetoolkit.app;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;
import java.util.*;

public class VpkBrowserActivity extends Activity {
    static final int PICK_VPK=71, SAVE_ENTRY=72;
    final int BG=0xFF0B0E10, SURFACE=0xFF12171B, LINE=0xFF232B31, BLUE=0xFF2D9CFF, TEXT=0xFFE6EAED, MUTED=0xFF9AA5AD;
    File archiveFile;
    ArrayList<SourceFormats.Entry> entries=new ArrayList<>();
    ArrayList<SourceFormats.Entry> filtered=new ArrayList<>();
    LinearLayout list;
    EditText search;
    TextView status;
    SourceFormats.Entry selected;

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(android.graphics.Typeface.DEFAULT,1);return t;}
    android.graphics.drawable.GradientDrawable bg(int color){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(3));return d;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(16),dp(16),dp(16),dp(12));root.setBackgroundColor(BG);setContentView(root);
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("VPK BROWSER",20,TEXT,true);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button open=button("打开 VPK",BLUE,BG);top.addView(open,new LinearLayout.LayoutParams(dp(110),dp(42)));open.setOnClickListener(v->pick());
        root.addView(top);
        status=text("选择一个 *_dir.vpk 文件以浏览归档。",11,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(12);root.addView(status,sp);
        search=new EditText(this);search.setSingleLine(true);search.setTextColor(TEXT);search.setHintTextColor(MUTED);search.setHint("搜索文件名");search.setPadding(dp(10),0,dp(10),0);search.setBackground(bg(SURFACE));LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(44));ep.topMargin=dp(14);root.addView(search,ep);
        ScrollView scroll=new ScrollView(this);list=new LinearLayout(this);list.setOrientation(1);scroll.addView(list);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,0,1);lp.topMargin=dp(10);root.addView(scroll,lp);
        Button export=button("导出所选文件",BLUE,BG);LinearLayout.LayoutParams xp=new LinearLayout.LayoutParams(-1,dp(48));xp.topMargin=dp(10);root.addView(export,xp);export.setOnClickListener(v->exportSelected());
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){filter();}public void afterTextChanged(Editable e){}});
    }
    Button button(String s,int color,int tc){Button b=new Button(this);b.setText(s);b.setTextSize(13);b.setTextColor(tc);b.setAllCaps(false);b.setBackground(bg(color));return b;}
    void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_VPK);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        try{
            if(request==PICK_VPK){Uri u=data.getData();getContentResolver().takePersistableUriPermission(u,data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION);DocumentFile doc=DocumentFile.fromSingleUri(this,u);String n=doc==null?"archive.vpk":doc.getName();if(n==null||!n.toLowerCase(Locale.ROOT).endsWith(".vpk"))throw new IOException("请选择 .vpk 文件");archiveFile=copyToCache(doc);entries.clear();entries.addAll(SourceFormats.readVpk(archiveFile));status.setText(n+"  ·  "+entries.size()+" 个文件");filter();}
            else if(request==SAVE_ENTRY&&selected!=null){OutputStream out=getContentResolver().openOutputStream(data.getData());if(out==null)throw new IOException("无法创建目标文件");byte[] bytes=SourceFormats.readVpkEntry(archiveFile,selected);out.write(bytes);out.close();Toast.makeText(this,"导出完成",Toast.LENGTH_SHORT).show();}
        }catch(Exception e){Toast.makeText(this,"操作失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    File copyToCache(DocumentFile doc)throws Exception{
        File out=new File(getCacheDir(),"browse_"+System.currentTimeMillis()+".vpk");
        InputStream in=getContentResolver().openInputStream(doc.getUri());if(in==null)throw new IOException("无法读取文件");
        try(FileOutputStream os=new FileOutputStream(out)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)os.write(b,0,n);}finally{in.close();}
        return out;
    }
    void filter(){
        String q=search.getText().toString().trim().toLowerCase(Locale.ROOT);filtered.clear();for(SourceFormats.Entry e:entries)if(q.isEmpty()||e.name.toLowerCase(Locale.ROOT).contains(q))filtered.add(e);
        list.removeAllViews();if(filtered.isEmpty()){TextView empty=text(entries.isEmpty()?"尚未载入 VPK。":"没有匹配的文件。",12,MUTED,false);empty.setPadding(dp(8),dp(18),dp(8),dp(18));list.addView(empty);return;}
        for(SourceFormats.Entry e:filtered){LinearLayout row=new LinearLayout(this);row.setOrientation(1);row.setPadding(dp(9),dp(10),dp(9),dp(10));row.setBackgroundColor(e==selected?0xFF1B2A36:SURFACE);row.addView(text(e.name,12,TEXT,false));row.addView(text(String.format(Locale.ROOT,"%.1f KB  ·  archive %s",e.size/1024.0,e.archiveIndex==0x7fff?"inline":Integer.toString(e.archiveIndex)),10,MUTED,false));row.setOnClickListener(v->{selected=e;filter();});list.addView(row);View line=new View(this);line.setBackgroundColor(LINE);list.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));}
    }
    void exportSelected(){
        if(selected==null||archiveFile==null){Toast.makeText(this,"请先选择一个文件",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");String name=selected.name.substring(selected.name.lastIndexOf('/')+1);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,SAVE_ENTRY);
    }
}
