package piotr_gorczynski.soccer2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.VH> {
    interface OnAddClick { void onAdd(String uid); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nickname;
        final TextView presence;
        final Button addBtn;
        String uid;
        VH(@NonNull View v) {
            super(v);
            nickname = v.findViewById(R.id.nickname);
            presence = v.findViewById(R.id.presence);
            addBtn = v.findViewById(R.id.addBtn);
        }
    }

    private final List<DocumentSnapshot> data = new ArrayList<>();
    private final OnAddClick listener;
    private Set<String> friendUids = new HashSet<>();
    private final Map<String,String> presCache = new HashMap<>();
    private final Map<String,Long> hbCache = new HashMap<>();
    private static final class RtdbSub { final DatabaseReference ref; final ValueEventListener l; RtdbSub(DatabaseReference r, ValueEventListener l){this.ref=r;this.l=l;}}
    private final Map<String,RtdbSub> presSubs = new HashMap<>();

    UserSearchAdapter(OnAddClick listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_search, parent, false);
        VH h = new VH(v);
        h.addBtn.setOnClickListener(btn -> {
            if (h.uid != null) listener.onAdd(h.uid);
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(h, position);
        } else {
            String uid = h.uid;
            if (uid == null) return;
            
            for (Object payload : payloads) {
                if ("presence".equals(payload)) {
                    String pState = presCache.get(uid);
                    if (pState != null) bindPresence(h, uid, pState);
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DocumentSnapshot d = data.get(position);
        String uid = d.getId();
        h.uid = uid;
        String nick = d.getString("nickname");
        if (nick == null) nick = uid.substring(0, 6);
        h.nickname.setText(nick);
        
        // Disable "Add" button if this user is already a friend
        boolean isAlreadyFriend = friendUids.contains(uid);
        h.addBtn.setEnabled(!isAlreadyFriend);
        h.addBtn.setAlpha(isAlreadyFriend ? 0.3f : 1.0f);
        
        // Load presence/status info
        String pState = presCache.get(uid);
        if (pState == null) {
            h.presence.setText("…");
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(uid);
            ValueEventListener l = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snap) {
                    if (!snap.exists()) {
                        presCache.put(uid, "offline");
                        hbCache.put(uid, 0L);
                        int idx = indexForUid(uid);
                        if (idx != RecyclerView.NO_POSITION) notifyItemChanged(idx, "presence");
                        return;
                    }
                    String stateStr = snap.child("state").getValue(String.class);
                    Long lastHbBox = snap.child("last_heartbeat").getValue(Long.class);
                    long lastHb = lastHbBox != null ? lastHbBox : 0L;
                    String state = "offline";
                    if ("online".equals(stateStr)) {
                        state = "online";
                    } else if (lastHb > 0L) {
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
            presSubs.put(uid, new RtdbSub(ref, l));
        } else {
            bindPresence(h, uid, pState);
        }
    }
    
    private void bindPresence(@NonNull VH h, @NonNull String uid, @NonNull String state) {
        Context context = h.itemView.getContext();
        String label;
        int colour = switch (state) {
            case "online" -> {
                label = context.getString(R.string.presence_online);
                yield ContextCompat.getColor(context, R.color.colorGreenDark);
            }
            case "active" -> {
                long last = hbCache.getOrDefault(uid, 0L);
                label = SafeStringFormatter.safeGetString(context, R.string.presence_last_seen, MatchAdapter.englishRelative(last));
                yield ContextCompat.getColor(context, R.color.colorGreenDark);
            }
            default -> {
                label = context.getString(R.string.presence_offline);
                yield ContextCompat.getColor(context, R.color.colorGrey);
            }
        };
        h.presence.setText(label);
        h.presence.setTextColor(colour);
    }
    
    private int indexForUid(@NonNull String uid) {
        for (int i = 0; i < data.size(); i++) {
            if (uid.equals(data.get(i).getId())) return i;
        }
        return -1;
    }

    @Override
    public long getItemId(int position) {
        return data.get(position).getId().hashCode() & 0xffffffffL;
    }

    @Override
    public int getItemCount() { return data.size(); }

    void addResults(List<DocumentSnapshot> docs) {
        int start = data.size();
        data.addAll(docs);
        notifyItemRangeInserted(start, docs.size());
    }

    void clear() {
        // Clean up presence subscriptions when clearing data
        for (RtdbSub sub : presSubs.values()) {
            sub.ref.removeEventListener(sub.l);
        }
        presSubs.clear();
        presCache.clear();
        hbCache.clear();
        data.clear();
        notifyDataSetChanged();
    }
    
    void setFriendUids(Set<String> friendUids) {
        this.friendUids = new HashSet<>(friendUids);
        notifyDataSetChanged(); // Refresh all items to update button states
    }
    
    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        for (RtdbSub sub : presSubs.values()) {
            sub.ref.removeEventListener(sub.l);
        }
        presSubs.clear();
    }
}
