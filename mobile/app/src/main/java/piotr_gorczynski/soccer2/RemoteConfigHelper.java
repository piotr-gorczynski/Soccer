package piotr_gorczynski.soccer2;

import android.content.Context;
import android.util.Log;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

/**
 * Remote Config helper for A/B testing registration prompts and copy
 * Implements experimentation features suggested in the user research plan
 */
public class RemoteConfigHelper {
    private static final String TAG = "RemoteConfigHelper";
    
    // Remote Config parameter keys
    public static final String SIGNUP_PROMPT_VARIANT = "signup_prompt_variant";
    public static final String REGISTRATION_COPY_VARIANT = "registration_copy_variant";
    public static final String SHOW_DECLINE_DIALOG = "show_decline_dialog";
    
    // Default values
    private static final String DEFAULT_SIGNUP_PROMPT = "after_first_online_click";
    private static final String DEFAULT_REGISTRATION_COPY = "save_nickname_ranking";
    private static final boolean DEFAULT_SHOW_DECLINE_DIALOG = true;
    
    private final FirebaseRemoteConfig remoteConfig;
    
    public RemoteConfigHelper(Context context) {
        remoteConfig = FirebaseRemoteConfig.getInstance();
        
        // Configure Remote Config
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour for production, shorter for development
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);
        
        // Set default values
        remoteConfig.setDefaultsAsync(getDefaults());
        
        // Fetch and activate
        fetchAndActivate();
        
        Log.d(TAG, "RemoteConfigHelper initialized");
    }
    
    /**
     * Get the signup prompt timing variant
     * Possible values: "on_open", "after_first_online_click", "after_1_match", "only_when_joining_tournament"
     */
    public String getSignupPromptVariant() {
        return remoteConfig.getString(SIGNUP_PROMPT_VARIANT);
    }
    
    /**
     * Get the registration copy variant
     * Possible values: "save_nickname_ranking", "play_tournaments_global", "secure_account_benefits"
     */
    public String getRegistrationCopyVariant() {
        return remoteConfig.getString(REGISTRATION_COPY_VARIANT);
    }
    
    /**
     * Whether to show the decline reason dialog
     */
    public boolean shouldShowDeclineDialog() {
        return remoteConfig.getBoolean(SHOW_DECLINE_DIALOG);
    }
    
    /**
     * Get registration message based on variant
     */
    public String getRegistrationMessage(Context context) {
        String variant = getRegistrationCopyVariant();
        switch (variant) {
            case "play_tournaments_global":
                return context.getString(R.string.register_benefits_message);
            case "secure_account_benefits": 
                return "Secure your account and never lose your progress!";
            case "save_nickname_ranking":
            default:
                return context.getString(R.string.save_progress_message);
        }
    }
    
    /**
     * Get registration title based on variant
     */
    public String getRegistrationTitle(Context context) {
        String variant = getRegistrationCopyVariant();
        switch (variant) {
            case "play_tournaments_global":
                return context.getString(R.string.register_benefits_title);
            case "secure_account_benefits":
                return "Secure Your Account";
            case "save_nickname_ranking":
            default:
                return context.getString(R.string.save_progress_title);
        }
    }
    
    /**
     * Check if should show signup prompt based on timing variant and trigger
     */
    public boolean shouldShowSignupPrompt(String trigger, int matchesPlayed, boolean hasClickedOnline) {
        String variant = getSignupPromptVariant();
        
        switch (variant) {
            case "on_open":
                return "app_open".equals(trigger);
            case "after_first_online_click":
                return hasClickedOnline && ("online_feature_click".equals(trigger) || "tournament_view".equals(trigger));
            case "after_1_match":
                return matchesPlayed >= 1 && "match_end".equals(trigger);
            case "only_when_joining_tournament":
                return "tournament_join".equals(trigger);
            default:
                return "tournament_join".equals(trigger); // Safe default
        }
    }
    
    /**
     * Fetch and activate remote config
     */
    private void fetchAndActivate() {
        remoteConfig.fetchAndActivate()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "Remote config fetch successful. Updated: " + updated);
                        Log.d(TAG, "Signup prompt variant: " + getSignupPromptVariant());
                        Log.d(TAG, "Registration copy variant: " + getRegistrationCopyVariant());
                    } else {
                        Log.w(TAG, "Remote config fetch failed", task.getException());
                    }
                });
    }
    
    /**
     * Get default Remote Config values
     */
    private java.util.Map<String, Object> getDefaults() {
        java.util.Map<String, Object> defaults = new java.util.HashMap<>();
        defaults.put(SIGNUP_PROMPT_VARIANT, DEFAULT_SIGNUP_PROMPT);
        defaults.put(REGISTRATION_COPY_VARIANT, DEFAULT_REGISTRATION_COPY);
        defaults.put(SHOW_DECLINE_DIALOG, DEFAULT_SHOW_DECLINE_DIALOG);
        return defaults;
    }
}