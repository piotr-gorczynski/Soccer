package piotr_gorczynski.soccer2;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;

/** Diagnostic snapshot: never attach the original throwable, message or credentials. */
final class AuthFailureException extends RuntimeException {
    final String method;
    final String step;
    final String code;

    AuthFailureException(String method, String step, Exception error) {
        super("Authentication failure: method=" + method + ", step=" + step
                + ", code=" + codeOf(error) + ", app=" + BuildConfig.APPLICATION_ID
                + ", version=" + BuildConfig.VERSION_NAME + ", build=" + BuildConfig.BUILD_TYPE
                + ", environment=" + BuildConfig.FLAVOR_environment);
        this.method = method;
        this.step = step;
        this.code = codeOf(error);
        if (error != null) setStackTrace(error.getStackTrace());
    }

    static String codeOf(Exception error) {
        if (error instanceof FirebaseAuthException) {
            return ((FirebaseAuthException) error).getErrorCode();
        }
        if (error instanceof FirebaseFirestoreException) {
            return "FIRESTORE_" + ((FirebaseFirestoreException) error).getCode().name();
        }
        if (error instanceof com.facebook.FacebookServiceException) {
            com.facebook.FacebookRequestError request =
                    ((com.facebook.FacebookServiceException) error).getRequestError();
            return "FACEBOOK_" + request.getErrorCode() + "_" + request.getSubErrorCode();
        }
        if (error instanceof com.facebook.FacebookException && error.getMessage() != null
                && error.getMessage().toLowerCase(java.util.Locale.ROOT).contains("key hash")) {
            return "FACEBOOK_KEY_HASH_MISMATCH";
        }
        return error == null ? "UNKNOWN" : error.getClass().getSimpleName();
    }
}
