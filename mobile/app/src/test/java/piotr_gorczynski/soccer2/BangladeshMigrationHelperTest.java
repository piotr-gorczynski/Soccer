package piotr_gorczynski.soccer2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BangladeshMigrationHelper
 */
@RunWith(RobolectricTestRunner.class)
public class BangladeshMigrationHelperTest {

    private Context mockContext;

    @Before
    public void setUp() {
        mockContext = mock(Context.class);
    }

    @Test
    public void testIsGlobalAppInstalled_ReturnsFalse_WhenNotInstalled() throws Exception {
        android.content.pm.PackageManager mockPm = mock(android.content.pm.PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockPm.getPackageInfo("piotr_gorczynski.soccer2", 0))
            .thenThrow(new android.content.pm.PackageManager.NameNotFoundException());

        boolean result = BangladeshMigrationHelper.isGlobalAppInstalled(mockContext);

        assertFalse("Should return false when Global app is not installed", result);
    }

    @Test
    public void testIsGlobalAppInstalled_ReturnsTrue_WhenInstalled() throws Exception {
        android.content.pm.PackageManager mockPm = mock(android.content.pm.PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockPm.getPackageInfo("piotr_gorczynski.soccer2", 0))
            .thenReturn(mock(android.content.pm.PackageInfo.class));

        boolean result = BangladeshMigrationHelper.isGlobalAppInstalled(mockContext);

        assertTrue("Should return true when Global app is installed", result);
    }

    @Test
    public void testPromptUninstallGlobalApp_StartsDeleteIntent() {
        ArgumentCaptor<Intent> intentCaptor = ArgumentCaptor.forClass(Intent.class);

        BangladeshMigrationHelper.promptUninstallGlobalApp(mockContext);

        verify(mockContext).startActivity(intentCaptor.capture());
        Intent capturedIntent = intentCaptor.getValue();
        assertEquals("Intent action should be ACTION_DELETE", Intent.ACTION_DELETE, capturedIntent.getAction());
        assertEquals("Intent data should target Global app package",
            "package:piotr_gorczynski.soccer2", capturedIntent.getData().toString());
    }

    @Test
    public void testShouldShowUninstallGlobalPrompt_BangladeshFlavor_GlobalInstalled() throws Exception {
        // Setup: Bangladesh flavor
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2.bd");

        // Setup: Global app is installed
        android.content.pm.PackageManager mockPm = mock(android.content.pm.PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockPm.getPackageInfo("piotr_gorczynski.soccer2", 0))
                .thenReturn(mock(android.content.pm.PackageInfo.class));

        boolean result = BangladeshMigrationHelper.shouldShowUninstallGlobalPrompt(mockContext);

        assertTrue("Should show uninstall prompt in Bangladesh flavor when Global app is installed", result);
    }

    @Test
    public void testShouldShowUninstallGlobalPrompt_GlobalFlavor_ShouldNotShow() {
        // Setup: Global flavor
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2");

        boolean result = BangladeshMigrationHelper.shouldShowUninstallGlobalPrompt(mockContext);

        assertFalse("Should not show uninstall prompt in global flavor", result);
    }

    @Test
    public void testShouldShowUninstallGlobalPrompt_BangladeshFlavor_GlobalNotInstalled() throws Exception {
        // Setup: Bangladesh flavor
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2.bd");

        // Setup: Global app not installed
        android.content.pm.PackageManager mockPm = mock(android.content.pm.PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockPm.getPackageInfo("piotr_gorczynski.soccer2", 0))
                .thenThrow(new android.content.pm.PackageManager.NameNotFoundException());

        boolean result = BangladeshMigrationHelper.shouldShowUninstallGlobalPrompt(mockContext);

        assertFalse("Should not show uninstall prompt when Global app is not installed", result);
    }

    @Test
    public void testBuildUninstallGlobalAppIntent_NoExtraReturnResult() {
        Intent intent = BangladeshMigrationHelper.buildUninstallGlobalAppIntent();

        assertEquals("Intent action should be ACTION_DELETE", Intent.ACTION_DELETE, intent.getAction());
        assertEquals("Intent data should target Global app package",
                "package:piotr_gorczynski.soccer2", intent.getData().toString());
        assertFalse("EXTRA_RETURN_RESULT must NOT be set; it causes some Android versions to return " +
                "RESULT_FIRST_USER immediately without showing the uninstall dialog",
                intent.hasExtra(Intent.EXTRA_RETURN_RESULT));
    }

    @Test
    public void testBuildGlobalUninstallBridgeIntent_ReturnsNullWhenBridgeNotAvailable() throws Exception {
        PackageManager mockPm = mock(PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockPm.getActivityInfo(any(ComponentName.class), anyInt()))
                .thenThrow(new PackageManager.NameNotFoundException());

        Intent result = BangladeshMigrationHelper.buildGlobalUninstallBridgeIntent(mockContext);

        assertNull("Should return null when GlobalUninstallBridgeActivity is not available", result);
    }

    @Test
    public void testBuildGlobalUninstallBridgeIntent_ReturnsIntentWhenBridgeAvailable() throws Exception {
        PackageManager mockPm = mock(PackageManager.class);
        when(mockContext.getPackageManager()).thenReturn(mockPm);
        when(mockPm.getActivityInfo(any(ComponentName.class), anyInt()))
                .thenReturn(mock(ActivityInfo.class));

        Intent result = BangladeshMigrationHelper.buildGlobalUninstallBridgeIntent(mockContext);

        assertNotNull("Should return an Intent when GlobalUninstallBridgeActivity is available", result);
        ComponentName component = result.getComponent();
        assertNotNull("Intent must have an explicit component", component);
        assertEquals("Component package should be the Global app",
                "piotr_gorczynski.soccer2", component.getPackageName());
        assertEquals("Component class should be GlobalUninstallBridgeActivity",
                "piotr_gorczynski.soccer2.GlobalUninstallBridgeActivity", component.getClassName());
        assertTrue("Intent must include FLAG_ACTIVITY_NEW_TASK so the bridge runs in an isolated task " +
                        "and does not expose the Global app's back stack when it finishes",
                (result.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0);
        assertTrue("Intent must include FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS to hide the isolated bridge task",
                (result.getFlags() & Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS) != 0);
    }
}
