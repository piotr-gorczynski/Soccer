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

import java.util.Map;
import java.util.Objects;

import piotr_gorczynski.soccer2.InvitationsActivity;
import piotr_gorczynski.soccer2.TournamentLobbyActivity;

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * 
 * This service handles FCM messages and displays notifications to the user.
 * Special care is taken to handle Android 14+ (API 34) CannotDeliverBroadcastException
 * which occurs when the app process is frozen or killed during broadcast delivery.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "TAG_Soccer";
    private static final String CHANNEL_ID = "invite_channel";
    private static final String CHANNEL_NAME = "Game Invites";
    private static final String TOURNAMENT_CHANNEL_ID = "tournament_channel";
    private static final String TOURNAMENT_CHANNEL_NAME = "Tournament Notifications";
    private static final String DEFAULT_TITLE = "Game Invite";
    private static final String DEFAULT_BODY = "You have a new game invitation";
    private static final String DEFAULT_TOURNAMENT_TITLE = "Tournament Update";
    private static final String DEFAULT_TOURNAMENT_BODY = "A tournament has started";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": 🔐 New FCM token: " + token);

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ✅ Token saved"))
                    .addOnFailureListener(e -> Log.e(TAG, getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to save token", e));
        } else {
            Log.w(TAG, getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ⚠️ No user logged in; token not saved");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Wrap entire method in try-catch to handle Android 14+ CannotDeliverBroadcastException
        // and any other system-level exceptions that may occur during broadcast delivery
        try {
            handleMessageReceived(remoteMessage);
        } catch (Throwable t) {
            // Catch Throwable to handle all possible exceptions including system-level ones
            // like RemoteServiceException$CannotDeliverBroadcastException on Android 14+
            Log.e(TAG, getClass().getSimpleName() + ".onMessageReceived: Critical error handling FCM message", t);
        }
    }

    /**
     * Internal method to handle the FCM message processing.
     * Separated from onMessageReceived to ensure proper exception handling.
     */
    private void handleMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Get data early to minimize processing time and reduce risk of CannotDeliverBroadcastException
        Map<String, String> data = remoteMessage.getData();
        
        Log.d(TAG, getClass().getSimpleName() + ".handleMessageReceived: 📨 Message received: " + data);
        
        if ("start".equals(data.get("type"))) {
            Log.d(TAG, getClass().getSimpleName() + ".handleMessageReceived: start message ignored (no auto-launch)");
            return;  // ignore legacy start push
        }

        try {
            showNotification(data);
        } catch (Exception e) {
            // Catch any exceptions during notification display to prevent crash
            Log.e(TAG, getClass().getSimpleName() + ".handleMessageReceived: Failed to show notification", e);
        }
    }

    /**
     * Displays a notification based on the FCM message data.
     * This method is optimized for quick execution to avoid Android 14+ broadcast timeout issues.
     */
    private void showNotification(@NonNull Map<String, String> data) {
        Context context = getApplicationContext();
        if (context == null) {
            Log.e(TAG, getClass().getSimpleName() + ".showNotification: Context is null");
            return;
        }

        // Determine notification type
        String notificationType = data.get("type");
        
        if ("tournament_started".equals(notificationType)) {
            showTournamentNotification(context, data);
        } else {
            showInviteNotification(context, data);
        }
    }

    /**
     * Displays a game invite notification.
     */
    private void showInviteNotification(@NonNull Context context, @NonNull Map<String, String> data) {
        // 1. Extract everything from the data payload with null safety
        String title = extractTitle(data);
        String body = extractBody(data);
        String fromNickname = data.get("fromNickname");
        String inviteId = extractInviteId(data);
        int notificationId = inviteId.hashCode();

        // 2. Ensure the notification channel exists (Oreo+) - do this early
        if (!ensureNotificationChannel(context, CHANNEL_ID, CHANNEL_NAME)) {
            return; // Cannot proceed without notification channel on Oreo+
        }

        // 3. Build an Intent to open InvitationsActivity
        Intent inviteIntent = new Intent(context, InvitationsActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        inviteIntent.putExtra("fromNickname", fromNickname);
        inviteIntent.putExtra("inviteId", inviteId);

        // 4. Create a direct PendingIntent (no TaskStackBuilder here)
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                inviteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 5. Build and show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // 6. Show it—but only if POST_NOTIFICATIONS permission is granted on Android 13+
        displayNotification(context, notificationId, builder);
    }

    /**
     * Displays a tournament notification.
     */
    private void showTournamentNotification(@NonNull Context context, @NonNull Map<String, String> data) {
        // 1. Extract tournament data
        String title = extractTournamentTitle(data);
        String body = extractTournamentBody(data);
        String tournamentId = data.get("tournamentId");
        String tournamentName = data.get("tournamentName");
        
        if (tournamentId == null || tournamentId.isEmpty()) {
            Log.w(TAG, getClass().getSimpleName() + ".showTournamentNotification: tournamentId is missing, cannot show notification");
            return;
        }
        
        int notificationId = tournamentId.hashCode();

        // 2. Ensure the notification channel exists (Oreo+)
        if (!ensureNotificationChannel(context, TOURNAMENT_CHANNEL_ID, TOURNAMENT_CHANNEL_NAME)) {
            return;
        }

        // 3. Build an Intent to open TournamentLobbyActivity
        Intent tournamentIntent = new Intent(context, TournamentLobbyActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        tournamentIntent.putExtra("tournamentId", tournamentId);
        tournamentIntent.putExtra("fromNotification", true);
        if (tournamentName != null && !tournamentName.isEmpty()) {
            tournamentIntent.putExtra("tournamentName", tournamentName);
        }

        // 4. Create a PendingIntent
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                tournamentId.hashCode(), // Use unique request code per tournament
                tournamentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 5. Build and show the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TOURNAMENT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // 6. Show the notification
        displayNotification(context, notificationId, builder);
    }

    /**
     * Extracts and validates the title from FCM data.
     */
    private String extractTitle(@NonNull Map<String, String> data) {
        String title = data.get("title");
        String trimmedTitle = title != null ? title.trim() : "";
        if (trimmedTitle.isEmpty()) {
            Log.w(TAG, getClass().getSimpleName() + ".extractTitle: title was null or empty, using fallback");
            return DEFAULT_TITLE;
        }
        return trimmedTitle;
    }

    /**
     * Extracts and validates the body from FCM data.
     */
    private String extractBody(@NonNull Map<String, String> data) {
        String body = data.get("body");
        String trimmedBody = body != null ? body.trim() : "";
        if (trimmedBody.isEmpty()) {
            Log.w(TAG, getClass().getSimpleName() + ".extractBody: body was null or empty, using fallback");
            return DEFAULT_BODY;
        }
        return trimmedBody;
    }

    /**
     * Extracts and validates the tournament title from FCM data.
     */
    private String extractTournamentTitle(@NonNull Map<String, String> data) {
        String title = data.get("title");
        String trimmedTitle = title != null ? title.trim() : "";
        if (trimmedTitle.isEmpty()) {
            Log.w(TAG, getClass().getSimpleName() + ".extractTournamentTitle: title was null or empty, using fallback");
            return DEFAULT_TOURNAMENT_TITLE;
        }
        return trimmedTitle;
    }

    /**
     * Extracts and validates the tournament body from FCM data.
     */
    private String extractTournamentBody(@NonNull Map<String, String> data) {
        String body = data.get("body");
        String trimmedBody = body != null ? body.trim() : "";
        if (trimmedBody.isEmpty()) {
            Log.w(TAG, getClass().getSimpleName() + ".extractTournamentBody: body was null or empty, using fallback");
            return DEFAULT_TOURNAMENT_BODY;
        }
        return trimmedBody;
    }

    /**
     * Extracts and validates the invite ID from FCM data.
     */
    private String extractInviteId(@NonNull Map<String, String> data) {
        String inviteIdRaw = data.get("inviteId");
        return inviteIdRaw != null ? inviteIdRaw : String.valueOf(System.currentTimeMillis());
    }

    /**
     * Ensures the notification channel exists on Android Oreo and above.
     * @param channelId The ID of the notification channel
     * @param channelName The user-visible name of the notification channel
     * @return true if channel is ready (or not needed), false if channel creation failed
     */
    private boolean ensureNotificationChannel(@NonNull Context context, @NonNull String channelId, @NonNull String channelName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) {
                    NotificationChannel channel = new NotificationChannel(
                            channelId,
                            channelName,
                            NotificationManager.IMPORTANCE_HIGH
                    );
                    nm.createNotificationChannel(channel);
                    return true;
                } else {
                    Log.e(TAG, getClass().getSimpleName() + ".ensureNotificationChannel: NotificationManager is null");
                    return false;
                }
            } catch (Exception e) {
                Log.e(TAG, getClass().getSimpleName() + ".ensureNotificationChannel: Failed to create channel", e);
                return false;
            }
        }
        return true; // Channel not needed for pre-Oreo
    }

    /**
     * Displays the notification with proper permission checks.
     */
    private void displayNotification(@NonNull Context context, int notificationId, 
                                     @NonNull NotificationCompat.Builder builder) {
        try {
            NotificationManagerCompat nm = NotificationManagerCompat.from(context);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                nm.notify(notificationId, builder.build());
            } else {
                Log.w(TAG, getClass().getSimpleName() + ".displayNotification: Missing POST_NOTIFICATIONS permission");
            }
        } catch (Exception e) {
            // Catch any exception during notification display (e.g., SecurityException, RemoteException)
            Log.e(TAG, getClass().getSimpleName() + ".displayNotification: Failed to display notification", e);
        }
    }
}
