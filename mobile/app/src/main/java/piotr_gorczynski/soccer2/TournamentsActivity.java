package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TournamentsActivity extends AppCompatActivity {


    private FirebaseFirestore db;

    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private TournamentAdapter adapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournaments);

        setContentView(R.layout.activity_tournaments);   // already there

        RecyclerView rv = findViewById(R.id.tournamentsList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new TournamentAdapter(docs, this::joinTournament);
        rv.setAdapter(adapter);


        db   = FirebaseFirestore.getInstance();

        listenForTournaments();
    }

    /** Reads all tournaments whose status == "registering" */
    private void listenForTournaments() {
        db.collection("tournaments")
                .whereEqualTo("status", "registering")
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;

                    for (DocumentChange dc : snap.getDocumentChanges()) {
                        switch (dc.getType()) {

                            case ADDED: {
                                docs.add(dc.getNewIndex(), dc.getDocument());
                                adapter.notifyItemInserted(dc.getNewIndex());
                                break;
                            }

                            case MODIFIED: {
                                // Was it just edited, or moved as well?
                                int oldIdx = dc.getOldIndex();
                                int newIdx = dc.getNewIndex();

                                if (oldIdx == newIdx) {
                                    docs.set(oldIdx, dc.getDocument());
                                    adapter.notifyItemChanged(oldIdx);
                                } else {               // moved in the result set
                                    docs.remove(oldIdx);
                                    docs.add(newIdx, dc.getDocument());
                                    adapter.notifyItemMoved(oldIdx, newIdx);
                                }
                                break;
                            }

                            case REMOVED: {
                                int oldIdx = dc.getOldIndex();
                                docs.remove(oldIdx);
                                adapter.notifyItemRemoved(oldIdx);
                                break;
                            }
                        }
                    }
                });
    }

    /** Calls your Cloud Function `joinTournament` (see blueprint) */
    private void joinTournament(String tournamentId) {

        if (TextUtils.isEmpty(tournamentId)) {
            Toast.makeText(this, "Tournament not found.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged-in.", Toast.LENGTH_LONG).show();
            return;
        }

        // 🔄 force-refresh the ID-token so App Check / IAM always passes
        user.getIdToken(true).addOnSuccessListener(tokenRes -> {

            FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");
            Map<String,Object> data     = Collections.singletonMap("tournamentId", tournamentId);

            functions
                    .getHttpsCallable("joinTournament")
                    .call(data)

                    /* ───── success ───── */
                    .addOnSuccessListener(r -> {Toast
                            .makeText(this, "Joined! Wait for the bracket to start.",
                                    Toast.LENGTH_SHORT).show();
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Joined");

                    })

                    /* ───── failure ───── */
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseFunctionsException ffe) {
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": code=" + ffe.getCode()
                                    + "  msg=" + ffe.getMessage()
                                    + "  details=" + ffe.getDetails());
                        }
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}

