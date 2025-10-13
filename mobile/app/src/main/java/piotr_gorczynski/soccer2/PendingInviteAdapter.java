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

    interface OnAcceptClick { void onAccept(@NonNull DocumentSnapshot doc); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView inviteReceivedAndStatus;
        final TextView nickname;
        final TextView presence;
        final Button acceptBtn;
        DocumentSnapshot doc;
        String uid;

        VH(@NonNull View itemView) {
            super(itemView);
            inviteReceivedAndStatus = itemView.findViewById(R.id.inviteReceivedAndStatus);
            nickname = itemView.findViewById(R.id.nickname);
            presence = itemView.findViewById(R.id.presence);
            acceptBtn = itemView.findViewById(R.id.acceptInviteBtn);
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
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_invite, parent, false);
        VH holder = new VH(view);
        holder.acceptBtn.setOnClickListener(v -> {
            DocumentSnapshot doc = holder.doc;
            if (doc != null) {
                acceptListener.onAccept(doc);
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
                    holder.nickname.setText(context.getString(R.string.invite_from_format, nick));
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

        if (uid == null) {
            holder.inviteReceivedAndStatus.setText("");
            holder.nickname.setText(context.getString(R.string.invite_from_loading));
            holder.presence.setText("");
            holder.acceptBtn.setEnabled(false);
            return;
        }

        holder.acceptBtn.setEnabled(true);
        holder.acceptBtn.setAlpha(1f);

        com.google.firebase.Timestamp createdAt = snapshot.getTimestamp("createdAt");
        String relativeTime = "";
        if (createdAt != null) {
            long createdAtMillis = createdAt.toDate().getTime();
            relativeTime = MatchAdapter.englishRelative(createdAtMillis);
        }

        if (!relativeTime.isEmpty()) {
            holder.inviteReceivedAndStatus.setText(context.getString(R.string.invite_received_format, relativeTime));
        } else {
            holder.inviteReceivedAndStatus.setText("");
        }

        String nick = nickCache.get(uid);
        if (nick == null) {
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
            holder.nickname.setText(context.getString(R.string.invite_from_format, nick));
        }

        String state = presCache.get(uid);
        if (state == null) {
            holder.presence.setText("…");
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(uid);
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {
                        presCache.put(uid, "offline");
                        hbCache.put(uid, 0L);
                        notifyUidChanged(uid, "presence");
                        return;
                    }

                    String stateStr = snapshot.child("state").getValue(String.class);
                    Long lastHeartbeatBox = snapshot.child("last_heartbeat").getValue(Long.class);
                    long lastHeartbeat = lastHeartbeatBox != null ? lastHeartbeatBox : 0L;

                    String resolvedState = "offline";
                    if ("online".equals(stateStr)) {
                        resolvedState = "online";
                    } else if (lastHeartbeat > 0L) {
                        resolvedState = "active";
                    }

                    presCache.put(uid, resolvedState);
                    hbCache.put(uid, lastHeartbeat);
                    notifyUidChanged(uid, "presence");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            };
            ref.addValueEventListener(listener);
            presSubs.put(uid, new RtdbSub(ref, listener));
        } else {
            bindPresence(holder, uid, state);
        }
    }

    private void bindPresence(@NonNull VH holder, @NonNull String uid, @NonNull String state) {
        String nick = nickCache.get(uid);
        if (nick == null) {
            nick = "User";
        }

        int colour;
        String label;
        switch (state) {
            case "online":
                label = context.getString(R.string.presence_user_online, nick);
                colour = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorGreenDark);
                break;
            case "active":
                long last = hbCache.getOrDefault(uid, 0L);
                label = context.getString(R.string.presence_user_last_seen, nick, MatchAdapter.englishRelative(last));
                colour = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorGreenDark);
                break;
            default:
                label = context.getString(R.string.presence_user_offline, nick);
                colour = ContextCompat.getColor(holder.itemView.getContext(), R.color.colorGrey);
                break;
        }

        holder.presence.setText(label);
        holder.presence.setTextColor(colour);

        Boolean deleted = userDeletedCache.get(uid);
        boolean userDeleted = deleted != null && deleted;
        holder.acceptBtn.setEnabled(!userDeleted);
        holder.acceptBtn.setAlpha(userDeleted ? 0.3f : 1f);
    }

    @Override
    public int getItemCount() {
        return docs.size();
    }

    void setData(@NonNull List<DocumentSnapshot> invitations) {
        docs.clear();
        docs.addAll(invitations);
        notifyDataSetChanged();
    }

    void removeInvite(@NonNull String inviteId) {
        for (int i = 0; i < docs.size(); i++) {
            if (inviteId.equals(docs.get(i).getId())) {
                docs.remove(i);
                notifyItemRemoved(i);
                break;
            }
        }
    }

    private void notifyUidChanged(@NonNull String uid, @NonNull Object payload) {
        for (int i = 0; i < docs.size(); i++) {
            String fromUid = docs.get(i).getString("from");
            if (uid.equals(fromUid)) {
                notifyItemChanged(i, payload);
            }
        }
    }

    void clearSubscriptions() {
        for (RtdbSub sub : presSubs.values()) {
            sub.ref.removeEventListener(sub.listener);
        }
        presSubs.clear();
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        clearSubscriptions();
    }
}
