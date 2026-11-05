package com.academyapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private ValueCallback<Uri[]> mFilePathCallback;
    private static final int FILE_CHOOSER_RESULT_CODE = 100;
    private static final int PERMISSION_REQUEST_CODE = 101;
    // الرابط الجديد
    private static final String WEB_URL = "https://ahmed-viggle.vercel.app/";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.hide(WindowInsetsCompat.Type.displayCutout());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_main);
        
        webView = findViewById(R.id.webview);
        if (webView == null) {
            webView = new WebView(this);
            setContentView(webView);
        }

        requestPermissions();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT); // تفعيل الكاش للأوفلاين

        webView.addJavascriptInterface(new WebAppInterface(), "AndroidApp");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = filePathCallback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "اختار ملف"), FILE_CHOOSER_RESULT_CODE);
                } catch (Exception e) {
                    mFilePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(() -> request.grant(request.getResources()));
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadUrl("file:///android_asset/offline.html");
                }
            }
        });

        // إعداد مستمع التحميل
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            new DownloadFileTask().execute(url);
        });

        webView.loadUrl(WEB_URL);
    }

    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            java.util.List<String> list = new java.util.ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(android.Manifest.permission.READ_MEDIA_IMAGES);
                list.add(android.Manifest.permission.READ_MEDIA_VIDEO);
                list.add(android.Manifest.permission.READ_MEDIA_AUDIO);
            } else {
                list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE);
                list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            list.add(android.Manifest.permission.CAMERA);
            list.add(android.Manifest.permission.RECORD_AUDIO);
            
            String[] perms = list.toArray(new String[0]);
            ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST_CODE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (mFilePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                String dataString = data.getDataString();
                if (dataString != null) results = new Uri[]{Uri.parse(dataString)};
            }
            mFilePathCallback.onReceiveValue(results);
            mFilePathCallback = null;
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public class WebAppInterface {
        @JavascriptInterface
        public void saveUser(String uid, String name, String phone, String photoURL) {
            SharedPreferences prefs = getSharedPreferences("app_user", MODE_PRIVATE);
            prefs.edit().putString("uid", uid).putString("name", name).apply();
        }
        @JavascriptInterface
        public void downloadFile(String url, String filename) {
            runOnUiThread(() -> new DownloadFileTask().execute(url));
        }
    }

    // كلاس التحميل مع شريط التقدم والسرعة
    private class DownloadFileTask extends AsyncTask<String, Integer, String> {
        private android.app.AlertDialog progressDialog;
        private android.widget.TextView tvProgress, tvSpeed, tvSize;
        private android.widget.ProgressBar progressBar;
        private long startTime;
        private long lastBytes = 0;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            
            // تصميم مخصص للدايلوج
            android.widget.LinearLayout layout = new android.widget.LinearLayout(MainActivity.this);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            layout.setPadding(50, 40, 50, 40);
            layout.setBackgroundColor(0xFF111827); // لون داكن

            android.widget.TextView title = new android.widget.TextView(MainActivity.this);
            title.setText("جاري التحميل...");
            title.setTextColor(0xFFFFFFFF);
            title.setTextSize(18);
            title.setGravity(android.view.Gravity.CENTER);
            layout.addView(title);

            progressBar = new android.widget.ProgressBar(MainActivity.this, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.setProgress(0);
            progressBar.getProgressDrawable().setColorFilter(0xFF6366F1, android.graphics.PorterDuff.Mode.SRC_IN); // لون بنفسجي
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 30, 0, 20);
            layout.addView(progressBar, params);

            tvProgress = new android.widget.TextView(MainActivity.this);
            tvProgress.setText("0%");
            tvProgress.setTextColor(0xFF6366F1);
            tvProgress.setTextSize(24);
            tvProgress.setGravity(android.view.Gravity.CENTER);
            layout.addView(tvProgress);

            tvSize = new android.widget.TextView(MainActivity.this);
            tvSize.setText("0 MB / 0 MB");
            tvSize.setTextColor(0xFF94A3B8);
            tvSize.setGravity(android.view.Gravity.CENTER);
            layout.addView(tvSize);

            tvSpeed = new android.widget.TextView(MainActivity.this);
            tvSpeed.setText("0.0 MB/s");
            tvSpeed.setTextColor(0xFF10B981);
            tvSpeed.setGravity(android.view.Gravity.CENTER);
            layout.addView(tvSpeed);

            progressDialog = new android.app.AlertDialog.Builder(MainActivity.this)
                .setView(layout)
                .setCancelable(false)
                .setNegativeButton("إلغاء", (dialog, which) -> {
                    dialog.dismiss();
                    cancel(true);
                })
                .create();
            progressDialog.show();
            startTime = System.currentTimeMillis();
        }
        
        @Override
        protected String doInBackground(String... urls) {
            String urlStr = urls[0];
            String fileName = android.webkit.URLUtil.guessFileName(urlStr, null, null);
            
            // المسار الجديد
            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AHMED PRO");
            if (!downloadDir.exists()) downloadDir.mkdirs();
            File file = new File(downloadDir, fileName);
            
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);
                conn.connect();
                
                int fileLength = conn.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream(), 8192);
                OutputStream output = new FileOutputStream(file);
                byte data[] = new byte[1024];
                long total = 0;
                int count;
                long lastUpdate = 0;
                
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) break;
                    total += count;
                    output.write(data, 0, count);
                    long now = System.currentTimeMillis();
                    if (now - lastUpdate > 300) {
                        lastUpdate = now;
                        long speed = (total - lastBytes) / ((now - startTime + 1) / 1000 + 1);
                        publishProgress((int) total, fileLength, (int) speed);
                    }
                }
                
                output.flush();
                output.close();
                input.close();
                conn.disconnect();
                
                if (fileLength > 0 && total < fileLength) return null;
                return file.getAbsolutePath();
                
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            int downloaded = values[0];
            int total = values[1];
            int speed = values[2];
            
            if (total > 0) {
                int percent = (int) (downloaded * 100.0 / total);
                progressBar.setProgress(percent);
                tvProgress.setText(percent + "%");
                tvSize.setText(String.format("%.1f MB / %.1f MB", downloaded / (1024.0 * 1024.0), total / (1024.0 * 1024.0)));
                tvSpeed.setText(String.format("%.1f MB/s", speed / (1024.0 * 1024.0)));
            } else {
                tvSize.setText(String.format("%.1f MB", downloaded / (1024.0 * 1024.0)));
            }
        }
        
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            if (result != null) {
                Toast.makeText(MainActivity.this, "✅ تم الحفظ في مجلد AHMED PRO", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, "❌ فشل التحميل", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.hide(WindowInsetsCompat.Type.displayCutout());
            controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
    }
    

    @Override
    protected void onResume() {
        super.onResume();
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.hide(WindowInsetsCompat.Type.displayCutout());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
    

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

}