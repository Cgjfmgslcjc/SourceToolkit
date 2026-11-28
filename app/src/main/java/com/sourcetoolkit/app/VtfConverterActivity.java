package com.sourcetoolkit.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import androidx.documentfile.provider.DocumentFile;
import java.io.*;
import java.nio.*;
import java.util.*;

public class VtfConverterActivity extends Activity {
    static final int PICK_IMAGE=81, SAVE_VTF=82, PICK_EXPORT_FOLDER=83;
    static final int MAX_FRAMES=65535;
    final int BG=0xFF0B0E10, TEXT=0xFFE6EAED, MUTED=0xFF9AA5AD, BLUE=0xFF2D9CFF;
    final ArrayList<Bitmap> frames=new ArrayList<>();
    int currentFrame=0;
    String exportName="texture";
    int exportMode=0;
    TextView info;
    ImageView previewView;
    Button previousButton,nextButton,removeButton;
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(16));root.setBackgroundColor(BG);setContentView(root);
        root.addView(text("VTF CONVERTER",20,TEXT));
        TextView desc=text("将一张或多张同尺寸 PNG / JPG 编码为 VTF 7.2 动画帧（RGBA8888）。",11,MUTED);
        LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(-1,-2);dp1.topMargin=dp(8);root.addView(desc,dp1);
        previewView=new ImageView(this);previewView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1);pp.topMargin=dp(12);pp.bottomMargin=dp(8);root.addView(previewView,pp);
        info=text("尚未选择图片",12,MUTED);info.setGravity(Gravity.CENTER);root.addView(info);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(0,dp(8),0,dp(8));
        previousButton=button("上一帧");nextButton=button("下一帧");removeButton=button("移除当前帧");
        nav.addView(previousButton,new LinearLayout.LayoutParams(0,dp(40),1));
        nav.addView(nextButton,new LinearLayout.LayoutParams(0,dp(40),1));
        nav.addView(removeButton,new LinearLayout.LayoutParams(0,dp(40),1));root.addView(nav);
        previousButton.setOnClickListener(v->showFrame(currentFrame-1));
        nextButton.setOnClickListener(v->showFrame(currentFrame+1));
        removeButton.setOnClickListener(v->removeFrame());
        Button choose=button("添加图片（可多选）");root.addView(choose,new LinearLayout.LayoutParams(-1,dp(48)));choose.setOnClickListener(v->pick());
        Button save=button("导出 VTF");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.topMargin=dp(10);root.addView(save,sp);save.setOnClickListener(v->save());
        updateUi();
    }

    Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(TEXT);b.setTextSize(12);b.setBackgroundColor(0xFF1B2227);return b;}

    void pick(){
        Intent i=new Intent(this,GameFolderPickerActivity.class);i.putExtra("mode","multi");i.putExtra("extensions","png,jpg,jpeg");i.putExtra("start_path","");startActivityForResult(i,PICK_IMAGE);
    }

    void updateUi(){
        boolean has=!frames.isEmpty();
        if(has){Bitmap b=frames.get(currentFrame);previewView.setImageBitmap(b);info.setText("帧 "+(currentFrame+1)+" / "+frames.size()+"    "+b.getWidth()+" × "+b.getHeight()+"    RGBA8888");}
        else{previewView.setImageDrawable(null);info.setText("尚未选择图片");}
        previousButton.setEnabled(has&&currentFrame>0);nextButton.setEnabled(has&&currentFrame<frames.size()-1);removeButton.setEnabled(has);
        previousButton.setAlpha(previousButton.isEnabled()?1f:.4f);nextButton.setAlpha(nextButton.isEnabled()?1f:.4f);removeButton.setAlpha(has?1f:.4f);
    }

    void showFrame(int index){if(index<0||index>=frames.size())return;currentFrame=index;updateUi();}
    void removeFrame(){
        if(frames.isEmpty())return;
        Bitmap removed=frames.remove(currentFrame);removed.recycle();
        if(currentFrame>=frames.size())currentFrame=Math.max(0,frames.size()-1);
        updateUi();
    }

    void save(){
        if(frames.isEmpty()){Toast.makeText(this,"请先添加图片",Toast.LENGTH_SHORT).show();return;}
        String game=getSharedPreferences(MainActivity.PREF,0).getString(MainActivity.GAME_URI,null);
        if(game==null){Toast.makeText(this,"请先在主界面设置游戏目录",Toast.LENGTH_LONG).show();return;}
        EditText name=new EditText(this);name.setSingleLine(true);name.setText(exportName);
        new AlertDialog.Builder(this).setTitle("导出文件名（不含扩展名）").setView(name)
          .setPositiveButton("下一步",(d,w)->{
            exportName=name.getText().toString().trim().replaceAll("[^A-Za-z0-9_ -]","_");
            if(exportName.isEmpty())exportName="texture";
            showExportTargets();
          }).setNegativeButton("取消",null).show();
    }
    void showExportTargets(){
        String[] labels={"导出到喷漆文件夹（materials/vgui/logos）","导出到贴图文件夹（materials）","导出到自定义文件夹"};
        new AlertDialog.Builder(this).setTitle("选择游戏内导出位置").setItems(labels,(d,which)->{
            exportMode=which;
            if(which==0)exportToGame("materials/vgui/logos");
            else if(which==1)exportToGame("materials");
            else{Intent i=new Intent(this,GameFolderPickerActivity.class);i.putExtra("mode","folder");i.putExtra("start_path","materials");startActivityForResult(i,PICK_EXPORT_FOLDER);}
        }).setNegativeButton("取消",null).show();
    }
    DocumentFile ensureFolder(DocumentFile root,String relative)throws IOException{
        DocumentFile current=root;
        if(relative.isEmpty())return current;
        for(String part:relative.split("/")){
            if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IOException("非法目录路径");
            DocumentFile next=current.findFile(part);
            if(next==null)next=current.createDirectory(part);
            if(next==null||!next.isDirectory())throw new IOException("无法创建目录："+part);
            current=next;
        }
        return current;
    }
    void writeGameFile(DocumentFile folder,String name,String mime,byte[] bytes)throws IOException{
        DocumentFile old=folder.findFile(name);
        if(old!=null&&!old.delete())throw new IOException("无法覆盖文件："+name);
        DocumentFile file=folder.createFile(mime,name);
        if(file==null)throw new IOException("无法创建文件："+name);
        try(OutputStream out=getContentResolver().openOutputStream(file.getUri())){
            if(out==null)throw new IOException("无法写入文件："+name);
            out.write(bytes);
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==PICK_EXPORT_FOLDER&&result==RESULT_OK&&data!=null){
            String folder=data.getStringExtra("folder_path");
            if(folder==null||!folder.startsWith("materials")){Toast.makeText(this,"只能导出到 materials 文件夹内",Toast.LENGTH_LONG).show();return;}
            exportToGame(folder);
            return;
        }
        if(result!=RESULT_OK||data==null)return;
        try{
            if(request==PICK_IMAGE){
                ArrayList<Uri> uris=new ArrayList<>();
                ArrayList<String> selectedUris=data.getStringArrayListExtra("selected_uris");
                if(selectedUris!=null)for(String u:selectedUris)uris.add(Uri.parse(u));
                if(uris.isEmpty())throw new IOException("没有选择图片");
                if(frames.size()+uris.size()>MAX_FRAMES)throw new IOException("VTF 帧数不能超过 65535");
                int expectedW=frames.isEmpty()?-1:frames.get(0).getWidth(),expectedH=frames.isEmpty()?-1:frames.get(0).getHeight();
                ArrayList<Bitmap> decoded=new ArrayList<>();
                for(Uri uri:uris){
                    Bitmap bitmap;try(InputStream in=openUri(uri)){bitmap=BitmapFactory.decodeStream(in);}
                    if(bitmap==null)throw new IOException("有图片无法解码");
                    if(expectedW<0){expectedW=bitmap.getWidth();expectedH=bitmap.getHeight();}
                    if(bitmap.getWidth()!=expectedW||bitmap.getHeight()!=expectedH){bitmap.recycle();for(Bitmap b:decoded)b.recycle();throw new IOException("多帧 VTF 要求所有图片尺寸一致。当前尺寸应为 "+expectedW+" × "+expectedH);}
                    decoded.add(bitmap);
                }
                frames.addAll(decoded);currentFrame=frames.size()-decoded.size();updateUi();Toast.makeText(this,"已添加 "+decoded.size()+" 帧",Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){Toast.makeText(this,"转换失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    InputStream openUri(Uri uri)throws IOException{
        if("file".equalsIgnoreCase(uri.getScheme()))return new FileInputStream(new File(uri.getPath()));
        InputStream in=getContentResolver().openInputStream(uri);if(in==null)throw new IOException("无法打开文件");return in;
    }
    void exportToGame(String relative){
        try{
            String uri=getSharedPreferences(MainActivity.PREF,0).getString(MainActivity.GAME_URI,null);
            if(uri==null)throw new IOException("尚未设置游戏目录");
            DocumentFile root=MainActivity.gameRoot(this);
            if(root==null||!root.isDirectory())throw new IOException("游戏目录授权失效，请重新选择");
            if(!relative.equals("materials")&&!relative.startsWith("materials/"))throw new IOException("导出位置必须在 materials 内");
            DocumentFile target=ensureFolder(root,relative);
            ByteArrayOutputStream vtf=new ByteArrayOutputStream();writeVtf(vtf,frames);
            writeGameFile(target,exportName+".vtf","application/octet-stream",vtf.toByteArray());
            String baseTexture=relative.equals("materials")?exportName:relative.substring("materials/".length()).replace('/','/')+"/"+exportName;
            String vmt;
            if(exportMode==0){
                vmt="\"UnlitGeneric\"\n{\n    \"$basetexture\" \"vgui/logos/"+exportName+"\"\n    \"$translucent\" \"1\"\n    \"$vertexcolor\" \"1\"\n    \"$vertexalpha\" \"1\"\n}\n";
            }else{
                vmt="\"LightmappedGeneric\"\n{\n    \"$basetexture\" \""+baseTexture+"\"\n}\n";
            }
            writeGameFile(target,exportName+".vmt","text/plain",vmt.getBytes("UTF-8"));
            Toast.makeText(this,"VTF 与配套 VMT 已导出到 garrysmod/"+relative,Toast.LENGTH_LONG).show();
        }catch(Exception e){Toast.makeText(this,"导出失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }

    static void writeVtf(OutputStream stream,List<Bitmap> images)throws IOException{
        if(images==null||images.isEmpty())throw new IOException("没有可写入的帧");
        Bitmap first=images.get(0);int w=first.getWidth(),h=first.getHeight();
        if(w<1||h<1||w>65535||h>65535)throw new IOException("图片尺寸超出 VTF 限制");
        if(images.size()>MAX_FRAMES)throw new IOException("VTF 帧数不能超过 65535");
        long frameBytes=(long)w*h*4,totalBytes=frameBytes*images.size();
        if(totalBytes>256L*1024*1024)throw new IOException("所有帧总数据超过 256 MB 限制");
        for(Bitmap image:images)if(image.getWidth()!=w||image.getHeight()!=h)throw new IOException("所有动画帧必须具有相同尺寸");
        ByteBuffer header=ByteBuffer.allocate(80).order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'V','T','F',0});
        header.putInt(7);header.putInt(2);header.putInt(80);
        header.putShort((short)w);header.putShort((short)h);
        header.putInt(0); // flags
        header.putShort((short)images.size()); // frame count
        header.putShort((short)0xFFFF); // first frame (no special first-frame index)
        header.putInt(0); // padding
        header.putFloat(0);header.putFloat(0);header.putFloat(0); // reflectivity
        header.putInt(0); // padding
        header.putFloat(1.0f); // bumpmap scale
        header.putInt(0); // IMAGE_FORMAT_RGBA8888
        header.put((byte)1); // one mip level
        header.putInt(-1); // IMAGE_FORMAT_NONE thumbnail
        header.put((byte)0);header.put((byte)0); // thumbnail dimensions
        header.putShort((short)1); // depth
        while(header.position()<80)header.put((byte)0);
        stream.write(header.array());
        int[] pixels=new int[w*h];byte[] row=new byte[w*4];
        for(Bitmap image:images){
            image.getPixels(pixels,0,w,0,0,w,h);
            for(int y=0;y<h;y++){
                for(int x=0;x<w;x++){
                    int p=pixels[y*w+x],i=x*4;
                    row[i]=(byte)((p>>16)&255);row[i+1]=(byte)((p>>8)&255);row[i+2]=(byte)(p&255);row[i+3]=(byte)((p>>>24)&255);
                }
                stream.write(row);
            }
        }
    }

    @Override protected void onDestroy(){super.onDestroy();for(Bitmap b:frames)if(!b.isRecycled())b.recycle();}
}
