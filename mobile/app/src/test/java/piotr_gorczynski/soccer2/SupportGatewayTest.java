package piotr_gorczynski.soccer2;

import android.app.Application;
import android.os.Looper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableReference;
import com.google.firebase.functions.HttpsCallableResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 30)
public class SupportGatewayTest {
    @Test public void callablePayloadsContainOnlyApprovedFieldsAndNeverCallerIdentity() {
        try (MockedStatic<FirebaseFunctions> factory = mockStatic(FirebaseFunctions.class)) {
            FirebaseFunctions functions = mock(FirebaseFunctions.class);
            HttpsCallableReference reference = mock(HttpsCallableReference.class);
            HttpsCallableResult result = mock(HttpsCallableResult.class);
            when(result.getData()).thenReturn(SupportTestData.map("ok", true));
            factory.when(() -> FirebaseFunctions.getInstance("us-central1")).thenReturn(functions);
            List<String> names = new ArrayList<>(); List<Map<?, ?>> payloads = new ArrayList<>();
            when(functions.getHttpsCallable(anyString())).thenAnswer(invocation -> {
                names.add(invocation.getArgument(0)); return reference;
            });
            when(reference.call(any())).thenAnswer(invocation -> {
                payloads.add(invocation.getArgument(0)); return Tasks.forResult(result);
            });
            SupportGateway gateway = SupportGateway.firebase();
            gateway.inbox(null); gateway.conversation("ticket", "older");
            gateway.send("ticket", new SupportDraft.Pending("stable_request_id_123", "My reply", true));
            gateway.markRead("ticket", 3); shadowOf(Looper.getMainLooper()).idle();
            assertEquals(Arrays.asList("listSupportConversations", "getSupportConversation",
                    "sendSupportMessage", "markSupportConversationRead"), names);
            assertTrue(payloads.get(0).isEmpty());
            assertEquals(new HashSet<>(Arrays.asList("ticketId", "cursor")), payloads.get(1).keySet());
            assertEquals(new HashSet<>(Arrays.asList("ticketId", "message", "requestId", "reopen")), payloads.get(2).keySet());
            assertEquals("stable_request_id_123", payloads.get(2).get("requestId"));
            assertEquals(true, payloads.get(2).get("reopen"));
            assertEquals(new HashSet<>(Arrays.asList("ticketId", "seenReplyVersion")), payloads.get(3).keySet());
            assertEquals(3L, payloads.get(3).get("seenReplyVersion"));
        }
    }
}
