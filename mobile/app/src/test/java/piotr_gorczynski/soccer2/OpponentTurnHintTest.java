package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OpponentTurnHintTest {

    @Test
    public void onlineGameShowsWaitingHintOnlyDuringOpponentTurn() {
        assertTrue(Field.shouldShowWaitingForOpponent(3, 0, 1));
        assertTrue(Field.shouldShowWaitingForOpponent(3, 1, 0));

        assertFalse(Field.shouldShowWaitingForOpponent(3, 0, 0));
        assertFalse(Field.shouldShowWaitingForOpponent(3, 1, 1));
    }

    @Test
    public void nonOnlineGamesNeverShowWaitingHint() {
        assertFalse(Field.shouldShowWaitingForOpponent(1, 0, 1));
        assertFalse(Field.shouldShowWaitingForOpponent(2, 0, 1));
    }
}
