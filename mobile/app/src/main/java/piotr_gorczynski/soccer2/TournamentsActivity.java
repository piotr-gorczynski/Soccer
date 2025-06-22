package piotr_gorczynski.soccer2;

import android.annotation.SuppressLint;
import android.content.Intent;
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

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournaments);

        // ── RecyclerView ─────────────────────────────────────────────
        RecyclerView registeringList = findViewById(R.id.registeringList);
        RecyclerView runningList = findViewById(R.id.runningList);
        RecyclerView endedList = findViewById(R.id.endedList);

        registeringList.setLayoutManager(new LinearLayoutManager(this));
        runningList.setLayoutManager(new LinearLayoutManager(this));
        endedList.setLayoutManager(new LinearLayoutManager(this));

        List<DocumentSnapshot> registeringDocs = new ArrayList<>();
        List<DocumentSnapshot> runningDocs = new ArrayList<>();
        List<DocumentSnapshot> endedDocs = new ArrayList<>();

        TournamentAdapter registeringAdapter = new TournamentAdapter(registeringDocs, this::joinTournament);
        TournamentAdapter runningAdapter = new TournamentAdapter(runningDocs, this::joinTournament);
        TournamentAdapter endedAdapter = new TournamentAdapter(
                endedDocs,
                (TournamentAdapter.OnEndedClick) doc -> {
                    Intent i = new Intent(this, TournamentResultsActivity.class)
                            .putExtra("tournamentId", doc.getId());
                    startActivity(i);
                }
        );
        endedList.setAdapter(endedAdapter);

        registeringList.setAdapter(registeringAdapter);
        runningList.setAdapter(runningAdapter);
        endedList.setAdapter(endedAdapter);

        // ── Firestore ────────────────────────────────────────────────
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tournaments").addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;

            // Clear and repopulate each list (simpler than tracking diffs for 3 views)
            registeringDocs.clear();
            runningDocs.clear();
            endedDocs.clear();

            for (DocumentSnapshot doc : snap.getDocuments()) {
                String status = doc.getString("status");
                if ("registering".equals(status)) registeringDocs.add(doc);
                else if ("running".equals(status)) runningDocs.add(doc);
                else if ("ended".equals(status)) endedDocs.add(doc);
            }

            registeringAdapter.notifyDataSetChanged();
            runningAdapter.notifyDataSetChanged();
            endedAdapter.notifyDataSetChanged();
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

