package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.view.View;
import androidx.core.content.ContextCompat;

public class MatchAdapter
        extends RecyclerView.Adapter<MatchAdapter.VH> {

    public static class VH extends RecyclerView.ViewHolder {
         final TextView opponent, presence, status;
        VH(View v) {
            super(v);
            opponent = v.findViewById(R.id.opponent);
            presence = v.findViewById(R.id.presence);
            status   = v.findViewById(R.id.status);
        }
    }

    /* ───── caches ───── */
    private final Map<String,String>  nickCache     = new HashMap<>();
    private final Map<String,String>  presCache     = new HashMap<>();       // uid → "online|active|offline"
    private final Map<String,ListenerRegistration> presSubs = new HashMap<>();

    private final List<DocumentSnapshot> matches = new ArrayList<>();
    private final String myUid;
    private final OnMatchClick clickCB;

    interface OnMatchClick { void onOpen(String matchId); }

    MatchAdapter(String myUid, OnMatchClick cb) {
        this.myUid   = myUid;
        this.clickCB = cb;
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int v)
    { return new VH(LayoutInflater.from(p.getContext())
            .inflate(R.layout.item_match, p, false)); }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DocumentSnapshot m = matches.get(pos);
        String a = m.getString("playerA");
        String b = m.getString("playerB");
        String oppUid = myUid.equals(a) ? b : a;

        /* ----------- nickname lookup ----------- */
        String nick = nickCache.get(oppUid);
        if (nick == null) {
            h.opponent.setText(Objects.requireNonNull(oppUid).substring(0, 6));   // temporary stub

            FirebaseFirestore.getInstance()
                    .collection("users").document(oppUid).get()
                    .addOnSuccessListener(d -> {
                        String n = d.getString("nickname");
                        if (n == null) return;

                        nickCache.put(oppUid, n);

                        int posNow = h.getAdapterPosition();        // ← works on all versions
                        if (posNow != RecyclerView.NO_POSITION) {
                            notifyItemChanged(posNow);              // refresh the correct row
                        }
                    });
        } else {
            h.opponent.setText(nick);   // nickname already cached
        }

        /* ---------- PRESENCE ---------- */
        String pState = presCache.get(oppUid);
        if (pState == null) {                     // first time we see this UID
            h.presence.setText("…");              // quick placeholder

            // real-time listener (once per UID)
            ListenerRegistration reg = FirebaseFirestore.getInstance()
                    .collection("users").document(Objects.requireNonNull(oppUid))
                    .addSnapshotListener((snap, e) -> {
                        if (e != null || snap == null || !snap.exists()) return;

                        boolean online = Boolean.TRUE.equals(snap.getBoolean("online"));
                        boolean active = Boolean.TRUE.equals(snap.getBoolean("active"));

                        String state = online ? "online"
                                : active ? "active"
                                : "offline";
                        presCache.put(oppUid, state);

                        int p = h.getAdapterPosition();
                        if (p != RecyclerView.NO_POSITION) notifyItemChanged(p);
                    });

            presSubs.put(oppUid, reg);
        } else {
            h.presence.setText(
                    switch (pState) {
                        case "online"  -> "Online";
                        case "active"  -> "Active";
                        default        -> "Offline";
                    });
            int colour = switch (pState) {
                case "online"  -> ContextCompat.getColor(h.itemView.getContext(), R.color.colorGreenDark);
                case "active"  -> ContextCompat.getColor(h.itemView.getContext(), R.color.colorAccent);
                default        -> ContextCompat.getColor(h.itemView.getContext(), R.color.colorGrey);
            };
            h.presence.setTextColor(colour);
        }

        /* ----------- status label ----------- */
        String st = m.getString("status");          // scheduled | playing | done
        h.status.setText(st);
        int colour = switch (Objects.requireNonNull(st)) {
            case "playing"   -> ContextCompat.getColor(
                    h.itemView.getContext(), R.color.colorAccent);
            case "done"      -> ContextCompat.getColor(
                    h.itemView.getContext(), R.color.colorGrey);
            default          -> ContextCompat.getColor(
                    h.itemView.getContext(), R.color.colorGreenDark);
        };
        h.status.setTextColor(colour);
        h.itemView.setOnClickListener(v -> clickCB.onOpen(m.getId()));
    }


    @Override public int getItemCount() { return matches.size(); }

    /* public helpers for the listener */
    void add(int idx, DocumentSnapshot d) { matches.add(idx, d); notifyItemInserted(idx); }
    void set(int idx, DocumentSnapshot d){ matches.set(idx, d);  notifyItemChanged(idx); }
    void move(int o,int n,DocumentSnapshot d){
        matches.remove(o); matches.add(n,d); notifyItemMoved(o,n); }
    void remove(int idx){ matches.remove(idx); notifyItemRemoved(idx); }

    /* tidy-up to avoid leaks */
    @Override public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        for (ListenerRegistration l : presSubs.values()) l.remove();
        presSubs.clear();
    }
}

