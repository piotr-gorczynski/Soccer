package piotr_gorczynski.soccer2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PastInviteAdapter extends RecyclerView.Adapter<PastInviteAdapter.VH> {

    public interface OnInviteClick { void onInvite(String uid, String tournamentId, String matchPath, String tournamentName); }
    public interface OnAddFriendClick { void onAddFriend(String uid); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView inviteReceivedAndStatus;
        final TextView nickname;
        final TextView tournamentName;
        final TextView presence;
        final Button sendInviteBtn;
        final Button addFriendBtn;
        String uid;
        DocumentSnapshot doc;
        
        VH(@NonNull View v) {
            super(v);
            inviteReceivedAndStatus = v.findViewById(R.id.inviteReceivedAndStatus);
            nickname = v.findViewById(R.id.nickname);
            tournamentName = v.findViewById(R.id.tournamentName);
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
    private final Map<String,String> tournamentNameCache = new HashMap<>();
    private final Map<String,String> tournamentStatusCache = new HashMap<>();
    private final Map<String,String> matchStatusCache = new HashMap<>();
    private final Map<String,String> matchWinnerCache = new HashMap<>();
    private final Map<String,String> winnerNicknameCache = new HashMap<>();
    private static final class RtdbSub { final DatabaseReference ref; final ValueEventListener l; RtdbSub(DatabaseReference r, ValueEventListener l){this.ref=r;this.l=l;}}
    private final Map<String,RtdbSub> presSubs = new HashMap<>();
    private Set<String> friendUids = new HashSet<>();

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
            if (h.uid == null || h.doc == null) {
                return;
            }
            
            String uid = h.uid;
            String tournamentId = h.doc.getString("tournamentId");
            String matchPath = h.doc.getString("matchPath");
            String tournamentName = tournamentId != null ? tournamentNameCache.get(tournamentId) : null;
            
            // Check if tournament has ended
            if (tournamentId != null && !tournamentId.isEmpty()) {
                String tournamentStatus = tournamentStatusCache.get(tournamentId);
                if (tournamentStatus != null && !"running".equals(tournamentStatus)) {
                    String displayName = tournamentName != null ? tournamentName : "Tournament";
                    String msg = SafeStringFormatter.safeGetString(context, R.string.tournament_not_running, displayName);
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            // Check if match is completed
            if (matchPath != null && !matchPath.isEmpty()) {
                String matchStatus = matchStatusCache.get(matchPath);
                if ("completed".equals(matchStatus)) {
                    String winnerId = matchWinnerCache.get(matchPath);
                    String winnerNickname = winnerId != null ? winnerNicknameCache.get(winnerId) : null;
                    if (winnerNickname == null) {
                        winnerNickname = "Player";
                    }
                    String msg = SafeStringFormatter.safeGetString(context, 
                        R.string.tournament_match_already_completed, winnerNickname);
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            
            inviteListener.onInvite(uid, tournamentId, matchPath, tournamentName);
        });
        h.addFriendBtn.setOnClickListener(btn -> {
            if (h.uid == null) {
                return;
            }
            if (friendUids.contains(h.uid)) {
                Toast.makeText(context, R.string.already_friends, Toast.LENGTH_SHORT).show();
                return;
            }
            addFriendListener.onAddFriend(h.uid);
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
                        h.nickname.setText(SafeStringFormatter.safeGetString(context, R.string.invite_from_format, nick));
                    }
                } else if ("presence".equals(payload)) {
                    String pState = presCache.get(uid);
                    if (pState != null) bindPresence(h, uid, pState);
                } else if ("tournament".equals(payload)) {
                    DocumentSnapshot doc = h.doc;
                    if (doc != null) {
                        String tournamentId = doc.getString("tournamentId");
                        if (tournamentId != null) {
                            String name = tournamentNameCache.get(tournamentId);
                            if (name != null) {
                                h.tournamentName.setText(SafeStringFormatter.safeGetString(context, R.string.tournament_name_format, name));
                                h.tournamentName.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                } else if ("tournamentStatus".equals(payload) || "matchStatus".equals(payload)) {
                    updateButtonState(h);
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
            h.inviteReceivedAndStatus.setText("");
            h.nickname.setText(context.getString(R.string.invite_from_loading));
            h.tournamentName.setVisibility(View.GONE);
            h.presence.setText("");
            // Keep buttons enabled to allow click listeners to handle validation
            h.sendInviteBtn.setEnabled(true);
            h.sendInviteBtn.setAlpha(0.3f);
            h.addFriendBtn.setEnabled(true);
            h.addFriendBtn.setAlpha(0.3f);
            h.addFriendBtn.setText(R.string.add_friend_label);
            return;
        }

        // Handle tournament name and status display
        String tournamentId = d.getString("tournamentId");
        if (tournamentId != null && !tournamentId.isEmpty()) {
            String cachedTournamentName = tournamentNameCache.get(tournamentId);
            if (cachedTournamentName != null) {
                h.tournamentName.setText(SafeStringFormatter.safeGetString(context, R.string.tournament_name_format, cachedTournamentName));
                h.tournamentName.setVisibility(View.VISIBLE);
            } else {
                h.tournamentName.setVisibility(View.GONE);
            }
            
            // Fetch tournament name and status if either is not cached
            // We use OR here to make a single API call if either value is missing,
            // then selectively update only the missing values in the callback
            String cachedTournamentStatus = tournamentStatusCache.get(tournamentId);
            if (cachedTournamentName == null || cachedTournamentStatus == null) {
                FirebaseFirestore.getInstance().collection("tournaments").document(tournamentId).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String name = doc.getString("name");
                                if (name != null && cachedTournamentName == null) {
                                    tournamentNameCache.put(tournamentId, name);
                                    notifyTournamentChanged(tournamentId);
                                }
                                String status = doc.getString("status");
                                if (status != null && cachedTournamentStatus == null) {
                                    tournamentStatusCache.put(tournamentId, status);
                                    notifyTournamentStatusChanged(tournamentId);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.w("TAG_Soccer", "Failed to load tournament data for " + tournamentId, e);
                        });
            }
        } else {
            h.tournamentName.setVisibility(View.GONE);
        }
        
        // Display when the invite was received (createdAt timestamp) and status combined
        com.google.firebase.Timestamp createdAt = d.getTimestamp("createdAt");
        String relativeTime = "";
        if (createdAt != null) {
            long createdAtMillis = createdAt.toDate().getTime();
            relativeTime = MatchAdapter.englishRelative(createdAtMillis);
        }
        
        // Get invite status
        String status = d.getString("status");
        String statusText = "";
        if (status != null) {
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
        }
        
        // Combine received time and status into one line
        if (!relativeTime.isEmpty() && !statusText.isEmpty()) {
            h.inviteReceivedAndStatus.setText(SafeStringFormatter.safeGetString(context, R.string.invite_received_and_status, relativeTime, statusText));
        } else if (!relativeTime.isEmpty()) {
            h.inviteReceivedAndStatus.setText(SafeStringFormatter.safeGetString(context, R.string.invite_received_format, relativeTime));
        } else if (!statusText.isEmpty()) {
            h.inviteReceivedAndStatus.setText(statusText);
        } else {
            h.inviteReceivedAndStatus.setText("");
        }
        
        // Fetch match status if matchPath is present
        String matchPath = d.getString("matchPath");
        if (matchPath != null && !matchPath.isEmpty()) {
            String cachedMatchStatus = matchStatusCache.get(matchPath);
            if (cachedMatchStatus == null) {
                FirebaseFirestore.getInstance().document(matchPath).get()
                        .addOnSuccessListener(doc -> {
                            if (doc.exists()) {
                                String matchStatus = doc.getString("status");
                                if (matchStatus != null) {
                                    matchStatusCache.put(matchPath, matchStatus);
                                    if ("completed".equals(matchStatus)) {
                                        String winnerId = doc.getString("winner");
                                        if (winnerId != null) {
                                            matchWinnerCache.put(matchPath, winnerId);
                                            // Fetch winner nickname and cache it if not already cached
                                            if (!winnerNicknameCache.containsKey(winnerId)) {
                                                FirebaseFirestore.getInstance().collection("users").document(winnerId).get()
                                                    .addOnSuccessListener(userDoc -> {
                                                        if (userDoc.exists()) {
                                                            String nickname = userDoc.getString("nickname");
                                                            if (nickname != null && !nickname.isEmpty()) {
                                                                winnerNicknameCache.put(winnerId, nickname);
                                                            }
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        android.util.Log.w("TAG_Soccer", "Failed to load winner nickname for " + winnerId, e);
                                                    });
                                            }
                                        }
                                    }
                                    notifyMatchStatusChanged(matchPath);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.w("TAG_Soccer", "Failed to load match status for " + matchPath, e);
                        });
            }
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
            h.nickname.setText(SafeStringFormatter.safeGetString(context, R.string.invite_from_format, nick));
        }

        // Load presence/heartbeat info
        String pState = presCache.get(uid);
        if (pState == null) {
            h.presence.setText("…");
            // Update button state even before presence data is loaded
            boolean alreadyFriend = friendUids.contains(uid);
            // Keep button enabled to allow click listener to show toast
            h.addFriendBtn.setEnabled(true);
            h.addFriendBtn.setAlpha(alreadyFriend ? 0.3f : 1f);
            
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
        String username = nickCache.get(uid);
        if (username == null) {
            username = "User"; // Fallback if username not loaded yet
        }
        
        int colour = switch (state) {
            case "online" -> {
                label = SafeStringFormatter.safeGetString(context, R.string.presence_user_online, username);
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGreenDark);
            }
            case "active" -> {
                long last = hbCache.getOrDefault(uid,0L);
                label = SafeStringFormatter.safeGetString(context, R.string.presence_user_last_seen, username, MatchAdapter.englishRelative(last));
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGreenDark);
            }
            default -> {
                label = SafeStringFormatter.safeGetString(context, R.string.presence_user_offline, username);
                yield ContextCompat.getColor(h.itemView.getContext(), R.color.colorGrey);
            }
        };
        h.presence.setText(label);
        h.presence.setTextColor(colour);
        
        updateButtonState(h);
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

    private void notifyTournamentChanged(@NonNull String tournamentId) {
        for (int i = 0; i < docs.size(); i++) {
            String docTournamentId = docs.get(i).getString("tournamentId");
            if (tournamentId.equals(docTournamentId)) {
                notifyItemChanged(i, "tournament");
            }
        }
    }

    private void notifyTournamentStatusChanged(@NonNull String tournamentId) {
        for (int i = 0; i < docs.size(); i++) {
            String docTournamentId = docs.get(i).getString("tournamentId");
            if (tournamentId.equals(docTournamentId)) {
                notifyItemChanged(i, "tournamentStatus");
            }
        }
    }

    private void notifyMatchStatusChanged(@NonNull String matchPath) {
        for (int i = 0; i < docs.size(); i++) {
            String docMatchPath = docs.get(i).getString("matchPath");
            if (matchPath.equals(docMatchPath)) {
                notifyItemChanged(i, "matchStatus");
            }
        }
    }

    private void updateButtonState(@NonNull VH h) {
        if (h.uid == null || h.doc == null) {
            return;
        }

        String uid = h.uid;
        String presenceState = presCache.get(uid);

        // Check user status
        Boolean isDeleted = userDeletedCache.get(uid);
        boolean userDeleted = isDeleted != null && isDeleted;

        // Buttons are disabled if user is deleted OR if user is offline (no heartbeat data)
        long lastHeartbeat = hbCache.getOrDefault(uid, 0L);
        boolean isTrulyOffline = "offline".equalsIgnoreCase(presenceState) && lastHeartbeat == 0L;

        boolean userEnabled = !userDeleted && !isTrulyOffline;

        // Check tournament and match status
        boolean tournamentEnded = false;
        boolean matchCompleted = false;

        String tournamentId = h.doc.getString("tournamentId");
        if (tournamentId != null && !tournamentId.isEmpty()) {
            String tournamentStatus = tournamentStatusCache.get(tournamentId);
            if (tournamentStatus != null && !"running".equals(tournamentStatus)) {
                tournamentEnded = true;
            }
        }

        String matchPath = h.doc.getString("matchPath");
        if (matchPath != null && !matchPath.isEmpty()) {
            String matchStatus = matchStatusCache.get(matchPath);
            if ("completed".equals(matchStatus)) {
                matchCompleted = true;
            }
        }

        // Invite button: Keep enabled so click listener can show toasts, but adjust visual appearance
        boolean inviteEnabled = userEnabled && !tournamentEnded && !matchCompleted;
        // Always keep button enabled to allow click listener to fire and show toasts
        h.sendInviteBtn.setEnabled(true);
        h.sendInviteBtn.setAlpha(inviteEnabled ? 1f : 0.3f);

        // Add friend button logic: Keep enabled so click listener can show toasts
        boolean alreadyFriend = friendUids.contains(uid);
        boolean canAddFriend = userEnabled && !alreadyFriend;
        // Always keep button enabled to allow click listener to fire and show toasts
        h.addFriendBtn.setEnabled(true);
        h.addFriendBtn.setAlpha(canAddFriend ? 1f : 0.3f);
        h.addFriendBtn.setText(context.getString(R.string.add_friend_label));
    }

    void setData(@NonNull List<DocumentSnapshot> invites) {
        docs.clear();
        docs.addAll(invites);
        notifyDataSetChanged();

        // Log what's actually in the adapter after setData
        android.util.Log.d("TAG_Soccer", "PastInviteAdapter.setData: Adapter now contains " + docs.size() + " items");
        for (int i = 0; i < docs.size(); i++) {
            android.util.Log.d("TAG_Soccer", "PastInviteAdapter.setData: [" + i + "] ID: " + docs.get(i).getId());
        }
    }

    void setFriendUids(@NonNull Set<String> friendUids) {
        this.friendUids = new HashSet<>(friendUids);
        notifyDataSetChanged();
    }

    void addFriendUid(@NonNull String friendUid) {
        if (!this.friendUids.add(friendUid)) {
            return;
        }
        for (int i = 0; i < docs.size(); i++) {
            String fromUid = docs.get(i).getString("from");
            if (friendUid.equals(fromUid)) {
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        for (RtdbSub sub : presSubs.values()) sub.ref.removeEventListener(sub.l);
        presSubs.clear();
    }
}
