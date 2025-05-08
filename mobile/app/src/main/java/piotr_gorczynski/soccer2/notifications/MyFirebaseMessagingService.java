package piotr_gorczynski.soccer2.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import piotr_gorczynski.soccer2.R;

import android.app.PendingIntent;
import android.content.Intent;

import piotr_gorczynski.soccer2.InvitationsActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("TAG_Soccer", "🔐 New FCM token: " + token);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("TAG_Soccer", "✅ Token saved"))
                    .addOnFailureListener(e -> Log.e("TAG_Soccer", "❌ Failed to save token", e));
        } else {
            Log.w("Soccer", "⚠️ No user logged in; token not saved");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("TAG_Soccer", "📨 Message received: " + remoteMessage.getData());

        Context context = getApplicationContext();

        // 1. Extract everything from the data payload
        String title        = remoteMessage.getData().get("title");
        String body         = remoteMessage.getData().get("body");
        String fromNickname = remoteMessage.getData().get("fromNickname");
        // Safely generate a notification ID
        String inviteIdRaw   = remoteMessage.getData().get("inviteId");
        String inviteId      = (inviteIdRaw != null
                ? inviteIdRaw
                : String.valueOf(System.currentTimeMillis()));
        int notificationId   = inviteId.hashCode();

        // 2. Build an Intent to open InvitationsActivity
        Intent inviteIntent = new Intent(context, InvitationsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        inviteIntent.putExtra("fromNickname", fromNickname);
        inviteIntent.putExtra("inviteId", inviteId);

        // 3. Create a direct PendingIntent (no TaskStackBuilder here)
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                inviteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 4. Ensure the notification channel exists (Oreo+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "invite_channel",
                    "Game Invites",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }

        // 5. Build and show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "invite_channel")
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // 6. Show it—but only if POST_NOTIFICATIONS permission is granted on Android 13+
        // Permission‐guarded notify() (Android 13+)
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            nm.notify(notificationId, builder.build());
        } else {
            Log.w("FCM", "Missing POST_NOTIFICATIONS permission");
        }

    }
}
