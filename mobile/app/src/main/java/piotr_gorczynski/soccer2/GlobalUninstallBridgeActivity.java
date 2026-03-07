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
 * uninstall dialog for the Global package ({@code piotr_gorczynski.soccer2}) and
 * then immediately finishes.  Because the Global app itself is the caller of
 * {@code ACTION_DELETE}, cross-app restrictions that cause early
 * {@code EXTRA_RETURN_RESULT} failures on certain Android versions are avoided.</p>
 *
 * <p>The Bangladesh app detects the outcome (installed / uninstalled) by checking
 * {@link BangladeshMigrationHelper#isGlobalAppInstalled} when it regains window
 * focus via {@code onWindowFocusChanged}.</p>
 */
public class GlobalUninstallBridgeActivity extends Activity {

    private static final String TAG = "TAG_Soccer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "GlobalUninstallBridgeActivity.onCreate: launching system uninstall for " + getPackageName());
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        finish();
    }
}
