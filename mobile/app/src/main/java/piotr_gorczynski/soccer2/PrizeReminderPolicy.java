package piotr_gorczynski.soccer2;

import java.util.HashSet;
import java.util.Set;

/** Remind each signed-in user at most once per foreground session. */
final class PrizeReminderPolicy {
    private final Set<String> remindedUsers = new HashSet<>();

    void startSession() {
        remindedUsers.clear();
    }

    boolean canRemind(String uid) {
        return uid != null && !remindedUsers.contains(uid);
    }

    void markShown(String uid) {
        remindedUsers.add(uid);
    }

    static boolean needsDetails(String status) {
        return "awaiting_details".equals(status) || "action_required".equals(status);
    }
}
