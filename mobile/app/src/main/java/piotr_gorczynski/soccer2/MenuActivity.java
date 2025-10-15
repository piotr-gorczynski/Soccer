package piotr_gorczynski.soccer2;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
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
import com.google.firebase.firestore.Source;
import com.google.firebase.messaging.FirebaseMessaging;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import androidx.appcompat.app.AlertDialog;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.UserMessagingPlatform;



public class MenuActivity extends BaseActivity {
    private InterstitialAd mInterstitialAd;

    private static final String PREF_AD_COUNTER = "adsCounter";
    private static final String PREF_AD_FREQUENCY = "adsFrequency";
    private static final int DEFAULT_AD_FREQUENCY = 10;
    private static final int FAILSAFE_AD_FREQUENCY = 2;
    private static final long AD_RETRY_DELAY_MS = 30_000L;

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private static final String PREF_FCM_TOKEN = "fcmToken";
    static final String PREF_LAST_INVITES_SEEN_TIMESTAMP = "lastInvitesSeenTimestamp";
    private static final String PREF_LAST_ACTIVE_TIMESTAMP = "lastActiveTimestamp";

    private boolean isBackendAvailable = true; // Track backend availability
    // Track whether we've already shown the offline toast while the
    // backend is unavailable to avoid spamming the user on every resume
    private boolean backendUnavailableToastShown = false;
    private boolean isBackendCheckInProgress = false;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean networkCallbackRegistered = false;
    private BackendServiceChecker serviceChecker;
    private Menu optionsMenu; // Hold reference to menu for updating warning icon
    private MenuItem accountMenuItem; // Reference to account menu item
    private String currentLanguage;
    private AnalyticsManager analyticsManager;
    private final Handler adRetryHandler = new Handler(Looper.getMainLooper());
    private final Runnable adRetryRunnable = this::loadInterstitialAd;
    private boolean isAdLoading = false;
    private View loadingOverlay;
    private final Handler overlayHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideOverlayRunnable = this::hideLoadingOverlayImmediate;
    private long loadingOverlayShownAtMs = 0L;
    private static final long MIN_LOADING_OVERLAY_DURATION_MS = 250L;

    /**
     * Helper to fetch user details from Firestore and update prefs/UI. This is
     * primarily used on cold start when Firebase already has an authenticated
     * user but the local SharedPreferences are empty.
     */
    private void fetchNicknameFromFirestore(@NonNull String uid,
                                            @NonNull SharedPreferences prefs,
                                            @NonNull Runnable onMissing) {

        // Skip nickname fetch if backend is unavailable to prevent Firestore errors
        if (!isBackendAvailable) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".fetchNicknameFromFirestore: Skipping nickname fetch - backend unavailable");
            // If we have no local nickname and backend is unavailable, run onMissing callback
            String localNickname = prefs.getString("nickname", null);
            if (localNickname == null || localNickname.trim().isEmpty()) {
                onMissing.run();
            }
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    String remoteNick = doc.getString("nickname");
                    String remoteEmail = doc.getString("email");
                    String remoteMethod = doc.getString("method");
                    if (doc.exists() && remoteNick != null && !remoteNick.trim().isEmpty()) {
                        SharedPreferences.Editor ed = prefs.edit();
                        String local = prefs.getString("nickname", null);
                        if (!uid.equals(prefs.getString("uid", null))) {
                            ed.putString("uid", uid);
                        }
                        if (local == null || !local.equals(remoteNick)) {
                            ed.putString("nickname", remoteNick);
                            TextView nicknameLabel = findViewById(R.id.nicknameLabel);
                            String labelText = SafeStringFormatter.safeGetString(this, R.string.hello_nickname, remoteNick);
                            if (nicknameLabel != null) {
                                nicknameLabel.setText(labelText);
                                Log.d(
                                        "TAG_Soccer",
                                        getClass().getSimpleName()
                                                + ".fetchNicknameFromFirestore: nicknameLabel=\""
                                                + labelText
                                                + "\""
                                );
                            } else {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + ".fetchNicknameFromFirestore: nicknameLabel is null, layout may not be properly inflated");
                            }
                        }
                        String localEmail = prefs.getString("email", null);
                        if (remoteEmail != null && (localEmail == null || !localEmail.equals(remoteEmail))) {
                            ed.putString("email", remoteEmail);
                        }
                        String localMethod = prefs.getString("method", null);
                        if (remoteMethod != null && (localMethod == null || !localMethod.equals(remoteMethod))) {
                            ed.putString("method", remoteMethod);
                        }
                        ed.apply();

                        updateUiForAuthState();
                        checkAndUpdateBlockedInviteWarning();
                    } else {
                        if (uid.equals(FirebaseAuth.getInstance().getUid())) {
                            onMissing.run();
                        } else {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + ".fetchNicknameFromFirestore: user changed, skipping nickname prompt");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".fetchNicknameFromFirestore: failed", e
                    );
                    Toast.makeText(
                            this,
                            R.string.failed_to_load_nickname,
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void ensureTermsAccepted(@NonNull String uid) {
        // Skip terms check if backend is unavailable to prevent Firestore errors
        if (!isBackendAvailable) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".ensureTermsAccepted: Skipping terms check - backend unavailable");
            return;
        }
        
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
                    Boolean accepted = doc.getBoolean("termsAccepted");
                    if (accepted == null || !accepted) {
                        startActivity(new Intent(this, TermsActivity.class));
                    }
                    // Consent is now requested early in onCreate() for all users
                })
                .addOnFailureListener(e -> Log.e(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".ensureTermsAccepted: failed",
                        e
                ));
    }

    /* ───────────── misc tasks that must always run on launch ───────────── */
    @SuppressLint("ApplySharedPref")
    private void runHousekeeping() {
        String uid = FirebaseAuth.getInstance().getUid();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (uid == null) {
            Log.w(
                    "TAG_Soccer",
                    getClass().getSimpleName()
                            + "."
                            + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName()
                            + ": ⚠️ No logged-in user; clearing stored credentials"
            );
            // Note: Preserve ads consent data as requested in issue - only clear if there are specific user-related data in default prefs
            // Currently no user-specific data is stored in default SharedPreferences, so we don't need to clear anything
            // prefs.edit().clear().commit(); // Commented out to preserve ads consent data
            FirebaseMessaging.getInstance().deleteToken();
            FirebaseMessaging.getInstance().setAutoInitEnabled(false);
            // Removed Firestore terminate/clearPersistence to prevent AsyncQueue threading crash
            return;
        }

        // 🔄 Sync nickname from Firestore
        fetchNicknameFromFirestore(uid, prefs, () -> {});

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
                String emailField = doc.getString("email");
                String methodField = doc.getString("method");
                String facebookIdField = doc.getString("facebookId");
                Boolean accountDeleted = doc.getBoolean("accountDeleted");
                
                // Check if account is deleted - logout if true
                if (accountDeleted != null && accountDeleted) {
                    Log.w("TAG_Soccer", getClass().getSimpleName() + "." +
                            Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                            ": Account is deleted - logging out user");
                    logoutUser();
                    return;
                }
                
                // Validate required fields based on login method
                boolean shouldLogout = false;
                String logoutReason = "";
                
                if ("facebook.com".equals(methodField)) {
                    // For Facebook login, facebookId is required
                    if (facebookIdField == null || facebookIdField.isEmpty()) {
                        shouldLogout = true;
                        logoutReason = "Missing facebookId for Facebook login";
                    }
                } else if ("google.com".equals(methodField) || "email".equals(methodField)) {
                    // For email/Google login, email is required
                    if (emailField == null || emailField.isEmpty()) {
                        shouldLogout = true;
                        logoutReason = "Missing email for email/Google login";
                    }
                }
                
                if (shouldLogout) {
                    Log.w("TAG_Soccer", getClass().getSimpleName() + "." +
                            Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                            ": " + logoutReason + " - logging out user");
                    logoutUser();
                    return;
                }

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

    private void logoutUser() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            // Only call forceUserOffline if user is actually logged in
            ((SoccerApp) getApplication()).forceUserOffline(uid);
        }

        FirebaseAuth.getInstance().signOut();

        // Remove only user-specific data, preserve language preferences and other device settings
        getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE)
                .edit()
                .remove(PREF_FCM_TOKEN)
                .remove("uid")
                .remove("email")
                .remove("nickname")
                .remove("method")
                .remove("facebookId")
                .remove("facebookName")
                .remove("facebookPhotoUrl")
                .apply();

        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".logoutUser: ✅ FCM token deleted");
                    } else {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + ".logoutUser: ❌ Failed to delete FCM token", t.getException());
                    }
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);

        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();

        // Update nickname label after logging out
        TextView nicknameLabel = findViewById(R.id.nicknameLabel);
        if (nicknameLabel != null) {
            nicknameLabel.setText(getString(R.string.welcome_to_soccer));
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName()
                            + ".logoutUser: nicknameLabel=\""
                            + nicknameLabel.getText()
                            + "\""
            );
        }

        updateUiForAuthState();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": InvitationsActivity onNewIntent: " + intent.toUri(Intent.URI_INTENT_SCHEME));
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onResume() {
        super.onResume();

        String lang = LanguageManager.getCurrentLanguageCode(this);
        if (currentLanguage == null || !currentLanguage.equals(lang)) {
            currentLanguage = lang;
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".onResume: language changed to " + lang + ", recreating");
            recreate();
            return;
        }

        // Check backend availability when activity resumes - this will trigger authentication logic when done
        checkBackendAvailabilityAndContinue();

        // Ensure the FCM token is stored after login
        ((SoccerApp) getApplication()).syncFcmTokenIfNeeded();
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerNetworkCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterNetworkCallback();
    }

    /**
     * Continue with authentication and UI logic after backend availability check is complete
     */
    @SuppressLint("ApplySharedPref")
    private void continueOnResumeAfterBackendCheck() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        SharedPreferences prefs =
                getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        String nickname = prefs.getString("nickname", null);
        if (auth.getCurrentUser() == null) {
            // Remove only user-specific data, preserve language preferences and other device settings
            SharedPreferences.Editor ed = prefs.edit();
            ed.remove("uid")
              .remove("email")
              .remove("nickname")
              .remove("method")
              .remove("facebookId")
              .remove("facebookName")
              .remove("facebookPhotoUrl")
              .remove("fcmToken");
            ed.commit();

            FirebaseMessaging.getInstance().deleteToken();
            FirebaseMessaging.getInstance().setAutoInitEnabled(false);
            // Removed Firestore terminate/clearPersistence to prevent AsyncQueue threading crash
            nickname = null;
        } else {
            String uid = auth.getUid();
            if (uid != null) {
                ensureTermsAccepted(uid);
                Runnable pickNick = () -> {
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        startActivity(new Intent(this, PickNicknameActivity.class));
                    }
                };
                if (nickname == null || nickname.isEmpty()) {
                    fetchNicknameFromFirestore(uid, prefs, pickNick);
                    return;
                } else {
                    // Refresh nickname in the background to keep prefs in sync
                    fetchNicknameFromFirestore(uid, prefs, () -> {});
                }
            }
        }

        TextView nicknameLabel = findViewById(R.id.nicknameLabel);
        String labelText;
        if (nickname != null && !nickname.isEmpty()) {
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName()
                            + "."
                            + Objects.requireNonNull(new Object() {
                    }.getClass().getEnclosingMethod()).getName()
                            + ": Nickname: "
                            + nickname
            );
            labelText = SafeStringFormatter.safeGetString(this, R.string.hello_nickname, nickname);
        } else {
            labelText = getString(R.string.welcome_to_soccer);
        }
        if (nicknameLabel != null) {
            nicknameLabel.setText(labelText);
        } else {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".continueOnResumeAfterBackendCheck: nicknameLabel is null, layout may not be properly inflated");
        }
        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName()
                        + ".continueOnResumeAfterBackendCheck: nicknameLabel=\""
                        + labelText
                        + "\""
        );
        updateUiForAuthState();
        checkAndUpdateBlockedInviteWarning(); // Also check on resume

        // Now that all authentication-related checks are done, look for any active match
        checkForActiveMatch();

        Button youVsAndroid = findViewById(R.id.youVsAndroidBtn);
        if (youVsAndroid != null) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".continueOnResumeAfterBackendCheck: youVsAndroid=" + youVsAndroid.getText());
        }
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
            if (inviteBtn != null) {
                inviteBtn.setEnabled(false);
                inviteBtn.setAlpha(0.3f);
            }
            if (pendingBtn != null) {
                pendingBtn.setEnabled(false);
                pendingBtn.setAlpha(0.3f);
            }
            if (tournamentsBtn != null) {
                tournamentsBtn.setEnabled(false);
                tournamentsBtn.setAlpha(0.3f);
            }
            if (rankingBtn != null) {
                rankingBtn.setEnabled(false);
                rankingBtn.setAlpha(0.3f);
            }

            return; // Skip the normal auth-based logic
        }

        // Backend is available - proceed with normal auth-based logic
        // Dim buttons when the user is not logged in, but keep them clickable so we can
        // show a toast informing them to register.
        float alpha = loggedIn ? 1f : 0.4f;
        if (inviteBtn != null) {
            inviteBtn.setEnabled(true);
            inviteBtn.setAlpha(alpha);
        }
        if (pendingBtn != null) {
            pendingBtn.setEnabled(true);
            pendingBtn.setAlpha(alpha);
        }
        if (tournamentsBtn != null) {
            tournamentsBtn.setEnabled(true);
            tournamentsBtn.setAlpha(alpha);
        }
        if (rankingBtn != null) {
            rankingBtn.setEnabled(true);
            rankingBtn.setAlpha(alpha);
        }
    }

    private void checkForActiveMatch() {
        SharedPreferences prefs =
                getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        String uid = FirebaseAuth.getInstance().getUid();
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": Auth UID at match-lookup = " + uid);

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
                    if (!qs.isEmpty()) {
                        doc = qs.getDocuments().get(0);
                        break;
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
        } else {
            /* no UID (not logged-in) → skip active-match lookup */
            continueWithInviteRestore();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        
        // Request consent early for EEA compliance - ensures GA4 doesn't collect data before consent
        if (!hasAdsConsent()) {
            ((SoccerApp) getApplication()).requestConsent(this);
        }
        
        /* ① Inflate the view immediately so onResume() has valid widgets */
        try {
            setContentView(R.layout.activity_menu);
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".onCreate: Failed to set content view", e);
            // Try to handle AppCompat theme/layout initialization failures
            try {
                // Attempt recovery by recreating the activity with basic theme handling
                handleContentViewFailure(e);
                return;
            } catch (Exception recoveryException) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + ".onCreate: Recovery failed", recoveryException);
                Toast.makeText(this, getString(R.string.app_launch_failed), Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

        currentLanguage = LanguageManager.getCurrentLanguageCode(this);

        loadingOverlay = findViewById(R.id.menu_loading_overlay);
        if (loadingOverlay != null) {
            loadingOverlay.setOnClickListener(v -> {
                // consume clicks while loading to avoid double taps
            });
        }

        // Setup toolbar with defensive error handling
        try {
            Toolbar toolbar = findViewById(R.id.menu_toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
            } else {
                Log.w("TAG_Soccer", getClass().getSimpleName() + ".onCreate: Toolbar not found in layout, continuing without action bar");
            }
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".onCreate: Failed to setup toolbar", e);
            // Continue without toolbar - non-critical failure
        }

        // Initialize backend service checker
        SoccerApp app = (SoccerApp) getApplication();
        serviceChecker = app.getServiceChecker();
        
        // Get analytics manager for user research
        analyticsManager = app.getAnalyticsManager();
        
        // Check and show in-app messaging for v7/v8 users
        InAppMessagingHelper.checkAndShowVersionMessage(this, analyticsManager);
        
        // Get initial backend availability state from the app
        isBackendAvailable = app.isBackendAvailable();
        // Backend availability check runs in onResume()

        runHousekeeping();          // ← always executed, even on cold resume

        Log.d("TAG_Soccer", "onCreate: Calling MobileAds");
        MobileAds.initialize(this, initializationStatus -> {});
        Log.d("TAG_Soccer", "onCreate: loadInterstitialAd");
        loadInterstitialAd();

    }
    private void loadInterstitialAd() {
        adRetryHandler.removeCallbacks(adRetryRunnable);

        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".loadInterstitialAd: Activity finishing, skipping load"
            );
            return;
        }

        if (isAdLoading) {
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".loadInterstitialAd: load already in progress"
            );
            return;
        }

        isAdLoading = true;

        AdRequest.Builder builder = new AdRequest.Builder();

        if (!ConsentUtils.isPersonalisedAllowed(this)) {
            Bundle extras = new Bundle();
            extras.putString("npa", "1");
            builder.addNetworkExtrasBundle(AdMobAdapter.class, extras);
        }

        AdRequest adRequest = builder.build();

        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".loadInterstitialAd: AD_UNIT_ID=" + BuildConfig.AD_UNIT_ID
        );

        InterstitialAd.load(this, BuildConfig.AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onAdLoaded: Interstitial ad loaded"
                        );
                        mInterstitialAd = interstitialAd;
                        isAdLoading = false;
                        adRetryHandler.removeCallbacks(adRetryRunnable);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName()
                                    + ".onAdFailedToLoad: Interstitial ad failed to load: "
                                    + loadAdError.getMessage()
                                    + ", retrying in "
                                    + AD_RETRY_DELAY_MS
                                    + "ms"
                        );
                        mInterstitialAd = null;
                        isAdLoading = false;
                        adRetryHandler.removeCallbacks(adRetryRunnable);
                        adRetryHandler.postDelayed(adRetryRunnable, AD_RETRY_DELAY_MS);
                    }
                });
    }

    private boolean hasAdsConsent() {
        ConsentInformation ci = UserMessagingPlatform.getConsentInformation(this);
        @ConsentInformation.ConsentStatus int status = ci.getConsentStatus();
        return status == ConsentInformation.ConsentStatus.OBTAINED
                || status == ConsentInformation.ConsentStatus.NOT_REQUIRED;
    }

    private void showConsentRequiredDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.ads_consent_required)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showLoadingOverlay() {
        if (loadingOverlay == null) {
            return;
        }
        overlayHandler.removeCallbacks(hideOverlayRunnable);
        loadingOverlayShownAtMs = SystemClock.elapsedRealtime();
        if (loadingOverlay.getVisibility() != View.VISIBLE) {
            loadingOverlay.setAlpha(0f);
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.animate().alpha(1f).setDuration(150L).start();
        } else {
            loadingOverlay.animate().cancel();
            loadingOverlay.setAlpha(1f);
        }
    }

    private void hideLoadingOverlayWithMinimumDuration() {
        if (loadingOverlay == null) {
            return;
        }
        long elapsed = SystemClock.elapsedRealtime() - loadingOverlayShownAtMs;
        long delay = Math.max(0L, MIN_LOADING_OVERLAY_DURATION_MS - elapsed);
        overlayHandler.removeCallbacks(hideOverlayRunnable);
        overlayHandler.postDelayed(hideOverlayRunnable, delay);
    }

    private void hideLoadingOverlayImmediate() {
        if (loadingOverlay == null) {
            return;
        }
        if (loadingOverlay.getVisibility() != View.VISIBLE) {
            return;
        }
        loadingOverlay.animate().cancel();
        loadingOverlay.animate().alpha(0f).setDuration(150L).withEndAction(() -> {
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
                loadingOverlay.setAlpha(1f);
            }
        }).start();
    }

    private void showRegistrationDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.register_dialog_message)
                .setPositiveButton(R.string.proceed, (dialog, which) -> {
                    startActivity(new Intent(this, UniversalLoginActivity.class));
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    // Show signup decline reason dialog to understand why
                    SignupDeclineReasonDialog.show(this, "registration_dismiss", analyticsManager, reason -> {
                        // User has provided feedback, no further action needed
                        Log.d("TAG_Soccer", "User declined registration, reason: " + reason);
                    });
                })
                .show();
    }

    private void showAdThenRun(Runnable action) {
        showLoadingOverlay();
        Runnable guardedAction = () -> {
            try {
                action.run();
            } finally {
                hideLoadingOverlayWithMinimumDuration();
            }
        };

        // Check consent before proceeding with ads logic
        if (!hasAdsConsent()) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + ".showAdThenRun: No ads consent, running action directly");
            showConsentRequiredDialog();
            guardedAction.run();
            return;
        }

        SharedPreferences prefs =
                getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        
        // Determine if user is authorized to get appropriate frequency
        boolean isUserAuthorized = FirebaseAuth.getInstance().getCurrentUser() != null;
        
        // For unauthorized users, use FAILSAFE_AD_FREQUENCY (1) as default
        // For authorized users, use stored frequency or DEFAULT_AD_FREQUENCY (10)
        int defaultFreq = isUserAuthorized ? DEFAULT_AD_FREQUENCY : FAILSAFE_AD_FREQUENCY;
        final int[] resolvedFrequency = {prefs.getInt(PREF_AD_FREQUENCY, defaultFreq)};

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".showAdThenRun: User authorized=" + isUserAuthorized + 
              ", default frequency=" + defaultFreq + ", stored frequency=" + resolvedFrequency[0]);

        if (isUserAuthorized) {
            // For authorized users, try to fetch frequency from Firebase
            FirebaseFirestore.getInstance()
                    .collection("settings")
                    .document("adsFreuency")
                    .get()
                    .addOnSuccessListener(doc -> {
                        Long freq = doc.getLong("value");
                        if (freq != null) {
                            int refreshedFrequency = Math.max(FAILSAFE_AD_FREQUENCY, freq.intValue());
                            resolvedFrequency[0] = refreshedFrequency;
                            prefs.edit().putInt(PREF_AD_FREQUENCY, refreshedFrequency).apply();
                            Log.d(
                                    "TAG_Soccer",
                                    getClass().getSimpleName() + ".showAdThenRun: refreshed ads frequency=" + refreshedFrequency
                            );
                        } else {
                            resolvedFrequency[0] = DEFAULT_AD_FREQUENCY;
                            prefs.edit().putInt(PREF_AD_FREQUENCY, DEFAULT_AD_FREQUENCY).apply();
                            Log.w(
                                    "TAG_Soccer",
                                    getClass().getSimpleName()
                                            + ".showAdThenRun: missing frequency value for authorized user, defaulting to "
                                            + DEFAULT_AD_FREQUENCY
                            );
                        }
                    })
                    .addOnFailureListener(e -> {
                        resolvedFrequency[0] = DEFAULT_AD_FREQUENCY;
                        prefs.edit().putInt(PREF_AD_FREQUENCY, DEFAULT_AD_FREQUENCY).apply();
                        Log.w(
                                "TAG_Soccer",
                                getClass().getSimpleName()
                                        + ".showAdThenRun: failed to refresh ads frequency for authorized user, defaulting to "
                                        + DEFAULT_AD_FREQUENCY,
                                e
                        );
                    })
                    .addOnCompleteListener(task -> processAdLogic(guardedAction, prefs, resolvedFrequency[0]));
        } else {
            // For unauthorized users, use FAILSAFE_AD_FREQUENCY directly
            resolvedFrequency[0] = FAILSAFE_AD_FREQUENCY;
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".showAdThenRun: unauthorized user, using failsafe frequency=" + FAILSAFE_AD_FREQUENCY);
            processAdLogic(guardedAction, prefs, resolvedFrequency[0]);
        }
    }

    private void processAdLogic(Runnable action, SharedPreferences prefs, int frequency) {
        int counter = prefs.getInt(PREF_AD_COUNTER, 0) + 1;
        
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".processAdLogic: counter=" + counter + 
              ", frequency=" + frequency + ", should show ad=" + (counter >= frequency));
        
        if (counter < frequency) {
            prefs.edit().putInt(PREF_AD_COUNTER, counter).apply();
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".processAdLogic: counter below threshold, running action directly");
            action.run();
            return;
        }
        
        // Reset counter and try to show ad
        prefs.edit().putInt(PREF_AD_COUNTER, 0).apply();

        // Double-check consent before showing ad
        if (!hasAdsConsent()) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + ".processAdLogic: Lost ads consent, running action directly");
            showConsentRequiredDialog();
            action.run();
            return;
        }

        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".processAdLogic: Ad ready=" + (mInterstitialAd != null) + 
                ", consent=" + hasAdsConsent()
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
                    Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onAdShowedFullScreenContent: Ad displayed successfully"
                    );
                    mInterstitialAd = null;
                }
            });

            Log.d("TAG_Soccer", getClass().getSimpleName() + ".processAdLogic: Showing interstitial ad");
            mInterstitialAd.show(this);
        } else {
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".processAdLogic: No cached interstitial - running action now"
            );
            if (!isAdLoading) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".processAdLogic: Starting ad load");
                loadInterstitialAd();
            }
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
                        // After checking for outgoing invites, check for new incoming invites
                        checkForMissedInvitations();
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
                        // After checking for outgoing invites, check for new incoming invites
                        checkForMissedInvitations();
                    }
                })
                .addOnFailureListener(e -> {
                        Log.e(
                                "TAG_Soccer",
                                getClass().getSimpleName() + ".continueWithInviteRestore: failed to query invites",
                                e
                        );
                        // Even if outgoing invite query fails, check for incoming invites
                        checkForMissedInvitations();
                }
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
        showAdThenRun(() -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("GameType", 1);
            startActivity(intent);
        });
    }

    public void OpenGamePlayerVsAndroid(View view) {
        showAdThenRun(() -> {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("GameType", 2);
            startActivity(intent);
        });
    }



    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
//---save whatever you need to persist—
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": MenuActivity.onSaveInstanceState entered");
        super.onSaveInstanceState(outState);
    }

    public void OpenInviteFriend(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showRegistrationDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(MenuActivity.this, FriendsListActivity.class)));
    }

    public void OpenInvites(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showRegistrationDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(this, InvitationsActivity.class)));
    }

    public void OpenTournaments(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showRegistrationDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(this, TournamentsActivity.class)));
    }

    public void OpenRanking(View view) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showRegistrationDialog();
            return;
        }

        showAdThenRun(() -> startActivity(new Intent(this, RankingActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tournaments_menu, menu);
        this.optionsMenu = menu;
        // Store reference to account item for later updates
        accountMenuItem = menu.findItem(R.id.action_account);
        if (accountMenuItem != null) {
            Drawable icon = accountMenuItem.getIcon();
            if (!isBackendAvailable && icon != null) {
                icon.setAlpha(130); // visually dim when offline
            }
        }
        // Hide offline indicator by default or based on current state
        MenuItem offlineItem = menu.findItem(R.id.action_offline);
        if (offlineItem != null) {
            offlineItem.setVisible(!isBackendAvailable);
        }
        checkAndUpdateBlockedInviteWarning(); // Check blocked invite status
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_account) {
            if (!isBackendAvailable) {
                Toast.makeText(this, R.string.server_unavailable_message, Toast.LENGTH_LONG).show();
                return true;
            }
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
        } else if (id == R.id.action_offline) {
            // Clicking the offline icon shows the same toast as when the check fails
            Toast.makeText(this, R.string.server_unavailable_message, Toast.LENGTH_LONG).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adRetryHandler.removeCallbacks(adRetryRunnable);
        overlayHandler.removeCallbacks(hideOverlayRunnable);
    }

    /**
     * Check backend service availability and continue with onResume logic when complete
     */
    private void checkBackendAvailabilityAndContinue() {
        if (isBackendCheckInProgress) {
            Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".checkBackendAvailabilityAndContinue: Check already in progress - skipping"
            );
            return;
        }

        if (serviceChecker == null) {
            Log.w(
                "TAG_Soccer",
                getClass().getSimpleName() + ".checkBackendAvailabilityAndContinue: Service checker not available, assuming backend is available"
            );
            isBackendAvailable = true;
            // Continue with the rest of onResume logic
            continueOnResumeAfterBackendCheck();
            return;
        }

        isBackendCheckInProgress = true;

        Log.d(
            "TAG_Soccer",
            getClass().getSimpleName() + ".checkBackendAvailabilityAndContinue: Checking backend availability before continuing onResume"
        );

        try {
            serviceChecker.checkServiceAvailability(new BackendServiceChecker.ServiceCheckCallback() {
                @Override
                public void onServiceAvailable() {
                    Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".checkBackendAvailabilityAndContinue: Backend is available - continuing onResume"
                );
                runOnUiThread(() -> {
                    isBackendAvailable = true;

                    // Reset flag so next outage will display a toast again
                    backendUnavailableToastShown = false;

                    ((SoccerApp) getApplication()).setBackendAvailable(true);

                    updateUiForAuthState();
                    if (optionsMenu != null) {
                        MenuItem offlineItem = optionsMenu.findItem(R.id.action_offline);
                        if (offlineItem != null) {
                            offlineItem.setVisible(false);
                        }
                        if (accountMenuItem != null && accountMenuItem.getIcon() != null) {
                            accountMenuItem.getIcon().setAlpha(255);
                        }
                    }

                    // Continue with the rest of onResume logic now that backend availability is confirmed
                    continueOnResumeAfterBackendCheck();

                    isBackendCheckInProgress = false;
                });
            }

            @Override
            public void onServiceUnavailable(String reason) {
                Log.w(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".checkBackendAvailabilityAndContinue: Backend is unavailable: " + reason + " - continuing onResume"
                );
                runOnUiThread(() -> {
                    isBackendAvailable = false;
                    ((SoccerApp) getApplication()).setBackendAvailable(false);
                    updateUiForAuthState();
                    if (optionsMenu != null) {
                        MenuItem offlineItem = optionsMenu.findItem(R.id.action_offline);
                        if (offlineItem != null) {
                            offlineItem.setVisible(true);
                        }
                        if (accountMenuItem != null && accountMenuItem.getIcon() != null) {
                            accountMenuItem.getIcon().setAlpha(130);
                        }
                    }
                    // Show toast notification once when the backend becomes unavailable
                    if (!backendUnavailableToastShown) {
                        Toast.makeText(MenuActivity.this,
                                R.string.server_unavailable_message,
                                Toast.LENGTH_LONG).show();
                        backendUnavailableToastShown = true;
                    }

                    // Continue with the rest of onResume logic even when backend is unavailable
                    // (the safeguards in ensureTermsAccepted and fetchNicknameFromFirestore will handle this)
                    continueOnResumeAfterBackendCheck();

                    isBackendCheckInProgress = false;
                });
            }
            });
        } catch (Exception e) {
            isBackendCheckInProgress = false;
            Log.e(
                "TAG_Soccer",
                getClass().getSimpleName() + ".checkBackendAvailabilityAndContinue: Service check failed to start",
                e
            );
        }
    }

    private void registerNetworkCallback() {
        if (networkCallbackRegistered) {
            return;
        }

        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (connectivityManager == null) {
            Log.w(
                "TAG_Soccer",
                getClass().getSimpleName() + ".registerNetworkCallback: ConnectivityManager not available"
            );
            return;
        }

        if (networkCallback == null) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    if (!isBackendAvailable) {
                        Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".registerNetworkCallback: Network available - rechecking backend"
                        );
                        runOnUiThread(() -> checkBackendAvailabilityAndContinue());
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    if (!isNetworkStillAvailable()) {
                        Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".registerNetworkCallback: Network lost"
                        );
                    }
                }
            };
        }

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
            networkCallbackRegistered = true;
        } catch (Exception e) {
            Log.w(
                "TAG_Soccer",
                getClass().getSimpleName() + ".registerNetworkCallback: Failed to register network callback",
                e
            );
        }
    }

    private void unregisterNetworkCallback() {
        if (!networkCallbackRegistered || connectivityManager == null || networkCallback == null) {
            return;
        }

        try {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        } catch (Exception e) {
            Log.w(
                "TAG_Soccer",
                getClass().getSimpleName() + ".unregisterNetworkCallback: Failed to unregister network callback",
                e
            );
        } finally {
            networkCallbackRegistered = false;
        }
    }

    private boolean isNetworkStillAvailable() {
        if (connectivityManager == null) {
            return false;
        }

        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
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

                    // Reset flag so next outage will display a toast again
                    backendUnavailableToastShown = false;

                    ((SoccerApp) getApplication()).setBackendAvailable(true);

                    updateUiForAuthState();
                    if (optionsMenu != null) {
                        MenuItem offlineItem = optionsMenu.findItem(R.id.action_offline);
                        if (offlineItem != null) {
                            offlineItem.setVisible(false);
                        }
                        if (accountMenuItem != null && accountMenuItem.getIcon() != null) {
                            accountMenuItem.getIcon().setAlpha(255);
                        }
                    }
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
                    ((SoccerApp) getApplication()).setBackendAvailable(false);
                    updateUiForAuthState();
                    if (optionsMenu != null) {
                        MenuItem offlineItem = optionsMenu.findItem(R.id.action_offline);
                        if (offlineItem != null) {
                            offlineItem.setVisible(true);
                        }
                        if (accountMenuItem != null && accountMenuItem.getIcon() != null) {
                            accountMenuItem.getIcon().setAlpha(130);
                        }
                    }
                    // Show toast notification once when the backend becomes unavailable
                    if (!backendUnavailableToastShown) {
                        Toast.makeText(MenuActivity.this,
                                R.string.server_unavailable_message,
                                Toast.LENGTH_LONG).show();
                        backendUnavailableToastShown = true;
                    }
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

    /**
     * Handles failures during setContentView() by attempting recovery strategies
     * for AppCompat theme and layout initialization issues.
     */
    private void handleContentViewFailure(Exception originalException) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".handleContentViewFailure: Attempting recovery from setContentView failure");
        
        // Strategy 1: Try to reinitialize with a simple fallback layout
        try {
            // Create a minimal layout programmatically to avoid XML layout issues
            android.widget.LinearLayout fallbackLayout = new android.widget.LinearLayout(this);
            fallbackLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            fallbackLayout.setGravity(android.view.Gravity.CENTER);
            
            android.widget.TextView errorText = new android.widget.TextView(this);
            errorText.setText(getString(R.string.layout_initialization_error));
            errorText.setTextSize(16);
            errorText.setPadding(32, 32, 32, 32);
            errorText.setGravity(android.view.Gravity.CENTER);
            
            android.widget.Button retryButton = new android.widget.Button(this);
            retryButton.setText(getString(R.string.retry));
            retryButton.setOnClickListener(v -> {
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".handleContentViewFailure: User requested retry");
                recreate(); // Restart the activity
            });
            
            fallbackLayout.addView(errorText);
            fallbackLayout.addView(retryButton);
            
            setContentView(fallbackLayout);
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".handleContentViewFailure: Successfully set fallback layout");
            
        } catch (Exception fallbackException) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".handleContentViewFailure: Fallback layout also failed", fallbackException);
            // Re-throw to trigger the outer exception handler
            throw new RuntimeException("Failed to recover from setContentView failure", fallbackException);
        }
    }

    /**
     * Check if user received any invitations while they were offline
     * and show a notification dialog if appropriate
     */
    private void checkForMissedInvitations() {
        // Skip check if backend is unavailable
        if (!isBackendAvailable) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkForMissedInvitations: Skipping - backend unavailable");
            updateLastActiveTimestamp();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkForMissedInvitations: Skipping - user not logged in");
            return;
        }

        SharedPreferences prefs = getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        long lastActiveTimestamp = prefs.getLong(PREF_LAST_ACTIVE_TIMESTAMP, 0L);
        final long lastInvitesSeenTimestamp = prefs.getLong(PREF_LAST_INVITES_SEEN_TIMESTAMP, 0L);
        
        // Update timestamp for next check
        updateLastActiveTimestamp();

        // Skip on first run (no previous timestamp)
        if (lastActiveTimestamp == 0L) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkForMissedInvitations: First run, skipping notification");
            return;
        }

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkForMissedInvitations: Checking for invites since " + lastActiveTimestamp);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        long nowMs = System.currentTimeMillis();

        // Query for invitations and filter client-side to avoid requiring composite Firestore indexes
        db.collection("invitations")
                .whereEqualTo("to", uid)
                .whereIn("status", Arrays.asList("pending", "cancelled", "expired"))
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkForMissedInvitations: No invites for user");
                        return;
                    }

                    DocumentSnapshot recentInvite = null;
                    long mostRecentCreatedAt = Long.MIN_VALUE;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        com.google.firebase.Timestamp createdAtTs = doc.getTimestamp("createdAt");
                        if (createdAtTs == null) {
                            continue;
                        }

                        long createdAtMs = createdAtTs.toDate().getTime();
                        if (createdAtMs <= lastActiveTimestamp) {
                            continue;
                        }

                        String status = doc.getString("status");
                        if (status == null) {
                            continue;
                        }

                        boolean shouldNotify = false;
                        if ("pending".equals(status)) {
                            com.google.firebase.Timestamp expireAtTs = doc.getTimestamp("expireAt");
                            if (expireAtTs == null || expireAtTs.toDate().getTime() > nowMs) {
                                shouldNotify = true;
                            } else {
                                Log.d(
                                        "TAG_Soccer",
                                        getClass().getSimpleName()
                                                + ".checkForMissedInvitations: Pending invite expired before notification could be shown"
                                );
                                shouldNotify = true; // Still notify so user knows invite was missed
                            }
                        } else if ("cancelled".equals(status) || "expired".equals(status)) {
                            shouldNotify = true;
                        }

                        if (shouldNotify && createdAtMs > mostRecentCreatedAt) {
                            mostRecentCreatedAt = createdAtMs;
                            recentInvite = doc;
                        }
                    }

                    if (recentInvite == null) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkForMissedInvitations: No new invites after filtering");
                        return;
                    }

                    if (mostRecentCreatedAt <= lastInvitesSeenTimestamp) {
                        Log.d(
                                "TAG_Soccer",
                                getClass().getSimpleName()
                                        + ".checkForMissedInvitations: Latest invite already viewed in InvitationsActivity; skipping dialog"
                        );
                        return;
                    }

                    // Check if invite is still valid (not expired)
                    String status = recentInvite.getString("status");
                    com.google.firebase.Timestamp expireAtTs = recentInvite.getTimestamp("expireAt");
                    boolean inviteActive = expireAtTs != null && expireAtTs.toDate().getTime() > nowMs;

                    if ("pending".equals(status) && !inviteActive) {
                        Log.d(
                                "TAG_Soccer",
                                getClass().getSimpleName() + ".checkForMissedInvitations: Invite found but already expired"
                        );
                    }

                    Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName()
                                    + ".checkForMissedInvitations: Found missed invite (status="
                                    + status
                                    + "), showing dialog"
                    );
                    showMissedInviteDialog();
                })
                .addOnFailureListener(e ->
                        Log.e(
                                "TAG_Soccer",
                                getClass().getSimpleName() + ".checkForMissedInvitations: Failed to query invites",
                                e
                        )
                );
    }

    /**
     * Show dialog informing user about missed invitation
     */
    private void showMissedInviteDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.missed_invite_title)
                .setMessage(R.string.missed_invite_message)
                .setPositiveButton(R.string.see_invites, (dialog, which) -> {
                    startActivity(new Intent(this, InvitationsActivity.class));
                })
                .setNegativeButton(R.string.close, null)
                .show();
    }

    /**
     * Update the last active timestamp in SharedPreferences
     */
    private void updateLastActiveTimestamp() {
        SharedPreferences prefs = getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        prefs.edit().putLong(PREF_LAST_ACTIVE_TIMESTAMP, System.currentTimeMillis()).apply();
    }

}
