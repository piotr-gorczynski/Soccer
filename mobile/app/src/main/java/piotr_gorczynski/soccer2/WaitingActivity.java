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

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;   // already used elsewhere

public class WaitingActivity extends BaseActivity {
    private boolean gameActivityLaunched = false;
    private ListenerRegistration matchListener;   // ⇐ capture this
    private ListenerRegistration inviteListener;
    private CountDownTimer ttlTimer;

    private String inviteId;                 // keep ID for back-press cancel

    private OnBackPressedCallback backCallback;   // keep a reference

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_waiting_for_opponent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
        }.getClass().getEnclosingMethod()).getName() + ": Started");
        inviteId = getIntent().getStringExtra("inviteId");
        if (inviteId == null || inviteId.isEmpty()) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
            }.getClass().getEnclosingMethod()).getName()
                    + ": Missing inviteId");
            finish();
            return;
        }

        TextView waitingMessage = findViewById(R.id.waitingMessage);
        TextView countdownTv = findViewById(R.id.countdown);
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
                if (msLeft <= 0) {
                    finish();
                    return;
                }

                // 🟢 show the fresh TTL immediately (05:00)
                countdownTv.setText(String.format(
                        Locale.US, "%02d:%02d", msLeft / 60000, (msLeft / 1000) % 60));

                ttlTimer = new CountDownTimer(msLeft, 1000) {
                    public void onTick(long millisUntilFinished) {
                        long min = millisUntilFinished / 60000;
                        long sec = (millisUntilFinished / 1000) % 60;
                        countdownTv.setText(
                                String.format(Locale.US, "%02d:%02d", min, sec));   // US = always 0-9 digits
                        //                            countdownTv.setText(String.format("%02d:%02d", min, sec));
                    }

                    public void onFinish() {
                        Log.d("TAG_Soccer", "CountDownTimer.onFinish: Calling expireInvite Cloud Function");

                        FirebaseFunctions.getInstance("us-central1")
                                .getHttpsCallable("expireInvite")
                                .call(Collections.singletonMap("invitationId", inviteId))
                                .addOnSuccessListener(r -> {
                                    Log.d("TAG_Soccer", "expireInvite: Success");
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TAG_Soccer", "expireInvite: FAILED", e);
                                    finish();  // fallback
                                });
                    }
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
        /* ── back-press guard (works for gesture + button) ─────────── */
        backCallback = new OnBackPressedCallback(true /* enabled */) {
            @Override
            public void handleOnBackPressed() {

                if (gameActivityLaunched) {               // already leaving for GameActivity
                    setEnabled(false);                    // hand control back to system
                    getOnBackPressedDispatcher().onBackPressed();
                    return;
                }

                new AlertDialog.Builder(WaitingActivity.this)
                        .setTitle(R.string.cancel_invite)            // “Cancel invite?”
                        .setMessage(R.string.leave_waiting_confirm)  // “Leaving will cancel…”
                        .setPositiveButton(R.string.yes,(d, w) -> {
                            backCallback.setEnabled(false);
                            cancelInvite(inviteId);
                        })     // call your helper
                        .setNegativeButton(R.string.no, null)
                        .show();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);
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

        SharedPreferences prefs =
                getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        String nickname = prefs.getString("nickname", getString(R.string.player_label));

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
        Log.d("TAG_Soccer",
                getClass().getSimpleName() + ".onDestroy: ❎ clearing activeInviteId");
        /* remove the marker so we don’t restore an invite that’s finished */
        getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE)
                .edit()
                .remove("activeInviteId")
                .apply();

        if (backCallback != null)
            backCallback.remove();

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
                            getString(R.string.could_not_cancel_invite, e.getMessage()),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
    }
}
