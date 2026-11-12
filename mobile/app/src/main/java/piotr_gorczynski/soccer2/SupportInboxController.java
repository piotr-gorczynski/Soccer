package piotr_gorczynski.soccer2;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** A foreground-only, account-bound badge. It never marks any messages read. */
final class SupportInboxController {
    interface Listener {
        void show(boolean signedIn, long unread, boolean online);
        void accountChanged(String previousUid);
    }
    private final FirebaseAuth auth;
    private final SupportGateway gateway;
    private final BooleanSupplier online;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final FirebaseAuth.AuthStateListener authListener = ignored -> bind();
    private final Runnable poll = new Runnable() {
        @Override public void run() { refresh(false); if (started) handler.postDelayed(this, 60000); }
    };
    private String uid;
    private long unread, fetchedAt = -60000;
    private int generation;
    private boolean started, loading;
    SupportInboxController(FirebaseAuth auth, SupportGateway gateway, BooleanSupplier online, Listener listener) {
        this.auth = auth; this.gateway = gateway; this.online = online; this.listener = listener;
    }
    void start() {
        if (started) return;
        started = true; bind(); auth.addAuthStateListener(authListener);
        refresh(true); handler.postDelayed(poll, 60000);
    }
    void stop() {
        started = false; generation++; loading = false;
        handler.removeCallbacks(poll); auth.removeAuthStateListener(authListener);
    }
    private void bind() {
        if (!started) return;
        String current = auth.getUid();
        if (!Objects.equals(current, uid)) {
            String previous = uid;
            uid = current; unread = 0; loading = false; fetchedAt = -60000; generation++;
            if (previous != null) listener.accountChanged(previous);
        }
        listener.show(uid != null, unread, online.getAsBoolean());
        refresh(false);
    }
    void refresh(boolean force) {
        if (!started) return;
        if (!Objects.equals(uid, auth.getUid())) { bind(); return; }
        boolean available = online.getAsBoolean();
        listener.show(uid != null, unread, available);
        if (!available) { generation++; loading = false; fetchedAt = -60000; return; }
        if (uid == null || loading || (!force && SystemClock.elapsedRealtime() - fetchedAt < 60000)) return;
        loading = true;
        fetchedAt = SystemClock.elapsedRealtime();
        int request = ++generation;
        String owner = uid;
        gateway.inbox(null).addOnCompleteListener(task -> {
            if (!started || request != generation || !Objects.equals(owner, auth.getUid())) return;
            loading = false;
            if (!online.getAsBoolean() || !task.isSuccessful()) return;
            try {
                unread = new SupportModels.Inbox(task.getResult()).unread;
                fetchedAt = SystemClock.elapsedRealtime();
                listener.show(true, unread, true);
            } catch (IllegalArgumentException ignored) { /* Keep last known count for this account only. */ }
        });
    }
}
