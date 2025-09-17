package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public class AddFriendActivity extends BaseActivity {

    EditText nicknameInput;
    Button searchButton;
    Button loadMoreButton;
    RecyclerView resultsList;
    TextView resultText;

    private UserSearchAdapter adapter;
    private DocumentSnapshot lastVisible;
    private String currentQuery;
    private String originalQuery;
    private boolean fallbackMode;
    private Set<String> friendUids = new HashSet<>();

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        Toolbar toolbar = findViewById(R.id.add_friend_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.add_a_friend);

        nicknameInput = findViewById(R.id.nicknameInput);
        searchButton = findViewById(R.id.searchButton);
        loadMoreButton = findViewById(R.id.loadMoreButton);
        resultsList = findViewById(R.id.searchResults);
        resultText = findViewById(R.id.addFriendResult);

        resultsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserSearchAdapter(this::searchAndAdd);
        resultsList.setAdapter(adapter);

        searchButton.setEnabled(false);
        nicknameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                searchButton.setEnabled(s.length() >= 1);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchButton.setOnClickListener(v -> {
            String query = nicknameInput.getText().toString().trim();
            if (query.length() < 1) return;
            adapter.clear();
            lastVisible = null;

            originalQuery = query;
            currentQuery = query.toLowerCase();
            fallbackMode = false;

            searchPage();
        });

        loadMoreButton.setOnClickListener(v -> searchPage());

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Load friends list to determine which users already are friends
        loadFriends();
    }
    
    private void loadFriends() {
        String uid = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("users").document(uid).collection("friends").get()
                .addOnSuccessListener(snap -> {
                    Set<String> newFriendUids = new HashSet<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        newFriendUids.add(doc.getId());
                    }
                    friendUids = newFriendUids;
                    adapter.setFriendUids(friendUids);
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + ".loadFriends: Failed to load friends", e);
                    // Continue with empty friends list
                    friendUids = new HashSet<>();
                    adapter.setFriendUids(friendUids);
                });
    }

    private void searchPage() {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        Query q;
        if (fallbackMode) {
            // Fallback mode: fetch all users ordered by nickname for client-side filtering
            q = db.collection("users")
                    .orderBy("nickname")
                    .limit(50); // Increased limit for client-side filtering
        } else {
            // Primary mode: fetch all users ordered by nicknameLowercase for client-side filtering
            q = db.collection("users")
                    .orderBy("nicknameLowercase")
                    .limit(50); // Increased limit for client-side filtering
        }

        if (lastVisible != null) q = q.startAfter(lastVisible);

        q.get()
                .addOnSuccessListener(snap -> {
                    java.util.List<DocumentSnapshot> docsAll = snap.getDocuments();
                    java.util.List<DocumentSnapshot> docs = new ArrayList<>();
                    for (DocumentSnapshot d : docsAll) {
                        // Filter out deleted accounts
                        Boolean accountDeleted = d.getBoolean("accountDeleted");
                        if (accountDeleted != null && accountDeleted) {
                            continue;
                        }
                        
                        // Filter out current user - users shouldn't see themselves in search results
                        if (d.getId().equals(currentUserId)) {
                            continue;
                        }
                        
                        // Client-side filtering: check if nickname contains the search query anywhere
                        String nickname = fallbackMode ? d.getString("nickname") : d.getString("nicknameLowercase");
                        if (nickname != null && nickname.toLowerCase().contains(currentQuery)) {
                            docs.add(d);
                        }
                    }
                    adapter.addResults(docs);

                    if (docs.isEmpty() && !fallbackMode && lastVisible == null) {
                        fallbackMode = true;
                        searchPage();
                        return;
                    }

                    if (docsAll.size() == 50) { // Updated to match new limit
                        lastVisible = docsAll.get(docsAll.size() - 1);
                        loadMoreButton.setVisibility(View.VISIBLE);
                    } else {
                        loadMoreButton.setVisibility(View.GONE);
                    }

                    if (adapter.getItemCount() == 0) {
                        resultText.setText(R.string.user_not_found);
                    } else {
                        resultText.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    resultText.setText(R.string.error_searching_user);
                    Log.e("TAG_Soccer", getClass().getSimpleName() + ".searchPage: User lookup failed", e);
                });
    }

    private void searchAndAdd(String uid) {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        if (uid.equals(currentUserId)) {
            resultText.setText(R.string.you_can_t_invite_yourself);
            return;
        }
        sendAddFriendViaCF(currentUserId, uid);
    }

    private void sendAddFriendViaCF(@NonNull String userId, @NonNull String friendId) {
        Map<String,Object> data = new java.util.HashMap<>();
        data.put("userId", userId);
        data.put("friendId", friendId);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("addFriend")
                .call(data)
                .addOnSuccessListener(res -> {
                    resultText.setText(R.string.add_friend);
                    // Add the friend to our local set and update the adapter
                    friendUids.add(friendId);
                    adapter.setFriendUids(friendUids);
                })
                .addOnFailureListener(e -> {
                    String text = getString(R.string.failed_to_add_friend);
                    if (e instanceof FirebaseFunctionsException ffe) {
                        String reason = ffe.getMessage();
                        Log.e("TAG_Soccer", "addFriend failed: " + reason, ffe);
                        if (reason != null && !reason.isEmpty()) {
                            text = getString(R.string.failed_to_add_friend_reason, reason);
                        }
                    } else {
                        Log.e("TAG_Soccer", "addFriend failed", e);
                    }
                    resultText.setText(text);
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
