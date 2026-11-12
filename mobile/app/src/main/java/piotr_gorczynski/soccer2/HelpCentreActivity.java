package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HelpCentreActivity extends SupportScreenActivity {
    private final Map<String, SupportModels.Ticket> tickets = new LinkedHashMap<>();
    private LinearLayout rows;
    private ScrollView scroll;
    private Button more;
    private String next;
    private boolean loading, replacePage = true;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if (isFinishing()) return;
        rows = findViewById(R.id.help_rows); scroll = findViewById(R.id.help_scroll);
        more = findViewById(R.id.help_load_more);
        LinearLayout feed = findViewById(R.id.help_feed);
        feed.removeView(more); feed.addView(more); // Inbox pages append below the list.
        more.setOnClickListener(view -> load(next));
        View guest = findViewById(R.id.help_guest);
        guest.setVisibility(supportAuth().getCurrentUser() != null && supportAuth().getCurrentUser().isAnonymous()
                ? View.VISIBLE : View.GONE);
    }
    @Override protected void onStart() { replacePage = true; super.onStart(); }
    @Override protected void refresh() { load(null); }
    private void load(String cursor) {
        if (loading || rows == null || !sessionActive()) return;
        loading = true; more.setEnabled(false);
        showStatus(R.string.menu_loading_message, true, false);
        withBackend(() -> {
            int token = requestGeneration();
            gateway.inbox(cursor).addOnCompleteListener(task -> {
                if (!accepts(token)) return;
                loading = false; more.setEnabled(true);
                if (!isDeviceOnline()) { connectionFailed(); return; }
                if (!task.isSuccessful()) { showStatus(errorText(task.getException()), false, true); return; }
                try {
                    SupportModels.Inbox page = new SupportModels.Inbox(task.getResult());
                    boolean reset = cursor == null && replacePage;
                    int oldY = reset ? 0 : scroll.getScrollY();
                    if (reset) tickets.clear();
                    for (SupportModels.Ticket ticket : page.tickets) tickets.put(ticket.id, ticket);
                    if (cursor != null || reset || next == null) next = page.next;
                    replacePage = false; render();
                    scroll.post(() -> { if (accepts(token)) scroll.scrollTo(0, oldY); });
                    retry.setText(R.string.help_refresh);
                    showStatus(tickets.isEmpty() ? R.string.help_empty : 0, false, true);
                } catch (IllegalArgumentException ignored) { showStatus(R.string.help_load_failed, false, true); }
            });
        });
    }
    private void render() {
        rows.removeAllViews();
        List<SupportModels.Ticket> ordered = new ArrayList<>(tickets.values());
        ordered.sort((a, b) -> Long.compare(SupportModels.time(b.updatedAt), SupportModels.time(a.updatedAt)));
        for (SupportModels.Ticket ticket : ordered) {
            LinearLayout row = SupportUi.panel(this, rows);
            String title = getString(SupportModels.topic(ticket.category));
            String summary = getString(R.string.payment_support_ticket_summary, ticket.reference,
                    getString(SupportModels.status(ticket.status)));
            TextView subject = SupportUi.text(this, row, title, ticket.unread > 0);
            SupportUi.text(this, row, summary, false);
            String date = SupportModels.date(this, ticket.updatedAt);
            SupportUi.text(this, row, date, false).setTextSize(14);
            String unread = ticket.unread > 0 ? getString(R.string.help_unread_count, ticket.unread) : "";
            if (!unread.isEmpty()) SupportUi.text(this, row, unread, true);
            row.setMinimumHeight(SupportUi.dp(this, 48));
            row.setFocusable(true); row.setClickable(true);
            row.setContentDescription(title + "\n" + summary + "\n" + date + "\n" + unread);
            for (int i = 0; i < row.getChildCount(); i++) row.getChildAt(i).setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            ViewCompat.setTooltipText(subject, title);
            row.setOnClickListener(view -> startActivity(new Intent(this, SupportConversationActivity.class)
                    .putExtra(SupportConversationActivity.TICKET_ID, ticket.id)));
        }
        more.setVisibility(next == null ? View.GONE : View.VISIBLE);
    }
    @Override protected void requestsInvalidated() {
        loading = false;
        if (more != null) more.setEnabled(true);
    }
}
