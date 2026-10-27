package piotr_gorczynski.soccer2;

import android.content.Context;
import com.google.firebase.auth.FirebaseAuth;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=AccountSignedOutTest.TestApp.class,sdk=34)
public class AccountSignedOutTest {
    public static class TestApp extends SoccerApp {
        @Override public void onCreate() {}
        @Override public synchronized void unregisterFromFcm() {}
    }

    @Test public void linkResultAfterSignOutClosesAccountAndClearsCachedIdentity() throws Exception {
        checkSignedOut(false);
    }

    @Test public void resumingAfterSignOutClosesAccountAndClearsCachedIdentity() throws Exception {
        checkSignedOut(true);
    }

    private void checkSignedOut(boolean resume) throws Exception {
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            org.robolectric.android.controller.ActivityController<AccountActivity> controller=Robolectric.buildActivity(AccountActivity.class);
            AccountActivity activity=controller.get();
            activity.setTheme(R.style.AppTheme);
            android.content.SharedPreferences prefs=activity.getSharedPreferences(LanguageManager.PREFS_FILE,Context.MODE_PRIVATE);
            prefs.edit().putString("uid","old-user").putString("nickname","btest2610")
                    .putString("email","test@example.invalid").putString("method","email")
                    .putString("unrelated_setting","preserved").commit();
            if(resume) controller.create().start().resume();
            else {
                Method refresh=AccountActivity.class.getDeclaredMethod("refreshAccountData");
                refresh.setAccessible(true);refresh.invoke(activity);
            }
            assertTrue(activity.isFinishing());
            assertFalse(prefs.contains("uid"));
            assertFalse(prefs.contains("nickname"));
            assertFalse(prefs.contains("email"));
            assertFalse(prefs.contains("method"));
            assertEquals("preserved",prefs.getString("unrelated_setting",null));
        }
    }
}
