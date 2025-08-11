package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity base class that applies the user's preferred language before any UI
 * components are created. This ensures all Activities display strings in the
 * selected locale as soon as they are launched.
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        String code = LanguageManager.getCurrentLanguageCode(newBase);
        Context context = LanguageManager.applyLanguage(newBase, code);
        super.attachBaseContext(context);
    }
}
