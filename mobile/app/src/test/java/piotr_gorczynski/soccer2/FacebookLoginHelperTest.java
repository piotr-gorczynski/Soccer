package piotr_gorczynski.soccer2;

import android.app.Activity;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.login.LoginBehavior;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import java.util.Collections;
import static org.mockito.Mockito.*;

public class FacebookLoginHelperTest {
    @Test public void avoidsNativeProxyAuthAndRegistersCallbackBeforeLaunching() {
        Activity activity = mock(Activity.class);
        CallbackManager callbacks = mock(CallbackManager.class);
        FacebookCallback<LoginResult> callback = mock(FacebookCallback.class);
        LoginManager manager = mock(LoginManager.class);
        try (MockedStatic<LoginManager> sdk = mockStatic(LoginManager.class)) {
            sdk.when(LoginManager::getInstance).thenReturn(manager);
            FacebookLoginHelper.logIn(activity, callbacks, callback);
            InOrder order = inOrder(manager);
            order.verify(manager).setLoginBehavior(LoginBehavior.WEB_ONLY);
            order.verify(manager).registerCallback(callbacks, callback);
            order.verify(manager).logInWithReadPermissions(activity,
                    Collections.singletonList("public_profile"));
            order.verifyNoMoreInteractions();
        }
    }
}
