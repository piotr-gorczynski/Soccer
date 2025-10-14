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

class PendingInviteAdapter extends RecyclerView.Adapter<PendingInviteAdapter.VH> {

    interface OnAcceptClick { void onAccept(@NonNull String inviteId); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView inviteReceived;
        final TextView nickname;
        final TextView presence;
        final Button acceptBtn;
        DocumentSnapshot doc;
        String uid;

        VH(@NonNull View v) {
            super(v);
            nickname = v.findViewById(R.id.nickname);
            inviteReceived = v.findViewById(R.id.inviteReceivedAndStatus);
            presence = v.findViewById(R.id.presence);
            acceptBtn = v.findViewById(R.id.acceptInviteBtn);
        }
    }

    private final Context context;
    private final OnAcceptClick acceptListener;
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private final Map<String, String> nickCache = new HashMap<>();
    private final Map<String, String> presCache = new HashMap<>();
    private final Map<String, Long> hbCache = new HashMap<>();
    private final Map<String, Boolean> userDeletedCache = new HashMap<>();

    private static final class RtdbSub {
        final DatabaseReference ref;
        final ValueEventListener listener;
        RtdbSub(DatabaseReference ref, ValueEventListener listener) {
            this.ref = ref;
            this.listener = listener;
        }
    }

    private final Map<String, RtdbSub> presSubs = new HashMap<>();

    PendingInviteAdapter(@NonNull Context context, @NonNull OnAcceptClick acceptListener) {
        this.context = context;
        this.acceptListener = acceptListener;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_invite, parent, false);
        VH holder = new VH(v);
        holder.acceptBtn.setOnClickListener(btn -> {
            DocumentSnapshot doc = holder.doc;
            if (doc == null) {
                return;
            }
            String id = doc.getId();
            if (id != null) {
                acceptListener.onAccept(id);
            }
        });
        return holder;
    }

    @Override
    public long getItemId(int position) {
        return docs.get(position).getId().hashCode() & 0xffffffffL;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }
        String uid = holder.uid;
        if (uid == null) {
            return;
        }
        for (Object payload : payloads) {
            if ("nickname".equals(payload)) {
                String nick = nickCache.get(uid);
                if (nick != null) {
                    holder.nickname.setText(SafeStringFormatter.safeGetString(context, R.string.invite_from_format, nick));
                }
            } else if ("presence".equals(payload)) {
                String state = presCache.get(uid);
                if (state != null) {
                    bindPresence(holder, uid, state);
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        DocumentSnapshot snapshot = docs.get(position);
        holder.doc = snapshot;

        String uid = snapshot.getString("from");
        holder.uid = uid;

        com.google.firebase.Timestamp createdAt = snapshot.getTimestamp("createdAt");
        if (createdAt != null) {
            holder.inviteReceived.setText(context.getString(
                    R.string.invite_received_format,
                    MatchAdapter.englishRelative(createdAt.toDate().getTime())));
        } else {
            holder.inviteReceived.setText("");
        }

        if (uid == null) {
            holder.nickname.setText(context.getString(R.string.invite_from_loading));
            holder.presence.setText("");
            holder.acceptBtn.setEnabled(false);
            return;
        }

        String cachedNick = nickCache.get(uid);
        if (cachedNick == null) {
            holder.nickname.setText(context.getString(R.string.invite_from_loading));
            FirebaseFirestore.getInstance().collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String n = doc.getString("nickname");
                            if (n != null) {
                                nickCache.put(uid, n);
                                notifyUidChanged(uid, "nickname");
                            }
                            Boolean deleted = doc.getBoolean("deleted");
                            userDeletedCache.put(uid, deleted != null && deleted);
                            notifyUidChanged(uid, "presence");
                        } else {
                            userDeletedCache.put(uid, true);
                            notifyUidChanged(uid, "presence");
                        }
                    });
        } else {
            holder.nickname.setText(SafeStringFormatter.safeGetString(context, R.string.invite_from_format, cachedNick));
        }

        String presenceState = presCache.get(uid);
        if (presenceState == null) {
            holder.presence.setText("…");
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(uid);
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
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

                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            };
            ref.addValueEventListener(listener);
            presSubs.put(uid, new RtdbSub(ref, listener));
        } else {
            bindPresence(holder, uid, presenceState);
        }

        holder.acceptBtn.setEnabled(true);
    }

    private void bindPresence(@NonNull VH holder, @NonNull String uid, @NonNull String state) {
        String username = nickCache.get(uid);
        if (username == null) {
            username = "User";
        }

        String label;
        int colour;
        switch (state) {
            case "online":
                label = SafeStringFormatter.safeGetString(context, R.string.presence_user_online, username);
                colour = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorGreenDark);
                break;
            case "active":
                long last = hbCache.getOrDefault(uid, 0L);
                label = SafeStringFormatter.safeGetString(context, R.string.presence_user_last_seen, username, MatchAdapter.englishRelative(last));
                colour = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorGreenDark);
                break;
            default:
                label = SafeStringFormatter.safeGetString(context, R.string.presence_user_offline, username);
                colour = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorGrey);
                break;
        }

        holder.presence.setText(label);
        holder.presence.setTextColor(colour);

        Boolean isDeleted = userDeletedCache.get(uid);
        boolean userDeleted = isDeleted != null && isDeleted;
        boolean enabled = !userDeleted;
        holder.acceptBtn.setEnabled(enabled);
        holder.acceptBtn.setAlpha(enabled ? 1f : 0.3f);
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    private void notifyUidChanged(@NonNull String uid, @NonNull Object payload) {
        for (int i = 0; i < docs.size(); i++) {
            String fromUid = docs.get(i).getString("from");
            if (uid.equals(fromUid)) {
                notifyItemChanged(i, payload);
            }
        }
    }

    void setData(@NonNull List<DocumentSnapshot> invites) {
        removePresenceSubscriptions();
        docs.clear();
        docs.addAll(invites);
        notifyDataSetChanged();
    }

    void clear() {
        removePresenceSubscriptions();
        docs.clear();
        notifyDataSetChanged();
    }

    void removeInviteById(@NonNull String inviteId) {
        for (int i = 0; i < docs.size(); i++) {
            DocumentSnapshot snapshot = docs.get(i);
            if (!inviteId.equals(snapshot.getId())) {
                continue;
            }
            docs.remove(i);
            notifyItemRemoved(i);
            String uid = snapshot.getString("from");
            if (uid != null) {
                RtdbSub sub = presSubs.remove(uid);
                if (sub != null) {
                    sub.ref.removeEventListener(sub.listener);
                }
                nickCache.remove(uid);
                presCache.remove(uid);
                hbCache.remove(uid);
                userDeletedCache.remove(uid);
            }
            break;
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        removePresenceSubscriptions();
    }

    void removePresenceSubscriptions() {
        for (RtdbSub sub : presSubs.values()) {
            sub.ref.removeEventListener(sub.listener);
        }
        presSubs.clear();
    }
}
