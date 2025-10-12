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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PastInviteAdapter extends RecyclerView.Adapter<PastInviteAdapter.VH> {

    public interface OnInviteClick { void onInvite(String uid); }
    public interface OnAddFriendClick { void onAddFriend(String uid); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView inviteReceivedTime;
        final TextView nickname;
        final TextView inviteStatus;
        final TextView presence;
        final Button sendInviteBtn;
        final Button addFriendBtn;
        String uid;
        DocumentSnapshot doc;
        
        VH(@NonNull View v) {
            super(v);
            inviteReceivedTime = v.findViewById(R.id.inviteReceivedTime);
            nickname = v.findViewById(R.id.nickname);
            inviteStatus = v.findViewById(R.id.inviteStatus);
            presence = v.findViewById(R.id.presence);
            sendInviteBtn = v.findViewById(R.id.sendInviteBtn);
            addFriendBtn = v.findViewById(R.id.addFriendBtn);
        }
    }

    private final Context context;
    private final OnInviteClick inviteListener;
    private final OnAddFriendClick addFriendListener;
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private final Map<String,String> nickCache = new HashMap<>();
    private final Map<String,String> presCache = new HashMap<>();
    private final Map<String,Long> hbCache = new HashMap<>();
    private final Map<String,Boolean> userDeletedCache = new HashMap<>();
    private static final class RtdbSub { final DatabaseReference ref; final ValueEventListener l; RtdbSub(DatabaseReference r, ValueEventListener l){this.ref=r;this.l=l;}}
    private final Map<String,RtdbSub> presSubs = new HashMap<>();

    PastInviteAdapter(Context context, OnInviteClick inviteListener, OnAddFriendClick addFriendListener) {
        this.context = context;
        this.inviteListener = inviteListener;
        this.addFriendListener = addFriendListener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_past_invite, parent, false);
        VH h = new VH(v);
        h.sendInviteBtn.setOnClickListener(btn -> {
            if (h.uid != null) inviteListener.onInvite(h.uid);
        });
        h.addFriendBtn.setOnClickListener(btn -> {
            if (h.uid != null) addFriendListener.onAddFriend(h.uid);
        });
        return h;
    }

    @Override
    public long getItemId(int position) {
        return docs.get(position).getId().hashCode() & 0xffffffffL;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(h, position);
        } else {
            String uid = h.uid;
            if (uid == null) return;
            
            for (Object payload : payloads) {
                if ("nickname".equals(payload)) {
                    String nick = nickCache.get(uid);
                    if (nick != null) {
                        h.nickname.setText(context.getString(R.string.invite_from_format, nick));
                    }
                } else if ("presence".equals(payload)) {
                    String pState = presCache.get(uid);
                    if (pState != null) bindPresence(h, uid, pState);
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DocumentSnapshot d = docs.get(position);
        h.doc = d;
        
        // Get the 'from' uid from the invitation
        String uid = d.getString("from");
        h.uid = uid;

        if (uid == null) {
            h.inviteReceivedTime.setText("");
            h.inviteStatus.setText("");
            h.nickname.setText(context.getString(R.string.invite_from_loading));
            h.presence.setText("");
            h.sendInviteBtn.setEnabled(false);
            h.addFriendBtn.setEnabled(false);
            return;
        }
        
        // Display when the invite was received (createdAt timestamp)
        com.google.firebase.Timestamp createdAt = d.getTimestamp("createdAt");
        if (createdAt != null) {
            long createdAtMillis = createdAt.toDate().getTime();
            String relativeTime = MatchAdapter.englishRelative(createdAtMillis);
            h.inviteReceivedTime.setText(context.getString(R.string.invite_received_format, relativeTime));
        } else {
            h.inviteReceivedTime.setText("");
        }
        
        // Display invite status
        String status = d.getString("status");
        if (status != null) {
            String statusText = "";
            switch (status) {
                case "accepted":
                    statusText = context.getString(R.string.invite_status_accepted);
                    break;
                case "cancelled":
                    statusText = context.getString(R.string.invite_status_cancelled);
                    break;
                case "expired":
                    statusText = context.getString(R.string.invite_status_expired);
                    break;
            }
            h.inviteStatus.setText(statusText);
        }

        // Load nickname
        String nick = nickCache.get(uid);
        if (nick == null) {
            h.nickname.setText(context.getString(R.string.invite_from_loading));
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String n = doc.getString("nickname");
                            if (n != null) {
                                nickCache.put(uid, n);
                                notifyUidChanged(uid, "nickname");
                            }

                            // Check if user is deleted
                            Boolean deleted = doc.getBoolean("deleted");
                            userDeletedCache.put(uid, deleted != null && deleted);
                            notifyUidChanged(uid, "presence");
                        } else {
                            // User document doesn't exist, mark as deleted
                            userDeletedCache.put(uid, true);
                            notifyUidChanged(uid, "presence");
                        }
                    });
        } else {
            h.nickname.setText(context.getString(R.string.invite_from_format, nick));
        }

        // Load presence/heartbeat info
        String pState = presCache.get(uid);
        if (pState == null) {
            h.presence.setText("…");
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(uid);
            ValueEventListener l = new ValueEventListener() {
                @Override public void onDataChange(@NonNull DataSnapshot snap) {
                    if (!snap.exists()) {
                        presCache.put(uid, "offline");
                        hbCache.put(uid, 0L);
                        notifyUidChanged(uid, "presence");
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
                    notifyUidChanged(uid, "presence");
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
                label = context.getString(R.string.presence_online);
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGreenDark);
            }
            case "active" -> {
                long last = hbCache.getOrDefault(uid,0L);
                label = context.getString(R.string.presence_last_seen, MatchAdapter.englishRelative(last));
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGreenDark);
            }
            default -> {
                label = context.getString(R.string.presence_offline);
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGrey);
            }
        };
        h.presence.setText(label);
        h.presence.setTextColor(colour);
        
        // Enable/disable buttons based on user status
        Boolean isDeleted = userDeletedCache.get(uid);
        boolean userDeleted = isDeleted != null && isDeleted;
        
        // Buttons are disabled if user is deleted OR if user is offline (no heartbeat data)
        long lastHeartbeat = hbCache.getOrDefault(uid, 0L);
        boolean isTrulyOffline = "offline".equalsIgnoreCase(state) && lastHeartbeat == 0L;
        
        boolean enabled = !userDeleted && !isTrulyOffline;
        h.sendInviteBtn.setEnabled(enabled);
        h.addFriendBtn.setEnabled(enabled);
    }

    @Override
    public int getItemCount() { return docs.size(); }

    private void notifyUidChanged(@NonNull String uid, @NonNull Object payload) {
        for (int i = 0; i < docs.size(); i++) {
            String fromUid = docs.get(i).getString("from");
            if (uid.equals(fromUid)) {
                notifyItemChanged(i, payload);
            }
        }
    }

    void setData(List<DocumentSnapshot> invites) {
        docs.clear();
        docs.addAll(invites);
        notifyDataSetChanged();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        for (RtdbSub sub : presSubs.values()) sub.ref.removeEventListener(sub.l);
        presSubs.clear();
    }
}
