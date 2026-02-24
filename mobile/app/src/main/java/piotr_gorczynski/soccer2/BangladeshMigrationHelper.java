package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.preference.PreferenceManager;

import java.util.Locale;

/**
 * Helper class for managing Bangladesh version migration promotion.
 * This class handles detection of Bangladesh users and showing migration prompts
 * only in the global app flavor.
 */
public class BangladeshMigrationHelper {
    
    // Use app-wide tag so these logs are visible when filtering logcat by TAG_Soccer.
    private static final String TAG = "TAG_Soccer";
    private static final String PREF_BD_PROMO_DISMISSED = "bd_promo_dismissed";
    private static final String PREF_BD_PROMO_LAST_SHOWN = "bd_promo_last_shown_ms";
    private static final String PREF_BD_PROMO_DISMISS_COUNT = "bd_promo_dismiss_count";
    
    // Show again after 7 days if dismissed
    private static final long PROMO_RESHOW_DELAY_MS = 7L * 24 * 60 * 60 * 1000; // 7 days
    
    // Bangladesh version Play Store URL
    private static final String BD_PLAY_STORE_URL = 
        "https://play.google.com/store/apps/details?id=piotr_gorczynski.soccer2.bd";
    
    private static String normalizeCountryCode(String countryCode) {
        if (countryCode == null) {
            return "";
        }
        return countryCode.trim().toUpperCase(Locale.US);
    }

    private static String resolveCountryCode(Context context) {
        if (context != null) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                if (telephonyManager != null) {
                    String networkCountry = normalizeCountryCode(telephonyManager.getNetworkCountryIso());
                    if (!networkCountry.isEmpty()) {
                        return networkCountry;
                    }

                    String simCountry = normalizeCountryCode(telephonyManager.getSimCountryIso());
                    if (!simCountry.isEmpty()) {
                        return simCountry;
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Unable to read telephony country, falling back to locale", e);
            }
        }

        String localeCountry = normalizeCountryCode(Locale.getDefault().getCountry());
        if (!localeCountry.isEmpty()) {
            return localeCountry;
        }

        String localeDisplayCountry = normalizeCountryCode(Locale.getDefault(Locale.Category.DISPLAY).getCountry());
        if (!localeDisplayCountry.isEmpty()) {
            return localeDisplayCountry;
        }

        return "";
    }

    /**
     * Check if user is in Bangladesh based on network/SIM country (preferred)
     * and locale as fallback.
     *
     * @return true if user's device information indicates Bangladesh
     */
    public static boolean isUserInBangladesh() {
        return isUserInBangladesh(null);
    }

    /**
     * Check if user is in Bangladesh based on network/SIM country (preferred)
     * and locale as fallback.
     *
     * @param context Android context (optional, but improves detection)
     * @return true if user's device information indicates Bangladesh
     */
    public static boolean isUserInBangladesh(Context context) {
        String countryCode = resolveCountryCode(context);
        Log.d(TAG, "BangladeshMigrationHelper.isUserInBangladesh: Device country code: " + countryCode);
        return "BD".equals(countryCode);
    }
    
    /**
     * Check if the Bangladesh promotion should be shown to the user.
     * 
     * Criteria:
     * - Must be running global app flavor (not Bangladesh flavor)
     * - User must be in Bangladesh (based on locale)
     * - User hasn't permanently dismissed the promo
     * - If dismissed, must have been at least 7 days ago
     * 
     * @param context Android context
     * @return true if promotion should be shown
     */
    public static boolean shouldShowPromotion(Context context) {
        // Only show in global flavor
        if (!AppFlavourDetector.isGlobalFlavour(context)) {
            Log.d(TAG, "BangladeshMigrationHelper.shouldShowPromotion: Not showing promo: not global flavor");
            return false;
        }
        
        // Only show to Bangladesh users
        if (!isUserInBangladesh(context)) {
            Log.d(TAG, "BangladeshMigrationHelper.shouldShowPromotion: Not showing promo: user not in Bangladesh");
            return false;
        }
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        // Check if permanently dismissed (after multiple dismissals)
        int dismissCount = prefs.getInt(PREF_BD_PROMO_DISMISS_COUNT, 0);
        if (dismissCount >= 3) {
            Log.d(TAG, "BangladeshMigrationHelper.shouldShowPromotion: Not showing promo: dismissed " + dismissCount + " times (permanent)");
            return false;
        }
        
        // Check if temporarily dismissed
        boolean isDismissed = prefs.getBoolean(PREF_BD_PROMO_DISMISSED, false);
        if (isDismissed) {
            long lastShownMs = prefs.getLong(PREF_BD_PROMO_LAST_SHOWN, 0);
            long currentMs = System.currentTimeMillis();
            long timeSinceDismissMs = currentMs - lastShownMs;
            
            if (timeSinceDismissMs < PROMO_RESHOW_DELAY_MS) {
                Log.d(TAG, "BangladeshMigrationHelper.shouldShowPromotion: Not showing promo: dismissed recently (will show again in " + 
                    ((PROMO_RESHOW_DELAY_MS - timeSinceDismissMs) / (24 * 60 * 60 * 1000)) + " days)");
                return false;
            } else {
                // Time to show again
                Log.d(TAG, "BangladeshMigrationHelper.shouldShowPromotion: Time to show promo again (7 days passed)");
            }
        }
        
        Log.d(TAG, "BangladeshMigrationHelper.shouldShowPromotion: Should show Bangladesh promotion");
        return true;
    }
    
    /**
     * Mark the promotion as shown (for analytics/tracking).
     * 
     * @param context Android context
     */
    public static void markPromotionShown(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putLong(PREF_BD_PROMO_LAST_SHOWN, System.currentTimeMillis())
            .apply();
        Log.d(TAG, "Marked promotion as shown");
    }
    
    /**
     * Mark the promotion as dismissed by the user.
     * After 3 dismissals, the promotion will not be shown again.
     * 
     * @param context Android context
     */
    public static void markPromotionDismissed(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int currentCount = prefs.getInt(PREF_BD_PROMO_DISMISS_COUNT, 0);
        int newCount = currentCount + 1;
        
        prefs.edit()
            .putBoolean(PREF_BD_PROMO_DISMISSED, true)
            .putLong(PREF_BD_PROMO_LAST_SHOWN, System.currentTimeMillis())
            .putInt(PREF_BD_PROMO_DISMISS_COUNT, newCount)
            .apply();
        
        if (newCount >= 3) {
            Log.d(TAG, "Promotion permanently dismissed after " + newCount + " dismissals");
        } else {
            Log.d(TAG, "Promotion dismissed (count: " + newCount + "/3)");
        }
    }
    
    /**
     * Mark the promotion as accepted (user clicked Install).
     * This permanently dismisses the promotion.
     * 
     * @param context Android context
     */
    public static void markPromotionAccepted(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .putBoolean(PREF_BD_PROMO_DISMISSED, true)
            .putInt(PREF_BD_PROMO_DISMISS_COUNT, 999) // Mark as permanently accepted
            .apply();
        Log.d(TAG, "Promotion accepted, marked as permanently dismissed");
    }
    
    /**
     * Open the Bangladesh version in Google Play Store.
     * 
     * @param context Android context
     */
    public static void openBangladeshPlayStore(Context context) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(BD_PLAY_STORE_URL));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened Bangladesh Play Store listing");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open Play Store", e);
        }
    }
    
    /**
     * Reset all promotion preferences (for testing/debugging).
     * This will allow the promotion to be shown again.
     * 
     * @param context Android context
     */
    public static void resetPromotionState(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit()
            .remove(PREF_BD_PROMO_DISMISSED)
            .remove(PREF_BD_PROMO_LAST_SHOWN)
            .remove(PREF_BD_PROMO_DISMISS_COUNT)
            .apply();
        Log.d(TAG, "Reset promotion state for testing");
    }
}
