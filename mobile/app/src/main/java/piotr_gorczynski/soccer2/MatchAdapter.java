package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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

    private final Map<String, Long> hbCache = new HashMap<>();   // heartbeat cache
    @SuppressWarnings("ClassCanBeRecord")
    private static final class RtdbSub {
        final DatabaseReference ref;
        final ValueEventListener l;
        RtdbSub(DatabaseReference ref, ValueEventListener l) {
            this.ref = ref; this.l = l;
        }
    }
    private final Map<String,RtdbSub> presSubs = new HashMap<>();

    private final List<DocumentSnapshot> matches = new ArrayList<>();
    private final String myUid;
    private final OnMatchClick clickCB;

    interface OnMatchClick { void onOpen(String matchId); }

    MatchAdapter(String myUid, OnMatchClick cb) {
        this.myUid   = myUid;
        this.clickCB = cb;
    }
    /* helper inside MatchAdapter ----------------------------------------- */
    private int indexForUid(@NonNull String uid) {
        for (int i = 0; i < matches.size(); i++) {
            DocumentSnapshot m = matches.get(i);
            String a = m.getString("playerA"), b = m.getString("playerB");
            if (uid.equals(a) || uid.equals(b)) return i;
        }
        return RecyclerView.NO_POSITION;
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

        /* ---------- PRESENCE (now from RTDB) ---------- */
        String pState = presCache.get(oppUid);
        if (pState == null) {                       // first time we see this UID
            h.presence.setText("…");                // quick placeholder

            DatabaseReference ref = FirebaseDatabase.getInstance()
                    .getReference("status").child(Objects.requireNonNull(oppUid));

            // one ValueEventListener per UID
            ValueEventListener l = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snap) {
                    if (!snap.exists()) return;

                    String stateStr   = snap.child("state").getValue(String.class);
                    Long   lastHbBox  = snap.child("last_heartbeat").getValue(Long.class);
                    long   lastHb     = lastHbBox != null ? lastHbBox : 0L;

                    String state = "offline";
                    if ("online".equals(stateStr)) {
                        state = "online";
                    } else if (System.currentTimeMillis() - lastHb < 20 * 60_000L) {
                        state = "active";           // seen within 20 min window
                    }

                    presCache.put(oppUid, state);
                    hbCache.put(oppUid, lastHb);    // for “Last seen …”

                    int row = indexForUid(oppUid);
                    if (row != RecyclerView.NO_POSITION) notifyItemChanged(row);
                }
                @Override public void onCancelled(@NonNull DatabaseError e) { }
            };

            ref.addValueEventListener(l);
            presSubs.put(oppUid, new RtdbSub(ref, l));   // ✅ now types match
        } else {                                    // we already have cached data
            String label;
            int colour = switch (pState) {
                case "online" -> {
                    label  = "Online";
                    yield ContextCompat.getColor(
                            h.itemView.getContext(), R.color.colorGreenDark);
                }
                case "active" -> {
                    Long cached = hbCache.get(oppUid);   // may be null
                    long lastHb = cached != null ? cached : 0L;
                    label = "Last seen " + englishRelative(lastHb);
                    yield ContextCompat.getColor(
                            h.itemView.getContext(), R.color.colorGreenDark);
                }
                default -> {
                    label  = "Offline";
                    yield ContextCompat.getColor(
                            h.itemView.getContext(), R.color.colorGrey);
                }
            };

            h.presence.setText(label);
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
        for (RtdbSub sub : presSubs.values())      // remove RTDB listeners
            sub.ref.removeEventListener(sub.l);
        presSubs.clear();
    }


    /** English relative time: "just now", "4 mins ago", "2 h ago", "yesterday", "5 days ago". */
    static String englishRelative(long eventTimeMillis) {
        long diff = System.currentTimeMillis() - eventTimeMillis;

        final long MIN  = 60_000L;
        final long HOUR = 60 * MIN;
        final long DAY  = 24 * HOUR;

        if (diff < MIN)  return "just now";

        if (diff < HOUR) {
            long m = diff / MIN;
            return m + " min" + (m == 1 ? "" : "s") + " ago";
        }

        if (diff < DAY) {
            long h = diff / HOUR;
            return h + " h ago";
        }

        if (diff < 2 * DAY) return "yesterday";

        long d = diff / DAY;          // diff ≥ 2 days  ⇒  d ≥ 2
        return d + " days ago";
    }
}

