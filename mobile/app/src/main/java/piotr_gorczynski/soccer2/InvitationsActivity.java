package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import android.content.SharedPreferences;

public class InvitationsActivity extends AppCompatActivity {

    ListView invitesList;
    ArrayAdapter<String> adapter;
    final ArrayList<String> inviteDescriptions = new ArrayList<>();
    final ArrayList<String> inviteIds = new ArrayList<>();

    FirebaseFirestore db;
    FirebaseAuth auth;

    private ListenerRegistration invitesSub;   // keep handle so we can remove it later

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": InvitationsActivity onNewIntent: " + intent.toUri(Intent.URI_INTENT_SCHEME));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invitations);

        invitesList = findViewById(R.id.invitesList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, inviteDescriptions);
        invitesList.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        listenForInvites();

        invitesList.setOnItemClickListener((parent, view, position, id) -> {
            String inviteId = inviteIds.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("Accept Invitation")
                    .setMessage("Do you want to accept this game invite?")
                    .setPositiveButton("Accept", (dialog, which) -> acceptInvite(inviteId))
                    .setNegativeButton("Cancel", null)
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
            Toast.makeText(this, "Invitation not found.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": User not signed-in");
            Toast.makeText(this, "You must be logged in to accept invites.",
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
                                    Toast.makeText(this, "Invalid response from server.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": matchPath received: " + matchPath);
                                Toast.makeText(this, "Invite accepted! Starting game…",
                                        Toast.LENGTH_SHORT).show();

                                SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                                String nickname = prefs.getString("nickname", "Player");

                                // Start game with matchPath, GameType 3, and local player nickname
                                startActivity(new Intent(this, GameActivity.class)
                                        .putExtra("matchPath", matchPath)
                                        .putExtra("GameType", 3)
                                        .putExtra("localNickname", nickname));
                            })

                            // ───────── failure ─────────
                            .addOnFailureListener(e -> {
                                String userMsg = "Could not accept invite.";

                                if (e instanceof FirebaseFunctionsException ffe) {
                                    Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": code=" + ffe.getCode()
                                            + "  message=" + ffe.getMessage()
                                            + "  details=" + ffe.getDetails());

                                    switch (ffe.getCode()) {
                                        /* sender withdrew the invite */
                                        case PERMISSION_DENIED -> userMsg = "Invite was cancelled.";
                                        /* logical failures coming back from CF */
                                        case FAILED_PRECONDITION ->
                                                userMsg = "Invite is no longer available.";
                                        /* time-out reached on the back-end */
                                        case DEADLINE_EXCEEDED -> userMsg = "Invite has expired.";
                                        default -> userMsg = "Invite is no longer available.";
                                    }
                                }

                                Toast.makeText(this, userMsg, Toast.LENGTH_LONG).show();

                                /* ⬇️  remove the stale item locally so the list refreshes */
                                int idx = inviteIds.indexOf(invitationId);
                                if (idx != -1) {
                                    inviteIds.remove(idx);
                                    inviteDescriptions.remove(idx);
                                    adapter.notifyDataSetChanged();
                                }
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Token refresh failed", e);
                    Toast.makeText(this, "Authentication error. Try logging in again.",
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
                });
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (invitesSub != null) invitesSub.remove();
    }

}
