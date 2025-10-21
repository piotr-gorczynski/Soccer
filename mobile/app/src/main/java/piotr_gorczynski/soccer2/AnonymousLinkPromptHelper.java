package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Utility class for prompting anonymous users to link their accounts
 * Implements progressive prompting as suggested in the user research plan
 */
public class AnonymousLinkPromptHelper {
    private static final String TAG = "TAG_Soccer";
    
    /**
     * Show prompt to anonymous user to save their progress by linking account
     * @param context Activity context
     * @param trigger What triggered this prompt (tournament_join, win_match, pick_nickname)
     * @param analyticsManager Analytics manager for tracking
     */
    public static void showSaveProgressPrompt(Context context, String trigger, AnalyticsManager analyticsManager) {
        // Only show for anonymous users
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null || !auth.getCurrentUser().isAnonymous()) {
            return;
        }
        
        analyticsManager.trackAnonymousLinkPrompt(trigger);
        analyticsManager.addAuthBreadcrumb("anonymous_link_prompt_shown", "trigger=" + trigger);
        
        new AlertDialog.Builder(context)
                .setTitle(R.string.save_progress_title)
                .setMessage(R.string.save_progress_message)
                .setPositiveButton(R.string.save_progress_button, (dialog, which) -> {
                    analyticsManager.trackAnonymousLinkDecision("link", trigger);
                    analyticsManager.addAuthBreadcrumb("anonymous_link_decision", "link, trigger=" + trigger);
                    
                    // Open the link account activity
                    Intent intent = new Intent(context, LinkAccountActivity.class);
                    context.startActivity(intent);
                })
                .setNegativeButton(R.string.maybe_later, (dialog, which) -> {
                    analyticsManager.trackAnonymousLinkDecision("later", trigger);
                    analyticsManager.addAuthBreadcrumb("anonymous_link_decision", "later, trigger=" + trigger);
                    dialog.dismiss();
                })
                .setOnCancelListener(dialog -> {
                    analyticsManager.trackAnonymousLinkDecision("dismiss", trigger);
                    analyticsManager.addAuthBreadcrumb("anonymous_link_decision", "dismiss, trigger=" + trigger);
                })
                .show();
        
        Log.d(TAG, "Showed save progress prompt with trigger: " + trigger);
    }
    
    /**
     * Check if we should show the prompt based on trigger and user state
     * This could be enhanced with frequency limits, user preferences, etc.
     */
    public static boolean shouldShowPrompt(String trigger) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        // Only show for anonymous users
        if (auth.getCurrentUser() == null || !auth.getCurrentUser().isAnonymous()) {
            return false;
        }
        
        // For now, always show (could add frequency limits later)
        return true;
    }
}