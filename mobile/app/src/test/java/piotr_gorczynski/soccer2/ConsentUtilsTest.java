package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.UserMessagingPlatform;
import com.google.firebase.analytics.FirebaseAnalytics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.mockito.MockedStatic;
import java.util.Map;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class ConsentUtilsTest {
    private void check(int status, String purposes, boolean analytics, boolean ads) {
        Context context = mock(Context.class);
        SharedPreferences prefs = mock(SharedPreferences.class);
        ConsentInformation consent = mock(ConsentInformation.class);
        FirebaseAnalytics firebase = mock(FirebaseAnalytics.class);
        when(consent.getConsentStatus()).thenReturn(status);
        when(prefs.getString("IABTCF_PurposeConsents", "")).thenReturn(purposes);
        try (MockedStatic<PreferenceManager> preferences = mockStatic(PreferenceManager.class);
             MockedStatic<UserMessagingPlatform> ump = mockStatic(UserMessagingPlatform.class);
             MockedStatic<FirebaseAnalytics> sdk = mockStatic(FirebaseAnalytics.class)) {
            preferences.when(() -> PreferenceManager.getDefaultSharedPreferences(context)).thenReturn(prefs);
            ump.when(() -> UserMessagingPlatform.getConsentInformation(context)).thenReturn(consent);
            sdk.when(() -> FirebaseAnalytics.getInstance(context)).thenReturn(firebase);
            ConsentUtils.updateFirebaseAnalyticsConsent(context);
            verify(firebase).setConsent(Map.of(
                    FirebaseAnalytics.ConsentType.ANALYTICS_STORAGE, analytics
                            ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED,
                    FirebaseAnalytics.ConsentType.AD_STORAGE, ads
                            ? FirebaseAnalytics.ConsentStatus.GRANTED : FirebaseAnalytics.ConsentStatus.DENIED));
        }
    }

    @Test public void restoresAcceptedPurposes() { check(ConsentInformation.ConsentStatus.OBTAINED, "1111", true, true); }
    @Test public void preservesRejection() { check(ConsentInformation.ConsentStatus.OBTAINED, "0000", false, false); }
    @Test public void analyticsDoesNotRequirePersonalizedAds() { check(ConsentInformation.ConsentStatus.OBTAINED, "1000", true, false); }
    @Test public void renewedConsentDoesNotReuseOldAcceptance() { check(ConsentInformation.ConsentStatus.REQUIRED, "1111", false, false); }
    @Test public void unknownDoesNotGrant() { check(ConsentInformation.ConsentStatus.UNKNOWN, "", false, false); }
    @Test public void notRequiredUsesExistingPolicy() { check(ConsentInformation.ConsentStatus.NOT_REQUIRED, "", true, true); }
}
