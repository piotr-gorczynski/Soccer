package piotr_gorczynski.soccer2;        // <-- adjust

import android.content.Intent;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Objects;

public class TournamentAdapter
        extends RecyclerView.Adapter<TournamentAdapter.VH> {

    /** callback for the Join button */
    public interface OnJoinClick { void onJoin(String tournamentId); }

    private final List<DocumentSnapshot> data;
    private final OnJoinClick listener;

    public TournamentAdapter(List<DocumentSnapshot> data, OnJoinClick listener) {
        this.data     = data;
        this.listener = listener;
    }

    // ---------- View-Holder ----------
    public static class VH extends RecyclerView.ViewHolder {
        TextView name, slots, endsIn;
        Button   joinBtn;
        VH(@NonNull View v) {
            super(v);
            name    = v.findViewById(R.id.name);
            slots   = v.findViewById(R.id.slots);
            endsIn  = v.findViewById(R.id.endsIn);
            joinBtn = v.findViewById(R.id.joinBtn);
        }
    }

    // ---------- REQUIRED METHODS ----------
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tournament, parent, false);
        return new VH(row);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {

        DocumentSnapshot doc = data.get(position);
        String tid          = doc.getId();

        String name         = doc.getString("name");
        long   joined       = Objects.requireNonNull(doc.getLong("participantsCount"));
        long   max          = Objects.requireNonNull(doc.getLong("maxParticipants"));
        String status       = doc.getString("status");            // "registering" | "running"
        Timestamp regDL     = doc.getTimestamp("registrationDeadline");
        Timestamp endDL     = doc.getTimestamp("matchesDeadline");

        /* ---------- common fields ---------- */
        h.name.setText(name);
        h.slots.setText(
                h.itemView.getContext().getString(R.string.slots_format, joined, max)
        );

        /* ---------- branching by status ---------- */
        if ("registering".equals(status)) {

            // ① label: “Registration ends in …”
            long mLeft = Objects.requireNonNull(regDL).toDate().getTime() - System.currentTimeMillis();
            String endsText = (mLeft <= 0)
                    ? "Registration closed"
                    : DateUtils.getRelativeTimeSpanString(regDL.toDate().getTime()).toString();
            h.endsIn.setText(endsText);

            // ② button: Join
            boolean full   = joined >= max;
            boolean closed = mLeft <= 0;
            h.joinBtn.setEnabled(!full && !closed);
            h.joinBtn.setText(R.string.join);
            h.joinBtn.setOnClickListener(v -> listener.onJoin(tid));

        } else {   // == "running"

            // ① label: “Tournament running – ends in …”
            long mLeft = endDL != null
                    ? endDL.toDate().getTime() - System.currentTimeMillis()
                    : -1;

            String endsText = (mLeft <= 0)
                    ? "Tournament running"
                    : "Ends in " + DateUtils.getRelativeTimeSpanString(
                    endDL.toDate().getTime()).toString();

            h.endsIn.setText(endsText);

            // ② button: Open
            h.joinBtn.setEnabled(true);
            h.joinBtn.setText(R.string.open);
            h.joinBtn.setOnClickListener(v -> {
                Intent i = new Intent(v.getContext(), TournamentLobbyActivity.class)
                        .putExtra("tournamentId", tid);
                v.getContext().startActivity(i);
            });
        }
    }

    @Override
    public int getItemCount() { return data.size(); }
}
