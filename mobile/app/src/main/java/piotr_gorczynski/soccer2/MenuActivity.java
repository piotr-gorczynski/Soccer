package piotr_gorczynski.soccer2;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.messaging.FirebaseMessaging;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Collections;
import java.util.Objects;


public class MenuActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 101;

    private static final String PREF_FCM_TOKEN = "fcmToken";

    /* ───────────── misc tasks that must always run on launch ───────────── */
    private void runHousekeeping() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
            }.getClass().getEnclosingMethod()).getName()
                    + ": ⚠️ No logged-in user; token not saved");
            return;
        }
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName()
                                + ": ❌ Failed to get FCM token", task.getException());
                        return;
                    }

                    String newToken = task.getResult();

                    String savedToken = prefs.getString(PREF_FCM_TOKEN, null);
                    if (newToken != null && newToken.equals(savedToken)) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName()
                                + ": 🔑 FCM token unchanged; skip Firestore write");
                        return;                     // ← exit early
                    }

                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .update("fcmToken", newToken)
                            .addOnSuccessListener(v -> {
                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName()
                                        + ": ✅ FCM token saved");
                                prefs.edit().putString(PREF_FCM_TOKEN, newToken).apply();
                            })
                            .addOnFailureListener(e ->
                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                                    }.getClass().getEnclosingMethod()).getName()
                                            + ": ❌ Failed to save FCM token", e));
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": InvitationsActivity onNewIntent: " + intent.toUri(Intent.URI_INTENT_SCHEME));
    }

    @Override
    protected void onResume() {
        super.onResume();
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
    }

    private void updateUiForAuthState() {
        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

        Button inviteBtn = findViewById(R.id.InviteFriend);
        Button pendingBtn = findViewById(R.id.ShowInvites);
        Button tournamentsBtn = findViewById(R.id.openTournamentsBtn);
        Button accountBtn = findViewById(R.id.Account);

        // 1) Enable or disable the three feature buttons
        inviteBtn.setEnabled(loggedIn);
        pendingBtn.setEnabled(loggedIn);
        tournamentsBtn.setEnabled(loggedIn);

        // Optional: visual cue (dim when disabled)
        float alpha = loggedIn ? 1f : 0.4f;
        inviteBtn.setAlpha(alpha);
        pendingBtn.setAlpha(alpha);
        tournamentsBtn.setAlpha(alpha);

        // 2) Update the Account / Logout button text + handler (your Option A code)
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

        ((SoccerApp) getApplication())
                .forceUserOffline(uid)
                .addOnCompleteListener(task -> {
                    FirebaseAuth.getInstance().signOut();   // <-- now it’s safe
                    finishLogoutUi();
                });
    }

    private void finishLogoutUi() {
        SharedPreferences.Editor ed = getSharedPreferences(
                getPackageName() + "_preferences", MODE_PRIVATE).edit();
        ed.clear().apply();                          // clears token + other cache

        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        updateUiForAuthState();      // dim buttons, “Login” label, etc.
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        /* ① Inflate the view immediately so onResume() has valid widgets */
        setContentView(R.layout.activity_menu);

        runHousekeeping();          // ← always executed, even on cold resume

        /* ───────────── 1️⃣  Look for any ACTIVE match involving this user ───────────── */
        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences",
                MODE_PRIVATE);
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collectionGroup("matches")                        // ← tops + tournaments/*
                    .whereEqualTo("status", "active")
                    .where(Filter.or(                                // OR on player0 / player1
                            Filter.equalTo("player0", uid),
                            Filter.equalTo("player1", uid)))
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snaps -> {
                        if (!snaps.isEmpty()) {
                            DocumentSnapshot doc = snaps.getDocuments().get(0);
                            String matchPath = doc.getReference().getPath();
                            Log.d("TAG_Soccer", "🟢 Found active match at " + matchPath);
                            startActivity(new Intent(this, GameActivity.class)
                                    .putExtra("matchPath", matchPath)
                                    .putExtra("GameType", 3)
                                    .putExtra("localNickname",
                                            prefs.getString("nickname", "Player"))
                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                            | Intent.FLAG_ACTIVITY_NEW_TASK));
                            finish();
                            return;          // 🔚 we’re done
                        }
                        /* nothing active → fall through to invite/normal flow */
                        continueWithInviteRestore(prefs);
                    })
                    .addOnFailureListener(err -> {
                        Log.e("TAG_Soccer", "Failed to query active matches", err);
                        continueWithInviteRestore(prefs);
                    });
            return;   // invite-path continues asynchronously
        }
        /* no UID (not logged-in) → skip active-match lookup */
        continueWithInviteRestore(prefs);
    }

    /*  🔻  old waiting-invite code moved unchanged into a helper  */
    private void continueWithInviteRestore(SharedPreferences prefs) {

        /* ───────────── 2️⃣  Restore WaitingActivity (existing code) ───────── */

        Log.d("TAG_Soccer",
                getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() + ": checking for activeInviteId…");
        /* ────────────────────────────────────────────────────────────────
         *  ⚡ Restore WaitingActivity if the process was killed while the
         *    user was waiting for the opponent to accept an invite.
         * ─────────────────────────────────────────────────────────────── */
        String pendingId = prefs.getString("activeInviteId", null);
        if (pendingId != null) {

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("invitations").document(pendingId);

            docRef.get().addOnSuccessListener(doc -> {
                Log.d("TAG_Soccer",
                        getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName() + ": found marker id=" + pendingId);

                boolean stillPending =
                        doc.exists() &&
                                "pending".equals(doc.getString("status")) &&
                                doc.getTimestamp("expireAt") != null &&
                                Objects.requireNonNull(doc.getTimestamp("expireAt"))
                                        .toDate().getTime() > System.currentTimeMillis();

                if (stillPending) {
                    Log.d("TAG_Soccer",
                            getClass().getSimpleName() +
                            "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": ↩️  invite still pending – reopening WaitingActivity");

                    // invite is valid → resume WaitingActivity
                    Intent i = new Intent(this, WaitingActivity.class)
                            .putExtra("inviteId", pendingId)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                    finish();     // don’t show MenuActivity
                } else {          //  invitation is NOT pending anymore
                    Log.d("TAG_Soccer", "invite no longer pending – checking for active match");
                    /* ───── 1️⃣  Check the exact matchPath (tournaments) ───── */
                    String mp = doc.getString("matchPath");
                    if (mp != null && !mp.isEmpty()) {
                        db.document(mp).get().addOnSuccessListener(mSnap -> {
                            if (mSnap.exists() && "active".equals(mSnap.getString("status"))) {
                                Log.d("TAG_Soccer", "🟢 tournament match ACTIVE – launching GameActivity");
                                startActivity(new Intent(this, GameActivity.class)
                                        .putExtra("matchPath", mp)
                                        .putExtra("GameType", 3)
                                        .putExtra("localNickname",
                                                prefs.getString("nickname", "Player"))
                                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                                | Intent.FLAG_ACTIVITY_NEW_TASK));
                                finish();
                                return;                 // 🔚 done
                            }
                            // fall through → try the generic query below
                            continueWhenNoActiveMatch(doc, db, pendingId, prefs);
                        });
                    } else {
                        /* friendlies → look in the top-level matches collection */
                        continueWhenNoActiveMatch(doc, db, pendingId, prefs);
                    }
                }
            });
        }
        /* ─────────── END of “restore waiting” section ─────────── */
    }

    /* centralised dialog builder */
    private void showResumeDialog(String inviteId, String nick, SharedPreferences prefs) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.invite_still_pending_title)
                .setMessage(getString(R.string.invite_still_pending_msg, nick))
                .setCancelable(false)
                .setPositiveButton(R.string.resume, (d, w) -> {
                    startActivity(new Intent(this, WaitingActivity.class)
                            .putExtra("inviteId", inviteId)
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_NEW_TASK));
                    finish();
                })
                .setNegativeButton(R.string.cancel_invite, (d, w) ->
                        FirebaseFunctions.getInstance("us-central1")
                                .getHttpsCallable("cancelInvite")
                                .call(Collections.singletonMap("invitationId", inviteId))
                                .addOnCompleteListener(t ->
                                        prefs.edit().remove("activeInviteId").apply()))
                .show();
    }

    /* ───────── helper called when match *not* found as active ───────── */
    private void continueWhenNoActiveMatch(DocumentSnapshot doc,
                                           FirebaseFirestore db,
                                           String pendingId,
                                           SharedPreferences prefs) {
        db.collection("matches")
                .whereEqualTo("invitationId", pendingId)
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .addOnSuccessListener(activeSnaps -> {
                    /* 1a) YES → jump straight into the game */
                    if (!activeSnaps.isEmpty()) {
                        String nickname = prefs.getString("nickname", "Player");
                        String matchPath = activeSnaps.getDocuments()
                                .get(0)
                                .getReference()
                                .getPath();
                        Log.d("TAG_Soccer",
                                getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() + ": 🟢 match ACTIVE – launching GameActivity");
                        startActivity(new Intent(this, GameActivity.class)
                                .putExtra("matchPath", matchPath)
                                .putExtra("GameType", 3)          // remote-vs-remote
                                .putExtra("localNickname", nickname) //  👈  **added**
                                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_NEW_TASK));

                        /* marker no longer needed once we’re in the match */
                        prefs.edit().remove("activeInviteId").apply();
                        finish();
                        return;
                    }
                    /* 1b) NO → marker is stale, forget it  */
                    Log.d("TAG_Soccer",
                            getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": invite handled but no active match – removing marker");
                    prefs.edit().remove("activeInviteId").apply();
                    /* ── 2️⃣ Ask the user what to do (async) ── */
                    db.collection("users")
                            .document(Objects.requireNonNull(doc.getString("to")))
                            .get()
                            .addOnSuccessListener(userSnap -> {
                                String nick = userSnap.getString("nickname");
                                if (nick == null || nick.isEmpty())
                                    nick = "opponent";
                                showResumeDialog(pendingId, nick, prefs);
                            })
                            .addOnFailureListener(err ->
                                    showResumeDialog(pendingId, "opponent", prefs));

                });
    }              // ← end of “else” branch

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

}
