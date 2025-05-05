package piotr_gorczynski.soccer2.notifications;

import android.Manifest;
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
        Log.d("FCM", "🔐 New FCM token: " + token);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "✅ Token saved"))
                    .addOnFailureListener(e -> Log.e("FCM", "❌ Failed to save token", e));
        } else {
            Log.w("FCM", "⚠️ No user logged in; token not saved");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("FCM", "📨 Message received: " + remoteMessage.getData());

        Context context = getApplicationContext();

        // Intent to open InvitationsActivity
        Intent intent = new Intent(context, InvitationsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "invite_channel")
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Game Invite")
                .setContentText("You received an invite from " + remoteMessage.getData().get("fromNickname"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)  // ✅ add this line
                .setAutoCancel(true);

        // Show it
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1001, builder.build());
        } else {
            Log.w("FCM", "❌ Notification permission not granted");
        }


    }
}
