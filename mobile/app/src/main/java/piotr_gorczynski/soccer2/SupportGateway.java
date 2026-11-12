package piotr_gorczynski.soccer2;

import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.HashMap;
import java.util.Map;

interface SupportGateway {
    Task<Object> inbox(String cursor);
    Task<Object> conversation(String ticketId, String cursor);
    Task<Object> send(String ticketId, SupportDraft.Pending pending);
    Task<Object> markRead(String ticketId, long version);

    static SupportGateway firebase() {
        return new SupportGateway() {
            private Task<Object> call(String name, Map<String, Object> data) {
                return FirebaseFunctions.getInstance("us-central1").getHttpsCallable(name)
                        .call(data).continueWith(task -> {
                            if (!task.isSuccessful()) throw task.getException();
                            return task.getResult().getData();
                        });
            }
            public Task<Object> inbox(String cursor) {
                Map<String, Object> data = new HashMap<>();
                if (cursor != null) data.put("cursor", cursor);
                return call("listSupportConversations", data);
            }
            public Task<Object> conversation(String ticketId, String cursor) {
                Map<String, Object> data = new HashMap<>();
                data.put("ticketId", ticketId);
                if (cursor != null) data.put("cursor", cursor);
                return call("getSupportConversation", data);
            }
            public Task<Object> send(String ticketId, SupportDraft.Pending pending) {
                Map<String, Object> data = new HashMap<>();
                data.put("ticketId", ticketId);
                data.put("message", pending.text);
                data.put("requestId", pending.id);
                data.put("reopen", pending.reopen);
                return call("sendSupportMessage", data);
            }
            public Task<Object> markRead(String ticketId, long version) {
                Map<String, Object> data = new HashMap<>();
                data.put("ticketId", ticketId);
                data.put("seenReplyVersion", version);
                return call("markSupportConversationRead", data);
            }
        };
    }
}
