package piotr_gorczynski.soccer2;

import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=android.app.Application.class)
public class GameplayMilestonesTest {
    public static class Screen extends android.app.Activity {
        AnalyticsManager manager;
        GameplayMilestones state;
        @Override public void onCreate(android.os.Bundle saved) {
            super.onCreate(saved);
            manager=mock(AnalyticsManager.class);
            state=new GameplayMilestones(getSharedPreferences(LanguageManager.PREFS_FILE,0),manager);
        }
    }
    SharedPreferences prefs;
    AnalyticsManager analytics;
    GameplayMilestones milestones;
    @Before public void setup() {
        prefs=RuntimeEnvironment.getApplication().getSharedPreferences(LanguageManager.PREFS_FILE,0);
        prefs.edit().clear().commit();analytics=mock(AnalyticsManager.class);
        milestones=new GameplayMilestones(prefs,analytics);
    }
    @Test public void firstLossReportsCompletionAndLegacyOnceButNotVictory() {
        milestones.completed(false,2,true);milestones.completed(true,2,true);
        verify(analytics).trackFirstGameCompleted(false,2,true);
        verify(analytics).trackFirstGameWin(false,2,true);
        verify(analytics,never()).trackFirstWin(anyInt());
        verifyNoMoreInteractions(analytics);
    }
    @Test public void firstVictoryReportsCompletionAndWinOnce() {
        milestones.completed(true,2,false);assertTrue(milestones.won(2));
        milestones.completed(true,2,false);assertFalse(milestones.won(2));
        verify(analytics).trackFirstGameCompleted(true,2,false);
        verify(analytics).trackFirstGameWin(true,2,false);
        verify(analytics).trackFirstWin(2);verifyNoMoreInteractions(analytics);
    }
    @Test public void lossThenVictoryReportsFirstWinOnlyAtSecondGame() {
        milestones.completed(false,2,false);
        verify(analytics,never()).trackFirstWin(anyInt());
        milestones.completed(true,2,false);assertTrue(milestones.won(2));
        verify(analytics).trackFirstWin(2);
        verify(analytics).trackFirstGameCompleted(false,2,false);
        verify(analytics,never()).trackFirstGameCompleted(eq(true),anyInt(),anyBoolean());
        assertFalse(milestones.won(2));
    }
    @Test public void recreationAndAppReopeningSharePersistedFirstWinClaim() {
        assertTrue(milestones.won(2));
        var recreated=new GameplayMilestones(prefs,analytics);
        var reopened=new GameplayMilestones(prefs,analytics);
        assertFalse(recreated.won(2));assertFalse(reopened.won(2));
        verify(analytics,times(1)).trackFirstWin(2);
    }
    @Test public void separateInstancesCannotBothClaimFirstCompletionOrWin() {
        var other=new GameplayMilestones(prefs,analytics);
        milestones.completed(true,1,false);other.completed(true,1,false);
        assertTrue(other.won(1));assertFalse(milestones.won(1));
        verify(analytics).trackFirstGameCompleted(true,1,false);verify(analytics).trackFirstWin(1);
    }
    @Test public void actualActivityRecreationDoesNotRepeatCompletionOrVictory() {
        var controller=org.robolectric.Robolectric.buildActivity(Screen.class).setup();
        var first=controller.get();first.state.completed(true,2,false);assertTrue(first.state.won(2));
        verify(first.manager).trackFirstGameCompleted(true,2,false);verify(first.manager).trackFirstWin(2);
        controller.recreate();var rotated=controller.get();
        rotated.state.completed(true,2,false);assertFalse(rotated.state.won(2));
        verifyNoInteractions(rotated.manager);controller.pause().stop().destroy();
    }
    @Test public void onlineCompletionDoesNotBroadenLegacyEventCoverage() {
        milestones.completed(false,3,false);milestones.completed(true,3,false);
        verify(analytics).trackFirstGameCompleted(false,3,false);
        verify(analytics,never()).trackFirstGameWin(anyBoolean(),anyInt(),anyBoolean());
    }
    @Test public void historicalUnknownWinHistoryIsNotInventedOnUpgrade() {
        prefs.edit().clear().putBoolean("first_game_completed",true).commit();
        milestones=new GameplayMilestones(prefs,analytics);
        assertTrue(milestones.hasRecordedWin());assertFalse(milestones.won(2));
        milestones.completed(true,2,false);verifyNoInteractions(analytics);
    }
    @Test public void existingPromptTrackingSurvivesUpgradeAfterAnObservedLoss() {
        prefs.edit().putBoolean("first_game_completed",true)
                .putBoolean("first_win_prompt_initialized",true)
                .putBoolean("first_win_prompt_win_recorded",false).commit();
        milestones=new GameplayMilestones(prefs,analytics);
        assertTrue(milestones.won(2));verify(analytics).trackFirstWin(2);
    }
    @Test public void failedPersistentClaimDoesNotEmitOrEnablePrompt() {
        var failed=mock(SharedPreferences.class);var edit=mock(SharedPreferences.Editor.class);
        when(failed.edit()).thenReturn(edit);when(edit.putBoolean(anyString(),anyBoolean())).thenReturn(edit);
        milestones=new GameplayMilestones(failed,analytics);
        assertFalse(milestones.won(2));milestones.completed(true,2,false);
        verifyNoInteractions(analytics);
    }
    @Test public void sharedVictoryDefinitionCoversLocalAndOnlinePlayersAndRejectsInvalidResults() {
        assertTrue(GameplayMilestones.playerWon(2,0,0));assertFalse(GameplayMilestones.playerWon(2,1,0));
        assertTrue(GameplayMilestones.playerWon(1,0,0));
        assertTrue(GameplayMilestones.playerWon(3,1,1));assertFalse(GameplayMilestones.playerWon(3,1,0));
        assertFalse(GameplayMilestones.playerWon(3,-1,-1));assertFalse(GameplayMilestones.playerWon(9,0,0));
    }
}
