package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Test cases for GameActivity.onNewIntent crash prevention fixes
 * 
 * This test validates that the onNewIntent method no longer throws
 * IllegalStateException for legitimate scenarios and properly handles
 * different types of intent updates.
 */
@RunWith(AndroidJUnit4.class)
public class GameActivityOnNewIntentTest {

    private TestableGameActivity gameActivity;
    private Intent originalIntent;
    private Intent newIntent;

    /**
     * Testable subclass of GameActivity that exposes onNewIntent for testing
     * and allows us to control the current intent
     */
    private static class TestableGameActivity extends GameActivity {
        private Intent currentIntent;
        
        public TestableGameActivity() {
            super();
        }
        
        public void setCurrentIntent(Intent intent) {
            this.currentIntent = intent;
        }
        
        @Override
        public Intent getIntent() {
            return currentIntent != null ? currentIntent : super.getIntent();
        }
        
        // Expose onNewIntent for testing
        public void callOnNewIntent(Intent intent) {
            super.onNewIntent(intent);
        }
    }

    @Before
    public void setUp() {
        gameActivity = new TestableGameActivity();
        
        // Create original intent with a matchPath
        originalIntent = new Intent(ApplicationProvider.getApplicationContext(), GameActivity.class);
        originalIntent.putExtra("matchPath", "matches/test123");
        originalIntent.putExtra("GameType", 3);
        gameActivity.setCurrentIntent(originalIntent);
        
        // Create new intent
        newIntent = new Intent(ApplicationProvider.getApplicationContext(), GameActivity.class);
    }

    @Test
    public void testOnNewIntent_withIdenticalMatchPath_shouldNotCrash() {
        // Mock Log.d to capture debug messages
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            // Setup - new intent with same matchPath
            newIntent.putExtra("matchPath", "matches/test123");
            newIntent.putExtra("GameType", 3);
            
            // Execute - this should not throw an exception
            gameActivity.callOnNewIntent(newIntent);
            
            // Verify - should log that matchPath is identical
            mockedLog.verify(() -> Log.d(eq("TAG_Soccer"), 
                Mockito.contains(": identical matchPath, skipping")));
        }
    }

    @Test
    public void testOnNewIntent_withNullMatchPaths_shouldNotCrash() {
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            // Setup - both intents have null matchPath
            originalIntent.removeExtra("matchPath");
            gameActivity.setCurrentIntent(originalIntent);
            // newIntent doesn't have matchPath set
            
            // Execute - this should not throw an exception
            gameActivity.callOnNewIntent(newIntent);
            
            // Verify - should log that paths are identical (both null)
            mockedLog.verify(() -> Log.d(eq("TAG_Soccer"), 
                Mockito.contains(": identical matchPath, skipping")));
        }
    }

    @Test
    public void testOnNewIntent_withDifferentMatchPath_shouldNotCrash() {
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            // Setup - new intent with different matchPath
            newIntent.putExtra("matchPath", "matches/different456");
            newIntent.putExtra("GameType", 3);
            
            // Execute - this should not throw an exception (previously would crash)
            gameActivity.callOnNewIntent(newIntent);
            
            // Verify - should log warning about different matchPath for online game
            mockedLog.verify(() -> Log.w(eq("TAG_Soccer"), 
                Mockito.contains(": Warning - Different matchPath for online game")));
            
            // Verify - should log successful intent update
            mockedLog.verify(() -> Log.d(eq("TAG_Soccer"), 
                Mockito.contains(": Intent updated successfully")));
        }
    }

    @Test
    public void testOnNewIntent_withNullToNonNullMatchPath_shouldNotCrash() {
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            // Setup - original intent has null matchPath, new intent has non-null
            originalIntent.removeExtra("matchPath");
            gameActivity.setCurrentIntent(originalIntent);
            newIntent.putExtra("matchPath", "matches/new789");
            newIntent.putExtra("GameType", 3);
            
            // Execute - this should not throw an exception
            gameActivity.callOnNewIntent(newIntent);
            
            // Verify - should log successful intent update (no warning since currentPath is null)
            mockedLog.verify(() -> Log.d(eq("TAG_Soccer"), 
                Mockito.contains(": Intent updated successfully")));
        }
    }

    @Test
    public void testOnNewIntent_withNonNullToNullMatchPath_shouldNotCrash() {
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            // Setup - original intent has matchPath, new intent has null
            // newIntent doesn't have matchPath set (null)
            newIntent.putExtra("GameType", 3);
            
            // Execute - this should not throw an exception
            gameActivity.callOnNewIntent(newIntent);
            
            // Verify - should log successful intent update (no warning since newPath is null)
            mockedLog.verify(() -> Log.d(eq("TAG_Soccer"), 
                Mockito.contains(": Intent updated successfully")));
        }
    }

    @Test
    public void testOnNewIntent_withNullIntent_shouldNotCrash() {
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            // Execute - this should not throw an exception
            gameActivity.callOnNewIntent(null);
            
            // Verify - should handle null intent gracefully
            mockedLog.verify(() -> Log.d(eq("TAG_Soccer"), 
                Mockito.contains(": Intent updated successfully")));
        }
    }

    @Test
    public void testOnNewIntent_forNonOnlineGame_shouldNotWarn() {
        try (MockedStatic<Log> mockedLog = Mockito.mockStatic(Log.class)) {
            // Setup - original intent for offline game (GameType != 3)
            originalIntent.putExtra("GameType", 1); // Player vs Player offline
            gameActivity.setCurrentIntent(originalIntent);
            
            newIntent.putExtra("matchPath", "matches/different456");
            newIntent.putExtra("GameType", 1);
            
            // Execute - this should not throw an exception
            gameActivity.callOnNewIntent(newIntent);
            
            // Verify - should NOT log warning (only for online games)
            mockedLog.verify(() -> Log.w(eq("TAG_Soccer"), 
                Mockito.contains(": Warning - Different matchPath for online game")), 
                Mockito.never());
            
            // Verify - should log successful intent update
            mockedLog.verify(() -> Log.d(eq("TAG_Soccer"), 
                Mockito.contains(": Intent updated successfully")));
        }
    }
}