package com.sourcetoolkit.app;

import java.io.*;
import java.util.*;

/** Bounds-checked readers for Source engine GMA and VPK containers. */
public final class SourceFormats {
    private SourceFormats() {}

    public static final class Entry {
        public final String name;
        /** Offset in the GMA payload or the selected VPK archive. */
        public final long offset, size, crc;
        /** VPK archive index; 0x7fff means data is stored in the _dir.vpk itself. */
        public final int archiveIndex;
        /** Bytes stored before the entry payload in a VPK tree record. */
        public final byte[] preload;
        /** Absolute payload offset for inline VPK entries; -1 for external archives/GMA. */
        public final long dataOffset;

        Entry(String n, long o, long s, long c, int a, byte[] p, long d) {
            name=n; offset=o; size=s; crc=c; archiveIndex=a; preload=p; dataOffset=d;
        }
    }

    private static int u8(RandomAccessFile f) throws IOException { return f.readUnsignedByte(); }
    private static int u16(RandomAccessFile f) throws IOException { return u8(f) | (u8(f) << 8); }
    private static long u32(RandomAccessFile f) throws IOException { return (long)u16(f) | ((long)u16(f) << 16); }
    private static long i64(RandomAccessFile f) throws IOException { long lo=u32(f), hi=u32(f); return lo | (hi << 32); }

    private static String cstr(RandomAccessFile f) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        while (true) {
            int x=f.read();
            if (x < 0) throw new EOFException("Unterminated string in archive");
            if (x == 0) break;
            b.write(x);
            if (b.size() > 1024 * 1024) throw new IOException("Archive string is too long");
        }
        return b.toString("UTF-8");
    }

    private static void requireRange(long offset, long length, long fileLength, String what) throws IOException {
        if (offset < 0 || length < 0 || offset > fileLength || length > fileLength - offset)
            throw new IOException("Invalid " + what + " range");
    }

    public static List<Entry> readGma(File file) throws IOException {
        ArrayList<Entry> out = new ArrayList<>();
        try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
            byte[] magic = new byte[4]; f.readFully(magic);
            if (!Arrays.equals(magic, new byte[]{'G','M','A','D'})) throw new IOException("Not a GMA file");
            int version=u8(f);
            if (version < 1 || version > 3) throw new IOException("Unsupported GMA version: " + version);
            i64(f); // SteamID
            i64(f); // timestamp
            while (!cstr(f).isEmpty()) { /* required content list */ }
            cstr(f); // addon name
            cstr(f); // addon description
            cstr(f); // addon author
            u32(f);   // addon version

            ArrayList<String> names = new ArrayList<>();
            ArrayList<Long> sizes = new ArrayList<>(), crcs = new ArrayList<>();
            while (true) {
                long id=i64(f);
                if (id == 0) break;
                String name=cstr(f);
                long size=i64(f);
                long crc=u32(f); // GMA stores CRC32 as a 32-bit value, not 64-bit.
                if (size < 0) throw new IOException("Negative GMA entry size: " + name);
                names.add(name); sizes.add(size); crcs.add(crc);
                if (names.size() > 1000000) throw new IOException("Too many GMA entries");
            }
            long offset=f.getFilePointer(), length=f.length();
            for (int i=0; i<names.size(); i++) {
                long size=sizes.get(i);
                requireRange(offset,size,length,"GMA entry " + names.get(i));
                out.add(new Entry(names.get(i),offset,size,crcs.get(i),-1,new byte[0],-1));
                offset += size;
            }
        }
        return out;
    }

    public static List<Entry> readVpk(File file) throws IOException {
        ArrayList<Entry> out = new ArrayList<>();
        try (RandomAccessFile f = new RandomAccessFile(file, "r")) {
            if (u32(f) != 0x55AA1234L) throw new IOException("Not a VPK file");
            long version=u32(f), treeSize=u32(f);
            long fileDataSectionSize=0;
            if (version == 2) {
                fileDataSectionSize=u32(f);
                u32(f); // archive MD5 section size
                u32(f); // other MD5 section size
                u32(f); // signature section size
            } else if (version != 1) {
                throw new IOException("Unsupported VPK version: " + version);
            }
            long treeStart=f.getFilePointer(), treeEnd=treeStart+treeSize;
            requireRange(treeStart,treeSize,f.length(),"VPK tree");
            while (f.getFilePointer() < treeEnd) {
                String ext=cstr(f);
                if (ext.isEmpty()) break;
                while (true) {
                    String path=cstr(f);
                    if (path.isEmpty()) break;
                    while (true) {
                        String name=cstr(f);
                        if (name.isEmpty()) break;
                        long crc=u32(f);
                        int preloadSize=u16(f), archive=u16(f);
                        long offset=u32(f), length=u32(f);
                        if (u16(f) != 0xFFFF) throw new IOException("Invalid VPK entry terminator");
                        byte[] preload=new byte[preloadSize]; f.readFully(preload);
                        String full=(path.equals(" ")?"":path+"/")+name+(ext.equals(" ")?"":"."+ext);
                        long dataOffset=archive==0x7fff ? treeEnd+offset : -1;
                        if (archive==0x7fff) requireRange(dataOffset,length,version==2?treeEnd+fileDataSectionSize:f.length(),"VPK inline entry " + full);
                        out.add(new Entry(full,offset,length,crc,archive,preload,dataOffset));
                    }
                }
            }
            if (f.getFilePointer() > treeEnd) throw new IOException("VPK tree records exceed declared tree size");
        }
        return out;
    }

    /** Reads a GMA entry payload. */
    public static byte[] readGmaEntry(File gma, Entry entry) throws IOException {
        if (entry.archiveIndex != -1) throw new IOException("Entry is not from a GMA");
        if (entry.size > Integer.MAX_VALUE) throw new IOException("Entry is too large for memory");
        byte[] data=new byte[(int)entry.size];
        try (RandomAccessFile f=new RandomAccessFile(gma,"r")) {
            requireRange(entry.offset,entry.size,f.length(),"GMA entry");
            f.seek(entry.offset); f.readFully(data);
        }
        return data;
    }

    /** Reads a VPK entry, including its preload bytes and payload. file must be the _dir.vpk. */
    public static byte[] readVpkEntry(File dirVpk, Entry entry) throws IOException {
        if (entry.archiveIndex < 0) throw new IOException("Entry is not from a VPK");
        if (entry.size + entry.preload.length > Integer.MAX_VALUE) throw new IOException("Entry is too large for memory");
        File archive=dirVpk;
        if (entry.archiveIndex != 0x7fff) {
            String n=dirVpk.getName();
            String base=n.toLowerCase(Locale.ROOT).endsWith("_dir.vpk") ? n.substring(0,n.length()-8) : n;
            archive=new File(dirVpk.getParentFile(),base+String.format(Locale.ROOT,"_%03d.vpk",entry.archiveIndex));
        }
        byte[] result=new byte[entry.preload.length+(int)entry.size];
        System.arraycopy(entry.preload,0,result,0,entry.preload.length);
        try (RandomAccessFile f=new RandomAccessFile(archive,"r")) {
            long payloadOffset=entry.archiveIndex==0x7fff ? entry.dataOffset : entry.offset;
            requireRange(payloadOffset,entry.size,f.length(),"VPK entry " + entry.name);
            f.seek(payloadOffset); f.readFully(result,entry.preload.length,(int)entry.size);
        }
        return result;
    }
}
