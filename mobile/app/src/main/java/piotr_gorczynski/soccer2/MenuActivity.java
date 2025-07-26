package piotr_gorczynski.soccer2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Map;
import java.util.Objects;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.UserMessagingPlatform;



public class MenuActivity extends AppCompatActivity {
    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // test ID
    private InterstitialAd mInterstitialAd;

    private static final String PREF_AD_COUNTER = "adsCounter";
    private static final String PREF_AD_FREQUENCY = "adsFrequency";
    private static final int DEFAULT_AD_FREQUENCY = 10;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private static final String PREF_FCM_TOKEN = "fcmToken";
    
    private boolean isBackendAvailable = true; // Track backend availability
    private BackendServiceChecker serviceChecker;
    private Menu optionsMenu; // Hold reference to menu for updating warning icon

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
            if (newToken == null) return;

            String savedToken = prefs.getString(PREF_FCM_TOKEN, null);
            DocumentReference docRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid);
            docRef.get().addOnSuccessListener(doc -> {
                String remoteToken = doc.getString("fcmToken");

                if (remoteToken == null || !remoteToken.equals(newToken)) {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                    }.getClass().getEnclosingMethod()).getName() + ": 🔑 Updating Firestore FCM token");
                    docRef.set(Map.of("fcmToken", newToken), SetOptions.merge())
                            .addOnSuccessListener(v -> {
                                prefs.edit().putString(PREF_FCM_TOKEN, newToken).apply();
                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() + ": ✅ FCM token saved");
                            })
                            .addOnFailureListener(e -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to save FCM token", e));
                } else if (savedToken == null || !savedToken.equals(remoteToken)) {
                    prefs.edit().putString(PREF_FCM_TOKEN, remoteToken).apply();
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                    }.getClass().getEnclosingMethod()).getName() + ": 🔑 Synced FCM token from Firestore");
                } else {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                    }.getClass().getEnclosingMethod()).getName() + ": 🔑 FCM token unchanged; skip Firestore write");
                }
            }).addOnFailureListener(e -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
            }.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to read Firestore token", e));
        });

        // ✅ Call permission request
        requestNotificationPermissionIfNeeded();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("invite_channel", "Game Invites", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        FirebaseFirestore.getInstance()
                .collection("settings")
                .document("adsFreuency")
                .get()
                .addOnSuccessListener(doc -> {
                    Long freq = doc.getLong("value");
                    if (freq != null) {
                        prefs.edit().putInt(PREF_AD_FREQUENCY, freq.intValue()).apply();
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".runHousekeeping: ads frequency=" + freq);
                    }
                })
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer",
                            getClass().getSimpleName() + ".runHousekeeping: failed to load ads frequency",
                            e));

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

        // Ensure the FCM token is stored after login
        ((SoccerApp) getApplication()).syncFcmTokenIfNeeded();
        
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
        checkAndUpdateBlockedInviteWarning(); // Also check on resume
    }

    private void updateUiForAuthState() {
        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

        Button inviteBtn = findViewById(R.id.InviteFriend);
        Button pendingBtn = findViewById(R.id.ShowInvites);
        Button tournamentsBtn = findViewById(R.id.openTournamentsBtn);
        Button rankingBtn = findViewById(R.id.openRankingBtn);

        // Check if backend is available - if not, disable ALL buttons
        if (!isBackendAvailable) {
            // Disable all buttons when backend is unavailable
            inviteBtn.setEnabled(false);
            pendingBtn.setEnabled(false);
            tournamentsBtn.setEnabled(false);
            rankingBtn.setEnabled(false);
            
            // Visual cue (dim all buttons)
            float disabledAlpha = 0.3f;
            inviteBtn.setAlpha(disabledAlpha);
            pendingBtn.setAlpha(disabledAlpha);
            tournamentsBtn.setAlpha(disabledAlpha);
            rankingBtn.setAlpha(disabledAlpha);

            return; // Skip the normal auth-based logic
        }

        // Backend is available - proceed with normal auth-based logic
        // 1) Enable or disable the three feature buttons based on login status
        inviteBtn.setEnabled(loggedIn);
        pendingBtn.setEnabled(loggedIn);
        tournamentsBtn.setEnabled(loggedIn);
        rankingBtn.setEnabled(loggedIn);

        // Optional: visual cue (dim when disabled due to not being logged in)
        float alpha = loggedIn ? 1f : 0.4f;
        inviteBtn.setAlpha(alpha);
        pendingBtn.setAlpha(alpha);
        tournamentsBtn.setAlpha(alpha);
        rankingBtn.setAlpha(alpha);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /* ① Inflate the view immediately so onResume() has valid widgets */
        setContentView(R.layout.activity_menu);

        Toolbar toolbar = findViewById(R.id.menu_toolbar);
        setSupportActionBar(toolbar);

        // Initialize backend service checker
        SoccerApp app = (SoccerApp) getApplication();
        app.requestConsent(this);
        serviceChecker = app.getServiceChecker();
        
        // Get initial backend availability state from the app
        isBackendAvailable = app.isBackendAvailable();
        
        // Check backend availability on app launch
        checkBackendAvailability();

        runHousekeeping();          // ← always executed, even on cold resume

        Log.d("TAG_Soccer", "onCreate: Calling MobileAds");
        MobileAds.initialize(this, initializationStatus -> {});
        Log.d("TAG_Soccer", "onCreate: loadInterstitialAd");
        loadInterstitialAd();

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
    private void loadInterstitialAd() {
        ConsentInformation consentInformation =
                UserMessagingPlatform.getConsentInformation(this);

        AdRequest.Builder builder = new AdRequest.Builder();

        if (consentInformation.getConsentStatus()
                != ConsentInformation.ConsentStatus.OBTAINED) {
            Bundle extras = new Bundle();
            extras.putString("npa", "1");
            builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        }

        AdRequest adRequest = builder.build();

        InterstitialAd.load(this, AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onAdLoaded: Interstitial ad loaded"
                        );
                        mInterstitialAd = interstitialAd;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onAdFailedToLoad: Interstitial ad failed to load: " + loadAdError.getMessage()
                        );
                        mInterstitialAd = null;
                    }
                });
    }

    private boolean hasAdsConsent() {
        ConsentInformation ci = UserMessagingPlatform.getConsentInformation(this);
        return ci.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED;
    }

    private void showConsentRequiredDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.ads_consent_required)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showAdThenRun(Runnable action) {
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        int frequency = prefs.getInt(PREF_AD_FREQUENCY, DEFAULT_AD_FREQUENCY);
        int counter = prefs.getInt(PREF_AD_COUNTER, 0) + 1;
        if (counter < frequency) {
            prefs.edit().putInt(PREF_AD_COUNTER, counter).apply();
            action.run();
            return;
        }
        prefs.edit().putInt(PREF_AD_COUNTER, 0).apply();

        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".showAdThenRun: Ad ready=" + (mInterstitialAd != null)
        );
        if (mInterstitialAd != null) {
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onAdDismissedFullScreenContent"
                    );
                    action.run();
                    loadInterstitialAd();
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onAdFailedToShowFullScreenContent: " + adError.getMessage()
                    );
                    action.run();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    mInterstitialAd = null;
                }
            });

            mInterstitialAd.show(this);
        } else {
            action.run();
        }
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

        Log.d(
            "TAG_Soccer",
            getClass().getSimpleName() + ".continueWithInviteRestore: querying for live invites"
        );

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
                .addOnFailureListener(e ->
                        Log.e(
                                "TAG_Soccer",
                                getClass().getSimpleName() + ".continueWithInviteRestore: failed to query invites",
                                e
                        )
                );
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



    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
//---save whatever you need to persist—
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": MenuActivity.onSaveInstanceState entered");
        super.onSaveInstanceState(outState);
    }

    public void OpenInviteFriend(View view) {
        if (!hasAdsConsent()) {
            showConsentRequiredDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(MenuActivity.this, InviteFriendActivity.class)));
    }

    public void OpenInvites(View view) {
        if (!hasAdsConsent()) {
            showConsentRequiredDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(this, InvitationsActivity.class)));
    }

    public void OpenTournaments(View view) {
        if (!hasAdsConsent()) {
            showConsentRequiredDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(this, TournamentsActivity.class)));
    }

    public void OpenRanking(View view) {
        if (!hasAdsConsent()) {
            showConsentRequiredDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(this, RankingActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tournaments_menu, menu);
        this.optionsMenu = menu;
        checkAndUpdateBlockedInviteWarning(); // Check blocked invite status
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_account) {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(new Intent(this, UniversalLoginActivity.class));
            } else {
                startActivity(new Intent(this, AccountActivity.class));
            }
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_invite_blocked_warning) {
            // Show toast explaining the warning
            Toast.makeText(this, R.string.invites_blocked_notification, Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Check backend service availability and update UI accordingly
     */
    private void checkBackendAvailability() {
        if (serviceChecker == null) {
            Log.w(
                "TAG_Soccer",
                getClass().getSimpleName() + ".checkBackendAvailability: Service checker not available, assuming backend is available"
            );
            isBackendAvailable = true;
            updateUiForAuthState();
            return;
        }
        
        Log.d(
            "TAG_Soccer",
            getClass().getSimpleName() + ".checkBackendAvailability: Checking backend availability from MenuActivity"
        );
        
        // Show a brief checking state (optional)
        runOnUiThread(() -> {
            // Could add a progress indicator here if desired
            Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".checkBackendAvailability: Starting backend availability check..."
            );
        });
        
        serviceChecker.checkServiceAvailability(new BackendServiceChecker.ServiceCheckCallback() {
            @Override
            public void onServiceAvailable() {
                Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".checkBackendAvailability: Backend is available - enabling UI"
                );
                runOnUiThread(() -> {
                    isBackendAvailable = true;
                    updateUiForAuthState();
                });
            }

            @Override
            public void onServiceUnavailable(String reason) {
                Log.w(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".checkBackendAvailability: Backend is unavailable: " + reason
                );
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

    /**
     * Check if the current user has blocked invites and show/hide warning icon accordingly
     */
    private void checkAndUpdateBlockedInviteWarning() {
        if (optionsMenu == null) return; // Menu not created yet
        
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            // No user logged in, hide warning
            MenuItem warningItem = optionsMenu.findItem(R.id.action_invite_blocked_warning);
            if (warningItem != null) {
                warningItem.setVisible(false);
            }
            return;
        }
        
        FirebaseFirestore.getInstance().collection("users").document(uid)
            .get()
            .addOnSuccessListener(doc -> {
                boolean showWarning = false;
                if (doc.exists()) {
                    Boolean blockInvites = doc.getBoolean("blockInviteFriend");
                    showWarning = blockInvites != null && blockInvites;
                }
                
                MenuItem warningItem = optionsMenu.findItem(R.id.action_invite_blocked_warning);
                if (warningItem != null) {
                    warningItem.setVisible(showWarning);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".checkAndUpdateBlockedInviteWarning: Failed to check blocked invite status",
                    e
                );
                // Hide warning on error
                MenuItem warningItem = optionsMenu.findItem(R.id.action_invite_blocked_warning);
                if (warningItem != null) {
                    warningItem.setVisible(false);
                }
            });
    }

}
