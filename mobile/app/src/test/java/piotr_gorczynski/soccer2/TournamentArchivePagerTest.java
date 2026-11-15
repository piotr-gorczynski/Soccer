package piotr_gorczynski.soccer2;

import android.app.Application;
import android.os.Looper;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk=30)
public class TournamentArchivePagerTest {
    static DocumentSnapshot doc(String id,String status,Object visibility,long time) {
        DocumentSnapshot doc=mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn(id); when(doc.getString("status")).thenReturn(status);
        when(doc.get("visibleInFlavours")).thenReturn(visibility);
        when(doc.getTimestamp("matchesDeadline")).thenReturn(new Timestamp(time,0));
        return doc;
    }
    private void idle() { shadowOf(Looper.getMainLooper()).idle(); }
    @Test public void openingAndResumingNeverFetchesArchive() {
        TournamentArchivePager.Source source=mock(TournamentArchivePager.Source.class);
        TournamentArchivePager pager=new TournamentArchivePager(source,mock(TournamentArchivePager.Listener.class),"bangladesh");
        pager.start(); pager.stop(); pager.start();
        verifyNoInteractions(source); assertTrue(pager.hasMore()); assertTrue(pager.documents.isEmpty());
    }
    @Test public void doubleClickMakesOneRequestAndStopRejectsDelayedResult() {
        TaskCompletionSource<List<DocumentSnapshot>> pending=new TaskCompletionSource<>();
        TournamentArchivePager.Source source=mock(TournamentArchivePager.Source.class);
        when(source.fetch(null)).thenReturn(pending.getTask());
        TournamentArchivePager pager=new TournamentArchivePager(source,mock(TournamentArchivePager.Listener.class),"global");
        pager.start(); pager.loadMore(); pager.loadMore(); verify(source,times(1)).fetch(null);
        assertTrue(pager.isLoading()); pager.stop(); pager.start();
        pending.setResult(Collections.singletonList(doc("late","ended",null,1))); idle();
        assertTrue(pager.documents.isEmpty()); assertTrue(pager.hasMore()); assertFalse(pager.isLoading());
    }
    @Test public void filteredFullPageKeepsButtonAndAdvancesPastLastRawDocument() {
        List<DocumentSnapshot> hidden=new ArrayList<>();
        for(int i=0;i<TournamentArchivePager.PAGE_SIZE;i++) hidden.add(doc("hidden"+i,"ended",Collections.singletonList("bangladesh"),30-i));
        DocumentSnapshot visible=doc("legacy","ended",null,1);
        TournamentArchivePager.Source source=mock(TournamentArchivePager.Source.class);
        when(source.fetch(null)).thenReturn(Tasks.forResult(hidden));
        when(source.fetch(hidden.get(19))).thenReturn(Tasks.forResult(Collections.singletonList(visible)));
        TournamentArchivePager pager=new TournamentArchivePager(source,mock(TournamentArchivePager.Listener.class),"global");
        pager.start(); pager.loadMore(); idle();
        assertTrue(pager.documents.isEmpty()); assertTrue(pager.hasMore());
        pager.loadMore(); idle(); assertEquals(Collections.singletonList(visible),pager.documents);
        assertFalse(pager.hasMore()); pager.loadMore(); verify(source,times(1)).fetch(hidden.get(19));
    }
    @Test public void archiveFiltersActiveAndOtherMarketsButKeepsGlobalAndLegacy() {
        DocumentSnapshot legacy=doc("legacy","ended",null,10), shared=doc("shared","ended",Arrays.asList("global"),20);
        TournamentArchivePager pager=new TournamentArchivePager(cursor -> Tasks.forResult(Arrays.asList(
                legacy, shared, doc("active","running",null,30),doc("foreign","ended",Arrays.asList("other"),40))),
                mock(TournamentArchivePager.Listener.class),"bangladesh");
        pager.start(); pager.loadMore(); idle(); assertEquals(Arrays.asList(shared,legacy),pager.documents);
        assertFalse(pager.hasMore());
    }
    @Test public void failurePreservesCursorAndAllowsRetry() {
        TournamentArchivePager.Source source=mock(TournamentArchivePager.Source.class);
        when(source.fetch(null)).thenReturn(Tasks.forException(new IllegalStateException("offline")))
                .thenReturn(Tasks.forResult(Collections.emptyList()));
        TournamentArchivePager.Listener listener=mock(TournamentArchivePager.Listener.class);
        TournamentArchivePager pager=new TournamentArchivePager(source,listener,"global");
        pager.start(); pager.loadMore(); idle(); assertTrue(pager.hasMore()); assertFalse(pager.isLoading());
        verify(listener).failed(); pager.loadMore(); idle(); verify(source,times(2)).fetch(null);
        assertFalse(pager.hasMore());
    }
    @Test public void duplicateDocumentsAreNotRepeatedAndDateTiesUseStableIdOrder() {
        DocumentSnapshot a=doc("a","ended",null,10), b=doc("b","ended",null,10);
        TournamentArchivePager pager=new TournamentArchivePager(cursor -> Tasks.forResult(Arrays.asList(a,b,a)),
                mock(TournamentArchivePager.Listener.class),"global");
        pager.start(); pager.loadMore(); idle(); assertEquals(Arrays.asList(b,a),pager.documents);
    }
}
