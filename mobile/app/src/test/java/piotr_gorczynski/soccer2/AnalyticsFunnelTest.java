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
    private void withManager(java.util.function.BiConsumer<AnalyticsManager,FirebaseAnalytics> action) {
        Context context=mock(Context.class);
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
            verifyNoInteractions(sdk);
            manager.trackAuthenticationSucceeded("google.com",false);
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("login"),params.capture());
            assertEquals("google",params.getValue().getString("method"));
            verify(sdk,never()).logEvent(eq("sign_up"),any(Bundle.class));
            verify(sdk,never()).logEvent(eq("login_success"),any(Bundle.class));
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
