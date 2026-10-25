package piotr_gorczynski.soccer2;

import android.content.Context;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=android.app.Application.class)
public class AnalyticsUserIdentityTest {
    @Test public void crashlyticsUsesSameIdentityAndClearsOnLogoutOrDisabledCollection() {
        FirebaseCrashlytics sdk=mock(FirebaseCrashlytics.class);
        AnalyticsUserIdentity.applyCrashlytics(sdk,"first-account",true);
        AnalyticsUserIdentity.applyCrashlytics(sdk,null,true);
        AnalyticsUserIdentity.applyCrashlytics(sdk,"second-account",true);
        AnalyticsUserIdentity.applyCrashlytics(sdk,"second-account",false);
        var order=inOrder(sdk);
        order.verify(sdk).setUserId(AnalyticsUserIdentity.forUid("first-account"));
        order.verify(sdk).setUserId("");
        order.verify(sdk).setUserId(AnalyticsUserIdentity.forUid("second-account"));
        order.verify(sdk).setUserId("");
        verifyNoMoreInteractions(sdk);
    }

    @Test public void analyticsDenialDoesNotSuppressEnabledCrashDiagnosticsIdentity() {
        Context context=mock(Context.class);
        FirebaseAnalytics analyticsSdk=mock(FirebaseAnalytics.class);
        FirebaseCrashlytics crashSdk=mock(FirebaseCrashlytics.class);
        FirebaseAuth auth=mock(FirebaseAuth.class);
        when(auth.getUid()).thenReturn("anonymous-account");
        when(crashSdk.isCrashlyticsCollectionEnabled()).thenReturn(true);
        try(MockedStatic<FirebaseAnalytics> analytics=mockStatic(FirebaseAnalytics.class);
            MockedStatic<FirebaseCrashlytics> crash=mockStatic(FirebaseCrashlytics.class);
            MockedStatic<FirebaseAuth> authentication=mockStatic(FirebaseAuth.class);
            MockedStatic<ConsentUtils> consent=mockStatic(ConsentUtils.class)) {
            analytics.when(()->FirebaseAnalytics.getInstance(context)).thenReturn(analyticsSdk);
            crash.when(FirebaseCrashlytics::getInstance).thenReturn(crashSdk);
            authentication.when(FirebaseAuth::getInstance).thenReturn(auth);
            AnalyticsUserIdentity.sync(context);
            verify(analyticsSdk).setUserId(null);
            verify(crashSdk).setUserId(AnalyticsUserIdentity.forUid("anonymous-account"));
        }
    }
    @Test public void stableOpaqueIdDiffersAcrossAccounts() {
        String id=AnalyticsUserIdentity.forUid("anonymous-auth-uid");
        assertEquals(id, AnalyticsUserIdentity.forUid("anonymous-auth-uid"));
        assertTrue(id.matches("u1_[0-9a-f]{64}"));
        assertFalse(id.contains("anonymous-auth-uid"));
        assertNotEquals(id, AnalyticsUserIdentity.forUid("different-auth-uid"));
        assertNull(AnalyticsUserIdentity.forUid(null));
        assertNull(AnalyticsUserIdentity.forUid(" "));
    }

    @Test public void logoutConsentWithdrawalAndAccountSwitchReplaceSdkIdentity() {
        FirebaseAnalytics sdk=mock(FirebaseAnalytics.class);
        AnalyticsUserIdentity.apply(sdk,"first-account",true);
        AnalyticsUserIdentity.apply(sdk,null,true);
        AnalyticsUserIdentity.apply(sdk,"second-account",true);
        AnalyticsUserIdentity.apply(sdk,"second-account",false);
        var order=inOrder(sdk);
        order.verify(sdk).setUserId(AnalyticsUserIdentity.forUid("first-account"));
        order.verify(sdk).setUserId(null);
        order.verify(sdk).setUserId(AnalyticsUserIdentity.forUid("second-account"));
        order.verify(sdk).setUserId(null);
        verifyNoMoreInteractions(sdk);
    }

    @Test public void restoresAuthIdentityAndClearsItWhenConsentIsUnresolved() {
        Context context=mock(Context.class);
        FirebaseAnalytics sdk=mock(FirebaseAnalytics.class);
        FirebaseAuth auth=mock(FirebaseAuth.class);
        when(auth.getUid()).thenReturn("restored-anonymous-account");
        try(MockedStatic<FirebaseAnalytics> analytics=mockStatic(FirebaseAnalytics.class);
            MockedStatic<FirebaseAuth> authentication=mockStatic(FirebaseAuth.class);
            MockedStatic<ConsentUtils> consent=mockStatic(ConsentUtils.class)) {
            analytics.when(()->FirebaseAnalytics.getInstance(context)).thenReturn(sdk);
            authentication.when(FirebaseAuth::getInstance).thenReturn(auth);
            consent.when(()->ConsentUtils.hasUmpConsent(context)).thenReturn(true);
            consent.when(()->ConsentUtils.isAnalyticsAllowed(context)).thenReturn(true);
            AnalyticsUserIdentity.sync(context);
            verify(sdk).setUserId(AnalyticsUserIdentity.forUid("restored-anonymous-account"));
            consent.when(()->ConsentUtils.hasUmpConsent(context)).thenReturn(false);
            AnalyticsUserIdentity.sync(context);
            verify(sdk).setUserId(null);
        }
    }
}
