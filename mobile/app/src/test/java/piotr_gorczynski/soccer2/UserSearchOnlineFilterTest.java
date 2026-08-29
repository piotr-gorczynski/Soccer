package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserSearchOnlineFilterTest {

    @Test
    public void filterDisabledShowsEveryPresenceState() {
        assertTrue(UserSearchAdapter.shouldShowPresence(false, "online"));
        assertTrue(UserSearchAdapter.shouldShowPresence(false, "active"));
        assertTrue(UserSearchAdapter.shouldShowPresence(false, "offline"));
        assertTrue(UserSearchAdapter.shouldShowPresence(false, null));
    }

    @Test
    public void filterEnabledShowsOnlineAndPreviouslyActiveUsers() {
        assertTrue(UserSearchAdapter.shouldShowPresence(true, "online"));
        assertTrue(UserSearchAdapter.shouldShowPresence(true, "active"));
        assertFalse(UserSearchAdapter.shouldShowPresence(true, "offline"));
        assertFalse(UserSearchAdapter.shouldShowPresence(true, null));
    }
}
