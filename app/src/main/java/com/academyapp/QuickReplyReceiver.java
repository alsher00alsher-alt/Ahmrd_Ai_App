package com.academyapp;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.core.app.RemoteInput;

public class QuickReplyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if (remoteInput != null) {
            CharSequence replyText = remoteInput.getCharSequence("key_text_reply");
            if (replyText != null) {
                String text = replyText.toString();
                String convId = intent.getStringExtra("convId");
                Intent openApp = new Intent(context, MainActivity.class);
                openApp.putExtra("quickReply", text);
                openApp.putExtra("convId", convId);
                openApp.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(openApp);
            }
        }
    }
}