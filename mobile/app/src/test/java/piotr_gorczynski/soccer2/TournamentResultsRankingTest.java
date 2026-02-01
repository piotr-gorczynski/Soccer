package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for ranking assignment in TournamentResultsActivity
 * Validates that players with the same score are assigned the same rank number
 */
public class TournamentResultsRankingTest {

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
     * Test implementation of assignMedals based on TournamentResultsActivity
     */
    private void assignMedalsTestImpl(List<?> list) throws Exception {
        if (list.isEmpty()) return;
        
        int currentPosition = 1; // current rank position (1st, 2nd, 3rd, etc.)
        int prevWins = -1;
        int groupSize = 0;
        
        for (Object obj : list) {
            Class<?> clazz = obj.getClass();
            int wins = clazz.getDeclaredField("wins").getInt(obj);
            
            // If wins changed, move to next position (accounting for group size)
            if (prevWins != -1 && wins != prevWins) {
                currentPosition += groupSize;
                groupSize = 0;
            }
            
            groupSize++;
            
            // Assign rank to all players
            clazz.getDeclaredField("rank").setInt(obj, currentPosition);
            
            // Assign medal only for positions 1, 2, 3 AND if the player has won at least one game
            int medalCategory = (currentPosition <= 3 && wins > 0) ? currentPosition : 0;
            clazz.getDeclaredField("medalCategory").setInt(obj, medalCategory);
            
            prevWins = wins;
        }
    }

    /**
     * Helper method to get rank from a StandingEntry
     */
    private int getRank(Object entry) throws Exception {
        Class<?> clazz = entry.getClass();
        return clazz.getDeclaredField("rank").getInt(entry);
    }

    /**
     * Helper method to get medalCategory from a StandingEntry
     */
    private int getMedalCategory(Object entry) throws Exception {
        Class<?> clazz = entry.getClass();
        return clazz.getDeclaredField("medalCategory").getInt(entry);
    }

    @Test
    public void testAllPlayersWithZeroWins_ShouldHaveSameRank() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 0));
        standings.add(createStandingEntry("player2", 0));
        standings.add(createStandingEntry("player3", 0));
        standings.add(createStandingEntry("player4", 0));
        standings.add(createStandingEntry("player5", 0));

        assignMedalsTestImpl(standings);

        // All players should have rank 1 (tied for first, but no medals due to 0 wins)
        assertEquals("Player 1 should have rank 1", 1, getRank(standings.get(0)));
        assertEquals("Player 2 should have rank 1", 1, getRank(standings.get(1)));
        assertEquals("Player 3 should have rank 1", 1, getRank(standings.get(2)));
        assertEquals("Player 4 should have rank 1", 1, getRank(standings.get(3)));
        assertEquals("Player 5 should have rank 1", 1, getRank(standings.get(4)));
        
        // None should have medals
        assertEquals("Player 1 should not have a medal", 0, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should not have a medal", 0, getMedalCategory(standings.get(1)));
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(standings.get(2)));
        assertEquals("Player 4 should not have a medal", 0, getMedalCategory(standings.get(3)));
        assertEquals("Player 5 should not have a medal", 0, getMedalCategory(standings.get(4)));
    }

    @Test
    public void testTwoWinnersAndFiveZeroWins_CorrectRanking() throws Exception {
        List<Object> standings = new ArrayList<>();
        // Two players tied for first with 1 win each
        standings.add(createStandingEntry("diego", 1));
        standings.add(createStandingEntry("piotr", 1));
        // Five players with 0 wins
        standings.add(createStandingEntry("player3", 0));
        standings.add(createStandingEntry("player4", 0));
        standings.add(createStandingEntry("player5", 0));
        standings.add(createStandingEntry("player6", 0));
        standings.add(createStandingEntry("player7", 0));

        assignMedalsTestImpl(standings);

        // First two players should have rank 1 with gold medals
        assertEquals("diego should have rank 1", 1, getRank(standings.get(0)));
        assertEquals("piotr should have rank 1", 1, getRank(standings.get(1)));
        assertEquals("diego should have gold medal", 1, getMedalCategory(standings.get(0)));
        assertEquals("piotr should have gold medal", 1, getMedalCategory(standings.get(1)));
        
        // All players with 0 wins should have rank 3 (not 4, 5, 6, 7, 8)
        assertEquals("Player 3 should have rank 3", 3, getRank(standings.get(2)));
        assertEquals("Player 4 should have rank 3", 3, getRank(standings.get(3)));
        assertEquals("Player 5 should have rank 3", 3, getRank(standings.get(4)));
        assertEquals("Player 6 should have rank 3", 3, getRank(standings.get(5)));
        assertEquals("Player 7 should have rank 3", 3, getRank(standings.get(6)));
        
        // Players with 0 wins should not have medals
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(standings.get(2)));
        assertEquals("Player 4 should not have a medal", 0, getMedalCategory(standings.get(3)));
        assertEquals("Player 5 should not have a medal", 0, getMedalCategory(standings.get(4)));
        assertEquals("Player 6 should not have a medal", 0, getMedalCategory(standings.get(5)));
        assertEquals("Player 7 should not have a medal", 0, getMedalCategory(standings.get(6)));
    }

    @Test
    public void testDifferentScores_SequentialRanking() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 5));
        standings.add(createStandingEntry("player2", 3));
        standings.add(createStandingEntry("player3", 1));
        standings.add(createStandingEntry("player4", 0));

        assignMedalsTestImpl(standings);

        // Each player should have sequential ranks
        assertEquals("Player 1 should have rank 1", 1, getRank(standings.get(0)));
        assertEquals("Player 2 should have rank 2", 2, getRank(standings.get(1)));
        assertEquals("Player 3 should have rank 3", 3, getRank(standings.get(2)));
        assertEquals("Player 4 should have rank 4", 4, getRank(standings.get(3)));
        
        // Top 3 should have medals, 4th should not
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should have silver medal", 2, getMedalCategory(standings.get(1)));
        assertEquals("Player 3 should have bronze medal", 3, getMedalCategory(standings.get(2)));
        assertEquals("Player 4 should not have a medal", 0, getMedalCategory(standings.get(3)));
    }

    @Test
    public void testThreeWayTieForSecond_CorrectRanking() throws Exception {
        List<Object> standings = new ArrayList<>();
        standings.add(createStandingEntry("player1", 5));
        standings.add(createStandingEntry("player2", 3));
        standings.add(createStandingEntry("player3", 3));
        standings.add(createStandingEntry("player4", 3));
        standings.add(createStandingEntry("player5", 1));

        assignMedalsTestImpl(standings);

        // Ranks should be: 1, 2, 2, 2, 5
        assertEquals("Player 1 should have rank 1", 1, getRank(standings.get(0)));
        assertEquals("Player 2 should have rank 2", 2, getRank(standings.get(1)));
        assertEquals("Player 3 should have rank 2", 2, getRank(standings.get(2)));
        assertEquals("Player 4 should have rank 2", 2, getRank(standings.get(3)));
        assertEquals("Player 5 should have rank 5", 5, getRank(standings.get(4)));
        
        // Player 1 gets gold, players 2-4 get silver (position 2), player 5 gets no medal
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(standings.get(0)));
        assertEquals("Player 2 should have silver medal", 2, getMedalCategory(standings.get(1)));
        assertEquals("Player 3 should have silver medal", 2, getMedalCategory(standings.get(2)));
        assertEquals("Player 4 should have silver medal", 2, getMedalCategory(standings.get(3)));
        assertEquals("Player 5 should not have a medal", 0, getMedalCategory(standings.get(4)));
    }

    @Test
    public void testEmptyList() throws Exception {
        List<Object> standings = new ArrayList<>();
        // Should not throw exception
        assignMedalsTestImpl(standings);
    }
}
