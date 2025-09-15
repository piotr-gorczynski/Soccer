package piotr_gorczynski.soccer2.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Source;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import piotr_gorczynski.soccer2.GameActivity;
import piotr_gorczynski.soccer2.R;

import android.app.PendingIntent;
import android.content.Intent;

import java.util.Objects;

import piotr_gorczynski.soccer2.InvitationsActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private void maybeLaunchGame(@NonNull String matchPath,
                                 @NonNull String localNickname) {

        DocumentReference docRef = FirebaseFirestore.getInstance()
                .document(matchPath);

        // Force a round-trip to the backend so we never rely on a stale cache.
        docRef.get(Source.SERVER)                // server read, not cache
                .addOnSuccessListener(snapshot -> {
                    boolean shouldStart =
                            snapshot.exists()
                                    && !"completed".equals(snapshot.getString("status"));

                    if (shouldStart) {
                        // Instead of directly starting activity (which causes RemoteServiceException),
                        // show a notification that allows user to tap to join the game
                        showGameReadyNotification(matchPath, localNickname);
                    } else {
                        Log.i("TAG_Soccer",
                                "Match " + matchPath + " is already finished - skipping launch");
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer",
                                "Could not check match status – defaulting to *not* launching",
                                e));
    }
    
    /**
     * Show a notification that allows user to tap to join a game.
     * This replaces direct startActivity() calls which are restricted in background services.
     */
    private void showGameReadyNotification(@NonNull String matchPath, 
                                          @NonNull String localNickname) {
        Context context = getApplicationContext();
        
        // Create intent for the game activity
        Intent gameIntent = new Intent(context, GameActivity.class)
                .putExtra("GameType", 3)
                .putExtra("matchPath", matchPath)
                .putExtra("localNickname", localNickname)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // Create pending intent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                matchPath.hashCode(), // unique ID based on match path
                gameIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Ensure notification channel exists (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "game_ready_channel",
                    "Game Ready",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications when a game is ready to join");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
        
        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "game_ready_channel")
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(getString(R.string.game_ready_title, localNickname))
                .setContentText(getString(R.string.game_ready_message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_notifications, 
                          getString(R.string.join_game_action), 
                          pendingIntent);
        
        // Show notification with permission check
        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            
            int notificationId = ("game_ready_" + matchPath).hashCode();
            nm.notify(notificationId, builder.build());
            
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".showGameReadyNotification: " +
                    "Game ready notification shown for match: " + matchPath);
        } else {
            Log.w("TAG_Soccer", getClass().getSimpleName() + ".showGameReadyNotification: " +
                    "Missing POST_NOTIFICATIONS permission - cannot show game ready notification");
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": 🔐 New FCM token: " + token);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ✅ Token saved"))
                    .addOnFailureListener(e -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to save token", e));
        } else {
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ⚠️ No user logged in; token not saved");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": 📨 Message received: " + remoteMessage.getData());
        if ("start".equals(remoteMessage.getData().get("type"))) {
            Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".onMessageReceived: start message ignored (no auto-launch)"
            );
            return;  // ignore legacy start push
        }

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
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Missing POST_NOTIFICATIONS permission");
        }

    }
}
