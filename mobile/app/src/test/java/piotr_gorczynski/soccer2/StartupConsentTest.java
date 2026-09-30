package piotr_gorczynski.soccer2;

import android.app.Activity;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class StartupConsentTest {
    @Test public void startupWaitsForUmpAndContinuesWhenNoFormIsRequired() {
        checkCompletion(false);
    }

    @Test public void umpFailureDoesNotGrantConsentOrDeadlockStartup() {
        checkCompletion(true);
    }

    private void checkCompletion(boolean failure) {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().visible().get();
        SoccerApp app = new SoccerApp();
        ConsentInformation info = mock(ConsentInformation.class);
        try (MockedStatic<UserMessagingPlatform> ump = mockStatic(UserMessagingPlatform.class);
             MockedStatic<ConsentUtils> consent = mockStatic(ConsentUtils.class)) {
            ump.when(() -> UserMessagingPlatform.getConsentInformation(activity)).thenReturn(info);
            ump.when(() -> UserMessagingPlatform.getConsentInformation(app)).thenReturn(info);
            doAnswer(call -> {
                assertFalse(app.isStartupConsentFinished());
                if (failure) {
                    ((ConsentInformation.OnConsentInfoUpdateFailureListener) call.getArgument(3))
                            .onConsentInfoUpdateFailure(new FormError(2, "offline"));
                } else {
                    ((ConsentInformation.OnConsentInfoUpdateSuccessListener) call.getArgument(2))
                            .onConsentInfoUpdateSuccess();
                }
                return null;
            }).when(info).requestConsentInfoUpdate(eq(activity), any(ConsentRequestParameters.class), any(), any());
            app.refreshConsentIfNeeded(activity);
            assertTrue(app.isStartupConsentFinished());
            assertFalse(app.canRequestAds());
            when(info.canRequestAds()).thenReturn(true);
            assertTrue(app.canRequestAds());
            app.refreshConsentIfNeeded(activity);
            verify(info, times(1)).requestConsentInfoUpdate(eq(activity), any(), any(), any());
            if (failure) consent.verifyNoInteractions();
            else consent.verify(() -> ConsentUtils.updateFirebaseAnalyticsConsent(activity));
        }
        activity.finish();
    }
}
