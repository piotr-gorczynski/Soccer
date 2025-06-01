package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Objects;

public class TournamentsActivity extends AppCompatActivity {


    private ArrayAdapter<String> adapter;

    private final ArrayList<String> tournamentDescriptions = new ArrayList<>();
    private final ArrayList<String> tournamentIds          = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth      auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournaments);

        ListView tournamentsList = findViewById(R.id.tournamentsList);
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                tournamentDescriptions);
        tournamentsList.setAdapter(adapter);

        db   = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        listenForTournaments();

        tournamentsList.setOnItemClickListener((parent, view, position, id) -> {
            String tid   = tournamentIds.get(position);
            String title = tournamentDescriptions.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("Join tournament")
                    .setMessage("Do you want to join “" + title + "”?")
                    .setPositiveButton("Join", (dialog, which) -> joinTournament(tid))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    /** Reads all tournaments whose status == "registering" */
    private void listenForTournaments() {
        db.collection("tournaments")
                .whereEqualTo("status", "registering")   // show only open sign-ups
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("TAG_Soccer", "Tournament listener failed", e);
                        return;
                    }
                    tournamentDescriptions.clear();
                    tournamentIds.clear();

                    for (DocumentSnapshot doc : Objects.requireNonNull(snap)) {
                        String name       = doc.getString("name");
                        Long   max        = doc.getLong("maxParticipants");
                        Long   joined     = doc.getLong("participantsCount");
                        String desc = name + "  (" + joined + " / " + max + ")";
                        tournamentDescriptions.add(desc);
                        tournamentIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    /** Calls your Cloud Function `joinTournament` (see blueprint) */
    private void joinTournament(String tournamentId) {
        if (TextUtils.isEmpty(tournamentId)) {
            Toast.makeText(this, "Tournament not found.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        // callable <joinTournament> lives in index.js
        FirebaseFirestore.getInstance()
                .collection("tournaments")          // naïve optimistic write
                .document(tournamentId)
                .collection("participants")
                .document(Objects.requireNonNull(auth.getCurrentUser()).getUid())
                .set(new Object())  // empty doc – CF will validate & overwrite
                .addOnSuccessListener(unused -> Toast
                        .makeText(this, "Joined! Wait for the bracket to start.",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", "Join failed", e);
                    Toast.makeText(this, e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}

