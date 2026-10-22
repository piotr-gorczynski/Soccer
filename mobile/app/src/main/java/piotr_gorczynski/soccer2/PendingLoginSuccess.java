package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

/** Carries an explicit sign-in across Terms/nickname screens, including process recreation. */
final class PendingLoginSuccess {
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("pending_login_success", Context.MODE_PRIVATE);
    }

    static synchronized void begin(Context context, String uid, String method) {
        prefs(context).edit().clear().putString("uid", uid).putString("method", method)
                .putString("attempt", UUID.randomUUID().toString()).apply();
    }

    static synchronized String attempt(Context context, String uid) {
        SharedPreferences p = prefs(context);
        return uid != null && uid.equals(p.getString("uid", null))
                ? p.getString("attempt", null) : null;
    }

    static synchronized void clear(Context context) {
        prefs(context).edit().clear().apply();
    }

    static synchronized void complete(Context context, String uid, String attempt,
                                      boolean termsAccepted, String nickname,
                                      AnalyticsManager analytics) {
        if (analytics == null || !termsAccepted || nickname == null || nickname.trim().isEmpty()
                || attempt == null || !attempt.equals(attempt(context, uid))) return;
        String method = prefs(context).getString("method", "unknown");
        clear(context);
        analytics.trackLoginSuccess(method);
    }
}
