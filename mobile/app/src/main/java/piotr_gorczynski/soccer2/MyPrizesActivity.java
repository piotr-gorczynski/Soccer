package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyPrizesActivity extends BaseActivity {
    private final List<PrizeItem> prizes = new ArrayList<>();
    private PrizeAdapter adapter;
    private TextView emptyPrizes;
    private ListenerRegistration prizesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AppFlavourDetector.supportsPrizeFeatures(this)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_my_prizes);

        Toolbar toolbar = findViewById(R.id.my_prizes_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.my_prizes);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        emptyPrizes = findViewById(R.id.emptyPrizes);
        RecyclerView list = findViewById(R.id.prizesList);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PrizeAdapter(prizes, paymentId -> startActivity(
                new Intent(this, PrizeDetailsActivity.class)
                        .putExtra("paymentId", paymentId)));
        list.setAdapter(adapter);
        loadPrizes();
    }

    private void loadPrizes() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        prizesListener = db.collection("payments")
                .whereEqualTo("userId", userId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        emptyPrizes.setVisibility(View.VISIBLE);
                        return;
                    }
                    prizes.clear();
                    for (DocumentSnapshot payment : snapshot.getDocuments()) {
                        PrizeItem item = new PrizeItem(payment);
                        prizes.add(item);
                        String tournamentId = payment.getString("tournamentId");
                        if (tournamentId != null && !tournamentId.isEmpty()) {
                            db.collection("tournaments").document(tournamentId).get()
                                    .addOnSuccessListener(tournament -> {
                                        if (tournament.exists()) {
                                            item.tournamentName = tournament.getString("name");
                                            adapter.notifyItemChanged(prizes.indexOf(item));
                                        }
                                    });
                        }
                    }
                    prizes.sort((left, right) -> {
                        if (left.createdAt == null && right.createdAt == null) return 0;
                        if (left.createdAt == null) return 1;
                        if (right.createdAt == null) return -1;
                        return right.createdAt.compareTo(left.createdAt);
                    });
                    adapter.notifyDataSetChanged();
                    emptyPrizes.setVisibility(prizes.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    @Override
    protected void onDestroy() {
        if (prizesListener != null) prizesListener.remove();
        super.onDestroy();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private static class PrizeItem {
        final String paymentId;
        final String status;
        final String currency;
        final Number amount;
        final long rank;
        final com.google.firebase.Timestamp createdAt;
        String tournamentName;

        PrizeItem(DocumentSnapshot payment) {
            paymentId = payment.getId();
            status = payment.getString("status");
            currency = payment.getString("currency");
            Number value = payment.getDouble("amount");
            if (value == null) value = payment.getLong("amount");
            amount = value;
            Long rankValue = payment.getLong("rank");
            rank = rankValue == null ? 1 : rankValue;
            createdAt = payment.getTimestamp("createdAt");
        }
    }

    private class PrizeAdapter extends RecyclerView.Adapter<PrizeAdapter.Holder> {
        private final List<PrizeItem> items;
        private final PrizeClickListener listener;

        PrizeAdapter(List<PrizeItem> items, PrizeClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_prize, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PrizeItem item = items.get(position);
            if (item.tournamentName == null) {
                holder.tournament.setText(R.string.prize_unknown_tournament);
            } else {
                holder.tournament.setText(item.tournamentName);
            }
            holder.amount.setText(formatAmount(item.amount) + " " +
                    (item.currency == null ? "" : item.currency));
            holder.rank.setText(getString(R.string.prize_rank, item.rank));
            holder.status.setText(getString(R.string.prize_list_status,
                    getPaymentStatusLabel(item.status)));
            holder.itemView.setOnClickListener(view -> listener.onClick(item.paymentId));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView tournament;
            final TextView amount;
            final TextView rank;
            final TextView status;

            Holder(View itemView) {
                super(itemView);
                tournament = itemView.findViewById(R.id.prizeTournamentName);
                amount = itemView.findViewById(R.id.prizeAmount);
                rank = itemView.findViewById(R.id.prizeRank);
                status = itemView.findViewById(R.id.prizeStatus);
            }
        }
    }

    private String getPaymentStatusLabel(String status) {
        int resource = switch (status == null ? "" : status) {
            case "ready_for_processing" -> R.string.payment_details_received;
            case "processing" -> R.string.payment_status_processing;
            case "sent" -> R.string.payment_status_sent;
            case "completed" -> R.string.payment_status_completed;
            case "action_required" -> R.string.payment_status_action_required;
            case "cancelled" -> R.string.payment_status_cancelled;
            default -> R.string.payment_details_required;
        };
        return getString(resource);
    }

    private String formatAmount(Number amount) {
        if (amount == null) return "";
        double value = amount.doubleValue();
        return value == Math.rint(value) ? String.valueOf(amount.longValue()) : String.valueOf(value);
    }

    private interface PrizeClickListener {
        void onClick(String paymentId);
    }
}
