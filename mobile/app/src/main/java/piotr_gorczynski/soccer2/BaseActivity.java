package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity base class that applies the user's preferred language before any UI
 * components are created. This ensures all Activities display strings in the
 * selected locale as soon as they are launched.
 * 
 * Also provides additional defensive measures for AppCompat theme and initialization issues.
 */
public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        try {
            String code = LanguageManager.getCurrentLanguageCode(newBase);
            Context context = LanguageManager.applyLanguage(newBase, code);
            super.attachBaseContext(context);
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".attachBaseContext: Language setup failed, using default context", e);
            // Fallback to original context if language setup fails
            super.attachBaseContext(newBase);
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".onCreate: AppCompat initialization failed", e);
            // Re-throw to allow individual activities to handle the error
            throw e;
        }
    }
}
