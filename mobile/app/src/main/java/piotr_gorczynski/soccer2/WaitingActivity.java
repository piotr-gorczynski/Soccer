package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;

import java.util.Objects;

public class WaitingActivity extends AppCompatActivity {
    private boolean gameActivityLaunched = false;
    private ListenerRegistration matchListener;   // ⇐ capture this

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_waiting_for_opponent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName() + ": Started");
        String inviteId = getIntent().getStringExtra("inviteId");
        if (inviteId == null || inviteId.isEmpty()) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName() + ": Missing inviteId");
            finish();
            return;
        }

        TextView waitingMessage = findViewById(R.id.waitingMessage);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("invitations").document(inviteId).get()
                .addOnSuccessListener(inviteDoc -> {
                    String toNickname = inviteDoc.getString("toNickname");
                    if (toNickname != null) {

                        String msg = getString(R.string.waiting_for_opponent_named, toNickname);
                        waitingMessage.setText(msg);
                    }

                    /* ⬇️ NEW: choose the right waiting strategy */
                    String matchPath = inviteDoc.getString("matchPath");

                    if (!TextUtils.isEmpty(matchPath)) {                  // ── tournament flow
                        listenForExistingMatch(db.document(matchPath));
                    } else {                                              // ── friendly flow
                        listenForNewMatch(db, inviteId);
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
        super.onDestroy();
    }
}
