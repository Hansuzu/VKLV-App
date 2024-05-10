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
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import androidx.core.content.ContextCompat;


public class MainActivity extends ListActivity {
    String TAG = "VK+LV:LIST";
    EditText et;
    boolean loadFromCache = true;
    boolean saveToCache = false;
    boolean buildingCache = false;
    int buildingCachePos = 0;
    boolean filterFavourites = false;
    int filterCached = 0;

    public class vRef extends Object {
        int vi;
        @Override
        public String toString() {
            return VData.vdb.get(vi).toString();
        }
        public boolean favourite() {return VData.vdb.get(vi).favourite;}
        vRef(int i) {
            vi = i;
        }
    }
    ArrayList<vRef> listItems = new ArrayList<>();
    public class VDBArrayAdapter extends ArrayAdapter<vRef> {
        public VDBArrayAdapter(Context context, ArrayList<vRef> array) {
            super(context, android.R.layout.simple_list_item_1, array);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);
            if (getItem(position).favourite()) v.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.heart,0);
            else v.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            return v;
        }
    }
    //ArrayAdapter<vRef> adapter;
    VDBArrayAdapter adapter;



    void addToList(int i) {
        listItems.add(new vRef(i));
    }

    void filter(CharSequence cs) {
        long start = System.currentTimeMillis();
        String so = cs.toString();
        String s = so.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");

        listItems.clear();
        if (VData.indexLoaded) VData.vi.search(s);
        int nbm = 0;
        int nbmi = 0;
        for (int i=0; i<VData.vdb.size(); ++i) {
            if (filterFavourites && !VData.vdb.get(i).favourite) continue;
            if (filterCached==1 && !VData.vdb.get(i).cached) continue;
            if (filterCached==2 && VData.vdb.get(i).cached) continue;
            if (VData.indexLoaded && VData.vi.idMatch(i)) {
                addToList(i);
            } else if (VData.vdb.get(i).no.startsWith(s)) {
                if (VData.vdb.get(i).no.equals(s)) {
                    if (so.charAt(so.length()-1) == ' ') {
                        open(i);
                        et.setText("");
                        return;
                    }
                }
                addToList(i);
                ++nbm;
                nbmi = i;
            } else if (!VData.indexLoaded) addToList(i);
        }
        if (nbm==1 && !filterFavourites && filterCached==0) {
            open(nbmi);
            et.setText("");
        } else {
            adapter.notifyDataSetChanged();
        }
        Log.d(TAG, "searching took "+((System.currentTimeMillis() - start)/1000.0)+" seconds");
    }
    void refilter() {
        filter(et.getText());
    }

    void continueBuildingCache() {
        if (!buildingCache) return;
        while (buildingCachePos < VData.vdb.size()) {
            buildingCachePos += 1;
            if (VData.vdb.get(buildingCachePos-1).cached) continue;
            et.setText("cache:build: " + VData.vdb.get(buildingCachePos - 1).no);
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
                int deleted = VData.clearCached(this);
                et.setText("cache cleared, " + (deleted) + " files deleted");
            } else if (s.equals("data_cache:clear")) {
                VData.vi.deleteCacheFiles();
                et.setText("data cache cleared");
            }
            if (s.equals("cache:info")) {
                int nbf = 0;
                for (int i=0; i<VData.vdb.size(); ++i) {
                    if (VData.vdb.get(i).cached) ++nbf;
                }
                et.setText("cache: " + (nbf) + "/" + (VData.vdb.size()) + " pages currently cached.");
            } else if (s.equals("cache:save:true")) {
                saveToCache = true;
                et.setText("cache: saving pages to cache enabled");
            } else if (s.equals("cache:save:false")) {
                saveToCache = false;
                et.setText("cache: saving pages to cache disabled");
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
                continueBuildingCache();
            }
        } else if (s.startsWith("uncache:")) {
            String n = s.substring(13);
            boolean found = false;
            boolean removed = false;
            for (int i = 0; i < VData.vdb.size() && !found; ++i) {
                if (VData.vdb.get(i).no.equals(n)) {
                    found = true;
                    removed = VData.uncache(this, i);
                    break;
                }
            }
            if (!found) et.setText("uncache failed: no match for '" + n + "'");
            else if (!removed) et.setText("uncache did nothing: cache file not found.");
            else et.setText("uncache: succesfully removed cache file.");
        } else if (s.equals("rng")) {
            open(new Random().nextInt(VData.vdb.size()));
        } else if (s.equals("rng2")) {
            int nb = 0;
            for (int i=0; i<VData.vdb.size(); ++i) if (VData.vdb.get(i).favourite) ++nb;
            int n = new Random().nextInt(nb);
            for (int i=0; i<VData.vdb.size(); ++i) if (VData.vdb.get(i).favourite) if (n--==0) open(i);
        }

        filter(s+" ");
        if (listItems.size()==1) {
            open(listItems.get(0).vi);
        }
    }

    void setFilterFavouritesButtonStyle() {
        //findViewById(R.id.filterFavourites).setVisibility(View.GONE);
        ImageButton cb = findViewById(R.id.filterFavourites);
        if (filterFavourites)  cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.dark_button_active));
        else                   cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.dark_button_inactive));
    }
    boolean filterFavouritesButtonClicked() {
        filterFavourites = !filterFavourites;
        setFilterFavouritesButtonStyle();
        refilter();
        return true;
    }
    void setFilterCachedButtonStyle() {
        //findViewById(R.id.filterFavourites).setVisibility(View.GONE);
        ImageButton cb = findViewById(R.id.filterCached);
        if (filterCached==0) {
            cb.setImageDrawable(getDrawable(R.drawable.content_save));
            cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.dark_button_inactive));
        } else if (filterCached == 1) {
            cb.setImageDrawable(getDrawable(R.drawable.content_save));
            cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.dark_button_active));
        } else if (filterCached == 2) {
            cb.setImageDrawable(getDrawable(R.drawable.content_save_off_outline));
            cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.dark_button_active));
        }
    }
    boolean filterCachedButtonClicked() {
        filterCached += 1;
        if (filterCached>=3) filterCached = 0;
        setFilterCachedButtonStyle();
        refilter();
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        VData.loadVDB(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        listItems.ensureCapacity(VData.vdb.size());
        for (int i=0; i<VData.vdb.size(); ++i) addToList(i);

        //adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listItems);
        adapter = new VDBArrayAdapter(this, listItems);
        setListAdapter(adapter);

        et = findViewById(R.id.editText);
        ImageButton cb = findViewById(R.id.clearText);
        cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.button_color_state_list));
        cb.setOnClickListener(v -> et.setText(""));
        findViewById(R.id.filterFavourites).setOnClickListener(v -> filterFavouritesButtonClicked());
        setFilterFavouritesButtonStyle();
        findViewById(R.id.filterCached).setOnClickListener(v -> filterCachedButtonClicked());
        setFilterCachedButtonStyle();

        ((ListView)findViewById(android.R.id.list)).setOnItemLongClickListener((parent, view, position, id) -> {
            VData.toggleFavourite(this, listItems.get(position).vi);
            adapter.notifyDataSetChanged();
            //*/
            return true;
        });
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
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
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
        long start = System.currentTimeMillis();
        super.onResume();
        VData.loadFavourites(this);
        VData.loadCacheInfo(this);
        if (showKeyboardOnResume) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
            et.requestFocus();
        }
        if (buildingCache) continueBuildingCache();
        refilter();
        Log.d(TAG, "onResume took "+((System.currentTimeMillis() - start)/1000.0)+" seconds.");
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
        if (vi<VData.vdb.size() && vi>=0) {
            VData.VDBE v = VData.vdb.get(vi);
            intent.putExtra("index", vi);
            //myIntent.putExtra("useWebViewCache", true);
            intent.putExtra("loadFromCache", loadFromCache);
            intent.putExtra("saveToCache", saveToCache);
            intent.putExtra("instantReturn", buildingCache);
            MainActivity.this.startActivity(intent);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View w, int position, long id) {
        open(listItems.get(position).vi);
    }
}
