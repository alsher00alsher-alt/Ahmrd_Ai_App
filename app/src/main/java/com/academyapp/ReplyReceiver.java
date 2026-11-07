package com.academyapp;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ReplyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence("key_text_reply");
            String message = replyText != null ? replyText.toString() : "";
            String toUserId = intent.getStringExtra("toUserId");
            String myName = intent.getStringExtra("fromName") != null ? intent.getStringExtra("fromName") : "الطالب";

            if (!message.isEmpty() && toUserId != null) {
                new Thread(() -> {
                    try {
                        URL url = new URL("https://mobile-store-theta-seven.vercel.app/api/reply-notification");
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Content-Type", "application/json");
                        conn.setDoOutput(true);
                        String jsonInputString = "{\"toUserId\":\"" + toUserId + "\",\"text\":\"" + message + "\",\"fromName\":\"" + myName + "\"}";
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(jsonInputString.getBytes(StandardCharsets.UTF_8));
                        }
                        conn.getResponseCode();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.cancelAll();
            }
        }
    }
}
