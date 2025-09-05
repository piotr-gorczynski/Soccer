package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.Intent;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.ArrayList;
import static org.junit.Assert.*;

/**
 * Test cases for GameActivity crash prevention fixes
 */
@RunWith(AndroidJUnit4.class)
public class GameActivityCrashTest {

    @Test
    public void testGameViewConstructor_withNullContext_shouldThrowException() {
        ArrayList<MoveTo> moves = new ArrayList<>();
        moves.add(new MoveTo(3, 4, 0));
        
        try {
            new GameView(null, moves, 1, 1);
            fail("Expected IllegalArgumentException for null context");
        } catch (IllegalArgumentException e) {
            assertEquals("Context cannot be null", e.getMessage());
        }
    }

    @Test
    public void testGameViewConstructor_withNullMoves_shouldThrowException() {
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            new GameView(context, null, 1, 1);
            fail("Expected IllegalArgumentException for null moves");
        } catch (IllegalArgumentException e) {
            assertEquals("Moves list cannot be null", e.getMessage());
        }
    }

    @Test
    public void testGameViewConstructor_withEmptyMoves_shouldThrowException() {
        Context context = ApplicationProvider.getApplicationContext();
        ArrayList<MoveTo> emptyMoves = new ArrayList<>();
        
        try {
            new GameView(context, emptyMoves, 1, 1);
            fail("Expected IllegalArgumentException for empty moves");
        } catch (IllegalArgumentException e) {
            assertEquals("Moves list cannot be empty", e.getMessage());
        }
    }

    @Test
    public void testGameViewMultiplayerConstructor_withNullContext_shouldThrowException() {
        ArrayList<MoveTo> moves = new ArrayList<>();
        moves.add(new MoveTo(3, 4, 0));
        
        try {
            new GameView(null, moves, 3, "Player 1", "Player 2", 0, 300, 300, null);
            fail("Expected IllegalArgumentException for null context");
        } catch (IllegalArgumentException e) {
            assertEquals("Context cannot be null", e.getMessage());
        }
    }

    @Test
    public void testGameViewMultiplayerConstructor_withNullMoves_shouldThrowException() {
        Context context = ApplicationProvider.getApplicationContext();
        
        try {
            new GameView(context, null, 3, "Player 1", "Player 2", 0, 300, 300, null);
            fail("Expected IllegalArgumentException for null moves");
        } catch (IllegalArgumentException e) {
            assertEquals("Moves list cannot be null", e.getMessage());
        }
    }
}