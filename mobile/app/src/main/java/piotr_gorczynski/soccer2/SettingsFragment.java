package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
import android.app.AlertDialog;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;


public class SettingsFragment extends PreferenceFragmentCompat {
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener consentChangeListener;
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_android_level, rootKey);
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        
        // Setup block invite friend preference
        CheckBoxPreference blockInvitePreference = findPreference("block_invite_friend");
        if (blockInvitePreference != null) {
            SoccerApp app = (SoccerApp) requireActivity().getApplication();
            boolean loggedIn = auth.getCurrentUser() != null;
            boolean backendAvailable = app.isBackendAvailable();

            if (loggedIn && backendAvailable) {
                // Load current value from Firestore
                loadBlockInvitePreference(blockInvitePreference);

                // Listen for changes when enabled
                blockInvitePreference.setOnPreferenceChangeListener((pref, newValue) -> {
                    if (!app.isBackendAvailable()) {
                        Log.e(
                                "TAG_Soccer",
                                getClass().getSimpleName() + ".onCreatePreferences: backend unavailable - ignoring block_invite change"
                        );
                        return false;
                    }
                    boolean blockInvites = (Boolean) newValue;
                    updateBlockInviteInFirestore(blockInvites);
                    return true;
                });
            } else {
                // Disable option when user not logged in or backend unavailable
                blockInvitePreference.setEnabled(false);
                Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".onCreatePreferences: block_invite disabled - loggedIn=" + loggedIn + ", backendAvailable=" + backendAvailable
                );
            }
        }

        // Setup ad consent preference
        CheckBoxPreference adsConsentPref = findPreference("ads_consent");
        if (adsConsentPref != null) {
            SoccerApp app = (SoccerApp) requireActivity().getApplication();
            boolean backendAvailable = app.isBackendAvailable();

            // Always display the current value
            updateAdsConsentCheckbox(adsConsentPref);

            if (backendAvailable) {
                adsConsentPref.setOnPreferenceChangeListener((pref, newValue) -> {
                    if (!app.isBackendAvailable()) {
                        Log.e(
                                "TAG_Soccer",
                                getClass().getSimpleName() + ".onCreatePreferences: backend unavailable - ignoring ads_consent click"
                        );
                        return false;
                    }
                    Log.d(
                            "TAG_Soccer",
                            getClass().getSimpleName() + ".onCreatePreferences: ads_consent clicked"
                    );
                    app.showAdsConsentForm(requireActivity());
                    // keep current value until consent form result is reflected
                    return false;
                });

                consentChangeListener = (sharedPreferences, key) -> {
                    if ("personalised_ads".equals(key)) {
                        updateAdsConsentCheckbox(adsConsentPref);
                    }
                };
                prefs.registerOnSharedPreferenceChangeListener(consentChangeListener);
            } else {
                // Disable option when backend unavailable
                adsConsentPref.setEnabled(false);
            }
        }

        Preference fbPref = findPreference("facebook_community");
        if (fbPref != null) {
            fbPref.setOnPreferenceClickListener(pref -> {
                Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.facebook.com/profile.php?id=61578949554301")
                );
                startActivity(intent);
                return true;
            });
        }

        // Setup language preference
        Preference languagePref = findPreference("language_preference");
        if (languagePref != null) {
            updateLanguagePreferenceSummary(languagePref);
            languagePref.setOnPreferenceClickListener(pref -> {
                showLanguageSelectionDialog();
                return true;
            });
        }
    }
    
    private void loadBlockInvitePreference(CheckBoxPreference preference) {
        String uid = auth.getUid();
        if (uid != null) {
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean blockInvites = doc.getBoolean("blockInviteFriend");
                        preference.setChecked(blockInvites != null ? blockInvites : false);
                    }
                })
                .addOnFailureListener(e ->
                    Log.e(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".loadBlockInvitePreference: Failed to load block invite preference",
                        e
                    )
                );
        }
    }
    
    private void updateBlockInviteInFirestore(boolean blockInvites) {
        SoccerApp app = (SoccerApp) requireActivity().getApplication();
        if (!app.isBackendAvailable()) {
            Log.e(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".updateBlockInviteInFirestore: backend unavailable - cannot save preference"
            );
            return;
        }
        String uid = auth.getUid();
        if (uid != null) {
            db.collection("users").document(uid)
                .update("blockInviteFriend", blockInvites)
                .addOnSuccessListener(aVoid ->
                    Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".updateBlockInviteInFirestore: Block invite preference updated: " + blockInvites
                    )
                )
                .addOnFailureListener(e ->
                    Log.e(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".updateBlockInviteInFirestore: Failed to update block invite preference",
                        e
                    )
                );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        SoccerApp app = (SoccerApp) requireActivity().getApplication();
        boolean backendAvailable = app.isBackendAvailable();

        CheckBoxPreference blockInvitePref = findPreference("block_invite_friend");
        if (blockInvitePref != null) {
            boolean loggedIn = auth.getCurrentUser() != null;
            blockInvitePref.setEnabled(loggedIn && backendAvailable);
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".onResume: block_invite enabled=" + blockInvitePref.isEnabled()
            );
        }

        CheckBoxPreference adsConsentPref = findPreference("ads_consent");
        if (adsConsentPref != null) {
            adsConsentPref.setEnabled(backendAvailable);
            updateAdsConsentCheckbox(adsConsentPref);
            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".onResume: ads_consent enabled=" + adsConsentPref.isEnabled()
            );
        }

        // Update language preference summary
        Preference languagePref = findPreference("language_preference");
        if (languagePref != null) {
            updateLanguagePreferenceSummary(languagePref);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (prefs != null && consentChangeListener != null) {
            prefs.unregisterOnSharedPreferenceChangeListener(consentChangeListener);
        }
    }

    private void updateAdsConsentCheckbox(CheckBoxPreference preference) {
        boolean personalised =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getBoolean("personalised_ads", false);

        preference.setChecked(personalised);

        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName()
                        + ".updateAdsConsentCheckbox: user chose "
                        + (personalised ? "PERSONALISED" : "NPA")
        );
    }

    private void updateLanguagePreferenceSummary(Preference preference) {
        String currentLanguage = LanguageManager.getCurrentLanguageName(requireContext());
        String summary = getString(R.string.current_language, currentLanguage);
        preference.setSummary(summary);
    }

    private void showLanguageSelectionDialog() {
        String[] languages = LanguageManager.getAvailableLanguages();
        String currentLanguage = LanguageManager.getCurrentLanguageName(requireContext());
        
        // Find current selection index
        int currentIndex = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLanguage)) {
                currentIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.select_language);
        builder.setSingleChoiceItems(languages, currentIndex, (dialog, which) -> {
            String selectedLanguage = languages[which];
            String languageCode = LanguageManager.getLanguageCode(selectedLanguage);
            
            // Set the language
            LanguageManager.setLanguage(requireContext(), languageCode);
            
            // Update the preference summary
            Preference languagePref = findPreference("language_preference");
            if (languagePref != null) {
                updateLanguagePreferenceSummary(languagePref);
            }
            
            dialog.dismiss();
            
            // Restart the activity to apply the language change
            requireActivity().recreate();
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }
}
