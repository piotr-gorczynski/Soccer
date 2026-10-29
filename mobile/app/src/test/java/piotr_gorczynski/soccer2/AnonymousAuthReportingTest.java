package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.*;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=android.app.Application.class)
public class AnonymousAuthReportingTest {
    Context context;
    FirebaseAnalytics analytics;
    FirebaseCrashlytics crashes;
    AnalyticsManager manager;
    MockedStatic<FirebaseAnalytics> sdk;
    MockedStatic<FirebaseCrashlytics> crashSdk;

    @Before public void setup() {
        context=mock(Context.class);
        analytics=mock(FirebaseAnalytics.class);
        crashes=mock(FirebaseCrashlytics.class);
        sdk=mockStatic(FirebaseAnalytics.class);
        crashSdk=mockStatic(FirebaseCrashlytics.class);
        sdk.when(()->FirebaseAnalytics.getInstance(context)).thenReturn(analytics);
        crashSdk.when(FirebaseCrashlytics::getInstance).thenReturn(crashes);
        manager=new AnalyticsManager(context);
        clearInvocations(analytics,crashes);
    }
    @After public void cleanup() { crashSdk.close();sdk.close(); }

    @Test public void oneSafeEventAndBreadcrumbForBothFlavours() {
        FirebaseTooManyRequestsException failure=new FirebaseTooManyRequestsException("private@example.com UID=secret response-text");
        failure.setStackTrace(new StackTraceElement[]{new StackTraceElement("PrivateClass", "secret", "private.java", 1)});
        manager.trackAuthFailure("anonymous","authentication",failure);
        ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
        verify(analytics).logEvent(eq("sign_up_error"),params.capture());
        verifyNoMoreInteractions(analytics);
        Bundle p=params.getValue();
            assertEquals(6,p.size());
            assertEquals("authentication",p.getString("stage"));
            assertEquals("authentication",p.getString("step"));
            assertEquals("failure",p.getString("outcome"));
            assertEquals("anonymous",p.getString("method"));
            assertEquals("TOO_MANY_REQUESTS",p.getString("code"));
            assertEquals("too_many_requests",p.getString("reason"));
            ArgumentCaptor<String> breadcrumb=ArgumentCaptor.forClass(String.class);
            verify(crashes).log(breadcrumb.capture());
            verifyNoMoreInteractions(crashes);
            String sent=p.toString()+breadcrumb.getValue();
            for(String forbidden:new String[]{"private@example.com","UID=secret","response-text","PrivateClass","private.java","FirebaseTooManyRequestsException"})
                assertFalse(forbidden,sent.contains(forbidden));
    }

    @Test public void otherProvidersKeepExistingDiagnostics() {
        manager.trackAuthFailure("google.com","authentication",new FirebaseAuthException("ERROR_APP_NOT_AUTHORIZED","private"));
        ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
        verify(analytics).logEvent(eq("sign_up_error"),params.capture());
        assertEquals(4,params.getValue().size());
        assertEquals("google",params.getValue().getString("method"));
        assertEquals("ERROR_APP_NOT_AUTHORIZED",params.getValue().getString("code"));
        assertEquals("provider_error",params.getValue().getString("reason"));
        verify(crashes).recordException(any(AuthFailureException.class));
    }

    @Test public void profileAndMissingUserStagesAreNotLabeledAsAuthentication() {
        for(String step:new String[]{"profile_read","profile_write","missing_user"}) {
            clearInvocations(analytics,crashes);
            manager.trackAuthFailure("anonymous",step,null);
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(analytics).logEvent(eq("sign_up_error"),params.capture());
            assertEquals(step,params.getValue().getString("step"));
            assertEquals(step,params.getValue().getString("stage"));
        }
    }

    @Test public void anonymousSuccessStillUsesSameAuthAndProfileOperations() {
        SoccerApp app=mock(SoccerApp.class);
        when(context.getApplicationContext()).thenReturn(app);
        when(app.getAnalyticsManager()).thenReturn(manager);
        FirebaseAuth auth=mock(FirebaseAuth.class);
        FirebaseUser user=mock(FirebaseUser.class);
        AuthResult result=mock(AuthResult.class);
        AdditionalUserInfo additional=mock(AdditionalUserInfo.class);
        FirebaseFirestore db=mock(FirebaseFirestore.class,RETURNS_DEEP_STUBS);
        when(auth.signInAnonymously()).thenReturn(Tasks.forResult(result));
        when(auth.getCurrentUser()).thenReturn(user);
        when(auth.getUid()).thenReturn("private-uid");
        when(user.getUid()).thenReturn("private-uid");
        when(result.getAdditionalUserInfo()).thenReturn(additional);
        when(additional.isNewUser()).thenReturn(true);
        when(db.collection("users").document("private-uid").get(Source.SERVER)).thenReturn(Tasks.forResult(mock(DocumentSnapshot.class)));
        when(db.collection("users").document("private-uid").set(anyMap(),any(SetOptions.class))).thenReturn(Tasks.forResult(null));
        FirebaseAuthManager.LoginCallback callback=mock(FirebaseAuthManager.LoginCallback.class);
        try(MockedStatic<FirebaseAuth> authSdk=mockStatic(FirebaseAuth.class);
            MockedStatic<FirebaseFirestore> store=mockStatic(FirebaseFirestore.class);
            MockedStatic<LanguageManager> language=mockStatic(LanguageManager.class);
            MockedStatic<AnalyticsUserIdentity> identity=mockStatic(AnalyticsUserIdentity.class);
            MockedStatic<PendingLoginSuccess> pending=mockStatic(PendingLoginSuccess.class)) {
            authSdk.when(FirebaseAuth::getInstance).thenReturn(auth);
            store.when(FirebaseFirestore::getInstance).thenReturn(db);
            language.when(()->LanguageManager.getCurrentLanguageCode(context)).thenReturn("en");
            new FirebaseAuthManager(context).loginAnonymously(callback);
            shadowOf(Looper.getMainLooper()).idle();
            verify(auth,times(1)).signInAnonymously();
            verify(db.collection("users").document("private-uid")).get(Source.SERVER);
            verify(db.collection("users").document("private-uid")).set(anyMap(),any(SetOptions.class));
            verify(app).enableFcmAutoInit();
            pending.verify(()->PendingLoginSuccess.begin(context,"private-uid","anonymous"));
            verify(callback).onLoginSuccess();
            verify(callback,never()).onLoginFailure(any());
            verify(analytics,never()).logEvent(eq("sign_up_error"),any());
            verify(analytics,never()).logEvent(eq("login_success"),any());
            if(AnalyticsManager.isBangladesh()) {
                verify(analytics).logEvent(eq("login_attempt"),any());
                verify(analytics).logEvent(eq("login"),any());
                verify(analytics).logEvent(eq("sign_up"),any());
            } else verifyNoInteractions(analytics);
        }
    }

    @Test public void sdkFailureIsReportedOnceAndStillNotifiesUiOnce() {
        SoccerApp app=mock(SoccerApp.class);
        when(context.getApplicationContext()).thenReturn(app);
        when(app.getAnalyticsManager()).thenReturn(manager);
        FirebaseAuth auth=mock(FirebaseAuth.class);
        FirebaseTooManyRequestsException failure=new FirebaseTooManyRequestsException("private server message");
        when(auth.signInAnonymously()).thenReturn(Tasks.forException(failure));
        FirebaseAuthManager.LoginCallback callback=mock(FirebaseAuthManager.LoginCallback.class);
        try(MockedStatic<FirebaseAuth> authSdk=mockStatic(FirebaseAuth.class)) {
            authSdk.when(FirebaseAuth::getInstance).thenReturn(auth);
            new FirebaseAuthManager(context).loginAnonymously(callback);
            shadowOf(Looper.getMainLooper()).idle();
            verify(auth,times(1)).signInAnonymously();
            verify(callback).onLoginFailure("private server message");
            verifyNoMoreInteractions(callback);
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(analytics,times(1)).logEvent(eq("sign_up_error"),params.capture());
            assertFalse(params.getValue().toString().contains("private server message"));
            verify(analytics,never()).logEvent(eq("login"),any());
            verify(analytics,never()).logEvent(eq("sign_up"),any());
            verify(analytics,never()).logEvent(eq("login_success"),any());
            verify(app,never()).enableFcmAutoInit();
        }
    }

    @Test public void profileReadFailureFollowsSuccessfulAuthWithoutAnotherAuthRequest() {
        SoccerApp app=mock(SoccerApp.class);
        when(context.getApplicationContext()).thenReturn(app);
        when(app.getAnalyticsManager()).thenReturn(manager);
        FirebaseAuth auth=mock(FirebaseAuth.class);
        FirebaseUser user=mock(FirebaseUser.class);
        FirebaseFirestore db=mock(FirebaseFirestore.class,RETURNS_DEEP_STUBS);
        when(auth.signInAnonymously()).thenReturn(Tasks.forResult(mock(AuthResult.class)));
        when(auth.getCurrentUser()).thenReturn(user);
        when(user.getUid()).thenReturn("private-uid");
        when(db.collection("users").document("private-uid").get(Source.SERVER))
                .thenReturn(Tasks.forException(new FirebaseFirestoreException("private",FirebaseFirestoreException.Code.PERMISSION_DENIED)));
        FirebaseAuthManager.LoginCallback callback=mock(FirebaseAuthManager.LoginCallback.class);
        try(MockedStatic<FirebaseAuth> authSdk=mockStatic(FirebaseAuth.class);
            MockedStatic<FirebaseFirestore> store=mockStatic(FirebaseFirestore.class);
            MockedStatic<AnalyticsUserIdentity> identity=mockStatic(AnalyticsUserIdentity.class);
            MockedStatic<PendingLoginSuccess> pending=mockStatic(PendingLoginSuccess.class)) {
            authSdk.when(FirebaseAuth::getInstance).thenReturn(auth);
            store.when(FirebaseFirestore::getInstance).thenReturn(db);
            new FirebaseAuthManager(context).loginAnonymously(callback);
            shadowOf(Looper.getMainLooper()).idle();
            verify(auth,times(1)).signInAnonymously();
            verify(callback).onLoginFailure("Failed to verify user profile: private");
            verify(callback,never()).onLoginSuccess();
            pending.verifyNoInteractions();
            ArgumentCaptor<Bundle> params=ArgumentCaptor.forClass(Bundle.class);
            verify(analytics).logEvent(eq("sign_up_error"),params.capture());
            assertEquals("profile_read",params.getValue().getString("step"));
            assertEquals("FIRESTORE_PERMISSION_DENIED",params.getValue().getString("code"));
            assertEquals("profile_read",params.getValue().getString("stage"));
            if (AnalyticsManager.isBangladesh()) verify(analytics).logEvent(eq("login"),any());
            else verify(analytics,never()).logEvent(eq("login"),any());
        }
    }
}
