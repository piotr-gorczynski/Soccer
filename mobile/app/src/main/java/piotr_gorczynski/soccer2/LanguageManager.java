package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;
import androidx.preference.PreferenceManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    
    private static final String TAG = "LanguageManager";
    private static final String PREF_LANGUAGE_CODE = "language_code";
    
    // Language code to display name resource mapping
    private static final Map<String, Integer> LANGUAGE_NAME_RES_IDS = new HashMap<>();
    static {
        LANGUAGE_NAME_RES_IDS.put("en", R.string.language_english);
        LANGUAGE_NAME_RES_IDS.put("pl", R.string.language_polish);
        LANGUAGE_NAME_RES_IDS.put("de", R.string.language_german);
        LANGUAGE_NAME_RES_IDS.put("fr", R.string.language_french);
        LANGUAGE_NAME_RES_IDS.put("es", R.string.language_spanish);
        LANGUAGE_NAME_RES_IDS.put("ur", R.string.language_urdu);
        LANGUAGE_NAME_RES_IDS.put("bn", R.string.language_bengali);
        LANGUAGE_NAME_RES_IDS.put("ne", R.string.language_nepali);
        LANGUAGE_NAME_RES_IDS.put("hi", R.string.language_hindi);
    }
    
    // Language display name to code mapping
    private static final Map<String, String> LANGUAGE_CODES = new HashMap<>();
    static {
        LANGUAGE_CODES.put("English", "en");
        LANGUAGE_CODES.put("Polish", "pl");
        LANGUAGE_CODES.put("German", "de");
        LANGUAGE_CODES.put("French", "fr");
        LANGUAGE_CODES.put("Spanish", "es");
        LANGUAGE_CODES.put("Urdu", "ur");
        LANGUAGE_CODES.put("Bengali", "bn");
        LANGUAGE_CODES.put("Nepali", "ne");
        LANGUAGE_CODES.put("Hindi", "hi");
    }
    
    /**
     * Get the currently selected language code from preferences
     */
    public static String getCurrentLanguageCode(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_LANGUAGE_CODE, "en"); // Default to English
    }
    
    /**
     * Get the display name for the current language
     */
    public static String getCurrentLanguageName(Context context) {
        String code = getCurrentLanguageCode(context);
        Integer resId = LANGUAGE_NAME_RES_IDS.get(code);
        return context.getString(resId != null ? resId : R.string.language_english);
    }
    
    /**
     * Set the language preference and apply it
     */
    public static void setLanguage(Context context, String languageCode) {
        // Save to SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString(PREF_LANGUAGE_CODE, languageCode).apply();
        
        // Save to Firestore if user is logged in
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("language", languageCode)
                    .addOnSuccessListener(aVoid ->
                            Log.d(TAG, "Language preference updated in Firestore: " + languageCode))
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Failed to update language preference in Firestore", e));
        }

        // Apply the language change to both the current and application contexts so
        // that all running activities receive the updated configuration immediately.
        Context appContext = context.getApplicationContext();
        applyLanguage(appContext, languageCode);
        if (appContext != context) {
            applyLanguage(context, languageCode);
        }
    }
    
    /**
     * Apply the language to the current context
     */
    public static Context applyLanguage(Context context, String languageCode) {
        Locale locale = Locale.forLanguageTag(languageCode);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);

        // Return a context with the updated configuration. This is particularly
        // important when attaching a base context for a newly created activity.
        return context.createConfigurationContext(config);
    }
    
    /**
     * Get all available languages as display names (localized)
     */
    public static String[] getAvailableLanguages(Context context) {
        return new String[]{
            context.getString(R.string.language_english),
            context.getString(R.string.language_polish),
            context.getString(R.string.language_german),
            context.getString(R.string.language_french),
            context.getString(R.string.language_spanish),
            context.getString(R.string.language_urdu),
            context.getString(R.string.language_bengali),
            context.getString(R.string.language_nepali),
            context.getString(R.string.language_hindi)
        };
    }
    
    /**
     * Get all available languages as display names (non-localized for consistent mapping)
     */
    public static String[] getAvailableLanguages() {
        return new String[]{
            "English", "Polish", "German", "French", "Spanish", 
            "Urdu", "Bengali", "Nepali", "Hindi"
        };
    }
    
    /**
     * Get language code from display name
     */
    public static String getLanguageCode(String displayName) {
        return LANGUAGE_CODES.getOrDefault(displayName, "en");
    }
    
    /**
     * Check if language preference has been set
     */
    public static boolean isLanguageSet(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.contains(PREF_LANGUAGE_CODE);
    }
    
    /**
     * Load language preference from Firestore for logged-in users
     */
    public static void loadLanguageFromFirestore(Context context) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            // Capture the language code at the time of the request so that if the
            // user changes the language before the Firestore response arrives we
            // don't override their newer preference with stale data.
            final String initialCode = getCurrentLanguageCode(context);

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("language")) {
                        String languageCode = doc.getString("language");
                        if (languageCode != null && LANGUAGE_NAME_RES_IDS.containsKey(languageCode)) {
                            String currentCode = getCurrentLanguageCode(context);

                            // Only apply the Firestore value if the language hasn't been
                            // changed locally since the request was initiated.
                            if (currentCode.equals(initialCode) && !currentCode.equals(languageCode)) {
                                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                                prefs.edit().putString(PREF_LANGUAGE_CODE, languageCode).apply();
                                applyLanguage(context, languageCode);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                    Log.e(TAG, "Failed to load language preference from Firestore", e));
        }
    }
}