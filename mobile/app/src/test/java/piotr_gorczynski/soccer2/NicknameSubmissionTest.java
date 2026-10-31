package piotr_gorczynski.soccer2;

import android.os.Looper;
import androidx.lifecycle.ViewModelStore;
import com.google.android.gms.tasks.TaskCompletionSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class, sdk=34)
public class NicknameSubmissionTest {
    private final TaskCompletionSource<Boolean> unique = new TaskCompletionSource<>();
    private final TaskCompletionSource<Boolean> allowed = new TaskCompletionSource<>();
    private final TaskCompletionSource<Void> saved = new TaskCompletionSource<>();
    private NicknameSubmission.Backend backend;
    private AnalyticsManager analytics;
    private NicknameSubmission model;
    private NicknameSubmission.Observer screen;

    @Before public void setup() {
        backend = mock(NicknameSubmission.Backend.class);
        analytics = mock(AnalyticsManager.class);
        screen = mock(NicknameSubmission.Observer.class);
        when(backend.currentUid()).thenReturn("account-a");
        when(backend.unique("privateNickname")).thenReturn(unique.getTask());
        when(backend.allowed("privateNickname")).thenReturn(allowed.getTask());
        when(backend.save("account-a", "privateNickname")).thenReturn(saved.getTask());
        model = new NicknameSubmission(backend, analytics);
        model.observe(screen);
        model.submit("account-a", "privateNickname");
        clearInvocations(screen);
    }

    private void drain() { shadowOf(Looper.getMainLooper()).idle(); }
    private void finishChecks() {
        unique.setResult(true); drain();
        allowed.setResult(true); drain();
    }

    @Test public void stoppedScreenDoesNotLoseModerationOrSaveAndResultWaitsForReturn() {
        unique.setResult(true); drain();
        model.observe(null); // Activity.onPause, before delayed AI response
        allowed.setResult(true); drain();
        verify(backend).save("account-a", "privateNickname");
        saved.setResult(null); drain();
        verifyNoInteractions(screen);
        verify(analytics).trackOnboardingStep("nickname", "success", "none");
        model.observe(screen);
        verify(screen).changed(NicknameSubmission.Result.SAVED);
        assertEquals("privateNickname", model.nickname());
    }

    @Test public void stoppingDuringUniquenessAlsoContinuesExactlyOnce() {
        model.observe(null);
        model.submit("account-a", "anotherNickname");
        finishChecks(); saved.setResult(null); drain();
        verify(backend, times(1)).unique(anyString());
        verify(backend, times(1)).allowed(anyString());
        verify(backend, times(1)).save(anyString(), anyString());
        verify(analytics, times(1)).trackOnboardingStep("nickname", "success", "none");
    }

    @Test public void recreatedScreenReceivesPendingStateAndThenOneResult() {
        model.observe(null);
        NicknameSubmission.Observer replacement = mock(NicknameSubmission.Observer.class);
        model.observe(replacement);
        verify(replacement).changed(NicknameSubmission.Result.BUSY);
        finishChecks(); saved.setResult(null); drain();
        verify(replacement).changed(NicknameSubmission.Result.SAVED);
        verifyNoInteractions(screen);
        model.consumeResult();
        model.observe(null); model.observe(screen);
        verify(screen).changed(NicknameSubmission.Result.IDLE);
        verify(screen, never()).changed(NicknameSubmission.Result.SAVED);
    }

    @Test public void viewModelStoreRetainsRequestUntilScreenIsActuallyFinished() {
        ViewModelStore store = new ViewModelStore();
        store.put("submission", model);
        model.observe(null);
        assertSame(model, store.get("submission"));
        store.clear();
        unique.setResult(true); drain();
        verify(backend, never()).allowed(anyString());
        verify(backend, never()).save(anyString(), anyString());
        verifyNoInteractions(analytics);
    }

    @Test public void changedAccountBeforeAiResultCannotWriteNickname() {
        unique.setResult(true); drain();
        when(backend.currentUid()).thenReturn("account-b");
        allowed.setResult(true); drain();
        verify(backend, never()).save(anyString(), anyString());
        verify(screen).changed(NicknameSubmission.Result.SESSION_CHANGED);
        verifyNoInteractions(analytics);
    }

    @Test public void logoutBeforeUniquenessCannotStartModeration() {
        when(backend.currentUid()).thenReturn(null);
        unique.setResult(true); drain();
        verify(backend, never()).allowed(anyString());
        verify(backend, never()).save(anyString(), anyString());
    }

    @Test public void changedAccountDuringWriteCannotPublishSuccessToNewSession() {
        finishChecks();
        when(backend.currentUid()).thenReturn("account-b");
        saved.setResult(null); drain();
        verify(screen, never()).changed(NicknameSubmission.Result.SAVED);
        verifyNoInteractions(analytics);
    }

    @Test public void takenNicknameDoesNotReachModeration() {
        unique.setResult(false); drain();
        verify(backend, never()).allowed(anyString());
        verify(analytics).trackOnboardingStep("uniqueness", "failure", "taken");
        verifyNoMoreInteractions(analytics);
    }

    @Test public void moderationFailureWhileStoppedIsDeliveredOnReturnWithoutPrivateData() {
        unique.setResult(true); drain(); model.observe(null);
        allowed.setException(new IllegalStateException("private message account-a privateNickname")); drain();
        model.observe(screen);
        verify(screen).changed(NicknameSubmission.Result.CONTENT_ERROR);
        verify(backend, never()).save(anyString(), anyString());
        verify(analytics).trackNicknameCheckError("", "");
        verifyNoMoreInteractions(analytics);
    }

    @Test public void rejectedContentIsNotSaved() {
        unique.setResult(true); drain(); allowed.setResult(false); drain();
        verify(backend, never()).save(anyString(), anyString());
        verify(analytics).trackOnboardingStep("content", "failure", "rejected");
    }

    @Test public void writeFailureIsReportedAndCanBeConsumedForRetry() {
        finishChecks(); saved.setException(new IllegalStateException("private message")); drain();
        verify(screen).changed(NicknameSubmission.Result.WRITE_ERROR);
        verify(analytics).trackOnboardingStep("profile_write", "failure", "unknown");
        verifyNoMoreInteractions(analytics);
        model.consumeResult(); assertFalse(model.hasPendingResult());
    }

    @Test public void uniquenessFailureIsReportedWithoutStartingAi() {
        unique.setException(new IllegalStateException("private message")); drain();
        verify(backend, never()).allowed(anyString());
        verify(analytics).trackOnboardingStep("uniqueness", "failure", "unknown");
        verifyNoMoreInteractions(analytics);
    }
}
