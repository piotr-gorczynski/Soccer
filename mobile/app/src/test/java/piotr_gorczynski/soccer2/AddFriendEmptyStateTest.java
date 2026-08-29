package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddFriendEmptyStateTest {

    @Test
    public void checkingFilterBeforeFirstSearchDoesNotShowEmptyState() {
        assertFalse(AddFriendActivity.shouldShowEmptyState(0, null));
    }

    @Test
    public void emptyCompletedSearchShowsEmptyState() {
        assertTrue(AddFriendActivity.shouldShowEmptyState(0, "player"));
    }

    @Test
    public void visibleResultsHideEmptyState() {
        assertFalse(AddFriendActivity.shouldShowEmptyState(1, "player"));
    }
}
