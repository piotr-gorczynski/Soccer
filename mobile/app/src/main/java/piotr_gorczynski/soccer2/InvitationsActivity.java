package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import android.content.SharedPreferences;

public class InvitationsActivity extends BaseActivity {

    RecyclerView invitationsRecyclerView;
    PendingInviteAdapter pendingAdapter;
    RecyclerView.AdapterDataObserver pendingInvitesObserver;
    PastInviteAdapter pastAdapter;
    RecyclerView.AdapterDataObserver pastInvitesObserver;
    SectionHeaderAdapter pendingHeaderAdapter;
    SectionHeaderAdapter pastHeaderAdapter;
    EmptyStateAdapter pendingEmptyAdapter;
    EmptyStateAdapter pastEmptyAdapter;
    ConcatAdapter concatAdapter;

    FirebaseFirestore db;
    FirebaseAuth auth;
    private Set<String> friendUids = new HashSet<>();

    private ListenerRegistration invitesSub;   // keep handle so we can remove it later
    private ListenerRegistration pastInvitesSub;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": InvitationsActivity onNewIntent: " + intent.toUri(Intent.URI_INTENT_SCHEME));
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        prefs.edit().putLong(MenuActivity.PREF_LAST_INVITES_SEEN_TIMESTAMP, System.currentTimeMillis()).apply();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        Toolbar toolbar = findViewById(R.id.invitations_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.pending_game_invitations);

        invitationsRecyclerView = findViewById(R.id.invitationsRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        invitationsRecyclerView.setLayoutManager(layoutManager);
        disableChangeAnimations(invitationsRecyclerView);

        pendingAdapter = new PendingInviteAdapter(this, this::acceptInvite);
        pastAdapter = new PastInviteAdapter(this, this::sendInviteViaCF, this::addFriend);
        pastAdapter.setFriendUids(friendUids);

        pendingHeaderAdapter = new SectionHeaderAdapter(getString(R.string.pending_invites));
        pastHeaderAdapter = new SectionHeaderAdapter(getString(R.string.past_invites));
        pendingEmptyAdapter = new EmptyStateAdapter(getString(R.string.no_pending_invites));
        pastEmptyAdapter = new EmptyStateAdapter(getString(R.string.no_past_invites));
        pendingEmptyAdapter.setVisible(false);
        pastEmptyAdapter.setVisible(false);

        concatAdapter = new ConcatAdapter(
                pendingHeaderAdapter,
                pendingEmptyAdapter,
                pendingAdapter,
                pastHeaderAdapter,
                pastEmptyAdapter,
                pastAdapter
        );
        invitationsRecyclerView.setAdapter(concatAdapter);

        pendingInvitesObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                onPendingInvitesChanged();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                onPendingInvitesChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                onPendingInvitesChanged();
            }
        };
        pendingAdapter.registerAdapterDataObserver(pendingInvitesObserver);

        pastInvitesObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                onPastInvitesChanged();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                onPastInvitesChanged();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                onPastInvitesChanged();
            }
        };
        pastAdapter.registerAdapterDataObserver(pastInvitesObserver);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        listenForInvites();
        listenForPastInvites();
        loadFriends();

    }

    private void disableChangeAnimations(@NonNull RecyclerView recyclerView) {
        RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        // Recent RecyclerView versions can crash when animations are running while the
        // adapter dataset is invalidated and the view is being remeasured (see
        // DefaultItemAnimator#runPendingAnimations). Because InvitationsActivity updates the
        // lists by calling notifyDataSetChanged() followed by requestLayout(), it is safer to
        // disable the animator entirely so that RecyclerView never tries to operate on a null
        // ViewHolder during those layout passes.
        recyclerView.setItemAnimator(null);
    }

    // At the top of the class — keeps logcat tidy

    /**
     * Attempts to accept a pending invitation by calling the
     * Cloud Function `acceptInvite`.
     *
     * @param invitationId Firestore document ID of the invitation
     */
    private void acceptInvite(@NonNull String invitationId) {

        if (TextUtils.isEmpty(invitationId)) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": acceptInvite called with empty invitationId");
            Toast.makeText(this, getString(R.string.invitation_not_found), Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": User not signed-in");
            Toast.makeText(this, getString(R.string.must_log_in_to_accept_invites),
                    Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Refreshing ID token…");
        user.getIdToken(/* forceRefresh = */ true)
                .addOnSuccessListener(tokenResult -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Token refresh OK");

                    FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");
                    Map<String, Object> data = Collections.singletonMap("invitationId", invitationId);

                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Calling Cloud Function acceptInvite");
                    functions.getHttpsCallable("acceptInvite")
                            .call(data)

                            // ───────── success ─────────
                            .addOnSuccessListener(result -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> payload = (Map<String, Object>) result.getData();
                                String matchPath = payload != null ? (String) payload.get("matchPath") : null;

                                if (TextUtils.isEmpty(matchPath)) {
                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": matchPath missing in response: " + payload);
                                    Toast.makeText(this, getString(R.string.invalid_response_from_server),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": matchPath received: " + matchPath);
                                Toast.makeText(this, getString(R.string.invite_accepted_starting_game),
                                        Toast.LENGTH_SHORT).show();

                                SharedPreferences prefs =
                                        getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
                                String nickname = prefs.getString("nickname", "Player");

                                // Start game with matchPath, GameType 3, and local player nickname
                                startActivity(new Intent(this, GameActivity.class)
                                        .putExtra("matchPath", matchPath)
                                        .putExtra("GameType", 3)
                                        .putExtra("localNickname", nickname));
                            })

                            // ───────── failure ─────────
                            .addOnFailureListener(e -> {
                                String userMsg = getString(R.string.could_not_accept_invite);

                                if (e instanceof FirebaseFunctionsException ffe) {
                                    Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": code=" + ffe.getCode()
                                            + "  message=" + ffe.getMessage()
                                            + "  details=" + ffe.getDetails());

                                    switch (ffe.getCode()) {
                                        /* sender withdrew the invite */
                                        case PERMISSION_DENIED -> userMsg = getString(R.string.invite_was_cancelled);
                                        /* logical failures coming back from CF */
                                        case FAILED_PRECONDITION ->
                                                userMsg = getString(R.string.invite_no_longer_available);
                                        /* time-out reached on the back-end */
                                        case DEADLINE_EXCEEDED -> userMsg = getString(R.string.invite_has_expired);
                                        default -> userMsg = getString(R.string.invite_no_longer_available);
                                    }
                                }

                                Toast.makeText(this, userMsg, Toast.LENGTH_LONG).show();

                                /* ⬇️  remove the stale item locally so the list refreshes */
                                if (pendingAdapter != null) {
                                    pendingAdapter.removeInviteById(invitationId);
                                    updatePendingInvitesEmptyState();
                                }
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Token refresh failed", e);
                    Toast.makeText(this, getString(R.string.authentication_error_try_again),
                            Toast.LENGTH_LONG).show();
                });
    }





    private void listenForInvites() {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        // Remove previous subscription if exists
        if (invitesSub != null) {
            invitesSub.remove();
        }

        Query query = db.collection("invitations")
                .whereEqualTo("to", currentUserId)
                .whereEqualTo("status", "pending")
                .orderBy("expireAt");

        invitesSub = query.addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        if (pendingAdapter != null) {
                            pendingAdapter.clear();
                        }
                        pendingEmptyAdapter.setVisible(true);
                        logUiSnapshot("listenForInvites_error");
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": Listen failed", e);
                        return;
                    }

                    List<DocumentSnapshot> pendingInvites = new ArrayList<>();

                    long nowMillis = System.currentTimeMillis();      // 🕔 current client time
                    for (DocumentSnapshot doc : Objects.requireNonNull(querySnapshot)) {
                        /* hide already-expired docs that still satisfy the
                        original query because the listener was opened
                        before they lapsed */
                        Timestamp exp = doc.getTimestamp("expireAt");
                        if (exp != null && exp.toDate().getTime() <= nowMillis) {
                            continue;       // skip – TTL passed
                        }
                        pendingInvites.add(doc);
                    }

                    if (pendingAdapter != null) {
                        pendingInvites.sort((left, right) -> {
                            Timestamp leftCreatedAt = left.getTimestamp("createdAt");
                            Timestamp rightCreatedAt = right.getTimestamp("createdAt");

                            long leftMillis = leftCreatedAt != null ? leftCreatedAt.toDate().getTime() : Long.MAX_VALUE;
                            long rightMillis = rightCreatedAt != null ? rightCreatedAt.toDate().getTime() : Long.MAX_VALUE;

                            return Long.compare(leftMillis, rightMillis);
                        });

                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".listenForInvites: Showing " + pendingInvites.size() + " pending invites");
                        for (DocumentSnapshot invite : pendingInvites) {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + ".listenForInvites: Pending invite ID: " + invite.getId());
                        }

                        pendingAdapter.setData(pendingInvites);
                    }

                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".listenForInvites: After setData, adapter reports " + pendingAdapter.getItemCount() + " items");

                    updatePendingInvitesEmptyState();
                    logUiSnapshot("listenForInvites_afterSetData");
                });
    }

    private void onPendingInvitesChanged() {
        updatePendingInvitesEmptyState();
        logUiSnapshot("pendingAdapterDataChanged");
    }

    private void updatePendingInvitesEmptyState() {
        if (pendingEmptyAdapter == null || pendingAdapter == null) {
            return;
        }

        boolean isEmpty = pendingAdapter.getItemCount() == 0;
        pendingEmptyAdapter.setVisible(isEmpty);
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".updatePendingInvitesEmptyState: Empty message visible = " + isEmpty);
    }

    private void listenForPastInvites() {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        // Remove previous subscription if exists
        if (pastInvitesSub != null) {
            pastInvitesSub.remove();
        }

        // Query without orderBy to avoid requiring complex composite index with whereIn
        // We'll sort the results client-side instead
        Query query = db.collection("invitations")
                .whereEqualTo("to", currentUserId)
                .whereIn("status", java.util.Arrays.asList("accepted", "cancelled", "expired"));

        pastInvitesSub = query.addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + ".listenForPastInvites: Listen failed", e);
                        pastAdapter.setData(Collections.emptyList());
                        pastEmptyAdapter.setVisible(true);
                        logUiSnapshot("listenForPastInvites_error");
                        return;
                    }

                    List<DocumentSnapshot> pastInvitesList = new ArrayList<>();
                    for (DocumentSnapshot doc : Objects.requireNonNull(querySnapshot)) {
                        pastInvitesList.add(doc);
                    }

                    // Sort by createdAt descending (newest first) - done client-side to avoid
                    // needing a complex composite index when using whereIn with orderBy
                    pastInvitesList.sort((left, right) -> {
                        Timestamp leftCreatedAt = left.getTimestamp("createdAt");
                        Timestamp rightCreatedAt = right.getTimestamp("createdAt");

                        long leftMillis = leftCreatedAt != null ? leftCreatedAt.toDate().getTime() : 0;
                        long rightMillis = rightCreatedAt != null ? rightCreatedAt.toDate().getTime() : 0;

                        // Descending order (newest first)
                        return Long.compare(rightMillis, leftMillis);
                    });

                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".listenForPastInvites: Showing " + pastInvitesList.size() + " past invites");
                    for (DocumentSnapshot invite : pastInvitesList) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".listenForPastInvites: Past invite ID: " + invite.getId());
                    }

                    pastAdapter.setData(pastInvitesList);

                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".listenForPastInvites: After setData, adapter reports " + pastAdapter.getItemCount() + " items");

                    updatePastInvitesEmptyState();
                    logUiSnapshot("listenForPastInvites_afterSetData");
                });
    }

    private void loadFriends() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + ".loadFriends: User not signed-in");
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .collection("friends")
                .get()
                .addOnSuccessListener(snap -> {
                    Set<String> newFriendUids = new HashSet<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        newFriendUids.add(doc.getId());
                    }
                    friendUids = newFriendUids;
                    pastAdapter.setFriendUids(friendUids);
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + ".loadFriends: Failed to load friends", e);
                    friendUids = new HashSet<>();
                    pastAdapter.setFriendUids(friendUids);
                });
    }

    private void onPastInvitesChanged() {
        updatePastInvitesEmptyState();
        logUiSnapshot("pastAdapterDataChanged");
    }

    private void updatePastInvitesEmptyState() {
        if (pastEmptyAdapter == null || pastAdapter == null) {
            return;
        }

        boolean isEmpty = pastAdapter.getItemCount() == 0;
        pastEmptyAdapter.setVisible(isEmpty);
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".updatePastInvitesEmptyState: Empty message visible = " + isEmpty);
    }

    private void logUiSnapshot(@NonNull String source) {
        if (invitationsRecyclerView == null) {
            return;
        }

        invitationsRecyclerView.post(() -> {
            RecyclerView.LayoutManager layoutManager = invitationsRecyclerView.getLayoutManager();
            int childCount = invitationsRecyclerView.getChildCount();
            int totalItems = concatAdapter != null ? concatAdapter.getItemCount() : -1;

            int firstVisible = -1;
            int lastVisible = -1;
            if (layoutManager instanceof LinearLayoutManager linearLayoutManager) {
                firstVisible = linearLayoutManager.findFirstVisibleItemPosition();
                lastVisible = linearLayoutManager.findLastVisibleItemPosition();
            }

            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + source
                    + ": RecyclerView childCount=" + childCount
                    + ", totalAdapterItems=" + totalItems
                    + ", firstVisible=" + firstVisible
                    + ", lastVisible=" + lastVisible);

            for (int i = 0; i < childCount; i++) {
                View child = invitationsRecyclerView.getChildAt(i);
                int adapterPosition = invitationsRecyclerView.getChildAdapterPosition(child);
                CharSequence summary = null;

                TextView header = child.findViewById(R.id.sectionTitle);
                if (header != null) {
                    summary = header.getText();
                } else {
                    TextView inviteTitle = child.findViewById(R.id.inviteReceivedAndStatus);
                    if (inviteTitle != null) {
                        summary = inviteTitle.getText();
                    } else {
                        TextView emptyMessage = child.findViewById(R.id.emptyMessage);
                        if (emptyMessage != null) {
                            summary = emptyMessage.getText();
                        }
                    }
                }

                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + source
                        + ": child[" + i + "] adapterPos=" + adapterPosition
                        + (summary != null ? ", text=" + summary : ""));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingAdapter != null && pendingInvitesObserver != null) {
            pendingAdapter.unregisterAdapterDataObserver(pendingInvitesObserver);
        }
        if (pendingAdapter != null) {
            pendingAdapter.removePresenceSubscriptions();
        }
        if (pastAdapter != null && pastInvitesObserver != null) {
            pastAdapter.unregisterAdapterDataObserver(pastInvitesObserver);
        }
        if (invitesSub != null) {
            invitesSub.remove();
        }
        if (pastInvitesSub != null) {
            pastInvitesSub.remove();
        }
    }

    private void addFriend(@NonNull String targetUid) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".addFriend: User not signed-in");
            Toast.makeText(this, R.string.must_log_in_to_accept_invites, Toast.LENGTH_SHORT).show();
            return;
        }

        if (friendUids.contains(targetUid)) {
            Toast.makeText(this, R.string.already_friends, Toast.LENGTH_SHORT).show();
            return;
        }

        sendAddFriendViaCF(currentUser.getUid(), targetUid);
    }

    private void sendAddFriendViaCF(@NonNull String userId, @NonNull String friendId) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", userId);
        data.put("friendId", friendId);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("addFriend")
                .call(data)
                .addOnSuccessListener(res -> {
                    Toast.makeText(this, R.string.friend_added, Toast.LENGTH_SHORT).show();
                    friendUids.add(friendId);
                    pastAdapter.addFriendUid(friendId);
                })
                .addOnFailureListener(e -> {
                    String text = SafeStringFormatter.safeGetString(this, R.string.failed_to_add_friend);
                    if (e instanceof FirebaseFunctionsException ffe) {
                        String reason = ffe.getMessage();
                        Log.e("TAG_Soccer", getClass().getSimpleName() + ".addFriend: addFriend failed: " + reason, ffe);
                        if (reason != null && !reason.isEmpty()) {
                            text = SafeStringFormatter.safeGetString(this, R.string.failed_to_add_friend_reason, reason);
                        }
                    } else {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + ".addFriend: addFriend failed", e);
                    }
                    Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
                });
    }

    private void sendInviteViaCF(@NonNull String targetUid) {
        Map<String,Object> data = Collections.singletonMap("toUid", targetUid);
        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("createInvite")
                .call(data)
                .addOnSuccessListener(res -> {
                    @SuppressWarnings("unchecked")
                    String inviteId = (String)((Map<String,Object>)Objects.requireNonNull(res.getData())).get("inviteId");
                    if (inviteId != null) {
                        startActivity(new Intent(this, WaitingActivity.class).putExtra("inviteId", inviteId));
                    }
                })
                .addOnFailureListener(e -> {
                    /* default fallback */
                    int msgId = R.string.failed_to_send_invite;
                    String customMessage = null;

                    if (e instanceof FirebaseFunctionsException ffe) {
                        FirebaseFunctionsException.Code code = ffe.getCode();
                        if (code == FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
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
                        } else if (code == FirebaseFunctionsException.Code.PERMISSION_DENIED) {
                            msgId = R.string.invite_already_sent;    // inviter cancelled meanwhile
                        }
                    }

                    if (customMessage != null) {
                        Toast.makeText(this, customMessage, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, msgId, Toast.LENGTH_LONG).show();
                    }

                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": createInvite failed", e);
                });
    }
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}
