package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Collections;
import java.util.Objects;

public class TournamentLobbyActivity extends AppCompatActivity {

    private ListenerRegistration tourListener, matchListener;
    private boolean gameStarted = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_tournament_lobby);

        String tid = getIntent().getStringExtra("tournamentId");
        String uid = Objects.requireNonNull(
                FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        /* ① tournament doc → counter + status label */
        tourListener = db.collection("tournaments").document(Objects.requireNonNull(tid))
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;

                    long joined = Objects.requireNonNull(snap.getLong("participantsCount"));
                    long max    = Objects.requireNonNull(snap.getLong("maxParticipants"));
                    TextView counter = findViewById(R.id.counter);
                    String slots = getString(R.string.lobby_slots_format, joined, max);
                    counter.setText(slots);

                    String status = snap.getString("status");
                    if ("running".equals(status)) {
                        findViewById(R.id.waitingLabel).setVisibility(View.GONE);
                    }
                });

        /* ② matches sub-collection → start GameActivity */
        matchListener = db.collection("tournaments").document(tid)
                .collection("matches")
                .whereArrayContainsAny("players", Collections.singletonList(uid))
                .addSnapshotListener((snap, e) -> {
                    if (gameStarted || snap == null || snap.isEmpty()) return;

                    DocumentSnapshot m = snap.getDocuments().get(0);
                    gameStarted = true;

                    Intent g = new Intent(this, GameActivity.class);
                    g.putExtra("matchId", m.getId());
                    g.putExtra("GameType", 4);   // “tournament”
                    startActivity(g);
                    finish();
                });
    }

    @Override protected void onDestroy() {
        if (tourListener  != null) tourListener.remove();
        if (matchListener != null) matchListener.remove();
        super.onDestroy();
    }
}

