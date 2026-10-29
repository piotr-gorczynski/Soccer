package piotr_gorczynski.soccer2;

import com.google.firebase.FirebaseApiNotAvailableException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.Set;

/** Bounded metadata only: never inspect messages, causes, class names or stack traces. */
final class AnonymousAuthDiagnostic {
    private static final Set<String> AUTH_CODES = Set.of(
            "ERROR_QUOTA_EXCEEDED", "ERROR_OPERATION_NOT_ALLOWED", "ERROR_APP_NOT_AUTHORIZED",
            "ERROR_API_NOT_AVAILABLE", "ERROR_API_NOT_AVAILABLE_WITHOUT_GOOGLE_PLAY",
            "ERROR_INTERNAL_ERROR", "ERROR_NETWORK_REQUEST_FAILED", "ERROR_TOO_MANY_REQUESTS",
            "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_USER_TOKEN", "ERROR_USER_TOKEN_EXPIRED",
            "ERROR_USER_DISABLED", "ERROR_USER_NOT_FOUND", "ERROR_REQUIRES_RECENT_LOGIN",
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL", "ERROR_CREDENTIAL_ALREADY_IN_USE",
            "ERROR_INVALID_EMAIL", "ERROR_WRONG_PASSWORD", "ERROR_EMAIL_ALREADY_IN_USE",
            "ERROR_WEAK_PASSWORD", "ERROR_ADMIN_RESTRICTED_OPERATION", "ERROR_TENANT_ID_MISMATCH",
            "ERROR_UNSUPPORTED_TENANT_OPERATION", "ERROR_WEB_INTERNAL_ERROR");

    final String step;
    final String code;
    final String reason;

    private AnonymousAuthDiagnostic(String step, String code, String reason) {
        this.step = step;
        this.code = code;
        this.reason = reason;
    }

    static AnonymousAuthDiagnostic from(String step, Exception error) {
        String safeStep = switch (step == null ? "" : step) {
            case "authentication", "profile_read", "profile_write", "missing_user" -> step;
            default -> "unknown";
        };
        if (error instanceof FirebaseNetworkException) return of(safeStep, "NETWORK_ERROR", "network");
        if (error instanceof FirebaseTooManyRequestsException)
            return of(safeStep, "TOO_MANY_REQUESTS", "too_many_requests");
        if (error instanceof FirebaseApiNotAvailableException)
            return of(safeStep, "API_NOT_AVAILABLE", "api_not_available");
        if (error instanceof FirebaseAuthException) {
            String sdkCode = ((FirebaseAuthException) error).getErrorCode();
            if (sdkCode != null && AUTH_CODES.contains(sdkCode)) {
                String reason = switch (sdkCode) {
                    case "ERROR_NETWORK_REQUEST_FAILED" -> "network";
                    case "ERROR_TOO_MANY_REQUESTS" -> "too_many_requests";
                    // This code does NOT establish the anonymous account-creation IP quota.
                    case "ERROR_QUOTA_EXCEEDED" -> "quota_exceeded";
                    case "ERROR_OPERATION_NOT_ALLOWED" -> "operation_not_allowed";
                    case "ERROR_APP_NOT_AUTHORIZED" -> "app_not_authorized";
                    case "ERROR_API_NOT_AVAILABLE", "ERROR_API_NOT_AVAILABLE_WITHOUT_GOOGLE_PLAY" -> "api_not_available";
                    case "ERROR_INTERNAL_ERROR", "ERROR_WEB_INTERNAL_ERROR" -> "internal";
                    default -> "provider_error";
                };
                return of(safeStep, sdkCode, reason);
            }
        }
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code sdkCode = ((FirebaseFirestoreException) error).getCode();
            // Code is an SDK enum, not an exception-supplied string.
            String reason = switch (sdkCode) {
                case UNAVAILABLE, DEADLINE_EXCEEDED -> "network";
                case PERMISSION_DENIED -> "permission_denied";
                case UNAUTHENTICATED -> "unauthenticated";
                case RESOURCE_EXHAUSTED -> "resource_exhausted";
                case INTERNAL -> "internal";
                case UNKNOWN -> "unknown";
                default -> "provider_error";
            };
            return of(safeStep, "FIRESTORE_" + sdkCode.name(), reason);
        }
        return of(safeStep, "missing_user".equals(safeStep) && error == null ? "MISSING_USER" : "UNKNOWN", "unknown");
    }

    private static AnonymousAuthDiagnostic of(String step, String code, String reason) {
        return new AnonymousAuthDiagnostic(step, code, reason);
    }

    String breadcrumb() {
        return "AUTH_FAILURE method=anonymous stage=" + step + " step=" + step
                + " outcome=failure reason=" + reason + " code=" + code;
    }
}
