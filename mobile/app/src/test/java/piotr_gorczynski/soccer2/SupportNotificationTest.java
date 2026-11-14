package piotr_gorczynski.soccer2;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import androidx.test.core.app.ApplicationProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.RemoteMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import piotr_gorczynski.soccer2.notifications.MyFirebaseMessagingService;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk=30, qualifiers="en")
public class SupportNotificationTest {
    private Context app;
    @Before public void reset() {
        app=ApplicationProvider.getApplicationContext();
        app.getSharedPreferences("support_push_versions",0).edit().clear().commit();
        app.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit()
                .putString(LanguageManager.PREF_LANGUAGE_CODE,"en").commit();
    }
    private RemoteMessage push(String ticket,String version) {
        return new RemoteMessage.Builder("test").addData("type","support_ticket_updated")
                .addData("ticketId",ticket).addData("replyVersion",version)
                .addData("body","PRIVATE TICKET TEXT").build();
    }
    private Notification[] notifications() {
        return shadowOf((NotificationManager)app.getSystemService(Context.NOTIFICATION_SERVICE))
                .getAllNotifications().toArray(new Notification[0]);
    }
    @Test public void replyWithoutPayoutCreatesGenericNotificationAndCorrectDeepLink() {
        FirebaseAuth auth=mock(FirebaseAuth.class); when(auth.getUid()).thenReturn("owner");
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            MyFirebaseMessagingService service=Robolectric.buildService(MyFirebaseMessagingService.class).create().get();
            service.onMessageReceived(push("ticket","1"));
            assertEquals(1,notifications().length);
            Notification notification=notifications()[0];
            assertEquals(app.getString(R.string.notification_support_reply),notification.extras.getString(Notification.EXTRA_TEXT));
            Intent route=shadowOf(notification.contentIntent).getSavedIntent();
            assertEquals(SupportNotificationEntryActivity.class.getName(),route.getComponent().getClassName());
            assertEquals("ticket",route.getStringExtra(SupportConversationActivity.TICKET_ID));
            assertEquals("ticket",route.getData().getLastPathSegment());
        }
    }
    @Test public void focusedConversationRefreshesWithoutNotificationOrImplicitRead() {
        FirebaseAuth auth=mock(FirebaseAuth.class); when(auth.getUid()).thenReturn("owner");
        SupportUpdates.Listener listener=mock(SupportUpdates.Listener.class);
        Object owner=new Object(); SupportUpdates.visible(owner,"owner","ticket"); SupportUpdates.add(listener);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            MyFirebaseMessagingService service=Robolectric.buildService(MyFirebaseMessagingService.class).create().get();
            service.onMessageReceived(push("ticket","1")); shadowOf(Looper.getMainLooper()).idle();
            verify(listener).changed("ticket"); assertEquals(0,notifications().length);
            // Another conversation still alerts.
            service.onMessageReceived(push("other","1")); assertEquals(1,notifications().length);
        } finally { SupportUpdates.hidden(owner); SupportUpdates.remove(listener); }
    }
    @Test public void duplicatesAndOutOfOrderVersionsAreAccountBound() {
        assertTrue(SupportUpdates.firstDelivery(app,"owner","ticket",2));
        assertFalse(SupportUpdates.firstDelivery(app,"owner","ticket",2));
        assertFalse(SupportUpdates.firstDelivery(app,"owner","ticket",1));
        assertTrue(SupportUpdates.firstDelivery(app,"owner","ticket",3));
        assertTrue(SupportUpdates.firstDelivery(app,"other","ticket",2));
    }
    @Test public void loggedOutAndInvalidPayloadsDoNotNotify() {
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            MyFirebaseMessagingService service=Robolectric.buildService(MyFirebaseMessagingService.class).create().get();
            service.onMessageReceived(push("ticket","1"));
            when(auth.getUid()).thenReturn("owner");
            service.onMessageReceived(push("../bad","1")); service.onMessageReceived(push("ticket","invalid"));
            assertEquals(0,notifications().length);
        }
    }
    @Test public void inboxUsesConversationCountWithLegacyCompatibility() {
        java.util.Map<String,Object> data=SupportTestData.inbox(8);
        data.put("unreadConversationCount",2);
        assertEquals(2,new SupportModels.Inbox(data).unread);
        data.remove("unreadConversationCount"); assertEquals(8,new SupportModels.Inbox(data).unread);
    }
    @Test public void clickBuildsMenuInboxAndRequestedConversationStack() {
        FirebaseAuth auth=mock(FirebaseAuth.class); when(auth.getUid()).thenReturn("owner");
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            var controller=Robolectric.buildActivity(SupportNotificationEntryActivity.class,
                    new Intent().putExtra(SupportConversationActivity.TICKET_ID,"ticket")).create();
            java.util.Set<String> screens=new java.util.HashSet<>();
            Intent next;
            while ((next=shadowOf(controller.get()).getNextStartedActivity()) != null) {
                screens.add(next.getComponent().getClassName());
                if (next.getComponent().getClassName().equals(SupportConversationActivity.class.getName()))
                    assertEquals("ticket",next.getStringExtra(SupportConversationActivity.TICKET_ID));
            }
            assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(MenuActivity.class.getName(),
                    HelpCentreActivity.class.getName(),SupportConversationActivity.class.getName())),screens);
            controller.destroy();
        }
    }
    @Test public void clickWhileSignedOutOpensMenuWithoutProtectedConversation() {
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            var controller=Robolectric.buildActivity(SupportNotificationEntryActivity.class,
                    new Intent().putExtra(SupportConversationActivity.TICKET_ID,"ticket")).create();
            assertEquals(MenuActivity.class.getName(),shadowOf(controller.get()).getNextStartedActivity().getComponent().getClassName());
            assertNull(shadowOf(controller.get()).getNextStartedActivity()); controller.destroy();
        }
    }
}
