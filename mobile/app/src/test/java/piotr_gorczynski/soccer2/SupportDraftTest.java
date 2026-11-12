package piotr_gorczynski.soccer2;

import android.app.Application;
import androidx.test.core.app.ApplicationProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 30)
public class SupportDraftTest {
    final Application app = ApplicationProvider.getApplicationContext();
    @Before public void reset() { app.getSharedPreferences("help_centre_drafts", 0).edit().clear().commit(); }
    @Test public void retryAndRecreationKeepExactIdTextAndReopenFlag() {
        SupportDraft draft = new SupportDraft(app, "owner", "ticket");
        draft.edit("  Help me  "); SupportDraft.Pending first = draft.begin(true);
        draft.edit("Changed while sending"); assertEquals("  Help me  ", draft.text);
        SupportDraft restored = new SupportDraft(app, "owner", "ticket");
        assertEquals(first.id, restored.begin(false).id);
        assertEquals("Help me", restored.pending.text); assertTrue(restored.pending.reopen);
        assertTrue(first.id.matches("[A-Za-z0-9_-]{16,64}"));
    }
    @Test public void accountsAndTicketsCannotSeeEachOthersDrafts() {
        SupportDraft draft = new SupportDraft(app, "owner", "ticket"); draft.edit("Private"); draft.begin(false);
        assertEquals("", new SupportDraft(app, "other", "ticket").text);
        assertEquals("", new SupportDraft(app, "owner", "another").text);
        SupportDraft.clearOwner(app, "owner"); assertEquals("", new SupportDraft(app, "owner", "ticket").text);
    }
    @Test public void deliveryClearsDraftButDefiniteRejectionPreservesEditableText() {
        SupportDraft draft = new SupportDraft(app, "owner", "ticket"); draft.edit("My text");
        String previous = draft.begin(false).id;
        draft.rejected(); draft.edit("Corrected"); assertNotEquals(previous, draft.begin(false).id);
        draft.delivered(); SupportDraft restored = new SupportDraft(app, "owner", "ticket");
        assertEquals("", restored.text); assertNull(restored.pending);
    }
    @Test public void emptyWhitespaceAndOversizedMessagesCannotBeginSending() {
        SupportDraft draft = new SupportDraft(app, "owner", "ticket");
        for (String text : new String[]{"", "   ", new String(new char[2001]).replace('\0', 'x')}) {
            draft.edit(text);
            assertThrows(IllegalArgumentException.class, () -> draft.begin(false));
            assertNull(draft.pending);
        }
    }
}
