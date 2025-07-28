package piotr_gorczynski.soccer2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.content.ContextCompat;

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

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.VH> {

    public interface OnInviteClick { void onInvite(String uid); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nickname;
        final TextView presence;
        final Button inviteBtn;
        String uid;
        DocumentSnapshot doc; // not used but kept for parity
        VH(@NonNull View v) {
            super(v);
            nickname = v.findViewById(R.id.nickname);
            presence = v.findViewById(R.id.presence);
            inviteBtn = v.findViewById(R.id.inviteBtn);
        }
    }

    private final Context context;
    private final OnInviteClick listener;
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private final Map<String,String> nickCache = new HashMap<>();
    private final Map<String,String> presCache = new HashMap<>();
    private final Map<String,Long> hbCache = new HashMap<>();
    private static final class RtdbSub { final DatabaseReference ref; final ValueEventListener l; RtdbSub(DatabaseReference r, ValueEventListener l){this.ref=r;this.l=l;}}
    private final Map<String,RtdbSub> presSubs = new HashMap<>();

    FriendAdapter(Context context, OnInviteClick listener) {
        this.context = context;
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        VH h = new VH(v);
        h.inviteBtn.setOnClickListener(btn -> {
            if (h.uid != null) listener.onInvite(h.uid);
        });
        return h;
    }

    @Override
    public long getItemId(int position) {
        return docs.get(position).getId().hashCode() & 0xffffffffL;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DocumentSnapshot d = docs.get(position);
        h.doc = d;
        String uid = d.getId();
        h.uid = uid;

        String nick = nickCache.get(uid);
        if (nick == null) {
            h.nickname.setText(uid.substring(0,6));
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        String n = doc.getString("nickname");
                        if (n != null) {
                            nickCache.put(uid, n);
                            int idx = indexForUid(uid);
                            if (idx != RecyclerView.NO_POSITION) notifyItemChanged(idx, "nickname");
                        }
                    });
        } else {
            h.nickname.setText(nick);
        }

        String pState = presCache.get(uid);
        if (pState == null) {
            h.presence.setText("…");
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(uid);
            ValueEventListener l = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snap) {
                    if (!snap.exists()) return;
                    String stateStr = snap.child("state").getValue(String.class);
                    Long lastHbBox = snap.child("last_heartbeat").getValue(Long.class);
                    long lastHb = lastHbBox != null ? lastHbBox : 0L;
                    String state = "offline";
                    if ("online".equals(stateStr)) {
                        state = "online";
                    } else if (System.currentTimeMillis() - lastHb < 20 * 60_000L) {
                        state = "active";
                    }
                    presCache.put(uid, state);
                    hbCache.put(uid, lastHb);
                    int idx = indexForUid(uid);
                    if (idx != RecyclerView.NO_POSITION) notifyItemChanged(idx, "presence");
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            };
            ref.addValueEventListener(l);
            presSubs.put(uid, new RtdbSub(ref,l));
        } else {
            bindPresence(h, uid, pState);
        }
    }

    private void bindPresence(@NonNull VH h, @NonNull String uid, @NonNull String state) {
        String label;
        int colour = switch (state) {
            case "online" -> {
                label = "Online";
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGreenDark);
            }
            case "active" -> {
                long last = hbCache.getOrDefault(uid,0L);
                label = "Last seen " + MatchAdapter.englishRelative(last);
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGreenDark);
            }
            default -> {
                label = "Offline";
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGrey);
            }
        };
        h.presence.setText(label);
        h.presence.setTextColor(colour);
        boolean isOffline = "offline".equalsIgnoreCase(state);
        h.inviteBtn.setVisibility(isOffline ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() { return docs.size(); }

    private int indexForUid(@NonNull String uid) {
        for (int i=0;i<docs.size();i++) if (uid.equals(docs.get(i).getId())) return i; return -1;
    }

    void setData(List<DocumentSnapshot> friends) {
        docs.clear();
        docs.addAll(friends);
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        for (RtdbSub sub : presSubs.values()) sub.ref.removeEventListener(sub.l);
        presSubs.clear();
    }
}
