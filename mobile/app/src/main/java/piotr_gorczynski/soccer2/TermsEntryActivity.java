package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

/** Check notification entry points before constructing their protected screen. */
public class TermsEntryActivity extends BaseActivity {
    private static final String DESTINATION = "terms_destination";
    private boolean checking;

    public static Intent wrap(Context context, Intent destination) {
        return new Intent(context, TermsEntryActivity.class)
                .putExtra(DESTINATION, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override protected void onResume() {
        super.onResume();
        if (checking || isFinishing()) return;
        String uid = FirebaseAuth.getInstance().getUid();
        Intent destination = getIntent().getParcelableExtra(DESTINATION);
        if (uid == null || !isAllowedDestination(destination)) {
            startActivity(new Intent(this, MenuActivity.class));
            finish();
            return;
        }
        checking = true;
        TermsPolicy policy = TermsPolicy.required(AppFlavourDetector.isBangladeshFlavour(this),
                LanguageManager.getCurrentLanguageCode(this));
        new TermsRepository(FirebaseFirestore.getInstance()).accepted(uid, policy)
                .addOnCompleteListener(task -> {
                    checking = false;
                    if (isFinishing() || isDestroyed()) return;
                    if (!uid.equals(FirebaseAuth.getInstance().getUid())) {
                        startActivity(new Intent(this, MenuActivity.class));
                        finish();
                    } else if (task.isSuccessful() && Boolean.TRUE.equals(task.getResult())) {
                        startActivity(destination);
                        finish();
                    } else {
                        startActivity(new Intent(this, TermsActivity.class));
                    }
                });
    }

    static boolean isAllowedDestination(Intent destination) {
        if (destination == null || destination.getComponent() == null) return false;
        String name = destination.getComponent().getClassName();
        return BuildConfig.APPLICATION_ID.equals(destination.getComponent().getPackageName())
                && (PrizeDetailsActivity.class.getName().equals(name)
                || TournamentResultsActivity.class.getName().equals(name)
                || TournamentLobbyActivity.class.getName().equals(name)
                || InvitationsActivity.class.getName().equals(name));
    }
}
