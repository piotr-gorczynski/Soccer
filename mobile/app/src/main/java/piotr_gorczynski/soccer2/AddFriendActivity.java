package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.widget.*;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.Map;
import java.util.Objects;

public class AddFriendActivity extends AppCompatActivity {

    EditText nicknameInput;
    Button addFriendButton;
    TextView resultText;

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        nicknameInput = findViewById(R.id.nicknameInput);
        addFriendButton = findViewById(R.id.addFriendButton);
        resultText = findViewById(R.id.addFriendResult);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        addFriendButton.setOnClickListener(v -> {
            String nickname = nicknameInput.getText().toString().trim();
            if (nickname.isEmpty()) {
                resultText.setText(R.string.please_enter_a_nickname);
                return;
            }
            searchAndAdd(nickname);
        });
    }

    private void searchAndAdd(String nickname) {
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
                    sendAddFriendViaCF(currentUserId, targetUid);
                })
                .addOnFailureListener(e -> {
                    resultText.setText(R.string.error_searching_user);
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                            + ": User lookup failed", e);
                });
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
                })
                .addOnFailureListener(e -> {
                    resultText.setText(R.string.failed_to_send_invite);
                    if (e instanceof FirebaseFunctionsException ffe) {
                        Log.e("TAG_Soccer", "addFriend failed: " + ffe.getMessage(), ffe);
                    } else {
                        Log.e("TAG_Soccer", "addFriend failed", e);
                    }
                });
    }
}
