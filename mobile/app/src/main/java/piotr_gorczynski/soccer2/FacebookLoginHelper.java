package piotr_gorczynski.soccer2;

import android.app.Activity;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import java.util.Collections;

/** Shared login/account-linking entry point; avoids SDK 18.3 native ProxyAuth crashes. */
final class FacebookLoginHelper {
    private FacebookLoginHelper() {}

    static void logIn(Activity activity, CallbackManager callbacks,
                      FacebookCallback<LoginResult> callback) {
        LoginManager manager = LoginManager.getInstance();
        // Native app launch can throw asynchronously from FacebookActivity.onResume,
        // outside the caller's try/catch. WEB_ONLY uses Custom Tabs with WebView fallback.
        manager.setLoginBehavior(LoginBehavior.WEB_ONLY);
        manager.registerCallback(callbacks, callback);
        manager.logInWithReadPermissions(activity, Collections.singletonList("public_profile"));
    }
}
