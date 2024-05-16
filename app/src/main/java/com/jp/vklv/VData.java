package com.jp.vklv;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import androidx.datastore.preferences.core.MutablePreferences;
import androidx.datastore.preferences.core.Preferences;
import androidx.datastore.preferences.core.PreferencesKeys;
import androidx.datastore.preferences.rxjava2.RxPreferenceDataStoreBuilder;
import androidx.datastore.rxjava2.RxDataStore;
import io.reactivex.Flowable;
import io.reactivex.Single;

public class VData {
    static String TAG = "VK+LV:VData";
    static char[] buffer = new char[8192];
    static public String is2string(InputStream is) {
        StringBuilder sb = new StringBuilder();
        try {
            Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            int n;
            while ((n = reader.read(buffer)) != -1) sb.append(buffer, 0, n);
        } catch(Exception e) {
            return "";
        } finally {
            try { is.close(); } catch (Exception e) { }
        }
        return sb.toString();
    }


    static public class VDBE {
        String no;
        String url;
        String name;
        String hd="";
        int a;
        int b;
        boolean favourite = false;
        boolean cached = false;
        public String toString() {
            if (hd.length()==0) hd = no+" "+name;
            return hd;
        }
        VDBE(byte[] s, int i0, int i1, int i2, int i3, int a, int b) {
            no = new String(s, i0, i1-i0-1, Charset.defaultCharset());
            url = new String(s, i1, i2-i1-1, Charset.defaultCharset());
            name = new String(s, i2, i3-i2-1, Charset.defaultCharset());
            this.a = a;
            this.b = b;
        }
    }
    public static ArrayList<VDBE> vdb = new ArrayList<>();
    static String getWebArchiveFileName(Context context, String id) {
        return context.getCacheDir() + "/" + id + ".mht";
    }
    static String favouritesFileName(Context context) {return context.getFilesDir()+"/favourites"; }
    static String cacheInfoFileName(Context context) {return context.getCacheDir()+"/cached"; }

    static int checkCached(Context context) {
        long  start = System.currentTimeMillis();
        File[] files = context.getCacheDir().listFiles();
        HashSet<String> fln = new HashSet<>();
        for (int i=0; i<files.length; ++i) {
            if (files[i].length() == 0) {
                files[i].delete();
                continue;
            }
            fln.add(files[i].getName());
        }
        int mismatches = 0;
        int cached = 0;
        for (int i=0; i<VData.vdb.size(); ++i) {
            boolean isCached = fln.contains(vdb.get(i).no + ".mht");
            if (VData.vdb.get(i).cached != isCached) ++mismatches;
            VData.vdb.get(i).cached = isCached;
            if (isCached) ++cached;
        }
        Log.d(TAG, "cached files checked in "+((System.currentTimeMillis()-start)/1000.0)+" seconds, "+cached+" cached files, "+mismatches+" mismatches");
        return mismatches;
    }

    static RandomAccessFile getCacheInfoFileRAF(Context context) {
        try {
            return new RandomAccessFile(cacheInfoFileName(context), "rw");
        } catch (Exception e) {
            return null;
        }
    }
    static RandomAccessFile getFavouritesFileRAF(Context context) {
        try {
            return new RandomAccessFile(favouritesFileName(context), "rw");
        } catch (Exception e) {
            return null;
        }
    }
    static int clearCached(Context context) {
        long  start = System.currentTimeMillis();
        File[] files = context.getCacheDir().listFiles();
        HashMap<String, Integer> fln = new HashMap<>();
        for (int i=0; i<files.length; ++i) {
            fln.put(files[i].getName(), i);
        }
        int deleted = 0;
        for (int i=0; i<VData.vdb.size(); ++i) {
            if (fln.containsKey(vdb.get(i).no + ".mht")) {
                File f = files[fln.get(vdb.get(i).no + ".mht")];
                f.delete();
                deleted++;
            }
            VData.vdb.get(i).cached = false;
        }
        saveCacheInfo(context);
        Log.d(TAG, "cached pages cleared in "+((System.currentTimeMillis()-start)/1000.0)+" seconds, "+deleted+" files deleted");
        return deleted;
    }

    static void saveFavourites(File f) {
        byte[] data = new byte[(vdb.size()>>3) + 1];
        for (int i=0; i<vdb.size(); ++i) {
            if (vdb.get(i).favourite) {
                data[(i>>3)]|=1<<(i&7);
            }
        }
        try {
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(data);
            fo.close();
        } catch (Exception e) {
        }
    }
    static void saveFavourites(Context context) {
        File f = new File(favouritesFileName(context));
        saveFavourites(f);
    }
    static void loadFavourites(Context context) {
        long start = System.currentTimeMillis();

        File f = new File(favouritesFileName(context));
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (Exception e) {
                return;
            }
            saveFavourites(f);
        }
        byte[] data = new byte[(vdb.size()>>3) + 1];
        int favourites = 0;
        try {
            FileInputStream is = new FileInputStream(f);
            is.read(data, 0, data.length);
            is.close();
            for (int i=0; i<vdb.size(); ++i) {
                vdb.get(i).favourite = ((data[(i>>3)] & (1<<(i&7)))!=0);
                if (vdb.get(i).favourite) favourites+=1;
            }
        } catch (Exception e) {
        }
        Log.d(TAG, "favourites loaded in "+((System.currentTimeMillis()-start)/1000.0)+" seconds, "+favourites+" favourites");
    }
    static void saveCacheInfo(File f) {
        byte[] data = new byte[(vdb.size()>>3) + 1];
        for (int i=0; i<vdb.size(); ++i) {
            if (vdb.get(i).cached) {
                data[(i>>3)]|=1<<(i&7);
            }
        }
        try {
            FileOutputStream fo = new FileOutputStream(f);
            fo.write(data);
            fo.close();
        } catch (Exception e) {
        }
    }
    static void saveCacheInfo(Context context) {
        File f = new File(cacheInfoFileName(context));
        saveCacheInfo(f);
    }
    static void loadCacheInfo(Context context) {
        long start = System.currentTimeMillis();
        File f = new File(cacheInfoFileName(context));
        if (!f.exists()) {
            try {
                checkCached(context);
                f.createNewFile();
            } catch (Exception e) {
                return;
            }
            saveCacheInfo(f);
        }
        byte[] data = new byte[(vdb.size()>>3) + 1];
        int cached = 0;
        try {
            FileInputStream is = new FileInputStream(f);
            is.read(data, 0, data.length);
            is.close();
            for (int i=0; i<vdb.size(); ++i) {
                vdb.get(i).cached = ((data[(i>>3)] & (1<<(i&7)))!=0);
                if (vdb.get(i).cached) cached+=1;
            }
        } catch (Exception e) {
        }
        Log.d(TAG, "cacheInfo loaded in "+((System.currentTimeMillis()-start)/1000.0)+" seconds, "+cached+" cached");
    }

    static void setRAFBit(RandomAccessFile raf, int i, boolean value) {
        long start = System.currentTimeMillis();
        try {
            raf.seek(i >> 3);
            byte b = raf.readByte();
            if (value) b |= (1<<(i&7));
            else       b &= (0xff ^ (1 << (i & 7)));
            raf.seek(i >> 3);
            raf.writeByte(b);
            raf.close();
        } catch (Exception e) {
            Log.d(TAG, "could not update info in RAF!");
        }
        Log.d(TAG, "setRAFBit took "+(System.currentTimeMillis()-start)/1000.0 + " seconds");
    }
    static boolean uncache(Context context, int i) {
        String fn = getWebArchiveFileName(context, vdb.get(i).no);
        File f = new File(fn);
        if (f.exists()) {
            f.delete();
            vdb.get(i).cached = false;
            setRAFBit(getCacheInfoFileRAF(context), i, false);

            return true;
        }
        return false;
    }
    static void cached(Context context, int i) {
        vdb.get(i).cached = true;
        setRAFBit(getCacheInfoFileRAF(context), i, true);
    }

    static void toggleFavourite(Context context, int i) {
        long start = System.currentTimeMillis();
        boolean nb = !vdb.get(i).favourite;
        vdb.get(i).favourite = nb;
        setRAFBit(getFavouritesFileRAF(context), i, nb);
        Log.d(TAG, "toggleFavourite "+i+" took "+(System.currentTimeMillis()-start)/1000.0 + " seconds");
    }
    static public class VDBIndex {
        int ITM = 10; // max 1<<ITM ids
        int n = 585836;
        char cache_version = 1;
        String sfxb;
        char[] sfxd;
        byte[] sfxis;
        int[] sfxi;
        int[] sfxr;
        int[] nxti;
        int[] id_at;
        int[] nxtp;
        boolean cached_data_loaded = false;

        String b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        int[] b64pos = new int[128];

        int first, last;

        private void read64data(char[] s, int[] data) {
            int n = data.length;
            for (int i=0; i<n; ++i) {
                data[i]=(b64pos[s[4*i]]<<18)|((b64pos[s[4*i+1]]<<12))|((b64pos[s[4*i+2]]<<6))|b64pos[s[4*i+3]];
            }
        }
        private void read64data(byte[] s, int[] data) {
            int n = data.length;
            for (int i=0; i<n; ++i) {
                data[i]=(b64pos[s[4*i]]<<18)|((b64pos[s[4*i+1]]<<12))|((b64pos[s[4*i+2]]<<6))|b64pos[s[4*i+3]];
            }
        }
        private void save64data(int[] data, FileWriter fo) {
            char[] cdata = new char[data.length*4+1];
            for (int i=0; i<data.length; ++i) {
                int v = data[i];
                cdata[4*i] = b64.charAt((v>>18)&63);
                cdata[4*i+1] = b64.charAt((v>>12)&63);
                cdata[4*i+2] = b64.charAt((v>>6)&63);
                cdata[4*i+3] = b64.charAt(v&63);
            }
            cdata[cdata.length-1] = cache_version;
            try {
                fo.write(cdata);
                fo.close();
            } catch (Exception e) { }
        }

        String cached_nxti;
        String cached_id_at;
        String cached_nxtp;
        String cached_sfxd;

        private boolean tryLoadCachedData() {
            File file_nxti = new File(cached_nxti);
            File file_id_at = new File(cached_id_at);
            File file_nxtp = new File(cached_nxtp);
            File file_sfxd = new File(cached_sfxd);
            if (file_nxti.exists() && file_id_at.exists() && file_nxtp.exists() && file_sfxd.exists()) {
                try {
                    char[] buffer = new char[nxtp.length*4+1];
                    FileReader fr = new FileReader(file_nxti);
                    fr.read(buffer, 0, nxti.length*4+1);
                    if (buffer[nxti.length*4]!=cache_version) return false;
                    read64data(buffer, nxti);
                    fr.close();
                    fr = new FileReader(file_id_at);
                    fr.read(buffer, 0, id_at.length*4+1);
                    if (buffer[id_at.length*4]!=cache_version) return false;
                    read64data(buffer, id_at);
                    fr.close();
                    fr = new FileReader(file_nxtp);
                    fr.read(buffer, 0, nxtp.length*4+1);
                    if (buffer[nxtp.length*4]!=cache_version) return false;
                    read64data(buffer, nxtp);
                    fr = new FileReader(file_sfxd);
                    fr.read(sfxd, 0, sfxd.length);
                    if (sfxd[sfxd.length-1]!=cache_version) return false;
                    return true;
                } catch (Exception e) { }
            }
            return false;
        }
        private void cacheData() {
            try {
                save64data(nxti, new FileWriter(cached_nxti));
                save64data(id_at, new FileWriter(cached_id_at));
                save64data(nxtp, new FileWriter(cached_nxtp));
                sfxd[sfxd.length-1] = cache_version;
                try {
                    FileWriter fo = new FileWriter(cached_sfxd);
                    fo.write(sfxd);
                    fo.close();
                } catch (Exception e) { }
            } catch (Exception e) { }
        }
        public void deleteCacheFiles(){
            File file_nxti = new File(cached_nxti);
            File file_id_at = new File(cached_id_at);
            File file_nxtp = new File(cached_nxtp);
            File file_sfxd = new File(cached_sfxd);
            if (file_nxti.exists()) file_nxti.delete();
            if (file_id_at.exists()) file_id_at.delete();
            if (file_nxtp.exists()) file_nxtp.delete();
            if (file_sfxd.exists()) file_sfxd.delete();
        }

        private void build() {
            sfxi = new int[n];
            nxti = new int[n];
            id_at = new int[n];
            sfxd = new char[n+1];
            nxtp = new int[(((n>>ITM)+1)<<ITM)+1];
            read64data(sfxis, sfxi);
            cached_data_loaded = tryLoadCachedData();
            if (!cached_data_loaded) {
                sfxr = new int[n];
                for (int i=0; i<n; ++i) sfxr[sfxi[i]]=i;
                int j = 0;
                char l = 0;
                for (int i=0; i<sfxb.length(); i+=5) {
                    int ix=(b64pos[sfxb.charAt(i+1)]<<18)|((b64pos[sfxb.charAt(i+2)]<<12))|((b64pos[sfxb.charAt(i+3)]<<6))|b64pos[sfxb.charAt(i+4)];
                    while (j<ix) sfxd[sfxi[j++]] = l;
                    l = sfxb.charAt(i);
                }
                while (j<n) sfxd[sfxi[j++]] = l;
            }
        }

        boolean ge(int pos, String s) {
            int j = sfxi[pos];
            for (int i=0; i<s.length() && j+i<n; ++i) {
                if (sfxd[j+i] > s.charAt(i)) return true;
                if (sfxd[j+i] < s.charAt(i)) return false;
            }
            return true;
        }
        boolean le(int pos, String s) {
            int j = sfxi[pos];
            for (int i=0; i<s.length() && j+i<n; ++i) {
                if (sfxd[j+i] < s.charAt(i)) return true;
                if (sfxd[j+i] > s.charAt(i)) return false;
            }
            return true;
        }

        public void search(String s) {
            int a=0; int b=n;
            while (a<b) {
                int m = (a+b)/2;
                if (ge(m, s)) b=m;
                else a=m+1;
            }
            first = a;

            a=0; b=n;
            while (a<b) {
                int m = (a+b)/2;
                if (le(m, s)) a=m+1;
                else b=m;
            }
            last = a-1;
        }

        private void setId(int id, int a, int b) {
            ArrayList<Integer> ix = new ArrayList<>();
            for (int i=a; i<=b; ++i) ix.add(sfxr[i]);
            Collections.sort(ix);
            int lp = 0;
            for (int i=0; i<ix.size(); ++i) {
                int j = ix.get(i);
                id_at[j]=id;
                while (j>=(lp<<ITM)) {
                    nxtp[(lp<<ITM)|id]=j;
                    lp++;
                }
                if (i+1<ix.size()) nxti[j]=ix.get(i+1);
                else               nxti[j]=-1;
            }

            while (lp<(n>>ITM)) {
                nxtp[((lp<<ITM)|id)]=-1;
                lp++;
            }
        }

        public boolean idMatch(int id) {
            if (last-first<10) {
                for (int i=first; i<=last; ++i) {
                    if (id_at[i]==id) return true;
                }
                return false;
            } else {
                int j = nxtp[(first&~1023)+id];
                while (j<first && j!=-1) j=nxti[j];
                return j!=-1 && j<=last;
            }
        }

        VDBIndex(Context context, InputStream sfxi, InputStream sfxbi) {
            cached_nxti = context.getCacheDir()+"/nxti.txt";
            cached_id_at = context.getCacheDir()+"/id_at.txt";
            cached_nxtp = context.getCacheDir()+"/nxtp.txt";
            cached_sfxd = context.getCacheDir()+"/sfxd.txt";

            for (int i=0; i<64; ++i) b64pos[b64.charAt(i)] = i;
            sfxis = new byte[n*4];
            try {
                sfxi.read(sfxis, 0, sfxis.length);
            }catch (Exception e) {} finally{
                try{sfxi.close();}catch(Exception e){}
            }
            sfxb = is2string(sfxbi);
            build();
        }
    }

    static VDBIndex vi;


    static boolean indexLoaded = false;
    public static boolean isIndexLoaded() { return indexLoaded;}
    static void loadVDBIndexAndCheckCacheInfo(Context context) {
        long start = System.currentTimeMillis();
        vi = new VDBIndex(context, context.getResources().openRawResource(R.raw.sfxi), context.getResources().openRawResource(R.raw.sfxb));
        if (!vi.cached_data_loaded) {
            for (int i = 0; i < vdb.size(); ++i) vi.setId(i, vdb.get(i).a, vdb.get(i).b);
            indexLoaded = true;
            vi.cacheData();
        } else indexLoaded = true;

        Log.d(TAG, "index loaded in "+((System.currentTimeMillis()-start)/1000.0)+" seconds");
        start = System.currentTimeMillis();
        int mismatches = checkCached(context);
        if (mismatches>0) saveCacheInfo(context);
        Log.d(TAG, "cached pages info checked in "+((System.currentTimeMillis()-start)/1000.0)+" seconds, "+mismatches+" mismatches.");
    }

    static public class Section {
        int tp;
        String title;
        int index;
        Section(int tp, String title, int index) {
            this.tp = tp;
            this.title = title;
            this.index = index;
        }
    }
    static ArrayList<Section> sections = new ArrayList<>();

    static void loadVDB(Context context) {
        if (!vdb.isEmpty()) return;
        long start = System.currentTimeMillis();
        vdb.ensureCapacity(1000);
        int sz = 84640;
        byte[] s = new byte[sz];
        try {
            context.getResources().openRawResource(R.raw.vdb).read(s, 0, sz);
        } catch (IOException e) {
            return;
        }
        Log.d(TAG, "VDB file read took "+((System.currentTimeMillis()-start)/1000.0)+" seconds");
        int j = 0; int p = 0; int i0 = 0; int i1 = 0; int i2 = 0; int i3 = 0; int a = 0; int b = 0;
        for (int i=0; i<sz; ++i) {
            if (s[i]==';') {
                if (p==0) i0=j;
                if (p==1) i1=j;
                if (p==2) i2=j;
                if (p==3) i3=j;
                p+=1;
                j=i+1;
            } else if (s[i]=='\n') {
                vdb.add(new VDBE(s, i0, i1, i2, i3, a, b));
                a=0;b=0;p=0;j=i+1;
            } else if (p==3) {
                a*=10; a+=s[i]-'0';
            } else if (p==4) {
                b*=10; b+=s[i]-'0';
            }
        }

        int n = 0;
        try {
            n = context.getResources().openRawResource(R.raw.sections).read(s, 0, sz);
        } catch (IOException e) { }

        char tp = ' ';
        String title = "";
        int ix = 0;
        p=0;
        for (int i=0; i<n; ++i) {
            if (s[i]==';') {
                if (p==0) {
                    tp = (char)(s[i-1]);
                    j=i+1;
                }
                if (p==1) {
                    title = new String(s, j, i-j, Charset.defaultCharset());
                }
                ++p;
            }else if (s[i]=='\n') {
                sections.add(new Section(tp-'0', title, ix));
                ix=0;
                p=0;
            } else if (p==2) {
                ix*=10;  ix+=s[i]-'0';
            }
        }

        new Thread(() -> loadVDBIndexAndCheckCacheInfo(context)).start();
        Log.d(TAG, "VDB loaded in "+((System.currentTimeMillis()-start)/1000.0)+" seconds");
    }

    static int findByUrl(String url) {
        for (int i=0; i<vdb.size(); ++i) {
            if (vdb.get(i).url.equals(url)) return i;
        }
        return -1;
    }

    static byte[] bbuffer = null;
    static void zipFile(ZipOutputStream zout, String fname, String nameInZip) {
        if (bbuffer == null) bbuffer = new byte[8192];
        try {
            ZipEntry zipEntry = new ZipEntry(nameInZip);
            FileInputStream fin = new FileInputStream(fname);
            zout.putNextEntry(zipEntry);
            int n;
            while ((n = fin.read(bbuffer)) != -1)  zout.write(bbuffer, 0, n);
            zout.closeEntry();
            fin.close();
        } catch (Exception e) {
            Log.d(TAG, "zip file failed, "+e);
        }
    }
    static int saveCacheAndFavourites(Context context, Uri uri, boolean cache, boolean favourites) {
        int nbf = 0;
        try {
            ZipOutputStream zout = new ZipOutputStream(new BufferedOutputStream(context.getContentResolver().openOutputStream(uri)));
            if (favourites) {
                zipFile(zout, favouritesFileName(context), "favourites");
                ++nbf;
            }
            if (cache) {
                for (int i = 0; i < vdb.size(); ++i) {
                    if (vdb.get(i).cached) {
                        zipFile(zout, getWebArchiveFileName(context, vdb.get(i).no), vdb.get(i).no + ".mht");
                        ++nbf;
                    }
                }
            }
            zout.close();
            Log.d(TAG, "saved to "+uri);
        } catch (Exception e) {
            Log.d(TAG, "exception, "+e);
        }
        return nbf;
    }


    static void unzipFile(ZipInputStream zin, String fname) {
        if (bbuffer == null) bbuffer = new byte[8192];
        try {
            FileOutputStream fout = new FileOutputStream(fname);
            int n;
            while ((n = zin.read(bbuffer)) != -1) fout.write(bbuffer, 0, n);
            zin.closeEntry();
            fout.close();
        } catch (Exception e) {
            Log.d(TAG, "unzip file failed, "+e);
        }
    }
    static int matchingCacheFile(Context context, String name) {
        // return internal file name that matches to saved file 'name'
        if (name.endsWith(".mht")) {
            for (int i=0; i<vdb.size(); ++i) {
                if (name.equals(vdb.get(i).no+".mht")) {
                    return i;
                }
            }
        }
        return -1;
    }
    static int loadCacheAndFavourites(Context context, Uri uri) {
        int nbf = 0;
        try {
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(context.getContentResolver().openInputStream(uri)));
            while (true) {
                try {
                    ZipEntry ze = zin.getNextEntry();
                    if (ze == null) break;
                    String t = ze.getName().toString();
                    String of;
                    int type = 0;
                    int id = -1;
                    if (t.equals("favourites")) {
                        of = favouritesFileName(context);
                    } else {
                        id = matchingCacheFile(context, t);
                        if (id<0) {
                            Log.d(TAG, "undetected file: "+ze.getName().toString());
                            continue;
                        }
                        type = 1;
                        of = getWebArchiveFileName(context, vdb.get(id).no);
                    }
                    Log.d(TAG, "detected file: "+of);
                    unzipFile(zin, of);
                    ++nbf;
                    if (type == 0) {
                        loadFavourites(context);
                    } else if (type == 1) {
                        cached(context, id);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "unzip failed "+e);
                    break;
                }
            }
            zin.close();
            Log.d(TAG, "loaded from "+uri);
        } catch (Exception e) {
            Log.d(TAG, "exception, "+e.toString());
        }
        return nbf;
    }


    static RxDataStore<Preferences> dataStore = null;
    static void createSettings(Context context) {
        if (dataStore == null) {
            dataStore = new RxPreferenceDataStoreBuilder(context, "settings").build();
        }
    }
    static void setIntValueToSettings(String key, int value) {
        Preferences.Key<Integer> KEY = PreferencesKeys.intKey(key);
        dataStore.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.set(KEY, value);
            return Single.just(mutablePreferences);
        });
    }
    static int getIntSetting(String key, int defaultValue) {
        Preferences.Key<Integer> KEY = PreferencesKeys.intKey(key);
        Single<Integer> value = dataStore.data().firstOrError().map(prefs -> prefs.get(KEY)).onErrorReturnItem(defaultValue);
        return value.blockingGet();
    }

    static void deleteIntSetting(String key) {
        Preferences.Key<Integer> KEY = PreferencesKeys.intKey(key);
        dataStore.updateDataAsync(prefsIn -> {
            MutablePreferences mutablePreferences = prefsIn.toMutablePreferences();
            mutablePreferences.remove(KEY);
            return Single.just(mutablePreferences);
        });
    }
}
