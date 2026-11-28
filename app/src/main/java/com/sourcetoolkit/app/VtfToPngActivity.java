package com.sourcetoolkit.app;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import java.io.*;import androidx.documentfile.provider.DocumentFile;

public class VtfToPngActivity extends Activity {
    static final int PICK_VTF=91,SAVE_PNG=92;
    final int BG=0xFF0B0E10,TEXT=0xFFE6EAED,MUTED=0xFF9AA5AD;
    byte[] source;
    Bitmap preview;
    int frameCount=1,currentFrame=0;
    ImageView image;
    TextView info;
    Button prev,next,save;
    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setAllCaps(false);b.setTextColor(TEXT);b.setBackgroundColor(0xFF1B2227);return b;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(18),dp(18),dp(18),dp(16));root.setBackgroundColor(BG);setContentView(root);
        root.addView(text("VTF → PNG",20,TEXT));
        TextView desc=text("读取 VTF 纹理，预览动画帧并导出当前帧为 PNG。",11,MUTED);
        LinearLayout.LayoutParams d=new LinearLayout.LayoutParams(-1,-2);d.topMargin=dp(8);root.addView(desc,d);
        image=new ImageView(this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,0,1);ip.topMargin=dp(12);ip.bottomMargin=dp(8);root.addView(image,ip);
        info=text("请选择一个 VTF 文件",12,MUTED);info.setGravity(Gravity.CENTER);root.addView(info);
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER);nav.setPadding(0,dp(10),0,dp(10));
        prev=button("上一帧");next=button("下一帧");
        nav.addView(prev,new LinearLayout.LayoutParams(0,dp(42),1));nav.addView(next,new LinearLayout.LayoutParams(0,dp(42),1));root.addView(nav);
        Button pick=button("选择 VTF 文件");root.addView(pick,new LinearLayout.LayoutParams(-1,dp(48)));
        save=button("导出当前帧 PNG");LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(48));sp.topMargin=dp(10);root.addView(save,sp);
        pick.setOnClickListener(v->pickVtf());prev.setOnClickListener(v->showFrame(currentFrame-1));next.setOnClickListener(v->showFrame(currentFrame+1));save.setOnClickListener(v->savePng());
        updateUi();
    }
    void pickVtf(){Intent i=new Intent(this,GameFolderPickerActivity.class);i.putExtra("mode","file");i.putExtra("extensions","vtf");i.putExtra("start_path","");startActivityForResult(i,PICK_VTF);}
    byte[] readAll(Uri uri)throws IOException{
        try(InputStream in=("file".equalsIgnoreCase(uri.getScheme())?new FileInputStream(new File(uri.getPath())):getContentResolver().openInputStream(uri));ByteArrayOutputStream out=new ByteArrayOutputStream()){
            if(in==null)throw new IOException("无法打开文件");
            byte[] buf=new byte[32768];int n;long total=0;
            while((n=in.read(buf))!=-1){total+=n;if(total>256L*1024*1024)throw new IOException("VTF 文件超过 256 MB 限制");out.write(buf,0,n);}
            return out.toByteArray();
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null)return;
        try{
            if(request==PICK_VTF){
                String selected=data.getStringExtra("selected_uri");if(selected==null)throw new IOException("没有选择 VTF 文件");
                byte[] bytes=readAll(Uri.parse(selected));
                Bitmap first=VtfDecoder.decode(bytes,0);
                if(preview!=null&&!preview.isRecycled())preview.recycle();
                source=bytes;preview=first;currentFrame=0;
                frameCount=1;
                // VTF frame count is stored as an unsigned 16-bit value at byte offset 24.
                if(bytes.length>=26)frameCount=(bytes[24]&255)|((bytes[25]&255)<<8);
                updateUi();
            }else if(request==SAVE_PNG&&preview!=null){
                String path=data.getStringExtra("folder_path");DocumentFile dir=MainActivity.gameRoot(this);
                if(path!=null&&!path.isEmpty())for(String part:path.split("/"))dir=dir.findFile(part);
                if(dir==null||!dir.isDirectory())throw new IOException("导出目录无效");
                DocumentFile outFile=dir.createFile("image/png","texture_frame_"+(currentFrame+1)+".png");
                if(outFile==null)throw new IOException("无法创建 PNG 文件");
                try(OutputStream out=getContentResolver().openOutputStream(outFile.getUri())){
                    if(out==null)throw new IOException("无法写入目标文件");
                    if(!preview.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG 编码失败");
                }
                Toast.makeText(this,"PNG 导出完成",Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){Toast.makeText(this,"处理失败："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void showFrame(int index){
        if(source==null||index<0||index>=frameCount)return;
        try{
            Bitmap nextBitmap=VtfDecoder.decode(source,index);
            if(preview!=null&&!preview.isRecycled())preview.recycle();
            preview=nextBitmap;currentFrame=index;updateUi();
        }catch(Exception e){Toast.makeText(this,"无法读取该帧："+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
    void updateUi(){
        boolean has=preview!=null&&!preview.isRecycled();
        image.setImageBitmap(has?preview:null);
        info.setText(has?"帧 "+(currentFrame+1)+" / "+frameCount+"    "+preview.getWidth()+" × "+preview.getHeight():"请选择一个 VTF 文件");
        prev.setEnabled(has&&currentFrame>0);next.setEnabled(has&&currentFrame<frameCount-1);save.setEnabled(has);
        prev.setAlpha(prev.isEnabled()?1f:.4f);next.setAlpha(next.isEnabled()?1f:.4f);save.setAlpha(has?1f:.4f);
    }
    void savePng(){
        if(preview==null)return;
        Intent i=new Intent(this,GameFolderPickerActivity.class);i.putExtra("mode","folder");i.putExtra("start_path","");startActivityForResult(i,SAVE_PNG);
    }
    @Override protected void onDestroy(){super.onDestroy();if(preview!=null&&!preview.isRecycled())preview.recycle();}
}
