package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Activity base class that applies the user's preferred language before any UI
 * components are created. This ensures all Activities display strings in the
 * selected locale as soon as they are launched.
 * 
 * Also provides additional defensive measures for AppCompat theme and initialization issues.
 */
public class BaseActivity extends AppCompatActivity {
    private View insetContent;

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        // Android 7 cannot draw dark navigation icons. Keep its native fitted
        // window and dark navigation bar rather than white icons on white.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        View content = findViewById(android.R.id.content);
        if (content == null || content == insetContent) return;
        insetContent = content;
        final int left = content.getPaddingLeft();
        final int top = content.getPaddingTop();
        final int right = content.getPaddingRight();
        final int bottom = content.getPaddingBottom();
        // All app screens use a custom toolbar or game view inside this container.
        // Keep controls clear of system bars, cutouts and the keyboard, without
        // accumulating padding on refresh or applying it again in child views.
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(left + safe.left, top + safe.top,
                    right + safe.right, bottom + safe.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(content);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) applySystemBarAppearance();
        if (hasFocus && !BangladeshMigrationHelper.shouldShowUninstallGlobalPrompt(this)
                && getApplication() instanceof SoccerApp) {
            ((SoccerApp) getApplication()).checkPrizeReminderAfterWindowFocus(this);
        }
    }

    private void applySystemBarAppearance() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        // AppTheme is always light; transparent bars expose its white background.
        // Enabling edge-to-edge alone does not select contrasting icon colors.
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
    }

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.enableEdgeToEdge(getWindow());
                applySystemBarAppearance();
            }
        } catch (Exception e) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".onCreate: AppCompat initialization failed", e);
            // Re-throw to allow individual activities to handle the error
            throw e;
        }
    }
}
