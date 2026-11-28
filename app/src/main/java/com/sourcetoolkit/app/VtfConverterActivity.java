package com.sourcetoolkit.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.io.*;
import java.nio.*;

public class VtfConverterActivity extends Activity {
    static final int PICK_IMAGE=81, SAVE_VTF=82;
    final int BG=0xFF0B0E10, TEXT=0xFFE6EAED, MUTED=0xFF9AA5AD, BLUE=0xFF2D9CFF;
    Bitmap bitmap;
    TextView info;
    ImageView previewView;
    int dp(float x){return (int)(x*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(16));root.setBackgroundColor(BG);setContentView(root);
        root.addView(text("VTF CONVERTER",20,TEXT));
        TextView desc=text("将 PNG / JPG 图片编码为 VTF 7.2（RGBA8888）。",11,MUTED);LinearLayout.LayoutParams dp1=new LinearLayout.LayoutParams(-1,-2);dp1.topMargin=dp(8);root.addView(desc,dp1);
        ImageView preview=new ImageView(this);previewView=preview;preview.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,0,1);pp.topMargin=dp(16);pp.bottomMargin=dp(12);root.addView(preview,pp);
        info=text("尚未选择图片",12,MUTED);info.setGravity(Gravity.CENTER);root.addView(info);
        Button choose=button("选择图片");root.addView(choose,new LinearLayout.LayoutParams(-1,dp(48)));choose.setOnClickListener(v->pick());
        Button save=button("导出 VTF");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.topMargin=dp(10);root.addView(save,sp);save.setOnClickListener(v->save());
        preview.setTag("preview");
    }
    Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(TEXT);b.setBackgroundColor(0xFF1B2227);return b;}
    void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMAGE);}
    @Override protected void onActivityResult(int r,int c,Intent d){
        super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null)return;
        try{if(r==PICK_IMAGE){InputStream in=getContentResolver().openInputStream(d.getData());bitmap=BitmapFactory.decodeStream(in);in.close();if(bitmap==null)throw new IOException("无法解码图片");previewView.setImageBitmap(bitmap);info.setText(bitmap.getWidth()+" × "+bitmap.getHeight()+"  ·  RGBA8888");}
            else if(r==SAVE_VTF&&bitmap!=null){OutputStream out=getContentResolver().openOutputStream(d.getData());if(out==null)throw new IOException("无法写入目标文件");writeVtf(out,bitmap);out.close();Toast.makeText(this,"VTF 导出完成",Toast.LENGTH_SHORT).show();}
        }catch(Exception e){Toast.makeText(this,"转换失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void save(){
        if(bitmap==null){Toast.makeText(this,"请先选择图片",Toast.LENGTH_SHORT).show();return;}
        Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/octet-stream");i.putExtra(Intent.EXTRA_TITLE,"texture.vtf");startActivityForResult(i,SAVE_VTF);
    }
    void writeVtf(OutputStream stream,Bitmap source)throws IOException{
        int w=source.getWidth(),h=source.getHeight();
        if(w<1||h<1||w>65535||h>65535)throw new IOException("图片尺寸超出 VTF 限制");
        long bytes=(long)w*h*4;if(bytes>256L*1024*1024)throw new IOException("图片过大，超过 256 MB 输出限制");
        ByteBuffer header=ByteBuffer.allocate(80).order(ByteOrder.LITTLE_ENDIAN);
        header.put(new byte[]{'V','T','F',0});
        header.putInt(7);header.putInt(2);header.putInt(80);
        header.putShort((short)w);header.putShort((short)h);
        header.putInt(0); // flags
        header.putShort((short)1);header.putShort((short)0xFFFF);
        header.putInt(0); // padding
        header.putFloat(0);header.putFloat(0);header.putFloat(0);
        header.putInt(0); // padding
        header.putFloat(1.0f);
        header.putInt(0); // IMAGE_FORMAT_RGBA8888
        header.put((byte)1); // one mip level
        header.putInt(-1); // IMAGE_FORMAT_NONE thumbnail
        header.put((byte)0);header.put((byte)0);
        header.putShort((short)1); // depth
        while(header.position()<80)header.put((byte)0);
        stream.write(header.array());
        int[] pixels=new int[w*h];source.getPixels(pixels,0,w,0,0,w,h);
        byte[] row=new byte[w*4];
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){int p=pixels[y*w+x],i=x*4;row[i]=(byte)((p>>16)&255);row[i+1]=(byte)((p>>8)&255);row[i+2]=(byte)(p&255);row[i+3]=(byte)((p>>>24)&255);}
            stream.write(row);
        }
    }
}
