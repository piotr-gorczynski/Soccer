package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
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
                    R.string.tournament_promo_eligibility,R.string.tournament_promo_deadline,
                    R.string.tournament_promo_payout}) {
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
        ((LinearLayout)card).addView(LayoutInflater.from(context).inflate(R.layout.item_tournament_promotion,(LinearLayout)card,false));
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
        controller=new TournamentPromotionCard(card,()->response.getTask(),offer->{assertEquals("cup",offer.id);clicks.incrementAndGet();},()->false,offer->fail());
        controller.start(); response.setResult(response()); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.VISIBLE,card.getVisibility());
        View item=((LinearLayout)card).getChildAt(0);
        item.performClick(); assertEquals(1,clicks.get());
        assertTrue(((TextView)root.findViewById(R.id.tournamentPromoPrize)).getText().toString().contains("BDT"));
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertEquals(View.GONE,card.getVisibility()); item.performClick(); assertEquals(1,clicks.get());
    }
    @Test public void stoppedOrFailedRequestCannotShowStaleCard() {
        if(!AnalyticsManager.isBangladesh()) return;
        View card=LayoutInflater.from(context).inflate(R.layout.activity_menu,null).findViewById(R.id.tournamentPromoCard);
        TaskCompletionSource<Object> response=new TaskCompletionSource<>();
        controller=new TournamentPromotionCard(card,()->response.getTask(),offer->fail(),()->true,offer->fail());
        controller.start(); controller.stop(); response.setResult(response()); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(View.GONE,card.getVisibility());
        controller=new TournamentPromotionCard(card,()->Tasks.forException(new IllegalStateException("network")),offer->fail(),()->true,offer->fail());
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
        shadowOf(Looper.getMainLooper()).idle(); assertTrue(TournamentPromotion.parseAll(joined.getResult()).isEmpty());
        when(snapshot.exists()).thenReturn(false);
        var notJoined=TournamentPromotionCard.excludeParticipant(offer,"owner",db);
        shadowOf(Looper.getMainLooper()).idle(); assertEquals("cup",TournamentPromotion.parseAll(notJoined.getResult()).get(0).id);
        when(participant.get(Source.SERVER)).thenReturn(Tasks.forException(new IllegalStateException("unavailable")));
        var failed=TournamentPromotionCard.excludeParticipant(offer,"owner",db);
        shadowOf(Looper.getMainLooper()).idle(); assertTrue(TournamentPromotion.parseAll(failed.getResult()).isEmpty());
    }
    @SuppressWarnings("unchecked")
    static Map<String,Object> multipleResponse() {
        Map<String,Object> first=(Map<String,Object>)response().get("tournament");
        Map<String,Object> second=new HashMap<>(first);
        second.put("id","second"); second.put("name","Second Cup");
        second.put("registrationDeadlineMillis",1010000L);
        return Map.of("serverTimeMillis",1000000L,"tournaments",java.util.List.of(first,second));
    }
    @Test public void multipleCardsNavigateIndependentlyAndExpireIndividually() {
        if(!AnalyticsManager.isBangladesh()) return;
        LinearLayout cards=(LinearLayout)LayoutInflater.from(context).inflate(R.layout.activity_menu,null).findViewById(R.id.tournamentPromoCard);
        java.util.List<String> clicked=new java.util.ArrayList<>();
        controller=new TournamentPromotionCard(cards,()->Tasks.forResult(multipleResponse()),offer->clicked.add(offer.id),()->false,offer->fail());
        controller.start(); shadowOf(Looper.getMainLooper()).idle();
        assertEquals(2,cards.getChildCount());
        cards.getChildAt(0).performClick(); cards.getChildAt(1).performClick();
        assertEquals(java.util.List.of("cup","second"),clicked);
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4));
        assertEquals(1,cards.getChildCount()); assertEquals(View.VISIBLE,cards.getVisibility());
        assertEquals("Second Cup",((TextView)cards.getChildAt(0).findViewById(R.id.tournamentPromoName)).getText().toString());
    }
    @Test public void joiningFirstTournamentDoesNotHideSecondTournament() {
        FirebaseFirestore db=mock(FirebaseFirestore.class,RETURNS_DEEP_STUBS);
        DocumentSnapshot joined=mock(DocumentSnapshot.class),notJoined=mock(DocumentSnapshot.class);
        when(joined.exists()).thenReturn(true); when(notJoined.exists()).thenReturn(false);
        when(db.collection("tournaments").document("cup").collection("participants").document("owner").get(Source.SERVER))
                .thenReturn(Tasks.forResult(joined));
        when(db.collection("tournaments").document("second").collection("participants").document("owner").get(Source.SERVER))
                .thenReturn(Tasks.forResult(notJoined));
        var result=TournamentPromotionCard.excludeParticipant(multipleResponse(),"owner",db);
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1,TournamentPromotion.parseAll(result.getResult()).size());
        assertEquals("second",TournamentPromotion.parseAll(result.getResult()).get(0).id);
    }
    @Test public void listResponsePreservesLegacyFallbackAndIgnoresMalformedOrDuplicateOffers() {
        assertEquals(1,TournamentPromotion.parseAll(response()).size());
        assertEquals(2,TournamentPromotion.parseAll(multipleResponse()).size());
        assertTrue(TournamentPromotion.parseAll(Map.of("serverTimeMillis",1000000L,"tournaments",java.util.List.of())).isEmpty());
        var first=((java.util.List<?>)multipleResponse().get("tournaments")).get(0);
        assertEquals(1,TournamentPromotion.parseAll(Map.of("serverTimeMillis",1000000L,
                "tournaments",java.util.List.of(first,Map.of("id","invalid"),first))).size());
    }

    @SuppressWarnings("unchecked")
    @Test public void payoutLineUsesOnlySupportedMethodsAndHidesWhenMissing() {
        if (!AnalyticsManager.isBangladesh()) return;
        var payload = response();
        var offer = (Map<String,Object>) payload.get("tournament");
        var cards = new LinearLayout(context);
        controller = new TournamentPromotionCard(cards, () -> Tasks.forResult(payload),
                ignored -> fail(), () -> false, ignored -> fail());
        for (var methods : java.util.List.of(java.util.List.of("bkash"),
                java.util.List.of("bkash", "nagad", "rocket"), java.util.List.of("nagad"))) {
            offer.put("payoutMethods", methods);
            controller.start();
            shadowOf(Looper.getMainLooper()).idle();
            TextView payout = cards.getChildAt(0).findViewById(R.id.tournamentPromoPayout);
            if (methods.contains("bkash")) {
                assertEquals(View.VISIBLE, payout.getVisibility());
                assertEquals("Prize paid via " + (methods.contains("rocket") ? "bKash / Rocket" : "bKash"),
                        payout.getText().toString());
            } else {
                assertEquals(View.GONE, payout.getVisibility());
            }
        }
        Configuration bn = new Configuration(context.getResources().getConfiguration());
        bn.setLocale(Locale.forLanguageTag("bn"));
        assertEquals("পুরস্কারের টাকা bKash / Rocket-এর মাধ্যমে দেওয়া হবে",
                context.createConfigurationContext(bn).getString(R.string.tournament_promo_payout, "bKash / Rocket"));
    }

    @Test public void onlyVisibleCardsCountOnceUntilMenuStops() {
        if (!AnalyticsManager.isBangladesh()) return;
        var activity = org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup();
        try {
            var cards = new LinearLayout(context);
            cards.setOrientation(LinearLayout.VERTICAL);
            activity.get().setContentView(cards);
            var visible = new java.util.concurrent.atomic.AtomicBoolean(false);
            var impressions = new java.util.ArrayList<String>();
            var result = new java.util.concurrent.atomic.AtomicReference<>(
                    Tasks.<Object>forResult(Map.of("tournaments", java.util.List.of())));
            controller = new TournamentPromotionCard(cards, result::get, ignored -> fail(),
                    visible::get, offer -> impressions.add(offer.id));
            controller.start();
            shadowOf(Looper.getMainLooper()).idle();
            drawCards(cards);
            assertTrue(impressions.isEmpty());
            result.set(Tasks.forException(new IllegalStateException("unavailable")));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(60));
            drawCards(cards);
            assertTrue(impressions.isEmpty());
            result.set(Tasks.forResult(response()));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(60));
            drawCards(cards);
            assertTrue(impressions.isEmpty());
            visible.set(true);
            drawCards(cards);
            drawCards(cards);
            assertEquals(java.util.List.of("cup"), impressions);
            result.set(Tasks.forResult(multipleResponse()));
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(60));
            drawCards(cards);
            assertEquals(java.util.List.of("cup", "second"), impressions);
            controller.stop();
            controller.start();
            shadowOf(Looper.getMainLooper()).idle();
            drawCards(cards);
            assertEquals(java.util.List.of("cup", "second", "cup", "second"), impressions);
        } finally {
            controller.stop();
            activity.pause().stop().destroy();
        }
    }

    private void drawCards(LinearLayout cards) {
        cards.measure(View.MeasureSpec.makeMeasureSpec(450, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.EXACTLY));
        cards.layout(0, 0, 450, 700);
        cards.getViewTreeObserver().dispatchOnPreDraw();
    }
}
