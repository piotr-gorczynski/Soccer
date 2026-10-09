package piotr_gorczynski.soccer2;

import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.Map;

/** Shared player actions. Identity and nickname are validated/read by the server. */
final class PlayerModeration {
    static void bind(View row, String uid, String screen) {
        View button = row.findViewById(R.id.playerActions);
        if (button == null) return;
        var user = FirebaseAuth.getInstance().getCurrentUser();
        button.setVisibility(user == null || uid == null || uid.equals(user.getUid()) ? View.GONE : View.VISIBLE);
        button.setOnClickListener(v -> show(row.getContext(), uid, screen));
    }
    private static boolean active(Context context) {
        while (context instanceof android.content.ContextWrapper) {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                return !activity.isFinishing() && !activity.isDestroyed();
            }
            context = ((android.content.ContextWrapper) context).getBaseContext();
        }
        return false;
    }
    private static void show(Context context, String uid, String screen) {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("users").document(user.getUid())
            .collection("blocks").document(uid).get(Source.SERVER).addOnSuccessListener(block -> {
                if (!active(context) || FirebaseAuth.getInstance().getCurrentUser() == null
                    || !user.getUid().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) return;
                new AlertDialog.Builder(context).setTitle(R.string.moderation_actions)
                    .setItems(new String[]{context.getString(R.string.moderation_report),
                        context.getString(block.exists() ? R.string.moderation_unblock : R.string.moderation_block)},
                        (dialog, which) -> {
                            if (which == 0) report(context, uid, screen);
                            else setBlock(context, uid, !block.exists());
                        }).show();
            }).addOnFailureListener(e -> fail(context));
    }
    private static void report(Context context, String uid, String screen) {
        EditText reason = new EditText(context);
        reason.setHint(R.string.moderation_reason);
        reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1000)});
        AlertDialog dialog = new AlertDialog.Builder(context).setTitle(R.string.moderation_report)
            .setView(reason).setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.payment_support_send, null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = reason.getText().toString().trim();
            if (text.isEmpty()) { reason.setError(context.getString(R.string.moderation_reason)); return; }
            v.setEnabled(false);
            FirebaseFunctions.getInstance("us-central1").getHttpsCallable("reportPlayer")
                .call(Map.of("reportedUid", uid, "reason", text, "context", screen))
                .addOnSuccessListener(result -> {
                    Map<?, ?> data = (Map<?, ?>) result.getData();
                    Toast.makeText(context, context.getString(R.string.payment_support_sent, data.get("reference")), Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                }).addOnFailureListener(e -> { v.setEnabled(true); fail(context); });
        }));
        dialog.show();
    }
    private static void setBlock(Context context, String uid, boolean blocked) {
        FirebaseFunctions.getInstance("us-central1").getHttpsCallable("setPlayerBlock")
            .call(Map.of("reportedUid", uid, "blocked", blocked))
            .addOnSuccessListener(result -> Toast.makeText(context,
                blocked ? R.string.moderation_blocked : R.string.moderation_unblocked, Toast.LENGTH_LONG).show())
            .addOnFailureListener(e -> fail(context));
    }
    static void showBlocked(Context context) {
        var user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        FirebaseFirestore.getInstance().collection("users").document(user.getUid()).collection("blocks")
            .get(Source.SERVER).addOnSuccessListener(snapshot -> {
                if (!active(context)) return;
                if (snapshot.isEmpty()) { Toast.makeText(context, R.string.moderation_empty, Toast.LENGTH_SHORT).show(); return; }
                String[] ids = snapshot.getDocuments().stream().map(d -> d.getId()).toArray(String[]::new);
                java.util.List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.DocumentSnapshot>> names = new java.util.ArrayList<>();
                for (String id : ids) names.add(FirebaseFirestore.getInstance().collection("users").document(id).get(Source.SERVER));
                com.google.android.gms.tasks.Tasks.whenAllSuccess(names).addOnSuccessListener(results -> {
                    if (!active(context)) return;
                    String[] labels = new String[results.size()];
                    for (int i = 0; i < results.size(); i++) {
                        var profile = (com.google.firebase.firestore.DocumentSnapshot) results.get(i);
                        labels[i] = UserDisplayName.from(profile);
                        if (labels[i] == null) labels[i] = context.getString(R.string.moderation_unknown_player);
                    }
                    new AlertDialog.Builder(context).setTitle(R.string.moderation_unblock)
                        .setItems(labels, (dialog, index) -> setBlock(context, ids[index], false))
                        .setNegativeButton(R.string.cancel, null).show();
                }).addOnFailureListener(e -> fail(context));
            }).addOnFailureListener(e -> fail(context));
    }
    private static void fail(Context context) {
        Toast.makeText(context, R.string.moderation_failed, Toast.LENGTH_LONG).show();
    }
}
