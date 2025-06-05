package piotr_gorczynski.soccer2;        // <-- adjust

import android.content.Context;
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
import java.util.concurrent.TimeUnit;

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

        String tid           = doc.getId();
        String name          = doc.getString("name");
        long joined = Objects.requireNonNull(
                doc.getLong("participantsCount"),
                "participantsCount missing in " + doc.getId()
        );

        long max    = Objects.requireNonNull(
                doc.getLong("maxParticipants"),
                "maxParticipants missing in " + doc.getId()
        );
        Timestamp deadlineTS = Objects.requireNonNull(
                doc.getTimestamp("registrationDeadline"),
                "registrationDeadline missing in " + doc.getId()
        );

        long millisLeft = deadlineTS.toDate().getTime() - System.currentTimeMillis();
        boolean full    = joined >= max;
        boolean closed  = millisLeft <= 0;

        // human-readable “ends in …”
        String endsText;
        if (closed) {
            endsText = "Registration closed";
        } else {
            long days  = TimeUnit.MILLISECONDS.toDays(millisLeft);
            long hours = TimeUnit.MILLISECONDS.toHours(millisLeft) % 24;
            if (days > 0)       endsText = "Registration ends in " + days  + " d " + hours + " h";
            else if (hours > 0) endsText = "Registration ends in " + hours + " h";
            else                endsText = "Registration ends in " +
                        TimeUnit.MILLISECONDS.toMinutes(millisLeft) + " m";
        }

        // bind to views
        h.name.setText(name);
        Context ctx = h.itemView.getContext();          // any Context works here
        String slotsText = ctx.getString(R.string.slots_format, joined, max);
        h.slots.setText(slotsText);
        h.endsIn.setText(endsText);

        h.joinBtn.setEnabled(!full && !closed);
        h.joinBtn.setOnClickListener(v -> listener.onJoin(tid));
    }

    @Override
    public int getItemCount() { return data.size(); }
}
