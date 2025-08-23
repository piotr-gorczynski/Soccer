package piotr_gorczynski.soccer2;


import android.app.Application;
import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.widget.Toast;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;

import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.FormError;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LifecycleOwner;

public class SoccerApp extends Application implements DefaultLifecycleObserver {

    private DatabaseReference userStatusDbRef;
    private BackendServiceChecker serviceChecker;
    private boolean isBackendAvailable = true; // assume available initially

    /* Creates {state:"online", last_heartbeat:TS} */
    private static Map<String,Object> buildOnline() {
        return Map.of(
                "state",          "online",
                "last_heartbeat", ServerValue.TIMESTAMP
        );
    }
    /* Creates {state:"offline", last_heartbeat:TS} */


    /* App is in background ➜ still logged-in, show last-seen */
    private static Map<String, Object> buildAway() {
        return Map.of(
                "state",          "offline",
                "last_heartbeat", ServerValue.TIMESTAMP   // numeric
        );
    }

    /* Explicit logout ➜ no more push, no last-seen label */
    private static Map<String, Object> buildLoggedOut() {
        return Map.of(
                "state",          "offline",
                "last_heartbeat", 0L                      // sentinel
        );
    }


    @Override
    public void onCreate() {
        super.onCreate();

        try {
            // Try to load Facebook client token from assets first, then fall back to strings.xml
            String clientToken = loadFacebookClientTokenFromAssets();
            int tokenRes = getResources().getIdentifier("facebook_client_token", "string", getPackageName());
            int appIdRes = getResources().getIdentifier("facebook_app_id", "string", getPackageName());

            // Use token from assets if available, otherwise fall back to strings.xml
            boolean hasToken = false;
            if (clientToken != null && !clientToken.isEmpty()) {
                hasToken = true;
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".onCreate: Using Facebook client token from assets");
            } else if (tokenRes != 0 && !getString(tokenRes).isEmpty() && !getString(tokenRes).equals("CLIENT_TOKEN_TO_BE_CONFIGURED")) {
                clientToken = getString(tokenRes);
                hasToken = true;
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".onCreate: Using Facebook client token from strings.xml");
            }

            boolean hasAppId = appIdRes != 0 && !getString(appIdRes).isEmpty();

            Log.d("TAG_Soccer", getClass().getSimpleName() + ".onCreate: Facebook config check - hasToken=" + hasToken + ", hasAppId=" + hasAppId);
            
            if (hasToken && hasAppId) {
                FacebookSdk.setClientToken(clientToken);
                FacebookSdk.setApplicationId(getString(appIdRes));
                FacebookSdk.sdkInitialize(getApplicationContext());
                AppEventsLogger.activateApp(this);
                Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".onCreate: Facebook SDK initialized"
                );
            } else {
                if (!hasAppId) {
                    Log.w(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onCreate: Facebook App ID missing or empty"
                    );
                }
                if (!hasToken) {
                    Log.w(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onCreate: Facebook Client Token missing, empty, or placeholder. " +
                            "Please configure 'facebook_client_token' either as an asset file or in strings.xml with your actual client token from Facebook App Dashboard."
                    );
                }
                Log.w(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".onCreate: Facebook SDK not configured; skipping initialization. " +
                        "To fix: Add your Facebook Client Token as an asset file or to strings.xml as 'facebook_client_token'."
                );
            }
        } catch (Throwable t) {
            Log.w(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".onCreate: Facebook SDK init failed",
                    t
            );
        }

        String code = LanguageManager.getCurrentLanguageCode(this);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code));
        // Ensure the app uses the saved language before any UI is shown
        checkLanguagePreference();

        // Disable FCM auto-init until a user signs in
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());


        // Initialize backend service checker
        serviceChecker = new BackendServiceChecker(this);
        
        // Configure default project ID if needed
        // This could be moved to a configuration activity later
        configureDefaultProjectId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1️⃣  Wipe the local cache & cached rules once per cold start
        db.terminate().addOnCompleteListener(termTask -> {
            Task<Void> clearTask;
            if (termTask.isSuccessful()) {
                clearTask = db.clearPersistence();
            } else {
                Log.w(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".onCreate: terminate failed",
                        termTask.getException()
                );
                clearTask = Tasks.forResult(null);
            }

            clearTask.addOnCompleteListener(t -> {
                if (!t.isSuccessful()) {
                    Log.w(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onCreate: clearPersistence failed",
                            t.getException()
                    );
                }

                // 2️⃣  Now it’s safe to register listeners or use Firestore
                ProcessLifecycleOwner.get()
                        .getLifecycle()
                        .addObserver(this);

                FirebaseAuth auth = FirebaseAuth.getInstance();
                auth.addAuthStateListener(a -> {
                    if (a.getCurrentUser() != null) {
                        startPresence(a.getCurrentUser().getUid());
                        enableFcmAutoInit();
                    } else {
                        stopPresence();
                        disableFcmAutoInit();
                    }
                });

                // handle “already signed-in” on cold start
                if (auth.getCurrentUser() != null) {
                    startPresence(auth.getCurrentUser().getUid());
                    enableFcmAutoInit();
                }
            });
        });

        MobileAds.initialize(this, initializationStatus -> {});
    }
    public void syncFcmTokenIfNeeded() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(newToken -> {
                    SharedPreferences prefs =
                            getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
                    String saved = prefs.getString("fcmToken", null);
                    if (saved != null && saved.equals(newToken)) return;

                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(uid)
                            .set(Map.of("fcmToken", newToken), SetOptions.merge())
                            .addOnSuccessListener(v ->
                                    prefs.edit().putString("fcmToken", newToken).apply());
                });
    }

    public void disableFcmAutoInit() {
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);
    }

    public void enableFcmAutoInit() {
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        syncFcmTokenIfNeeded();
    }

    /* ---------------- central place to start presence tracking ---------- */
    private void startPresence(@NonNull String uid) {

        if (userStatusDbRef != null                      // already tracking?
                && Objects.requireNonNull(userStatusDbRef.getKey()).equals(uid)) return;

        userStatusDbRef = FirebaseDatabase.getInstance()
                .getReference("status").child(uid);

        userStatusDbRef.onDisconnect().updateChildren(buildAway());
        setUserOnline();
        cancelHeartbeat();
    }

    /* ---------------- tidy up when a user signs out --------------------- */
    private void stopPresence() {

        if (userStatusDbRef == null) return;   // nothing to do

        cancelHeartbeat();                     // stop WM

        // Mark “logged-out” exactly once
        userStatusDbRef.updateChildren(buildLoggedOut());

        userStatusDbRef = null;                // disable onStart/onStop
    }


    /* ------------ APP RETURNS TO FOREGROUND --------------------------- */
    @Override public void onStart(@NonNull LifecycleOwner owner) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": APP RETURNS TO FOREGROUND");

        // Always apply the saved language when returning to the foreground
        checkLanguagePreference();

        if (userStatusDbRef == null) return;             // ← ADD
        FirebaseDatabase.getInstance().goOnline();
        cancelHeartbeat();

        setUserOnline();                     // ← run it right away
    }


    /* ------------ APP GOES TO BACKGROUND ------------------------------ */
    @Override public void onStop(@NonNull LifecycleOwner owner) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": APP GOES TO BACKGROUND");
        if (userStatusDbRef == null) return;             // ← ADD

        Map<String,Object> offline = buildAway();      // fresh TS each time

        userStatusDbRef.setValue(offline)                 // atomic write
                .addOnSuccessListener(v -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ✅ calling goOffline()");
                    FirebaseDatabase.getInstance().goOffline();
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ✅ calling scheduleHeartbeat()");
                    scheduleHeartbeat();                      // 15-min pulses
                })
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": ❌ seting value offline failed", e));
    }

    /* -------------- same heartbeat worker you already have ----- */
    private static final String HEARTBEAT_WORK = "presence-heartbeat";

    private void scheduleHeartbeat() {
        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(
                        HeartbeatWorker.class,
                        15, TimeUnit.MINUTES)        // WorkManager’s minimum
                        .build();

        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": Launching WorkManager");
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                HEARTBEAT_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                req);
    }

    private void cancelHeartbeat() {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": Cancelling WorkManager");
        WorkManager wm = WorkManager.getInstance(this);
        wm.cancelUniqueWork(HEARTBEAT_WORK);
    }
    public static class HeartbeatWorker extends Worker {

        public HeartbeatWorker(@NonNull Context context,
                               @NonNull WorkerParameters params) {
            super(context, params);
        }

        @NonNull
        @Override
        public Result doWork() {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) return Result.success();

            /* open connection just long enough for the write */
            FirebaseDatabase.getInstance().goOnline();

            DatabaseReference hbRef = FirebaseDatabase.getInstance()
                    .getReference("status")
                    .child(uid);
            Map<String,Object> pulse = Map.of(
                "last_heartbeat", ServerValue.TIMESTAMP
            );
            try {
                Tasks.await( hbRef.updateChildren(pulse) );      // block until ACK
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ":✅ heartbeat written for uid=" + uid);
                return Result.success();
            } catch (Exception e) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": ❌ heartbeat write failed", e);
                return Result.retry();          // let WM try again later
            } finally {
                FirebaseDatabase.getInstance().goOffline();      // always close
            }

        }
    }
    private void setUserOnline() {
        // Only set user online if user is authenticated
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                    + ": User not authenticated, skipping setUserOnline");
            return;
        }

        userStatusDbRef.setValue(buildOnline())
                .addOnSuccessListener(v ->
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ✅ setUserOnline ok"))
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ❌ setUserOnline failed", e));
    }

    public Task<Void> forceUserOffline(@NonNull String uid) {
        // Validate that uid is not null or empty
        if (uid == null || uid.isEmpty()) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                    + ": Cannot force user offline - invalid UID");
            return Tasks.forException(new IllegalArgumentException("Invalid UID"));
        }

        cancelHeartbeat();                                   // stop worker first

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("status")
                .child(uid);

        Map<String, Object> offline = new HashMap<>();
        offline.put("state",          "offline");
        offline.put("last_heartbeat", 0L);

        return ref.updateChildren(offline)     // <-- return the Task to caller
                .addOnSuccessListener(v ->
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": ✅ user " + uid + " marked offline"))
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": ❌ could not mark offline", e));
    }

    /**
     * Check backend service availability
     */
    private void checkBackendAvailability() {
        if (serviceChecker == null) return;
        
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkBackendAvailability: Checking backend service availability");
        serviceChecker.checkServiceAvailability(new BackendServiceChecker.ServiceCheckCallback() {
            @Override
            public void onServiceAvailable() {
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkBackendAvailability: Backend service is available");
                isBackendAvailable = true;
                // Notify any listeners that backend is available
                notifyBackendAvailabilityChanged(true);
            }

            @Override
            public void onServiceUnavailable(String reason) {
                Log.w("TAG_Soccer", getClass().getSimpleName() + ".checkBackendAvailability: Backend service is unavailable: " + reason);
                isBackendAvailable = false;
                // Notify any listeners that backend is unavailable
                notifyBackendAvailabilityChanged(false);
            }
        });
    }
    
    /**
     * Get current backend availability status
     */
    public boolean isBackendAvailable() {
        return isBackendAvailable;
    }

    /**
     * Update backend availability status so other activities can query
     * the latest value.
     */
    public void setBackendAvailable(boolean available) {
        isBackendAvailable = available;
    }
    
    /**
     * Get the backend service checker instance
     */
    public BackendServiceChecker getServiceChecker() {
        return serviceChecker;
    }
    
    /**
     * Notify listeners about backend availability changes
     * This can be enhanced with proper event bus or observer pattern
     */
    private void notifyBackendAvailabilityChanged(boolean available) {
        // For now, we'll let activities query the state directly
        // This could be enhanced with a proper event system if needed
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".notifyBackendAvailabilityChanged: Backend availability changed: " + available);
    }
    
    /**
     * Configure default project ID if not already set
     */
    private void configureDefaultProjectId() {
        SharedPreferences prefs =
                getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);

        // Always attempt to read the project ID from google-services.json
        String firebaseProjectId = null;
        try {
            FirebaseApp app = FirebaseApp.initializeApp(this);
            if (app != null && app.getOptions() != null) {
                firebaseProjectId = app.getOptions().getProjectId();
            }
        } catch (IllegalStateException e) {
            Log.w("TAG_Soccer", "FirebaseApp not initialized", e);
            prefs.edit().remove("backend_project_id").apply();
            return;
        }

        if (firebaseProjectId == null || firebaseProjectId.isEmpty()) {
            Log.w("TAG_Soccer", "Project ID missing in google-services.json");
            prefs.edit().remove("backend_project_id").apply();
            return;
        }

        serviceChecker.setProjectId(firebaseProjectId);
        // Clean up any stored value from older versions
        prefs.edit().remove("backend_project_id").apply();
        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".configureDefaultProjectId: Set project ID to " + firebaseProjectId
        );
    }
    
    /**
     * Debug method to manually test backend service availability
     * Can be called from debugging sessions
     */
    public void debugTestBackendService() {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".debugTestBackendService: === DEBUG: Testing Backend Service ===");
        if (serviceChecker != null) {
            serviceChecker.testServiceCheck();
        } else {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".debugTestBackendService: Service checker not initialized");
        }
    }

    public void requestConsent(Activity activity) {
        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".requestConsent: starting"
        );

        ConsentRequestParameters params = new ConsentRequestParameters
                .Builder()
                .setTagForUnderAgeOfConsent(false)
                .build();

        ConsentInformation consentInformation = UserMessagingPlatform.getConsentInformation(activity);
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".requestConsent: consent info updated. form available=" + consentInformation.isConsentFormAvailable()
                    );
                    if (consentInformation.isConsentFormAvailable()) {
                        loadAndShowConsentForm(activity);
                    }
                },
                formError -> Log.w("TAG_Soccer", "UMP: Failed to update consent info: " + formError.getMessage())
        );
    }

    public void showAdsConsentForm(Activity activity) {
        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() + ".showAdsConsentForm: showing privacy options"
        );

        if (!isNetworkAvailable(activity)) {
            Toast.makeText(
                    activity,
                    R.string.no_internet_for_privacy_options,
                    Toast.LENGTH_LONG)
                    .show();
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".showAdsConsentForm: no network"
            );
            return;
        }

        /* in SoccerApp.showAdsConsentForm() */
        UserMessagingPlatform.showPrivacyOptionsForm(
                activity,
                formError -> {
                    if (formError != null) {
                        Log.w("TAG_Soccer", "UMP: " + formError.getMessage());
                        return;
                    }

                    boolean personalised = ConsentUtils.isPersonalisedAllowed(activity);

                    PreferenceManager.getDefaultSharedPreferences(activity)
                            .edit()
                            .putBoolean("personalised_ads", personalised)
                            .apply();

                    Log.d("TAG_Soccer",
                            "showAdsConsentForm: user selected "
                                    + (personalised ? "PERSONALISED" : "NPA"));
                });
    }

    private void loadAndShowConsentForm(Activity activity) {
        UserMessagingPlatform.loadConsentForm(
                activity,
                consentForm -> {
                    Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".loadAndShowConsentForm: form loaded"
                    );
                    if (UserMessagingPlatform.getConsentInformation(activity).getConsentStatus()
                            == ConsentInformation.ConsentStatus.REQUIRED) {
                        consentForm.show(
                                activity,
                                formError -> {
                                    if (formError != null) {
                                        Log.w("TAG_Soccer", "UMP: Consent form error: " + formError.getMessage());
                                    } else {
                                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".loadAndShowConsentForm: form dismissed");
                                        Log.d(
                                                "TAG_Soccer",
                                                getClass().getSimpleName() +
                                                        ".loadAndShowConsentForm: consent status=" +
                                                        UserMessagingPlatform.getConsentInformation(activity)
                                                                .getConsentStatus()
                                        );
                                        boolean personalised = ConsentUtils.isPersonalisedAllowed(activity);
                                        PreferenceManager.getDefaultSharedPreferences(activity)
                                                .edit().putBoolean("personalised_ads", personalised).apply();
                                    }
                                });
                    } else {
                        Log.d(
                                "TAG_Soccer",
                                getClass().getSimpleName() + ".loadAndShowConsentForm: consent not required"
                        );
                    }
                },
                formError -> Log.w("TAG_Soccer", "UMP: Failed to load consent form: " + formError.getMessage())
        );
    }

    private static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Check language preference and apply it, or load from Firestore if user is logged in
     */
    private void checkLanguagePreference() {
        String currentLanguageCode = LanguageManager.getCurrentLanguageCode(this);
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".checkLanguagePreference: current language code: " + currentLanguageCode);
        
        // Apply the current language setting
        LanguageManager.applyLanguage(this, currentLanguageCode);
        
        // If user is logged in, load language preference from Firestore
        LanguageManager.loadLanguageFromFirestore(this);
    }

    /**
     * Load Facebook client token from assets if available
     * @return The client token string, or null if not found
     */
    private String loadFacebookClientTokenFromAssets() {
        try (InputStream is = getAssets().open("facebook_client_token");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line = reader.readLine();
            if (line != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".loadFacebookClientTokenFromAssets: Loaded client token from asset");
                    return trimmed;
                }
            }
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".loadFacebookClientTokenFromAssets: facebook_client_token asset was empty");
        } catch (IOException e) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".loadFacebookClientTokenFromAssets: facebook_client_token asset not found", e);
        }
        return null;
    }


}
