package piotr_gorczynski.soccer2;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** An explicit, bounded archive request; never fetch history on screen entry. */
final class TournamentArchivePager {
    static final int PAGE_SIZE = 20;
    interface Source { Task<List<DocumentSnapshot>> fetch(DocumentSnapshot cursor); }
    interface Listener { void changed(); void failed(); }
    final List<DocumentSnapshot> documents = new ArrayList<>();
    private final Source source;
    private final Listener listener;
    private final String flavour;
    private DocumentSnapshot cursor;
    private boolean active, loading, hasMore = true;
    private int generation;

    TournamentArchivePager(Source source, Listener listener, String flavour) {
        this.source = source; this.listener = listener; this.flavour = flavour;
    }
    void start() { active = true; generation++; listener.changed(); }
    void stop() { active = false; generation++; loading = false; }
    boolean isLoading() { return loading; }
    boolean hasMore() { return hasMore; }
    void loadMore() {
        if (!active || loading || !hasMore) return;
        loading = true; listener.changed();
        final int request = generation;
        final Task<List<DocumentSnapshot>> task;
        try { task = source.fetch(cursor); }
        catch (RuntimeException ignored) { loading = false; listener.changed(); listener.failed(); return; }
        task.addOnCompleteListener(result -> {
            if (!active || request != generation) return;
            loading = false;
            if (!result.isSuccessful() || result.getResult() == null) {
                listener.changed(); listener.failed(); return;
            }
            List<DocumentSnapshot> page = result.getResult();
            Set<String> seen = new HashSet<>();
            for (DocumentSnapshot doc : documents) seen.add(doc.getId());
            for (DocumentSnapshot doc : page) {
                if ("ended".equals(status(doc))
                        && TournamentVisibility.isVisible(doc.get("visibleInFlavours"), flavour)
                        && seen.add(doc.getId())) documents.add(doc);
            }
            // Always advance using the raw page, including hidden/active rows.
            // A filtered-empty page must not hide the button when more data exists.
            if (!page.isEmpty()) cursor = page.get(page.size() - 1);
            hasMore = page.size() == PAGE_SIZE;
            documents.sort((a, b) -> {
                int time = Long.compare(deadline(b), deadline(a));
                return time == 0 ? b.getId().compareTo(a.getId()) : time;
            });
            listener.changed();
        });
    }
    static String status(DocumentSnapshot doc) {
        String value = doc.getString("status");
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
    private static long deadline(DocumentSnapshot doc) {
        Timestamp value = doc.getTimestamp("matchesDeadline");
        return value == null ? Long.MIN_VALUE : value.toDate().getTime();
    }
}
