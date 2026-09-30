package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsBuildMetadataTest {
    @Test public void identifiesInstallationBeforeLoginAndPreservesVersionAfterLogin() {
        Context context = mock(Context.class);
        FirebaseAnalytics analytics = mock(FirebaseAnalytics.class);
        FirebaseCrashlytics crashes = mock(FirebaseCrashlytics.class);
        try (MockedStatic<FirebaseAnalytics> sdk = mockStatic(FirebaseAnalytics.class);
             MockedStatic<FirebaseCrashlytics> crashSdk = mockStatic(FirebaseCrashlytics.class)) {
            sdk.when(() -> FirebaseAnalytics.getInstance(context)).thenReturn(analytics);
            crashSdk.when(FirebaseCrashlytics::getInstance).thenReturn(crashes);
            AnalyticsManager manager = new AnalyticsManager(context);
            String market = BuildConfig.APPLICATION_ID.endsWith(".bd") ? "bangladesh" : "global";
            ArgumentCaptor<Bundle> params = ArgumentCaptor.forClass(Bundle.class);
            verify(analytics).setDefaultEventParameters(params.capture());
            assertEquals(market, params.getValue().getString("app_variant"));
            assertEquals(BuildConfig.FLAVOR_environment, params.getValue().getString("app_environment"));
            assertEquals(BuildConfig.BUILD_TYPE, params.getValue().getString("app_build_type"));
            verify(analytics).setUserProperty("app_variant", market);
            verify(analytics).setUserProperty("app_version", BuildConfig.VERSION_NAME);
            manager.trackAppVariantConflictShown();
            manager.trackAppVariantConflictClosed();
            verify(analytics).logEvent(eq("app_variant_conflict_shown"), any(Bundle.class));
            verify(analytics).logEvent(eq("app_variant_conflict_closed"), any(Bundle.class));
            manager.setUserProperties("google", "pl", true);
            verify(analytics, times(2)).setUserProperty("app_version", BuildConfig.VERSION_NAME);
            verify(crashes).setCustomKey("app_version", BuildConfig.VERSION_NAME);
        }
    }
}
