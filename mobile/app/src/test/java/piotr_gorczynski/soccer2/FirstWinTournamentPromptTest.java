package piotr_gorczynski.soccer2;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.TaskCompletionSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class FirstWinTournamentPromptTest {
    public static class Screen extends AppCompatActivity {
        @Override protected void onCreate(Bundle b) {setTheme(R.style.AppTheme);super.onCreate(b);}
    }
    Screen activity;
    SharedPreferences prefs;
    AnalyticsManager analytics;
    TaskCompletionSource<Object> request;
    AtomicInteger loads,joins,declines,replacements;
    AtomicLong clock;
    AtomicReference<String> uid;
    FirstWinTournamentPrompt prompt;
    boolean visible;
    @Before public void setup() {
        activity=Robolectric.buildActivity(Screen.class).setup().get();
        prefs=activity.getSharedPreferences(LanguageManager.PREFS_FILE,0);prefs.edit().clear().commit();
        analytics=mock(AnalyticsManager.class);request=new TaskCompletionSource<>();
        loads=new AtomicInteger();joins=new AtomicInteger();declines=new AtomicInteger();replacements=new AtomicInteger();
        clock=new AtomicLong(1000);uid=new AtomicReference<>();visible=true;
    }
    @After public void cleanup() {if(prompt!=null) prompt.stop();activity.finish();}
    void bd() {Assume.assumeTrue(AnalyticsManager.isBangladesh());}
    void create() {
        prompt=new FirstWinTournamentPrompt(activity,prefs,()->{loads.incrementAndGet();return request.getTask();},
                uid::get,()->visible,replacements::incrementAndGet,offer->{
                    joins.incrementAndGet();
                    PendingTournamentPromotion.select(activity,offer.id,uid.get(),System.currentTimeMillis(),"first_win");
                },declines::incrementAndGet,analytics,clock::get);
    }
    Object response(String... methods) {
        var r=TournamentPromotionTest.response();
        @SuppressWarnings("unchecked") var offer=(Map<String,Object>)r.get("tournament");
        offer.put("payoutMethods",List.of(methods));return r;
    }
    void complete(Object response) {request.setResult(response);Shadows.shadowOf(Looper.getMainLooper()).idle();}
    AlertDialog dialog() {return (AlertDialog)ShadowDialog.getLatestDialog();}
    @Test public void firstWinIsNotFirstCompletedLossAndGlobalDoesNothing() {
        assertFalse(FirstWinTournamentPrompt.playerWon(2,1,0));
        assertTrue(FirstWinTournamentPrompt.playerWon(2,0,0));
        assertTrue(FirstWinTournamentPrompt.playerWon(3,1,1));
        assertFalse(FirstWinTournamentPrompt.playerWon(3,1,0));
        assertFalse(FirstWinTournamentPrompt.playerWon(3,-1,-1));
        create();prefs.edit().putBoolean("first_game_completed",true).commit(); // a loss after experiment initialization
        prompt.completedWin();prompt.completedWin();
        if(!AnalyticsManager.isBangladesh()) {
            assertEquals(0,loads.get());assertFalse(prefs.contains("first_win_prompt_win_recorded"));
            verifyNoInteractions(analytics);return;
        }
        assertEquals(1,loads.get());complete(response("bkash","rocket"));
        assertTrue(dialog().isShowing());assertEquals(1,replacements.get());
        verify(analytics).trackFirstWinTournamentPrompt(any(),eq("shown"));
    }
    @Test public void joinUsesTheExistingSelectionAndSurvivesLoginBindingAndMenuConsumption() {
        bd();create();prompt.completedWin();complete(response("bkash","rocket"));
        assertEquals(activity.getString(R.string.first_win_tournament_join),dialog().getButton(-1).getText());
        dialog().getButton(-1).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1,joins.get());assertEquals(0,declines.get());
        long now=System.currentTimeMillis();
        assertTrue(PendingTournamentPromotion.isLoginEntry(activity,PendingTournamentPromotion.loginIntent(activity),now));
        assertTrue(PendingTournamentPromotion.isFirstWinSelection(activity,null,now));
        assertEquals("cup",PendingTournamentPromotion.forUser(activity,"new-user",now));
        PendingTournamentPromotion.clear(activity); // Menu consumes selection before RegulationActivity.
        assertEquals("first_win",PendingTournamentPromotion.joinSource(activity,"cup","new-user",now));
        assertNull(PendingTournamentPromotion.joinSource(activity,"other","new-user",now));
        assertNull(PendingTournamentPromotion.joinSource(activity,"cup","different-user",now));
        assertNull(PendingTournamentPromotion.joinSource(activity,"cup","new-user",now+86400001L));
        verify(analytics).trackFirstWinTournamentPrompt(any(),eq("join"));
    }
    @Test public void freshPrefetchIsReusedWithoutDelayingTheWinAndStalePrefetchIsRefreshed() {
        bd();create();prompt.prefetch();assertEquals(1,loads.get());complete(response());
        assertEquals(0,replacements.get());verifyNoInteractions(analytics);
        prompt.completedWin();Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1,loads.get());assertEquals(1,replacements.get());
        prompt.stop();prefs.edit().clear().commit();request=new TaskCompletionSource<>();create();
        prompt.prefetch();complete(response());clock.set(62000);request=new TaskCompletionSource<>();
        prompt.completedWin();assertEquals(3,loads.get());complete(response());assertEquals(2,replacements.get());
    }
    @Test public void notNowConsumesExposureAcrossRecreationAndNeverStartsJoining() {
        bd();create();prompt.completedWin();complete(response());
        dialog().getButton(-2).performClick();Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1,declines.get());assertEquals(0,joins.get());
        verify(analytics).trackFirstWinTournamentPrompt(any(),eq("not_now"));
        prompt.stop();request=new TaskCompletionSource<>();create();prompt.completedWin();
        assertEquals(1,loads.get());assertTrue(prefs.getBoolean("first_win_prompt_shown_cup",false));
        assertNull(PendingTournamentPromotion.forUser(activity,"user",System.currentTimeMillis()));
    }
    @Test public void prefetchCannotInviteSomeoneWhoJoinedBeforeTheirWin() {
        bd();uid.set("owner");create();prompt.prefetch();complete(response());
        var db=mock(com.google.firebase.firestore.FirebaseFirestore.class,RETURNS_DEEP_STUBS);
        var participant=db.collection("tournaments").document("cup").collection("participants").document("owner");
        var snapshot=mock(com.google.firebase.firestore.DocumentSnapshot.class);
        when(snapshot.exists()).thenReturn(true);
        when(participant.get(com.google.firebase.firestore.Source.SERVER))
                .thenReturn(com.google.android.gms.tasks.Tasks.forResult(snapshot));
        try(var firestore=mockStatic(com.google.firebase.firestore.FirebaseFirestore.class)) {
            firestore.when(com.google.firebase.firestore.FirebaseFirestore::getInstance).thenReturn(db);
            prompt.completedWin();Shadows.shadowOf(Looper.getMainLooper()).idle();
            assertEquals(0,replacements.get());verifyNoInteractions(analytics);
        }
    }
    @Test public void eligibleWinShowsOnlyTournamentDialogWithoutShowingStandardResultFirst() {
        bd();create();AtomicInteger standard=new AtomicInteger();
        prompt.completedWin(com.google.android.gms.tasks.Tasks.forResult(true),standard::incrementAndGet);
        assertEquals(0,standard.get());assertEquals(0,replacements.get());
        complete(response());
        assertEquals(0,standard.get());assertEquals(1,replacements.get());assertTrue(dialog().isShowing());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(2));
        assertEquals(0,standard.get());
    }
    @Test public void timeoutShowsStandardOnceAndLatePromotionCannotCoverIt() {
        bd();create();AtomicInteger standard=new AtomicInteger();
        prompt.completedWin(com.google.android.gms.tasks.Tasks.forResult(true),standard::incrementAndGet);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1501));
        assertEquals(1,standard.get());complete(response());
        assertEquals(1,standard.get());assertEquals(0,replacements.get());verifyNoInteractions(analytics);
        assertFalse(prefs.getBoolean("first_win_prompt_shown_cup",false));
    }
    @Test public void noOfferOrFailedConfirmationShowsOnlyStandardResult() {
        bd();create();AtomicInteger standard=new AtomicInteger();
        prompt.completedWin(com.google.android.gms.tasks.Tasks.forResult(false),standard::incrementAndGet);
        assertEquals(1,standard.get());assertEquals(0,loads.get());verifyNoInteractions(analytics);
        prompt.stop();prefs.edit().clear().commit();create();
        prompt.completedWin(com.google.android.gms.tasks.Tasks.forResult(true),standard::incrementAndGet);
        complete(Map.of("serverTimeMillis",1000000L,"tournaments",List.of()));
        assertEquals(2,standard.get());assertEquals(0,replacements.get());verifyNoInteractions(analytics);
    }
    @Test public void lateOnlineWinConfirmationNeverReplacesStandardButConsumesTheRealFirstWin() {
        bd();create();AtomicInteger standard=new AtomicInteger();var confirmation=new TaskCompletionSource<Boolean>();
        prompt.completedWin(confirmation.getTask(),standard::incrementAndGet);
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(1501));
        assertEquals(1,standard.get());confirmation.setResult(true);Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0,loads.get());assertEquals(0,replacements.get());assertEquals(1,standard.get());
        assertTrue(prefs.getBoolean("first_win_prompt_win_recorded",false));verifyNoInteractions(analytics);
    }
    @Test public void loadFailureAndNoActiveOfferNeverShowAnInvitation() {
        bd();create();prompt.completedWin();request.setException(new IllegalStateException("offline"));
        Shadows.shadowOf(Looper.getMainLooper()).idle();verifyNoInteractions(analytics);assertEquals(0,replacements.get());
        prefs.edit().clear().commit();request=new TaskCompletionSource<>();create();prompt.completedWin();
        complete(Map.of("serverTimeMillis",1000000L,"tournaments",List.of()));
        verifyNoInteractions(analytics);assertEquals(0,replacements.get());
    }
    @Test public void expiredOfferStoppedScreenAndChangedAccountDoNotShow() {
        bd();
        for(String scenario:List.of("expired","stopped","account","hidden")) {
            prefs.edit().clear().commit();request=new TaskCompletionSource<>();uid.set(null);visible=true;clock.set(1000);
            create();prompt.completedWin();
            if(scenario.equals("expired"))clock.set(5000);
            if(scenario.equals("stopped"))prompt.stop();
            if(scenario.equals("account"))uid.set("someone-else");
            if(scenario.equals("hidden"))visible=false;
            complete(response());assertEquals(0,replacements.get());
        }
        verifyNoInteractions(analytics);
    }
    @Test public void alreadyShownTournamentIsExcludedEvenWithFreshWinFlagAndMultipleOffersShowOne() {
        bd();prefs.edit().putBoolean("first_win_prompt_shown_cup",true).commit();create();prompt.completedWin();complete(response());
        verifyNoInteractions(analytics);
        prefs.edit().clear().commit();request=new TaskCompletionSource<>();create();prompt.completedWin();
        @SuppressWarnings("unchecked") var first=(Map<String,Object>)((Map<?,?>)response()).get("tournament");
        var second=new HashMap<>(first);second.put("id","cup2");
        complete(Map.of("serverTimeMillis",1000000L,"tournaments",List.of(first,second)));
        assertEquals(1,replacements.get());verify(analytics,times(1)).trackFirstWinTournamentPrompt(any(),eq("shown"));
    }
    @Test public void payoutTextUsesOnlyConfiguredPublicMethodsAndBengaliResources() {
        bd();create();
        String both=prompt.message(TournamentPromotion.parse(response("bkash","rocket","nagad")));
        assertTrue(both.contains("2,300 BDT"));assertTrue(both.contains("bKash / Rocket"));assertFalse(both.contains("Nagad"));
        String one=prompt.message(TournamentPromotion.parse(response("bkash")));
        assertTrue(one.contains("bKash"));assertFalse(one.contains("Rocket"));
        String none=prompt.message(TournamentPromotion.parse(response()));assertFalse(none.contains("Prize paid via"));
        var config=new android.content.res.Configuration(activity.getResources().getConfiguration());
        config.setLocale(java.util.Locale.forLanguageTag("bn"));
        var bn=activity.createConfigurationContext(config);
        assertEquals("আপনি জিতেছেন! 🏆",bn.getString(R.string.first_win_tournament_title));
        assertEquals("এখন নয়",bn.getString(R.string.first_win_tournament_not_now));
    }
    @Test public void legacyPlayersAreNotMislabelledAsFirstWinAndMenuClicksReplaceAttribution() {
        bd();prefs.edit().putBoolean("first_game_completed",true).commit();create();prompt.completedWin();
        assertEquals(0,loads.get());verifyNoInteractions(analytics);
        long now=System.currentTimeMillis();
        PendingTournamentPromotion.select(activity,"cup","user",now,"first_win");
        assertEquals("first_win",PendingTournamentPromotion.joinSource(activity,"cup","user",now));
        PendingTournamentPromotion.select(activity,"cup","user",now);
        assertNull(PendingTournamentPromotion.joinSource(activity,"cup","user",now));
    }
}
