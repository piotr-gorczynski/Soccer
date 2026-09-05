package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
    private static final String TAG = "TAG_Soccer";
    private static final long PRESENCE_TIMEOUT_MS = 5_000L;

    interface OnAddClick { void onAdd(String uid); }
    interface OnVisibleResultsChanged { void onChanged(int visibleResultCount); }

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

    private final List<DocumentSnapshot> allData = new ArrayList<>();
    private final List<DocumentSnapshot> data = new ArrayList<>();
    private final Set<String> resultUids = new HashSet<>();
    private final OnAddClick listener;
    private final OnVisibleResultsChanged resultsChangedListener;
    private boolean onlineOnly;
    private Set<String> friendUids = new HashSet<>();
    private final Map<String,String> presCache = new HashMap<>();
    private final Map<String,Long> hbCache = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String,Runnable> presenceTimeouts = new HashMap<>();
    private static final class RtdbSub { final DatabaseReference ref; final ValueEventListener l; RtdbSub(DatabaseReference r, ValueEventListener l){this.ref=r;this.l=l;}}
    private final Map<String,RtdbSub> presSubs = new HashMap<>();

    UserSearchAdapter(OnAddClick listener, OnVisibleResultsChanged resultsChangedListener) {
        this.listener = listener;
        this.resultsChangedListener = resultsChangedListener;
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

    int addResults(List<DocumentSnapshot> docs) {
        int addedCount = 0;
        for (DocumentSnapshot doc : docs) {
            if (resultUids.add(doc.getId())) {
                allData.add(doc);
                subscribeToPresence(doc.getId());
                addedCount++;
            }
        }
        refreshVisibleData();
        return addedCount;
    }

    void setOnlineOnly(boolean onlineOnly) {
        if (this.onlineOnly == onlineOnly) return;
        this.onlineOnly = onlineOnly;
        refreshVisibleData();
    }

    static boolean shouldShowPresence(boolean onlineOnly, String state) {
        return !onlineOnly || "online".equals(state) || "active".equals(state);
    }

    private void refreshVisibleData() {
        data.clear();
        for (DocumentSnapshot doc : allData) {
            if (shouldShowPresence(onlineOnly, presCache.get(doc.getId()))) {
                data.add(doc);
            }
        }
        notifyDataSetChanged();
        resultsChangedListener.onChanged(data.size());
    }

    private void subscribeToPresence(@NonNull String uid) {
        if (presSubs.containsKey(uid)) return;
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        database.goOnline();
        DatabaseReference ref = database.getReference("status").child(uid);
        Log.d(TAG, "UserSearchAdapter.presence: subscribing uid=" + uid
                + ", path=" + ref.toString());

        Runnable timeout = () -> {
            presenceTimeouts.remove(uid);
            if (presCache.containsKey(uid)) return;
            Log.w(TAG, "UserSearchAdapter.presence: timed out uid=" + uid
                    + ", defaulting to offline");
            updatePresence(uid, "offline", 0L);
        };
        presenceTimeouts.put(uid, timeout);
        mainHandler.postDelayed(timeout, PRESENCE_TIMEOUT_MS);

        ValueEventListener listener = new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String state = "offline";
                long lastHeartbeat = 0L;
                if (snap.exists()) {
                    String stateValue = snap.child("state").getValue(String.class);
                    Long heartbeatValue = snap.child("last_heartbeat").getValue(Long.class);
                    if (heartbeatValue != null) {
                        lastHeartbeat = heartbeatValue;
                    } else {
                        Double heartbeatDouble = snap.child("last_heartbeat").getValue(Double.class);
                        if (heartbeatDouble != null) lastHeartbeat = heartbeatDouble.longValue();
                    }
                    if ("online".equals(stateValue)) {
                        state = "online";
                    } else if (lastHeartbeat > 0L) {
                        state = "active";
                    }
                }
                cancelPresenceTimeout(uid);
                Log.d(TAG, "UserSearchAdapter.presence: received uid=" + uid
                        + ", exists=" + snap.exists()
                        + ", state=" + state
                        + ", lastHeartbeat=" + lastHeartbeat);
                updatePresence(uid, state, lastHeartbeat);
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                cancelPresenceTimeout(uid);
                Log.e(TAG, "UserSearchAdapter.presence: cancelled uid=" + uid
                        + ", code=" + error.getCode()
                        + ", message=" + error.getMessage(), error.toException());
                updatePresence(uid, "offline", 0L);
            }
        };
        ref.addValueEventListener(listener);
        presSubs.put(uid, new RtdbSub(ref, listener));
    }

    private void updatePresence(@NonNull String uid, @NonNull String state, long lastHeartbeat) {
        presCache.put(uid, state);
        hbCache.put(uid, lastHeartbeat);
        if (onlineOnly) {
            refreshVisibleData();
        } else {
            int index = indexForUid(uid);
            if (index != RecyclerView.NO_POSITION) notifyItemChanged(index, "presence");
        }
    }

    private void cancelPresenceTimeout(@NonNull String uid) {
        Runnable timeout = presenceTimeouts.remove(uid);
        if (timeout != null) mainHandler.removeCallbacks(timeout);
    }

    void clear() {
        // Clean up presence subscriptions when clearing data
        for (RtdbSub sub : presSubs.values()) {
            sub.ref.removeEventListener(sub.l);
        }
        presSubs.clear();
        for (Runnable timeout : presenceTimeouts.values()) {
            mainHandler.removeCallbacks(timeout);
        }
        presenceTimeouts.clear();
        presCache.clear();
        hbCache.clear();
        resultUids.clear();
        allData.clear();
        data.clear();
        notifyDataSetChanged();
        resultsChangedListener.onChanged(0);
    }
    
    void setFriendUids(Set<String> friendUids) {
        this.friendUids = new HashSet<>(friendUids);
        notifyDataSetChanged(); // Refresh all items to update button states
    }
    
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        super.onAttachedToRecyclerView(rv);
        for (DocumentSnapshot doc : allData) subscribeToPresence(doc.getId());
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        for (RtdbSub sub : presSubs.values()) {
            sub.ref.removeEventListener(sub.l);
        }
        presSubs.clear();
        for (Runnable timeout : presenceTimeouts.values()) {
            mainHandler.removeCallbacks(timeout);
        }
        presenceTimeouts.clear();
    }
}
