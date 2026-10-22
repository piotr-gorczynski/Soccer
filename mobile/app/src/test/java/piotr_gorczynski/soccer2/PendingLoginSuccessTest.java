package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = android.app.Application.class)
public class PendingLoginSuccessTest {
    private Context context;
    private AnalyticsManager analytics;

    @Before public void setup() {
        context = ApplicationProvider.getApplicationContext();
        analytics = mock(AnalyticsManager.class);
        PendingLoginSuccess.clear(context);
    }

    @Test public void requiresTermsAndSavedNicknameThenEmitsOnlyOnce() {
        PendingLoginSuccess.begin(context, "user", "anonymous");
        String attempt = PendingLoginSuccess.attempt(context, "user");
        PendingLoginSuccess.complete(context, "user", attempt, false, "player", analytics);
        PendingLoginSuccess.complete(context, "user", attempt, true, null, analytics);
        PendingLoginSuccess.complete(context, "user", attempt, true, "  ", analytics);
        verifyNoInteractions(analytics);
        PendingLoginSuccess.complete(context, "user", attempt, true, "player", analytics);
        PendingLoginSuccess.complete(context, "user", attempt, true, "player", analytics);
        verify(analytics, times(1)).trackLoginSuccess("anonymous");
        assertNull(PendingLoginSuccess.attempt(context, "user"));
    }

    @Test public void restoredSessionWithoutPendingSignInDoesNotEmit() {
        PendingLoginSuccess.complete(context, "user", null, true, "player", analytics);
        verifyNoInteractions(analytics);
    }

    @Test public void staleChecksCannotCompleteAnotherUserOrNewAttempt() {
        PendingLoginSuccess.begin(context, "user", "google.com");
        String old = PendingLoginSuccess.attempt(context, "user");
        PendingLoginSuccess.begin(context, "user", "facebook");
        PendingLoginSuccess.complete(context, "user", old, true, "player", analytics);
        String current = PendingLoginSuccess.attempt(context, "user");
        PendingLoginSuccess.complete(context, "other", current, true, "player", analytics);
        verifyNoInteractions(analytics);
        PendingLoginSuccess.complete(context, "user", current, true, "player", analytics);
        verify(analytics).trackLoginSuccess("facebook");
    }

    @Test public void pendingAttemptSurvivesNewContextButClearingCancelsIt() {
        PendingLoginSuccess.begin(context, "user", "email");
        String attempt = PendingLoginSuccess.attempt(context, "user");
        Context anotherContext = context.createConfigurationContext(context.getResources().getConfiguration());
        assertEquals(attempt, PendingLoginSuccess.attempt(anotherContext, "user"));
        PendingLoginSuccess.clear(anotherContext);
        PendingLoginSuccess.complete(context, "user", attempt, true, "player", analytics);
        verifyNoInteractions(analytics);
    }
}
