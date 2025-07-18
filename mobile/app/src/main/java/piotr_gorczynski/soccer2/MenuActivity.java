package piotr_gorczynski.soccer2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;


public class MenuActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private static final String PREF_FCM_TOKEN = "fcmToken";
    
    private boolean isBackendAvailable = true; // Track backend availability
    private BackendServiceChecker serviceChecker;

    /* ───────────── misc tasks that must always run on launch ───────────── */
    private void runHousekeeping() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
            }.getClass().getEnclosingMethod()).getName() + ": ⚠️ No logged-in user; token not saved");
            return;
        }
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to get FCM token", task.getException());
                return;
            }

            String newToken = task.getResult();

            String savedToken = prefs.getString(PREF_FCM_TOKEN, null);
            if (newToken != null && newToken.equals(savedToken)) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() + ": 🔑 FCM token unchanged; skip Firestore write");
                return;                     // ← exit early
            }

            FirebaseFirestore.getInstance().collection("users").document(uid).update("fcmToken", newToken).addOnSuccessListener(v -> {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() + ": ✅ FCM token saved");
                prefs.edit().putString(PREF_FCM_TOKEN, newToken).apply();
            }).addOnFailureListener(e -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
            }.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to save FCM token", e));
        });

        // ✅ Call permission request
        requestNotificationPermissionIfNeeded();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("invite_channel", "Game Invites", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": InvitationsActivity onNewIntent: " + intent.toUri(Intent.URI_INTENT_SCHEME));
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check backend availability when activity resumes
        checkBackendAvailability();
        
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String nickname = prefs.getString("nickname", null);

        TextView nicknameLabel = findViewById(R.id.nicknameLabel);
        if (nickname != null && !nickname.isEmpty()) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
            }.getClass().getEnclosingMethod()).getName() + ": Nickname: " + nickname);
            nicknameLabel.setText(getString(R.string.hello_nickname, nickname));
        } else {
            nicknameLabel.setText(getString(R.string.welcome_to_soccer));
        }
        updateUiForAuthState();
        
        // Add debug functionality - long press on settings button to manually test backend
        Button settingsBtn = findViewById(R.id.Settings);
        if (settingsBtn != null) {
            settingsBtn.setOnLongClickListener(v -> {
                Log.d("TAG_Soccer", "Manual backend test triggered via long press");
                if (serviceChecker != null) {
                    serviceChecker.testServiceCheck();
                    Toast.makeText(this, "Backend test started - check logs", Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    private void updateUiForAuthState() {
        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

        Button inviteBtn = findViewById(R.id.InviteFriend);
        Button pendingBtn = findViewById(R.id.ShowInvites);
        Button tournamentsBtn = findViewById(R.id.openTournamentsBtn);
        Button accountBtn = findViewById(R.id.Account);

        // Check if backend is available - if not, disable ALL buttons
        if (!isBackendAvailable) {
            // Disable all buttons when backend is unavailable
            inviteBtn.setEnabled(false);
            pendingBtn.setEnabled(false);
            tournamentsBtn.setEnabled(false);
            accountBtn.setEnabled(false);
            
            // Visual cue (dim all buttons)
            float disabledAlpha = 0.3f;
            inviteBtn.setAlpha(disabledAlpha);
            pendingBtn.setAlpha(disabledAlpha);
            tournamentsBtn.setAlpha(disabledAlpha);
            accountBtn.setAlpha(disabledAlpha);
            
            // Show that we're checking or unavailable
            accountBtn.setText(R.string.server_unavailable);
            accountBtn.setOnClickListener(null);
            
            return; // Skip the normal auth-based logic
        }

        // Backend is available - proceed with normal auth-based logic
        // 1) Enable or disable the three feature buttons based on login status
        inviteBtn.setEnabled(loggedIn);
        pendingBtn.setEnabled(loggedIn);
        tournamentsBtn.setEnabled(loggedIn);

        // Optional: visual cue (dim when disabled due to not being logged in)
        float alpha = loggedIn ? 1f : 0.4f;
        inviteBtn.setAlpha(alpha);
        pendingBtn.setAlpha(alpha);
        tournamentsBtn.setAlpha(alpha);
        
        // Account button is always enabled when backend is available
        accountBtn.setEnabled(true);
        accountBtn.setAlpha(1f);

        // 2) Update the Account / Logout button text + handler 
        if (loggedIn) {
            accountBtn.setText(R.string.logout);
            accountBtn.setOnClickListener(v -> performLogout());
        } else {
            accountBtn.setText(R.string.login);
            accountBtn.setOnClickListener(this::OpenAccount);
        }
    }

    private void performLogout() {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {          // safety net – should never be null here
            finishLogoutUi();
            return;
        }

        ((SoccerApp) getApplication()).forceUserOffline(uid).addOnCompleteListener(task -> {
            FirebaseAuth.getInstance().signOut();   // <-- now it’s safe
            finishLogoutUi();
        });
    }

    private void finishLogoutUi() {
        SharedPreferences.Editor ed = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE).edit();
        ed.clear().apply();                          // clears token + other cache

        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        TextView nicknameLabel = findViewById(R.id.nicknameLabel);
        nicknameLabel.setText(getString(R.string.welcome_to_soccer));
        updateUiForAuthState();      // dim buttons, “Login” label, etc.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /* ① Inflate the view immediately so onResume() has valid widgets */
        setContentView(R.layout.activity_menu);

        // Initialize backend service checker
        SoccerApp app = (SoccerApp) getApplication();
        serviceChecker = app.getServiceChecker();
        
        // Get initial backend availability state from the app
        isBackendAvailable = app.isBackendAvailable();
        
        // Check backend availability on app launch
        checkBackendAvailability();

        runHousekeeping();          // ← always executed, even on cold resume

        /* ───────────── 1️⃣  Look for any ACTIVE match involving this user ───────────── */
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String uid = FirebaseAuth.getInstance().getUid();
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": Auth UID at match-lookup = " + FirebaseAuth.getInstance().getUid());

        if (uid != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Task<QuerySnapshot> qP0 = db.collectionGroup("matches")
                    .whereEqualTo("status", "active")
                    .whereEqualTo("player0", uid).limit(1).get();
            Task<QuerySnapshot> qP1 = db.collectionGroup("matches")
                    .whereEqualTo("status", "active")
                    .whereEqualTo("player1", uid).limit(1).get();
            Tasks.whenAllSuccess(qP0, qP1).addOnSuccessListener(results -> {
                DocumentSnapshot doc = null;
                for (Object r : results) {
                    QuerySnapshot qs = (QuerySnapshot) r;
                    if (!qs.isEmpty()) {          // status already == "active"
                        doc = qs.getDocuments().get(0);
                        break;                    // first non-empty wins
                    }
                }
                if (doc != null) {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                    }.getClass().getEnclosingMethod()).getName()
                            + ": Found active match: "
                            + doc.getReference().getPath()
                            + ". calling startGame...");
                    startGame(doc.getReference().getPath(), prefs);
                } else {
                    continueWithInviteRestore();
                }
            }).addOnFailureListener(err -> {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName()
                        + ": Failed to query active matches → " + err.getMessage(), err);
                continueWithInviteRestore();
            });
            return;   // invite-path continues asynchronously
        }        /* no UID (not logged-in) → skip active-match lookup */
        continueWithInviteRestore();
    }

    /* helper: launch GameActivity then finish this MenuActivity */
    private void startGame(String matchPath, SharedPreferences prefs) {
        startActivity(new Intent(this, GameActivity.class).putExtra("matchPath", matchPath).putExtra("GameType", 3).putExtra("localNickname", prefs.getString("nickname", "Player")).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }

    /*  🔻  old waiting-invite code moved unchanged into a helper  */
    private void continueWithInviteRestore() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long nowMs = System.currentTimeMillis();

        Log.d("TAG_Soccer", "continueWithInviteRestore: querying for live invites");

        db.collection("invitations")
                .whereEqualTo("from", uid)
                .whereEqualTo("status", "pending")
                .orderBy("expireAt")  // expires soonest first
                .limit(1)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName() + ": continueWithInviteRestore: no pending invites");
                        return;
                    }

                    DocumentSnapshot doc = snap.getDocuments().get(0);
                    String inviteId = doc.getId();

                    // Make sure the invite hasn’t already expired
                    if (doc.getTimestamp("expireAt") != null &&
                            Objects.requireNonNull(doc.getTimestamp("expireAt")).toDate().getTime() > nowMs) {

                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName() + ": continueWithInviteRestore: ↩️ valid invite found "
                                + inviteId + " → resuming WaitingActivity");

                        startActivity(new Intent(this, WaitingActivity.class)
                                .putExtra("inviteId", inviteId)
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK));
                        finish();

                    } else {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName() + ": continueWithInviteRestore: invite " + inviteId + " is already expired → skipping");
                    }
                })
                .addOnFailureListener(e -> Log.e("TAG_Soccer", "continueWithInviteRestore: failed to query invites", e));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() + ": ✅ Notification permission granted");
            } else {
                Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() + ": ❌ Notification permission denied");
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }


    public void OpenGamePlayerVsPlayer(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("GameType", 1);
        startActivity(intent);
    }

    public void OpenGamePlayerVsAndroid(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("GameType", 2);
        startActivity(intent);
    }

    public void OpenSettings(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    public void OpenAccount(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
//---save whatever you need to persist—
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": MenuActivity.onSaveInstanceState entered");
        super.onSaveInstanceState(outState);
    }

    public void OpenInviteFriend(View view) {
        Intent intent = new Intent(this, InviteFriendActivity.class);
        startActivity(intent);
    }

    public void OpenInvites(View view) {
        Intent intent = new Intent(this, InvitationsActivity.class);
        startActivity(intent);
    }

    public void OpenTournaments(View view) {
        startActivity(new Intent(this, TournamentsActivity.class));
    }

    /**
     * Check backend service availability and update UI accordingly
     */
    private void checkBackendAvailability() {
        if (serviceChecker == null) {
            Log.w("TAG_Soccer", "Service checker not available, assuming backend is available");
            isBackendAvailable = true;
            updateUiForAuthState();
            return;
        }
        
        Log.d("TAG_Soccer", "Checking backend availability from MenuActivity");
        
        // Show a brief checking state (optional)
        runOnUiThread(() -> {
            // Could add a progress indicator here if desired
            Log.d("TAG_Soccer", "Starting backend availability check...");
        });
        
        serviceChecker.checkServiceAvailability(new BackendServiceChecker.ServiceCheckCallback() {
            @Override
            public void onServiceAvailable() {
                Log.d("TAG_Soccer", "Backend is available - enabling UI");
                runOnUiThread(() -> {
                    isBackendAvailable = true;
                    updateUiForAuthState();
                });
            }

            @Override
            public void onServiceUnavailable(String reason) {
                Log.w("TAG_Soccer", "Backend is unavailable: " + reason);
                runOnUiThread(() -> {
                    isBackendAvailable = false;
                    updateUiForAuthState();
                    // Show toast notification about server unavailability
                    Toast.makeText(MenuActivity.this, 
                            "We are sorry, but the Soccer server is not available at the moment...", 
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

}
