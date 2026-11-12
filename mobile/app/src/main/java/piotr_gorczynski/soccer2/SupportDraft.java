package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.UUID;

/** Local, account-scoped draft. No message text is logged or sent to Analytics. */
final class SupportDraft {
    static final class Pending {
        final String id, text;
        final boolean reopen;
        Pending(String id, String text, boolean reopen) { this.id = id; this.text = text; this.reopen = reopen; }
    }
    private final SharedPreferences preferences;
    private final String key;
    String text;
    Pending pending;
    SupportDraft(Context context, String uid, String ticketId) {
        preferences = context.getSharedPreferences("help_centre_drafts", Context.MODE_PRIVATE);
        key = ownerPrefix(uid) + ticketId + ":";
        text = preferences.getString(key + "text", "");
        String id = preferences.getString(key + "pending_id", null);
        if (id != null) pending = new Pending(id, preferences.getString(key + "pending_text", text),
                preferences.getBoolean(key + "pending_reopen", false));
    }
    void edit(String value) { if (pending == null) { text = value; save(false); } }
    Pending begin(boolean reopen) {
        if (pending == null) {
            if (text.trim().isEmpty() || text.length() > 2000) throw new IllegalArgumentException("Invalid message");
            pending = new Pending(UUID.randomUUID().toString(), text.trim(), reopen);
            // Persist the idempotency key before a request can leave the process.
            // An asynchronous write could be lost on immediate process termination.
            if (!save(true)) {
                pending = null;
                throw new IllegalStateException("Could not persist support draft");
            }
        }
        return pending;
    }
    void rejected() { pending = null; save(false); }
    void delivered() { text = ""; pending = null; save(false); }
    private boolean save(boolean durable) {
        SharedPreferences.Editor editor = preferences.edit().putString(key + "text", text);
        if (pending == null) editor.remove(key + "pending_id").remove(key + "pending_text").remove(key + "pending_reopen");
        else editor.putString(key + "pending_id", pending.id).putString(key + "pending_text", pending.text)
                .putBoolean(key + "pending_reopen", pending.reopen);
        if (durable) return editor.commit();
        editor.apply(); return true;
    }
    static void clearOwner(Context context, String uid) {
        SharedPreferences prefs = context.getSharedPreferences("help_centre_drafts", Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        for (String key : prefs.getAll().keySet()) if (key.startsWith(ownerPrefix(uid))) edit.remove(key);
        edit.apply();
    }
    private static String ownerPrefix(String uid) { return uid.length() + ":" + uid + ":"; }
}
