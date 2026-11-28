package com.sourcetoolkit.app;

import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.*;
import java.nio.*;
import java.util.Arrays;

/** Small VTF decoder for common addon-icon formats. */
public final class VtfDecoder {
    private VtfDecoder() {}
    public static Bitmap decode(byte[] data) throws IOException { return decode(data,0); }
    public static Bitmap decode(byte[] data,int frameIndex) throws IOException {
        if(data==null||data.length<64)throw new IOException("VTF 文件太短");
        ByteBuffer b=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if(b.get(0)!='V'||b.get(1)!='T'||b.get(2)!='F'||b.get(3)!=0)throw new IOException("无效的 VTF 文件头");
        int major=b.getInt(4),minor=b.getInt(8),headerSize=b.getInt(12);
        if(major!=7||minor<2||headerSize<64||headerSize>data.length)throw new IOException("暂不支持此 VTF 版本");
        int width=b.getShort(16)&65535,height=b.getShort(18)&65535;
        int frames=b.getShort(24)&65535;
        int format=b.getInt(52),mipCount=b.get(56)&255;
        int lowFormat=b.getInt(57),lowW=b.get(61)&255,lowH=b.get(62)&255;
        int depth=minor>=2?(b.getShort(63)&65535):1;
        if(width<1||height<1||frames<1||mipCount<1)throw new IOException("VTF 尺寸或帧数无效");
        if(frameIndex<0||frameIndex>=frames)throw new IOException("VTF 帧索引超出范围");
        long pos=headerSize;
        if(lowFormat!=-1&&lowW>0&&lowH>0)pos+=imageSize(lowW,lowH,lowFormat);
        long faces=((b.getInt(20)&0x4000)!=0)?(minor>=5?7:6):1;
        for(int mip=mipCount-1;mip>=1;mip--){
            int mw=Math.max(1,width>>mip),mh=Math.max(1,height>>mip);
            pos+=(long)imageSize(mw,mh,format)*frames*faces*depth;
        }
        pos+=(long)0; // top mip begins at pos
        int size=imageSize(width,height,format);
        pos+=(long)frameIndex*size;
        if(pos<0||pos+size>data.length)throw new IOException("VTF 图像数据不完整");
        return decodePixels(data,(int)pos,width,height,format);
    }

    private static int imageSize(int w,int h,int format)throws IOException{
        long n;
        switch(format){
            case 0: case 1: case 12: n=(long)w*h*4;break; // RGBA/ABGR/BGRA
            case 2: case 3: n=(long)w*h*3;break; // RGB/BGR
            case 13: n=(long)((w+3)/4)*((h+3)/4)*8;break; // DXT1
            case 14: case 15: n=(long)((w+3)/4)*((h+3)/4)*16;break; // DXT3/5
            default: throw new IOException("不支持的 VTF 像素格式: "+format);
        }
        if(n>Integer.MAX_VALUE)throw new IOException("VTF 图像过大");
        return (int)n;
    }

    private static Bitmap decodePixels(byte[] data,int offset,int w,int h,int format)throws IOException{
        int[] pixels=new int[w*h];
        if(format==0||format==1||format==12||format==2||format==3){
            int channels=(format==2||format==3)?3:4,p=offset;
            for(int i=0;i<pixels.length;i++){
                int a=255,r,g,bl;
                if(format==0){r=data[p++]&255;g=data[p++]&255;bl=data[p++]&255;a=data[p++]&255;}
                else if(format==12){bl=data[p++]&255;g=data[p++]&255;r=data[p++]&255;a=data[p++]&255;}
                else if(format==1){a=data[p++]&255;bl=data[p++]&255;g=data[p++]&255;r=data[p++]&255;}
                else if(format==2){r=data[p++]&255;g=data[p++]&255;bl=data[p++]&255;}
                else{bl=data[p++]&255;g=data[p++]&255;r=data[p++]&255;}
                pixels[i]=Color.argb(a,r,g,bl);
            }
        }else{
            decodeDxt(data,offset,w,h,format,pixels);
        }
        return Bitmap.createBitmap(pixels,w,h,Bitmap.Config.ARGB_8888);
    }

    private static int rgb565(int c){
        int r=(c>>11)&31,g=(c>>5)&63,b=c&31;
        return Color.rgb((r*255+15)/31,(g*255+31)/63,(b*255+15)/31);
    }
    private static void decodeDxt(byte[] d,int off,int w,int h,int fmt,int[] out)throws IOException{
        int blockBytes=fmt==13?8:16,blocksX=(w+3)/4,blocksY=(h+3)/4,p=off;
        for(int by=0;by<blocksY;by++)for(int bx=0;bx<blocksX;bx++){
            int[] alpha=new int[16];Arrays.fill(alpha,255);
            if(fmt==14){for(int i=0;i<8;i++){int v=d[p++]&255;alpha[i*2]=(v&15)*17;alpha[i*2+1]=(v>>>4)*17;}}
            else if(fmt==15){
                int a0=d[p++]&255,a1=d[p++]&255;long bits=0;for(int i=0;i<6;i++)bits|=(long)(d[p++]&255)<<(8*i);
                int[] table=new int[8];table[0]=a0;table[1]=a1;
                if(a0>a1){for(int i=1;i<=6;i++)table[i+1]=((7-i)*a0+i*a1)/7;}
                else{for(int i=1;i<=4;i++)table[i+1]=((5-i)*a0+i*a1)/5;table[6]=0;table[7]=255;}
                for(int i=0;i<16;i++)alpha[i]=table[(int)((bits>>(3*i))&7)];
            }
            int c0=(d[p]&255)|((d[p+1]&255)<<8),c1=(d[p+2]&255)|((d[p+3]&255)<<8);p+=4;
            int[] colors=new int[4];colors[0]=rgb565(c0);colors[1]=rgb565(c1);
            if(c0>c1||fmt!=13){colors[2]=mix(colors[0],colors[1],2,1,3);colors[3]=mix(colors[0],colors[1],1,2,3);}
            else{colors[2]=mix(colors[0],colors[1],1,1,2);colors[3]=Color.TRANSPARENT;}
            int indices=(d[p]&255)|((d[p+1]&255)<<8)|((d[p+2]&255)<<16)|((d[p+3]&255)<<24);p+=4;
            for(int i=0;i<16;i++){
                int x=bx*4+(i&3),y=by*4+(i>>2);if(x>=w||y>=h)continue;
                int ci=(indices>>(2*i))&3,c=colors[ci];
                if(fmt==13&&c==Color.TRANSPARENT)out[y*w+x]=c;
                else out[y*w+x]=Color.argb(alpha[i],Color.red(c),Color.green(c),Color.blue(c));
            }
        }
    }
    private static int mix(int a,int b,int aw,int bw,int div){
        return Color.rgb((Color.red(a)*aw+Color.red(b)*bw)/div,(Color.green(a)*aw+Color.green(b)*bw)/div,(Color.blue(a)*aw+Color.blue(b)*bw)/div);
    }
}
