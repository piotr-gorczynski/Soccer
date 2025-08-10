package piotr_gorczynski.soccer2;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class LanguageSelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
        String[] languages = LanguageManager.getAvailableLanguages();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_language);
        builder.setCancelable(false); // Force user to select a language
        builder.setSingleChoiceItems(languages, 0, (dialog, which) -> {
            String selectedLanguage = languages[which];
            String languageCode = LanguageManager.getLanguageCode(selectedLanguage);
            
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