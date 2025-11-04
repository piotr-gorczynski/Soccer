package piotr_gorczynski.soccer2;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test to verify that the splash_background drawable can be loaded without crashes.
 * This test specifically checks for the fix to the issue where @mipmap/ic_launcher
 * (an adaptive icon) was incorrectly used as a bitmap source, causing a crash on API 26+.
 * 
 * The fix changed it to use @mipmap/ic_launcher_foreground which is an actual bitmap.
 */
@RunWith(AndroidJUnit4.class)
public class SplashBackgroundDrawableTest {

    @Test
    public void testSplashBackgroundDrawableCanBeLoaded() {
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            // Attempt to load the splash_background drawable
            Drawable drawable = context.getDrawable(R.drawable.splash_background);
            
            // Verify the drawable was loaded successfully
            assertNotNull("splash_background drawable should be loaded successfully", drawable);
            
        } catch (Exception e) {
            fail("Failed to load splash_background drawable. This may indicate that the drawable " +
                 "contains invalid references (e.g., using @mipmap/ic_launcher adaptive icon as a bitmap source): " 
                 + e.getMessage());
        }
    }

    @Test
    public void testLauncherForegroundIconExists() {
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            // Verify that the ic_launcher_foreground resource exists and can be loaded
            // This is the resource that splash_background.xml should reference
            int resId = context.getResources().getIdentifier(
                "ic_launcher_foreground", "mipmap", context.getPackageName());
            
            assertTrue("ic_launcher_foreground mipmap resource should exist", resId != 0);
            
        } catch (Exception e) {
            fail("Failed to verify ic_launcher_foreground resource: " + e.getMessage());
        }
    }

    @Test
    public void testColorGreenDarkExists() {
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            // Verify that the colorGreenDark color resource exists
            // This is used as the background color in splash_background.xml
            int color = context.getColor(R.color.colorGreenDark);
            
            // Verify it's a valid color (not 0, which would indicate an error)
            assertTrue("colorGreenDark should be a valid color", color != 0);
            
        } catch (Exception e) {
            fail("Failed to load colorGreenDark: " + e.getMessage());
        }
    }
}
