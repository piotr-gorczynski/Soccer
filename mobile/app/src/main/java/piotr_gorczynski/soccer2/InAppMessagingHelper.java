package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;

/**
 * In-app messaging helper for targeting users from specific app versions
 * Implements targeted messaging as suggested in the user research plan
 */
public class InAppMessagingHelper {
    private static final String TAG = "InAppMessaging";
    
    // SharedPreferences keys for tracking message display
    private static final String PREF_KEY_MESSAGES_SHOWN = "messages_shown_count";
    private static final String PREF_KEY_LAST_MESSAGE_TIME = "last_message_time";
    private static final String PREF_KEY_MESSAGE_VERSION_SHOWN = "message_version_7_8_shown";
    
    // Timing constraints
    private static final long MIN_TIME_BETWEEN_MESSAGES = 24 * 60 * 60 * 1000; // 24 hours
    private static final int MAX_MESSAGES_PER_USER = 3;
    
    /**
     * Show targeted message to v7/v8 users who haven't signed up
     * @param context Activity context
     * @param analyticsManager Analytics manager for tracking
     */
    public static void showVersionTargetedMessage(Context context, AnalyticsManager analyticsManager) {
        // Only show to unauthenticated users
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            return;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check if we should show the message based on frequency and timing
        if (!shouldShowMessage(prefs)) {
            Log.d(TAG, "Skipping version targeted message due to frequency/timing constraints");
            return;
        }
        
        // Track that the message was shown
        analyticsManager.addAuthBreadcrumb("in_app_message_shown", "target=v7_v8_users");
        
        // Show the targeted message
        new AlertDialog.Builder(context)
                .setTitle(R.string.register_benefits_title)
                .setMessage(R.string.register_benefits_message)
                .setPositiveButton(R.string.register_now, (dialog, which) -> {
                    analyticsManager.addAuthBreadcrumb("in_app_message_action", "register_clicked");
                    
                    // Open registration activity
                    Intent intent = new Intent(context, UniversalLoginActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton(R.string.maybe_later, (dialog, which) -> {
                    analyticsManager.addAuthBreadcrumb("in_app_message_action", "dismissed");
                    dialog.dismiss();
                })
                .show();
        
        // Update message display tracking
        updateMessageDisplayTracking(prefs);
        
        Log.d(TAG, "Showed version targeted message to v7/v8 user");
    }
    
    /**
     * Check if we should show in-app message based on user session count and version
     * Call this from MenuActivity or main screens after a few seconds delay
     */
    public static void checkAndShowVersionMessage(Context context, AnalyticsManager analyticsManager) {
        // Only show to users who have used the app multiple times (indicating they're engaged)
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int sessionCount = prefs.getInt("session_count", 0);
        
        // Increment session count
        prefs.edit().putInt("session_count", sessionCount + 1).apply();
        
        // Show message after 2+ sessions to engaged users
        if (sessionCount >= 2) {
            // Delay showing the message slightly so it doesn't appear immediately on app open
            android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
            handler.postDelayed(() -> {
                showVersionTargetedMessage(context, analyticsManager);
            }, 3000); // 3 second delay
        }
    }
    
    /**
     * Check if we should show the message based on frequency limits
     */
    private static boolean shouldShowMessage(SharedPreferences prefs) {
        // Check if message was already shown for this version
        if (prefs.getBoolean(PREF_KEY_MESSAGE_VERSION_SHOWN, false)) {
            return false;
        }
        
        // Check message frequency limits
        int messagesShown = prefs.getInt(PREF_KEY_MESSAGES_SHOWN, 0);
        if (messagesShown >= MAX_MESSAGES_PER_USER) {
            return false;
        }
        
        // Check time since last message
        long lastMessageTime = prefs.getLong(PREF_KEY_LAST_MESSAGE_TIME, 0);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime < MIN_TIME_BETWEEN_MESSAGES) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Update tracking of message display
     */
    private static void updateMessageDisplayTracking(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        
        // Mark that version message was shown
        editor.putBoolean(PREF_KEY_MESSAGE_VERSION_SHOWN, true);
        
        // Increment message count
        int messagesShown = prefs.getInt(PREF_KEY_MESSAGES_SHOWN, 0);
        editor.putInt(PREF_KEY_MESSAGES_SHOWN, messagesShown + 1);
        
        // Update last message time
        editor.putLong(PREF_KEY_LAST_MESSAGE_TIME, System.currentTimeMillis());
        
        editor.apply();
    }
    
    /**
     * Reset message tracking (useful for testing or when user logs in)
     */
    public static void resetMessageTracking(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.remove(PREF_KEY_MESSAGES_SHOWN);
        editor.remove(PREF_KEY_LAST_MESSAGE_TIME);
        editor.remove(PREF_KEY_MESSAGE_VERSION_SHOWN);
        
        editor.apply();
        
        Log.d(TAG, "Reset in-app message tracking");
    }
}