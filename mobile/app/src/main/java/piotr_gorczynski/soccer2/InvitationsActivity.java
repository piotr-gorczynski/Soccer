package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
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
            Toast.makeText(this, "You clicked invite: " + inviteId, Toast.LENGTH_SHORT).show();
            // You can navigate to a match or call a function to accept it here
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
