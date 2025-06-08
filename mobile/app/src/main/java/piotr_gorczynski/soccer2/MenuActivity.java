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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;



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

        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));

        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (connected != null && connected) {
                    userDocRef.update("online", true);
                    userDocRef.update("lastOnline", com.google.firebase.firestore.FieldValue.serverTimestamp());
                } else {
                    userDocRef.update("online", false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("TAG", "Listener cancelled", error.toException());
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        DocumentReference userDocRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(Objects.requireNonNull(FirebaseAuth.getInstance().getUid()));

        userDocRef.update("online", false);
    }

}
