package piotr_gorczynski.soccer2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class LanguageSelectionActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Switch from splash theme to normal theme
        setTheme(R.style.AppTheme);
        
        // Set the loading screen content view
        setContentView(R.layout.activity_language_selection);
        
        // Check if language is already set
        if (LanguageManager.isLanguageSet(this)) {
            // Language is already set, proceed to main activity
            proceedToMainActivity();
            return;
        }
        
        // Show language selection dialog
        showLanguageSelectionDialog();
    }

    private void showLanguageSelectionDialog() {
        // Check if activity is still valid before showing dialog
        if (isFinishing() || isDestroyed()) {
            Log.w("TAG_Soccer", getClass().getSimpleName() + ".showLanguageSelectionDialog: Activity finishing or destroyed, skipping dialog");
            return;
        }
        
        String[] languages = LanguageManager.getAvailableLanguages(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language);
        builder.setCancelable(false); // Force user to select a language
        builder.setSingleChoiceItems(languages, 0, (dialog, which) -> {
            // Get the selected localized language name
            String selectedLocalizedLanguage = languages[which];
            String languageCode = LanguageManager.getLanguageCodeFromLocalizedName(this, selectedLocalizedLanguage);

            Log.d(
                    "TAG_Soccer",
                    getClass().getSimpleName()
                            + ".showLanguageSelectionDialog: selectedLanguage="
                            + selectedLocalizedLanguage
                            + ", code="
                            + languageCode
            );

            // Set the language
            LanguageManager.setLanguage(this, languageCode);

            dialog.dismiss();
            proceedToMainActivity();
        });
        builder.show();
    }

    private void proceedToMainActivity() {
        Intent intent = new Intent(this, MenuActivity.class);
        startActivity(intent);
        finish();
    }
}