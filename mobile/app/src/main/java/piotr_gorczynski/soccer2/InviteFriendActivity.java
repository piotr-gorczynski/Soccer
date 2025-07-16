package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.Collections;
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
                    sendInviteViaCF(targetUid);
                })
                .addOnFailureListener(e -> {
                    resultText.setText(R.string.error_searching_user);
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": User lookup failed", e);
                });
    }

    private void sendInviteViaCF(@NonNull String targetUid) {
        Map<String,Object> data = Collections.singletonMap("toUid", targetUid);

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("createInvite")
                .call(data)
                .addOnSuccessListener(res -> {
                    @SuppressWarnings("unchecked")
                    String inviteId = (String) ((Map<String,Object>) Objects.requireNonNull(res.getData())).get("inviteId");
                    if (inviteId == null) {
                        resultText.setText(R.string.failed_to_send_invite);
                        return;
                    }
                    Intent i = new Intent(this, WaitingActivity.class)
                            .putExtra("inviteId", inviteId);
                    startActivity(i);
                })
                .addOnFailureListener(e -> {
                    /* default fallback */
                    int msgId = R.string.failed_to_send_invite;
                    if (e instanceof FirebaseFunctionsException ffe) {
                        FirebaseFunctionsException.Code code = ffe.getCode();
                        if (code == FirebaseFunctionsException.Code.FAILED_PRECONDITION) {
                            /* Cloud Function puts a short reason in getMessage() */
                            String reason = String.valueOf(ffe.getMessage());   // never null
                            msgId = switch (reason) {
                                /* inviter already has an unanswered invite */
                                case "sender_busy" ->              // you = sender
                                        R.string.invite_already_sent;
                                /* target has its own outgoing invite and is waiting */
                                case "target_busy" ->              // they = sender elsewhere
                                        R.string.target_player_busy;
                                default -> R.string.failed_to_send_invite;
                            };
                        } else if (code == FirebaseFunctionsException.Code.PERMISSION_DENIED) {
                            msgId = R.string.invite_already_sent;    // inviter cancelled meanwhile
                        }
                    }
                    resultText.setText(msgId);
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": createInvite failed", e);
                });
    }

}
