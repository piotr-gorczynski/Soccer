package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Objects;

import android.widget.TextView;

public class TournamentLobbyActivity extends AppCompatActivity {

    private MatchAdapter mAdapter;
    private ListenerRegistration tourListener, matchListener;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_tournament_lobby);

        String tid   = getIntent().getStringExtra("tournamentId");
        String myUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        /* matches RecyclerView */
        RecyclerView rv = findViewById(R.id.matchesList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MatchAdapter(myUid,
                matchId -> startActivity(
                        new Intent(this, GameActivity.class)
                                .putExtra("matchId", matchId)
                                .putExtra("GameType", 4)));
        rv.setAdapter(mAdapter);

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

        /* NEW: live listener for *my* matches */
        matchListener = db.collection("tournaments").document(tid)
                .collection("matches")
                // Option 1: array field ["playerA","playerB"] -> array-contains
                //.whereArrayContains("players", myUid)
                // Option 2: two separate equality clauses
                .whereIn("playerA", List.of(myUid))
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        switch (dc.getType()) {
                            case ADDED    ->
                                    mAdapter.add(dc.getNewIndex(), dc.getDocument());
                            case MODIFIED -> {
                                int o = dc.getOldIndex(), n = dc.getNewIndex();
                                if (o == n) mAdapter.set(o, dc.getDocument());
                                else         mAdapter.move(o, n, dc.getDocument());
                            }
                            case REMOVED  -> mAdapter.remove(dc.getOldIndex());
                        }
                    }
                    findViewById(R.id.matchesList)
                            .setVisibility(View.VISIBLE);
                });
    }

    @Override protected void onDestroy() {
        if (tourListener  != null) tourListener.remove();
        if (matchListener != null) matchListener.remove();
        super.onDestroy();
    }
}

