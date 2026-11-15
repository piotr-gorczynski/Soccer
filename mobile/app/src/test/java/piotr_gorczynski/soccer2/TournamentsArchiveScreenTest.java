package piotr_gorczynski.soccer2;

import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=30)
public class TournamentsArchiveScreenTest {
    public static class Screen extends TournamentsActivity {
        @Override protected AnalyticsManager tournamentAnalytics() { return mock(AnalyticsManager.class); }
        @Override protected RemoteConfigHelper tournamentRemoteConfig() { return mock(RemoteConfigHelper.class); }
    }
    private MockedStatic<FirebaseFirestore> sdk;
    private CollectionReference collection;
    private Query active,archive;
    private ListenerRegistration registration;
    private ActivityController<Screen> controller;
    @Before public void setup() {
        FirebaseFirestore db=mock(FirebaseFirestore.class); collection=mock(CollectionReference.class);
        active=mock(Query.class); archive=mock(Query.class); registration=mock(ListenerRegistration.class);
        when(db.collection("tournaments")).thenReturn(collection);
        when(collection.whereIn("status",Arrays.asList("registering","running"))).thenReturn(active);
        when(active.addSnapshotListener(any(EventListener.class))).thenReturn(registration);
        when(collection.orderBy("matchesDeadline",Query.Direction.DESCENDING)).thenReturn(archive);
        when(archive.limit(20)).thenReturn(archive);
        sdk=mockStatic(FirebaseFirestore.class); sdk.when(FirebaseFirestore::getInstance).thenReturn(db);
        controller=Robolectric.buildActivity(Screen.class).create().start().resume();
    }
    @After public void cleanup() { controller.pause().stop().destroy(); sdk.close(); }
    private void idle() { shadowOf(Looper.getMainLooper()).idle(); }
    @Test public void initialScreenShowsOnlyArchiveButtonAndFetchesActiveStatuses() {
        verify(collection).whereIn("status",Arrays.asList("registering","running"));
        verify(collection,never()).orderBy(anyString(),any(Query.Direction.class));
        assertEquals(View.VISIBLE,controller.get().findViewById(R.id.loadMoreEnded).getVisibility());
        assertEquals(View.GONE,controller.get().findViewById(R.id.endedList).getVisibility());
    }
    @Test public void clickFetchesOneBoundedPageAndShowsResults() {
        DocumentSnapshot ended=TournamentArchivePagerTest.doc("ended","ended",null,1);
        QuerySnapshot snapshot=mock(QuerySnapshot.class); when(snapshot.getDocuments()).thenReturn(Collections.singletonList(ended));
        TaskCompletionSource<QuerySnapshot> pending=new TaskCompletionSource<>(); when(archive.get(Source.SERVER)).thenReturn(pending.getTask());
        Button button=controller.get().findViewById(R.id.loadMoreEnded); button.performClick();
        verify(archive).limit(20); verify(archive).get(Source.SERVER); assertFalse(button.isEnabled());
        pending.setResult(snapshot); idle();
        RecyclerView rows=controller.get().findViewById(R.id.endedList);
        assertEquals(1,rows.getAdapter().getItemCount()); assertEquals(View.VISIBLE,rows.getVisibility());
        assertEquals(View.GONE,button.getVisibility());
    }
    @Test public void activeUpdatesDoNotLoadHistoryAndListenerIsDetachedOnStop() {
        ArgumentCaptor<EventListener<QuerySnapshot>> callback=ArgumentCaptor.forClass(EventListener.class);
        verify(active).addSnapshotListener(callback.capture());
        java.util.List<DocumentSnapshot> documents=Arrays.asList(
                TournamentArchivePagerTest.doc("open","registering",null,1),
                TournamentArchivePagerTest.doc("playing","running",null,2));
        QuerySnapshot snapshot=mock(QuerySnapshot.class); when(snapshot.getDocuments()).thenReturn(documents);
        callback.getValue().onEvent(snapshot,null);
        assertEquals(1,((RecyclerView)controller.get().findViewById(R.id.registeringList)).getAdapter().getItemCount());
        assertEquals(1,((RecyclerView)controller.get().findViewById(R.id.runningList)).getAdapter().getItemCount());
        verifyNoInteractions(archive); controller.pause().stop(); verify(registration).remove();
        controller.start().resume(); verify(active,times(2)).addSnapshotListener(any(EventListener.class));
    }
    @Test public void failedArchiveRequestLeavesRetryButtonAvailable() {
        when(archive.get(Source.SERVER)).thenReturn(Tasks.forException(new IllegalStateException("offline")));
        Button button=controller.get().findViewById(R.id.loadMoreEnded); button.performClick(); idle();
        assertTrue(button.isEnabled()); assertEquals(View.VISIBLE,button.getVisibility());
    }
}
