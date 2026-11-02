package piotr_gorczynski.soccer2;

import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class, sdk=34)
public class AddFriendFilteredPaginationTest {
    private AddFriendActivity activity;
    private UserSearchAdapter adapter;
    private RecyclerView list;
    private final AtomicInteger visible = new AtomicInteger(4);

    @Before public void setup() throws Exception {
        activity = Robolectric.buildActivity(AddFriendActivity.class).get();
        var context = RuntimeEnvironment.getApplication();
        activity.loadMoreButton = new Button(context);
        activity.loadMoreButton.setVisibility(View.GONE);
        activity.searchButton = new Button(context);
        activity.onlineOnlyCheckbox = new CheckBox(context);
        activity.onlineOnlyCheckbox.setChecked(true);
        activity.resultText = new TextView(context);
        list = mock(RecyclerView.class);
        activity.resultsList = list;
        adapter = mock(UserSearchAdapter.class);
        when(adapter.getItemCount()).thenAnswer(inv -> visible.get());
        field("adapter", adapter);
        field("currentQuery", "");
        field("hasMoreResults", true);
    }

    private void field(String name, Object value) throws Exception {
        Field f = AddFriendActivity.class.getDeclaredField(name);
        f.setAccessible(true); f.set(activity, value);
    }
    private Object field(String name) throws Exception {
        Field f = AddFriendActivity.class.getDeclaredField(name);
        f.setAccessible(true); return f.get(activity);
    }
    private void call(String name) throws Exception {
        Method m = AddFriendActivity.class.getDeclaredMethod(name);
        m.setAccessible(true); m.invoke(activity);
    }
    private void countChanged(int count) throws Exception {
        visible.set(count);
        Method m = AddFriendActivity.class.getDeclaredMethod("onVisibleResultsChanged", int.class);
        m.setAccessible(true); m.invoke(activity, count);
    }
    private void nextPage() throws Exception {
        Method m = AddFriendActivity.class.getDeclaredMethod("searchPage", boolean.class, int.class);
        m.setAccessible(true); m.invoke(activity, true, 0);
    }

    @Test public void fourFilteredRowsExposeNextPageWithoutScrollOrLayout() throws Exception {
        call("revealLoadMoreIfAtEnd");
        assertEquals(View.VISIBLE, activity.loadMoreButton.getVisibility());
        verify(list, never()).getLayoutManager();
    }

    @Test public void noMatchingPresenceRowsStillExposeNextPage() throws Exception {
        countChanged(0);
        assertEquals(View.VISIBLE, activity.loadMoreButton.getVisibility());
        assertEquals("", activity.resultText.getText().toString());
    }

    @Test public void presenceUpdatesCannotStrandPaginationAndDoNotResetCursor() throws Exception {
        DocumentSnapshot cursor = mock(DocumentSnapshot.class);
        field("lastVisible", cursor);
        for (int count : new int[]{4, 0, 2, 1}) {
            countChanged(count);
            assertEquals(View.VISIBLE, activity.loadMoreButton.getVisibility());
            assertSame(cursor, field("lastVisible"));
        }
    }

    @Test public void noMorePagesHidesButtonAndAllowsFinalEmptyMessage() throws Exception {
        field("hasMoreResults", false);
        countChanged(0);
        assertEquals(View.GONE, activity.loadMoreButton.getVisibility());
        assertEquals(activity.getString(R.string.user_not_found), activity.resultText.getText().toString());
    }

    @Test public void noButtonWhileRequestIsLoading() throws Exception {
        field("isPageLoading", true);
        countChanged(0);
        assertEquals(View.GONE, activity.loadMoreButton.getVisibility());
    }

    @Test public void unfilteredLongListStillWaitsUntilEnd() throws Exception {
        activity.onlineOnlyCheckbox.setChecked(false);
        LinearLayoutManager layout = mock(LinearLayoutManager.class);
        when(list.getLayoutManager()).thenReturn(layout);
        visible.set(50);
        when(layout.findLastVisibleItemPosition()).thenReturn(3);
        call("revealLoadMoreIfAtEnd");
        assertEquals(View.GONE, activity.loadMoreButton.getVisibility());
        when(layout.findLastVisibleItemPosition()).thenReturn(49);
        call("revealLoadMoreIfAtEnd");
        assertEquals(View.VISIBLE, activity.loadMoreButton.getVisibility());
    }

    @Test public void nextRequestUsesUnfilteredCursorAndShowsLaterMatchingUser() throws Exception {
        DocumentSnapshot cursor = mock(DocumentSnapshot.class);
        field("lastVisible", cursor);
        field("fallbackMode", true);
        visible.set(0);
        activity.auth = mock(FirebaseAuth.class);
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getUid()).thenReturn("current-user");
        when(activity.auth.getCurrentUser()).thenReturn(user);
        activity.db = mock(FirebaseFirestore.class);
        CollectionReference users = mock(CollectionReference.class);
        Query query = mock(Query.class);
        when(activity.db.collection("users")).thenReturn(users);
        when(users.orderBy("nickname")).thenReturn(query);
        when(query.limit(50)).thenReturn(query);
        when(query.startAfter(cursor)).thenReturn(query);
        TaskCompletionSource<QuerySnapshot> response = new TaskCompletionSource<>();
        when(query.get()).thenReturn(response.getTask());

        call("revealLoadMoreIfAtEnd");
        assertEquals(View.VISIBLE, activity.loadMoreButton.getVisibility());
        nextPage(); nextPage(); // Same handler as Load more; repeated tap must not duplicate requests.
        verify(query, times(1)).get();
        verify(query).startAfter(cursor);
        assertFalse(activity.loadMoreButton.isEnabled());

        DocumentSnapshot later = mock(DocumentSnapshot.class);
        when(later.getId()).thenReturn("later-user");
        when(later.getBoolean("termsAccepted")).thenReturn(true);
        when(later.getString("nickname")).thenReturn("LaterPlayer");
        QuerySnapshot snapshot = mock(QuerySnapshot.class);
        when(snapshot.getDocuments()).thenReturn(Collections.singletonList(later));
        when(adapter.addResults(Collections.singletonList(later))).thenAnswer(inv -> {
            visible.set(1); return 1;
        });
        response.setResult(snapshot);
        shadowOf(Looper.getMainLooper()).idle();
        verify(adapter).addResults(Collections.singletonList(later));
        assertEquals(1, adapter.getItemCount());
        assertEquals(View.GONE, activity.loadMoreButton.getVisibility());
        assertTrue(activity.searchButton.isEnabled());
        assertEquals("", activity.resultText.getText().toString());
    }
}
