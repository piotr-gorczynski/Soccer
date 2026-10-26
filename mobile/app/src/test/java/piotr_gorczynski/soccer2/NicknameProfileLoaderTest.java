package piotr_gorczynski.soccer2;

import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = android.app.Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class NicknameProfileLoaderTest {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private void idle(long millis) {
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));
    }

    @Test public void transientFailureRetriesAndReturnsExistingNickname() {
        AtomicInteger reads = new AtomicInteger();
        DocumentSnapshot profile = mock(DocumentSnapshot.class);
        when(profile.getString("nickname")).thenReturn("Existing nickname");
        Task<DocumentSnapshot> task = new NicknameProfileLoader(() -> reads.incrementAndGet() == 1
                ? Tasks.forException(new Exception("network")) : Tasks.forResult(profile), handler).start();
        idle(0);
        assertFalse(task.isComplete());
        idle(NicknameProfileLoader.RETRY_DELAY_MS);
        assertTrue(task.isSuccessful());
        assertEquals("Existing nickname", task.getResult().getString("nickname"));
        assertEquals(2, reads.get());
    }

    @Test public void repeatedFailuresRemainErrorsInsteadOfMissingProfiles() {
        AtomicInteger reads = new AtomicInteger();
        Task<DocumentSnapshot> task = new NicknameProfileLoader(() -> {
            reads.incrementAndGet();
            return Tasks.forException(new Exception("unavailable"));
        }, handler).start();
        idle(NicknameProfileLoader.TIMEOUT_MS);
        assertTrue(task.isComplete());
        assertFalse(task.isSuccessful());
        assertNotNull(task.getException());
        assertEquals(3, reads.get());
    }

    @Test public void stalledReadTimesOutAndLateSuccessCannotStartOnboarding() {
        TaskCompletionSource<DocumentSnapshot> pending = new TaskCompletionSource<>();
        Task<DocumentSnapshot> task = new NicknameProfileLoader(pending::getTask, handler).start();
        idle(NicknameProfileLoader.TIMEOUT_MS);
        assertTrue(task.getException() instanceof TimeoutException);
        pending.setResult(mock(DocumentSnapshot.class));
        idle(0);
        assertFalse(task.isSuccessful());
        assertTrue(task.getException() instanceof TimeoutException);
    }

    @Test public void confirmedMissingNicknameIsSuccessfulNotANetworkError() {
        DocumentSnapshot profile = mock(DocumentSnapshot.class);
        when(profile.exists()).thenReturn(true);
        Task<DocumentSnapshot> task = new NicknameProfileLoader(() -> Tasks.forResult(profile), handler).start();
        idle(0);
        assertTrue(task.isSuccessful());
        assertNull(task.getResult().getString("nickname"));
    }
    @Test public void unavailableBackendDoesNotOpenNicknameSetupWithoutLocalNickname() throws Exception {
        MenuActivity activity = mock(MenuActivity.class, CALLS_REAL_METHODS);
        android.content.SharedPreferences prefs = mock(android.content.SharedPreferences.class);
        when(prefs.getString("nickname", null)).thenReturn(null);
        Runnable openNickname = mock(Runnable.class);
        java.lang.reflect.Field available = MenuActivity.class.getDeclaredField("isBackendAvailable");
        available.setAccessible(true);
        available.setBoolean(activity, false);
        java.lang.reflect.Method fetch = MenuActivity.class.getDeclaredMethod("fetchNicknameFromFirestore",
                String.class, android.content.SharedPreferences.class, Runnable.class);
        fetch.setAccessible(true);
        fetch.invoke(activity, "existing-account", prefs, openNickname);
        verify(openNickname, never()).run();
    }

}
