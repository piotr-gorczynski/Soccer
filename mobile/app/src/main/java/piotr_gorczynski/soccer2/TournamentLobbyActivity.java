package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;


import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Objects;


public class TournamentLobbyActivity extends AppCompatActivity {

    private MatchAdapter mAdapter;
    //private PlayerAdapter pAdapter;
    private ListenerRegistration matchListener;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_tournament_lobby);

        String tid   = getIntent().getStringExtra("tournamentId");
        String myUid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        RecyclerView rv = findViewById(R.id.matchesList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MatchAdapter(myUid,
                matchId -> startActivity(
                        new Intent(this, GameActivity.class)
                                .putExtra("matchId", matchId)
                                .putExtra("GameType", 4)));
        rv.setAdapter(mAdapter);

        matchListener = db.collection("tournaments").document(Objects.requireNonNull(tid))
                .collection("matches")
                .whereArrayContains("players", myUid)
                .addSnapshotListener((snap,e)->{
                    if (e!=null || snap==null) return;
                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        switch (dc.getType()) {
                            case ADDED    -> mAdapter.add(dc.getNewIndex(), dc.getDocument());
                            case MODIFIED -> {
                                int o=dc.getOldIndex(), n=dc.getNewIndex();
                                if (o==n) mAdapter.set(o, dc.getDocument());
                                else      mAdapter.move(o,n,dc.getDocument());
                            }
                            case REMOVED  -> mAdapter.remove(dc.getOldIndex());
                        }
                    }
                });
    }


    @Override protected void onDestroy() {
        if (matchListener != null) matchListener.remove();
        super.onDestroy();
    }
}

