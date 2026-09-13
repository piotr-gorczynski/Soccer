package piotr_gorczynski.soccer2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TournamentVisibilityTest {

    @Test
    public void globalTournamentIsVisibleInEveryFlavour() {
        assertTrue(TournamentVisibility.isVisible(
                Collections.singletonList("global"),
                "bangladesh"
        ));
        assertTrue(TournamentVisibility.isVisible(
                Collections.singletonList("global"),
                "global"
        ));
    }

    @Test
    public void marketTournamentIsVisibleOnlyInMatchingFlavour() {
        assertTrue(TournamentVisibility.isVisible(
                Collections.singletonList("bangladesh"),
                "bangladesh"
        ));
        assertFalse(TournamentVisibility.isVisible(
                Collections.singletonList("bangladesh"),
                "global"
        ));
    }

    @Test
    public void tournamentCanExplicitlyListSeveralMarkets() {
        assertTrue(TournamentVisibility.isVisible(
                Arrays.asList("bangladesh", "india"),
                "india"
        ));
    }

    @Test
    public void missingVisibilityFieldRemainsBackwardCompatible() {
        assertTrue(TournamentVisibility.isVisible(null, "bangladesh"));
    }

    @Test
    public void emptyVisibilityListHidesTournament() {
        assertFalse(TournamentVisibility.isVisible(Collections.emptyList(), "bangladesh"));
    }
}
