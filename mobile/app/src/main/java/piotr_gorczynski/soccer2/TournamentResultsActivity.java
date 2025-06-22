package piotr_gorczynski.soccer2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TournamentResultsActivity extends AppCompatActivity {

    private TextView tournamentName;
    private StandingsAdapter adapter;
    private final List<StandingEntry> standings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_results);

        tournamentName = findViewById(R.id.tournamentName);
        RecyclerView standingsList = findViewById(R.id.standingsList);
        standingsList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StandingsAdapter(standings);
        standingsList.setAdapter(adapter);

        String tid = getIntent().getStringExtra("tournamentId");
        if (tid != null) {
            loadTournament(tid);
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private void loadTournament(String tid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("tournaments").document(tid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    tournamentName.setText(doc.getString("name"));
                });

        // Step 1: fetch all matches
        db.collection("tournaments").document(tid)
                .collection("matches").get()
                .addOnSuccessListener(matchSnap -> {
                    Map<String, StandingEntry> scoreMap = new java.util.HashMap<>();

                    for (DocumentSnapshot match : matchSnap) {
                        String winner = match.getString("winner");
                        if (winner == null || winner.isEmpty()) continue;

                        if (!scoreMap.containsKey(winner)) {
                            scoreMap.put(winner, new StandingEntry(winner));
                        }
                        Objects.requireNonNull(scoreMap.get(winner)).wins += 1;
                    }

                    // Step 2: fetch nicknames
                    List<String> uids = new ArrayList<>(scoreMap.keySet());
                    if (uids.isEmpty()) {
                        tournamentName.setText(R.string.no_match_results_found);
                        return;
                    }

                    db.collection("users").whereIn(FieldPath.documentId(), uids)
                            .get().addOnSuccessListener(userSnap -> {
                                for (DocumentSnapshot user : userSnap) {
                                    StandingEntry e = scoreMap.get(user.getId());
                                    if (e != null) {
                                        e.nickname = user.getString("nickname");
                                    }
                                }

                                standings.clear();
                                standings.addAll(scoreMap.values());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    standings.sort(Comparator.comparingInt((StandingEntry e) -> e.wins).reversed());
                                }
                                adapter.notifyDataSetChanged();
                            });
                });
    }

    /** Internal structure for one row */
    static class StandingEntry {
        final String uid;
        String nickname;
        int wins = 0;

        StandingEntry(String uid) {
            this.uid = uid;
        }
    }


    /** RecyclerView adapter */
    static class StandingsAdapter extends RecyclerView.Adapter<StandingsAdapter.VH> {

        static class VH extends RecyclerView.ViewHolder {
            final TextView rank, player, points;

            VH(android.view.View itemView) {
                super(itemView);
                rank = itemView.findViewById(R.id.rank);
                player = itemView.findViewById(R.id.player);
                points = itemView.findViewById(R.id.points);
            }
        }

        private final List<StandingEntry> data;

        StandingsAdapter(List<StandingEntry> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_standing, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            StandingEntry entry = data.get(position);
            holder.rank.setText(String.valueOf(position + 1));
            holder.player.setText(entry.nickname != null ? entry.nickname : entry.uid);
            Context context = holder.itemView.getContext();
            String formatted = context.getResources()
                    .getQuantityString(R.plurals.wins_format, entry.wins, entry.wins);
            holder.points.setText(formatted);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
