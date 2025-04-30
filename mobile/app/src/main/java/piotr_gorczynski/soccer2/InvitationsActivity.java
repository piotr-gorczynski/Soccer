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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class InvitationsActivity extends AppCompatActivity {

    ListView invitesList;
    ArrayAdapter<String> adapter;
    final ArrayList<String> inviteDescriptions = new ArrayList<>();
    final ArrayList<String> inviteIds = new ArrayList<>();

    FirebaseFirestore db;
    FirebaseAuth auth;

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
    private static final String TAG = "INVITE";

    /**
     * Attempts to accept a pending invitation by calling the
     * Cloud Function `acceptInvite`.
     *
     * @param invitationId Firestore document ID of the invitation
     */
    private void acceptInvite(@NonNull String invitationId) {

        if (TextUtils.isEmpty(invitationId)) {
            Log.w(TAG, "acceptInvite called with empty invitationId");
            Toast.makeText(this, "Invitation not found.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not signed-in");
            Toast.makeText(this, "You must be logged in to accept invites.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "Refreshing ID token…");
        user.getIdToken(/* forceRefresh = */ true)
                .addOnSuccessListener(tokenResult -> {
                    Log.d(TAG, "Token refresh OK");

                    FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");
                    Map<String, Object> data = Collections.singletonMap("invitationId", invitationId);

                    Log.d(TAG, "Calling Cloud Function acceptInvite");
                    functions.getHttpsCallable("acceptInvite")
                            .call(data)

                            // ───────── success ─────────
                            .addOnSuccessListener(result -> {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> payload = (Map<String, Object>) result.getData();
                                String matchId = payload != null ? (String) payload.get("matchId") : null;

                                if (TextUtils.isEmpty(matchId)) {
                                    Log.e(TAG, "matchId missing in response: " + payload);
                                    Toast.makeText(this, "Invalid response from server.",
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }

                                Log.d(TAG, "matchId received: " + matchId);
                                Toast.makeText(this, "Invite accepted! Starting game…",
                                        Toast.LENGTH_SHORT).show();

                                startActivity(new Intent(this, GameActivity.class)
                                        .putExtra("matchId", matchId)
                                        .putExtra("GameType", 1));
                                finish();
                            })

                            // ───────── failure ─────────
                            .addOnFailureListener(e -> {
                                if (e instanceof FirebaseFunctionsException) {
                                    FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                    // ← this line gives you the real reason (App Check, IAM, etc.)
                                    Log.w(TAG, "code=" + ffe.getCode()
                                            + "  message=" + ffe.getMessage()
                                            + "  details=" + ffe.getDetails());
                                }
                                Log.e(TAG, "Cloud Function call failed", e);
                                Toast.makeText(this, "Failed to accept invite: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Token refresh failed", e);
                    Toast.makeText(this, "Authentication error. Try logging in again.",
                            Toast.LENGTH_LONG).show();
                });
    }





    private void listenForInvites() {
        String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();

        db.collection("invitations")
                .whereEqualTo("to", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.e("Invites", "Listen failed", e);
                        return;
                    }

                    inviteDescriptions.clear();
                    inviteIds.clear();

                    for (DocumentSnapshot doc : Objects.requireNonNull(querySnapshot).getDocuments()) {
                        String fromNickname = doc.getString("fromNickname");
                        inviteDescriptions.add("Invite from: " + fromNickname);
                        inviteIds.add(doc.getId());
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}
