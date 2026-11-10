package piotr_gorczynski.soccer2;

import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {30, 36})
public class BaseActivityInsetsTest {
    public static class Screen extends BaseActivity {
        @Override protected void onCreate(Bundle state) {
            super.onCreate(state);
            setContentView(new FrameLayout(this));
        }
    }

    private WindowInsetsCompat insets(Insets bars, Insets cutout, Insets keyboard) {
        return new WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.systemBars(), bars)
                .setInsets(WindowInsetsCompat.Type.displayCutout(), cutout)
                .setInsets(WindowInsetsCompat.Type.ime(), keyboard).build();
    }

    private void padding(View view, int left, int top, int right, int bottom) {
        assertEquals(left, view.getPaddingLeft());
        assertEquals(top, view.getPaddingTop());
        assertEquals(right, view.getPaddingRight());
        assertEquals(bottom, view.getPaddingBottom());
    }

    @Test public void whiteSystemBarsUseDarkIcons() {
        try (var controller = Robolectric.buildActivity(Screen.class).create()) {
            var window = controller.get().getWindow();
            var appearance = WindowCompat.getInsetsController(window, window.getDecorView());
            assertTrue(appearance.isAppearanceLightStatusBars());
            assertTrue(appearance.isAppearanceLightNavigationBars());
        }
    }

    @Test public void returningWindowFocusRestoresDarkIcons() {
        try (var controller = Robolectric.buildActivity(Screen.class).create()) {
            var activity = controller.get();
            var window = activity.getWindow();
            var appearance = WindowCompat.getInsetsController(window, window.getDecorView());
            appearance.setAppearanceLightStatusBars(false);
            appearance.setAppearanceLightNavigationBars(false);
            activity.onWindowFocusChanged(true);
            assertTrue(appearance.isAppearanceLightStatusBars());
            assertTrue(appearance.isAppearanceLightNavigationBars());
        }
    }

    @Test @Config(sdk = 24) public void legacyAndroidKeepsNativeFittedWindow() {
        try (var controller = Robolectric.buildActivity(Screen.class).create()) {
            var activity = controller.get();
            assertEquals(0, activity.getWindow().getDecorView().getSystemUiVisibility()
                    & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            View content = activity.findViewById(android.R.id.content);
            ViewCompat.dispatchApplyWindowInsets(content,
                    insets(Insets.of(0, 24, 0, 48), Insets.NONE, Insets.NONE));
            padding(content, 0, 0, 0, 0);
        }
    }

    @Test public void systemBarsAndCutoutProtectContentWithoutAddingThemTogether() {
        try (var controller = Robolectric.buildActivity(Screen.class).create()) {
            View content = controller.get().findViewById(android.R.id.content);
            ViewCompat.dispatchApplyWindowInsets(content,
                    insets(Insets.of(0, 24, 0, 48), Insets.of(40, 32, 0, 0), Insets.NONE));
            padding(content, 40, 32, 0, 48);
        }
    }

    @Test public void repeatedDispatchDoesNotAccumulatePaddingAndRotationReplacesInsets() {
        try (var controller = Robolectric.buildActivity(Screen.class).create()) {
            View content = controller.get().findViewById(android.R.id.content);
            WindowInsetsCompat portrait = insets(Insets.of(0, 24, 0, 48), Insets.NONE, Insets.NONE);
            ViewCompat.dispatchApplyWindowInsets(content, portrait);
            ViewCompat.dispatchApplyWindowInsets(content, portrait);
            padding(content, 0, 24, 0, 48);
            ViewCompat.dispatchApplyWindowInsets(content,
                    insets(Insets.of(0, 0, 24, 0), Insets.of(32, 0, 0, 0), Insets.NONE));
            padding(content, 32, 0, 24, 0);
        }
    }

    @Test public void keyboardUsesMaximumBottomInsetAndHidingItRestoresNavigationInset() {
        try (var controller = Robolectric.buildActivity(Screen.class).create()) {
            View content = controller.get().findViewById(android.R.id.content);
            ViewCompat.dispatchApplyWindowInsets(content,
                    insets(Insets.of(0, 24, 0, 48), Insets.NONE, Insets.of(0, 0, 0, 300)));
            padding(content, 0, 24, 0, 300);
            ViewCompat.dispatchApplyWindowInsets(content,
                    insets(Insets.of(0, 24, 0, 48), Insets.NONE, Insets.NONE));
            padding(content, 0, 24, 0, 48);
        }
    }

    @Test public void replacingScreenContentDoesNotInstallDuplicatePadding() {
        try (var controller = Robolectric.buildActivity(Screen.class).create()) {
            Screen activity = controller.get();
            View content = activity.findViewById(android.R.id.content);
            WindowInsetsCompat bars = insets(Insets.of(0, 24, 0, 48), Insets.NONE, Insets.NONE);
            ViewCompat.dispatchApplyWindowInsets(content, bars);
            FrameLayout next = new FrameLayout(activity);
            next.setPadding(8, 9, 10, 11);
            activity.setContentView(next);
            ViewCompat.dispatchApplyWindowInsets(content, bars);
            padding(content, 0, 24, 0, 48);
            padding(next, 8, 9, 10, 11);
        }
    }
}
