package com.timetabletracker.app;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private static final int CAMERA_REQUEST = 1001;
    private static final String APP_URL = "https://dhingiahimanshu75-sys.github.io/Timetable---tracker/";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        swipeRefresh = new SwipeRefreshLayout(this);
        webView = new WebView(this);
        swipeRefresh.addView(webView);
        setContentView(swipeRefresh);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
                injectLoginHelpers();
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        swipeRefresh.setOnRefreshListener(() -> {
            // Do NOT reload the whole WebView here. Supabase keeps its login
            // session inside the page's storage; a full reload can unexpectedly
            // send the user back to the login screen. Refresh the live data instead.
            swipeRefresh.setRefreshing(true);
            webView.evaluateJavascript(
                "(async()=>{try{" +
                "if(typeof loadData==='function' && window.user){await loadData();}" +
                "if(typeof ensureProfile==='function' && window.user){await ensureProfile();}" +
                "if(typeof checkExpiredTasks==='function'){await checkExpiredTasks();}" +
                "if(typeof applyRankUp==='function'){await applyRankUp();}" +
                "if(typeof render==='function'){render();}" +
                "if(typeof loadFriendRequests==='function' && window.user){await loadFriendRequests();}" +
                "if(typeof loadFriends==='function' && window.user){await loadFriends();}" +
                "}catch(e){console.error('Native refresh:',e)}finally{window.AndroidRefreshDone&&window.AndroidRefreshDone();}})();",
                null
            );
        });

        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);

        webView.loadUrl(APP_URL);
    }

    private void injectLoginHelpers() {
        webView.evaluateJavascript(
            "(()=>{" +
            "const p=document.getElementById('password');" +
            "if(p&&!document.getElementById('passwordToggle')){" +
            "const b=document.createElement('button');" +
            "b.id='passwordToggle';b.type='button';b.textContent='👁️';" +
            "b.setAttribute('aria-label','Show password');" +
            "b.style.cssText='position:absolute;right:12px;top:50%;transform:translateY(-50%);border:0;background:transparent;padding:6px;margin:0;width:auto;min-width:0;font-size:20px;cursor:pointer;';" +
            "const wrap=document.createElement('div');wrap.style.cssText='position:relative;margin-bottom:10px;';" +
            "p.parentNode.insertBefore(wrap,p);wrap.appendChild(p);" +
            "p.style.marginBottom='0';p.style.paddingRight='48px';" +
            "b.onclick=()=>{const show=p.type==='password';p.type=show?'text':'password';b.textContent=show?'🙈':'👁️';b.setAttribute('aria-label',show?'Hide password':'Show password');};" +
            "wrap.appendChild(b);" +
            "}" +
            "})();",
            null
        );
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
