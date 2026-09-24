package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

/** One app-wide reminder, checked on navigation and after other dialogs close. */
final class PrizeReminderHelper {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final PrizeReminderPolicy policy = new PrizeReminderPolicy();
    private Activity resumedActivity;
    private AlertDialog dialog;
    private int generation;

    void startSession() {
        policy.startSession();
    }

    void onResume(Activity activity) {
        resumedActivity = activity;
        generation++;
        onWindowFocusGained(activity);
    }

    void onWindowFocusGained(Activity activity) {
        if (resumedActivity != activity) return;
        int requestGeneration = generation;
        if (!isEligibleScreen(activity) || !AppFlavourDetector.supportsPrizeFeatures(activity)) return;
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> check(activity, requestGeneration), 1000);
    }

    void onPause(Activity activity) {
        if (resumedActivity != activity) return;
        resumedActivity = null;
        generation++;
        handler.removeCallbacksAndMessages(null);
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private boolean isEligibleScreen(Activity activity) {
        return activity instanceof MenuActivity
                || activity instanceof TournamentsActivity
                || activity instanceof TournamentLobbyActivity
                || activity instanceof RankingActivity
                || activity instanceof FriendsListActivity
                || activity instanceof InvitationsActivity
                || activity instanceof SettingsActivity
                || activity instanceof AccountActivity;
    }

    private boolean canShow(Activity activity, int requestGeneration, String uid) {
        return resumedActivity == activity && generation == requestGeneration
                && !activity.isFinishing() && !activity.isDestroyed()
                && activity.hasWindowFocus() && dialog == null
                && uid.equals(FirebaseAuth.getInstance().getUid())
                && ((SoccerApp) activity.getApplication()).isBackendAvailable();
    }

    private void check(Activity activity, int requestGeneration) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || !canShow(activity, requestGeneration, uid)) return;
        if (!policy.canRemind(uid)) return;

        // Filter status locally to reuse the existing userId query without a new index.
        // Server-only reads avoid reminding about details already submitted on another device.
        FirebaseFirestore.getInstance().collection("payments")
                .whereEqualTo("userId", uid).get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    if (!canShow(activity, requestGeneration, uid)) return;
                    if (!policy.canRemind(uid)) return;
                    for (DocumentSnapshot payment : snapshot.getDocuments()) {
                        if (!PrizeReminderPolicy.needsDetails(payment.getString("status"))) continue;
                        dialog = new AlertDialog.Builder(activity)
                                .setTitle(R.string.prize_reminder_title)
                                .setMessage(R.string.prize_reminder_message)
                                .setPositiveButton(R.string.prize_reminder_open, (ignored, which) -> {
                                    if (uid.equals(FirebaseAuth.getInstance().getUid())) {
                                        activity.startActivity(new Intent(activity, PrizeDetailsActivity.class)
                                                .putExtra("paymentId", payment.getId()));
                                    }
                                })
                                .setNegativeButton(R.string.close, null)
                                .create();
                        dialog.setOnDismissListener(ignored -> dialog = null);
                        dialog.show();
                        policy.markShown(uid);
                        break;
                    }
                })
                .addOnFailureListener(error -> Log.w("TAG_Soccer", "Prize reminder check failed", error));
    }
}
