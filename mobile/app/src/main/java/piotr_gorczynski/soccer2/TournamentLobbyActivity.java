package piotr_gorczynski.soccer2;

import static java.util.Objects.*;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

public class TournamentLobbyActivity extends AppCompatActivity {

    private MatchAdapter mAdapter;
    private ListenerRegistration matchListenerA, matchListenerB;
    private TextView tournamentName;

    /* one common handler so we don’t repeat the diff logic */
    EventListener<QuerySnapshot> makeHandler(String label) {
        return (snap, err) -> {
            if (err != null) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": "+ label + " listener FAILED", err);
                return;                           // <-- bail early on error
            }

            if (snap == null) {
                Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": " + label + " listener returned a NULL snapshot");
                return;
            }

            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": " + String.format(
                    "%s → got snapshot: size=%d (fromCache=%b, pending=%b)",
                    label, snap.size(),
                    snap.getMetadata().isFromCache(),
                    snap.getMetadata().hasPendingWrites()));

            /* dump every doc once, before we feed the adapter */
            if (BuildConfig.DEBUG) {
                for (DocumentSnapshot d : snap.getDocuments()) {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": " + String.format(
                            "  • %s  p0=%s  p1=%s  status=%s",
                            d.getId(),
                            d.getString("player0"),
                            d.getString("player1"),
                            d.getString("status")));
                }
            }

            /* === existing diff logic follows unchanged === */
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
    }

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": Method start");
        setContentView(R.layout.activity_tournament_lobby);

        tournamentName = findViewById(R.id.tournamentName);

        String tid   = getIntent().getStringExtra("tournamentId");
        String nameExtra = getIntent().getStringExtra("tournamentName");
        String myUid = requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (nameExtra != null) {
            tournamentName.setText(nameExtra);
        } else if (tid != null) {
            db.collection("tournaments").document(tid).get()
                    .addOnSuccessListener(doc -> {
                        String n = doc.getString("name");
                        if (n != null) tournamentName.setText(n);
                    });
        }

        RecyclerView rv = findViewById(R.id.matchesList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new MatchAdapter(
                this,                      // 👈 Context
                myUid,
                requireNonNull(tid)
        );
        rv.setAdapter(mAdapter);

        /* one common handler so we don’t repeat the diff logic */
/*        EventListener<QuerySnapshot> matchHandler = (snap, e) -> {
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
        };*/

        /* listen where player0 == myUid */
        ListenerRegistration l0 = db.collection("tournaments").document(requireNonNull(tid))
                .collection("matches")
                .whereEqualTo("player0", myUid)
                .addSnapshotListener(makeHandler("player0"));

        /* listen where player1 == myUid */
        ListenerRegistration l1 = db.collection("tournaments").document(tid)
                .collection("matches")
                .whereEqualTo("player1", myUid)
                .addSnapshotListener(makeHandler("player1"));

        /* keep references so you can remove them in onDestroy() */
        matchListenerA = l0;
        matchListenerB = l1;

        /* ───── One-shot sanity read: how many docs are in the sub-collection? ───── */
        db.collection("tournaments").document(tid)
                .collection("matches")
                .get()
                .addOnSuccessListener(q ->
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": Direct read: matches sub-col size = " + q.size()))
                .addOnFailureListener(e ->
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": Direct read FAILED", e));

    }


    @Override protected void onDestroy() {
        if (matchListenerA != null) matchListenerA.remove();
        if (matchListenerB != null) matchListenerB.remove();
        super.onDestroy();
    }
}

