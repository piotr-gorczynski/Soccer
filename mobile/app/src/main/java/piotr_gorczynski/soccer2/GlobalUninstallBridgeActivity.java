package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

/**
 * Transparent bridge Activity exported from the Global app so that the Bangladesh
 * flavour can trigger an uninstall of the Global app via the system UI.
 *
 * <p>The Bangladesh app starts this Activity, which launches the standard system
 * uninstall dialog for the Global package ({@code piotr_gorczynski.soccer2}).
 * The bridge stays alive while the system dialog is visible (so the dialog remains
 * in the foreground) and only calls {@code finish()} once the dialog has been
 * dismissed — either because the user confirmed the uninstall or cancelled it.
 * This prevents the system dialog from being obscured by the Bangladesh app.</p>
 *
 * <p>The Bangladesh app detects the outcome (installed / uninstalled) by checking
 * {@link BangladeshMigrationHelper#isGlobalAppInstalled} when it regains window
 * focus via {@code onWindowFocusChanged}.</p>
 */
public class GlobalUninstallBridgeActivity extends Activity {

    private static final String TAG = "TAG_Soccer";

    private static final String KEY_LAUNCHED = "uninstallDialogLaunched";
    private static final String KEY_PAUSED = "hasBeenPaused";

    private boolean uninstallDialogLaunched = false;
    private boolean hasBeenPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            uninstallDialogLaunched = savedInstanceState.getBoolean(KEY_LAUNCHED, false);
            hasBeenPaused = savedInstanceState.getBoolean(KEY_PAUSED, false);
        }
        if (!uninstallDialogLaunched) {
            Log.d(TAG, "GlobalUninstallBridgeActivity.onCreate: launching system uninstall for " + getPackageName());
            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            uninstallDialogLaunched = true;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_LAUNCHED, uninstallDialogLaunched);
        outState.putBoolean(KEY_PAUSED, hasBeenPaused);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The system uninstall dialog (or any other activity) has come to the foreground.
        // Record that we have been paused so that the next onResume() knows the dialog
        // was actually shown and can safely finish this bridge.
        hasBeenPaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasBeenPaused) {
            // We were previously paused by the system uninstall dialog.  Now that focus
            // has returned, the dialog has been dismissed (confirmed or cancelled).
            // If the uninstall was confirmed the global-app process is being killed and
            // this code may never be reached, but finish() here covers the cancellation
            // case and returns focus cleanly to the Bangladesh app.
            Log.d(TAG, "GlobalUninstallBridgeActivity.onResume: system dialog dismissed; finishing");
            finish();
        }
        // If !hasBeenPaused this is the initial onResume() after onCreate() — the system
        // dialog has not appeared yet, so we stay alive and wait.
    }
}
