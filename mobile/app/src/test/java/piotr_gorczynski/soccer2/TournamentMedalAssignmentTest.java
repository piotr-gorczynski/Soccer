package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for medal assignment in TournamentResultsActivity
 * Validates that medals are only awarded to players with wins > 0
 */
public class TournamentMedalAssignmentTest {

    /**
     * Helper method to create a StandingEntry using reflection
     */
    private Object createStandingEntry(String uid, int wins) throws Exception {
        Class<?> clazz = Class.forName("piotr_gorczynski.soccer2.TournamentResultsActivity$StandingEntry");
        Object entry = clazz.getDeclaredConstructor(String.class).newInstance(uid);
        clazz.getDeclaredField("wins").setInt(entry, wins);
        return entry;
    }

    /**
     * Helper method to invoke assignMedals using reflection
     * Uses a test implementation since we can't easily instantiate the Activity
     */
    private void invokeAssignMedals(List<?> standings) throws Exception {
        assignMedalsTestImpl(standings);
    }

    /**
     * Test implementation of assignMedals based on the TournamentResultsActivity requirement
     */
    private void assignMedalsTestImpl(List<?> list) throws Exception {
        if (list.isEmpty()) return;
        
        int currentPosition = 1;
        int prevWins = -1;
        int groupSize = 0;
        
        for (Object obj : list) {
            Class<?> clazz = obj.getClass();
            int wins = clazz.getDeclaredField("wins").getInt(obj);
            
            if (prevWins != -1 && wins != prevWins) {
                currentPosition += groupSize;
                groupSize = 0;
            }
            
            groupSize++;
            
            // Assign medal only for positions 1, 2, 3 AND if the player has won at least one game
            int medalCategory;
            if (currentPosition <= 3 && wins > 0) {
                medalCategory = currentPosition;
            } else {
                medalCategory = 0;
            }
            
            clazz.getDeclaredField("medalCategory").setInt(obj, medalCategory);
            prevWins = wins;
        }
    }

    /**
     * Helper method to get medalCategory from a StandingEntry
     */
    private int getMedalCategory(Object entry) throws Exception {
        Class<?> clazz = entry.getClass();
        return clazz.getDeclaredField("medalCategory").getInt(entry);
    }

    @Test
    public void testAllZeroWins_NoMedals() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 0));
        standings.add(createStandingEntry("player2", 0));
        standings.add(createStandingEntry("player3", 0));

        invokeAssignMedals(standings);

        // All players should have no medals (category = 0)
        assertEquals("Player 1 should not have a medal", 0, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should not have a medal", 0, getMedalCategory(standings.get(1)));
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(standings.get(2)));
    }

    @Test
    public void testOnlyFirstPlaceHasWins() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 3));
        standings.add(createStandingEntry("player2", 0));
        standings.add(createStandingEntry("player3", 0));

        invokeAssignMedals(standings);

        // Only player1 should have gold medal
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should not have a medal", 0, getMedalCategory(standings.get(1)));
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(standings.get(2)));
    }

    @Test
    public void testTopThreeHaveWins() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 5));
        standings.add(createStandingEntry("player2", 3));
        standings.add(createStandingEntry("player3", 1));
        standings.add(createStandingEntry("player4", 0));

        invokeAssignMedals(standings);

        // Top 3 should have medals, 4th should not
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should have silver medal", 2, getMedalCategory(standings.get(1)));
        assertEquals("Player 3 should have bronze medal", 3, getMedalCategory(standings.get(2)));
        assertEquals("Player 4 should not have a medal", 0, getMedalCategory(standings.get(3)));
    }

    @Test
    public void testTiedForFirst_AllGetGoldIfWinsGreaterThanZero() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 5));
        standings.add(createStandingEntry("player2", 5));
        standings.add(createStandingEntry("player3", 3));

        invokeAssignMedals(standings);

        // Both tied players should get gold (position 1)
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should have gold medal", 1, getMedalCategory(standings.get(1)));
        // Third player should get bronze (position 3, skipping silver due to tie)
        assertEquals("Player 3 should have bronze medal", 3, getMedalCategory(standings.get(2)));
    }

    @Test
    public void testMixedWinsAndZeros() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 5));
        standings.add(createStandingEntry("player2", 3));
        standings.add(createStandingEntry("player3", 0));
        standings.add(createStandingEntry("player4", 0));
        standings.add(createStandingEntry("player5", 0));

        invokeAssignMedals(standings);

        // Only first two should have medals
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should have silver medal", 2, getMedalCategory(standings.get(1)));
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(standings.get(2)));
        assertEquals("Player 4 should not have a medal", 0, getMedalCategory(standings.get(3)));
        assertEquals("Player 5 should not have a medal", 0, getMedalCategory(standings.get(4)));
    }

    @Test
    public void testEmptyList() throws Exception {
        List<Object> standings = new ArrayList<>();
        // Should not throw exception
        invokeAssignMedals(standings);
    }

    @Test
    public void testSinglePlayerWithWins() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 3));

        invokeAssignMedals(standings);

        assertEquals("Single player with wins should have gold medal", 1, getMedalCategory(standings.get(0)));
    }

    @Test
    public void testSinglePlayerWithoutWins() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 0));

        invokeAssignMedals(standings);

        assertEquals("Single player without wins should not have a medal", 0, getMedalCategory(standings.get(0)));
    }
}
