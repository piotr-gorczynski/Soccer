package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

public class PrizeReminderPolicyTest {
    @Test public void navigationDoesNotRepeatButForegroundReturnDoes() {
        PrizeReminderPolicy policy = new PrizeReminderPolicy();
        assertTrue(policy.canRemind("winner"));
        policy.markShown("winner");
        assertFalse(policy.canRemind("winner"));
        assertFalse(policy.canRemind("winner"));
        policy.startSession();
        assertTrue(policy.canRemind("winner"));
    }

    @Test public void switchingAccountsDoesNotSuppressAnotherWinnersReminder() {
        PrizeReminderPolicy policy = new PrizeReminderPolicy();
        assertFalse(policy.canRemind(null));
        policy.markShown("first");
        assertTrue(policy.canRemind("second"));
        assertFalse(policy.canRemind("first"));
    }

    @Test public void missingOrRejectedDetailsQualify() {
        assertTrue(PrizeReminderPolicy.needsDetails("awaiting_details"));
        assertTrue(PrizeReminderPolicy.needsDetails("action_required"));
        for (String status : new String[] {null, "", "ready_for_processing", "processing",
                "sent", "completed", "cancelled"}) {
            assertFalse("Unexpected reminder for " + status, PrizeReminderPolicy.needsDetails(status));
        }
    }

    @Test public void checkingWithoutShowingDoesNotConsumeReminder() {
        PrizeReminderPolicy policy = new PrizeReminderPolicy();
        assertTrue(policy.canRemind("winner"));
        assertFalse(PrizeReminderPolicy.needsDetails("ready_for_processing"));
        assertTrue(policy.canRemind("winner"));
    }
}
