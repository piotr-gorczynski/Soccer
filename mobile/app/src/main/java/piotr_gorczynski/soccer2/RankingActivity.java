package piotr_gorczynski.soccer2;

import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RankingActivity extends AppCompatActivity {

    private final List<RankingEntry> ranking = new ArrayList<>();
    private RankingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        RecyclerView list = findViewById(R.id.rankingList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RankingAdapter(ranking);
        list.setAdapter(adapter);

        loadRanking();
    }

    private void loadRanking() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collectionGroup("matches").get().addOnSuccessListener(snap -> {
            Map<String, RankingEntry> scoreMap = new HashMap<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                // Skip "friendly" matches stored at the top level
                String path = doc.getReference().getPath();
                if (!path.startsWith("tournaments/")) continue;
                String winner = doc.getString("winner");
                if (winner == null || winner.isEmpty()) continue;
                RankingEntry e = scoreMap.get(winner);
                if (e == null) {
                    e = new RankingEntry(winner);
                    scoreMap.put(winner, e);
                }
                e.wins += 1;
            }

            List<String> uids = new ArrayList<>(scoreMap.keySet());
            if (uids.isEmpty()) {
                ranking.clear();
                adapter.notifyDataSetChanged();
                return;
            }

            db.collection("users").whereIn(FieldPath.documentId(), uids)
                    .get().addOnSuccessListener(userSnap -> {
                        for (DocumentSnapshot user : userSnap) {
                            RankingEntry e = scoreMap.get(user.getId());
                            if (e != null) {
                                e.nickname = user.getString("nickname");
                            }
                        }

                        ranking.clear();
                        ranking.addAll(scoreMap.values());

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            ranking.sort(Comparator.comparingInt((RankingEntry e) -> e.wins).reversed());
                        }
                        assignMedals(ranking);
                        adapter.notifyDataSetChanged();
                    });
        });
    }

    private void assignMedals(List<RankingEntry> list) {
        int category = 1; // 1 gold, 2 silver, 3 bronze
        int prevWins = -1;
        for (RankingEntry e : list) {
            if (prevWins != -1 && e.wins != prevWins) {
                category += 1;
            }
            e.medalCategory = category <= 3 ? category : 0;
            prevWins = e.wins;
        }
    }

    static class RankingEntry {
        final String uid;
        String nickname;
        int wins = 0;
        int medalCategory = 0; // 0=none,1=gold,2=silver,3=bronze

        RankingEntry(String uid) {
            this.uid = uid;
        }
    }

    static class RankingAdapter extends RecyclerView.Adapter<RankingAdapter.VH> {
        static class VH extends RecyclerView.ViewHolder {
            final android.widget.TextView rank, player, points;
            VH(android.view.View itemView) {
                super(itemView);
                rank = itemView.findViewById(R.id.rank);
                player = itemView.findViewById(R.id.player);
                points = itemView.findViewById(R.id.points);
            }
        }

        private final List<RankingEntry> data;
        RankingAdapter(List<RankingEntry> data) {
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
            RankingEntry entry = data.get(position);
            String medal = null;
            switch (entry.medalCategory) {
                case 1 -> medal = "🥇"; // 🥇
                case 2 -> medal = "🥈"; // 🥈
                case 3 -> medal = "🥉"; // 🥉
            }
            if (medal != null) {
                holder.rank.setText(medal);
            } else {
                holder.rank.setText(String.valueOf(position + 1));
            }
            holder.player.setText(entry.nickname != null ? entry.nickname : entry.uid);
            Context context = holder.itemView.getContext();
            String formatted = context.getResources().getQuantityString(R.plurals.wins_format, entry.wins, entry.wins);
            holder.points.setText(formatted);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}
