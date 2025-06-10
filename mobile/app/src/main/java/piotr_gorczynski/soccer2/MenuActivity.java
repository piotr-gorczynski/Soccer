package piotr_gorczynski.soccer2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

// WorkManager
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;

import java.util.concurrent.TimeUnit;           // for TimeUnit.MINUTES

import android.content.Context;          // ← choose this one


public class MenuActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": InvitationsActivity onNewIntent: " + intent.toUri(Intent.URI_INTENT_SCHEME));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String nickname = prefs.getString("nickname", null);

        TextView nicknameLabel = findViewById(R.id.nicknameLabel);
        if (nickname != null && !nickname.isEmpty()) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Nickname: "+nickname);
            nicknameLabel.setText(getString(R.string.hello_nickname, nickname));
        } else {
            nicknameLabel.setText(getString(R.string.welcome_to_soccer));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        /*
        FirebaseFirestore.getInstance()
                .clearPersistence()            // <-- clean cash
                .addOnSuccessListener(v ->
                        Log.d("TAG_Soccer","Firestore cache cleared"));*/

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to get FCM token", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": 🔑 Token from MenuActivity: " + token);

                    String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                            : null;

                    if (uid != null) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": uid: "+uid);
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .update("fcmToken", token)
                                .addOnSuccessListener(aVoid -> Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ✅ Token saved"))
                                .addOnFailureListener(e -> Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ❌ Failed to save token", e));
                    } else {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ⚠️ No logged-in user; token not saved");
                    }
                });
        // ✅ Call permission request
        requestNotificationPermissionIfNeeded();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "invite_channel",
                    "Game Invites",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ✅ Notification permission granted");
            } else {
                Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ❌ Notification permission denied");
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }


    public void OpenGamePlayerVsPlayer(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("GameType",1);
        startActivity(intent);
    }

    public void OpenGamePlayerVsAndroid(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("GameType",2);
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
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": MenuActivity.onSaveInstanceState entered");
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


    @Override
    protected void onStart() {
        super.onStart();

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;   // not signed in

        cancelHeartbeat();      // ⬅ stop periodic pings while UI is visible

        /* ------------------------------------------------------------------ */
        /* 0️⃣  Show which DB URL the SDK resolved                             */
        /* ------------------------------------------------------------------ */
        DatabaseReference root = FirebaseDatabase.getInstance().getReference();
        Log.d("TAG_Soccer", "RTDB root = " + root);
        // should print: https://soccer-dev-1744877837-default-rtdb.firebaseio.com/

        /* ------------------------------------------------------------------ */
        /* Build the presence references & values                             */
        /* ------------------------------------------------------------------ */
        DatabaseReference userStatusDbRef = root.child("status").child(uid);

        Map<String, Object> isOnline  = new HashMap<>();
        Map<String, Object> isOffline = new HashMap<>();
        isOnline .put("state", "online");   isOnline .put("last_changed", ServerValue.TIMESTAMP);
        isOffline.put("state", "offline");  isOffline.put("last_changed", ServerValue.TIMESTAMP);

        // keep a tiny node in sync so the SDK stays connected
        root.child("connection-stay-awake").keepSynced(true);

        /* ------------------------------------------------------------------ */
        /* 1️⃣  Wait for a *real* connection, then schedule onDisconnect       */
        /* ------------------------------------------------------------------ */
        root.child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                Boolean connected = snap.getValue(Boolean.class);
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": .info/connected = " + connected);

                if (!Boolean.TRUE.equals(connected)) return;  // not yet online

                /* ---------------------------------------------------------- */
                /* 2️⃣  Schedule offline-write, *then* write "online"         */
                /* ---------------------------------------------------------- */
                userStatusDbRef.onDisconnect().setValue(isOffline)
                        .addOnFailureListener(e ->
                                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                        + ": ❌ onDisconnect().setValue failed", e))
                        .addOnSuccessListener(v -> userStatusDbRef.setValue(isOnline)
                                .addOnSuccessListener(x ->
                                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                                + ": ✅ presence=online written"))
                                .addOnFailureListener(e ->
                                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                                + ": ❌ presence write failed", e)));
            }

            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": ❌ .info/connected cancelled", e.toException());
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        scheduleHeartbeat();    // ⬅ start pings while we’re in background

        DatabaseReference userStatusDbRef =
                FirebaseDatabase.getInstance().getReference("status").child(uid);

        userStatusDbRef.setValue("offline")
                .addOnFailureListener(e ->
                        Log.e( "TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": ❌ manual offline write failed", e));
    }

    private static final String HEARTBEAT_WORK = "presence-heartbeat";

    private void scheduleHeartbeat() {
        PeriodicWorkRequest req =
                new PeriodicWorkRequest.Builder(
                        HeartbeatWorker.class,
                        15, TimeUnit.MINUTES)        // WorkManager’s minimum
                        .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                HEARTBEAT_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                req);
    }

    private void cancelHeartbeat() {
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

            FirebaseDatabase.getInstance()
                    .getReference("status")
                    .child(uid)
                    .child("last_heartbeat")
                    .setValue(ServerValue.TIMESTAMP);

            return Result.success();
        }
    }



}
