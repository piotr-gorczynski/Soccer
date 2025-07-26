package piotr_gorczynski.soccer2;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Provides FirebaseAuth from the custom FirebaseApp.
 */
public final class AuthProvider {
    private AuthProvider() { }

    public static FirebaseAuth getAuth() {
        try {
            FirebaseApp app = FirebaseApp.getInstance("custom");
            return FirebaseAuth.getInstance(app);
        } catch (IllegalStateException e) {
            // Fallback if the custom app isn't initialized
            return FirebaseAuth.getInstance();
        }
    }
}
