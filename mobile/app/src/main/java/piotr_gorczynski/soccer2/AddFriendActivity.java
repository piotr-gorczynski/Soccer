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
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.Map;
import java.util.Objects;

public class AddFriendActivity extends AppCompatActivity {

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

    FirebaseFirestore db;
    FirebaseAuth auth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

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
                searchButton.setEnabled(s.length() > 1);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchButton.setOnClickListener(v -> {
            String query = nicknameInput.getText().toString().trim();
            if (query.length() <= 1) return;
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
    }

    private void searchPage() {

        Query q;
        if (fallbackMode) {
            q = db.collection("users")
                    .orderBy("nickname")
                    .startAt(originalQuery)
                    .endAt(originalQuery + "\uf8ff")
                    .limit(10);
        } else {
            q = db.collection("users")
                    .orderBy("nicknameLowercase")
                    .startAt(currentQuery)
                    .endAt(currentQuery + "\uf8ff")
                    .limit(10);
        }

        if (lastVisible != null) q = q.startAfter(lastVisible);

        q.get()
                .addOnSuccessListener(snap -> {
                    java.util.List<DocumentSnapshot> docs = snap.getDocuments();
                    adapter.addResults(docs);

                    if (docs.isEmpty() && !fallbackMode && lastVisible == null) {
                        fallbackMode = true;
                        searchPage();
                        return;
                    }

                    if (docs.size() == 10) {
                        lastVisible = docs.get(docs.size() - 1);
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
}
