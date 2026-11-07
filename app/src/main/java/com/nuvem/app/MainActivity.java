package com.nuvem.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.webkit.HttpAuthHandler;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.net.URLDecoder;

public class MainActivity extends Activity {
    static final String URL = "http://localhost:8000";
    static final String USER = "admin";
    static final String PASS = "7hW0Gk0aktCp0N";

    WebView web;
    ValueCallback<Uri[]> chooser;
    int tentativas = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.parseColor("#0b0b14"));
        getWindow().setNavigationBarColor(Color.parseColor("#0b0b14"));
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0b0b14"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedHttpAuthRequest(WebView v, HttpAuthHandler h, String host, String realm) {
                if (tentativas++ < 3) h.proceed(USER, PASS); else h.cancel();
            }
            @Override
            public void onPageFinished(WebView v, String u) {
                tentativas = 0;
            }
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    v.loadDataWithBaseURL(null,
                        "<body style='background:#0b0b14;color:#fff;font-family:sans-serif;text-align:center;padding-top:30vh'>"
                        + "<h2>Servidor offline</h2><p>Abra o Termux e inicie o servidor.</p>"
                        + "<button style='font-size:18px;padding:12px 24px' onclick='location.href=\"" + URL + "\"'>Tentar de novo</button>",
                        "text/html", "utf-8", null);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView w, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try {
                    startActivityForResult(p.createIntent(), 1);
                } catch (Exception e) {
                    chooser = null;
                    return false;
                }
                return true;
            }
        });

        web.setDownloadListener((url, ua, cd, mime, len) -> {
            String nome = null;
            if (cd != null) {
                int i = cd.indexOf("filename*=UTF-8''");
                if (i >= 0) {
                    try {
                        nome = URLDecoder.decode(cd.substring(i + 17).split(";")[0].trim(), "UTF-8");
                    } catch (Exception e) { }
                }
            }
            if (nome == null) nome = URLUtil.guessFileName(url, cd, mime);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(url));
            r.addRequestHeader("Authorization", "Basic "
                + Base64.encodeToString((USER + ":" + PASS).getBytes(), Base64.NO_WRAP));
            r.setTitle(nome);
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nome);
            ((DownloadManager) getSystemService(DOWNLOAD_SERVICE)).enqueue(r);
        });

        web.loadUrl(URL);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 1 && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
