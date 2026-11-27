package com.sourcetoolkit.app;

import java.io.*;
import java.util.*;

/** Binary index readers for Source engine GMA and VPK containers. */
public final class SourceFormats {
    private SourceFormats() {}
    public static final class Entry {
        public String name; public long offset, size, crc; public int archiveIndex; public byte[] preload;
        Entry(String n,long o,long s,long c,int a,byte[] p){name=n;offset=o;size=s;crc=c;archiveIndex=a;preload=p;}
    }
    private static int u8(RandomAccessFile f)throws IOException{return f.readUnsignedByte();}
    private static int u16(RandomAccessFile f)throws IOException{return u8(f)|(u8(f)<<8);}
    private static long u32(RandomAccessFile f)throws IOException{return (long)u16(f)|((long)u16(f)<<16);}
    private static long i64(RandomAccessFile f)throws IOException{long lo=u32(f),hi=u32(f);return lo|(hi<<32);}
    private static String cstr(RandomAccessFile f)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();int x;while((x=f.read())>0)b.write(x);return b.toString("UTF-8");}
    public static List<Entry> readVpk(File file)throws IOException{
        ArrayList<Entry> out=new ArrayList<>();try(RandomAccessFile f=new RandomAccessFile(file,"r")){
            if(u32(f)!=0x55AA1234L)throw new IOException("Not a VPK file");long version=u32(f),treeSize=u32(f);
            if(version==2){u32(f);u32(f);u32(f);u32(f);}else if(version!=1)throw new IOException("Unsupported VPK version: "+version);
            long treeEnd=(version==1?12:28)+treeSize;
            while(f.getFilePointer()<treeEnd){String ext=cstr(f);if(ext.isEmpty())break;while(true){String path=cstr(f);if(path.isEmpty())break;while(true){String name=cstr(f);if(name.isEmpty())break;long crc=u32(f);int preloadSize=u16(f),archive=u16(f);long offset=u32(f),length=u32(f);if(u16(f)!=0xFFFF)throw new IOException("Invalid VPK entry terminator");byte[] preload=new byte[preloadSize];f.readFully(preload);String full=(path.equals(" ")?"":path+"/")+name+(ext.equals(" ")?"":"."+ext);out.add(new Entry(full,offset,length,crc,archive,preload));}}}
        }return out;
    }
    public static List<Entry> readGma(File file)throws IOException{
        ArrayList<Entry> out=new ArrayList<>();try(RandomAccessFile f=new RandomAccessFile(file,"r")){
            byte[] magic=new byte[4];f.readFully(magic);if(!Arrays.equals(magic,new byte[]{'G','M','A','D'}))throw new IOException("Not a GMA file");int version=u8(f);if(version<1)throw new IOException("Unsupported GMA version");i64(f);i64(f);while(!cstr(f).isEmpty()){} cstr(f);cstr(f);cstr(f);u32(f);
            ArrayList<String> names=new ArrayList<>();ArrayList<Long> sizes=new ArrayList<>(),crcs=new ArrayList<>();while(true){long id=i64(f);if(id==0)break;String name=cstr(f);long size=i64(f),crc=i64(f);names.add(name);sizes.add(size);crcs.add(crc);}
            long offset=f.getFilePointer();for(int i=0;i<names.size();i++){out.add(new Entry(names.get(i),offset,sizes.get(i),crcs.get(i),-1,new byte[0]));offset+=sizes.get(i);}
        }return out;
    }
}
