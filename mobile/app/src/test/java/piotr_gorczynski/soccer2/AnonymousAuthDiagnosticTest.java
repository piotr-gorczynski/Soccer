package piotr_gorczynski.soccer2;

import com.google.firebase.FirebaseApiNotAvailableException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=34, application=android.app.Application.class)
public class AnonymousAuthDiagnosticTest {
    private void check(Exception error, String code, String reason) {
        AnonymousAuthDiagnostic d = AnonymousAuthDiagnostic.from("authentication", error);
        assertEquals(code, d.code);
        assertEquals(reason, d.reason);
        assertEquals("authentication", d.step);
    }

    @Test public void network() {
        check(new FirebaseNetworkException("secret email@example.com"), "NETWORK_ERROR", "network");
    }
    @Test public void tooManyRequestsIsNotLabeledAsAnonymousIpQuota() {
        check(new FirebaseTooManyRequestsException("IP=private"), "TOO_MANY_REQUESTS", "too_many_requests");
    }
    @Test public void knownAuthCodesArePreservedAndClassified() {
        String[][] cases = {
            {"ERROR_OPERATION_NOT_ALLOWED", "operation_not_allowed"},
            {"ERROR_APP_NOT_AUTHORIZED", "app_not_authorized"},
            {"ERROR_QUOTA_EXCEEDED", "quota_exceeded"},
            {"ERROR_API_NOT_AVAILABLE", "api_not_available"},
            {"ERROR_API_NOT_AVAILABLE_WITHOUT_GOOGLE_PLAY", "api_not_available"},
            {"ERROR_INTERNAL_ERROR", "internal"},
            {"ERROR_NETWORK_REQUEST_FAILED", "network"},
            {"ERROR_TOO_MANY_REQUESTS", "too_many_requests"}
        };
        for (String[] c : cases) check(new FirebaseAuthException(c[0], "private"), c[0], c[1]);
    }
    @Test public void unavailableApi() {
        check(new FirebaseApiNotAvailableException("provider response"), "API_NOT_AVAILABLE", "api_not_available");
    }
    @Test public void recognizedGenericAuthCode() {
        check(new FirebaseAuthException("ERROR_USER_DISABLED", "private"), "ERROR_USER_DISABLED", "provider_error");
    }
    @Test public void unknownCodeAndUnknownClassNeverLeak() {
        check(new FirebaseAuthException("ERROR_PRIVATE_UID_123", "private"), "UNKNOWN", "unknown");
        class PrivateNicknameException extends RuntimeException {
            @Override public String getMessage() { throw new AssertionError("Must not inspect messages"); }
        }
        check(new PrivateNicknameException(), "UNKNOWN", "unknown");
        check(null, "UNKNOWN", "unknown");
    }
    @Test public void profileStagesRemainDistinctAndNoFreeTextStepIsSent() {
        FirebaseFirestoreException error = new FirebaseFirestoreException("private", FirebaseFirestoreException.Code.PERMISSION_DENIED);
        for (String stage : new String[]{"profile_read", "profile_write"}) {
            AnonymousAuthDiagnostic d = AnonymousAuthDiagnostic.from(stage, error);
            assertEquals(stage, d.step);
            assertEquals("FIRESTORE_PERMISSION_DENIED", d.code);
            assertEquals("permission_denied", d.reason);
        }
        assertEquals("unknown", AnonymousAuthDiagnostic.from("user-entered-value", error).step);
    }
    @Test public void missingUserIsNotAnAuthenticationSdkError() {
        AnonymousAuthDiagnostic d = AnonymousAuthDiagnostic.from("missing_user", null);
        assertEquals("missing_user", d.step);
        assertEquals("MISSING_USER", d.code);
        assertEquals("unknown", d.reason);
    }
}
