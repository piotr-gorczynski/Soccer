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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class TournamentsActivity extends AppCompatActivity {


    private ArrayAdapter<String> adapter;

    private final ArrayList<String> tournamentDescriptions = new ArrayList<>();
    private final ArrayList<String> tournamentIds          = new ArrayList<>();

    private FirebaseFirestore db;

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
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Tournament listener failed", e);
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

