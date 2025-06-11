package piotr_gorczynski.soccer2;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;

import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LifecycleOwner;

public class SoccerApp extends Application implements DefaultLifecycleObserver {

    private DatabaseReference userStatusDbRef;
    private Map<String,Object> isOnline, isOffline;

    @Override
    public void onCreate() {
        super.onCreate();

        /* --- one-time setup identical to what you had ------------------ */
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        userStatusDbRef = root.child("status").child(uid);

        isOnline  = Map.of("state","online" , "last_changed", ServerValue.TIMESTAMP);
        isOffline = Map.of("state","offline", "last_changed", ServerValue.TIMESTAMP);

        root.child("connection-stay-awake").keepSynced(true);
        userStatusDbRef.onDisconnect().updateChildren(isOffline);

        /* --- observe the process-wide lifecycle ------------------------ */
        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(this);         // <-- THIS is the switch
    }

    /* ------------ APP RETURNS TO FOREGROUND --------------------------- */
    @Override public void onStart(@NonNull LifecycleOwner owner) {
        FirebaseDatabase.getInstance().goOnline();
        cancelHeartbeat();
        userStatusDbRef.updateChildren(isOnline);
        Log.d("TAG_Soccer", "⏫ App to FOREGROUND → presence=online");
    }

    /* ------------ APP GOES TO BACKGROUND ------------------------------ */
    @Override public void onStop(@NonNull LifecycleOwner owner) {
        FirebaseDatabase.getInstance().goOffline();
        userStatusDbRef.updateChildren(isOffline);
        scheduleHeartbeat();
        Log.d("TAG_Soccer", "⏬ App to BACKGROUND → presence=offline");
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
                ExistingPeriodicWorkPolicy.UPDATE,
                req);
    }

    private void cancelHeartbeat() {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": Cancelling WorkManager");
        WorkManager.getInstance(this).cancelUniqueWork(HEARTBEAT_WORK);
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

            DatabaseReference hbRef = FirebaseDatabase.getInstance()
                    .getReference("status")
                    .child(uid)
                    .child("last_heartbeat");

            hbRef.setValue(ServerValue.TIMESTAMP)
                    .addOnSuccessListener(v ->
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": ✅ heartbeat written for uid=" + uid))
                    .addOnFailureListener(e ->
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": ❌ heartbeat write failed for uid=" + uid, e));

            return Result.success();
        }
    }

}
