package piotr_gorczynski.soccer2;

import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SupportConversationActivity extends SupportScreenActivity {
    static final String TICKET_ID = "supportTicketId";
    private String ticketId, next;
    private SupportModels.Ticket ticket;
    private final Map<String, SupportModels.Message> messages = new LinkedHashMap<>();
    private SupportDraft draft;
    private LinearLayout rows;
    private ScrollView scroll;
    private EditText editor;
    private Button more, send;
    private View latestSupportView;
    private boolean loading, sending, readInFlight, readScheduled, canAcknowledge, firstLoad = true;
    private long acknowledgedVersion, readAttemptVersion = -1;
    private final Runnable readVisible = () -> { readScheduled = false; markVisibleReplyRead(); };
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); if (isFinishing()) return;
        ticketId = getIntent().getStringExtra(TICKET_ID);
        if (!SupportModels.validId(ticketId)) { finish(); return; }
        draft = new SupportDraft(this, ownerUid, ticketId);
        rows = findViewById(R.id.help_rows); scroll = findViewById(R.id.help_scroll);
        more = findViewById(R.id.help_load_more); more.setText(R.string.help_load_older);
        more.setOnClickListener(view -> load(next));
        editor = findViewById(R.id.help_message); editor.setText(draft.text);
        send = findViewById(R.id.help_send); send.setOnClickListener(view -> sendMessage());
        editor.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                draft.edit(s.toString()); updateComposer();
            }
            public void afterTextChanged(Editable value) { }
        });
        scroll.getViewTreeObserver().addOnScrollChangedListener(this::scheduleRead);
        rows.getViewTreeObserver().addOnGlobalLayoutListener(this::scheduleRead);
    }
    @Override protected void refresh() { load(null); }
    private void load(String cursor) {
        if (loading || sending || rows == null || !sessionActive()) return;
        loading = true; canAcknowledge = false; updateComposer(); more.setEnabled(false);
        showStatus(R.string.menu_loading_message, true, false);
        withBackend(() -> {
            int token = requestGeneration();
            gateway.conversation(ticketId, cursor).addOnCompleteListener(task -> {
                if (!accepts(token)) return;
                loading = false; more.setEnabled(true);
                if (!isDeviceOnline()) { connectionFailed(); return; }
                if (!task.isSuccessful()) { showStatus(errorText(task.getException()), false, true); updateComposer(); return; }
                try {
                    SupportModels.ThreadPage page = new SupportModels.ThreadPage(task.getResult());
                    if (!ticketId.equals(page.ticket.id)) throw new IllegalArgumentException("Wrong conversation");
                    int oldHeight = rows.getHeight(), oldY = scroll.getScrollY();
                    boolean bottom = firstLoad || oldY + scroll.getHeight() >= scroll.getChildAt(0).getHeight() - SupportUi.dp(this, 64);
                    ticket = page.ticket;
                    acknowledgedVersion = Math.max(acknowledgedVersion, ticket.version - ticket.unread);
                    for (SupportModels.Message message : page.messages) messages.put(message.id, message);
                    if (page.initial != null && !messages.containsKey("initial")) messages.put(page.initial.id, page.initial);
                    if (messages.containsKey("initial")) messages.remove("legacy_initial");
                    if (cursor != null || firstLoad || next == null) next = page.next;
                    if (draft.pending != null) {
                        SupportModels.Message delivered = messages.get("user_" + draft.pending.id);
                        if (delivered != null && "user".equals(delivered.author) && delivered.body.equals(draft.pending.text)) {
                            draft.delivered(); editor.setText("");
                        }
                    }
                    render(); firstLoad = false; canAcknowledge = true; readAttemptVersion = -1;
                    retry.setText(R.string.help_refresh);
                    showStatus(0, false, true); updateComposer();
                    scroll.post(() -> {
                        if (!accepts(token)) return;
                        if (cursor != null) scroll.scrollTo(0, oldY + rows.getHeight() - oldHeight);
                        else if (bottom) scroll.fullScroll(View.FOCUS_DOWN);
                        else scroll.scrollTo(0, oldY);
                        scheduleRead();
                    });
                } catch (IllegalArgumentException ignored) { showStatus(R.string.help_load_failed, false, true); updateComposer(); }
            });
        });
    }
    private void render() {
        rows.removeAllViews(); latestSupportView = null;
        if (getSupportActionBar() != null) getSupportActionBar().setTitle(ticket.reference);
        SupportUi.text(this, rows, getString(SupportModels.topic(ticket.category)), true);
        SupportUi.text(this, rows, getString(SupportModels.status(ticket.status)), false);
        List<SupportModels.Message> ordered = new ArrayList<>(messages.values());
        ordered.sort((a, b) -> {
            int byTime = Long.compare(SupportModels.time(a.createdAt), SupportModels.time(b.createdAt));
            return byTime == 0 ? a.id.compareTo(b.id) : byTime;
        });
        for (SupportModels.Message message : ordered) {
            LinearLayout panel = SupportUi.panel(this, rows);
            String author = getString("support".equals(message.author) ? R.string.help_support : R.string.help_you);
            SupportUi.text(this, panel, getString(R.string.payment_support_ticket_summary,
                    author, SupportModels.date(this, message.createdAt)), true).setTextSize(14);
            SupportUi.text(this, panel, message.body, false).setTextIsSelectable(true);
            if ("support".equals(message.author) && message.body.trim().equals(ticket.latestReply.trim())
                    && message.createdAt.equals(ticket.latestReplyAt)) latestSupportView = panel;
        }
        more.setVisibility(next == null ? View.GONE : View.VISIBLE);
    }
    private void updateComposer() {
        if (editor == null || draft == null) return;
        boolean reply = ticket != null && ticket.canReply;
        findViewById(R.id.help_composer).setVisibility(reply ? View.VISIBLE : View.GONE);
        boolean pending = draft.pending != null;
        editor.setEnabled(!sending && !pending);
        findViewById(R.id.help_pending).setVisibility(pending ? View.VISIBLE : View.GONE);
        send.setText(pending ? R.string.help_retry_send : ticket != null && "resolved".equals(ticket.status)
                ? R.string.help_reopen : R.string.help_send);
        send.setEnabled(reply && !loading && !sending && isDeviceOnline()
                && (pending || !editor.getText().toString().trim().isEmpty()));
    }
    private void sendMessage() {
        if (sending || loading || ticket == null || !ticket.canReply) return;
        if (draft.pending == null && editor.getText().toString().trim().isEmpty()) return;
        if (draft.pending == null && "resolved".equals(ticket.status)) {
            new AlertDialog.Builder(this).setMessage(R.string.help_reopen_confirm)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.help_reopen, (dialog, which) -> submit(true)).show();
        } else submit(false);
    }
    private void submit(boolean reopen) {
        if (!sessionActive() || sending) return;
        final SupportDraft.Pending pending;
        try { pending = draft.begin(reopen); }
        catch (IllegalArgumentException | IllegalStateException ignored) {
            showStatus(R.string.help_load_failed, false, true); updateComposer(); return;
        }
        sending = true; updateComposer(); showStatus(R.string.menu_loading_message, true, false);
        withBackend(() -> {
            int token = requestGeneration();
            gateway.send(ticketId, pending).addOnCompleteListener(task -> {
                if (!accepts(token)) return;
                sending = false;
                if (!task.isSuccessful()) {
                    if (definitiveSendFailure(task.getException())) draft.rejected();
                    showStatus(errorText(task.getException()), false, true); updateComposer(); return;
                }
                try {
                    if (!Boolean.TRUE.equals(SupportModels.map(task.getResult()).get("ok")))
                        throw new IllegalArgumentException("Invalid send result");
                    draft.delivered(); editor.setText(""); updateComposer();
                    Toast.makeText(this, R.string.help_sent, Toast.LENGTH_SHORT).show(); refresh();
                } catch (IllegalArgumentException ignored) { showStatus(R.string.help_load_failed, false, true); updateComposer(); }
            });
        });
    }
    protected boolean isSupportWindowVisible(View view) {
        return view.getWindowVisibility() == View.VISIBLE;
    }
    private boolean latestIsVisible() {
        if (!sessionActive() || !canAcknowledge || loading || !hasWindowFocus() || !isDeviceOnline()
                || latestSupportView == null || !latestSupportView.isShown()
                || !isSupportWindowVisible(latestSupportView) || latestSupportView.getHeight() <= 0
                || ticket == null || ticket.version <= acknowledgedVersion) return false;
        Rect visible = new Rect();
        return latestSupportView.getGlobalVisibleRect(visible)
                && visible.height() >= Math.min(latestSupportView.getHeight(), SupportUi.dp(this, 48));
    }
    private void scheduleRead() {
        if (readScheduled || readInFlight || ticket == null || readAttemptVersion == ticket.version || !latestIsVisible()) return;
        readScheduled = true; ui.postDelayed(readVisible, 400);
    }
    private void markVisibleReplyRead() {
        if (!latestIsVisible() || readInFlight || readAttemptVersion == ticket.version) return;
        final long version = ticket.version;
        readAttemptVersion = version; readInFlight = true;
        int token = requestGeneration();
        gateway.markRead(ticketId, version).addOnCompleteListener(task -> {
            if (!accepts(token)) return;
            readInFlight = false;
            if (!isDeviceOnline() || !task.isSuccessful()) return;
            try {
                Map<?, ?> result = SupportModels.map(task.getResult());
                if (!Boolean.TRUE.equals(result.get("ok"))) return;
                acknowledgedVersion = Math.max(acknowledgedVersion, version);
                if (SupportModels.number(result, "unreadCount") > 0) refresh();
            } catch (IllegalArgumentException ignored) { }
        });
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused); if (focused) scheduleRead();
    }
    @Override protected void requestsInvalidated() {
        loading = false; sending = false; readInFlight = false; readScheduled = false; canAcknowledge = false;
        ui.removeCallbacks(readVisible); if (more != null) more.setEnabled(true); updateComposer();
    }
}
