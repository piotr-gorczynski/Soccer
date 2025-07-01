package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class WaitingActivity extends AppCompatActivity {
    private boolean gameActivityLaunched = false;
    private ListenerRegistration matchListener;   // ⇐ capture this
    private ListenerRegistration inviteListener;
    private CountDownTimer ttlTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_waiting_for_opponent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName() + ": Started");
        String inviteId = getIntent().getStringExtra("inviteId");
        if (inviteId == null || inviteId.isEmpty()) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName()
                    + ": Missing inviteId");
            finish();
            return;
        }

        TextView waitingMessage = findViewById(R.id.waitingMessage);
        TextView countdownTv    = findViewById(R.id.countdown);
        findViewById(R.id.cancelInviteBtn).setOnClickListener(v -> cancelInvite(inviteId));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference inviteRef = db.collection("invitations").document(inviteId);
        inviteRef.get().addOnSuccessListener(inviteDoc -> {

                    /* 1️⃣  Put the placeholder on screen immediately */
                    waitingMessage.setText(getString(R.string.waiting_for_opponent));

                    /* 2️⃣  Fetch the receiver’s nickname from /users/{to} */
                    String toUid = inviteDoc.getString("to");
                    if (!TextUtils.isEmpty(toUid)) {
                        db.collection("users").document(toUid).get()
                                .addOnSuccessListener(userDoc -> {
                                    String nick = userDoc.getString("nickname");
                                    if (!TextUtils.isEmpty(nick)) {
                                        waitingMessage.setText(
                                                getString(R.string.waiting_for_opponent_named, nick));
                                    }
                                });
                    }
                    /* 3️⃣  Start a live listener on *this* invitation
                    so we detect status = expired/cancelled ↓ */
                    inviteListener = inviteRef.addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;
                    String status = snap.getString("status");
                    if ("expired".equals(status) && !isFinishing()) {
                        Toast.makeText(this,
                                R.string.invite_expired, Toast.LENGTH_LONG).show();
                        finish();
                    }
                });

                /* 4️⃣  Start TTL countdown ------------------------- */
                Timestamp expireAt = inviteDoc.getTimestamp("expireAt");
                if (expireAt != null) {
                    long msLeft = expireAt.toDate().getTime() - System.currentTimeMillis();
                    if (msLeft <= 0) {          // already expired
                        finish();
                        return;
                    }
                    ttlTimer = new CountDownTimer(msLeft, 1000) {
                        public void onTick(long millisUntilFinished) {
                            long min = millisUntilFinished / 60000;
                            long sec = (millisUntilFinished / 1000) % 60;
                            countdownTv.setText(
                                    String.format(Locale.US, "%02d:%02d", min, sec));   // US = always 0-9 digits
                            //                            countdownTv.setText(String.format("%02d:%02d", min, sec));
                        }
                        public void onFinish() { finish(); }
                    }.start();
                }
                    /* 3️⃣  Decide which match-listener to start (unchanged) */
                    String matchPath = inviteDoc.getString("matchPath");
                    if (!TextUtils.isEmpty(matchPath)) {
                        listenForExistingMatch(db.document(matchPath));   // tournament
                    } else {
                        listenForNewMatch(db, inviteId);                  // friendly
                    }
                });
    }

    /* --- helper for tournament matches ----------------------------------- */
    private void listenForExistingMatch(DocumentReference matchRef) {
        matchListener = matchRef.addSnapshotListener((snap, err) -> {
            if (err != null || snap == null || !snap.exists()) return;

            String status = snap.getString("status");
            if ("active".equals(status) && !gameActivityLaunched) {
                launchGame(snap.getReference().getPath());   // full path
            }
        });
    }

    /* --- common launcher -------------------------------------------------- */
    private void launchGame(String matchPath) {
        gameActivityLaunched = true;

        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        String nickname = prefs.getString("nickname", "Player");

        startActivity(new Intent(this, GameActivity.class)
                .putExtra("matchPath",   matchPath)   // 👈 unified key
                .putExtra("GameType",    3)
                .putExtra("localNickname", nickname));

        finish();
    }

    /* --- helper for friend-for-fun matches ------------------------------- */
    private void listenForNewMatch(FirebaseFirestore db, String inviteId) {
        matchListener = db.collection("matches")
                .whereEqualTo("invitationId", inviteId)
                .addSnapshotListener((snaps, err) -> {
                    if (err != null || snaps == null || snaps.isEmpty()) return;
                    if (!gameActivityLaunched) {
                        launchGame(snaps.getDocuments().get(0)
                                .getReference().getPath());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        // just in case we never hit the first match…
        if (matchListener != null) {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
            }.getClass().getEnclosingMethod()).getName() + ": removing matchListener");
            matchListener.remove();
            matchListener = null;
        }
        if (ttlTimer!= null) {
            ttlTimer.cancel();
        }
        if (inviteListener != null) {
            inviteListener.remove();
        }

        super.onDestroy();
    }

    private void cancelInvite(@NonNull String inviteId) {

        findViewById(R.id.cancelInviteBtn).setEnabled(false);   // debounce

        Map<String,Object> data = Collections.singletonMap("invitationId", inviteId);

        findViewById(R.id.cancelInviteBtn).setEnabled(false);   // debounce

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("cancelInvite")
                .call(Collections.singletonMap("invitationId", inviteId))

                /* success or failure — finish either way */
                .addOnSuccessListener(result -> finish())

                /* handle network / CF errors */
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName()
                            + ": cancelInvite failed", e);
                    Toast.makeText(this,
                            "Could not cancel invite: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}
