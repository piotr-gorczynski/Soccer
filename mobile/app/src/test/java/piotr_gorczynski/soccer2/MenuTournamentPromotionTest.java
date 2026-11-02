package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=MenuAdNavigationTest.TestApp.class,sdk=34)
public class MenuTournamentPromotionTest {
    public static class TestMenu extends MenuActivity {
        int loginLaunches,adCalls;
        final TaskCompletionSource<Boolean> terms=new TaskCompletionSource<>();
        @Override protected boolean canContinueAfterAd() { return true; }
        @Override protected void launchTournamentPromotionLogin() { loginLaunches++; }
        @Override public void showAdThenRun(Runnable action) { adCalls++; }
        @Override protected Task<Boolean> readTermsAcceptance(String uid,TermsPolicy policy) {return terms.getTask();}
    }
    TestMenu menu;
    FirebaseAuth auth;
    FirebaseFirestore db;
    AnalyticsManager analytics;
    DocumentSnapshot tournament;
    TaskCompletionSource<DocumentSnapshot> tournamentResponse,participantResponse;
    DocumentReference reference;
    MockedStatic<FirebaseAuth> authSdk;
    MockedStatic<FirebaseFirestore> firestore;

    @Before public void setup() throws Exception {
        menu=Robolectric.buildActivity(TestMenu.class).get(); menu.setTheme(R.style.AppTheme);
        menu.setContentView(R.layout.activity_menu);
        menu.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit().clear().commit();
        auth=mock(FirebaseAuth.class); when(auth.getUid()).thenReturn("owner");
        authSdk=mockStatic(FirebaseAuth.class); authSdk.when(FirebaseAuth::getInstance).thenReturn(auth);
        db=mock(FirebaseFirestore.class); firestore=mockStatic(FirebaseFirestore.class);
        firestore.when(FirebaseFirestore::getInstance).thenReturn(db);
        analytics=mock(AnalyticsManager.class); set("analyticsManager",analytics);
        DocumentSnapshot profile=mock(DocumentSnapshot.class); when(profile.getString("nickname")).thenReturn("saved");
        set("nicknameProfileTask",Tasks.forResult(profile)); set("nicknameProfileUid","owner"); set("confirmedTermsUid","owner");
        set("menuInitializationPending",false); set("menuResumeWorkPending",false);
        CollectionReference tournaments=mock(CollectionReference.class),participants=mock(CollectionReference.class);
        reference=mock(DocumentReference.class); DocumentReference participant=mock(DocumentReference.class);
        when(db.collection("tournaments")).thenReturn(tournaments); when(tournaments.document("cup")).thenReturn(reference);
        when(reference.collection("participants")).thenReturn(participants); when(participants.document("owner")).thenReturn(participant);
        tournamentResponse=new TaskCompletionSource<>(); participantResponse=new TaskCompletionSource<>();
        when(reference.get(Source.SERVER)).thenReturn(tournamentResponse.getTask());
        when(participant.get(Source.SERVER)).thenReturn(participantResponse.getTask());
        tournament=mock(DocumentSnapshot.class);
        when(tournament.exists()).thenReturn(true); when(tournament.getId()).thenReturn("cup");
        when(tournament.getString("status")).thenReturn("registering");
        when(tournament.get("visibleInFlavours")).thenReturn(List.of("bangladesh"));
        when(tournament.getTimestamp("registrationDeadline")).thenReturn(new Timestamp(new java.util.Date(System.currentTimeMillis()+60_000)));
        when(tournament.get("maxParticipants")).thenReturn(20L); when(tournament.get("participantsCount")).thenReturn(1L);
        when(tournament.getString("regulation")).thenReturn("rules"); when(tournament.getReference()).thenReturn(reference);
    }
    @After public void cleanup() { firestore.close();authSdk.close(); }
    void set(String name,Object value)throws Exception {Field f=MenuActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(menu,value);}
    void resume()throws Exception {Method m=MenuActivity.class.getDeclaredMethod("continueTournamentPromotion");m.setAccessible(true);m.invoke(menu);}
    void click()throws Exception {Method m=MenuActivity.class.getDeclaredMethod("openTournamentPromotion",TournamentPromotion.class);m.setAccessible(true);m.invoke(menu,TournamentPromotion.parse(TournamentPromotionTest.response()));}
    void drain(){shadowOf(Looper.getMainLooper()).idle();}
    void select(){PendingTournamentPromotion.select(menu,"cup","owner",System.currentTimeMillis());}
    void participant(boolean joined){DocumentSnapshot doc=mock(DocumentSnapshot.class);when(doc.exists()).thenReturn(joined);participantResponse.setResult(doc);drain();}

    @Test public void guestClickLaunchesExistingLoginWithoutAdAndGlobalDoesNothing()throws Exception {
        when(auth.getUid()).thenReturn(null); click(); click();
        assertEquals(AnalyticsManager.isBangladesh()?1:0,menu.loginLaunches); assertEquals(0,menu.adCalls);
        if(AnalyticsManager.isBangladesh())verify(analytics,times(1)).trackTournamentPromotionClick("cup");
        else verifyNoInteractions(analytics);
    }
    @Test public void completedProfileAndTermsResumeSelectedTournamentWithoutJoiningAutomatically()throws Exception {
        select();resume();
        if(!AnalyticsManager.isBangladesh()){verifyNoInteractions(db);return;}
        tournamentResponse.setResult(tournament);drain();participant(false);
        assertNull(shadowOf(menu).getNextStartedActivity());
        menu.terms.setResult(true);drain();
        Intent target=shadowOf(menu).getNextStartedActivity();
        assertEquals(RegulationActivity.class.getName(),target.getComponent().getClassName());
        assertEquals("cup",target.getStringExtra("tournamentId"));assertEquals("rules",target.getStringExtra("regulationId"));
        assertNull(PendingTournamentPromotion.forUser(menu,"owner",System.currentTimeMillis()));
        verify(analytics).trackTournamentJoinStart("cup",true);assertEquals(0,menu.adCalls);
    }
    @Test public void missingNicknameOrUnconfirmedTermsNeverNavigates()throws Exception {
        select();set("confirmedTermsUid",null);resume();verifyNoInteractions(db);
        set("confirmedTermsUid","owner");DocumentSnapshot profile=mock(DocumentSnapshot.class);
        set("nicknameProfileTask",Tasks.forResult(profile));resume();verifyNoInteractions(db);
    }
    @Test public void closedOrFullTournamentIsRevalidatedAndSelectionCleared()throws Exception {
        if(!AnalyticsManager.isBangladesh())return;
        select();resume();when(tournament.get("participantsCount")).thenReturn(20L);
        tournamentResponse.setResult(tournament);drain();
        assertNull(shadowOf(menu).getNextStartedActivity());
        assertNull(PendingTournamentPromotion.forUser(menu,"owner",System.currentTimeMillis()));
        verify(reference,never()).collection("participants");
    }
    @Test public void existingParticipantOpensLobbyRatherThanDuplicateJoin()throws Exception {
        if(!AnalyticsManager.isBangladesh())return;
        select();resume();tournamentResponse.setResult(tournament);drain();participant(true);
        menu.terms.setResult(true);drain();
        assertEquals(TournamentLobbyActivity.class.getName(),shadowOf(menu).getNextStartedActivity().getComponent().getClassName());
        verifyNoInteractions(analytics);
    }
    @Test public void offlineReadKeepsDestinationForRetryWithoutOpeningScreen()throws Exception {
        if(!AnalyticsManager.isBangladesh())return;
        select();resume();tournamentResponse.setException(new IllegalStateException("network"));drain();
        assertEquals("cup",PendingTournamentPromotion.forUser(menu,"owner",System.currentTimeMillis()));
        assertNull(shadowOf(menu).getNextStartedActivity());
    }
}
