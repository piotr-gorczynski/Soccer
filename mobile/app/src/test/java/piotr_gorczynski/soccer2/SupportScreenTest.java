package piotr_gorczynski.soccer2;

import android.app.Application;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.functions.FirebaseFunctionsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 30, qualifiers = "w360dp-h640dp")
public class SupportScreenTest {
    static FirebaseAuth auth;
    static SupportTestData.Gateway gateway;
    static boolean online, focused;
    final AtomicReference<String> uid = new AtomicReference<>("owner");
    public static class Inbox extends HelpCentreActivity {
        @Override protected FirebaseAuth supportAuth() { return auth; }
        @Override protected SupportGateway createGateway() { return SupportScreenTest.gateway; }
        @Override protected boolean isDeviceOnline() { return online; }
        @Override protected void checkService(BackendServiceChecker.ServiceCheckCallback callback) { callback.onServiceAvailable(); }
    }
    public static class Conversation extends SupportConversationActivity {
        @Override protected FirebaseAuth supportAuth() { return auth; }
        @Override protected SupportGateway createGateway() { return SupportScreenTest.gateway; }
        @Override protected boolean isDeviceOnline() { return online; }
        @Override protected void checkService(BackendServiceChecker.ServiceCheckCallback callback) { callback.onServiceAvailable(); }
        @Override public boolean hasWindowFocus() { return focused; }
        // Robolectric's host stays GONE; model the foreground OS window signal.
        @Override protected boolean isSupportWindowVisible(View view) { return true; }
    }
    @Before public void prepare() {
        online = true; focused = false; gateway = new SupportTestData.Gateway(); auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class); when(user.isAnonymous()).thenReturn(true);
        when(auth.getUid()).thenAnswer(ignored -> uid.get());
        when(auth.getCurrentUser()).thenAnswer(ignored -> uid.get() == null ? null : user);
        androidx.test.core.app.ApplicationProvider.getApplicationContext()
                .getSharedPreferences("help_centre_drafts", 0).edit().clear().commit();
    }
    private ActivityController<Conversation> thread() {
        Intent intent = new Intent().putExtra(SupportConversationActivity.TICKET_ID, "ticket");
        return Robolectric.buildActivity(Conversation.class, intent).setup().visible().windowFocusChanged(true);
    }
    private void idle() { shadowOf(Looper.getMainLooper()).idle(); }
    private void layout(SupportScreenActivity activity) {
        View decor = activity.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 1080, 1920); idle();
    }
    private String viewState(Conversation activity) {
        try {
            var field = SupportConversationActivity.class.getDeclaredField("latestSupportView");
            field.setAccessible(true); View latest = (View) field.get(activity);
            android.graphics.Rect rect = new android.graphics.Rect();
            return latest == null ? "no latest view" : "shown=" + latest.isShown() + " window=" + latest.getWindowVisibility()
                    + " height=" + latest.getHeight() + " rect=" + latest.getGlobalVisibleRect(rect) + ":" + rect;
        } catch (Exception error) { return error.toString(); }
    }
    @Test public void inboxLoadsAndDoesNotMarkReadAndUsesPaginationCursor() {
        gateway.inboxResult = Tasks.forResult(SupportTestData.map("conversations",
                Collections.singletonList(SupportTestData.ticket("ticket", "open", 2, 2)),
                "unreadCount", 2, "nextCursor", "ticket"));
        try (var controller = Robolectric.buildActivity(Inbox.class).setup()) {
            idle(); Inbox activity = controller.get();
            assertEquals(1, gateway.inboxCalls); assertEquals(0, gateway.readCalls);
            assertEquals(View.VISIBLE, activity.findViewById(R.id.help_guest).getVisibility());
            activity.findViewById(R.id.help_load_more).performClick(); idle();
            assertEquals("ticket", gateway.requestedCursor);
        }
    }
    @Test public void offlineLaunchDoesNotReadSendOrMarkAnything() {
        online = false;
        try (var controller = thread()) {
            idle(); assertEquals(0, gateway.threadCalls); assertEquals(0, gateway.readCalls);
            assertEquals(0, gateway.sendCalls);
            assertEquals(View.VISIBLE, controller.get().findViewById(R.id.help_retry).getVisibility());
        }
    }
    @Test public void loadedConversationDoesNotMarkReadWithoutFocus() {
        try (var controller = thread()) {
            idle(); layout(controller.get());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(1, gateway.threadCalls); assertEquals(0, gateway.readCalls);
        }
    }
    @Test public void visibleFocusedLatestReplyMarksOnlyLoadedVersionOnce() {
        try (var controller = thread()) {
            idle(); layout(controller.get());
            ScrollView scroll = controller.get().findViewById(R.id.help_scroll);
            scroll.fullScroll(View.FOCUS_DOWN); layout(controller.get());
            focused = true; controller.get().onWindowFocusChanged(true);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(viewState(controller.get()), 1, gateway.readCalls); assertEquals(1, gateway.seenVersion);
            controller.get().onWindowFocusChanged(true);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(1, gateway.readCalls);
        }
    }
    @Test public void offscreenReplyDoesNotMarkRead() {
        try (var controller = thread()) {
            idle(); layout(controller.get());
            View rows = controller.get().findViewById(R.id.help_rows);
            rows.setVisibility(View.GONE);
            focused = true; controller.get().onWindowFocusChanged(true);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(0, gateway.readCalls);
        }
    }
    @Test public void leavingBeforeReadDelayDoesNotAcknowledgeReply() {
        try (var controller = thread()) {
            idle(); layout(controller.get()); focused = true;
            controller.get().onWindowFocusChanged(true); controller.pause().stop();
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(0, gateway.readCalls);
        }
    }
    @Test public void replyScrolledOutsideViewportDoesNotMarkRead() {
        java.util.List<Object> messages = new java.util.ArrayList<>();
        messages.add(SupportTestData.message("support0", "support", "Public response"));
        for (int i = 0; i < 40; i++) messages.add(SupportTestData.message("user" + i, "user", "Another message " + i));
        gateway.threadResult = Tasks.forResult(SupportTestData.map("conversation", SupportTestData.ticket("ticket", "open", 1, 1),
                "messages", messages));
        try (var controller = thread()) {
            idle(); layout(controller.get());
            ScrollView scroll = controller.get().findViewById(R.id.help_scroll);
            scroll.fullScroll(View.FOCUS_DOWN); layout(controller.get());
            assertTrue(scroll.getScrollY() > 0);
            focused = true; controller.get().onWindowFocusChanged(true);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(0, gateway.readCalls);
        }
    }
    @Test public void accountSwitchClosesScreenAndIgnoresOldResponse() {
        TaskCompletionSource<Object> response = new TaskCompletionSource<>(); gateway.threadResult = response.getTask();
        try (var controller = thread()) {
            idle();
            ArgumentCaptor<FirebaseAuth.AuthStateListener> capture = ArgumentCaptor.forClass(FirebaseAuth.AuthStateListener.class);
            verify(auth).addAuthStateListener(capture.capture());
            uid.set("other"); capture.getValue().onAuthStateChanged(auth);
            response.setResult(SupportTestData.map("conversation", SupportTestData.ticket("ticket", "open", 1, 1),
                    "messages", Collections.singletonList(SupportTestData.message("s", "support", "Private"))));
            idle(); assertTrue(controller.get().isFinishing()); assertEquals(0, gateway.readCalls);
            assertEquals(View.INVISIBLE, controller.get().findViewById(android.R.id.content).getVisibility());
        }
    }
    @Test public void emptyMessageDisabledAndNetworkFailureRetriesExactPendingRequest() {
        gateway.sendResult = Tasks.forException(new FirebaseFunctionsException("Private network text",
                FirebaseFunctionsException.Code.UNAVAILABLE, null));
        try (var controller = thread()) {
            idle(); Conversation activity = controller.get();
            EditText editor = activity.findViewById(R.id.help_message);
            Button send = activity.findViewById(R.id.help_send);
            editor.setText("   "); assertFalse(send.isEnabled());
            editor.setText("My reply"); assertTrue(send.isEnabled()); send.performClick(); idle();
            assertEquals(1, gateway.sendCalls); String requestId = gateway.submitted.id;
            assertFalse(editor.isEnabled());
            assertFalse(((TextView) activity.findViewById(R.id.help_status)).getText().toString().contains("Private network text"));
            send.performClick(); idle(); assertEquals(2, gateway.sendCalls);
            assertEquals(requestId, gateway.submitted.id); assertEquals("My reply", gateway.submitted.text);
        }
    }
    @Test public void successfulSendClearsDraftAndReloadsConversation() {
        gateway.sendResult = Tasks.forResult(SupportTestData.map("ok", true));
        try (var controller = thread()) {
            idle(); EditText editor = controller.get().findViewById(R.id.help_message);
            editor.setText("My reply"); controller.get().findViewById(R.id.help_send).performClick(); idle();
            assertEquals("", editor.getText().toString()); assertEquals(2, gateway.threadCalls);
            assertNull(new SupportDraft(controller.get(), "owner", "ticket").pending);
        }
    }
    @Test public void confirmedMessageAfterRestartClearsPendingWithoutSendingAgain() {
        Application app = androidx.test.core.app.ApplicationProvider.getApplicationContext();
        SupportDraft draft = new SupportDraft(app, "owner", "ticket"); draft.edit("Already delivered");
        String id = draft.begin(false).id;
        gateway.threadResult = Tasks.forResult(SupportTestData.map("conversation", SupportTestData.ticket("ticket", "open", 0, 0),
                "messages", Collections.singletonList(SupportTestData.message("user_" + id, "user", "Already delivered"))));
        try (var controller = thread()) {
            idle(); assertEquals(0, gateway.sendCalls);
            assertEquals("", ((EditText) controller.get().findViewById(R.id.help_message)).getText().toString());
            assertNull(new SupportDraft(app, "owner", "ticket").pending);
        }
    }
    @Test public void closedConversationHasNoComposer() {
        gateway.threadResult = Tasks.forResult(SupportTestData.map("conversation", SupportTestData.ticket("ticket", "closed", 1, 0),
                "messages", Collections.emptyList()));
        try (var controller = thread()) {
            idle(); assertEquals(View.GONE, controller.get().findViewById(R.id.help_composer).getVisibility());
            assertEquals(0, gateway.sendCalls);
        }
    }
    @Test public void reopeningRequiresConfirmationAndSendsExplicitFlag() {
        gateway.threadResult = Tasks.forResult(SupportTestData.map("conversation", SupportTestData.ticket("ticket", "resolved", 1, 0),
                "messages", Collections.emptyList()));
        try (var controller = thread()) {
            idle(); EditText editor = controller.get().findViewById(R.id.help_message);
            editor.setText("Still unresolved"); controller.get().findViewById(R.id.help_send).performClick(); idle();
            assertEquals(0, gateway.sendCalls);
            androidx.appcompat.app.AlertDialog dialog = (androidx.appcompat.app.AlertDialog)
                    org.robolectric.shadows.ShadowDialog.getLatestDialog();
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick(); idle();
            assertEquals(1, gateway.sendCalls); assertTrue(gateway.submitted.reopen);
        }
    }
    @Test public void failedReadReceiptIsRetriedOnlyAfterFreshLoadAndNeverInventsSuccess() {
        gateway.readResult = Tasks.forException(new IllegalStateException("Private"));
        try (var controller = thread()) {
            idle(); layout(controller.get()); focused = true; controller.get().onWindowFocusChanged(true);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(1, gateway.readCalls);
            controller.get().onWindowFocusChanged(true);
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(1, gateway.readCalls);
            gateway.readResult = Tasks.forResult(SupportTestData.map("ok", true, "unreadCount", 0));
            controller.get().refresh(); idle(); layout(controller.get());
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
            assertEquals(2, gateway.readCalls);
        }
    }
    @Test public void badgeLayoutOffersOneAccessibleTargetAndHidesZeroBadge() {
        try (var controller = Robolectric.buildActivity(Inbox.class).setup()) {
            View action = controller.get().getLayoutInflater().inflate(R.layout.action_help_inbox, new FrameLayout(controller.get()), false);
            assertTrue(action.isClickable()); assertTrue(action.isFocusable());
            assertEquals(View.GONE, action.findViewById(R.id.help_unread_badge).getVisibility());
            assertEquals(controller.get().getString(R.string.help_centre), action.getContentDescription());
        }
    }
}
