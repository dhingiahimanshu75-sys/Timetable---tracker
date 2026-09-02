package com.timetabletracker.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.app.NotificationCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends Activity {
    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private static final int CAMERA_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;
    private static final String APP_URL = "https://dhingiahimanshu75-sys.github.io/Timetable---tracker/";
    private static final String CHANNEL_ID = "timetable_reminders";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createNotificationChannel();
        swipeRefresh = new SwipeRefreshLayout(this);
        webView = new WebView(this);
        swipeRefresh.addView(webView);
        setContentView(swipeRefresh);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidNotify");

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
            // Reload the existing app page so the latest GitHub Pages code is used.
            // DOM storage is kept enabled; no WebView data/cookies/cache are cleared.
            swipeRefresh.setRefreshing(true);
            webView.reload();
            webView.postDelayed(() -> swipeRefresh.setRefreshing(false), 10000);
        });

        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_REQUEST);
        webView.loadUrl(APP_URL);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Timetable reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Verification window reminders");
            channel.enableVibration(true);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private class AndroidBridge {
        @JavascriptInterface public void refreshDone() {
            runOnUiThread(() -> swipeRefresh.setRefreshing(false));
        }
        @JavascriptInterface public void notifyTask(String taskName) {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
                NotificationManager nm = getSystemService(NotificationManager.class);
                NotificationCompat.Builder b = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)
                    .setContentTitle("⏰ Timetable Reminder")
                    .setContentText(taskName + " ka verification window open hai.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0,300,200,500});
                nm.notify(Math.abs(taskName.hashCode()), b.build());
            });
        }
    }

    private void injectLoginHelpers() {
        webView.evaluateJavascript(
            "(()=>{" +
            "const p=document.getElementById('password');" +
            "if(p&&!document.getElementById('passwordToggle')){" +
            "const b=document.createElement('button');b.id='passwordToggle';b.type='button';b.textContent='👁️';b.setAttribute('aria-label','Show password');" +
            "b.style.cssText='position:absolute;right:12px;top:50%;transform:translateY(-50%);border:0;background:transparent;padding:6px;margin:0;width:auto;min-width:0;font-size:20px;cursor:pointer;';" +
            "const wrap=document.createElement('div');wrap.style.cssText='position:relative;margin-bottom:10px;';p.parentNode.insertBefore(wrap,p);wrap.appendChild(p);p.style.marginBottom='0';p.style.paddingRight='48px';" +
            "b.onclick=()=>{const show=p.type==='password';p.type=show?'text':'password';b.textContent=show?'🙈':'👁️';b.setAttribute('aria-label',show?'Hide password':'Show password');};wrap.appendChild(b);" +
            "}" +
            "if(typeof window.sendTaskNotification==='function'&&!window.sendTaskNotification.__nativeWrapped){" +
            "const old=window.sendTaskNotification;const wrapped=function(task){try{if(window.AndroidNotify)AndroidNotify.notifyTask(String(task?.name||'Task'));}catch(e){};try{return old.apply(this,arguments);}catch(e){}};wrapped.__nativeWrapped=true;window.sendTaskNotification=wrapped;" +
            "}" +
            "})();", null);
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
