package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;

import java.util.Objects;

public class WaitingActivity extends AppCompatActivity {
    private boolean gameActivityLaunched = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_waiting_for_opponent);
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName() + ": Started");
        String inviteId = getIntent().getStringExtra("inviteId");
        if (inviteId == null || inviteId.isEmpty()) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {}.getClass().getEnclosingMethod()).getName() + ": Missing inviteId");
            finish();
            return;
        }

        TextView waitingMessage = findViewById(R.id.waitingMessage);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("invitations").document(inviteId).get()
                .addOnSuccessListener(inviteDoc -> {
                    String toNickname = inviteDoc.getString("toNickname");
                    if (toNickname != null) {

                        String msg = getString(R.string.waiting_for_opponent_named, toNickname);
                        waitingMessage.setText(msg);
                    }
                });

        // 🔍 Listen for match that was created after invite is accepted
        db.collection("matches")
                .whereEqualTo("invitationId", inviteId)
                .addSnapshotListener((snapshots, error) -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Match snapshot listener triggered");

                    if (error != null) {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Match listener error", error);
                        return;
                    }

                    if (snapshots == null) {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Match snapshots null");
                        return;
                    }

                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Snapshot size: " + snapshots.size());

                    if (!snapshots.isEmpty()) {
                        if (!gameActivityLaunched) {
                            // Launch GameActivity only once!
                            gameActivityLaunched = true;
                            DocumentSnapshot matchDoc = snapshots.getDocuments().get(0);
                            String matchId = matchDoc.getId();
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": ✅ Match found with ID: " + matchId +". Starting GameActivity...");

                            Intent intent = new Intent(this, GameActivity.class);
                            intent.putExtra("matchId", matchId);
                            intent.putExtra("GameType", 3);

                            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                            String nickname = prefs.getString("nickname", "Player");
                            intent.putExtra("localNickname", nickname);

                            startActivity(intent);
                            finish();
                        } else {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": gameActivityLaunched==true, therefore skipping...");
                        }
                    } else {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": No matches found with invitationId=" + inviteId);
                    }
                });
    }
}
