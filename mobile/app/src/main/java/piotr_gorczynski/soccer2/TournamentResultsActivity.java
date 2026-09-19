package piotr_gorczynski.soccer2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentResultsActivity extends BaseActivity {

    private StandingsAdapter adapter;
    private final List<StandingEntry> standings = new ArrayList<>();
    private final List<String> payoutMethodCodes = new ArrayList<>();
    private LinearLayout paymentDetailsPanel;
    private Spinner paymentMethodSpinner;
    private EditText paymentAccountNumber;
    private TextView paymentPrizeSummary;
    private TextView paymentDetailsStatus;
    private TextView paymentIssueMessage;
    private TextView paymentTransferDetails;
    private Button savePaymentDetailsButton;
    private DocumentSnapshot winnerPayment;
    private ListenerRegistration paymentListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tournament_results);

        Toolbar toolbar = findViewById(R.id.results_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        RecyclerView standingsList = findViewById(R.id.standingsList);
        standingsList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StandingsAdapter(standings);
        standingsList.setAdapter(adapter);

        paymentDetailsPanel = findViewById(R.id.paymentDetailsPanel);
        paymentMethodSpinner = findViewById(R.id.paymentMethodSpinner);
        paymentAccountNumber = findViewById(R.id.paymentAccountNumber);
        paymentPrizeSummary = findViewById(R.id.paymentPrizeSummary);
        paymentDetailsStatus = findViewById(R.id.paymentDetailsStatus);
        paymentIssueMessage = findViewById(R.id.paymentIssueMessage);
        paymentTransferDetails = findViewById(R.id.paymentTransferDetails);
        savePaymentDetailsButton = findViewById(R.id.savePaymentDetailsButton);

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
                    Objects.requireNonNull(getSupportActionBar())
                            .setTitle(doc.getString("name"));
                    loadWinnerPayment(tid, doc.getString("regulation"));
                });

        // Step 1: fetch all participants
        db.collection("tournaments").document(tid)
                .collection("participants").get()
                .addOnSuccessListener(partSnap -> {

                    Map<String, StandingEntry> scoreMap = new java.util.HashMap<>();
                    for (DocumentSnapshot part : partSnap) {
                        scoreMap.put(part.getId(), new StandingEntry(part.getId()));
                    }

                    // Step 2: fetch all matches and count wins
                    db.collection("tournaments").document(tid)
                            .collection("matches").get()
                            .addOnSuccessListener(matchSnap -> {
                                for (DocumentSnapshot match : matchSnap) {
                                    String winner = match.getString("winner");
                                    if (winner == null || winner.isEmpty()) continue;

                                    StandingEntry entry = scoreMap.get(winner);
                                    if (entry != null) {
                                        entry.wins += 1;
                                    }
                                }

                                // Step 3: fetch all nicknames
                                List<String> uids = new ArrayList<>(scoreMap.keySet());
                                if (uids.isEmpty()) {
                                    standings.clear();
                                    adapter.notifyDataSetChanged();
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
                                            assignMedals(standings);
                                            adapter.notifyDataSetChanged();
                                        });
                            });
                });
    }

    private void loadWinnerPayment(String tournamentId, String regulationId) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null || TextUtils.isEmpty(regulationId)) {
            paymentDetailsPanel.setVisibility(View.GONE);
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (paymentListener != null) paymentListener.remove();
        paymentListener = db.collection("payments")
                .whereEqualTo("userId", userId)
                .whereEqualTo("tournamentId", tournamentId)
                .whereEqualTo("rank", 1)
                .limit(1)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null) {
                        paymentDetailsPanel.setVisibility(View.GONE);
                        return;
                    }
                    if (snapshot.isEmpty()) {
                        paymentDetailsPanel.setVisibility(View.GONE);
                        return;
                    }
                    winnerPayment = snapshot.getDocuments().get(0);
                    db.collection("regulations").document(regulationId).get()
                            .addOnSuccessListener(this::showPaymentDetailsForm)
                            .addOnFailureListener(regulationError ->
                                    paymentDetailsPanel.setVisibility(View.GONE));
                });
    }

    @SuppressWarnings("unchecked")
    private void showPaymentDetailsForm(DocumentSnapshot regulation) {
        if (!regulation.exists() || winnerPayment == null) return;

        Object prizeRulesValue = regulation.get("prizeRules");
        if (!(prizeRulesValue instanceof Map)) return;
        Object methodsValue = ((Map<String, Object>) prizeRulesValue).get("payoutMethods");
        if (!(methodsValue instanceof List) || ((List<?>) methodsValue).isEmpty()) return;

        payoutMethodCodes.clear();
        List<String> methodLabels = new ArrayList<>();
        for (Object value : (List<?>) methodsValue) {
            if (!(value instanceof String) || TextUtils.isEmpty((String) value)) continue;
            String code = ((String) value).trim().toLowerCase(java.util.Locale.ROOT);
            if (!payoutMethodCodes.contains(code)) {
                payoutMethodCodes.add(code);
                methodLabels.add(formatPayoutMethod(code));
            }
        }
        if (payoutMethodCodes.isEmpty()) return;

        ArrayAdapter<String> methodsAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, methodLabels);
        methodsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        paymentMethodSpinner.setAdapter(methodsAdapter);

        Number amount = winnerPayment.getDouble("amount");
        if (amount == null) amount = winnerPayment.getLong("amount");
        String currency = winnerPayment.getString("currency");
        String amountText = amount == null ? "" : formatAmount(amount);
        paymentPrizeSummary.setText(getString(
                R.string.payment_prize_summary, amountText, currency == null ? "" : currency));

        String paymentStatus = winnerPayment.getString("status");
        Map<String, Object> recipientInfo = (Map<String, Object>) winnerPayment.get("recipientInfo");
        if (recipientInfo != null) {
            String savedMethod = recipientInfo.get("method") instanceof String
                    ? (String) recipientInfo.get("method") : null;
            String savedAccount = recipientInfo.get("accountNumber") instanceof String
                    ? (String) recipientInfo.get("accountNumber") : null;
            int selectedIndex = savedMethod == null ? -1 : payoutMethodCodes.indexOf(savedMethod);
            if (selectedIndex >= 0) paymentMethodSpinner.setSelection(selectedIndex);
            if (savedAccount != null) paymentAccountNumber.setText(savedAccount);
        }

        setPaymentStatusMessage(paymentStatus);

        boolean editable = "awaiting_details".equals(paymentStatus)
                || "action_required".equals(paymentStatus);
        paymentMethodSpinner.setEnabled(editable);
        paymentAccountNumber.setEnabled(editable);
        savePaymentDetailsButton.setVisibility(editable ? View.VISIBLE : View.GONE);
        savePaymentDetailsButton.setOnClickListener(view -> savePaymentDetails());
        paymentDetailsPanel.setVisibility(View.VISIBLE);
    }

    @SuppressWarnings("unchecked")
    private void setPaymentStatusMessage(String status) {
        int messageResource = switch (status == null ? "" : status) {
            case "ready_for_processing" -> R.string.payment_details_received;
            case "processing" -> R.string.payment_status_processing;
            case "sent" -> R.string.payment_status_sent;
            case "completed" -> R.string.payment_status_completed;
            case "action_required" -> R.string.payment_status_action_required;
            case "cancelled" -> R.string.payment_status_cancelled;
            default -> R.string.payment_details_required;
        };
        paymentDetailsStatus.setText(messageResource);
        paymentIssueMessage.setText("");
        paymentIssueMessage.setVisibility(View.GONE);
        paymentTransferDetails.setText("");
        paymentTransferDetails.setVisibility(View.GONE);

        if ("action_required".equals(status) && winnerPayment != null) {
            Object issueValue = winnerPayment.get("issue");
            if (issueValue instanceof Map) {
                Object message = ((Map<String, Object>) issueValue).get("userMessage");
                if (message instanceof String && !TextUtils.isEmpty((String) message)) {
                    paymentIssueMessage.setText((String) message);
                    paymentIssueMessage.setVisibility(View.VISIBLE);
                }
            }
        }

        if (("sent".equals(status) || "completed".equals(status)) && winnerPayment != null) {
            Object transferValue = winnerPayment.get("transfer");
            if (transferValue instanceof Map) {
                Map<String, Object> transfer = (Map<String, Object>) transferValue;
                Object provider = transfer.get("provider");
                Object reference = transfer.get("providerReference");
                if (provider instanceof String && reference instanceof String
                        && !TextUtils.isEmpty((String) provider)
                        && !TextUtils.isEmpty((String) reference)) {
                    paymentTransferDetails.setText(getString(
                            R.string.payment_transfer_details,
                            formatPayoutMethod((String) provider),
                            reference));
                    paymentTransferDetails.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (paymentListener != null) paymentListener.remove();
        super.onDestroy();
    }

    private void savePaymentDetails() {
        if (winnerPayment == null || payoutMethodCodes.isEmpty()) return;
        int selected = paymentMethodSpinner.getSelectedItemPosition();
        if (selected < 0 || selected >= payoutMethodCodes.size()) return;

        String method = payoutMethodCodes.get(selected);
        String accountNumber = BangladeshPayoutAccountValidator.normalize(
                paymentAccountNumber.getText().toString());
        if (!BangladeshPayoutAccountValidator.isValid(method, accountNumber)) {
            int errorResource = switch (method) {
                case "bkash", "nagad" -> R.string.payment_account_number_error_mobile;
                case "rocket" -> R.string.payment_account_number_error_rocket;
                default -> R.string.payment_account_number_error;
            };
            paymentAccountNumber.setError(getString(errorResource));
            return;
        }

        paymentAccountNumber.setText(accountNumber);

        Map<String, Object> recipientInfo = new HashMap<>();
        recipientInfo.put("method", method);
        recipientInfo.put("accountNumber", accountNumber);
        recipientInfo.put("submittedAt", FieldValue.serverTimestamp());

        savePaymentDetailsButton.setEnabled(false);
        Map<String, Object> update = new HashMap<>();
        update.put("recipientInfo", recipientInfo);
        update.put("status", "ready_for_processing");
        update.put("statusUpdatedAt", FieldValue.serverTimestamp());

        winnerPayment.getReference().update(update)
                .addOnSuccessListener(unused -> {
                    paymentDetailsStatus.setText(R.string.payment_details_received);
                    paymentMethodSpinner.setEnabled(false);
                    paymentAccountNumber.setEnabled(false);
                    savePaymentDetailsButton.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.payment_details_received, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(error -> {
                    savePaymentDetailsButton.setEnabled(true);
                    Toast.makeText(this, R.string.payment_details_save_failed, Toast.LENGTH_LONG).show();
                });
    }

    private String formatPayoutMethod(String method) {
        return switch (method) {
            case "bkash" -> "bKash";
            case "nagad" -> "Nagad";
            case "rocket" -> "Rocket";
            case "remitly" -> "Remitly";
            default -> method;
        };
    }

    private String formatAmount(Number amount) {
        double value = amount.doubleValue();
        return value == Math.rint(value)
                ? String.valueOf(amount.longValue())
                : String.valueOf(value);
    }

    private void assignMedals(List<StandingEntry> list) {
        if (list.isEmpty()) return;
        
        int currentPosition = 1; // current rank position (1st, 2nd, 3rd, etc.)
        int prevWins = -1;
        int groupSize = 0;
        
        for (StandingEntry e : list) {
            // If wins changed, move to next position (accounting for group size)
            if (prevWins != -1 && e.wins != prevWins) {
                currentPosition += groupSize;
                groupSize = 0;
            }
            
            groupSize++;
            
            // Assign rank to all players
            e.rank = currentPosition;
            
            // Assign medal only for positions 1, 2, 3 AND if the player has won at least one game
            if (currentPosition <= 3 && e.wins > 0) {
                e.medalCategory = currentPosition;
            } else {
                e.medalCategory = 0;
            }
            
            prevWins = e.wins;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /** Internal structure for one row */
    static class StandingEntry {
        final String uid;
        String nickname;
        int wins = 0;
        int medalCategory = 0; // 0=none,1=gold,2=silver,3=bronze
        int rank = 0; // actual rank position (1, 2, 3, etc.)

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
            String medal = null;
            switch (entry.medalCategory) {
                case 1 -> medal = "\uD83E\uDD47"; // 🥇
                case 2 -> medal = "\uD83E\uDD48"; // 🥈
                case 3 -> medal = "\uD83E\uDD49"; // 🥉
            }
            if (medal != null) {
                holder.rank.setText(medal);
            } else {
                holder.rank.setText(String.valueOf(entry.rank));
            }
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
