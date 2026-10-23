package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.Shadows;
import android.os.Looper;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = android.app.Application.class)
public class AuthFailureReportingTest {
    @Test public void reportsSdkCodeWithoutOriginalMessageOrCause() {
        Context context = mock(Context.class);
        FirebaseAnalytics analytics = mock(FirebaseAnalytics.class);
        FirebaseCrashlytics crashes = mock(FirebaseCrashlytics.class);
        try (MockedStatic<FirebaseAnalytics> sdk = mockStatic(FirebaseAnalytics.class);
             MockedStatic<FirebaseCrashlytics> crashSdk = mockStatic(FirebaseCrashlytics.class)) {
            sdk.when(() -> FirebaseAnalytics.getInstance(context)).thenReturn(analytics);
            crashSdk.when(FirebaseCrashlytics::getInstance).thenReturn(crashes);
            AnalyticsManager manager = new AnalyticsManager(context);
            FirebaseAuthException original = new FirebaseAuthException(
                    "ERROR_INVALID_CREDENTIAL", "private@example.com token=secret");
            manager.trackAuthFailure("facebook", "credential_exchange", original);
            ArgumentCaptor<Throwable> report = ArgumentCaptor.forClass(Throwable.class);
            verify(crashes).recordException(report.capture());
            Throwable safe = report.getValue();
            assertNull(safe.getCause());
            assertFalse(safe.toString().contains("private@example.com"));
            assertFalse(safe.toString().contains("secret"));
            assertTrue(safe.getMessage().contains("ERROR_INVALID_CREDENTIAL"));
            assertTrue(safe.getMessage().contains(BuildConfig.APPLICATION_ID));
            assertArrayEquals(original.getStackTrace(), safe.getStackTrace());
            ArgumentCaptor<Bundle> params = ArgumentCaptor.forClass(Bundle.class);
            verify(analytics).logEvent(eq("sign_up_error"), params.capture());
            assertEquals("facebook", params.getValue().getString("method"));
            assertEquals("credential_exchange", params.getValue().getString("step"));
            assertFalse(params.getValue().containsKey("message"));
            clearInvocations(crashes, analytics);
            manager.trackAuthFailure("google.com", "authentication",
                    new FirebaseAuthException("ERROR_WEB_CONTEXT_CANCELED", "cancelled"));
            verify(crashes, never()).recordException(any(Throwable.class));
            verify(analytics).logEvent(eq("auth_cancelled"), any(Bundle.class));
        }
    }

    @Test public void retainsFirestoreStageCodeAndToleratesMissingServices() {
        AuthFailureException error = new AuthFailureException("email", "profile_read",
                new FirebaseFirestoreException("sensitive", FirebaseFirestoreException.Code.PERMISSION_DENIED));
        assertEquals("FIRESTORE_PERMISSION_DENIED", error.code);
        assertFalse(error.getMessage().contains("sensitive"));
        new AnalyticsManager(null).trackAuthFailure("email", "missing_user", null);
    }

    @Test public void successEventNormalizesAllProvidersAndDoesNotImplySignup() {
        Context context = mock(Context.class);
        FirebaseAnalytics analytics = mock(FirebaseAnalytics.class);
        FirebaseCrashlytics crashes = mock(FirebaseCrashlytics.class);
        try (MockedStatic<FirebaseAnalytics> sdk = mockStatic(FirebaseAnalytics.class);
             MockedStatic<FirebaseCrashlytics> crashSdk = mockStatic(FirebaseCrashlytics.class)) {
            sdk.when(() -> FirebaseAnalytics.getInstance(context)).thenReturn(analytics);
            crashSdk.when(FirebaseCrashlytics::getInstance).thenReturn(crashes);
            AnalyticsManager manager = new AnalyticsManager(context);
            clearInvocations(analytics);
            String[] providers = {"google.com", "facebook.com", "microsoft.com", "password", "anonymous"};
            String[] expected = {"google", "facebook", "microsoft", "email", "anonymous"};
            for (String provider : providers) manager.trackLoginSuccess(provider);
            ArgumentCaptor<Bundle> params = ArgumentCaptor.forClass(Bundle.class);
            verify(analytics, times(5)).logEvent(eq("login_success"), params.capture());
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], params.getAllValues().get(i).getString("method"));
                assertEquals(1, params.getAllValues().get(i).size());
            }
            verifyNoMoreInteractions(analytics);
            new AnalyticsManager(null).trackLoginSuccess(null);
        }
    }

    @Test public void verifiedEmailSignInDefersEventEvenWithProfileReadFallback() {
        Context context = mock(Context.class);
        SoccerApp app = mock(SoccerApp.class);
        AnalyticsManager analytics = mock(AnalyticsManager.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class);
        FirebaseFirestore db = mock(FirebaseFirestore.class, RETURNS_DEEP_STUBS);
        FirebaseAuthManager.LoginCallback callback = mock(FirebaseAuthManager.LoginCallback.class);
        when(context.getApplicationContext()).thenReturn(app);
        when(app.getAnalyticsManager()).thenReturn(analytics);
        when(auth.signInWithEmailAndPassword("test@example.com", "test-password"))
                .thenReturn(Tasks.forResult(null));
        when(auth.getCurrentUser()).thenReturn(user);
        when(user.getUid()).thenReturn("test-uid");
        when(auth.getUid()).thenReturn("test-uid");
        when(user.isEmailVerified()).thenReturn(true);
        when(db.collection("users").document("test-uid").get(Source.SERVER))
                .thenReturn(Tasks.forException(new RuntimeException("offline")));
        try (MockedStatic<FirebaseAuth> sdk = mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> store = mockStatic(FirebaseFirestore.class);
             MockedStatic<PendingLoginSuccess> pending = mockStatic(PendingLoginSuccess.class)) {
            sdk.when(FirebaseAuth::getInstance).thenReturn(auth);
            store.when(FirebaseFirestore::getInstance).thenReturn(db);
            new FirebaseAuthManager(context).loginUser("test@example.com", "test-password", callback);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            verify(analytics, never()).trackLoginSuccess(anyString());
            pending.verify(() -> PendingLoginSuccess.begin(context, "test-uid", "email"));
            verify(callback).onLoginSuccess();
            verify(callback, never()).onLoginFailure(anyString());
        }
    }

    @Test public void emailFailurePreservesSdkErrorAndNotifiesUiOnce() {
        Context context = mock(Context.class);
        SoccerApp app = mock(SoccerApp.class);
        AnalyticsManager analytics = mock(AnalyticsManager.class);
        FirebaseAuth auth = mock(FirebaseAuth.class);
        FirebaseAuthManager.LoginCallback callback = mock(FirebaseAuthManager.LoginCallback.class);
        FirebaseAuthException failure = new FirebaseAuthException("ERROR_INVALID_CREDENTIAL", "UI message");
        when(context.getApplicationContext()).thenReturn(app);
        when(app.getAnalyticsManager()).thenReturn(analytics);
        when(auth.signInWithEmailAndPassword("test@example.com", "test-password"))
                .thenReturn(Tasks.forException(failure));
        try (MockedStatic<FirebaseAuth> sdk = mockStatic(FirebaseAuth.class)) {
            sdk.when(FirebaseAuth::getInstance).thenReturn(auth);
            new FirebaseAuthManager(context).loginUser("test@example.com", "test-password", callback);
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            verify(analytics).trackAuthFailure("email", "authentication", failure);
            verify(analytics).trackLoginAttempt("email");
            verify(callback).onLoginFailure("UI message");
            verify(callback, never()).onLoginSuccess();
            verifyNoMoreInteractions(analytics, callback);
        }
    }
}
