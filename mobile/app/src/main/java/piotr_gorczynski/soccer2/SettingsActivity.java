package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;


public class SettingsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.title_activity_settings);
        Log.d(
                "TAG_Soccer",
                getClass().getSimpleName() +
                        ".onCreate: toolbar title set to \"" +
                        Objects.requireNonNull(getSupportActionBar()).getTitle() +
                        "\""
        );

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    void changeLanguage(String languageCode) {
        if (languageCode.equals(LanguageManager.getCurrentLanguageCode(this))) return;

        // Finish before changing AppCompat locales, so this Activity is not also
        // scheduled for recreation. A fresh Activity gets the selected locale in
        // BaseActivity.attachBaseContext, including on Android 11 devices where
        // recreating the existing Settings screen can restore English resources.
        finish();
        LanguageManager.setLanguage(this, languageCode);
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

