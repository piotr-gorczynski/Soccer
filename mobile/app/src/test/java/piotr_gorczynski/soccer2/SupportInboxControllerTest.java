package piotr_gorczynski.soccer2;

import android.app.Application;
import android.os.Looper;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 30)
public class SupportInboxControllerTest {
    final AtomicReference<String> uid = new AtomicReference<>("owner");
    final AtomicBoolean online = new AtomicBoolean(true);
    final FirebaseAuth auth = mock(FirebaseAuth.class);
    final SupportTestData.Gateway gateway = new SupportTestData.Gateway();
    final SupportInboxController.Listener listener = mock(SupportInboxController.Listener.class);
    SupportInboxController controller() {
        when(auth.getUid()).thenAnswer(ignored -> uid.get());
        return new SupportInboxController(auth, gateway, online::get, listener);
    }
    @Test public void signedOutHasNoBadgeAndMakesNoRequest() {
        uid.set(null);
        SupportInboxController controller = controller(); controller.start();
        assertEquals(0, gateway.inboxCalls);
        verify(listener, atLeastOnce()).show(false, 0, true);
        controller.stop();
    }
    @Test public void foregroundFetchPollAndReturnRefreshDoNotMarkRead() {
        gateway.inboxResult = Tasks.forResult(SupportTestData.inbox(4));
        SupportInboxController controller = controller(); controller.start(); shadowOf(Looper.getMainLooper()).idle();
        verify(listener).show(true, 4, true);
        controller.refresh(false); assertEquals(1, gateway.inboxCalls);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(60));
        assertEquals(2, gateway.inboxCalls);
        controller.stop();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(120));
        assertEquals(2, gateway.inboxCalls);
        controller.start(); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(3, gateway.inboxCalls); assertEquals(0, gateway.readCalls); controller.stop();
    }
    @Test public void oldAccountCallbackCannotPopulateNewAccountBadge() {
        TaskCompletionSource<Object> old = new TaskCompletionSource<>(); gateway.inboxResult = old.getTask();
        SupportInboxController controller = controller(); controller.start();
        ArgumentCaptor<FirebaseAuth.AuthStateListener> capture = ArgumentCaptor.forClass(FirebaseAuth.AuthStateListener.class);
        verify(auth).addAuthStateListener(capture.capture());
        uid.set("other"); gateway.inboxResult = Tasks.forResult(SupportTestData.inbox(2));
        capture.getValue().onAuthStateChanged(auth); shadowOf(Looper.getMainLooper()).idle();
        clearInvocations(listener);
        old.setResult(SupportTestData.inbox(99)); shadowOf(Looper.getMainLooper()).idle();
        verifyNoInteractions(listener); controller.stop();
    }
    @Test public void logoutClearsCountAndOldCallbackIsIgnored() {
        TaskCompletionSource<Object> delayed = new TaskCompletionSource<>(); gateway.inboxResult = delayed.getTask();
        SupportInboxController controller = controller(); controller.start();
        uid.set(null); controller.refresh(true);
        verify(listener).accountChanged("owner");
        verify(listener, atLeastOnce()).show(false, 0, true);
        clearInvocations(listener);
        delayed.setResult(SupportTestData.inbox(7)); shadowOf(Looper.getMainLooper()).idle();
        verifyNoInteractions(listener); controller.stop();
    }
    @Test public void offlineAndStoppedCallbacksDoNotUpdateCount() {
        TaskCompletionSource<Object> delayed = new TaskCompletionSource<>(); gateway.inboxResult = delayed.getTask();
        SupportInboxController controller = controller(); controller.start(); controller.stop();
        clearInvocations(listener); delayed.setResult(SupportTestData.inbox(7)); shadowOf(Looper.getMainLooper()).idle();
        verifyNoInteractions(listener);
        online.set(false); controller.start();
        assertEquals(1, gateway.inboxCalls);
        verify(listener, atLeastOnce()).show(true, 0, false); controller.stop();
    }
    @Test public void failedRequestDoesNotInventOrClearUnreadMessages() {
        gateway.inboxResult = Tasks.forResult(SupportTestData.inbox(5));
        SupportInboxController controller = controller(); controller.start(); shadowOf(Looper.getMainLooper()).idle();
        gateway.inboxResult = Tasks.forException(new IllegalStateException("private error"));
        clearInvocations(listener); controller.refresh(true); shadowOf(Looper.getMainLooper()).idle();
        verify(listener, atLeastOnce()).show(true, 5, true);
        verify(listener, never()).show(true, 0, true); controller.stop();
    }
    @Test public void queuedAuthCallbackAfterStopDoesNotTouchTheMenu() {
        SupportInboxController controller = controller(); controller.start();
        ArgumentCaptor<FirebaseAuth.AuthStateListener> capture = ArgumentCaptor.forClass(FirebaseAuth.AuthStateListener.class);
        verify(auth).addAuthStateListener(capture.capture()); controller.stop(); clearInvocations(listener);
        uid.set("other"); capture.getValue().onAuthStateChanged(auth);
        verifyNoInteractions(listener);
    }
}
