package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;

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

    private void acceptInvite(String invitationId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "You must be logged in to accept invites.", Toast.LENGTH_LONG).show();
            Log.e("DEBUG", "No Firebase user");
            return;
        }

        Log.d("DEBUG", "Refreshing ID token...");
        user.getIdToken(true).addOnSuccessListener(result -> {
            Log.d("DEBUG", "Token refresh OK");

            FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");

            Log.d("DEBUG", "Calling Firebase function acceptInvite...");

            functions
                    .getHttpsCallable("acceptInvite")
                    .call(Collections.singletonMap("invitationId", invitationId))
                    .addOnSuccessListener(result1 -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> data = (Map<String, Object>) result1.getData();

                        if (data != null && data.containsKey("matchId")) {
                            String matchId = (String) data.get("matchId");
                            Log.d("DEBUG", "matchId received: " + matchId);
                            Toast.makeText(this, "Invite accepted! Starting game...", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(this, GameActivity.class);
                            intent.putExtra("matchId", matchId);
                            startActivity(intent);
                            finish();
                        } else {
                            Log.e("DEBUG", "matchId missing in response");
                            Toast.makeText(this, "No matchId received from server.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("DEBUG", "Function call failed", e);
                        Toast.makeText(this, "Failed to accept invite: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        }).addOnFailureListener(e -> {
            Log.e("DEBUG", "Token refresh failed", e);
            Toast.makeText(this, "Authentication error. Try logging in again.", Toast.LENGTH_LONG).show();
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
