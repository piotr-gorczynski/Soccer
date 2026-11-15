package piotr_gorczynski.soccer2;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/** SDK types/codes only; never interpret messages or expose exception class names. */
final class TermsFailureDiagnostics {
    final String reason, code;
    private TermsFailureDiagnostics(String reason, String code) { this.reason=reason; this.code=code; }
    static TermsFailureDiagnostics from(Throwable error) {
        for (int depth=0; error!=null && depth<8; depth++, error=error.getCause()) {
            if (error instanceof FirebaseFirestoreException failure) {
                String reason=switch(failure.getCode()) {
                    case PERMISSION_DENIED -> "permission";
                    case UNAUTHENTICATED -> "unauthenticated";
                    case UNAVAILABLE -> "unavailable";
                    case DEADLINE_EXCEEDED -> "timeout";
                    case RESOURCE_EXHAUSTED -> "resource_exhausted";
                    case FAILED_PRECONDITION -> "failed_precondition";
                    case NOT_FOUND -> "not_found";
                    case ALREADY_EXISTS -> "already_exists";
                    case ABORTED -> "aborted";
                    case CANCELLED -> "cancelled";
                    case INVALID_ARGUMENT -> "invalid_argument";
                    case OUT_OF_RANGE -> "out_of_range";
                    case INTERNAL -> "internal";
                    case DATA_LOSS -> "data_loss";
                    case UNIMPLEMENTED -> "unimplemented";
                    default -> "unknown";
                };
                return new TermsFailureDiagnostics(reason,failure.getCode().name());
            }
            if (error instanceof java.net.SocketTimeoutException)
                return new TermsFailureDiagnostics("timeout","TIMEOUT");
            if (error instanceof FirebaseNetworkException || error instanceof java.net.UnknownHostException
                    || error instanceof java.net.ConnectException)
                return new TermsFailureDiagnostics("network","NETWORK_ERROR");
            if (error instanceof java.io.IOException)
                return new TermsFailureDiagnostics("io_error","IO_ERROR");
        }
        return new TermsFailureDiagnostics("unknown","UNKNOWN");
    }
    static String safeCode(String code) {
        if ("NONE".equals(code) || "TIMEOUT".equals(code) || "NETWORK_ERROR".equals(code) || "IO_ERROR".equals(code)) return code;
        try { return FirebaseFirestoreException.Code.valueOf(code==null ? "UNKNOWN" : code).name(); }
        catch (IllegalArgumentException ignored) { return "UNKNOWN"; }
    }
    static String safeReason(String reason) {
        return switch(reason==null ? "unknown" : reason) {
            case "none","network","timeout","io_error","permission","unauthenticated","unavailable",
                    "resource_exhausted","failed_precondition","not_found","already_exists","aborted",
                    "cancelled","invalid_argument","out_of_range","internal","data_loss","unimplemented" -> reason;
            default -> "unknown";
        };
    }
}
