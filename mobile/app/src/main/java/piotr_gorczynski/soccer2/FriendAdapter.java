package piotr_gorczynski.soccer2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    public interface OnRemoveClick { void onRemove(String uid); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView nickname;
        final TextView presence;
        final Button inviteBtn;
        final Button removeBtn;
        final TextView inviteStats;
        final TextView matchStats;
        String uid;
        DocumentSnapshot doc; // not used but kept for parity
        VH(@NonNull View v) {
            super(v);
            nickname = v.findViewById(R.id.nickname);
            presence = v.findViewById(R.id.presence);
            inviteBtn = v.findViewById(R.id.inviteBtn);
            removeBtn = v.findViewById(R.id.removeBtn);
            inviteStats = v.findViewById(R.id.inviteStats);
            matchStats = v.findViewById(R.id.matchStats);
        }
    }

    private final Context context;
    private final OnInviteClick listener;
    private final OnRemoveClick removeListener;
    private final List<DocumentSnapshot> docs = new ArrayList<>();
    private final Map<String,String> nickCache = new HashMap<>();
    private final Map<String,String> presCache = new HashMap<>();
    private final Map<String,Long> hbCache = new HashMap<>();
    private static final class RtdbSub { final DatabaseReference ref; final ValueEventListener l; RtdbSub(DatabaseReference r, ValueEventListener l){this.ref=r;this.l=l;}}
    private final Map<String,RtdbSub> presSubs = new HashMap<>();
    private final Map<String,String> inviteStatsCache = new HashMap<>();
    private final Map<String,String> matchStatsCache = new HashMap<>();
    private String currentUserId;

    FriendAdapter(Context context, OnInviteClick listener, OnRemoveClick removeListener, String currentUserId) {
        this.context = context;
        this.listener = listener;
        this.removeListener = removeListener;
        this.currentUserId = currentUserId;
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
        h.removeBtn.setOnClickListener(btn -> {
            if (h.uid != null) removeListener.onRemove(h.uid);
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
                    if (nick != null) h.nickname.setText(nick);
                } else if ("presence".equals(payload)) {
                    String pState = presCache.get(uid);
                    if (pState != null) bindPresence(h, uid, pState);
                } else if ("inviteStats".equals(payload)) {
                    String stats = inviteStatsCache.get(uid);
                    if (stats != null) h.inviteStats.setText(stats);
                } else if ("matchStats".equals(payload)) {
                    String stats = matchStatsCache.get(uid);
                    if (stats != null) h.matchStats.setText(stats);
                }
            }
        }
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
            presSubs.put(uid, new RtdbSub(ref,l));
        } else {
            bindPresence(h, uid, pState);
        }

        // Fetch and display invitation statistics
        String cachedStats = inviteStatsCache.get(uid);
        if (cachedStats == null) {
            h.inviteStats.setText(context.getString(R.string.loading_invite_stats));
            fetchInviteStats(uid, h);
        } else {
            h.inviteStats.setText(cachedStats);
        }
        
        // Fetch and display match statistics
        String cachedMatchStats = matchStatsCache.get(uid);
        if (cachedMatchStats == null) {
            h.matchStats.setText("");
            fetchMatchStats(uid, h);
        } else {
            h.matchStats.setText(cachedMatchStats);
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
        
        // Show invite button for all users except those with no heartbeat data
        long lastHeartbeat = hbCache.getOrDefault(uid, 0L);
        boolean isTrulyOffline = "offline".equalsIgnoreCase(state) && lastHeartbeat == 0L;
        h.inviteBtn.setVisibility(isTrulyOffline ? View.GONE : View.VISIBLE);
        h.removeBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() { return docs.size(); }

    private int indexForUid(@NonNull String uid) {
        for (int i=0;i<docs.size();i++) if (uid.equals(docs.get(i).getId())) return i; return -1;
    }

    @Nullable
    Long getCachedHeartbeatFor(@NonNull String uid) {
        return hbCache.get(uid);
    }

    void setData(List<DocumentSnapshot> friends) {
        android.util.Log.d("TAG_Soccer", "setData: Updating adapter with " + friends.size() + " friends");
        
        // Log first few friend UIDs for debugging
        if (!friends.isEmpty()) {
            StringBuilder friendIds = new StringBuilder("setData: Friend UIDs: ");
            for (int i = 0; i < Math.min(5, friends.size()); i++) {
                friendIds.append(friends.get(i).getId());
                if (i < Math.min(4, friends.size() - 1)) {
                    friendIds.append(", ");
                }
            }
            if (friends.size() > 5) {
                friendIds.append("...");
            }
            android.util.Log.d("TAG_Soccer", friendIds.toString());
        }
        
        docs.clear();
        docs.addAll(friends);
        notifyDataSetChanged();
        
        android.util.Log.d("TAG_Soccer", "setData: notifyDataSetChanged() called, adapter now has " + docs.size() + " items");
    }

    private void fetchInviteStats(@NonNull String targetUid, @NonNull VH h) {
        if (currentUserId == null) {
            h.inviteStats.setText("");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Query for invites sent FROM current user TO friend
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> sentTask = 
                db.collection("invitations")
                .whereEqualTo("from", currentUserId)
                .whereEqualTo("to", targetUid)
                .get();
        
        // Query for invites received FROM friend TO current user
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> receivedTask = 
                db.collection("invitations")
                .whereEqualTo("from", targetUid)
                .whereEqualTo("to", currentUserId)
                .get();
        
        // Wait for both queries to complete
        com.google.android.gms.tasks.Tasks.whenAllSuccess(sentTask, receivedTask)
                .addOnSuccessListener(results -> {
                    // Process sent invites
                    com.google.firebase.firestore.QuerySnapshot sentSnapshot = 
                            (com.google.firebase.firestore.QuerySnapshot) results.get(0);
                    int totalSent = sentSnapshot.size();
                    int totalSentAccepted = 0;
                    
                    for (DocumentSnapshot doc : sentSnapshot) {
                        String status = doc.getString("status");
                        if ("accepted".equals(status)) {
                            totalSentAccepted++;
                        }
                    }
                    
                    // Process received invites
                    com.google.firebase.firestore.QuerySnapshot receivedSnapshot = 
                            (com.google.firebase.firestore.QuerySnapshot) results.get(1);
                    int totalReceived = receivedSnapshot.size();
                    int totalReceivedAccepted = 0;
                    
                    for (DocumentSnapshot doc : receivedSnapshot) {
                        String status = doc.getString("status");
                        if ("accepted".equals(status)) {
                            totalReceivedAccepted++;
                        }
                    }
                    
                    // Format: Sent: X (accepted: Y) | Received: Z (accepted: W)
                    String statsText = SafeStringFormatter.safeGetString(context, R.string.invite_stats_format, 
                            totalSent, totalSentAccepted, totalReceived, totalReceivedAccepted);
                    inviteStatsCache.put(targetUid, statsText);
                    
                    int idx = indexForUid(targetUid);
                    if (idx != RecyclerView.NO_POSITION) {
                        notifyItemChanged(idx, "inviteStats");
                    }
                })
                .addOnFailureListener(e -> {
                    String errorText = SafeStringFormatter.safeGetString(context, R.string.invite_stats_format, 0, 0, 0, 0);
                    inviteStatsCache.put(targetUid, errorText);
                    
                    int idx = indexForUid(targetUid);
                    if (idx != RecyclerView.NO_POSITION) {
                        notifyItemChanged(idx, "inviteStats");
                    }
                });
    }

    void invalidateInviteStatsFor(@NonNull String uid) {
        inviteStatsCache.remove(uid);
        int idx = indexForUid(uid);
        if (idx != RecyclerView.NO_POSITION) {
            notifyItemChanged(idx);
        }
    }

    private void fetchMatchStats(@NonNull String targetUid, @NonNull VH h) {
        if (currentUserId == null) {
            h.matchStats.setText("");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Query 1: matches from /matches collection where current user played with friend
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> friendlyMatchesTask = 
                db.collection("matches")
                .whereIn("status", java.util.Arrays.asList("completed"))
                .get();
        
        // Query 2: Get all tournaments to query their matches sub-collections
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> tournamentsTask = 
                db.collection("tournaments")
                .get();
        
        // Wait for both initial queries to complete
        com.google.android.gms.tasks.Tasks.whenAllSuccess(friendlyMatchesTask, tournamentsTask)
                .addOnSuccessListener(results -> {
                    // Process friendly matches
                    com.google.firebase.firestore.QuerySnapshot friendlySnapshot = 
                            (com.google.firebase.firestore.QuerySnapshot) results.get(0);
                    
                    int wins = 0;
                    int losses = 0;
                    
                    // Count wins/losses from friendly matches
                    for (DocumentSnapshot doc : friendlySnapshot) {
                        String player0 = doc.getString("player0");
                        String player1 = doc.getString("player1");
                        String winner = doc.getString("winner");
                        
                        // Check if this match involves both current user and friend
                        boolean isRelevantMatch = 
                            (currentUserId.equals(player0) && targetUid.equals(player1)) ||
                            (currentUserId.equals(player1) && targetUid.equals(player0));
                        
                        if (isRelevantMatch && winner != null) {
                            if (winner.equals(currentUserId)) {
                                wins++;
                            } else if (winner.equals(targetUid)) {
                                losses++;
                            }
                        }
                    }
                    
                    // Process tournament matches
                    com.google.firebase.firestore.QuerySnapshot tournamentsSnapshot = 
                            (com.google.firebase.firestore.QuerySnapshot) results.get(1);
                    
                    // Create a list of tasks to fetch matches from all tournaments
                    java.util.List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> tournamentMatchesTasks = 
                            new java.util.ArrayList<>();
                    
                    for (DocumentSnapshot tournament : tournamentsSnapshot) {
                        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> tournamentMatchesTask = 
                                db.collection("tournaments")
                                .document(tournament.getId())
                                .collection("matches")
                                .whereIn("status", java.util.Arrays.asList("completed", "done"))
                                .get();
                        tournamentMatchesTasks.add(tournamentMatchesTask);
                    }
                    
                    // If there are no tournaments, directly update the stats
                    if (tournamentMatchesTasks.isEmpty()) {
                        String statsText = SafeStringFormatter.safeGetString(context, R.string.match_stats_format, wins, losses);
                        matchStatsCache.put(targetUid, statsText);
                        
                        int idx = indexForUid(targetUid);
                        if (idx != RecyclerView.NO_POSITION) {
                            notifyItemChanged(idx, "matchStats");
                        }
                        return;
                    }
                    
                    // Wait for all tournament matches queries to complete
                    final int finalWins = wins;
                    final int finalLosses = losses;
                    
                    com.google.android.gms.tasks.Tasks.whenAllSuccess(tournamentMatchesTasks)
                            .addOnSuccessListener(tournamentResults -> {
                                int totalWins = finalWins;
                                int totalLosses = finalLosses;
                                
                                // Count wins/losses from tournament matches
                                for (Object result : tournamentResults) {
                                    com.google.firebase.firestore.QuerySnapshot tournamentMatchesSnapshot = 
                                            (com.google.firebase.firestore.QuerySnapshot) result;
                                    
                                    for (DocumentSnapshot doc : tournamentMatchesSnapshot) {
                                        String player0 = doc.getString("player0");
                                        String player1 = doc.getString("player1");
                                        String winner = doc.getString("winner");
                                        
                                        // Check if this match involves both current user and friend
                                        boolean isRelevantMatch = 
                                            (currentUserId.equals(player0) && targetUid.equals(player1)) ||
                                            (currentUserId.equals(player1) && targetUid.equals(player0));
                                        
                                        if (isRelevantMatch && winner != null) {
                                            if (winner.equals(currentUserId)) {
                                                totalWins++;
                                            } else if (winner.equals(targetUid)) {
                                                totalLosses++;
                                            }
                                        }
                                    }
                                }
                                
                                String statsText = SafeStringFormatter.safeGetString(context, R.string.match_stats_format, totalWins, totalLosses);
                                matchStatsCache.put(targetUid, statsText);
                                
                                int idx = indexForUid(targetUid);
                                if (idx != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(idx, "matchStats");
                                }
                            })
                            .addOnFailureListener(e -> {
                                // If tournament queries fail, still show the friendly matches stats
                                String statsText = SafeStringFormatter.safeGetString(context, R.string.match_stats_format, finalWins, finalLosses);
                                matchStatsCache.put(targetUid, statsText);
                                
                                int idx = indexForUid(targetUid);
                                if (idx != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(idx, "matchStats");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    String errorText = SafeStringFormatter.safeGetString(context, R.string.match_stats_format, 0, 0);
                    matchStatsCache.put(targetUid, errorText);
                    
                    int idx = indexForUid(targetUid);
                    if (idx != RecyclerView.NO_POSITION) {
                        notifyItemChanged(idx, "matchStats");
                    }
                });
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        super.onDetachedFromRecyclerView(rv);
        for (RtdbSub sub : presSubs.values()) sub.ref.removeEventListener(sub.l);
        presSubs.clear();
    }
}
