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

import java.util.HashMap;
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
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": APP RETURNS TO FOREGROUND");
        FirebaseDatabase.getInstance().goOnline();
        cancelHeartbeat();

        DatabaseReference info = FirebaseDatabase.getInstance()
                .getReference(".info/connected");

        ValueEventListener[] holder = new ValueEventListener[1];   // tidy removal
        holder[0] = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                if (!Boolean.TRUE.equals(snap.getValue(Boolean.class))) return;

                /* socket is really up → broadcast presence */
                userStatusDbRef.onDisconnect().updateChildren(isOffline);
                userStatusDbRef.updateChildren(isOnline)
                        .addOnSuccessListener(v ->
                                Log.d("TAG_Soccer", "✅ presence=online written"));

                info.removeEventListener(holder[0]);   // listen just once
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { /*ignore*/ }
        };
        info.addValueEventListener(holder[0]);
    }


    /* ------------ APP GOES TO BACKGROUND ------------------------------ */
    @Override public void onStop(@NonNull LifecycleOwner owner) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": APP GOES TO BACKGROUND");
        /* 1. Build one single map */
        Map<String,Object> offline = new HashMap<>();
        offline.put("state",         "offline");
        offline.put("last_changed",  ServerValue.TIMESTAMP);
        offline.put("last_heartbeat",ServerValue.TIMESTAMP);   // 👈 NEW

        /* 2. Flush it to RTDB first … */
        userStatusDbRef.updateChildren(offline)
                .addOnSuccessListener(v -> {
                    /* … then shut the socket down */
                    FirebaseDatabase.getInstance().goOffline();
                    scheduleHeartbeat();          // keep the 15-min pulses if you like
                })
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": ❌ offline write failed", e));
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
