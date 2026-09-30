package piotr_gorczynski.soccer2;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;


/**
 * Shared installed-variant conflict check and Global-app uninstall flow.
 * Does not detect country or promote the Bangladesh app in the Global flavor.
 */
public class BangladeshMigrationHelper {
    
    // Use app-wide tag so these logs are visible when filtering logcat by TAG_Soccer.
    private static final String TAG = "TAG_Soccer";

    private static final String GLOBAL_APP_PACKAGE = "piotr_gorczynski.soccer2";

    /** Both variants require removing Global when the other variant is installed. */
    public static boolean shouldShowUninstallGlobalPrompt(Context context) {
        String otherPackage = AppFlavourDetector.isBangladeshFlavour(context)
                ? GLOBAL_APP_PACKAGE : "piotr_gorczynski.soccer2.bd";
        return isPackageInstalled(context, otherPackage);
    }

    public static int uninstallTitle(Context context) {
        return AppFlavourDetector.isBangladeshFlavour(context)
                ? R.string.uninstall_global_title : R.string.bangladesh_installed_title;
    }

    public static int uninstallMessage(Context context) {
        return AppFlavourDetector.isBangladeshFlavour(context)
                ? R.string.uninstall_global_message : R.string.bangladesh_installed_message;
    }

    /**
     * Check if the Global app is installed on the device.
     * Useful in the Bangladesh flavor to determine whether to prompt the user to uninstall it.
     *
     * @param context Android context
     * @return true if the Global app (piotr_gorczynski.soccer2) is installed
     */
    public static boolean isGlobalAppInstalled(Context context) {
        return isPackageInstalled(context, GLOBAL_APP_PACKAGE);
    }

    private static boolean isPackageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Build uninstall intent for the Global app package.
     *
     * <p>Note: {@code EXTRA_RETURN_RESULT} is intentionally <em>not</em> set here.
     * On some Android versions, setting it causes the PackageInstaller to return
     * {@code RESULT_FIRST_USER} immediately without showing the uninstall dialog,
     * which makes the uninstall flow impossible to complete.</p>
     */
    public static Intent buildUninstallGlobalAppIntent() {
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + GLOBAL_APP_PACKAGE));
        return intent;
    }

    /**
     * Build an explicit Intent that starts {@code GlobalUninstallBridgeActivity} inside
     * the Global app, if that activity is present and reachable.
     *
     * <p>Using this bridge means the <em>Global</em> app itself triggers
     * {@code ACTION_DELETE} for its own package, which avoids cross-app restrictions
     * that on certain Android versions cause {@code ACTION_DELETE} launched from a
     * third-party app to return {@code RESULT_FIRST_USER} without showing the dialog.</p>
     *
     * @param context Android context (used to verify the activity is reachable)
     * @return explicit Intent targeting the bridge, or {@code null} if the Global app
     *         is not installed or the bridge activity is not exported in that version
     */
    public static Intent buildGlobalUninstallBridgeIntent(Context context) {
        ComponentName bridge = new ComponentName(
                GLOBAL_APP_PACKAGE,
                GLOBAL_APP_PACKAGE + ".GlobalUninstallBridgeActivity");
        try {
            context.getPackageManager().getActivityInfo(bridge, 0);
            Intent intent = new Intent();
            intent.setComponent(bridge);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            return intent;
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "BangladeshMigrationHelper.buildGlobalUninstallBridgeIntent: bridge activity not available in Global app");
            return null;
        }
    }

    /**
     * Open the Android system uninstall dialog for the Global app so the user can remove it.
     * Android does not allow apps to silently uninstall other apps; user confirmation is required.
     * This should only be called from the Bangladesh flavor.
     *
     * @param context Android context
     */
    public static void promptUninstallGlobalApp(Context context) {
        try {
            Intent intent = buildUninstallGlobalAppIntent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.d(TAG, "Opened system uninstall dialog for Global app");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open uninstall dialog for Global app", e);
        }
    }
}
