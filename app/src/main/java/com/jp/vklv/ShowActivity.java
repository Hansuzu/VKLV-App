package com.jp.vklv;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toolbar;

import java.io.File;
import java.io.RandomAccessFile;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;


public class ShowActivity extends AppCompatActivity {
    String TAG = "VK+LV:SHOW";
    WebView wv;
    ProgressBar progressBar;
    int index;
    boolean referredPageLoaded = true;
    boolean loadFromCache = true;
    boolean saveToCache = false;
    boolean returnOnLoaded = false;
    boolean loadedFromCacheFile = false;
    boolean shouldExecuteScript = false;

    private void saveCurrentViewToCache() {
        wv.saveWebArchive(VData.webArchiveFile(this, VData.vdb.get(index).no));
        VData.cached(this, index);
        saveToCache = false;
        setSaveButtonStyle();
    }

    void pageActuallyLoaded() {
        progressBar.setVisibility(View.GONE);
        if (saveToCache) saveCurrentViewToCache();
        if (referredPageLoaded) findViewById(R.id.saveButton).setEnabled(true);
        if (returnOnLoaded) {
            finish();
        }
    }
    void pageLoadFinished() {
        if (loadedFromCacheFile) {
            pageActuallyLoaded();
        } else if (shouldExecuteScript) {
            String script = "el=document.getElementsByClassName(\"virrenkuva\")[0];" +
                    "nel=el.cloneNode(true);nel.className=\"virrenkuva\";el.after(nel);" +
                    "nel.style=\"position:sticky;top:0;z-index:1;\";el.remove();" +
                    "document.getElementById(\"js-show-image\").onclick=function(){document.getElementById(\"js-song-image\").style.display=\"inline\";};" +
                    "document.getElementById(\"js-hide-image\").onclick=function(){document.getElementById(\"js-song-image\").style.display=\"none\";};";
            wv.evaluateJavascript(script, new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    shouldExecuteScript = false;
                    pageLoadFinished();
                }
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
        if (VData.vdb.get(index).cached) {
            VData.uncache(this, index);
        } else {
            saveCurrentViewToCache();
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
        setFavouriteButtonStyle();
    }
    void prepareLoadPage() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(100);
        progressBar.setProgress(1);
        findViewById(R.id.saveButton).setEnabled(false);
        if (referredPageLoaded) {
            findViewById(R.id.favouriteButton).setEnabled(true);
        } else {
            findViewById(R.id.favouriteButton).setEnabled(false);
        }
        setFavouriteButtonStyle();
        setSaveButtonStyle();
    }
    private void refreshButtonClicked() {
        shouldExecuteScript = true;
        loadedFromCacheFile = false;
        referredPageLoaded = true;
        prepareLoadPage();
        wv.loadUrl(VData.vdb.get(index).url);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        index = intent.getIntExtra("index", 0);
        loadFromCache = intent.getBooleanExtra("loadFromCache", false);
        saveToCache = intent.getBooleanExtra("saveToCache", false);
        returnOnLoaded = intent.getBooleanExtra("instantReturn", false);

        setContentView(R.layout.activity_show);
        wv = findViewById(R.id.webview);

        WebSettings settings = wv.getSettings();
        if (intent.getBooleanExtra("useWebViewCache", false)) {
            settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        }
        settings.setJavaScriptEnabled(true);

        progressBar = findViewById(R.id.progressbar);

        findViewById(R.id.saveButton).setOnClickListener(v -> saveButtonClicked());
        findViewById(R.id.favouriteButton).setOnClickListener(v -> favouriteButtonClicked());
        findViewById(R.id.refreshButton).setOnClickListener(v -> refreshButtonClicked());


        wv.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                pageLoadFinished();
            }
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                saveToCache = false;
                //Log.d("a", "disable cache due to error..."+Integer.toString(errorCode) +" "+description);
            }
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String host = request.getUrl().getHost();
                Log.d(TAG, "overrideUrlLoading: "+request.getUrl().toString());
                loadedFromCacheFile = false;
                shouldExecuteScript = true;
                if (host.equals("virsikirja.fi") || host.equals("www.lhpk.fi")) {
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
                    prepareLoadPage();
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


        referredPageLoaded = true;
        prepareLoadPage();


        if (loadFromCache && VData.vdb.get(index).cached) {
            if (returnOnLoaded) finish();
            settings.setAllowFileAccess(true);
            loadedFromCacheFile = true;
            saveToCache = false;
            shouldExecuteScript = false;
            Log.d(TAG, "Load cached archive");
            wv.loadUrl(VData.webArchiveFile(this, VData.vdb.get(index).no));
            return;
        }
        loadedFromCacheFile = false;
        shouldExecuteScript = true;
        wv.loadUrl(VData.vdb.get(index).url);
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
