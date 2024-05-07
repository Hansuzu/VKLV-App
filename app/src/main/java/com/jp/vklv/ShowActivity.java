package com.jp.vklv;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;


public class ShowActivity extends AppCompatActivity {
    String TAG = "VK+LV:SHOW";
    WebView wv;
    String url;
    String id;
    String title;
    boolean loadFromCache;
    boolean saveToCache;
    boolean returnOnLoaded;
    boolean loadedFromCacheFile = false;
    boolean shouldExecuteScript = false;

    String webArchiveFile() {
        return getCacheDir() + "/" + id + ".mht";
    }

    void pageLoadFinished() {
        if (loadedFromCacheFile) {}
        else if (shouldExecuteScript) {
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
            if (saveToCache) {
                wv.saveWebArchive(webArchiveFile());
                saveToCache = false;
            }
            if (returnOnLoaded) {
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        title = intent.getStringExtra("title");
        id = intent.getStringExtra("id");
        url = intent.getStringExtra("url");
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

        loadedFromCacheFile = false;
        shouldExecuteScript = false;

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
                if (host.equals("virsikirja.fi") || host.equals("www.lhpk.fi")) return false;
                final Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                startActivity(intent);
                return true;
            }
        });
        if (loadFromCache) {
            File file = new File(webArchiveFile());
            if (file.exists()) {
                if (returnOnLoaded) finish();
                settings.setAllowFileAccess(true);
                loadedFromCacheFile = true;
                Log.d(TAG, "Load cached archive");
                wv.loadUrl(webArchiveFile());
                return;
            }
        }
        shouldExecuteScript = true;
        wv.loadUrl(url);
    }

}
