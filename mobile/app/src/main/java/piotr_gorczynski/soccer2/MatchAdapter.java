package piotr_gorczynski.soccer2;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import android.content.Context;

public class MatchAdapter
        extends RecyclerView.Adapter<MatchAdapter.VH> {
    private final Context context;

    public static class VH extends RecyclerView.ViewHolder {
         final TextView opponent, presence, status;
         final Button inviteBtn;

        // Cached for click handling
        DocumentSnapshot snap;
        String           oppUid;

         VH(View v) {
            super(v);
            opponent = v.findViewById(R.id.opponent);
            presence = v.findViewById(R.id.presence);
            status   = v.findViewById(R.id.status);
            inviteBtn = v.findViewById(R.id.inviteBtn);
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

    private final String tournamentId;


    MatchAdapter(@NonNull Context context,
                 @NonNull String  myUid,
                 @NonNull String  tournamentId) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".<init>: Constructor stared...");
        this.context       = context;
        this.myUid         = myUid;
        this.tournamentId  = Objects.requireNonNull(tournamentId,
                "tournamentId must not be null");
        setHasStableIds(true);   // ✅ tell RecyclerView this adapter uses stable IDs
    }

    @Override
    public long getItemId(int position) {
        // For Firestore: each match doc ID is already unique.
        // Turn it into a positive 64-bit value:
        return matches.get(position).getId().hashCode() & 0xffffffffL;
    }

    /* helper inside MatchAdapter ----------------------------------------- */
    private int indexForUid(@NonNull String uid) {
        for (int i = 0; i < matches.size(); i++) {
            DocumentSnapshot m = matches.get(i);
            String player0 = m.getString("player0"), player1 = m.getString("player1");
            if (uid.equals(player0) || uid.equals(player1)) return i;
        }
        return RecyclerView.NO_POSITION;
    }

    @Override
    public @NonNull VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        VH h = new VH(view);

        // ✅ Set listener once
        h.inviteBtn.setOnClickListener(v -> {
            DocumentSnapshot mNow = h.snap;   // cached doc
            String oppUidNow      = h.oppUid; // cached opponent UID
            // … but if they’re still null (very first tap), look them up
            if (mNow == null || oppUidNow == null) {
                int pos = h.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;

                mNow       = matches.get(pos);
                String p0  = mNow.getString("player0");
                String p1  = mNow.getString("player1");
                oppUidNow  = myUid.equals(p0) ? p1 : p0;
                if (oppUidNow == null) return;
            }

            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object() {
                    }.getClass().getEnclosingMethod()).getName()
                    + ": On click started");

            String matchPath = mNow.getReference().getPath();
            Map<String, Object> invite = new HashMap<>();
            invite.put("from", myUid);
            invite.put("to", oppUidNow);
            SharedPreferences prefs = context.getSharedPreferences(context.getPackageName() + "_preferences", MODE_PRIVATE);
            invite.put("fromNickname", prefs.getString("nickname", null));
            invite.put("toNickname", h.opponent.getText());
            invite.put("createdAt", FieldValue.serverTimestamp());
            invite.put("status", "pending");
            invite.put("tournamentId", tournamentId);
            invite.put("matchPath", matchPath);

            FirebaseFirestore.getInstance().collection("invitations")
                    .add(invite)
                    .addOnSuccessListener(docRef -> {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName()
                                + ": Invite sent");

                        Toast.makeText(context, "Invitation sent", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(context, WaitingActivity.class);
                        intent.putExtra("inviteId", docRef.getId());
                        context.startActivity(intent);
                        if (context instanceof Activity) {
                            ((Activity) context).finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to send invite", Toast.LENGTH_SHORT).show();
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName()
                                + ": Sending invite failed", e);
                    });
        });

        return h;
    }


    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        DocumentSnapshot m = matches.get(pos);
        h.snap   = m;                                       // 🆕 keep current doc
        h.oppUid = myUid.equals(m.getString("player0"))
                ? m.getString("player1")
                : m.getString("player0");


        String player0 = m.getString("player0");
        String player1 = m.getString("player1");
        String oppUid = myUid.equals(player0) ? player1 : player0;

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
                        int idx = indexForUid(oppUid);          // or use the doc-id instead of UID
                        if (idx != RecyclerView.NO_POSITION) {
                            notifyItemChanged(idx, "nickname");             // refresh the *right* row
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

                    int idx = indexForUid(oppUid);
                    if (idx != RecyclerView.NO_POSITION) {
                        notifyItemChanged(idx, "presence");   // 🔄 partial update
                    }
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



        // Determine button visibility
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": inviteBtn visibility logic: st=" + st + ", pState=" + pState);

        boolean isActiveOrCompleted = "playing".equals(st) || "completed".equals(st);
        boolean isOffline = "offline".equalsIgnoreCase(pState);

        if (isActiveOrCompleted || isOffline) {
            h.inviteBtn.setVisibility(View.GONE);
        } else {
            h.inviteBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH h,
                                 int position,
                                 @NonNull List<Object> payloads) {

        if (!payloads.isEmpty()) {
            Object tag = payloads.get(0);

            // --- presence only ---
            if ("presence".equals(tag)) {
                // ---- inside the payload branch for "presence" ----
                String pState = Objects.requireNonNullElse(
                        presCache.get(h.oppUid),        // may be null
                        "offline");                     // safe fallback

                long lastHb = 0L;
                Long boxed   = hbCache.get(h.oppUid);   // could be null
                if (boxed != null) lastHb = boxed;

                // build label / colour safely
                String label;
                int colour = switch (pState) {
                    case "online"  -> {
                        label = "Online";
                        yield ContextCompat.getColor(
                                h.itemView.getContext(), R.color.colorGreenDark);
                    }
                    case "active"  -> {
                        label = "Last seen " + englishRelative(lastHb);
                        yield ContextCompat.getColor(
                                h.itemView.getContext(), R.color.colorGreenDark);
                    }
                    default        -> {
                        label = "Offline";
                        yield ContextCompat.getColor(
                                h.itemView.getContext(), R.color.colorGrey);
                    }
                };
                h.presence.setText(label);
                h.presence.setTextColor(colour);
                return;
            }

            // --- nickname only (optional) ---
            if ("nickname".equals(tag)) {
                String nick = nickCache.get(h.oppUid);
                h.opponent.setText(nick != null ? nick
                        : h.oppUid.substring(0, 6));
                return;
            }
        }

        // No payload (or unknown) → do the full bind
        super.onBindViewHolder(h, position, payloads);
    }    
    

    @Override public int getItemCount() { return matches.size(); }

    /* public helpers for the listener */
    void add(int idx, DocumentSnapshot d) { matches.add(idx, d); notifyItemInserted(idx); }
    void set(int idx, DocumentSnapshot d){
        matches.set(idx, d);
        notifyItemChanged(idx);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                + ": 📌 notifyItemChanged(" + idx + ") triggered from nickname or presence update");
    }
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

