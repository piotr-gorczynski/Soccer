package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.google.firebase.messaging.RemoteMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import piotr_gorczynski.soccer2.notifications.MyFirebaseMessagingService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test cases for MyFirebaseMessagingService crash prevention fixes
 * Tests the fix for android.app.ActivityThread.throwRemoteServiceException
 */
@RunWith(AndroidJUnit4.class)
@Config(sdk = 28)
public class MyFirebaseMessagingServiceTest {

    /**
     * Helper method to create a RemoteMessage from a data map
     */
    private RemoteMessage createRemoteMessage(Map<String, String> data) {
        RemoteMessage.Builder builder = new RemoteMessage.Builder("test");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (entry.getValue() != null) {
                builder.addData(entry.getKey(), entry.getValue());
            }
        }
        return builder.build();
    }

    /**
     * Test that the service handles null title gracefully without crashing
     */
    @Test
    public void testOnMessageReceived_withNullTitle_shouldNotCrash() {
        MyFirebaseMessagingService service = new MyFirebaseMessagingService();
        
        // Create a RemoteMessage with null title
        Map<String, String> data = new HashMap<>();
        data.put("type", "invite");
        data.put("title", null);
        data.put("body", "Test body");
        data.put("inviteId", "test123");
        data.put("fromNickname", "TestUser");
        
        // This should not throw an exception
        try {
            RemoteMessage message = createRemoteMessage(data);
            
            // The service should handle this gracefully
            service.onMessageReceived(message);
            
            // Test passes if we reach this point without exception
        } catch (Exception e) {
            fail("Service should not crash with null title: " + e.getMessage());
        }
    }

    /**
     * Test that the service handles null body gracefully without crashing
     */
    @Test
    public void testOnMessageReceived_withNullBody_shouldNotCrash() {
        MyFirebaseMessagingService service = new MyFirebaseMessagingService();
        
        // Create a RemoteMessage with null body
        Map<String, String> data = new HashMap<>();
        data.put("type", "invite");
        data.put("title", "Test title");
        data.put("body", null);
        data.put("inviteId", "test123");
        data.put("fromNickname", "TestUser");
        
        // This should not throw an exception
        try {
            RemoteMessage message = createRemoteMessage(data);
            
            // The service should handle this gracefully
            service.onMessageReceived(message);
            
            // Test passes if we reach this point without exception
        } catch (Exception e) {
            fail("Service should not crash with null body: " + e.getMessage());
        }
    }

    /**
     * Test that the service handles empty title gracefully without crashing
     */
    @Test
    public void testOnMessageReceived_withEmptyTitle_shouldNotCrash() {
        MyFirebaseMessagingService service = new MyFirebaseMessagingService();
        
        // Create a RemoteMessage with empty title
        Map<String, String> data = new HashMap<>();
        data.put("type", "invite");
        data.put("title", "");
        data.put("body", "Test body");
        data.put("inviteId", "test123");
        data.put("fromNickname", "TestUser");
        
        // This should not throw an exception
        try {
            RemoteMessage message = createRemoteMessage(data);
            
            // The service should handle this gracefully
            service.onMessageReceived(message);
            
            // Test passes if we reach this point without exception
        } catch (Exception e) {
            fail("Service should not crash with empty title: " + e.getMessage());
        }
    }

    /**
     * Test that the service handles both null title and body gracefully without crashing
     */
    @Test
    public void testOnMessageReceived_withBothNullTitleAndBody_shouldNotCrash() {
        MyFirebaseMessagingService service = new MyFirebaseMessagingService();
        
        // Create a RemoteMessage with both null title and body
        Map<String, String> data = new HashMap<>();
        data.put("type", "invite");
        data.put("inviteId", "test123");
        data.put("fromNickname", "TestUser");
        
        // This should not throw an exception
        try {
            RemoteMessage message = createRemoteMessage(data);
            
            // The service should handle this gracefully
            service.onMessageReceived(message);
            
            // Test passes if we reach this point without exception
        } catch (Exception e) {
            fail("Service should not crash with null title and body: " + e.getMessage());
        }
    }

    /**
     * Test that the service correctly ignores start messages
     */
    @Test
    public void testOnMessageReceived_withStartType_shouldBeIgnored() {
        MyFirebaseMessagingService service = new MyFirebaseMessagingService();
        
        // Create a RemoteMessage with type "start"
        Map<String, String> data = new HashMap<>();
        data.put("type", "start");
        data.put("title", "Test title");
        data.put("body", "Test body");
        
        // This should not throw an exception and should be silently ignored
        try {
            RemoteMessage message = createRemoteMessage(data);
            
            // The service should ignore this message
            service.onMessageReceived(message);
            
            // Test passes if we reach this point without exception
        } catch (Exception e) {
            fail("Service should handle start messages without crashing: " + e.getMessage());
        }
    }
}
