package com.jp.vklv;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import androidx.core.content.ContextCompat;


public class MainActivity extends ListActivity {
    String TAG = "VK+LV:LIST";
    EditText et;
    boolean loadFromCache = true;
    boolean saveToCache = false;
    boolean buildingCache = false;
    boolean indexLoaded = false;
    int buildingCachePos = 0;

    String webArchiveFile(String no) {
        return getCacheDir() + "/" + no + ".mht";
    }

    char[] buffer = new char[8192];
    public String is2string(InputStream is) {
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

    ArrayList<VDBE> vdb = new ArrayList<>();

    public class VDBE {
        String no;
        String url;
        String name;
        String hd="";
        int a;
        int b;
        public String toString() {
            if (hd.length()==0) hd = no+" "+name;
            return hd;
        }
        VDBE(byte[] s, int i0, int i1, int i2, int i3, int a, int b) {
            no=new String(s, i0, i1-i0-1, Charset.defaultCharset());
            url=new String(s, i1, i2-i1-1, Charset.defaultCharset());
            name=new String(s, i2, i3-i2-1, Charset.defaultCharset());
            this.a=a;
            this.b=b;
        }
    }

    public class vRef extends Object {
        int vi;
        @Override
        public String toString() {
            return vdb.get(vi).toString();
        }
        vRef(int i) {
            vi = i;
        }
    }
    ArrayList<vRef> listItems = new ArrayList<>();
    ArrayAdapter<vRef> adapter;

    public class VDBIndex {
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
        private String cached_nxti() {return getCacheDir()+"/nxti.txt";}
        private String cached_id_at() {return getCacheDir()+"/id_at.txt";}
        private String cached_nxtp() {return getCacheDir()+"/nxtp.txt";}
        private String cached_sfxd() {return getCacheDir()+"/sfxd.txt";}

        private boolean tryLoadCachedData() {
            File file_nxti = new File(cached_nxti());
            File file_id_at = new File(cached_id_at());
            File file_nxtp = new File(cached_nxtp());
            File file_sfxd = new File(cached_sfxd());
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
                save64data(nxti, new FileWriter(cached_nxti()));
                save64data(id_at, new FileWriter(cached_id_at()));
                save64data(nxtp, new FileWriter(cached_nxtp()));
                sfxd[sfxd.length-1] = cache_version;
                try {
                    FileWriter fo = new FileWriter(cached_sfxd());
                    fo.write(sfxd);
                    fo.close();
                } catch (Exception e) { }
            } catch (Exception e) { }
        }
        public void deleteCacheFiles(){
            File file_nxti = new File(cached_nxti());
            File file_id_at = new File(cached_id_at());
            File file_nxtp = new File(cached_nxtp());
            File file_sfxd = new File(cached_sfxd());
            if (file_nxti.exists()) file_nxti.delete();
            if (file_id_at.exists()) file_id_at.delete();
            if (file_nxtp.exists()) file_nxtp.delete();
            if (file_sfxd.exists()) file_sfxd.delete();
        }

        private void build() {
            sfxi = new int[n];
            sfxr = new int[n];
            nxti = new int[n];
            id_at = new int[n];
            sfxd = new char[n+1];
            nxtp = new int[(((n>>ITM)+1)<<ITM)+1];
            read64data(sfxis, sfxi);
            cached_data_loaded = tryLoadCachedData();
            if (!cached_data_loaded) {
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

        private void search(String s) {
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

        private boolean idMatch(int id) {
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

        VDBIndex(InputStream sfxi, InputStream sfxbi) {
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

    VDBIndex vi;

    void loadVDBIndex() {
        long start = System.currentTimeMillis();
        vi = new VDBIndex(getResources().openRawResource(R.raw.sfxi), getResources().openRawResource(R.raw.sfxb));
        if (!vi.cached_data_loaded) {
            for (int i = 0; i < vdb.size(); ++i) vi.setId(i, vdb.get(i).a, vdb.get(i).b);
            indexLoaded = true;
            vi.cacheData();
        } else indexLoaded = true;

        Log.d(TAG, "index loaded in "+((System.currentTimeMillis()-start)/1000.0)+" seconds");
    }

    void loadVDB() {
        long start = System.currentTimeMillis();
        vdb.ensureCapacity(1000);
        int sz = 84640;
        byte[] s = new byte[sz];
        try {
            getResources().openRawResource(R.raw.vdb).read(s, 0, sz);
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
        new Thread(() -> loadVDBIndex()).start();
        Log.d(TAG, "VDB loaded in "+((System.currentTimeMillis()-start)/1000.0)+" seconds");
    }

    void addToList(int i) {
        listItems.add(new vRef(i));
    }

    void filter(CharSequence cs) {
        //long start = System.currentTimeMillis();
        String so = cs.toString();
        String s = so.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");

        listItems.clear();
        if (indexLoaded) vi.search(s);
        int nbm = 0;
        int nbmi = 0;
        for (int i=0; i<vdb.size(); ++i) {
            if (indexLoaded && vi.idMatch(i)) {
                addToList(i);
            } else if (vdb.get(i).no.startsWith(s)) {
                if (vdb.get(i).no.equals(s)) {
                    if (so.charAt(so.length()-1) == ' ') {
                        open(i);
                        et.setText("");
                        return;
                    }
                }
                addToList(i);
                ++nbm;
                nbmi = i;
            } else if (!indexLoaded) addToList(i);
        }
        if (nbm==1) {
            open(nbmi);
            et.setText("");
        } else {
            adapter.notifyDataSetChanged();
        }
        //Log.d(TAG, "searching took "+((System.currentTimeMillis() - start)/1000.0)+" seconds");
    }

    void continueBuildingCache() {
        if (!buildingCache) return;
        while (buildingCachePos < vdb.size()) {
            buildingCachePos += 1;
            et.setText("cache:build: " + vdb.get(buildingCachePos - 1).no);
            if (new File(webArchiveFile(vdb.get(buildingCachePos - 1).no)).exists()) continue;
            open(buildingCachePos - 1);
            return;
        }
        buildingCache = false;
        et.setText("cache:build done");
    }

    void applyText(){
        String s = et.getText().toString();
        if (s.startsWith("cache:")) {
            if (s.equals("cache:clear")) {
                File[] files = getCacheDir().listFiles();
                for (int i = 0; i < files.length; ++i) {
                    files[i].delete();
                }
                et.setText("cache cleared, " + Integer.toString(files.length) + " files deleted");
            } else if (s.equals("data_cache:clear")) {
                vi.deleteCacheFiles();
                et.setText("data cache cleared");
            }
            if (s.equals("cache:info")) {
                File[] files = getCacheDir().listFiles();
                et.setText("cache: " + (files.length - 3) + "/" + Integer.toString(vdb.size()) + " pages currently cached.");
            } else if (s.equals("cache:save:true")) {
                saveToCache = true;
                et.setText("cache: saving pages to cache enabled");
            } else if (s.equals("cache:save:false")) {
                saveToCache = false;
                et.setText("cache: saving pages to cache disbled");
            } else if (s.equals("cache:load:true")) {
                loadFromCache = true;
                et.setText("cache: loading pages from cache enabled");
            } else if (s.equals("cache:load:false")) {
                loadFromCache = false;
                et.setText("cache: loading pages from cache disabled");
            } else if (s.equals("cache:build")) {
                saveToCache = true;
                buildingCache = true;
                buildingCachePos = 0;
                et.setText("");
                filter("");
                continueBuildingCache();
            }
        } else if (s.startsWith("uncache:")) {
            String n = s.substring(13);
            boolean found = false;
            boolean removed = false;
            for (int i = 0; i < vdb.size() && !found; ++i) {
                if (vdb.get(i).no.equals(n)) {
                    found = true;
                    File f = new File(webArchiveFile(n));
                    if (f.exists()) {
                        f.delete();
                        removed = true;
                    }
                }
            }
            if (!found) et.setText("uncache failed: no match for '" + n + "'");
            else if (!removed) et.setText("uncache did nothing: cache file not found.");
            else et.setText("uncache: succesfully removed cache file.");
        } else if (s.equals("rng")) {
            open(new Random().nextInt(vdb.size()));
        }

        filter(s+" ");
        if (listItems.size()==1) {
            open(listItems.get(0).vi);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        loadVDB();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        listItems.ensureCapacity(vdb.size());
        for (int i=0; i<vdb.size(); ++i) addToList(i);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        setListAdapter(adapter);

        et = findViewById(R.id.editText);
        ImageButton cb = findViewById(R.id.clearText);
        cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.button_color_state_list));
        cb.setOnClickListener(v -> et.setText(""));
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                MainActivity.this.filter(cs);
            }
            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
            @Override
            public void afterTextChanged(Editable arg0) {}
        });
        et.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                applyText();
                return true;
            }
            return false;
        });

        Log.d(TAG, "onCreate took "+((System.currentTimeMillis() - start)/1000.0)+" seconds.");
    }

    boolean showKeyboardOnResume = true;
    @Override
    protected void onResume() {
        super.onResume();
        if (showKeyboardOnResume) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            et.requestFocus();
        }
        if (buildingCache) continueBuildingCache();
    }

    private boolean checkKeyboard() {
        Rect r = new Rect();
        findViewById(android.R.id.content).getWindowVisibleDisplayFrame(r);
        int screenHeight = findViewById(android.R.id.content).getRootView().getHeight();
        int keypadHeight = screenHeight - r.bottom;
        return keypadHeight > screenHeight * 0.15;
    }
    @Override
    protected void onPause() {
        showKeyboardOnResume = checkKeyboard();
        if (!showKeyboardOnResume) et.clearFocus();
        super.onPause();
    }

    void open(int vi) {
        Intent intent = new Intent(MainActivity.this, ShowActivity.class);
        VDBE v = vdb.get(vi);
        intent.putExtra("url", v.url);
        intent.putExtra("id", v.no);
        intent.putExtra("title", v.name);
        //myIntent.putExtra("useWebViewCache", true);
        intent.putExtra("loadFromCache", loadFromCache);
        intent.putExtra("saveToCache", saveToCache);
        intent.putExtra("instantReturn", buildingCache);
        MainActivity.this.startActivity(intent);
    }

    @Override
    protected void onListItemClick(ListView l, View w, int position, long id) {
        open(listItems.get(position).vi);
    }
}
