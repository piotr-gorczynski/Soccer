package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.util.Log;
import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.pref_android_level, rootKey);
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        // Setup block invite friend preference
        CheckBoxPreference blockInvitePreference = findPreference("block_invite_friend");
        if (blockInvitePreference != null) {
            // Load current value from Firestore
            loadBlockInvitePreference(blockInvitePreference);

            // Listen for changes
            blockInvitePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean blockInvites = (Boolean) newValue;
                updateBlockInviteInFirestore(blockInvites);
                return true;
            });
        }

        // Setup ad consent preference
        Preference adsConsentPref = findPreference("ads_consent");
        if (adsConsentPref != null) {
            adsConsentPref.setOnPreferenceClickListener(pref -> {
                Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".onCreatePreferences: ads_consent clicked"
                );
                SoccerApp app = (SoccerApp) requireActivity().getApplication();
                app.requestConsent(requireActivity());
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
}
