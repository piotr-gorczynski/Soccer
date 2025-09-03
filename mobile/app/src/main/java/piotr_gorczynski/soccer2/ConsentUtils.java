package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;

public class ConsentUtils {

    /**
     * Determine if the user allowed personalised ads based on the
     * Transparency & Consent Framework string stored by UMP.
     */
    public static boolean isPersonalisedAllowed(Context ctx) {
        // UMP writes the IAB keys into the default shared‑prefs file
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        String purposes = sp.getString("IABTCF_PurposeConsents", "");
        Log.d("TAG_Soccer", "purpose string = \"" + purposes + "\"");

        // Purposes are 1‑based → purpose 4 (“Select personalised ads”) ⇒ index 3
        return purposes.length() >= 4 && purposes.charAt(3) == '1';
    }

    /**
     * Determine if the user allowed analytics storage based on the
     * Transparency & Consent Framework string stored by UMP.
     */
    public static boolean isAnalyticsAllowed(Context ctx) {
        // UMP writes the IAB keys into the default shared‑prefs file
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        String purposes = sp.getString("IABTCF_PurposeConsents", "");
        Log.d("TAG_Soccer", "analytics consent - purpose string = \"" + purposes + "\"");

        // Purpose 1 ("Store and/or access information on a device") -> index 0
        // This purpose is typically required for analytics storage
        return purposes.length() >= 1 && purposes.charAt(0) == '1';
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
     * Update Firebase Analytics consent based on UMP consent choices
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
