package piotr_gorczynski.soccer2;        // <-- adjust

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TournamentAdapter
        extends RecyclerView.Adapter<TournamentAdapter.VH> {

    /** callback for the Join button */
    @SuppressWarnings("unused")
    public interface OnJoinClick { void onJoin(DocumentSnapshot tournamentDoc); }

    /** callback for the Leave button */
    @SuppressWarnings("unused")
    public interface OnLeaveClick { void onLeave(DocumentSnapshot tournamentDoc); }

    public interface OnEndedClick {
        void onEnded(DocumentSnapshot tournamentDoc);
    }

    private final List<DocumentSnapshot> data;
    private final OnJoinClick joinListener;
    private final OnLeaveClick leaveListener;

    private final OnEndedClick endedListener;
    private final boolean isEndedMode;

    public TournamentAdapter(List<DocumentSnapshot> data, OnJoinClick joinListener, OnLeaveClick leaveListener) {
        this.data     = data;
        this.joinListener = joinListener;
        this.leaveListener = leaveListener;
        this.endedListener = null;
        this.isEndedMode = false;
    }

    // New constructor for ended mode
    public TournamentAdapter(List<DocumentSnapshot> data, OnEndedClick endedListener) {
        this.data = data;
        this.joinListener = null;
        this.leaveListener = null;
        this.endedListener = endedListener;
        this.isEndedMode = true;
    }

    // ---------- View-Holder ----------
    public static class VH extends RecyclerView.ViewHolder {
        final TextView name, slots, endsIn, notRegistered;
        final Button   joinBtn, leaveBtn;
        VH(@NonNull View v) {
            super(v);
            name    = v.findViewById(R.id.name);
            slots   = v.findViewById(R.id.slots);
            endsIn  = v.findViewById(R.id.endsIn);
            notRegistered = v.findViewById(R.id.notRegistered);
            joinBtn = v.findViewById(R.id.joinBtn);
            leaveBtn = v.findViewById(R.id.leaveBtn);
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
        Object raw = doc.get("participantsCount");   // returns Long *or* Double
        long joined = raw instanceof Number ? ((Number) raw).longValue() : 0L;
        raw = doc.get("maxParticipants");
        long max = raw instanceof Number ? ((Number) raw).longValue() : 0L;
        String status       = doc.getString("status");            // "registering" | "running"
        Timestamp regDL     = doc.getTimestamp("registrationDeadline");
        Timestamp endDL     = doc.getTimestamp("matchesDeadline");



        /* ---------- common fields ---------- */
        h.name.setText(name);
        h.slots.setText(
                h.itemView.getContext().getString(R.string.slots_format, joined, max)
        );

        /* ---------- branching by status ---------- */
        if (isEndedMode) {
            // === Ended mode ===
            String endedWhen = endDL != null
                    ? "Ended " + englishRelative(endDL.toDate().getTime())
                    : "Ended";
            h.endsIn.setText(endedWhen);

            h.joinBtn.setEnabled(true);
            h.joinBtn.setText(R.string.view_results);
            h.joinBtn.setOnClickListener(v -> {
                if (endedListener != null)
                    endedListener.onEnded(doc);
            });
            h.leaveBtn.setVisibility(View.GONE);

        } else if ("registering".equals(status)) {

            // ① label: “Registration ends in …”
            long mLeft = Objects.requireNonNull(regDL).toDate().getTime() - System.currentTimeMillis();
            String endsText = (mLeft <= 0)
                    ? "Registration closed. Tournament will start within 1 hour"
                    : "Registration ends "+englishRelative(regDL.toDate().getTime());
            h.endsIn.setText(endsText);

            // ② buttons: Join & Leave
            boolean full   = joined >= max;
            boolean closed = mLeft <= 0;
            h.joinBtn.setText(R.string.join);
            h.leaveBtn.setText(R.string.leave);
            h.joinBtn.setEnabled(false);
            h.leaveBtn.setEnabled(false);
            h.leaveBtn.setVisibility(View.VISIBLE);

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;
            if (uid != null) {
                FirebaseFirestore.getInstance()
                        .collection("tournaments").document(tid)
                        .collection("participants").document(uid)
                        .get().addOnSuccessListener(p -> {
                            boolean joinedAlready = p.exists();
                            if (joinedAlready) {
                                h.joinBtn.setEnabled(false);
                                h.leaveBtn.setEnabled(true);
                            } else {
                                h.joinBtn.setEnabled(!full && !closed);
                                h.leaveBtn.setEnabled(false);
                            }
                        });
            }

            h.joinBtn.setOnClickListener(v -> {
                if (joinListener != null) joinListener.onJoin(doc);
            });
            h.leaveBtn.setOnClickListener(v -> {
                if (leaveListener != null) leaveListener.onLeave(doc);
            });

        } else {   // == "running"

            // ① label: “Tournament running – ends in …”
            long mLeft = endDL != null
                    ? endDL.toDate().getTime() - System.currentTimeMillis()
                    : -1;
            if (endDL == null) {
                Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": " + tid + " has no matchesDeadline!");
            } else {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": Deadline  : " + endDL.toDate());
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": Device now: " + new Date());
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                        + ": mLeft (ms): " + mLeft);
            }
            String endsText = (mLeft <= 0)
                    ? "Tournament ends now..."
                    : "Tournament running. Ends " + englishRelative(
                    endDL.toDate().getTime());

            h.endsIn.setText(endsText);
            h.notRegistered.setVisibility(View.GONE);

            // ② button: Open (enabled only if user joined)
            h.joinBtn.setText(R.string.open);
            h.leaveBtn.setVisibility(View.GONE);
            h.joinBtn.setEnabled(false);

            String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : null;

            if (uid != null) {
                FirebaseFirestore.getInstance()
                        .collection("tournaments").document(tid)
                        .collection("participants").document(uid)
                        .get().addOnSuccessListener(p -> {
                            boolean joinedAlready = p.exists();
                            if (joinedAlready) {
                                h.joinBtn.setEnabled(true);
                                h.notRegistered.setVisibility(View.GONE);
                            } else {
                                h.joinBtn.setVisibility(View.GONE);
                                h.notRegistered.setText(R.string.not_registered);
                                h.notRegistered.setVisibility(View.VISIBLE);
                            }
                        });
            } else {
                h.joinBtn.setVisibility(View.GONE);
                h.notRegistered.setText(R.string.not_registered);
                h.notRegistered.setVisibility(View.VISIBLE);
            }

            h.joinBtn.setOnClickListener(v -> {
                Intent i = new Intent(v.getContext(), TournamentLobbyActivity.class)
                        .putExtra("tournamentId", tid)
                        .putExtra("tournamentName", name);
                v.getContext().startActivity(i);
            });
        }
    }

    @Override
    public int getItemCount() { return data.size(); }

    /** Absolute, English-only relative time. Works for past & future moments. */
    public static String englishRelative(long targetTimeMillis) {

        final long NOW  = System.currentTimeMillis();
        long diff       = targetTimeMillis - NOW;      // + => future, – => past

        final long MIN  = 60_000L;
        final long HOUR = 60 * MIN;
        final long DAY  = 24 * HOUR;

        // ── “just now / in a moment” ───────────────────────────────
        if (Math.abs(diff) < MIN) {
            return diff >= 0 ? "in a moment" : "just now";
        }

        // ── FUTURE (“in …”) ────────────────────────────────────────
        if (diff > 0) {
            if (diff < HOUR)  { long m = diff / MIN;  return "in " + m + " min" + (m == 1 ? "" : "s"); }
            if (diff < DAY)   { long h = diff / HOUR; return "in " + h + " h"; }
            if (diff < 2*DAY) { return "tomorrow"; }
            long d = diff / DAY;                       return "in " + d + " days";
        }

        // ── PAST (“… ago”) ─────────────────────────────────────────
        diff = -diff;                                 // make positive
        if (diff < HOUR)  { long m = diff / MIN;  return m + " min" + (m == 1 ? "" : "s") + " ago"; }
        if (diff < DAY)   { long h = diff / HOUR; return h + " h ago"; }
        if (diff < 2*DAY) { return "yesterday"; }
        long d = diff / DAY;                          return d + " days ago";
    }

}
