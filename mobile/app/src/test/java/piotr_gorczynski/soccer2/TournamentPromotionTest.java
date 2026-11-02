package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.core.widget.NestedScrollView;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Source;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class TournamentPromotionTest {
    Context context;
    TournamentPromotionCard controller;
    @Before public void setup() {
        context=new android.view.ContextThemeWrapper(RuntimeEnvironment.getApplication(),R.style.AppTheme);
        PendingTournamentPromotion.clear(context);
    }
    @After public void cleanup() { if (controller!=null) controller.stop(); }
    static Map<String,Object> response() {
        Map<String,Object> offer=new HashMap<>();
        offer.put("id","cup"); offer.put("name","Kickoff Cup"); offer.put("regulationId","rules");
        offer.put("firstPrize",2300L); offer.put("currency","BDT"); offer.put("minimumAge",18);
        offer.put("registrationDeadlineMillis",1003000L);
        return new HashMap<>(Map.of("serverTimeMillis",1000000L,"tournament",offer));
    }
    @SuppressWarnings("unchecked")
    @Test public void malformedExpiredOrNonCashOffersAreNotDisplayed() {
        assertNull(TournamentPromotion.parse(null));
        assertNull(TournamentPromotion.parse(Map.of("serverTimeMillis",1)));
        for(var replacement:Map.of("currency","USD","firstPrize",0,"minimumAge",17,
                "id","bad/path","registrationDeadlineMillis",999999).entrySet()) {
            Map<String,Object> value=response();
            ((Map<String,Object>)value.get("tournament")).put(replacement.getKey(),replacement.getValue());
            assertNull(TournamentPromotion.parse(value));
        }
    }
    @Test public void projectionUsesServerAmountInsteadOfFixedThousand() {
        TournamentPromotion offer=TournamentPromotion.parse(response());
        assertNotNull(offer); assertEquals(2300,offer.firstPrize); assertEquals("BDT",offer.currency);
    }
    @Test public void pendingSelectionSurvivesLoginAndRecreationButNotAccountSwitchOrExpiry() {
        long now=10_000;
        PendingTournamentPromotion.select(context,"cup",null,now);
        assertNull(PendingTournamentPromotion.forUser(context,null,now+1));
        Context recreated=context.createConfigurationContext(new Configuration(context.getResources().getConfiguration()));
        assertEquals("cup",PendingTournamentPromotion.forUser(recreated,"user-a",now+2));
        assertNull(PendingTournamentPromotion.forUser(context,"user-b",now+3));
        assertNull(PendingTournamentPromotion.forUser(context,"user-a",now+4));
        PendingTournamentPromotion.select(context,"cup","user-a",now);
        assertNull(PendingTournamentPromotion.forUser(context,"user-a",now+25*60*60*1000L));
    }
    @Test public void allSupportedLocalesHaveFormattedCopy() {
        for(String lang:new String[]{"en","bn","pl","de","fr","es","ar","fa","hi","ur","ne","si","am","sw","so","mg","mn","my","km","lo"}) {
            Configuration c=new Configuration(context.getResources().getConfiguration());
            c.setLocale(Locale.forLanguageTag(lang)); Context localized=context.createConfigurationContext(c);
            for(int id:new int[]{R.string.tournament_promo_join,R.string.tournament_promo_prize,
                    R.string.tournament_promo_eligibility,R.string.tournament_promo_deadline}) {
                assertFalse(localized.getString(id).isBlank());
                if(!lang.equals("en")) assertNotEquals("Missing "+lang,context.getString(id),localized.getString(id));
            }
            assertTrue(localized.getString(R.string.tournament_promo_prize,"2300","BDT").contains("2300"));
            assertFalse(localized.getString(R.string.tournament_promo_eligibility,18).contains("%"));
            assertTrue(localized.getString(R.string.tournament_promo_deadline,"TEST_DATE").contains("TEST_DATE"));
        }
    }
    @Test public void onlyBangladeshLayoutHasCardAndSmallScreenKeepsAllButtonsReachable() {
        View root=LayoutInflater.from(context).inflate(R.layout.activity_menu,null);
        View card=root.findViewById(R.id.tournamentPromoCard);
        if(!AnalyticsManager.isBangladesh()) { assertNull(card); return; }
        assertNotNull(card); card.setVisibility(View.VISIBLE);
        float titleSize=((TextView)root.findViewById(R.id.tournamentPromoName)).getTextSize();
        assertEquals(titleSize,((TextView)root.findViewById(R.id.tournamentPromoPrize)).getTextSize(),0.01f);
        assertEquals(titleSize,((TextView)root.findViewById(R.id.tournamentPromoAction)).getTextSize(),0.01f);
        ((TextView)root.findViewById(R.id.tournamentPromoName)).setText("Bangladesh Kickoff Cup");
        ((TextView)root.findViewById(R.id.tournamentPromoPrize)).setText("1st prize: 2300 BDT");
        assertTrue(root.findViewById(R.id.menu_center_container).getParent() instanceof NestedScrollView);
        root.findViewById(R.id.openMyPrizesBtn).setVisibility(View.VISIBLE);
        root.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));
        root.layout(0,0,320,480);
        NestedScrollView scroll=(NestedScrollView)root.findViewById(R.id.menu_center_container).getParent();
        assertTrue(scroll.getChildAt(0).getMeasuredHeight()>scroll.getMeasuredHeight());
        assertNotNull(root.findViewById(R.id.youVsAndroidBtn)); assertNotNull(root.findViewById(R.id.openMyPrizesBtn));
    }
    @Test public void cardClickUsesOfferAndDeadlineHidesItWithoutAnotherRequest() {
        if(!AnalyticsManager.isBangladesh()) return;
        View root=LayoutInflater.from(context).inflate(R.layout.activity_menu,null);
        View card=root.findViewById(R.id.tournamentPromoCard);
        TaskCompletionSource<Object> response=new TaskCompletionSource<>();
        AtomicInteger clicks=new AtomicInteger();
        controller=new TournamentPromotionCard(card,()->response.getTask(),offer->{assertEquals("cup",offer.id);clicks.incrementAndGet();});
        controller.start(); response.setResult(response()); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.VISIBLE,card.getVisibility()); card.performClick(); assertEquals(1,clicks.get());
        assertTrue(((TextView)root.findViewById(R.id.tournamentPromoPrize)).getText().toString().contains("BDT"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertEquals(View.GONE,card.getVisibility()); card.performClick(); assertEquals(1,clicks.get());
    }
    @Test public void stoppedOrFailedRequestCannotShowStaleCard() {
        if(!AnalyticsManager.isBangladesh()) return;
        View card=LayoutInflater.from(context).inflate(R.layout.activity_menu,null).findViewById(R.id.tournamentPromoCard);
        TaskCompletionSource<Object> response=new TaskCompletionSource<>();
        controller=new TournamentPromotionCard(card,()->response.getTask(),offer->fail());
        controller.start(); controller.stop(); response.setResult(response()); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.GONE,card.getVisibility());
        controller=new TournamentPromotionCard(card,()->com.google.android.gms.tasks.Tasks.forException(new IllegalStateException("network")),offer->fail());
        controller.start(); shadowOf(Looper.getMainLooper()).idle(); assertEquals(View.GONE,card.getVisibility());
    }
    @Test public void participantIsHiddenNonParticipantShownAndGuestDoesNotReadMembership() {
        FirebaseFirestore db=mock(FirebaseFirestore.class,RETURNS_DEEP_STUBS);
        Object offer=response();
        assertSame(offer,TournamentPromotionCard.excludeParticipant(offer,null,db).getResult());
        verifyNoInteractions(db);
        DocumentReference participant=db.collection("tournaments").document("cup")
                .collection("participants").document("owner");
        DocumentSnapshot snapshot=mock(DocumentSnapshot.class);
        when(participant.get(Source.SERVER)).thenReturn(Tasks.forResult(snapshot));
        when(snapshot.exists()).thenReturn(true);
        var joined=TournamentPromotionCard.excludeParticipant(offer,"owner",db);
        shadowOf(Looper.getMainLooper()).idle(); assertNull(joined.getResult());
        when(snapshot.exists()).thenReturn(false);
        var notJoined=TournamentPromotionCard.excludeParticipant(offer,"owner",db);
        shadowOf(Looper.getMainLooper()).idle(); assertSame(offer,notJoined.getResult());
        when(participant.get(Source.SERVER)).thenReturn(Tasks.forException(new IllegalStateException("unavailable")));
        var failed=TournamentPromotionCard.excludeParticipant(offer,"owner",db);
        shadowOf(Looper.getMainLooper()).idle(); assertNull(failed.getResult());
    }
}
