package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.UserMessagingPlatform;

import java.util.Map;

public class ConsentUtils {

    /**
     * Determine if the user allowed personalised ads based on the
     * Transparency & Consent Framework string stored by UMP for EEA regulations,
     * or UMP consent status for US state regulations.
     */
    public static boolean isPersonalisedAllowed(Context ctx) {
        // First, check IAB TCF strings (for EEA regulations)
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String purposes = sp.getString("IABTCF_PurposeConsents", "");
        
        Log.d("TAG_Soccer", "Checking personalized ads consent - IAB TCF string = \"" + purposes + "\"");

        // If IAB TCF data is available, use it (EEA regulations)
        if (!purposes.isEmpty()) {
            // Purposes are 1-based -> purpose 4 ("Select personalised ads") => index 3
            boolean iabConsent = purposes.length() >= 4 && purposes.charAt(3) == '1';
            Log.d("TAG_Soccer", "Using IAB TCF consent for personalized ads: " + iabConsent);
            return iabConsent;
        }

        // Fall back to UMP consent status (for US state regulations)
        boolean umpConsent = hasUmpConsent(ctx);
        Log.d("TAG_Soccer", "Using UMP consent status for personalized ads (US regulations): " + umpConsent);
        return umpConsent;
    }

    /**
     * Determine if the user allowed analytics storage based on the
     * Transparency & Consent Framework string stored by UMP for EEA regulations,
     * or UMP consent status for US state regulations.
     */
    public static boolean isAnalyticsAllowed(Context ctx) {
        // First, check IAB TCF strings (for EEA regulations)
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        String purposes = sp.getString("IABTCF_PurposeConsents", "");
        
        Log.d("TAG_Soccer", "Checking analytics consent - IAB TCF string = \"" + purposes + "\"");

        // If IAB TCF data is available, use it (EEA regulations)
        if (!purposes.isEmpty()) {
            // Purpose 1 ("Store and/or access information on a device") -> index 0
            // This purpose is typically required for analytics storage
            boolean iabConsent = purposes.length() >= 1 && purposes.charAt(0) == '1';
            Log.d("TAG_Soccer", "Using IAB TCF consent for analytics: " + iabConsent);
            return iabConsent;
        }

        // Fall back to UMP consent status (for US state regulations)
        boolean umpConsent = hasUmpConsent(ctx);
        Log.d("TAG_Soccer", "Using UMP consent status for analytics (US regulations): " + umpConsent);
        return umpConsent;
    }

    /**
     * Check if user has provided consent through UMP (for US state regulations).
     * This method works for both EEA and US regulations but is primarily used
     * as a fallback when IAB TCF data is not available.
     */
    public static boolean hasUmpConsent(Context ctx) {
        try {
            ConsentInformation consentInfo = UserMessagingPlatform.getConsentInformation(ctx);
            ConsentInformation.ConsentStatus status = consentInfo.getConsentStatus();
            
            Log.d("TAG_Soccer", "UMP consent status: " + status);
            
            // OBTAINED means user has provided consent
            // NOT_REQUIRED means consent is not required (e.g., user not in regulated region)
            return status == ConsentInformation.ConsentStatus.OBTAINED || 
                   status == ConsentInformation.ConsentStatus.NOT_REQUIRED;
        } catch (Exception e) {
            Log.w("TAG_Soccer", "Error checking UMP consent status", e);
            return false;
        }
    }

    /**
     * Set default Firebase Analytics consent to DENIED for EEA compliance.
     * This should be called at app startup before any analytics data is collected.
     */
    public static void setDefaultFirebaseAnalyticsConsent(Context ctx) {
        Log.d("TAG_Soccer", "Setting default Firebase Analytics consent to DENIED for EEA compliance");

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(ctx);
        
        // Set consent to DENIED by default to ensure no data collection until explicit consent
        Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> defaultConsentMap = Map.of(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_STORAGE, FirebaseAnalytics.ConsentStatus.DENIED
        );

        firebaseAnalytics.setConsent(defaultConsentMap);
        
        Log.d("TAG_Soccer", "Default Firebase Analytics consent set to DENIED");
    }

    /**
     * Update Firebase Analytics consent based on UMP consent choices.
     * This method now supports both EEA regulations (via IAB TCF) and US state regulations (via UMP status).
     */
    public static void updateFirebaseAnalyticsConsent(Context ctx) {
        boolean analyticsAllowed = isAnalyticsAllowed(ctx);
        boolean adsPersonalizationAllowed = isPersonalisedAllowed(ctx);

        Log.d("TAG_Soccer", "Updating Firebase Analytics consent: analytics=" + analyticsAllowed + 
              ", ads_personalization=" + adsPersonalizationAllowed);

        FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(ctx);
        
        // Set consent for Firebase Analytics
        Map<FirebaseAnalytics.ConsentType, FirebaseAnalytics.ConsentStatus> consentMap = Map.of(
            FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE,
            analyticsAllowed ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED,
            FirebaseAnalytics.ConsentType.AD_STORAGE,
            adsPersonalizationAllowed ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED
        );

        firebaseAnalytics.setConsent(consentMap);
        
        Log.d("TAG_Soccer", "Firebase Analytics consent updated successfully");
    }

    private ConsentUtils() {
        // no instances
    }
}