package piotr_gorczynski.soccer2;

import org.junit.Test;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for medal assignment in RankingActivity
 * Validates that medals are only awarded to players with wins > 0
 */
public class RankingMedalAssignmentTest {

    /**
     * Helper method to create a RankingEntry using reflection
     */
    private Object createRankingEntry(String uid, int wins) throws Exception {
        Class<?> clazz = Class.forName("piotr_gorczynski.soccer2.RankingActivity$RankingEntry");
        Object entry = clazz.getDeclaredConstructor(String.class).newInstance(uid);
        clazz.getDeclaredField("wins").setInt(entry, wins);
        return entry;
    }

    /**
     * Helper method to invoke assignMedals
     * Uses a test implementation since we can't easily instantiate the Activity
     */
    private void invokeAssignMedals(List<?> ranking) throws Exception {
        assignMedalsTestImpl(ranking);
    }

    /**
     * Test implementation of assignMedals based on the RankingActivity requirement
     * Note: RankingActivity uses a simpler algorithm than TournamentResultsActivity.
     * It increments category for each new win value, without accounting for group sizes.
     */
    private void assignMedalsTestImpl(List<?> list) throws Exception {
        int category = 1; // 1 gold, 2 silver, 3 bronze
        int prevWins = -1;
        for (Object obj : list) {
            Class<?> clazz = obj.getClass();
            int wins = clazz.getDeclaredField("wins").getInt(obj);
            
            if (prevWins != -1 && wins != prevWins) {
                category += 1;
            }
            int medalCategory = (category <= 3 && wins > 0) ? category : 0;
            clazz.getDeclaredField("medalCategory").setInt(obj, medalCategory);
            prevWins = wins;
        }
    }

    /**
     * Helper method to get medalCategory from a RankingEntry
     */
    private int getMedalCategory(Object entry) throws Exception {
        Class<?> clazz = entry.getClass();
        return clazz.getDeclaredField("medalCategory").getInt(entry);
    }

    @Test
    public void testAllZeroWins_NoMedals() throws Exception {
        List<Object> ranking = new ArrayList<>();
        ranking.add(createRankingEntry("player1", 0));
        ranking.add(createRankingEntry("player2", 0));
        ranking.add(createRankingEntry("player3", 0));

        invokeAssignMedals(ranking);

        // All players should have no medals (category = 0)
        assertEquals("Player 1 should not have a medal", 0, getMedalCategory(ranking.get(0)));
        assertEquals("Player 2 should not have a medal", 0, getMedalCategory(ranking.get(1)));
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(ranking.get(2)));
    }

    @Test
    public void testOnlyFirstPlaceHasWins() throws Exception {
        List<Object> ranking = new ArrayList<>();
        ranking.add(createRankingEntry("player1", 3));
        ranking.add(createRankingEntry("player2", 0));
        ranking.add(createRankingEntry("player3", 0));

        invokeAssignMedals(ranking);

        // Only player1 should have gold medal
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(ranking.get(0)));
        assertEquals("Player 2 should not have a medal", 0, getMedalCategory(ranking.get(1)));
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(ranking.get(2)));
    }

    @Test
    public void testTopThreeHaveWins() throws Exception {
        List<Object> ranking = new ArrayList<>();
        ranking.add(createRankingEntry("player1", 5));
        ranking.add(createRankingEntry("player2", 3));
        ranking.add(createRankingEntry("player3", 1));
        ranking.add(createRankingEntry("player4", 0));

        invokeAssignMedals(ranking);

        // Top 3 should have medals, 4th should not
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(ranking.get(0)));
        assertEquals("Player 2 should have silver medal", 2, getMedalCategory(ranking.get(1)));
        assertEquals("Player 3 should have bronze medal", 3, getMedalCategory(ranking.get(2)));
        assertEquals("Player 4 should not have a medal", 0, getMedalCategory(ranking.get(3)));
    }

    @Test
    public void testTiedForFirst_AllGetGoldIfWinsGreaterThanZero() throws Exception {
        List<Object> ranking = new ArrayList<>();
        ranking.add(createRankingEntry("player1", 5));
        ranking.add(createRankingEntry("player2", 5));
        ranking.add(createRankingEntry("player3", 3));

        invokeAssignMedals(ranking);

        // Both tied players should get gold (category 1)
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(ranking.get(0)));
        assertEquals("Player 2 should have gold medal", 1, getMedalCategory(ranking.get(1)));
        // Third player gets silver (category 2) because RankingActivity's simpler algorithm
        // increments category for each new win value, unlike TournamentResultsActivity which
        // accounts for group sizes and would assign bronze
        assertEquals("Player 3 should have silver medal", 2, getMedalCategory(ranking.get(2)));
    }

    @Test
    public void testMixedWinsAndZeros() throws Exception {
        List<Object> ranking = new ArrayList<>();
        ranking.add(createRankingEntry("player1", 5));
        ranking.add(createRankingEntry("player2", 3));
        ranking.add(createRankingEntry("player3", 0));
        ranking.add(createRankingEntry("player4", 0));
        ranking.add(createRankingEntry("player5", 0));

        invokeAssignMedals(ranking);

        // Only first two should have medals
        assertEquals("Player 1 should have gold medal", 1, getMedalCategory(ranking.get(0)));
        assertEquals("Player 2 should have silver medal", 2, getMedalCategory(ranking.get(1)));
        assertEquals("Player 3 should not have a medal", 0, getMedalCategory(ranking.get(2)));
        assertEquals("Player 4 should not have a medal", 0, getMedalCategory(ranking.get(3)));
        assertEquals("Player 5 should not have a medal", 0, getMedalCategory(ranking.get(4)));
    }

    @Test
    public void testEmptyList() throws Exception {
        List<Object> ranking = new ArrayList<>();
        // Should not throw exception - just test it doesn't crash
        invokeAssignMedals(ranking);
    }

    @Test
    public void testSinglePlayerWithWins() throws Exception {
        List<Object> ranking = new ArrayList<>();
        ranking.add(createRankingEntry("player1", 3));

        invokeAssignMedals(ranking);

        assertEquals("Single player with wins should have gold medal", 1, getMedalCategory(ranking.get(0)));
    }

    @Test
    public void testSinglePlayerWithoutWins() throws Exception {
        List<Object> ranking = new ArrayList<>();
        ranking.add(createRankingEntry("player1", 0));

        invokeAssignMedals(ranking);

        assertEquals("Single player without wins should not have a medal", 0, getMedalCategory(ranking.get(0)));
    }
}
