package piotr_gorczynski.soccer2;


import android.app.Application;
import android.app.Activity;
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

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.FormError;

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

        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());


        // Initialize backend service checker
        serviceChecker = new BackendServiceChecker(this);
        
        // Configure default project ID if needed
        // This could be moved to a configuration activity later
        configureDefaultProjectId();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1️⃣  Wipe the local cache & cached rules once per cold start
        db.clearPersistence().addOnCompleteListener(t -> {
            if (!t.isSuccessful()) {
                Log.w("TAG_Soccer", getClass().getSimpleName() + ".onCreate: clearPersistence failed", t.getException());
            }

            // 2️⃣  Now it’s safe to register listeners or use Firestore
            ProcessLifecycleOwner.get()
                    .getLifecycle()
                    .addObserver(this);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.addAuthStateListener(a -> {
                if (a.getCurrentUser() != null) {
                    startPresence(a.getCurrentUser().getUid());
                    syncFcmTokenIfNeeded();
                } else {
                    stopPresence();
                }
            });

            // handle “already signed-in” on cold start
            if (auth.getCurrentUser() != null) {
                startPresence(auth.getCurrentUser().getUid());
            }
        });

        MobileAds.initialize(this, initializationStatus -> {});
    }
    public void syncFcmTokenIfNeeded() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(newToken -> {
                    SharedPreferences prefs = getSharedPreferences(
                            getPackageName() + "_preferences", MODE_PRIVATE);
                    String saved = prefs.getString("fcmToken", null);
                    if (saved != null && saved.equals(newToken)) return;

                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .update("fcmToken", newToken)
                            .addOnSuccessListener(v ->
                                    prefs.edit().putString("fcmToken", newToken).apply());
                });
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
        
        // Check backend availability when app returns to foreground
        checkBackendAvailability();
        
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

        userStatusDbRef.setValue(buildOnline())
                .addOnSuccessListener(v ->
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ✅ setUserOnline ok"))
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ❌ setUserOnline failed", e));
    }

    public Task<Void> forceUserOffline(@NonNull String uid) {

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
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String existingProjectId = prefs.getString("backend_project_id", null);
        
        if (existingProjectId == null || existingProjectId.isEmpty()) {
            // Set default project ID based on expected pattern
            // Use the default project ID that matches our Cloud Build
            // environment. This ensures the service-check function URL is
            // correct on fresh installs.
            String defaultProjectId = "soccer-dev-1744877837";
            serviceChecker.setProjectId(defaultProjectId);
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".configureDefaultProjectId: Configured default project ID: " + defaultProjectId);
        }
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



}
