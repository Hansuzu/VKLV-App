package com.jp.vklv;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
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

    ListView sideMenu;
    boolean showSideMenu = false;

    Toast toast = null;
    void toastMessage(String message) {
        if (toast!=null) toast.cancel();
        toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public class vRef extends Object {
        int vi;
        @Override
        public String toString() {
            return vi>=0?VData.vdb.get(vi).toString():VData.sections.get(-vi-1).title;
        }
        public boolean favourite() {return vi>=0?VData.vdb.get(vi).favourite:false;}
        public boolean isSection() { return vi<0;}
        vRef(int i) {vi = i;}
    }
    ArrayList<vRef> listItems = new ArrayList<>();
    public class VDBArrayAdapter extends ArrayAdapter<vRef> {
        public VDBArrayAdapter(Context context, ArrayList<vRef> array) {
            super(context, android.R.layout.simple_list_item_1, array);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView v = (TextView) super.getView(position, convertView, parent);
            /*
            v.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.heart,0);
            if (getItem(position).favourite()) v.getCompoundDrawables()[2].setTint(0xff000000);
            else  v.getCompoundDrawables()[2].setTint(0x0f000000);
            */
            if (getItem(position).favourite())  v.setCompoundDrawablesWithIntrinsicBounds(0,0,R.drawable.heart,0);
            else v.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0);
            int ptb = 40;
            if (getItem(position).isSection()) {
                int tp = VData.sections.get(-getItem(position).vi-1).tp;
                if (tp == 0)  {
                    v.setBackground(getDrawable(R.drawable.list_item_h3_background));
                    v.setPadding(120, ptb, 50, ptb);
                    v.setTextColor(0xFF666677);
                }
                if (tp == 1)  {
                    v.setBackground(getDrawable(R.drawable.list_item_h2_background));
                    v.setPadding(70, ptb, 50, ptb);
                    v.setTextColor(Color.BLACK);
                    //v.setTextSize(20);
                }
                if (tp == 2)  {
                    v.setBackground(getDrawable(R.drawable.list_item_h1_background));
                    v.setPadding(20, ptb, 50, ptb);
                    v.setTextColor(Color.BLACK);
                    //v.setTextSize(22);
                }
            } else {
                v.setBackground(getDrawable(R.drawable.list_item_background));
                v.setPadding(50, ptb, 50, ptb);
                v.setTextColor(Color.BLACK);
                //v.setTextSize(16);
            }
            return v;
        }
    }
    //ArrayAdapter<vRef> adapter;
    VDBArrayAdapter adapter;


    ArrayList<vRef> menuItems = new ArrayList<>();
    VDBArrayAdapter menuAdapter;

    void addToList(int i) {
        listItems.add(new vRef(i));
    }
    void addSectionToList(int i) {
        if (VData.sections.get(i).tp>0) return;
        while (listItems.size()>0 && listItems.get(listItems.size()-1).isSection()) {
            int j = -listItems.get(listItems.size()-1).vi-1;
            if (VData.sections.get(i).tp >= VData.sections.get(j).tp) {
                listItems.remove(listItems.size()-1);
            } else break;
        }
        listItems.add(new vRef(-i-1));
    }

    void filter(CharSequence cs) {
        long start = System.currentTimeMillis();
        String so = cs.toString();
        String s = so.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");

        listItems.clear();
        if (VData.indexLoaded) VData.vi.search(s);
        int nbm = 0;
        int nbmi = 0;
        int section_i = 0;
        for (int i=0; i<VData.vdb.size(); ++i) {
            //if (so.length()==0 && !filterFavourites && filterCached==0)
                while (section_i<VData.sections.size() && VData.sections.get(section_i).index<=i)
                    addSectionToList(section_i++);

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
        while (listItems.size()>0 && listItems.get(listItems.size()-1).isSection()) {
            listItems.remove(listItems.size()-1);
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
            String first = s.substring(8);
            String last = first;
            for (int i=8; i<s.length(); ++i) {
                if (s.charAt(i)=='-') {
                    first = s.substring(8, i);
                    last = s.substring(i+1);
                }
            }
            Log.d(TAG, "remove "+first+" "+last);
            int ifirst = -1;
            int ilast = -1;
            for (int i = 0; i < VData.vdb.size(); ++i) {
                if (VData.vdb.get(i).no.equals(first)) ifirst = i;
                if (VData.vdb.get(i).no.equals(last)) ilast = i;
            }
            if (ifirst!=-1 && ilast !=-1) {
                int removed = 0;
                int notRemoved = 0;
                for (int i = ifirst; i <= ilast; ++i) {
                    if (VData.uncache(this, i)) ++removed;
                    else ++notRemoved;
                }
                et.setText("uncache: removed "+removed+" files, did not remove "+notRemoved+" files.");
            } else et.setText("uncache failed: no match for '" + s.substring(13) + "'");

        } else if (s.equals("rng")) {
            open(new Random().nextInt(VData.vdb.size()));
        } else if (s.equals("rng2")) {
            int nb = 0;
            for (int i=0; i<VData.vdb.size(); ++i) if (VData.vdb.get(i).favourite) ++nb;
            int n = new Random().nextInt(nb);
            for (int i=0; i<VData.vdb.size(); ++i) if (VData.vdb.get(i).favourite) if (n--==0) open(i);
        } else if (s.startsWith("export:")) {
            if (s.equals("export:all")) {
                tryExportCacheAndFavourites(true, true);
            } else if (s.equals("export:cache")) {
                tryExportCacheAndFavourites(true, false);
            } else if (s.equals("export:favourites")) {
                tryExportCacheAndFavourites(false, true);
            }
        } else if (s.startsWith("import:")) {
            if (s.equals("import:all")) {
                tryImportCacheAndFavourites();
            }
        }

        filter(s+" ");
        if (listItems.size()<5) {
            int nb = 0;
            int nbi = 0;
            for (int i=0; i<listItems.size(); ++i) {
                if (!listItems.get(i).isSection()) {
                    ++nb;
                    nbi = i;
                }
            }
            if (nb==1) open(listItems.get(nbi).vi);
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
        if (filterFavourites) {
            toastMessage("Näytetään suosikit.");
        } else {
            toastMessage("Näytetään kaikki.");
        }
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
        if (filterCached == 0) {
            toastMessage("Näytetään kaikki.");
        } else if (filterCached == 1){
            toastMessage("Näytetään offline-tilaan tallennetut.");
        } else {
            toastMessage("Näytetään offline-tilaan tallentamattomat.");
        }
        refilter();
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long start = System.currentTimeMillis();
        VData.loadVDB(this);
        super.onCreate(savedInstanceState);
        VData.createSettings(this);

        setContentView(R.layout.main);

        listItems.ensureCapacity(VData.vdb.size());
        int section_i = 0;
        for (int i=0; i<VData.vdb.size(); ++i) {
            while (section_i<VData.sections.size() && VData.sections.get(section_i).index<=i)
                addSectionToList(section_i++);
            addToList(i);
        }

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

        for (int i=0; i<VData.sections.size(); ++i) menuItems.add(new vRef(-i-1));
        menuAdapter = new VDBArrayAdapter(this, menuItems);
        sideMenu = findViewById(R.id.sideMenu);
        sideMenu.setAdapter(menuAdapter);
        sideMenu.setVisibility(View.INVISIBLE);
        sideMenu.setOnItemClickListener((parent, view, position, id) -> {
            int vi = VData.sections.get(-menuItems.get(position).vi -1).index;
            Log.d(TAG, "onClick, vi="+vi);
            for (int i=0; i<listItems.size(); ++i) {
                if (listItems.get(i).vi == menuItems.get(position).vi) {
                    closeSideMenu();
                    getListView().smoothScrollToPositionFromTop(i, 10, 50);
                    break;
                }
                if (listItems.get(i).vi >= vi) {
                    closeSideMenu();
                    getListView().smoothScrollToPositionFromTop(i-1, 10, 50);
                    break;
                }
            }
        });

        ((ListView)findViewById(android.R.id.list)).setOnItemLongClickListener((parent, view, position, id) -> {
            if (listItems.get(position).isSection()) return false;
            VData.toggleFavourite(this, listItems.get(position).vi);
            if (VData.vdb.get(listItems.get(position).vi).favourite) {
                toastMessage("Virsi lisätty suosikkeihin.");
            } else {
                toastMessage("Virsi poistettu suosikeista.");
            }
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
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
        if (listItems.get(position).isSection()) {
            toastMessage(VData.sections.get(-listItems.get(position).vi-1).title);
            return;
        }
        open(listItems.get(position).vi);
    }

    float motionStartX;
    float motionStartY;

    void openSideMenu() {
        sideMenu.animate().translationX(0).setDuration(100);
        showSideMenu = true;
    }
    void closeSideMenu() {
        sideMenu.animate().translationX(-sideMenu.getWidth()).setDuration(100);
        showSideMenu = false;
    }

    boolean touchMovesSideMenu = false;
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            motionStartX = e.getRawX();
            motionStartY = e.getRawY();
            touchMovesSideMenu = false;
            if (showSideMenu && motionStartX>=sideMenu.getWidth()) {
                closeSideMenu();
                return true;
            }
        } else if (e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_UP) {
            float dx = e.getRawX() - motionStartX;
            float dy = e.getRawY() - motionStartY;
            float adx = dx>0?dx:-dx;
            float ady = dy>0?dy:-dy;
            if (adx>ady || touchMovesSideMenu) {
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    if (dx > 100 && !showSideMenu) {
                        touchMovesSideMenu = true;
                        float x = -sideMenu.getWidth() + 3*(dx - 100);
                        if (x>0) x = 0;
                        sideMenu.animate().translationX(x).setDuration(0);
                        sideMenu.setVisibility(View.VISIBLE);
                        MotionEvent cancelEvent = MotionEvent.obtain(e);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        return super.dispatchTouchEvent(cancelEvent);
                    } else if (dx < -50 && showSideMenu) {
                        touchMovesSideMenu = true;
                        sideMenu.animate().translationX(3*(dx + 50)).setDuration(0);
                        MotionEvent cancelEvent = MotionEvent.obtain(e);
                        cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                        return super.dispatchTouchEvent(cancelEvent);
                    }
                } else if (touchMovesSideMenu) {
                    if (dx >= 200 && !showSideMenu) {
                        openSideMenu();
                    } else if (!showSideMenu) {
                        closeSideMenu();
                    } else if (dx <= -150 && showSideMenu) {
                        closeSideMenu();
                    } else if (showSideMenu) {
                        openSideMenu();
                    }
                }
            }
        }
        return super.dispatchTouchEvent(e);
    }
    static int OPEN_SAVED_DATA_REQUEST_CODE = 1;
    static int SAVE_DATA_ALL_REQUEST_CODE = 2;
    static int SAVE_DATA_CACHE_REQUEST_CODE = 3;
    static int SAVE_DATA_FAVOURITES_REQUEST_CODE = 4;
    private void tryImportCacheAndFavourites() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setType("application/zip");
        startActivityForResult(Intent.createChooser(intent,"Valitse tiedosto"), OPEN_SAVED_DATA_REQUEST_CODE);
    }
    private void tryExportCacheAndFavourites(boolean cache, boolean favourites) {
        if (!cache && !favourites) return;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.setType("application/zip");
        int CODE = SAVE_DATA_ALL_REQUEST_CODE;
        if (cache && favourites) intent.putExtra(Intent.EXTRA_TITLE, "vklv_data.zip");
        else if (favourites) {
            intent.putExtra(Intent.EXTRA_TITLE, "vklv_data_favourites.zip");
            CODE = SAVE_DATA_FAVOURITES_REQUEST_CODE;
        } else if (cache) {
            intent.putExtra(Intent.EXTRA_TITLE, "vklv_data_cached.zip");
            CODE = SAVE_DATA_CACHE_REQUEST_CODE;
        }
        startActivityForResult(Intent.createChooser(intent,"Valitse tiedosto"), CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OPEN_SAVED_DATA_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) {
                    new Thread(() -> {
                        int nbf = VData.loadCacheAndFavourites(this, data.getData());
                        runOnUiThread(() -> toastMessage("Data tuotu, tuotiin yhteensä " + nbf + " tiedostoa."));
                    }).start();
                    toastMessage("Aloitetaan datan tuominen...");
                    refilter();
                }
            }
        } else if (requestCode == SAVE_DATA_ALL_REQUEST_CODE || requestCode == SAVE_DATA_CACHE_REQUEST_CODE || requestCode == SAVE_DATA_FAVOURITES_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null && data.getData() != null) {
                    boolean cache = (requestCode == SAVE_DATA_ALL_REQUEST_CODE || requestCode == SAVE_DATA_CACHE_REQUEST_CODE);
                    boolean favourites = (requestCode == SAVE_DATA_ALL_REQUEST_CODE || requestCode == SAVE_DATA_FAVOURITES_REQUEST_CODE);
                    new Thread(() -> {
                        int nbf = VData.saveCacheAndFavourites(this, data.getData(), cache, favourites);
                        runOnUiThread(() -> toastMessage("Data viety, luotu zip tiedosto, johon pakattiin "+ nbf+" tiedostoa."));
                    }).start();
                    toastMessage("Aloitetaan datan vieminen...");
                }
            }
        }
    }
}
