package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RegulationActivity extends BaseActivity {

    private String tournamentId;
    private String regulationId;
    private AnalyticsManager analyticsManager;

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
        
        // Get analytics manager from SoccerApp  
        analyticsManager = ((SoccerApp) getApplicationContext()).getAnalyticsManager();

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

                            String langCode = LanguageManager.getCurrentLanguageCode(this);
                            doc.getReference().collection(langCode).document("rules").get()
                                    .addOnSuccessListener(ruleDoc -> {
                                        if (ruleDoc.exists()) {
                                            List<?> rules = (List<?>) ruleDoc.get("rules");
                                            if (rules != null) {
                                                StringBuilder sb = new StringBuilder();
                                                for (Object r : rules) {
                                                    sb.append("• ").append(r.toString()).append("\n\n");
                                                }
                                                bodyTv.setText(sb.toString().trim());
                                            } else {
                                                bodyTv.setText(R.string.regulation_not_found);
                                            }
                                        } else {
                                            String body = doc.getString("body");
                                            if (!TextUtils.isEmpty(body)) {
                                                bodyTv.setText(body);
                                            } else {
                                                bodyTv.setText(R.string.regulation_not_found);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                                ": Failed to load regulation", e);
                                        String body = doc.getString("body");
                                        if (!TextUtils.isEmpty(body)) {
                                            bodyTv.setText(body);
                                        } else {
                                            Toast.makeText(this, R.string.regulation_load_error, Toast.LENGTH_LONG).show();
                                        }
                                    });
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
            Toast.makeText(this, getString(R.string.tournament_not_found), Toast.LENGTH_LONG).show();
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                    Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                    ": user not logged-in");
            Toast.makeText(this, getString(R.string.must_be_logged_in), Toast.LENGTH_LONG).show();
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
                        
                        // Track successful tournament join
                        analyticsManager.trackTournamentJoinSuccess(tournamentId);
                        analyticsManager.addTournamentBreadcrumb("join_success", tournamentId, "regulation_accepted");
                        
                        Toast.makeText(this, getString(R.string.joined_wait_for_bracket), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        String errorCode = "unknown";
                        String errorMessage = e.getMessage();
                        
                        if (e instanceof FirebaseFunctionsException ffe) {
                            errorCode = ffe.getCode().name();
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName()
                                    + ": code=" + ffe.getCode()
                                    + "  msg=" + ffe.getMessage()
                                    + "  details=" + ffe.getDetails());
                        }
                        
                        // Track tournament join error
                        analyticsManager.trackTournamentJoinError(tournamentId, errorCode, errorMessage);
                        analyticsManager.addTournamentBreadcrumb("join_error", tournamentId, "error=" + errorCode + ", msg=" + errorMessage);
                        
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                                ": joinTournament failed", e);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
