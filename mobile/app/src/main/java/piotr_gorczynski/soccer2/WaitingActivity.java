package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.*;

public class WaitingActivity extends AppCompatActivity {

    private static final String TAG = "WaitingActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_waiting_for_opponent);

        String inviteId = getIntent().getStringExtra("inviteId");
        if (inviteId == null || inviteId.isEmpty()) {
            Log.e(TAG, "Missing inviteId");
            finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();@

        // 🔍 Listen for match that was created after invite is accepted
        db.collection("matches")
                .whereEqualTo("invitationId", inviteId)
                .addSnapshotListener((snapshots, error) -> {
                    Log.d(TAG, "Match snapshot listener triggered");

                    if (error != null) {
                        Log.e(TAG, "Match listener error", error);
                        return;
                    }

                    if (snapshots == null) {
                        Log.w(TAG, "Match snapshots null");
                        return;
                    }

                    Log.d(TAG, "Snapshot size: " + snapshots.size());

                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot matchDoc = snapshots.getDocuments().get(0);
                        String matchId = matchDoc.getId();
                        Log.d(TAG, "✅ Match found with ID: " + matchId);

                        Intent intent = new Intent(this, GameActivity.class);
                        intent.putExtra("matchId", matchId);
                        intent.putExtra("GameType", 3);

                        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        String nickname = prefs.getString("nickname", "Player");
                        intent.putExtra("localNickname", nickname);

                        startActivity(intent);
                        finish();
                    } else {
                        Log.d(TAG, "No matches found with invitationId=" + inviteId);
                    }
                });
    }
}
