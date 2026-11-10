package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=RegulationRetryUiTest.TestApp.class,sdk=30)
public class RegulationRetryUiTest {
    public static class TestApp extends SoccerApp {
        final AnalyticsManager analytics=mock(AnalyticsManager.class);
        @Override public void onCreate() {}
        @Override public AnalyticsManager getAnalyticsManager() { return analytics; }
    }

    private final FirebaseAuth auth=mock(FirebaseAuth.class);
    private final FirebaseFirestore firestore=mock(FirebaseFirestore.class);
    private final DocumentReference reference=mock(DocumentReference.class);
    private final DocumentReference localizedReference=mock(DocumentReference.class);
    private final DocumentSnapshot document=mock(DocumentSnapshot.class);
    private final DocumentSnapshot localized=mock(DocumentSnapshot.class);

    private void prepare() {
        when(auth.getCurrentUser()).thenReturn(mock(FirebaseUser.class));
        CollectionReference regulations=mock(CollectionReference.class), translations=mock(CollectionReference.class);
        when(firestore.collection("regulations")).thenReturn(regulations);
        when(regulations.document("rules-id")).thenReturn(reference);
        when(reference.collection(anyString())).thenReturn(translations);
        when(translations.document("rules")).thenReturn(localizedReference);
        when(document.exists()).thenReturn(true);
        when(document.getReference()).thenReturn(reference);
        when(document.getString("name")).thenReturn("Test tournament rules");
        when(document.getData()).thenReturn(Map.of("market","BD","minimumAge",18,
                "prizeRules",Map.of("cashPrizesEnabled",true,"payoutMethods",List.of("bkash","rocket"))));
        when(localized.exists()).thenReturn(true);
        when(localized.get("rules")).thenReturn(List.of("Test rules content"));
        when(localizedReference.get()).thenReturn(Tasks.forResult(localized));
    }

    private void withScreen(java.util.function.Consumer<ActivityController<RegulationActivity>> check) {
        try(MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class);
            MockedStatic<FirebaseFirestore> fs=mockStatic(FirebaseFirestore.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);
            fs.when(FirebaseFirestore::getInstance).thenReturn(firestore);
            Intent intent=new Intent().putExtra("regulationId","rules-id").putExtra("tournamentId","cup-test");
            ActivityController<RegulationActivity> controller=Robolectric.buildActivity(RegulationActivity.class,intent);
            controller.get().setTheme(R.style.AppTheme);
            controller.create().start().resume();ShadowLooper.idleMainLooper();
            check.accept(controller);
            controller.pause().stop().destroy();
        }
    }

    @Test public void networkFailureShowsPersistentRetryAndNeverEnablesAccept() {
        prepare();when(reference.get()).thenReturn(Tasks.forException(new FirebaseFirestoreException(
                "private detail",FirebaseFirestoreException.Code.UNAVAILABLE)));
        withScreen(controller->{
            RegulationActivity a=controller.get();
            assertEquals(View.VISIBLE,a.findViewById(R.id.regulationErrorMessage).getVisibility());
            assertEquals(a.getString(R.string.regulation_retry_message),
                    ((TextView)a.findViewById(R.id.regulationErrorMessage)).getText().toString());
            assertTrue(a.findViewById(R.id.retryRegulation).isEnabled());
            assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
            assertTrue(a.findViewById(R.id.declineRegulation).isEnabled());
            a.findViewById(R.id.acceptRegulation).performClick();
            verify(((TestApp)a.getApplication()).analytics,never()).trackTournamentJoinSubmit();
        });
    }

    @Test public void retryRecoversAndStillRequiresThreeConfirmations() {
        prepare();when(reference.get()).thenReturn(Tasks.forException(new FirebaseFirestoreException(
                "network",FirebaseFirestoreException.Code.UNAVAILABLE)),Tasks.forResult(document));
        withScreen(controller->{
            RegulationActivity a=controller.get();
            a.findViewById(R.id.retryRegulation).performClick();ShadowLooper.idleMainLooper();
            assertEquals(View.GONE,a.findViewById(R.id.regulationStatus).getVisibility());
            assertEquals(View.VISIBLE,a.findViewById(R.id.regulationScroll).getVisibility());
            assertEquals("• Test rules content",((TextView)a.findViewById(R.id.regulationBody)).getText().toString());
            assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
            a.findViewById(R.id.confirmMinimumAge).performClick();
            a.findViewById(R.id.confirmPayoutAccount).performClick();
            assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
            a.findViewById(R.id.confirmTournamentTerms).performClick();
            assertTrue(a.findViewById(R.id.acceptRegulation).isEnabled());
            verify(reference,times(2)).get();
            ArgumentCaptor<android.os.Bundle> params=ArgumentCaptor.forClass(android.os.Bundle.class);
            verify(((TestApp)a.getApplication()).analytics,times(4))
                    .trackTournamentRegulationStep(eq("cup-test"),params.capture());
            assertEquals("failure",params.getAllValues().get(0).getString("outcome"));
            assertEquals("retry",params.getAllValues().get(1).getString("stage"));
            assertEquals("success",params.getAllValues().get(2).getString("outcome"));
        });
    }

    @Test public void repeatedRetryCannotStartConcurrentReads() {
        prepare();TaskCompletionSource<DocumentSnapshot> pending=new TaskCompletionSource<>();
        when(reference.get()).thenReturn(Tasks.forException(new Exception("failed")),pending.getTask());
        withScreen(controller->{
            RegulationActivity a=controller.get();Button retry=a.findViewById(R.id.retryRegulation);
            retry.performClick();retry.performClick();retry.performClick();
            assertFalse(retry.isEnabled());
            assertEquals(View.VISIBLE,a.findViewById(R.id.regulationLoading).getVisibility());
            assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
            verify(reference,times(2)).get();
            pending.setResult(document);ShadowLooper.idleMainLooper();
            assertEquals(View.VISIBLE,a.findViewById(R.id.regulationScroll).getVisibility());
        });
    }

    @Test public void missingRuleContentCannotEnableAccept() {
        prepare();when(reference.get()).thenReturn(Tasks.forResult(document));
        when(localized.get("rules")).thenReturn(Collections.emptyList());
        withScreen(controller->{
            RegulationActivity a=controller.get();
            assertEquals(View.VISIBLE,a.findViewById(R.id.regulationErrorMessage).getVisibility());
            assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
        });
    }

    @Test public void legitimateFallbackRemainsAvailableAfterTranslationFailure() {
        prepare();when(reference.get()).thenReturn(Tasks.forResult(document));
        when(document.getString("body")).thenReturn("Fallback rules content");
        when(localizedReference.get()).thenReturn(Tasks.forException(new Exception("unavailable")));
        withScreen(controller->{
            RegulationActivity a=controller.get();
            assertEquals(View.GONE,a.findViewById(R.id.regulationStatus).getVisibility());
            assertEquals("Fallback rules content",((TextView)a.findViewById(R.id.regulationBody)).getText().toString());
            assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
        });
    }

    @Test public void invalidEligibilityConfigurationShowsErrorAndCannotJoin() {
        prepare();when(reference.get()).thenReturn(Tasks.forResult(document));
        when(document.getData()).thenReturn(Map.of("minimumAge",18,
                "prizeRules",Map.of("cashPrizesEnabled",true,"payoutMethods",List.of())));
        withScreen(controller->{
            RegulationActivity a=controller.get();
            assertEquals(a.getString(R.string.eligibility_configuration_error),
                    ((TextView)a.findViewById(R.id.regulationErrorMessage)).getText().toString());
            assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
        });
    }

    @Test public void lateResultAfterClosingCannotReplaceTheScreenOrReportLoad() {
        prepare();TaskCompletionSource<DocumentSnapshot> pending=new TaskCompletionSource<>();
        when(reference.get()).thenReturn(pending.getTask());
        withScreen(controller->{
            RegulationActivity a=controller.get();AnalyticsManager manager=((TestApp)a.getApplication()).analytics;
            a.finish();clearInvocations(manager);
            pending.setResult(document);ShadowLooper.idleMainLooper();
            verifyNoInteractions(manager);verifyNoInteractions(localizedReference);
        });
    }
}
