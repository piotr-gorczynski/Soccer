package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AddFriendInitialLoadTest {

    @Test
    public void emptyQueryBrowsesAllUsers() {
        assertTrue(AddFriendActivity.isBrowseAllQuery(
                AddFriendActivity.normalizeSearchText("")));
    }

    @Test
    public void nicknameQueryUsesFilteredSearch() {
        assertFalse(AddFriendActivity.isBrowseAllQuery(
                AddFriendActivity.normalizeSearchText("Player")));
    }
}
