package piotr_gorczynski.soccer2;

import android.os.Looper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class MenuTermsReadFailureTest {
    private void set(MenuActivity activity, String name, Object value) throws Exception {
        Field f=MenuActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);
    }
    private void check(MenuActivity activity) throws Exception {
        Method m=MenuActivity.class.getDeclaredMethod("checkTerms",String.class,Runnable.class);
        m.setAccessible(true);m.invoke(activity,"owner",null);
    }
    @Test public void offlineMenuDoesNotEvenReadConsentOrOpenDocument() throws Exception {
        MenuActivity activity=mock(MenuActivity.class,CALLS_REAL_METHODS);
        set(activity,"isBackendAvailable",false);
        check(activity);
        verify(activity,never()).readTermsAcceptance(anyString(),any());
    }
    @Test public void failedConsentReadUsesOfflineUiInsteadOfOpeningTerms() throws Exception {
        MenuActivity activity=mock(MenuActivity.class,CALLS_REAL_METHODS);
        set(activity,"isBackendAvailable",true);
        doReturn(false).when(activity).isFinishing();
        doReturn(false).when(activity).isDestroyed();
        doNothing().when(activity).showBackendUnavailable();
        doReturn(Tasks.forException(new IllegalStateException("network")))
                .when(activity).readTermsAcceptance(anyString(),any());
        FirebaseAuth auth=mock(FirebaseAuth.class);when(auth.getUid()).thenReturn("owner");
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class);
            MockedStatic<PendingLoginSuccess> pending=mockStatic(PendingLoginSuccess.class);
            MockedStatic<AppFlavourDetector> flavour=mockStatic(AppFlavourDetector.class);
            MockedStatic<LanguageManager> language=mockStatic(LanguageManager.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            language.when(()->LanguageManager.getCurrentLanguageCode(activity)).thenReturn("en");
            flavour.when(()->AppFlavourDetector.isBangladeshFlavour(activity)).thenReturn(true);
            check(activity);shadowOf(Looper.getMainLooper()).idle();
            verify(activity).showBackendUnavailable();
            Field opened=MenuActivity.class.getDeclaredField("termsScreenOpen");opened.setAccessible(true);
            assertFalse(opened.getBoolean(activity));
        }
    }
}
