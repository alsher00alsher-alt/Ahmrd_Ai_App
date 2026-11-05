package com.academyapp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class CallActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int notifId = intent.getIntExtra("notifId", 0);
        
        if ("com.academyapp.REJECT_CALL".equals(action)) {
            String channelName = intent.getStringExtra("channelName");
            // إغلاق الإشعار
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null && notifId != 0) nm.cancel(notifId);
            
            // إرسال إشارة الرفض للسيرفر (اختياري)
            if (channelName != null) {
                new Thread(() -> {
                    try {
                        URL url = new URL("https://moon-dash-theta.vercel.app/api/reject-call");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        String json = "{\"channelName\":\"" + channelName + "\"}";
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(json.getBytes(StandardCharsets.UTF_8));
                        }
                        conn.getResponseCode();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }
}
