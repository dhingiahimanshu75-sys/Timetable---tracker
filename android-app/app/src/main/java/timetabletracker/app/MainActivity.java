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
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        swipeRefresh = new SwipeRefreshLayout(this);
        webView = new WebView(this);
        swipeRefresh.addView(webView);
        setContentView(swipeRefresh);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true); s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true); s.setAllowContentAccess(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { swipeRefresh.setRefreshing(false); }
        });
        swipeRefresh.setOnRefreshListener(() -> webView.reload());
        webView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> swipeRefresh.setEnabled(scrollY == 0));
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });
        if (android.os.Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        webView.loadUrl("https://dhingiahimanshu75-sys.github.io/Timetable---tracker/");
    }
    @Override public void onBackPressed() { if (webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
