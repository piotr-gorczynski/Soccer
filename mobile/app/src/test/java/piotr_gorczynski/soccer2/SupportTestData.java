package piotr_gorczynski.soccer2;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import java.util.*;

final class SupportTestData {
    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }
    static Map<String, Object> ticket(String id, String status, long version, long unread) {
        return map("ticketId", id, "reference", "SUP-EXAMPLE", "category", "player_report", "status", status,
                "supportReplyVersion", version, "unreadCount", unread, "canReply", !"closed".equals(status),
                "updatedAt", "2026-11-11T10:00:00.000Z", "latestSupportReply", "Public response",
                "latestSupportReplyAt", "2026-11-11T10:00:00.000Z");
    }
    static Map<String, Object> inbox(long count) {
        return map("conversations", new ArrayList<>(), "unreadCount", count, "nextCursor", null);
    }
    static Map<String, Object> message(String id, String author, String text) {
        return map("messageId", id, "authorType", author, "message", text, "createdAt", "2026-11-11T10:00:00.000Z");
    }
    static class Gateway implements SupportGateway {
        int inboxCalls, threadCalls, sendCalls, readCalls;
        String requestedCursor;
        long seenVersion;
        SupportDraft.Pending submitted;
        Task<Object> inboxResult = Tasks.forResult(SupportTestData.inbox(0));
        Task<Object> threadResult = Tasks.forResult(map("conversation", ticket("ticket", "open", 1, 1),
                "messages", Collections.singletonList(message("support1", "support", "Public response")),
                "initialMessage", null, "nextCursor", null));
        Task<Object> sendResult = new TaskCompletionSource<Object>().getTask();
        Task<Object> readResult = Tasks.forResult(map("ok", true, "unreadCount", 0));
        public Task<Object> inbox(String cursor) { inboxCalls++; requestedCursor = cursor; return inboxResult; }
        public Task<Object> conversation(String ticketId, String cursor) { threadCalls++; requestedCursor = cursor; return threadResult; }
        public Task<Object> send(String ticketId, SupportDraft.Pending pending) { sendCalls++; submitted = pending; return sendResult; }
        public Task<Object> markRead(String ticketId, long version) { readCalls++; seenVersion = version; return readResult; }
    }
}
