package piotr_gorczynski.soccer2;

import static java.util.Objects.*;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

public class TournamentLobbyActivity extends AppCompatActivity {

    private MatchAdapter mAdapter;
    private ListenerRegistration matchListenerA, matchListenerB;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_tournament_lobby);

        String tid   = getIntent().getStringExtra("tournamentId");
        String myUid = requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        RecyclerView rv = findViewById(R.id.matchesList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MatchAdapter(
                this,                      // 👈 Context
                myUid,
                matchId -> startActivity(
                        new Intent(this, GameActivity.class)
                                .putExtra("matchId", matchId)
                                .putExtra("GameType", 4)
                )
        );
        rv.setAdapter(mAdapter);

        /* one common handler so we don’t repeat the diff logic */
        EventListener<QuerySnapshot> matchHandler = (snap, e) -> {
            if (e != null || snap == null) return;

            for (DocumentChange dc : snap.getDocumentChanges()) {
                switch (dc.getType()) {
                    case ADDED    -> mAdapter.add   (dc.getNewIndex(), dc.getDocument());
                    case MODIFIED -> {
                        int o = dc.getOldIndex(), n = dc.getNewIndex();
                        if (o == n)  mAdapter.set (o, dc.getDocument());
                        else         mAdapter.move(o, n, dc.getDocument());
                    }
                    case REMOVED  -> mAdapter.remove(dc.getOldIndex());
                }
            }
        };

        /* listen where playerA == myUid */
        ListenerRegistration lA = db.collection("tournaments").document(requireNonNull(tid))
                .collection("matches")
                .whereEqualTo("playerA", myUid)
                .addSnapshotListener(matchHandler);

        /* listen where playerB == myUid */
        ListenerRegistration lB = db.collection("tournaments").document(tid)
                .collection("matches")
                .whereEqualTo("playerB", myUid)
                .addSnapshotListener(matchHandler);

        /* keep references so you can remove them in onDestroy() */
        matchListenerA = lA;
        matchListenerB = lB;

    }


    @Override protected void onDestroy() {
        if (matchListenerA != null) matchListenerA.remove();
        if (matchListenerB != null) matchListenerB.remove();
        super.onDestroy();
    }
}

