package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

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
        sortSpinner.setSelection(SORT_BY_LAST_SEEN);  // Default to "Sort by last seen"
        
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortMode = position;
                loadFriends();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        addBtn.setOnClickListener(v -> startActivity(new Intent(this, AddFriendActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        String uid = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("users").document(uid).collection("friends").get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    if (docs.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        adapter.setData(new ArrayList<>());
                        return;
                    }
                    
                    // Extract friend UIDs
                    List<String> friendUids = new ArrayList<>();
                    for (DocumentSnapshot doc : docs) {
                        friendUids.add(doc.getId());
                    }
                    
                    if (currentSortMode == SORT_ALPHABETICALLY) {
                        // Sort alphabetically by nickname
                        sortByNickname(docs, friendUids);
                    } else {
                        // Sort by last seen (descending)
                        sortByLastSeen(docs, friendUids);
                    }
                })
                .addOnFailureListener(e -> emptyText.setVisibility(View.VISIBLE));
    }

    private void sortByNickname(List<DocumentSnapshot> docs, List<String> friendUids) {
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
                    
                    emptyText.setVisibility(View.GONE);
                    adapter.setData(docs);
                })
                .addOnFailureListener(e -> {
                    // If fetching user data fails, still show the friends (unsorted)
                    emptyText.setVisibility(View.GONE);
                    adapter.setData(docs);
                });
    }

    private void sortByLastSeen(List<DocumentSnapshot> docs, List<String> friendUids) {
        // Fetch heartbeat data from Firebase Realtime Database
        Map<String, Long> heartbeatMap = new HashMap<>();
        final int[] pendingRequests = {friendUids.size()};
        
        if (friendUids.isEmpty()) {
            emptyText.setVisibility(View.GONE);
            adapter.setData(docs);
            return;
        }
        
        for (String friendUid : friendUids) {
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(friendUid);
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Long lastHb = snapshot.child("last_heartbeat").getValue(Long.class);
                    if (lastHb != null) {
                        heartbeatMap.put(friendUid, lastHb);
                    } else {
                        heartbeatMap.put(friendUid, 0L);
                    }
                    
                    pendingRequests[0]--;
                    if (pendingRequests[0] == 0) {
                        // All requests completed, now sort
                        Collections.sort(docs, new Comparator<DocumentSnapshot>() {
                            @Override
                            public int compare(DocumentSnapshot d1, DocumentSnapshot d2) {
                                Long hb1 = heartbeatMap.get(d1.getId());
                                Long hb2 = heartbeatMap.get(d2.getId());
                                
                                if (hb1 == null) hb1 = 0L;
                                if (hb2 == null) hb2 = 0L;
                                
                                // Sort descending (most recent first)
                                return Long.compare(hb2, hb1);
                            }
                        });
                        
                        emptyText.setVisibility(View.GONE);
                        adapter.setData(docs);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    heartbeatMap.put(friendUid, 0L);
                    pendingRequests[0]--;
                    if (pendingRequests[0] == 0) {
                        // Still sort even if some requests failed
                        Collections.sort(docs, new Comparator<DocumentSnapshot>() {
                            @Override
                            public int compare(DocumentSnapshot d1, DocumentSnapshot d2) {
                                Long hb1 = heartbeatMap.get(d1.getId());
                                Long hb2 = heartbeatMap.get(d2.getId());
                                
                                if (hb1 == null) hb1 = 0L;
                                if (hb2 == null) hb2 = 0L;
                                
                                // Sort descending (most recent first)
                                return Long.compare(hb2, hb1);
                            }
                        });
                        
                        emptyText.setVisibility(View.GONE);
                        adapter.setData(docs);
                    }
                }
            });
        }
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
