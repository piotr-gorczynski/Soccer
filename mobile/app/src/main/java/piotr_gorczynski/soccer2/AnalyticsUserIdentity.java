package piotr_gorczynski.soccer2;

import android.content.Context;
import android.util.Log;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Pseudonymous account correlation; no account fields are event parameters. */
final class AnalyticsUserIdentity {
    private AnalyticsUserIdentity() {}

    static void sync(Context context) {
        syncCrashlytics();
        FirebaseAnalytics analytics = null;
        try {
            analytics = FirebaseAnalytics.getInstance(context);
            if (analytics == null) return;
            boolean allowed = ConsentUtils.hasUmpConsent(context)
                    && ConsentUtils.isAnalyticsAllowed(context);
            apply(analytics, FirebaseAuth.getInstance().getUid(), allowed);
        } catch (RuntimeException error) {
            if (analytics != null) analytics.setUserId(null);
            Log.w("TAG_Soccer", "Could not synchronize Analytics account identity");
        }
    }

    private static void syncCrashlytics() {
        FirebaseCrashlytics crashlytics = null;
        try {
            crashlytics = FirebaseCrashlytics.getInstance();
            if (crashlytics == null) return;
            applyCrashlytics(crashlytics, FirebaseAuth.getInstance().getUid(),
                    crashlytics.isCrashlyticsCollectionEnabled());
        } catch (RuntimeException error) {
            if (crashlytics != null) crashlytics.setUserId("");
            Log.w("TAG_Soccer", "Could not synchronize Crashlytics account identity");
        }
    }

    static void applyCrashlytics(FirebaseCrashlytics crashlytics, String uid, boolean enabled) {
        String id = enabled ? forUid(uid) : null;
        // Crashlytics uses an empty string to clear the ID, unlike Analytics' null.
        crashlytics.setUserId(id == null ? "" : id);
    }

    static void apply(FirebaseAnalytics analytics, String uid, boolean allowed) {
        analytics.setUserId(allowed ? forUid(uid) : null);
    }

    /** Recompute in trusted tooling to join user_id to an Auth/Firestore account. */
    static String forUid(String uid) {
        if (uid == null || uid.isBlank()) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    ("soccer-analytics-v1:" + BuildConfig.FLAVOR_environment + ":" + uid)
                            .getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("u1_");
            for (byte value : digest) {
                result.append(Character.forDigit((value & 0xff) >>> 4, 16));
                result.append(Character.forDigit(value & 0xf, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
