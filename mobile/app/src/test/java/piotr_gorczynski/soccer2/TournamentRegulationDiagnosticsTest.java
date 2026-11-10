package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=30, application=android.app.Application.class)
public class TournamentRegulationDiagnosticsTest {
    private final List<Bundle> events=new ArrayList<>();
    private TournamentRegulationDiagnostics create() {
        return new TournamentRegulationDiagnostics(null,events::add);
    }
    @Test public void successfulLoadDoesNotClaimCheckboxesHaveBeenSeen() {
        TournamentRegulationDiagnostics d=create();
        d.load(true,true,true,"none");
        assertEquals("success",events.get(0).getString("outcome"));
        assertEquals(0,events.get(0).getLong("confirmations_seen"));
    }
    @Test public void allThreeCheckboxesMustActuallyBeObservedAndReportOnlyOnce() {
        TournamentRegulationDiagnostics d=create(); d.load(true,true,true,"none");
        d.checkboxVisible(0);d.checkboxVisible(1);d.checkboxVisible(0);
        assertEquals(1,events.size());
        d.checkboxVisible(2);d.checkboxVisible(2);d.checkboxVisible(0);
        assertEquals(2,events.size());
        assertEquals("confirmations_visible",events.get(1).getString("stage"));
    }
    @Test public void invalidOrMissingContentCannotReportVisibleConfirmations() {
        TournamentRegulationDiagnostics d=create();d.load(false,true,true,"network");
        for(int i=0;i<3;i++)d.checkboxVisible(i);
        assertEquals(1,events.size());
        assertEquals("failure",events.get(0).getString("outcome"));
        assertEquals("network",events.get(0).getString("reason"));
    }
    @Test public void readyReportsOnlyOnceEvenWhenToggledAgain() {
        TournamentRegulationDiagnostics d=create();d.load(true,true,true,"none");
        d.controls(2,false);d.controls(3,true);d.controls(3,true);
        d.controls(1,false);d.controls(3,true);
        assertEquals(2,events.size());
        assertEquals("accept_ready",events.get(1).getString("stage"));
        assertEquals(3,events.get(1).getLong("checked_count"));
    }
    @Test public void backReportsObservedStateWithoutGuessingWhyUserLeft() {
        TournamentRegulationDiagnostics d=create();d.load(true,true,true,"none");
        for(int i=0;i<3;i++)d.checkboxVisible(i);
        d.controls(2,false);d.exit(false);d.exit(false);
        Bundle exit=events.get(events.size()-1);
        assertEquals(3,events.size());
        assertEquals("abandoned",exit.getString("outcome"));
        assertEquals(1,exit.getLong("content_loaded"));
        assertEquals(1,exit.getLong("confirmations_seen"));
        assertEquals(2,exit.getLong("checked_count"));
    }
    @Test public void explicitDeclineIsSeparateAndNotDuplicatedOnStop() {
        TournamentRegulationDiagnostics d=create();d.exit(true);d.exit(false);
        assertEquals(1,events.size());assertEquals("declined",events.get(0).getString("outcome"));
    }
    @Test public void submittingDoesNotProduceAbandonmentAfterSuccessOrFailure() {
        TournamentRegulationDiagnostics d=create();d.load(true,true,true,"none");
        d.submitted();d.exit(false);
        assertEquals(1,events.size());
    }
    @Test public void recreationPreservesDeduplicationAndVisibilityProgress() {
        TournamentRegulationDiagnostics first=create();first.load(true,true,true,"none");
        first.checkboxVisible(0);first.checkboxVisible(1);first.controls(3,true);
        TournamentRegulationDiagnostics restored=new TournamentRegulationDiagnostics(first.save(),events::add);
        restored.load(true,true,true,"none");restored.controls(3,true);restored.checkboxVisible(2);
        assertEquals(3,events.size());assertEquals("confirmations_visible",events.get(2).getString("stage"));
    }
    @Test public void lateCallbackAfterExitDoesNotReportAnotherLoad() {
        TournamentRegulationDiagnostics d=create();d.exit(false);d.load(true,true,true,"none");
        assertEquals(1,events.size());
    }
    @Test public void missingTranslationWithFallbackIsSuccessfulAndControlled() {
        TournamentRegulationDiagnostics d=create();d.load(true,true,true,"fallback");
        assertEquals("success",events.get(0).getString("outcome"));
        assertEquals("fallback",events.get(0).getString("reason"));
    }
    @Test public void errorsUseTypesAndNeverExceptionMessages() {
        assertEquals("network",RegulationActivity.regulationFailureReason(new FirebaseFirestoreException(
                "private detail",FirebaseFirestoreException.Code.UNAVAILABLE)));
        assertEquals("permission",RegulationActivity.regulationFailureReason(new FirebaseFirestoreException(
                "private detail",FirebaseFirestoreException.Code.PERMISSION_DENIED)));
        assertEquals("unknown",RegulationActivity.regulationFailureReason(new Exception("private detail")));
        TournamentRegulationDiagnostics d=create();d.load(false,false,false,"private detail");
        assertEquals("unknown",events.get(0).getString("reason"));
    }
    @Test public void analyticsContainsOnlyPublicTournamentIdAndControlledStateInBothVariants() {
        Context context=mock(Context.class);FirebaseAnalytics sdk=mock(FirebaseAnalytics.class);
        FirebaseCrashlytics crash=mock(FirebaseCrashlytics.class);
        try(MockedStatic<FirebaseAnalytics> fa=mockStatic(FirebaseAnalytics.class);
            MockedStatic<FirebaseCrashlytics> fc=mockStatic(FirebaseCrashlytics.class)) {
            fa.when(()->FirebaseAnalytics.getInstance(context)).thenReturn(sdk);
            fc.when(FirebaseCrashlytics::getInstance).thenReturn(crash);
            AnalyticsManager manager=new AnalyticsManager(context);clearInvocations(sdk,crash);
            TournamentRegulationDiagnostics d=new TournamentRegulationDiagnostics(null,
                    params->manager.trackTournamentRegulationStep("cup-123",params));
            d.load(true,true,true,"none");
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(sdk).logEvent(eq("tournament_regulation_step"),params.capture());
            assertEquals(11,params.getValue().size());
            assertEquals("cup-123",params.getValue().getString("tournament_id"));
            assertFalse(params.getValue().containsKey("uid"));
            assertFalse(params.getValue().containsKey("nickname"));
            verifyNoMoreInteractions(sdk);verifyNoInteractions(crash);
        }
    }

    @Test public void explicitRetryReportsRecoveryWithoutDuplicatingTheSameAttempt() {
        TournamentRegulationDiagnostics d=create();d.beginLoad(false);
        d.load(false,false,false,"network");d.load(false,false,false,"network");
        d.beginLoad(true);d.load(true,true,true,"none");d.load(true,true,true,"none");
        assertEquals(3,events.size());
        assertEquals("failure",events.get(0).getString("outcome"));
        assertEquals("retry",events.get(1).getString("stage"));
        assertEquals("success",events.get(2).getString("outcome"));
        assertEquals(2,events.get(2).getLong("load_attempt"));
    }
}
