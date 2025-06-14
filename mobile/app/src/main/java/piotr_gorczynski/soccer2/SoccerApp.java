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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LifecycleOwner;

public class SoccerApp extends Application implements DefaultLifecycleObserver {

    private DatabaseReference userStatusDbRef;

    /* Creates {state:"online", last_heartbeat:TS} */
    private static Map<String,Object> buildOnline() {
        return Map.of(
                "state",          "online",
                "last_heartbeat", ServerValue.TIMESTAMP
        );
    }
    /* Creates {state:"offline", last_heartbeat:TS} */
    private static Map<String,Object> buildOffline() {
        return Map.of(
                "state",          "offline",
                "last_heartbeat", ServerValue.TIMESTAMP
        );
    }


    @Override
    public void onCreate() {
        super.onCreate();

        /* --- one-time setup identical to what you had ------------------ */
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        userStatusDbRef = root.child("status").child(uid);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": Writing to " + userStatusDbRef);

        userStatusDbRef.onDisconnect().updateChildren(buildOffline());

        root.child("connection-stay-awake").keepSynced(true);

        /* --- observe the process-wide lifecycle ------------------------ */
        ProcessLifecycleOwner.get()
                .getLifecycle()
                .addObserver(this);         // <-- THIS is the switch
    }

    /* ------------ APP RETURNS TO FOREGROUND --------------------------- */
    @Override public void onStart(@NonNull LifecycleOwner owner) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": APP RETURNS TO FOREGROUND");
        FirebaseDatabase.getInstance().goOnline();
        cancelHeartbeat();

        DatabaseReference info = FirebaseDatabase.getInstance()
                .getReference(".info/connected");

        info.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (!Boolean.TRUE.equals(snap.getValue(Boolean.class))) return;

                setUserOnline();    // ✨ ONE call does everything
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }


    /* ------------ APP GOES TO BACKGROUND ------------------------------ */
    @Override public void onStop(@NonNull LifecycleOwner owner) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": APP GOES TO BACKGROUND");

        Map<String,Object> offline = buildOffline();      // fresh TS each time

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
                    .child(uid);
            hbRef.setValue(buildOnline())
                    .addOnSuccessListener(v ->
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": ✅ heartbeat written for uid=" + uid))
                    .addOnFailureListener(e ->
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": ❌ heartbeat write failed for uid=" + uid, e));

            return Result.success();
        }
    }
    private void setUserOnline() {

        userStatusDbRef.setValue(buildOnline())
                .addOnSuccessListener(v ->
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ✅ setUserOnline worked"))
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": ❌ setUserOnline failed", e));
        userStatusDbRef.onDisconnect().updateChildren(buildOffline());
    }

}
