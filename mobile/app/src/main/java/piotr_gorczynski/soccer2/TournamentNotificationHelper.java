package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to manage tournament start notifications.
 * Shows a dialog when a tournament starts that the user is registered for.
 */
public class TournamentNotificationHelper {
    private static final String TAG = "TAG_Soccer";
    private static final String PREFS_NAME = "TournamentNotifications";
    private static final String KEY_SHOWN_TOURNAMENTS = "shown_tournaments";
    
    // Maximum number of tournament IDs to keep in storage to prevent unbounded growth
    private static final int MAX_STORED_TOURNAMENT_IDS = 100;
    
    // Time window to check for recently started tournaments (in milliseconds)
    // We check for tournaments that started within the last 24 hours
    private static final long RECENT_START_WINDOW_MS = TimeUnit.HOURS.toMillis(24);

    /**
     * Check for recently started tournaments and show notification if applicable.
     * This should be called when the app returns to foreground.
     */
    public static void checkForTournamentNotifications(Activity activity) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.d(TAG, "TournamentNotificationHelper.checkForTournamentNotifications: No user logged in");
            return;
        }

        String uid = user.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d(TAG, "TournamentNotificationHelper.checkForTournamentNotifications: Checking for tournament notifications");

        // Query for tournaments that are currently running
        db.collection("tournaments")
                .whereEqualTo("status", "running")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Log.d(TAG, "TournamentNotificationHelper: No running tournaments found");
                        return;
                    }

                    Log.d(TAG, "TournamentNotificationHelper: Found " + querySnapshot.size() + " running tournaments");

                    // Check each tournament to see if we should notify the user
                    for (DocumentSnapshot tournamentDoc : querySnapshot.getDocuments()) {
                        checkAndShowNotification(activity, tournamentDoc, uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "TournamentNotificationHelper: Failed to fetch running tournaments", e);
                });
    }

    /**
     * Check if we should show a notification for this specific tournament.
     */
    private static void checkAndShowNotification(Activity activity, DocumentSnapshot tournamentDoc, String uid) {
        String tournamentId = tournamentDoc.getId();
        String tournamentName = tournamentDoc.getString("name");

        // Check if user already opened this tournament via notification
        if (activity instanceof TournamentLobbyActivity) {
            TournamentLobbyActivity tournamentActivity = (TournamentLobbyActivity) activity;
            String currentTournamentId = tournamentActivity.getIntent().getStringExtra("tournamentId");
            boolean fromNotification = tournamentActivity.getIntent().getBooleanExtra("fromNotification", false);
            
            if (fromNotification && tournamentId.equals(currentTournamentId)) {
                Log.d(TAG, "TournamentNotificationHelper: Tournament " + tournamentId + " already opened from notification, skipping dialog");
                // Mark as shown to prevent showing it later
                markNotificationShown(activity, tournamentId);
                return;
            }
        }

        // Check if we've already shown this tournament
        if (hasShownNotification(activity, tournamentId)) {
            Log.d(TAG, "TournamentNotificationHelper: Already shown notification for tournament " + tournamentId);
            return;
        }

        // Check if the tournament started recently
        Timestamp startedAt = tournamentDoc.getTimestamp("startedAt");
        if (startedAt == null) {
            Log.d(TAG, "TournamentNotificationHelper: Tournament " + tournamentId + " has no startedAt timestamp");
            return;
        }

        long startedAtMs = startedAt.toDate().getTime();
        long nowMs = System.currentTimeMillis();
        long timeSinceStart = nowMs - startedAtMs;

        if (timeSinceStart > RECENT_START_WINDOW_MS) {
            Log.d(TAG, "TournamentNotificationHelper: Tournament " + tournamentId + " started too long ago");
            return;
        }

        // Check if user is a participant in this tournament
        tournamentDoc.getReference()
                .collection("participants")
                .document(uid)
                .get()
                .addOnSuccessListener(participantDoc -> {
                    if (participantDoc.exists()) {
                        Log.d(TAG, "TournamentNotificationHelper: User is participant in tournament " + tournamentId + ", showing notification");
                        showTournamentStartedDialog(activity, tournamentId, tournamentName != null ? tournamentName : "Unknown");
                    } else {
                        Log.d(TAG, "TournamentNotificationHelper: User is not a participant in tournament " + tournamentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "TournamentNotificationHelper: Failed to check participation for tournament " + tournamentId, e);
                });
    }

    /**
     * Show the tournament started dialog.
     */
    private static void showTournamentStartedDialog(Activity activity, String tournamentId, String tournamentName) {
        // Mark as shown immediately to avoid duplicate dialogs
        markNotificationShown(activity, tournamentId);

        activity.runOnUiThread(() -> {
            String message = activity.getString(R.string.tournament_started_dialog_message, tournamentName);

            new AlertDialog.Builder(activity)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        Log.d(TAG, "TournamentNotificationHelper: User accepted to open tournament lobby for " + tournamentId);
                        openTournamentLobby(activity, tournamentId, tournamentName);
                    })
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        Log.d(TAG, "TournamentNotificationHelper: User declined to open tournament lobby for " + tournamentId);
                        dialog.dismiss();
                    })
                    .setCancelable(true)
                    .show();
        });
    }

    /**
     * Open the tournament lobby with ad logic if applicable.
     */
    private static void openTournamentLobby(Activity activity, String tournamentId, String tournamentName) {
        // If this is MenuActivity, use its ad logic
        if (activity instanceof MenuActivity) {
            MenuActivity menuActivity = (MenuActivity) activity;
            menuActivity.showAdThenRun(() -> {
                Intent intent = new Intent(activity, TournamentLobbyActivity.class)
                        .putExtra("tournamentId", tournamentId)
                        .putExtra("tournamentName", tournamentName);
                activity.startActivity(intent);
            });
        } else {
            // For other activities, just open the lobby directly
            Intent intent = new Intent(activity, TournamentLobbyActivity.class)
                    .putExtra("tournamentId", tournamentId)
                    .putExtra("tournamentName", tournamentName);
            activity.startActivity(intent);
        }
    }

    /**
     * Check if we've already shown a notification for this tournament.
     */
    private static boolean hasShownNotification(Context context, String tournamentId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> shownTournaments = prefs.getStringSet(KEY_SHOWN_TOURNAMENTS, new HashSet<>());
        return shownTournaments.contains(tournamentId);
    }

    /**
     * Mark that we've shown a notification for this tournament.
     * Implements a simple cleanup mechanism to prevent unbounded storage growth.
     */
    private static void markNotificationShown(Context context, String tournamentId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> shownTournaments = new HashSet<>(prefs.getStringSet(KEY_SHOWN_TOURNAMENTS, new HashSet<>()));
        shownTournaments.add(tournamentId);
        
        // If we exceed the maximum size, remove oldest entries (arbitrary removal since we don't track timestamps)
        // This prevents unbounded growth of the storage
        if (shownTournaments.size() > MAX_STORED_TOURNAMENT_IDS) {
            Log.d(TAG, "TournamentNotificationHelper: Cleaning up old tournament IDs, current size: " + shownTournaments.size());
            // Keep only the most recent MAX_STORED_TOURNAMENT_IDS entries
            // Since we don't track timestamps, we just keep a subset
            Set<String> trimmedSet = new HashSet<>();
            int count = 0;
            for (String id : shownTournaments) {
                if (count++ >= MAX_STORED_TOURNAMENT_IDS) break;
                trimmedSet.add(id);
            }
            shownTournaments = trimmedSet;
        }
        
        prefs.edit().putStringSet(KEY_SHOWN_TOURNAMENTS, shownTournaments).apply();
        Log.d(TAG, "TournamentNotificationHelper: Marked tournament " + tournamentId + " as shown");
    }
}
