package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=android.app.Application.class)
public class AnalyticsFunnelTest {
    @Test public void gameplayMilestonesHaveDistinctNamesAndControlledPayloadsInBothFlavours() {
        withManager((manager,sdk)->{
            manager.trackFirstGameWin(false,2,true);
            manager.trackFirstGameCompleted(false,2,true);
            manager.trackFirstWin(2);
            ArgumentCaptor<Bundle> legacy=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("first_game_win"),legacy.capture());
            assertEquals(0L,legacy.getValue().getLong("won"));
            ArgumentCaptor<Bundle> completed=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("first_game_completed"),completed.capture());
            assertEquals(3,completed.getValue().size());
            assertEquals(0L,completed.getValue().getLong("won"));
            assertEquals(2,completed.getValue().getInt("game_type"));
            assertEquals(1L,completed.getValue().getLong("is_anonymous"));
            ArgumentCaptor<Bundle> win=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("first_win"),win.capture());
            assertEquals(1,win.getValue().size());assertEquals(2,win.getValue().getInt("game_type"));
            verifyNoMoreInteractions(sdk);
        });
    }
    @Test public void firstWinPromptReportsActionsWithoutEverReportingAJoin() {
        withManager((manager,sdk)->{
            var offer=TournamentPromotion.parse(TournamentPromotionTest.response());
            manager.trackFirstWinTournamentPrompt(offer,"shown");
            manager.trackFirstWinTournamentPrompt(offer,"join");
            manager.trackFirstWinTournamentPrompt(offer,"not_now");
            manager.trackFirstWinTournamentPrompt(offer,"private user input");
            if(!AnalyticsManager.isBangladesh()) {verifyNoInteractions(sdk);return;}
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk,times(3)).logEvent(eq("first_win_tournament_prompt"),params.capture());
            for(Bundle b:params.getAllValues()) {
                assertEquals("first_win",b.getString("source"));assertEquals("cup",b.getString("tournament_id"));
                assertEquals(5,b.size());assertEquals(2300L,b.getLong("prize_bdt"));
            }
            verify(sdk,never()).logEvent(eq("tournament_join_success"),any());
        });
    }
    @Test public void successfulJoinKeepsFirstWinAttributionAfterOnboardingOnlyInBangladesh() {
        var context=org.robolectric.RuntimeEnvironment.getApplication();
        context.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit().clear().commit();
        long now=System.currentTimeMillis();
        PendingTournamentPromotion.select(context,"cup",null,now,"first_win");
        assertEquals("cup",PendingTournamentPromotion.forUser(context,"owner",now));
        PendingTournamentPromotion.clear(context);
        try(var auth=mockStatic(com.google.firebase.auth.FirebaseAuth.class)) {
            var instance=mock(com.google.firebase.auth.FirebaseAuth.class);
            auth.when(com.google.firebase.auth.FirebaseAuth::getInstance).thenReturn(instance);
            when(instance.getUid()).thenReturn("owner");
            withManager((manager,sdk)->{
                manager.trackTournamentJoinSuccess("cup");
                ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
                verify(sdk).logEvent(eq("tournament_join_success"),params.capture());
                assertEquals(AnalyticsManager.isBangladesh()?"first_win":null,params.getValue().getString("source"));
            });
        } finally {context.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit().clear().commit();}
    }
    @Test public void feedbackClickContainsOnlyControlledValuesAndGlobalIsUnchanged() {
        withManager((manager,sdk)->{
            manager.trackPostPayoutFeedbackLinkClick("private-user-entered-value");
            if (!AnalyticsManager.isBangladesh()) { verifyNoInteractions(sdk); return; }
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("post_payout_feedback_link_click"),params.capture());
            Bundle b=params.getValue();assertEquals(3,b.size());
            assertEquals("facebook",b.getString("destination"));
            assertEquals("post_payout_message",b.getString("source"));
            assertEquals("community",b.getString("destination_type"));
            verifyNoMoreInteractions(sdk);
        });
    }
    @Test public void tournamentCardClickReportsOnlyPublicIdAndOnlyInBangladesh() {
        withManager((manager,sdk)->{
            manager.trackTournamentPromotionClick(null);
            manager.trackTournamentPromotionClick("private@example.com");
            manager.trackTournamentPromotionClick("bad/path");
            manager.trackTournamentPromotionClick("cup-123");
            if (!AnalyticsManager.isBangladesh()) { verifyNoInteractions(sdk); return; }
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("tournament_promo_click"),params.capture());
            assertEquals(1,params.getValue().size());
            assertEquals("cup-123",params.getValue().getString("tournament_id"));
            verifyNoMoreInteractions(sdk);
        });
    }
    @Test public void tournamentImpressionReportsOnlyPublicFieldsAndOnlyInBangladesh() {
        withManager((manager, sdk) -> {
            manager.trackTournamentCardImpression(TournamentPromotion.parse(TournamentPromotionTest.response()));
            if (!AnalyticsManager.isBangladesh()) {
                verifyNoInteractions(sdk);
                return;
            }
            ArgumentCaptor<Bundle> params = ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("tournament_card_impression"), params.capture());
            Bundle value = params.getValue();
            assertEquals(5, value.size());
            assertEquals("cup", value.getString("tournament_id"));
            assertEquals(2300L, value.getLong("prize_bdt"));
            assertEquals("BDT", value.getString("currency"));
            assertEquals(1L, value.getLong("free_entry"));
            assertEquals(1L, value.getLong("registration_open"));
            verifyNoMoreInteractions(sdk);
        });
    }

    @Test public void firebaseNetworkExceptionIsReportedAsNetworkFailure() {
        withManager((manager,sdk)->{
            manager.trackAuthFailure("anonymous", "authentication",
                new com.google.firebase.FirebaseNetworkException("sensitive error detail"));
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("sign_up_error"),params.capture());
            assertEquals("network",params.getValue().getString("reason"));
            verify(sdk,never()).logEvent(eq("login_success"),any(Bundle.class));
        });
    }
    @Test public void termsDiagnosticsOnlyForwardControlledFieldsInBangladesh() {
        withManager((manager,sdk)->{
            Bundle input=new Bundle();
            input.putString("stage","terms_save");input.putString("outcome","failure");
            input.putString("reason","private nickname");input.putString("code","private exception");
            input.putString("entry_source","private uid");input.putString("action","private email");
            input.putString("message","secret");input.putLong("duration_ms",-1);
            manager.trackTermsStep(input);
            if(!AnalyticsManager.isBangladesh()){verifyNoInteractions(sdk);return;}
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("onboarding_step"),params.capture());
            Bundle value=params.getValue();assertEquals(7,value.size());
            assertEquals("unknown",value.getString("reason"));assertEquals("UNKNOWN",value.getString("code"));
            assertEquals("onboarding",value.getString("entry_source"));assertEquals(0,value.getLong("duration_ms"));
            assertFalse(value.containsKey("message"));assertFalse(value.toString().contains("private"));
            verifyNoMoreInteractions(sdk);
        });
    }
    private void withManager(java.util.function.BiConsumer<AnalyticsManager,FirebaseAnalytics> action) {
        Context context=mock(Context.class);
        when(context.getApplicationContext()).thenReturn(org.robolectric.RuntimeEnvironment.getApplication());
        FirebaseAnalytics sdk=mock(FirebaseAnalytics.class);
        try (MockedStatic<FirebaseAnalytics> analytics=mockStatic(FirebaseAnalytics.class);
             MockedStatic<FirebaseCrashlytics> crashes=mockStatic(FirebaseCrashlytics.class)) {
            analytics.when(()->FirebaseAnalytics.getInstance(context)).thenReturn(sdk);
            crashes.when(FirebaseCrashlytics::getInstance).thenReturn(mock(FirebaseCrashlytics.class));
            AnalyticsManager manager=new AnalyticsManager(context);
            clearInvocations(sdk);
            action.accept(manager,sdk);
        }
    }

    @Test public void bdScreenOpenIsNotSuccessfulLoginAndReturningAuthIsNotSignup() {
        withManager((manager,sdk)->{
            manager.trackLoginScreenOpened();
            if (!AnalyticsManager.isBangladesh()) {
                verify(sdk).logEvent(eq("login"),any(Bundle.class)); return;
            }
            ArgumentCaptor<Bundle> opened=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("onboarding_step"),opened.capture());
            assertEquals("authentication",opened.getValue().getString("stage"));
            assertEquals("shown",opened.getValue().getString("outcome"));
            assertEquals("account",opened.getValue().getString("source"));
            verify(sdk,never()).logEvent(eq("login"),any(Bundle.class));
            manager.trackAuthenticationSucceeded("google.com",false);
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("login"),params.capture());
            assertEquals("google",params.getValue().getString("method"));
            verify(sdk,never()).logEvent(eq("sign_up"),any(Bundle.class));
            verify(sdk,never()).logEvent(eq("login_success"),any(Bundle.class));
        });
    }

    @Test public void loginOpenSourceIsControlledAndGlobalKeepsLegacyEvent() {
        withManager((manager,sdk)->{
            manager.trackLoginScreenOpened("tournament");
            manager.trackLoginScreenOpened("private@example.com");
            ArgumentCaptor<Bundle> values=ArgumentCaptor.forClass(Bundle.class);
            if(!AnalyticsManager.isBangladesh()) {
                verify(sdk,times(2)).logEvent(eq("login"),values.capture());
                for(Bundle value:values.getAllValues())assertTrue(value.isEmpty());
            } else {
                verify(sdk,times(2)).logEvent(eq("onboarding_step"),values.capture());
                assertEquals("tournament",values.getAllValues().get(0).getString("source"));
                assertEquals("account",values.getAllValues().get(1).getString("source"));
                for(Bundle value:values.getAllValues()) {
                    assertEquals(4,value.size());assertEquals("authentication",value.getString("stage"));
                    assertEquals("shown",value.getString("outcome"));assertEquals("none",value.getString("reason"));
                    assertFalse(value.toString().contains("private"));
                }
            }
            verifyNoMoreInteractions(sdk);
        });
    }

    @Test public void signupRequiresNewAccountEvidenceAndLegacyCallbackDoesNotDuplicateIt() {
        withManager((manager,sdk)->{
            manager.trackAuthenticationSucceeded("anonymous",true);
            manager.trackSignupSuccess("anonymous");
            verify(sdk,times(1)).logEvent(eq("sign_up"),any(Bundle.class));
            verify(sdk,times(AnalyticsManager.isBangladesh()?1:0)).logEvent(eq("login"),any(Bundle.class));
        });
    }

    @Test public void noNicknameOrRawErrorIsSentByLegacyHelpers() {
        withManager((manager,sdk)->{
            manager.trackNicknameCheckError("private@example.com", "phone=123 wallet=secret");
            manager.trackTournamentJoinError("private-user", "private@example.com", "wallet=secret");
            manager.trackSignupError("private@example.com","secret","wallet=secret","private-user");
            ArgumentCaptor<String> names=ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk,times(3)).logEvent(names.capture(),params.capture());
            for (Bundle bundle:params.getAllValues()) for(String key:bundle.keySet()) {
                String value=String.valueOf(bundle.get(key));
                assertFalse(value.contains("private"));assertFalse(value.contains("secret"));
                assertFalse(key.equals("nickname"));assertFalse(key.contains("message"));
            }
        });
    }

    @Test public void bdOutcomesHaveOnlyControlledScalarParameters() {
        withManager((manager,sdk)->{
            manager.trackOnboardingStep("nickname","success","none");
            manager.trackOnboardingStep("private@example.com","my name","wallet=secret");
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk,times(AnalyticsManager.isBangladesh()?2:0)).logEvent(eq("onboarding_step"),params.capture());
            if (!AnalyticsManager.isBangladesh()) return;
            assertEquals("success",params.getAllValues().get(0).getString("outcome"));
            for(String key:params.getAllValues().get(1).keySet())
                assertEquals("unknown",params.getAllValues().get(1).getString(key));
            for(Bundle bundle:params.getAllValues()) for(String key:bundle.keySet()) {
                assertTrue(key.length()<=40); assertTrue(bundle.get(key) instanceof String);
                assertTrue(bundle.getString(key).length()<=100);
            }
        });
    }

    @Test public void joinedStartUsesGaSupportedNumericFlag() {
        withManager((manager,sdk)->{
            manager.trackTournamentJoinStart("public-tournament",true);
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("tournament_join_start"),params.capture());
            assertEquals(1L,params.getValue().getLong("is_authenticated"));
        });
    }
}
