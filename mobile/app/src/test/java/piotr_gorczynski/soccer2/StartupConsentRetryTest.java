package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.os.Looper;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class, sdk = 36)
@LooperMode(LooperMode.Mode.PAUSED)
public class StartupConsentRetryTest {
    public static class RetryActivity extends Activity {
        boolean focused = true;
        @Override public boolean hasWindowFocus() { return focused; }
    }
    static class TestApp extends SoccerApp {
        boolean online;
        boolean ready = true;
        @Override protected boolean canRetryConsent(Activity host) {
            return host != null && online && ready;
        }
    }

    Activity host;
    TestApp app;
    ConsentInformation info;
    MockedStatic<UserMessagingPlatform> ump;
    MockedStatic<ConsentUtils> consent;
    int requests;
    int failureCode = FormError.ErrorCode.INTERNET_ERROR;
    boolean succeed;
    boolean defer;
    ConsentInformation.OnConsentInfoUpdateSuccessListener pendingSuccess;

    @Before public void setup() {
        host = Robolectric.buildActivity(RetryActivity.class).setup().visible().get();
        app = new TestApp();
        info = mock(ConsentInformation.class);
        ump = mockStatic(UserMessagingPlatform.class);
        consent = mockStatic(ConsentUtils.class);
        ump.when(() -> UserMessagingPlatform.getConsentInformation(host)).thenReturn(info);
        ump.when(() -> UserMessagingPlatform.getConsentInformation(app)).thenReturn(info);
        doAnswer(call -> {
            requests++;
            if (defer) pendingSuccess = call.getArgument(2);
            else if (succeed) ((ConsentInformation.OnConsentInfoUpdateSuccessListener)
                    call.getArgument(2)).onConsentInfoUpdateSuccess();
            else ((ConsentInformation.OnConsentInfoUpdateFailureListener)
                    call.getArgument(3)).onConsentInfoUpdateFailure(new FormError(failureCode, "test failure"));
            return null;
        }).when(info).requestConsentInfoUpdate(eq(host), any(ConsentRequestParameters.class), any(), any());
    }

    @After public void cleanup() {
        app.onConsentHostStopped(host);
        host.finish();
        consent.close();
        ump.close();
    }

    void advance(long seconds) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(seconds));
    }

    @Test public void offlineFailureAllowsMenuAndNetworkRecoveryRetriesInSameApp() {
        app.refreshConsentIfNeeded(host);
        assertTrue(app.isStartupConsentFinished());
        assertFalse(app.canRequestAds());
        advance(60);
        assertEquals(1, requests);
        consent.verifyNoInteractions();
        app.online = true;
        succeed = true;
        app.refreshConsentIfNeeded(host);
        assertEquals(2, requests);
        consent.verify(() -> ConsentUtils.updateFirebaseAnalyticsConsent(host), times(1));
        // Success, including a user's rejection, must not start another retry cycle.
        app.refreshConsentIfNeeded(host);
        advance(120);
        assertEquals(2, requests);
    }

    @Test public void onlineFailureBacksOffAndStopsAfterThreeRetries() {
        app.online = true;
        app.refreshConsentIfNeeded(host);
        advance(4);
        assertEquals(1, requests);
        // Repeated network/focus signals cannot bypass the delay or duplicate timers.
        for (int i = 0; i < 10; i++) app.refreshConsentIfNeeded(host);
        advance(1);
        assertEquals(2, requests);
        advance(14);
        assertEquals(2, requests);
        advance(1);
        assertEquals(3, requests);
        advance(45);
        assertEquals(4, requests);
        for (int i = 0; i < 10; i++) app.refreshConsentIfNeeded(host);
        advance(600);
        assertEquals(4, requests);
        consent.verifyNoInteractions();
    }

    @Test public void stoppedHostCancelsTimerAndResumeCanRecover() {
        app.online = true;
        app.refreshConsentIfNeeded(host);
        app.ready = false;
        app.onConsentHostStopped(host);
        advance(60);
        app.refreshConsentIfNeeded(host);
        assertEquals(1, requests);
        app.ready = true;
        succeed = true;
        app.refreshConsentIfNeeded(host);
        assertEquals(2, requests);
    }

    @Test public void lostFocusPreventsScheduledRetryUntilFocusReturns() {
        app.online = true;
        app.refreshConsentIfNeeded(host);
        app.ready = false;
        advance(30);
        assertEquals(1, requests);
        app.ready = true;
        succeed = true;
        app.refreshConsentIfNeeded(host);
        assertEquals(2, requests);
    }

    @Test public void simultaneousSignalsCannotStartOverlappingRequest() {
        app.refreshConsentIfNeeded(host);
        advance(10);
        app.online = true;
        defer = true;
        app.refreshConsentIfNeeded(host);
        for (int i = 0; i < 10; i++) app.refreshConsentIfNeeded(host);
        assertEquals(2, requests);
        pendingSuccess.onConsentInfoUpdateSuccess();
        app.refreshConsentIfNeeded(host);
        assertEquals(2, requests);
    }

    @Test public void delayedResponseDoesNotShowFormOverAnotherWindow() {
        app.refreshConsentIfNeeded(host);
        advance(10);
        app.online = true;
        defer = true;
        when(info.isConsentFormAvailable()).thenReturn(true);
        app.refreshConsentIfNeeded(host);
        ((RetryActivity) host).focused = false;
        pendingSuccess.onConsentInfoUpdateSuccess();
        ump.verify(() -> UserMessagingPlatform.loadConsentForm(eq(host), any(), any()), never());
        ((RetryActivity) host).focused = true;
        app.refreshConsentIfNeeded(host);
        assertEquals(3, requests);
    }

    @Test public void invalidOperationIsNotRetried() {
        app.online = true;
        failureCode = FormError.ErrorCode.INVALID_OPERATION;
        app.refreshConsentIfNeeded(host);
        advance(120);
        app.refreshConsentIfNeeded(host);
        assertEquals(1, requests);
        assertTrue(app.isStartupConsentFinished());
        consent.verifyNoInteractions();
    }

    @Test public void timeoutCanRecoverWithoutChangingConsentOnFailure() {
        app.online = true;
        failureCode = FormError.ErrorCode.TIME_OUT;
        app.refreshConsentIfNeeded(host);
        consent.verifyNoInteractions();
        succeed = true;
        advance(5);
        assertEquals(2, requests);
        consent.verify(() -> ConsentUtils.updateFirebaseAnalyticsConsent(host), times(1));
    }

    @Test public void disconnectedTimerWaitsForNetworkInsteadOfSpendingRetryBudget() {
        app.online = true;
        app.refreshConsentIfNeeded(host);
        app.online = false;
        advance(60);
        assertEquals(1, requests);
        app.online = true;
        succeed = true;
        app.refreshConsentIfNeeded(host);
        assertEquals(2, requests);
        advance(120);
        assertEquals(2, requests);
    }

    @Test public void consentFormLoadNetworkFailureIsRetried() {
        app.online = true;
        succeed = true;
        when(info.isConsentFormAvailable()).thenReturn(true);
        ump.when(() -> UserMessagingPlatform.loadConsentForm(eq(host), any(), any()))
                .thenAnswer(call -> {
                    ((UserMessagingPlatform.OnConsentFormLoadFailureListener) call.getArgument(2))
                            .onConsentFormLoadFailure(new FormError(FormError.ErrorCode.INTERNET_ERROR, "offline"));
                    return null;
                });
        app.refreshConsentIfNeeded(host);
        assertTrue(app.isStartupConsentFinished());
        when(info.isConsentFormAvailable()).thenReturn(false);
        advance(5);
        assertEquals(2, requests);
    }

    @Test public void completedFormDoesNotRetryEvenWhenUserRejects() {
        app.refreshConsentIfNeeded(host);
        advance(10);
        app.online = true;
        succeed = true;
        when(info.isConsentFormAvailable()).thenReturn(true);
        when(info.getConsentStatus()).thenReturn(ConsentInformation.ConsentStatus.REQUIRED);
        ConsentForm form = mock(ConsentForm.class);
        ump.when(() -> UserMessagingPlatform.loadConsentForm(eq(host), any(), any()))
                .thenAnswer(call -> {
                    ((UserMessagingPlatform.OnConsentFormLoadSuccessListener) call.getArgument(1))
                            .onConsentFormLoadSuccess(form);
                    return null;
                });
        doAnswer(call -> {
            // OBTAINED means a choice was made; it does not mean acceptance.
            when(info.getConsentStatus()).thenReturn(ConsentInformation.ConsentStatus.OBTAINED);
            when(info.canRequestAds()).thenReturn(false);
            ((ConsentForm.OnConsentFormDismissedListener) call.getArgument(1))
                    .onConsentFormDismissed(null);
            return null;
        }).when(form).show(eq(host), any());
        app.refreshConsentIfNeeded(host);
        verify(form).show(eq(host), any());
        consent.verify(() -> ConsentUtils.updateFirebaseAnalyticsConsent(host), times(2));
        app.refreshConsentIfNeeded(host);
        advance(120);
        assertEquals(2, requests);
        assertFalse(app.canRequestAds());
    }
}
