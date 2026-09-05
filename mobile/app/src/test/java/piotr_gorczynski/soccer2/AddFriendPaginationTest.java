package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddFriendPaginationTest {

    @Test
    public void loadMoreContinuesPastAFullPageWithNoMatches() {
        assertTrue(AddFriendActivity.shouldContinueLoading(0, true));
    }

    @Test
    public void loadMoreStopsAfterAppendingResults() {
        assertFalse(AddFriendActivity.shouldContinueLoading(1, true));
    }

    @Test
    public void loadMoreStopsAtEndOfCollection() {
        assertFalse(AddFriendActivity.shouldContinueLoading(0, false));
    }

    @Test
    public void initialSearchContinuesUntilItFindsAMatch() {
        assertTrue(AddFriendActivity.shouldContinueLoading(0, true));
    }

    @Test
    public void loadMoreIsHiddenBeforeLastLoadedItemIsVisible() {
        assertFalse(AddFriendActivity.shouldRevealLoadMore(true, false, 20, 18));
    }

    @Test
    public void loadMoreIsRevealedAtLastLoadedItem() {
        assertTrue(AddFriendActivity.shouldRevealLoadMore(true, false, 20, 19));
    }

    @Test
    public void loadMoreStaysHiddenWhileLoadingOrWithoutAnotherPage() {
        assertFalse(AddFriendActivity.shouldRevealLoadMore(true, true, 20, 19));
        assertFalse(AddFriendActivity.shouldRevealLoadMore(false, false, 20, 19));
    }

    @Test
    public void loadMoreStaysHiddenForEmptyResults() {
        assertFalse(AddFriendActivity.shouldRevealLoadMore(true, false, 0, -1));
    }
}
