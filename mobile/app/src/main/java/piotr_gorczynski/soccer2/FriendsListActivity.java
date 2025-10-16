package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class FriendsListActivity extends BaseActivity {

    private static final String TAG = "FriendsListActivity";
    private static final int SORT_BY_LAST_SEEN = 0;
    private static final int SORT_ALPHABETICALLY = 1;

    private RecyclerView list;
    private TextView emptyText;
    private FriendAdapter adapter;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Spinner sortSpinner;
    private int currentSortMode = SORT_BY_LAST_SEEN;  // Default to sort by last seen
    @Nullable
    private String pendingInviteStatsRefreshUid;
    private boolean isLoadingFriends = false;
    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        Log.d(TAG, "onCreate: Initializing FriendsListActivity");

        Toolbar toolbar = findViewById(R.id.friends_list_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.invite_a_friend);

        Button addBtn = findViewById(R.id.addFriendBtn);
        list = findViewById(R.id.friendsList);
        emptyText = findViewById(R.id.emptyFriends);
        sortSpinner = findViewById(R.id.sortSpinner);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        list.setLayoutManager(new LinearLayoutManager(this));
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        adapter = new FriendAdapter(this, this::sendInviteViaCF, this::removeFriend, currentUserId);
        list.setAdapter(adapter);

        // Setup sort spinner
        ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAdapter.add(getString(R.string.sort_by_last_seen));
        spinnerAdapter.add(getString(R.string.sort_alphabetically));
        sortSpinner.setAdapter(spinnerAdapter);
        
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected: position=" + position + ", spinnerInitialized=" + spinnerInitialized);
                currentSortMode = position;
                
                // Only reload friends if this is a user-initiated selection change (not the initial setup)
                if (spinnerInitialized) {
                    Log.d(TAG, "onItemSelected: User changed sort mode to " + 
                        (position == SORT_BY_LAST_SEEN ? "SORT_BY_LAST_SEEN" : "SORT_ALPHABETICALLY"));
                    loadFriends();
                } else {
                    Log.d(TAG, "onItemSelected: Initial spinner setup, marking as initialized");
                    spinnerInitialized = true;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onNothingSelected called");
            }
        });
        
        // Set selection AFTER attaching listener
        // Note: This will trigger onItemSelected, but we skip loading friends during initial setup
        sortSpinner.setSelection(SORT_BY_LAST_SEEN);  // Default to "Sort by last seen"
        Log.d(TAG, "onCreate: Set spinner selection to SORT_BY_LAST_SEEN");

        addBtn.setOnClickListener(v -> startActivity(new Intent(this, AddFriendActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Loading friends with currentSortMode=" + 
            (currentSortMode == SORT_BY_LAST_SEEN ? "SORT_BY_LAST_SEEN" : "SORT_ALPHABETICALLY"));
        loadFriends();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (pendingInviteStatsRefreshUid != null && adapter != null) {
            adapter.invalidateInviteStatsFor(pendingInviteStatsRefreshUid);
            pendingInviteStatsRefreshUid = null;
        }
    }

    private void loadFriends() {
        if (isLoadingFriends) {
            Log.d(TAG, "loadFriends: Already loading, skipping duplicate call");
            return;  // Prevent concurrent loads
        }
        isLoadingFriends = true;
        
        Log.d(TAG, "loadFriends: Starting to load friends with sortMode=" + 
            (currentSortMode == SORT_BY_LAST_SEEN ? "SORT_BY_LAST_SEEN" : "SORT_ALPHABETICALLY"));
        
        String uid = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("users").document(uid).collection("friends").get()
                .addOnSuccessListener(snap -> {
                    isLoadingFriends = false;
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    Log.d(TAG, "loadFriends: Retrieved " + docs.size() + " friends from Firestore");
                    
                    if (docs.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        adapter.setData(new ArrayList<>());
                        Log.d(TAG, "loadFriends: No friends found, showing empty message");
                        return;
                    }

                    emptyText.setVisibility(View.GONE);

                    // Extract friend UIDs
                    List<String> friendUids = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) {
                        friendUids.add(doc.getId());
                    }
                    Log.d(TAG, "loadFriends: Friend UIDs: " + friendUids);

                    if (currentSortMode == SORT_ALPHABETICALLY) {
                        // Sort alphabetically by nickname
                        Log.d(TAG, "loadFriends: Sorting alphabetically");
                        sortByNickname(docs, friendUids);
                    } else {
                        // Sort by last seen (descending)
                        Log.d(TAG, "loadFriends: Sorting by last seen");
                        sortByLastSeen(docs, friendUids);
                    }
                })
                .addOnFailureListener(e -> {
                    isLoadingFriends = false;
                    emptyText.setVisibility(View.VISIBLE);
                    Log.e(TAG, "loadFriends: Failed to load friends", e);
                });
    }

    private void sortByNickname(List<DocumentSnapshot> docs, List<String> friendUids) {
        Log.d(TAG, "sortByNickname: Fetching user documents for " + friendUids.size() + " friends");
        
        // Fetch user documents to get nicknames for sorting
        db.collection("users").whereIn(FieldPath.documentId(), friendUids)
                .get()
                .addOnSuccessListener(userSnap -> {
                    // Create a map of UID to nickname (lowercase for case-insensitive sorting)
                    Map<String, String> nicknameMap = new HashMap<>();
                    for (DocumentSnapshot userDoc : userSnap.getDocuments()) {
                        String nickname = userDoc.getString("nickname");
                        if (nickname != null) {
                            nicknameMap.put(userDoc.getId(), nickname.toLowerCase());
                            Log.d(TAG, "sortByNickname: User " + userDoc.getId() + " has nickname: " + nickname);
                        } else {
                            Log.w(TAG, "sortByNickname: User " + userDoc.getId() + " has no nickname");
                        }
                    }
                    
                    // Sort friend documents by nickname (ascending)
                    Collections.sort(docs, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot d1, DocumentSnapshot d2) {
                            String nick1 = nicknameMap.get(d1.getId());
                            String nick2 = nicknameMap.get(d2.getId());
                            
                            // Handle null nicknames (put them at the end)
                            if (nick1 == null && nick2 == null) return 0;
                            if (nick1 == null) return 1;
                            if (nick2 == null) return -1;
                            
                            return nick1.compareTo(nick2);
                        }
                    });
                    
                    Log.d(TAG, "sortByNickname: Sorted friends alphabetically, updating adapter");
                    emptyText.setVisibility(View.GONE);
                    adapter.setData(docs);
                })
                .addOnFailureListener(e -> {
                    // If fetching user data fails, still show the friends (unsorted)
                    Log.e(TAG, "sortByNickname: Failed to fetch user data for sorting", e);
                    emptyText.setVisibility(View.GONE);
                    adapter.setData(docs);
                });
    }

    private void sortByLastSeen(List<DocumentSnapshot> docs, List<String> friendUids) {
        // Work on a mutable copy so we can safely sort without modifying the Firestore snapshot list
        List<DocumentSnapshot> mutableDocs = new ArrayList<>(docs);

        Log.d(TAG, "sortByLastSeen: Fetching heartbeat data for " + friendUids.size() + " friends");

        if (friendUids.isEmpty()) {
            adapter.setData(mutableDocs);
            return;
        }

        DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference("status");
        statusRef.get()
                .addOnSuccessListener(snapshot -> {
                    Map<String, Long> heartbeatMap = new HashMap<>();
                    for (String friendUid : friendUids) {
                        Long lastHb = 0L;

                        // Try to read the heartbeat as a generic number so we gracefully handle
                        // any schema differences (Long vs Double) that may exist in existing data.
                        Number hbNumber = snapshot.child(friendUid).child("last_heartbeat").getValue(Number.class);
                        if (hbNumber != null) {
                            lastHb = hbNumber.longValue();
                        }

                        // As a fallback, re-use the cached heartbeat value from the adapter if we
                        // have already subscribed to presence updates for this friend.  This keeps
                        // the sorting stable even when the status entry is temporarily missing in
                        // the RTDB snapshot (for example right after a user goes offline).
                        if (lastHb == 0L && adapter != null) {
                            Long cachedHb = adapter.getCachedHeartbeatFor(friendUid);
                            if (cachedHb != null) {
                                lastHb = cachedHb;
                            }
                        }

                        heartbeatMap.put(friendUid, lastHb);
                        Log.d(TAG, "sortByLastSeen: Friend " + friendUid + " has heartbeat: " + lastHb);
                    }

                    Collections.sort(mutableDocs, new Comparator<DocumentSnapshot>() {
                        @Override
                        public int compare(DocumentSnapshot d1, DocumentSnapshot d2) {
                            long hb1 = heartbeatMap.getOrDefault(d1.getId(), 0L);
                            long hb2 = heartbeatMap.getOrDefault(d2.getId(), 0L);

                            int result = Long.compare(hb2, hb1);  // Sort descending (most recent first)
                            if (result != 0) {
                                return result;
                            }

                            // When heartbeats are equal fall back to UID comparison to keep order stable
                            return d1.getId().compareTo(d2.getId());
                        }
                    });

                    Log.d(TAG, "sortByLastSeen: Sorted friends by last seen, updating adapter");
                    // Log the sorted order for debugging
                    StringBuilder sortedOrder = new StringBuilder("sortByLastSeen: Sorted order: ");
                    for (int i = 0; i < Math.min(5, mutableDocs.size()); i++) {
                        String uid = mutableDocs.get(i).getId();
                        Long hb = heartbeatMap.get(uid);
                        sortedOrder.append(uid).append("(").append(hb).append(")");
                        if (i < Math.min(4, mutableDocs.size() - 1)) {
                            sortedOrder.append(", ");
                        }
                    }
                    if (mutableDocs.size() > 5) {
                        sortedOrder.append("...");
                    }
                    Log.d(TAG, sortedOrder.toString());

                    adapter.setData(mutableDocs);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "sortByLastSeen: Failed to fetch heartbeat data", e);
                    adapter.setData(mutableDocs);
                });
    }

    private void sendInviteViaCF(@NonNull String targetUid) {
        java.util.Map<String,Object> data = java.util.Collections.singletonMap("toUid", targetUid);
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("createInvite")
                .call(data)
                .addOnSuccessListener(res -> {
                    @SuppressWarnings("unchecked")
                    String inviteId = (String)((java.util.Map<String,Object>)java.util.Objects.requireNonNull(res.getData())).get("inviteId");
                    if (inviteId != null) {
                        pendingInviteStatsRefreshUid = targetUid;
                        startActivity(new Intent(this, WaitingActivity.class).putExtra("inviteId", inviteId));
                    }
                })
                .addOnFailureListener(e -> {
                    /* default fallback */
                    int msgId = R.string.failed_to_send_invite;
                    String customMessage = null;

                    if (e instanceof com.google.firebase.functions.FirebaseFunctionsException ffe) {
                        com.google.firebase.functions.FirebaseFunctionsException.Code code = ffe.getCode();
                        if (code == com.google.firebase.functions.FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
                            /* Cloud Function puts a short reason in getMessage() */
                            String reason = String.valueOf(ffe.getMessage()); // never null

                            if (reason.contains("blocked invites")) {
                                customMessage = reason; // Use the full message from the server
                            } else {
                                msgId = switch (reason) {
                                    /* inviter already has an unanswered invite */
                                    case "sender_busy" -> R.string.invite_already_sent;
                                    /* target has its own outgoing invite and is waiting */
                                    case "target_busy" -> R.string.target_player_busy;
                                    case "User account no longer available" -> R.string.account_no_longer_available;
                                    default -> R.string.failed_to_send_invite;
                                };
                            }
                        } else if (code == com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED) {
                            msgId = R.string.invite_already_sent;    // inviter cancelled meanwhile
                        }
                    }

                    if (customMessage != null) {
                        android.widget.Toast.makeText(this, customMessage, android.widget.Toast.LENGTH_LONG).show();
                    } else {
                        android.widget.Toast.makeText(this, msgId, android.widget.Toast.LENGTH_LONG).show();
                    }

                    android.util.Log.e("TAG_Soccer", getClass().getSimpleName() + "." + java.util.Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": createInvite failed", e);
                });
    }

    private void removeFriend(@NonNull String uidToRemove) {
        String uid = java.util.Objects.requireNonNull(auth.getCurrentUser()).getUid();
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("userId", uid);
        data.put("friendId", uidToRemove);
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("removeFriend")
                .call(data)
                .addOnSuccessListener(r -> loadFriends());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
