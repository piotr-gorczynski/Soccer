package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Objects;

public class RegulationActivity extends AppCompatActivity {

    private String tournamentId;
    private String regulationId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regulation);

        TextView nameTv = findViewById(R.id.regulationName);
        TextView bodyTv = findViewById(R.id.regulationBody);
        Button acceptBtn = findViewById(R.id.acceptRegulation);
        Button declineBtn = findViewById(R.id.declineRegulation);

        tournamentId = getIntent().getStringExtra("tournamentId");
        regulationId = getIntent().getStringExtra("regulationId");

        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                ": tournamentId=" + tournamentId + " regulationId=" + regulationId);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (!TextUtils.isEmpty(regulationId)) {
            FirebaseUser authUser = FirebaseAuth.getInstance().getCurrentUser();
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                    ": querying regulation from Firestore, currentUser=" +
                    (authUser != null ? authUser.getUid() : "null"));

            if (authUser == null) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                        Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                        ": user not authenticated – cannot load regulation");
                Toast.makeText(this, R.string.regulation_auth_required, Toast.LENGTH_LONG).show();
                return;
            }
            db.collection("regulations").document(regulationId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                    ": document found");
                            nameTv.setText(doc.getString("name"));
                            String bodyJson = doc.getString("body");
                            if (bodyJson != null) {
                                try {
                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                            Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                            ": parsing body JSON");
                                    JSONObject obj = new JSONObject(bodyJson);
                                    JSONArray rules = obj.optJSONArray("rules");
                                    if (rules != null) {
                                        StringBuilder sb = new StringBuilder();
                                        for (int i = 0; i < rules.length(); i++) {
                                            sb.append("• ").append(rules.getString(i)).append("\n\n");
                                        }
                                        bodyTv.setText(sb.toString().trim());
                                    } else {
                                        bodyTv.setText(bodyJson);
                                    }
                                } catch (Exception e) {
                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                            Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                            ": JSON parse error", e);
                                    bodyTv.setText(bodyJson);
                                }
                            }
                        } else {
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                    ": regulation document not found");
                            bodyTv.setText(R.string.regulation_not_found);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                ": Failed to load regulation", e);
                        Toast.makeText(this, R.string.regulation_load_error, Toast.LENGTH_LONG).show();
                    });
        } else {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                    ": empty regulationId");
            bodyTv.setText(R.string.regulation_not_found);
        }

        declineBtn.setOnClickListener(v -> finish());

        acceptBtn.setOnClickListener(v -> acceptAndJoin());
    }

    private void acceptAndJoin() {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                ": starting acceptAndJoin");

        if (TextUtils.isEmpty(tournamentId)) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                    ": empty tournamentId");
            Toast.makeText(this, "Tournament not found.", Toast.LENGTH_LONG).show();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                    ": user not logged-in");
            Toast.makeText(this, "You must be logged-in.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                ": refreshing ID token");
        user.getIdToken(true).addOnSuccessListener(tokenRes -> {
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                    ": token refresh OK");
            FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");
            Map<String,Object> data = Map.of(
                    "tournamentId", tournamentId,
                    "regulation", "accepted"
            );
            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                    ": calling joinTournament");
            functions.getHttpsCallable("joinTournament")
                    .call(data)
                    .addOnSuccessListener(r -> {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                ": joinTournament success");
                        Toast.makeText(this, "Joined! Wait for the bracket to start.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof FirebaseFunctionsException ffe) {
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": code=" + ffe.getCode()
                                    + "  msg=" + ffe.getMessage()
                                    + "  details=" + ffe.getDetails());
                        }
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                ": joinTournament failed", e);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
