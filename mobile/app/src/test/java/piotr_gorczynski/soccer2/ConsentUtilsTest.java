package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.mockito.MockedStatic;
import java.util.Map;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 36)
public class ConsentUtilsTest {
    @Test public void allFourConsentTypesDefaultToDeniedInMergedManifest() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        Bundle metadata = app.getPackageManager().getApplicationInfo(
                app.getPackageName(), PackageManager.GET_META_DATA).metaData;
        for (String key : new String[] {
                "google_analytics_default_allow_analytics_storage",
                "google_analytics_default_allow_ad_storage",
                "google_analytics_default_allow_ad_user_data",
                "google_analytics_default_allow_ad_personalization_signals"}) {
            assertTrue("Missing consent default: " + key, metadata.containsKey(key));
            assertFalse(key, metadata.getBoolean(key, true));
        }
    }

    private void check(int status, String purposes, boolean analytics, boolean ads) {
        checkSequence(status, new String[] {purposes}, new boolean[][] {{analytics, ads}});
    }

    private void checkSequence(int status, String[] purposes, boolean[][] expected) {
        Context context = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        ConsentInformation consent = mock(ConsentInformation.class);
        FirebaseAnalytics firebase = mock(FirebaseAnalytics.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        when(auth.getUid()).thenReturn("consent-test-account");
        when(consent.getConsentStatus()).thenReturn(status);
        try (MockedStatic<PreferenceManager> preferences = mockStatic(PreferenceManager.class);
             MockedStatic<UserMessagingPlatform> ump = mockStatic(UserMessagingPlatform.class);
             MockedStatic<FirebaseAnalytics> sdk = mockStatic(FirebaseAnalytics.class);
             MockedStatic<FirebaseAuth> authentication = mockStatic(FirebaseAuth.class)) {
            authentication.when(FirebaseAuth::getInstance).thenReturn(auth);
            preferences.when(() -> PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(prefs);
            ump.when(() -> UserMessagingPlatform.getConsentInformation(context)).thenReturn(consent);
            sdk.when(() -> FirebaseAnalytics.getInstance(context)).thenReturn(firebase);
            for (int i = 0; i < purposes.length; i++) {
                when(prefs.getString("IABTCF_PurposeConsents", "")).thenReturn(purposes[i]);
                ConsentUtils.updateFirebaseAnalyticsConsent(context);
                verify(firebase).setConsent(Map.of(
                        FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, expected[i][0]
                                ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED,
                        FirebaseAnalytics.ConsentType.AD_STORAGE, expected[i][1]
                                ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED));
                verify(firebase).setUserId(expected[i][0]
                        ? AnalyticsUserIdentity.forUid("consent-test-account") : null);
                // Exact map and identity only: never replace UMP's AD_USER_DATA /
                // AD_PERSONALIZATION choices or force allow_ad_personalization_signals.
                verifyNoMoreInteractions(firebase);
                clearInvocations(firebase);
            }
        }
    }

    @Test public void restoresAcceptedPurposes() { check(ConsentInformation.ConsentStatus.OBTAINED, "1111", true, true); }
    @Test public void preservesRejection() { check(ConsentInformation.ConsentStatus.OBTAINED, "0000", false, false); }
    @Test public void analyticsDoesNotRequirePersonalizedAds() { check(ConsentInformation.ConsentStatus.OBTAINED, "1000", true, false); }
    @Test public void renewedConsentDoesNotReuseOldAcceptance() { check(ConsentInformation.ConsentStatus.REQUIRED, "1111", false, false); }
    @Test public void unknownDoesNotGrant() { check(ConsentInformation.ConsentStatus.UNKNOWN, "", false, false); }
    @Test public void notRequiredUsesExistingPolicy() { check(ConsentInformation.ConsentStatus.NOT_REQUIRED, "", true, true); }
    @Test public void rejectAcceptWithdrawPreservesStorageAndDoesNotOverrideUmpAdvertiserConsent() {
        checkSequence(ConsentInformation.ConsentStatus.OBTAINED,
                new String[] {"00000000000", "11111111111", "00000000000"},
                new boolean[][] {{false, false}, {true, true}, {false, false}});
    }
}
