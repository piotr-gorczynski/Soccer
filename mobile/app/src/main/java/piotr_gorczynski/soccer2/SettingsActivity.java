package piotr_gorczynski.soccer2;

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

        String title = getString(R.string.settings);
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".onCreate: toolbar title set to " + title);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_container, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

