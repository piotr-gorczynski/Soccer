package piotr_gorczynski.soccer2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.List;

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

        TournamentAdapter registeringAdapter = new TournamentAdapter(
                registeringDocs,
                this::joinTournament,
                this::leaveTournament
        );
        TournamentAdapter runningAdapter = new TournamentAdapter(
                runningDocs,
                this::joinTournament,
                this::leaveTournament
        );
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

        // Hide sections if empty
        findViewById(R.id.registeringList).setVisibility(
                registeringAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.runningList).setVisibility(
                runningAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.endedList).setVisibility(
                endedAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);

        // Also hide the headers
        findViewById(R.id.registeringHeader).setVisibility(
                registeringAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.runningHeader).setVisibility(
                runningAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
        findViewById(R.id.endedHeader).setVisibility(
                endedAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);


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

            // Hide sections if empty
            findViewById(R.id.registeringList).setVisibility(
                    registeringAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.runningList).setVisibility(
                    runningAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.endedList).setVisibility(
                    endedAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);

            // Also hide the headers
            findViewById(R.id.registeringHeader).setVisibility(
                    registeringAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.runningHeader).setVisibility(
                    runningAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.endedHeader).setVisibility(
                    endedAdapter.getItemCount() == 0 ? View.GONE : View.VISIBLE);


        });
    }

    /** Calls your Cloud Function `joinTournament` (see blueprint) */
    private void joinTournament(DocumentSnapshot tournamentDoc) {
        if (tournamentDoc == null || !tournamentDoc.exists()) return;

        String tid = tournamentDoc.getId();
        String regId = tournamentDoc.getString("regulation");

        Intent i = new Intent(this, RegulationActivity.class)
                .putExtra("tournamentId", tid)
                .putExtra("regulationId", regId);
        startActivity(i);
    }

    /** Prompt confirmation and call leaveTournament Cloud Function */
    private void leaveTournament(DocumentSnapshot tournamentDoc) {
        if (tournamentDoc == null || !tournamentDoc.exists()) return;

        String tid = tournamentDoc.getId();

        new AlertDialog.Builder(this)
                .setMessage(R.string.leave_tournament_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> doLeave(tid))
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void doLeave(String tid) {
        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("leaveTournament")
                .call(java.util.Collections.singletonMap("tournamentId", tid))
                .addOnSuccessListener(r ->
                        Toast.makeText(this, R.string.left_tournament, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseFunctionsException ffe) {
                        Toast.makeText(this, ffe.getMessage(), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}

