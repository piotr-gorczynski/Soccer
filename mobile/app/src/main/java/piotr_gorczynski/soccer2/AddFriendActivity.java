package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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

    private static final String TAG = "TAG_Soccer";
    private static final int PAGE_SIZE = 50;

    EditText nicknameInput;
    Button searchButton;
    Button loadMoreButton;
    CheckBox onlineOnlyCheckbox;
    RecyclerView resultsList;
    TextView resultText;

    private UserSearchAdapter adapter;
    private DocumentSnapshot lastVisible;
    private String currentQuery;
    private String originalQuery;
    private boolean fallbackMode;
    private boolean isPageLoading;
    private boolean hasMoreResults;
    private boolean loadMoreRevealedForCurrentPage;
    private int lastKnownVisibleResultCount;
    private boolean searchFeedbackActive = true;
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
        onlineOnlyCheckbox = findViewById(R.id.onlineOnlyCheckbox);
        resultsList = findViewById(R.id.searchResults);
        resultText = findViewById(R.id.addFriendResult);

        resultsList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserSearchAdapter(this::searchAndAdd, this::onVisibleResultsChanged);
        resultsList.setAdapter(adapter);
        resultsList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                revealLoadMoreIfAtEnd();
            }
        });

        onlineOnlyCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                adapter.setOnlineOnly(isChecked));

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
            searchFeedbackActive = true;
            adapter.clear();
            lastVisible = null;

            originalQuery = query;
            currentQuery = query.toLowerCase();
            fallbackMode = false;
            hasMoreResults = false;
            loadMoreRevealedForCurrentPage = false;

            loadMoreButton.setVisibility(View.GONE);
            searchPage(false, 0);
        });

        loadMoreButton.setOnClickListener(v -> {
            Log.d(TAG, "AddFriendActivity.pagination: Load more tapped"
                    + ", inProgress=" + isPageLoading
                    + ", visibleCount=" + adapter.getItemCount()
                    + ", cursorAvailable=" + (lastVisible != null));
            searchPage(true, 0);
        });

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Load friends list to determine which users already are friends
        loadFriends();
    }

    private void updateEmptyState(int visibleResultCount) {
        if (!searchFeedbackActive) return;
        if (shouldShowEmptyState(visibleResultCount, currentQuery)) {
            resultText.setText(R.string.user_not_found);
        } else {
            resultText.setText("");
        }
    }

    private void onVisibleResultsChanged(int visibleResultCount) {
        if (!isPageLoading && visibleResultCount != lastKnownVisibleResultCount) {
            loadMoreRevealedForCurrentPage = false;
            loadMoreButton.setVisibility(View.GONE);
        }
        lastKnownVisibleResultCount = visibleResultCount;
        updateEmptyState(visibleResultCount);
        resultsList.post(this::revealLoadMoreIfAtEnd);
    }

    static boolean shouldShowEmptyState(int visibleResultCount, String query) {
        return visibleResultCount == 0 && query != null;
    }
    
    private void loadFriends() {
        if (auth.getCurrentUser() == null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".loadFriends: User not authenticated");
            finish();
            return;
        }
        String uid = auth.getCurrentUser().getUid();
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

    private void searchPage(boolean loadMoreRequest, int pagesFetchedForAction) {
        if (isPageLoading) {
            Log.d(TAG, "AddFriendActivity.pagination: Request ignored because another page is loading"
                    + ", loadMore=" + loadMoreRequest
                    + ", visibleCount=" + adapter.getItemCount());
            return;
        }
        isPageLoading = true;
        loadMoreButton.setEnabled(false);
        loadMoreButton.setVisibility(View.GONE);
        fetchPage(loadMoreRequest, pagesFetchedForAction);
    }

    private void fetchPage(boolean loadMoreRequest, int pagesFetchedForAction) {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        boolean hadCursor = lastVisible != null;
        int visibleBefore = adapter.getItemCount();

        Query q;
        if (fallbackMode) {
            // Fallback mode: fetch all users ordered by nickname for client-side filtering
            q = db.collection("users")
                    .orderBy("nickname")
                    .limit(PAGE_SIZE);
        } else {
            // Primary mode: fetch all users ordered by nicknameLowercase for client-side filtering
            q = db.collection("users")
                    .orderBy("nicknameLowercase")
                    .limit(PAGE_SIZE);
        }

        if (lastVisible != null) q = q.startAfter(lastVisible);

        Log.d(TAG, "AddFriendActivity.pagination: Request started"
                + ", loadMore=" + loadMoreRequest
                + ", pageInAction=" + (pagesFetchedForAction + 1)
                + ", fallbackMode=" + fallbackMode
                + ", cursorAvailable=" + hadCursor
                + ", visibleBefore=" + visibleBefore);

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
                        
                        // Filter out users who haven't accepted terms
                        Boolean termsAccepted = d.getBoolean("termsAccepted");
                        if (termsAccepted == null || !termsAccepted) {
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
                    int appendedCount = adapter.addResults(docs);
                    boolean hasMore = docsAll.size() == PAGE_SIZE;

                    if (hasMore) {
                        lastVisible = docsAll.get(docsAll.size() - 1);
                    }

                    Log.d(TAG, "AddFriendActivity.pagination: Request completed"
                            + ", loadMore=" + loadMoreRequest
                            + ", pageInAction=" + (pagesFetchedForAction + 1)
                            + ", returned=" + docsAll.size()
                            + ", matched=" + docs.size()
                            + ", appended=" + appendedCount
                            + ", visibleAfter=" + adapter.getItemCount()
                            + ", hasMore=" + hasMore);

                    if (docs.isEmpty() && !fallbackMode && !hadCursor) {
                        fallbackMode = true;
                        lastVisible = null;
                        Log.d(TAG, "AddFriendActivity.pagination: Switching to fallback nickname field");
                        isPageLoading = false;
                        searchPage(loadMoreRequest, pagesFetchedForAction + 1);
                        return;
                    }

                    if (shouldContinueLoading(loadMoreRequest, appendedCount, hasMore)) {
                        Log.d(TAG, "AddFriendActivity.pagination: No matching results on page; fetching next page for the same tap");
                        isPageLoading = false;
                        searchPage(true, pagesFetchedForAction + 1);
                        return;
                    }

                    finishPageRequest(hasMore);
                    updateEmptyState(adapter.getItemCount());
                })
                .addOnFailureListener(e -> {
                    finishPageRequest(lastVisible != null);
                    resultText.setText(R.string.error_searching_user);
                    Log.e(TAG, "AddFriendActivity.pagination: Request failed"
                            + ", loadMore=" + loadMoreRequest
                            + ", pageInAction=" + (pagesFetchedForAction + 1)
                            + ", cursorPreserved=" + (lastVisible != null)
                            + ", visibleCount=" + adapter.getItemCount(), e);
                });
    }

    private void finishPageRequest(boolean hasMore) {
        isPageLoading = false;
        hasMoreResults = hasMore;
        loadMoreRevealedForCurrentPage = false;
        loadMoreButton.setEnabled(true);
        loadMoreButton.setVisibility(View.GONE);
        Log.d(TAG, "AddFriendActivity.pagination: Action finished"
                + ", visibleCount=" + adapter.getItemCount()
                + ", hasMore=" + hasMore
                + ", loadMoreVisible=false");
        resultsList.post(this::revealLoadMoreIfAtEnd);
    }

    private void revealLoadMoreIfAtEnd() {
        if (loadMoreRevealedForCurrentPage || !hasMoreResults || isPageLoading) return;

        RecyclerView.LayoutManager manager = resultsList.getLayoutManager();
        if (!(manager instanceof LinearLayoutManager linearLayoutManager)) return;

        int itemCount = adapter.getItemCount();
        int lastVisiblePosition = linearLayoutManager.findLastVisibleItemPosition();
        if (shouldRevealLoadMore(hasMoreResults, isPageLoading, itemCount, lastVisiblePosition)) {
            loadMoreRevealedForCurrentPage = true;
            loadMoreButton.setVisibility(View.VISIBLE);
            Log.d(TAG, "AddFriendActivity.pagination: Load more revealed at end of list"
                    + ", visibleCount=" + itemCount
                    + ", lastVisiblePosition=" + lastVisiblePosition);
        }
    }

    static boolean shouldContinueLoading(boolean loadMoreRequest, int appendedCount, boolean hasMore) {
        return loadMoreRequest && appendedCount == 0 && hasMore;
    }

    static boolean shouldRevealLoadMore(boolean hasMore, boolean isLoading,
                                        int itemCount, int lastVisiblePosition) {
        return hasMore && !isLoading && itemCount > 0 && lastVisiblePosition >= itemCount - 1;
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
                    searchFeedbackActive = false;
                    resultText.setText(R.string.add_friend);
                    // Add the friend to our local set and update the adapter
                    friendUids.add(friendId);
                    adapter.setFriendUids(friendUids);
                })
                .addOnFailureListener(e -> {
                    searchFeedbackActive = false;
                    String text = SafeStringFormatter.safeGetString(this, R.string.failed_to_add_friend);
                    if (e instanceof FirebaseFunctionsException ffe) {
                        String reason = ffe.getMessage();
                        Log.e("TAG_Soccer", "addFriend failed: " + reason, ffe);
                        if (reason != null && !reason.isEmpty()) {
                            text = SafeStringFormatter.safeGetString(this, R.string.failed_to_add_friend_reason, reason);
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
