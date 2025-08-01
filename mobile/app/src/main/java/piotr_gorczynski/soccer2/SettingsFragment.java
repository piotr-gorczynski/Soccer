package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.util.Log;
import android.content.Intent;
import android.net.Uri;
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
                    boolean blockInvites = (Boolean) newValue;
                    updateBlockInviteInFirestore(blockInvites);
                    return true;
                });
            } else {
                // Disable option when user not logged in or backend unavailable
                blockInvitePreference.setEnabled(false);
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
        CheckBoxPreference adsConsentPref = findPreference("ads_consent");
        if (adsConsentPref != null) {
            updateAdsConsentCheckbox(adsConsentPref);
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
}
