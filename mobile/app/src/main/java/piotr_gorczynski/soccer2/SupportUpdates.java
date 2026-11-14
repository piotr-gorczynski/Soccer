package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.util.HashSet;
import java.util.Set;

/** Process-local invalidation only. The backend remains the source of unread state. */
public final class SupportUpdates {
    public interface Listener { void changed(String ticketId); }
    private static final Set<Listener> listeners = new HashSet<>();
    private static Object visibleOwner;
    private static String visibleUid, visibleTicket;
    private SupportUpdates() { }
    public static synchronized void add(Listener listener) { listeners.add(listener); }
    public static synchronized void remove(Listener listener) { listeners.remove(listener); }
    public static synchronized void visible(Object owner, String uid, String ticket) {
        visibleOwner = owner; visibleUid = uid; visibleTicket = ticket;
    }
    public static synchronized void hidden(Object owner) {
        if (visibleOwner == owner) { visibleOwner = null; visibleUid = null; visibleTicket = null; }
    }
    public static synchronized boolean isVisible(String uid, String ticket) {
        return uid != null && uid.equals(visibleUid) && ticket.equals(visibleTicket);
    }
    public static boolean validTicket(String ticket) { return SupportModels.validId(ticket); }
    public static void received(String ticket) {
        new Handler(Looper.getMainLooper()).post(() -> {
            synchronized (SupportUpdates.class) {
                for (Listener listener : new HashSet<>(listeners)) listener.changed(ticket);
            }
        });
    }
    public static synchronized boolean firstDelivery(Context context, String uid, String ticket, long version) {
        if (uid == null || !validTicket(ticket) || version < 1) return false;
        android.content.SharedPreferences prefs = context.getSharedPreferences("support_push_versions", 0);
        String key = uid + ":" + ticket;
        if (prefs.getLong(key, 0) >= version) return false;
        return prefs.edit().putLong(key, version).commit();
    }
}
