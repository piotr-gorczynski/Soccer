package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import android.content.SharedPreferences;

public class InvitationsActivity extends BaseActivity {

    ListView invitesList;
    TextView emptyText;
    ArrayAdapter<String> adapter;
    final ArrayList<String> inviteDescriptions = new ArrayList<>();
    final ArrayList<String> inviteIds = new ArrayList<>();

    RecyclerView pastInvitesList;
    TextView pastInvitesLabel;
    TextView emptyPastInvites;
    PastInviteAdapter pastAdapter;
    RecyclerView.AdapterDataObserver pastInvitesObserver;

    FirebaseFirestore db;
    FirebaseAuth auth;

    private ListenerRegistration invitesSub;   // keep handle so we can remove it later
    private ListenerRegistration pastInvitesSub;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": InvitationsActivity onNewIntent: " + intent.toUri(Intent.URI_INTENT_SCHEME));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        Toolbar toolbar = findViewById(R.id.invitations_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.pending_game_invitations);

        invitesList = findViewById(R.id.invitesList);
        emptyText = findViewById(R.id.emptyInvites);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, inviteDescriptions);
        invitesList.setAdapter(adapter);

        pastInvitesList = findViewById(R.id.pastInvitesList);
        pastInvitesLabel = findViewById(R.id.pastInvitesLabel);
        emptyPastInvites = findViewById(R.id.emptyPastInvites);
        pastInvitesList.setLayoutManager(new LinearLayoutManager(this));
        pastAdapter = new PastInviteAdapter(this, this::sendInviteViaCF, this::addFriend);
        pastInvitesList.setAdapter(pastAdapter);
        pastInvitesObserver = new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                updatePastInvitesEmptyState();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                updatePastInvitesEmptyState();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                updatePastInvitesEmptyState();
            }
        };
        pastAdapter.registerAdapterDataObserver(pastInvitesObserver);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        listenForInvites();
        listenForPastInvites();

        invitesList.setOnItemClickListener((parent, view, position, id) -> {
            String inviteId = inviteIds.get(position);

            new AlertDialog.Builder(this)
                    .setTitle(R.string.accept_invitation_title)
                    .setMessage(R.string.accept_invitation_message)
                    .setPositiveButton(R.string.accept, (dialog, which) -> acceptInvite(inviteId))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });

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
                                int idx = inviteIds.indexOf(invitationId);
                                if (idx != -1) {
                                    inviteIds.remove(idx);
                                    inviteDescriptions.remove(idx);
                                    adapter.notifyDataSetChanged();
                                    if (inviteIds.isEmpty()) {
                                        emptyText.setVisibility(View.VISIBLE);
                                    }
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

        invitesSub = db.collection("invitations")
                .whereEqualTo("to", currentUserId)
                .whereEqualTo("status", "pending")
                .orderBy("expireAt")                // ← added
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        inviteDescriptions.clear();
                        inviteIds.clear();
                        adapter.notifyDataSetChanged();
                        emptyText.setVisibility(View.VISIBLE);
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                + ": Listen failed", e);
                        return;
                    }

                    inviteDescriptions.clear();
                    inviteIds.clear();

                    long nowMillis = System.currentTimeMillis();      // 🕔 current client time
                    for (DocumentSnapshot doc : Objects.requireNonNull(querySnapshot)) {
                        /* hide already-expired docs that still satisfy the
                        original query because the listener was opened
                        before they lapsed */
                        Timestamp exp = doc.getTimestamp("expireAt");
                        if (exp != null && exp.toDate().getTime() <= nowMillis) {
                            continue;       // skip – TTL passed
                        }
                        String fromUid = doc.getString("from");

                        // optimistic placeholder
                        inviteDescriptions.add("Invite from: ...");
                        inviteIds.add(doc.getId());

                        // async nickname lookup
                        db.collection("users").document(Objects.requireNonNull(fromUid)).get()
                                .addOnSuccessListener(userSnap -> {
                                    String nick = userSnap.getString("nickname");
                                    int idx = inviteIds.indexOf(doc.getId());
                                    if (idx != -1 && nick != null) {
                                        inviteDescriptions.set(idx, "Invite from: " + nick);
                                        adapter.notifyDataSetChanged();
                                    }
                                });
                    }

                    adapter.notifyDataSetChanged();
                    if (inviteIds.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                    }
                });
    }

    private void listenForPastInvites() {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        pastInvitesSub = db.collection("invitations")
                .whereEqualTo("to", currentUserId)
                .whereIn("status", java.util.Arrays.asList("accepted", "cancelled", "expired"))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + ".listenForPastInvites: Listen failed", e);
                        emptyPastInvites.setVisibility(View.VISIBLE);
                        return;
                    }

                    List<DocumentSnapshot> pastInvitesList = new ArrayList<>();
                    for (DocumentSnapshot doc : Objects.requireNonNull(querySnapshot)) {
                        pastInvitesList.add(doc);
                    }

                    pastAdapter.setData(pastInvitesList);
                    updatePastInvitesEmptyState();
                });
    }

    private void updatePastInvitesEmptyState() {
        if (emptyPastInvites == null || pastAdapter == null) {
            return;
        }

        if (pastAdapter.getItemCount() == 0) {
            emptyPastInvites.setVisibility(View.VISIBLE);
        } else {
            emptyPastInvites.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pastAdapter != null && pastInvitesObserver != null) {
            pastAdapter.unregisterAdapterDataObserver(pastInvitesObserver);
        }
    }

    private void addFriend(@NonNull String targetUid) {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        
        // Add to both users' friends subcollections
        db.collection("users").document(currentUserId).collection("friends").document(targetUid)
                .set(Collections.singletonMap("addedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()))
                .addOnSuccessListener(aVoid -> {
                    // Also add current user to target's friends
                    db.collection("users").document(targetUid).collection("friends").document(currentUserId)
                            .set(Collections.singletonMap("addedAt", com.google.firebase.firestore.FieldValue.serverTimestamp()))
                            .addOnSuccessListener(aVoid2 -> {
                                Toast.makeText(this, R.string.friend_added, Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + ".addFriend: Error adding to target's friends", e);
                                Toast.makeText(this, R.string.failed_to_add_friend, Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + ".addFriend: Error adding friend", e);
                    Toast.makeText(this, R.string.failed_to_add_friend, Toast.LENGTH_SHORT).show();
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
                    int msgId = R.string.failed_to_send_invite;
                    String customMessage = null;

                    if (e instanceof FirebaseFunctionsException ffe) {
                        FirebaseFunctionsException.Code code = ffe.getCode();
                        String details = ffe.getMessage();
                        
                        switch (code) {
                            case ALREADY_EXISTS:
                                if (details != null && details.contains("active invitation")) {
                                    msgId = R.string.active_invitation_exists;
                                } else if (details != null && details.contains("blocked")) {
                                    msgId = R.string.user_blocked_invites;
                                }
                                break;
                            case FAILED_PRECONDITION:
                                msgId = R.string.cannot_invite_this_user;
                                break;
                            case NOT_FOUND:
                                msgId = R.string.user_not_found;
                                break;
                            default:
                                customMessage = details;
                        }
                    }

                    if (customMessage != null) {
                        Toast.makeText(this, getString(msgId) + ": " + customMessage, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, msgId, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (invitesSub != null) invitesSub.remove();
        if (pastInvitesSub != null) pastInvitesSub.remove();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}
