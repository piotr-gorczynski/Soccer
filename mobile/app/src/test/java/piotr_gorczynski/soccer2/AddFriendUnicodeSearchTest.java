package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AddFriendUnicodeSearchTest {

    @Test
    public void BengaliNicknameMatchesTheExactSearchText() {
        String nickname = "তদশশ";

        assertTrue(AddFriendActivity.normalizeSearchText(nickname)
                .contains(AddFriendActivity.normalizeSearchText(nickname)));
    }

    @Test
    public void ArabicNicknameWithWhitespaceMatchesTheExactSearchText() {
        String nickname = "منافس منافس";

        assertTrue(AddFriendActivity.normalizeSearchText(nickname)
                .contains(AddFriendActivity.normalizeSearchText(nickname)));
    }

    @Test
    public void CanonicallyEquivalentUnicodeTextIsNormalizedBeforeComparison() {
        assertEquals(AddFriendActivity.normalizeSearchText("Caf\u00e9"),
                AddFriendActivity.normalizeSearchText("Cafe\u0301"));
    }
}
