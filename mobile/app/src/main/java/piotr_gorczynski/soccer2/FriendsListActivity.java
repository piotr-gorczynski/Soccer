package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView list;
    private TextView emptyText;
    private FriendAdapter adapter;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        Button addBtn = findViewById(R.id.addFriendBtn);
        list = findViewById(R.id.friendsList);
        emptyText = findViewById(R.id.emptyFriends);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter(this, this::sendInviteViaCF, this::removeFriend);
        list.setAdapter(adapter);

        addBtn.setOnClickListener(v -> startActivity(new Intent(this, AddFriendActivity.class)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadFriends();
    }

    private void loadFriends() {
        String uid = Objects.requireNonNull(auth.getCurrentUser()).getUid();
        db.collection("users").document(uid).collection("friends").get()
                .addOnSuccessListener(snap -> {
                    List<DocumentSnapshot> docs = snap.getDocuments();
                    if (docs.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        adapter.setData(new ArrayList<>());
                    } else {
                        emptyText.setVisibility(View.GONE);
                        adapter.setData(docs);
                    }
                })
                .addOnFailureListener(e -> emptyText.setVisibility(View.VISIBLE));
    }

    private void sendInviteViaCF(@NonNull String targetUid) {
        java.util.Map<String,Object> data = java.util.Collections.singletonMap("toUid", targetUid);
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("createInvite")
                .call(data)
                .addOnSuccessListener(res -> {
                    @SuppressWarnings("unchecked")
                    String inviteId = (String)((java.util.Map<String,Object>)java.util.Objects.requireNonNull(res.getData())).get("inviteId");
                    if (inviteId != null) {
                        startActivity(new Intent(this, WaitingActivity.class).putExtra("inviteId", inviteId));
                    }
                });
    }

    private void removeFriend(@NonNull String uidToRemove) {
        String uid = java.util.Objects.requireNonNull(auth.getCurrentUser()).getUid();
        java.util.Map<String,Object> data = new java.util.HashMap<>();
        data.put("userId", uid);
        data.put("friendId", uidToRemove);
        com.google.firebase.functions.FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("removeFriend")
                .call(data)
                .addOnSuccessListener(r -> loadFriends());
    }
}
