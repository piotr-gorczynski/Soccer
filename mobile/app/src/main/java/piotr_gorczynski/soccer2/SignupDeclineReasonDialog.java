package piotr_gorczynski.soccer2;

import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;

/**
 * Dialog to collect user feedback on why they don't want to register
 * Implements one-tap research as suggested in the ChatGPT response
 */
public class SignupDeclineReasonDialog {
    private static final String TAG = "SignupDeclineDialog";
    
    public interface OnReasonSelectedListener {
        void onReasonSelected(String reason);
    }
    
    /**
     * Show the decline reason dialog
     * @param context Activity context
     * @param trigger What triggered this dialog ("tournament_join", "signup_dismiss", etc.)
     * @param analyticsManager Analytics manager for tracking
     * @param listener Callback for when reason is selected
     */
    public static void show(Context context, String trigger, AnalyticsManager analyticsManager, OnReasonSelectedListener listener) {
        // Create custom dialog layout
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_signup_decline_reason, null);
        
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.signup_decline_dialog_title)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        
        // Set up button click listeners
        setupButtonListener(dialogView, R.id.btn_just_playing_offline, "just_playing_offline", dialog, analyticsManager, listener);
        setupButtonListener(dialogView, R.id.btn_privacy_concerns, "privacy_concerns", dialog, analyticsManager, listener);
        setupButtonListener(dialogView, R.id.btn_too_complicated, "too_complicated", dialog, analyticsManager, listener);
        setupButtonListener(dialogView, R.id.btn_dont_use_providers, "dont_use_google_email", dialog, analyticsManager, listener);
        setupButtonListener(dialogView, R.id.btn_other_reason, "other", dialog, analyticsManager, listener);
        
        // Track that the dialog was shown
        analyticsManager.addAuthBreadcrumb("decline_reason_dialog_shown", "trigger=" + trigger);
        
        dialog.show();
        Log.d(TAG, "Signup decline reason dialog shown with trigger: " + trigger);
    }
    
    private static void setupButtonListener(View dialogView, int buttonId, String reason, 
                                          Dialog dialog, AnalyticsManager analyticsManager, 
                                          OnReasonSelectedListener listener) {
        Button button = dialogView.findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(v -> {
                // Track the reason selection
                analyticsManager.trackSignupDeclineReason(reason);
                analyticsManager.addAuthBreadcrumb("decline_reason_selected", reason);
                
                // Notify listener
                if (listener != null) {
                    listener.onReasonSelected(reason);
                }
                
                // Close dialog
                dialog.dismiss();
                
                Log.d(TAG, "User selected decline reason: " + reason);
            });
        }
    }
}