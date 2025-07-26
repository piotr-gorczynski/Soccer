package piotr_gorczynski.soccer2;

import android.content.Context;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

/**
 * Utility class to provide a FirebaseApp instance with a custom auth domain.
 */
public class CustomFirebaseApp {
    private static final String CUSTOM_APP_NAME = "custom";
    private static FirebaseApp customApp;

    public static synchronized FirebaseApp getApp(Context context) {
        if (customApp != null) return customApp;

        FirebaseApp defaultApp = FirebaseApp.initializeApp(context);
        if (defaultApp == null) {
            throw new IllegalStateException("Default FirebaseApp not initialized");
        }

        FirebaseOptions original = defaultApp.getOptions();

        FirebaseOptions options = new FirebaseOptions.Builder()
                .setApiKey(original.getApiKey())
                .setApplicationId(original.getApplicationId())
                .setProjectId(original.getProjectId())
                .setDatabaseUrl(original.getDatabaseUrl())
                .setStorageBucket(original.getStorageBucket())
                .setGcmSenderId(original.getGcmSenderId())
                .setAuthDomain("piotr-gorczynski.com")
                .build();

        customApp = FirebaseApp.initializeApp(context, options, CUSTOM_APP_NAME);
        return customApp;
    }
}
