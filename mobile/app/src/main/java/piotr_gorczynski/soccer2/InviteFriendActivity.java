package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InviteFriendActivity extends AppCompatActivity {

    EditText nicknameInput;
    Button sendInviteButton;
    TextView resultText;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friend);

        nicknameInput = findViewById(R.id.nicknameInput);
        sendInviteButton = findViewById(R.id.sendInviteButton);
        resultText = findViewById(R.id.inviteResult);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        sendInviteButton.setOnClickListener(view -> {
            String nickname = nicknameInput.getText().toString().trim();
            if (nickname.isEmpty()) {
                resultText.setText(R.string.please_enter_a_nickname);
                return;
            }

            searchAndInvite(nickname);
        });
    }

    private void searchAndInvite(String nickname) {
        db.collection("users")
                .whereEqualTo("nickname", nickname)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        resultText.setText(R.string.user_not_found);
                        return;
                    }

                    String currentUserId = Objects.requireNonNull(auth.getCurrentUser()).getUid();
                    DocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                    String targetUid = userDoc.getId();

                    if (targetUid.equals(currentUserId)) {
                        resultText.setText(R.string.you_can_t_invite_yourself);
                        return;
                    }

                    // ✅ Check if an invite already exists
                    db.collection("invitations")
                            .whereEqualTo("from", currentUserId)
                            .whereEqualTo("to", targetUid)
                            .whereEqualTo("status", "pending")
                            .get()
                            .addOnSuccessListener(existingInvites -> {
                                if (!existingInvites.isEmpty()) {
                                    resultText.setText(R.string.invite_already_sent);
                                } else {
                                    sendInvite(currentUserId, targetUid);
                                }
                            })
                            .addOnFailureListener(e -> {
                                resultText.setText(R.string.failed_to_check_existing_invites);
                                Log.e("TAG_Soccer", "Invite check failed", e);
                            });

                })
                .addOnFailureListener(e -> {
                    resultText.setText(R.string.error_searching_user);
                    Log.e("TAG_Soccer", "User lookup failed", e);
                });
    }

    private void sendInvite(String fromUid, String toUid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get both users' nicknames from the users collection
        Task<DocumentSnapshot> fromTask = db.collection("users").document(fromUid).get();
        Task<DocumentSnapshot> toTask = db.collection("users").document(toUid).get();

        Tasks.whenAllSuccess(fromTask, toTask).addOnSuccessListener(tasks -> {
            String fromNickname = fromTask.getResult().getString("nickname");
            String toNickname = toTask.getResult().getString("nickname");

            Map<String, Object> invite = new HashMap<>();
            invite.put("from", fromUid);
            invite.put("fromNickname", fromNickname);
            invite.put("to", toUid);
            invite.put("toNickname", toNickname);
            invite.put("status", "pending");
            invite.put("createdAt", FieldValue.serverTimestamp());

            db.collection("invitations")
                    .add(invite)
                    .addOnSuccessListener(docRef -> {
                        resultText.setText(R.string.invitation_sent);

                        String inviteId = docRef.getId();

                        // ✅ Start WaitingActivity (which will listen for match creation)
                        Intent intent = new Intent(this, WaitingActivity.class);
                        intent.putExtra("inviteId", inviteId);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        resultText.setText(R.string.failed_to_send_invite);
                        Log.e("TAG_Soccer", "Sending failed", e);
                    });

        }).addOnFailureListener(e -> {
            resultText.setText(R.string.failed_to_load_user_info);
            Log.e("TAG_Soccer", "Nickname fetch error", e);
        });
    }
}
