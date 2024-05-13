package com.jp.vklv;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


public class ShowActivity extends AppCompatActivity {
    Toast toast = null;
    String TAG = "VK+LV:SHOW";

    DisplayMetrics displayMetrics = new DisplayMetrics();
    WebView wv;
    WebSettings settings;
    ProgressBar progressBar;
    int index;
    boolean referredPageLoaded = true;
    boolean loadFromCache = true;
    boolean saveToCache = false;
    boolean returnOnLoaded = false;
    boolean loadedFromCacheFile = false;
    int shouldExecuteScript = 0;
    int host = 0;

    EditText urlBar;

    private void saveCurrentViewToCache(boolean callPageActuallyloaded) {
        wv.saveWebArchive(VData.getWebArchiveFileName(this, VData.vdb.get(index).no), false, v -> {
            if (callPageActuallyloaded) pageActuallyLoaded();
        });
        VData.cached(this, index);
        saveToCache = false;
        setSaveButtonStyle();
    }

    void pageActuallyLoaded() {
        progressBar.setVisibility(View.GONE);
        if (saveToCache) {
            saveCurrentViewToCache(true);
            return;
        }
        if (referredPageLoaded) findViewById(R.id.saveButton).setEnabled(true);
        if (returnOnLoaded) {
            finish();
        }
        hideUrlBarAfter(1000, true);
    }
    void pageLoadFinished() {
        if (loadedFromCacheFile) {
            pageActuallyLoaded();
        } else if (shouldExecuteScript>0) {
            String script1 = "" +
                    "document.getElementsByClassName(\"extrabtn\")[document.getElementsByClassName(\"audiobtn\").length].remove();" +
                    "document.getElementsByClassName(\"print\")[0].remove();" +
                    "document.getElementsByClassName(\"heateor_sss_sharing_container\")[0].remove();" +
                    "document.getElementsByClassName(\"mobilemenu\")[0].remove();" +
                    "el=document.getElementsByClassName(\"virrenkuva\")[0];" +
                    "nel=el.cloneNode(true);nel.className=\"virrenkuva\";el.after(nel);" +
                    "nel.style=\"position:sticky;top:0;z-index:1;\";el.remove();" +
                    "document.getElementById(\"js-show-image\").onclick=function(){document.getElementById(\"js-song-image\").style.display=\"inline\";};" +
                    "document.getElementById(\"js-hide-image\").onclick=function(){document.getElementById(\"js-song-image\").style.display=\"none\";};" +
                    "";
            String script2 = "imgs=document.getElementsByClassName(\"nimg\");for(i=0;i<imgs.length;i++){imgs[i].style.minHeight=\"0\";}" +
                    "ngs=document.getElementsByClassName(\"noteimage\");for(i=0;i<ngs.length;i++){ngs[i].style.maxHeight=\"50vh\";ngs[i].style.height=\"auto\";ngs[i].style.top=\"0px\";ngs[i].style.position=\"static\";}" +
                    "e=document.getElementById(\"noteimagecont\");e.style.position=\"sticky\";e.style.top=\"36px\";" +
                    "t=document.getElementById(\"textpart\");t.style.marginTop=\"44px\";" +
                    "if (document.getElementById(\"noteimage2\")) document.getElementById(\"noteimage\").id=\"noteimage1\";" +
                    "";
            wv.evaluateJavascript(shouldExecuteScript==1?script1:script2, value -> {
                shouldExecuteScript = 0;
                pageLoadFinished();
            });
        } else {
            pageActuallyLoaded();
        }
    }

    private void setSaveButtonStyle() {
        ImageButton cb = findViewById(R.id.saveButton);
        if (VData.vdb.get(index).cached)  cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.light_button_color_state_list_active));
        else cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.light_button_color_state_list_inactive));
    }
    private void saveButtonClicked() {
        if (index<0) return;
        if (toast != null) toast.cancel();
        if (VData.vdb.get(index).cached) {
            toast = Toast.makeText(this, "Sivun offline-tallennus poistettu.", Toast.LENGTH_SHORT);
            toast.show();
            VData.uncache(this, index);
        } else {
            saveCurrentViewToCache(false);
            toast = Toast.makeText(this, "Sivu tallennettu offline-tilassa käytettäväksi.", Toast.LENGTH_SHORT);
            toast.show();
        }
        setSaveButtonStyle();
    }

    private void setFavouriteButtonStyle() {
        ImageButton cb = findViewById(R.id.favouriteButton);
        Log.d(TAG, "set Favourite style "+VData.vdb.get(index).favourite);
        if (VData.vdb.get(index).favourite)  cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.light_button_color_state_list_active));
        else cb.setImageTintList(ContextCompat.getColorStateList(this, R.color.light_button_color_state_list_inactive));
        //cb.setVisibility(View.GONE);
    }
    private void favouriteButtonClicked() {
        if (index<0) return;
        VData.toggleFavourite(this, index);
        if (toast != null) toast.cancel();
        if (VData.vdb.get(index).favourite) {
            toast = Toast.makeText(this, "Virsi lisätty suosikkeihin.", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            toast = Toast.makeText(this, "Virsi poistettu suosikeista.", Toast.LENGTH_SHORT);
            toast.show();
        }
        setFavouriteButtonStyle();
    }
    void prepareLoadPage(String url) {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(100);
        progressBar.setProgress(1);
        urlBar.setText(url);
        if (loadedFromCacheFile) {
            findViewById(R.id.offlineSymbol).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.offlineSymbol).setVisibility(View.GONE);
        }
        findViewById(R.id.saveButton).setEnabled(false);
        if (referredPageLoaded) {
            findViewById(R.id.favouriteButton).setEnabled(true);
        } else {
            findViewById(R.id.favouriteButton).setEnabled(false);
        }
        makeUrlBarVisible();
        setFavouriteButtonStyle();
        setSaveButtonStyle();
    }
    private void reload() {
        shouldExecuteScript = host;
        loadedFromCacheFile = false;
        referredPageLoaded = true;
        prepareLoadPage(VData.vdb.get(index).url);
        wv.loadUrl(VData.vdb.get(index).url);
    }

    private void load() {
        referredPageLoaded = true;
        if (loadFromCache && VData.vdb.get(index).cached) {
            if (returnOnLoaded) finish();
            settings.setAllowFileAccess(true);
            loadedFromCacheFile = true;
            saveToCache = false;
            shouldExecuteScript = 0;
            Log.d(TAG, "Load cached archive");
            prepareLoadPage(VData.vdb.get(index).url);
            wv.loadUrl(VData.getWebArchiveFileName(this, VData.vdb.get(index).no));
            return;
        }
        loadedFromCacheFile = false;
        shouldExecuteScript = host;
        prepareLoadPage(VData.vdb.get(index).url);
        wv.loadUrl(VData.vdb.get(index).url);
    }
    private void refreshButtonClicked() {
        reload();
    }
    private void navigateToNext() {
        if (index+1 < VData.vdb.size()) {
            index++;
            load();
        }
    }
    private void navigateToPrevious() {
        if (index-1 >= 0) {
            index--;
            load();
        }
    }

    int zoom;
    private void applyTextZoom() {
        settings.setTextZoom(zoom);
    }
    private void increaseFontSizeButtonClicked() {
        if (zoom<500) zoom+=20;
        applyTextZoom();
        VData.setIntValueToSettings("textZoom", zoom);
    }
    private void decreaseFontSizeButtonClicked() {
        if (zoom>20) zoom-=20;
        applyTextZoom();
        VData.setIntValueToSettings("textZoom", zoom);
    }


    private class GestureHintView extends View {
        // left, top, bottom, down
        float[] fullness = new float[4];
        float move = 170;
        int size = 60;
        public void setFullness(float left,  float top, float right) {
            if (left==fullness[0] && top==fullness[1] && right==fullness[2]) return;
            fullness[0]=left;
            fullness[1]=top;
            fullness[2]=right;
            invalidate();
        }

        int getTintColor(float fullness) {
            if (fullness>=1) return 0xffcccccc;
            else  return 0xcccccc | ((int)(128*fullness+30)<<24);
        }

        int getMovedPos(float fullness) {
            return (int)(move*fullness);
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            int midX = displayMetrics.widthPixels/2;
            int midY = displayMetrics.heightPixels/2;
            if (fullness[0]>0) {
                int x = getMovedPos(fullness[0]);
                Drawable d = getResources().getDrawable(R.drawable.chevron_left_circle);
                d.setTint(getTintColor(fullness[0]));
                d.setBounds(x-size, midY-size, x+size, midY+size);
                d.draw(c);
            }
            if (fullness[1]>0) {
                int y = getMovedPos(fullness[1]);
                Drawable d = getResources().getDrawable(R.drawable.refresh);
                d.setTint(getTintColor(fullness[1]));
                d.setBounds(midX-size, y-size, midX+size, y+size);
                d.draw(c);
            }
            if (fullness[2]>0) {
                int x = displayMetrics.widthPixels - getMovedPos(fullness[2]);
                Drawable d = getResources().getDrawable(R.drawable.chevron_right_circle);
                d.setTint(getTintColor(fullness[2]));
                d.setBounds(x-size, midY-size, x+size, midY+size);
                d.draw(c);
            }
        }

        public GestureHintView(Context context) {
            super(context);
        }
    }
    GestureHintView ghv;
    float motionStartX;
    float motionStartY;

    boolean motionCanRefresh;
    float motionCanRefreshY;
    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        float showLeft = 0;
        float showRight = 0;
        float showTop = 0;
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            motionStartX = e.getRawX();
            motionStartY = e.getRawY();
            if (wv.getScrollY()==0) {
                motionCanRefresh = true;
                motionCanRefreshY = motionStartY;
            } else motionCanRefresh = false;
        } else if (e.getAction() == MotionEvent.ACTION_MOVE || e.getAction() == MotionEvent.ACTION_UP) {
            if (wv.getScrollY() == 0 && !motionCanRefresh) {
                motionCanRefresh = true;
                motionCanRefreshY = e.getRawY();
            }
            float dx = e.getRawX() - motionStartX;
            float dy = e.getRawY() - motionStartY;
            float adx = dx>0?dx:-dx;
            float ady = dy>0?dy:-dy;
            float threshold = (float)(displayMetrics.widthPixels*0.25);
            if (adx>ady) {
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    if (dx>0) {
                        showLeft = adx>=threshold?1:adx/threshold;
                    } else {
                        showRight = adx>=threshold?1:adx/threshold;
                    }
                } else if (adx>=threshold) {
                    if (dx<0) navigateToNext();
                    else      navigateToPrevious();
                }
            } else if (motionCanRefresh && dy>0) {
                Log.d(TAG, "Motion can refresh");
                dy = e.getRawY() - motionCanRefreshY;
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    showTop = dy>=threshold?1:dy/threshold;
                } else if (dy>=threshold) {
                    refreshButtonClicked();
                }
            }
        }
        ghv.setFullness(showLeft, showTop, showRight);
        return super.dispatchTouchEvent(e);
    }

    void openInBrowserClicked() {
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(((EditText)findViewById(R.id.urlbar)).getText().toString()));
        startActivity(intent);
    }
    int urli = 0;
    boolean urlContainerHidden = false;

    void makeUrlBarVisible() {
        View view = findViewById(R.id.urlbarcontainer);
        view.setVisibility(View.VISIBLE);
        view.animate().translationY(0).setDuration(100);
        urlContainerHidden = false;
        ++urli;
    }
    void hideUrlBarAfter(int ms, boolean makeVisibleIfAlreadyHidden) {
        View view = findViewById(R.id.urlbarcontainer);
        if (makeVisibleIfAlreadyHidden && urlContainerHidden) {
            makeUrlBarVisible();
        } else if (urlContainerHidden) return;
        ++urli;
        int v = urli;
        if (ms>0) {
            view.postDelayed(() -> {
                if (v == urli) {
                    urlContainerHidden = true;
                    view.animate().translationY(-view.getHeight()).setDuration(300);
                }
            }, ms);
        } else {
            urlContainerHidden = true;
            view.animate().translationY(-view.getHeight()).setDuration(300);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        index = intent.getIntExtra("index", 0);
        loadFromCache = intent.getBooleanExtra("loadFromCache", false);
        saveToCache = intent.getBooleanExtra("saveToCache", false);
        returnOnLoaded = intent.getBooleanExtra("instantReturn", false);

        if (VData.vdb.get(index).url.contains("virsikirja.fi")) host = 1;
        else host = 2;

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        setContentView(R.layout.activity_show);
        wv = findViewById(R.id.webview);
        wv.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            if (scrollY==0) hideUrlBarAfter(3500, true);
            else            hideUrlBarAfter(0, false);
        });

        settings = wv.getSettings();
        if (intent.getBooleanExtra("useWebViewCache", false)) {
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        settings.setJavaScriptEnabled(true);
        zoom = VData.getIntSetting("textZoom", settings.getTextZoom());
        applyTextZoom();

        progressBar = findViewById(R.id.progressbar);
        urlBar = findViewById(R.id.urlbar);

        findViewById(R.id.saveButton).setOnClickListener(v -> saveButtonClicked());
        findViewById(R.id.favouriteButton).setOnClickListener(v -> favouriteButtonClicked());
        findViewById(R.id.refreshButton).setOnClickListener(v -> refreshButtonClicked());

        findViewById(R.id.increaseFontSize).setOnClickListener(v -> increaseFontSizeButtonClicked());
        findViewById(R.id.decreaseFontSize).setOnClickListener(v -> decreaseFontSizeButtonClicked());
        findViewById(R.id.openInBrowser).setOnClickListener(v -> openInBrowserClicked());


        ghv = new GestureHintView(this);
        addContentView(ghv, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        wv.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                pageLoadFinished();
            }
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                saveToCache = false;
                Log.d(TAG, "disable cache due to error..."+errorCode +" "+description);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                new AlertDialog.Builder(ShowActivity.this)
                        .setTitle("SSL-varmennnus epäonnistui.")
                        .setMessage("SSL-varmennus epäonnistui. Haluatko jatkaa? \n\n"+error)
                        .setPositiveButton("Jatka", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                handler.proceed();
                            }
                        })
                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton("Älä jatka.", null)
                        .show();
            }
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                Log.d(TAG, "overrideUrlLoading: "+request.getUrl().toString());
                if (host.equals("virsikirja.fi") || host.equals("www.lhpk.fi")) {
                    if (host.equals("virsikirja.fi")) shouldExecuteScript = 1;
                    else shouldExecuteScript = 2;
                    loadedFromCacheFile = false;
                    if (!request.getUrl().toString().equals(VData.vdb.get(index).url)) {
                        int vi = VData.findByUrl(request.getUrl().toString());
                        if (vi==-1) {
                            referredPageLoaded = false;
                        } else {
                            referredPageLoaded = true;
                            index = vi;
                        }
                    } else {
                        referredPageLoaded = true;
                    }
                    prepareLoadPage(request.getUrl().toString());
                    return false;
                }
                final Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                startActivity(intent);
                return true;
            }
        });
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                progressBar.setProgress(progress);
            }
        });

        load();
    }

    @Override
    protected void onPause() {
        wv.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        wv.onResume();
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        wv.destroy();
        super.onDestroy();
    }
}
