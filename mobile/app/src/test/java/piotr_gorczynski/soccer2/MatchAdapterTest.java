package piotr_gorczynski.soccer2;

import android.content.Context;
import android.widget.FrameLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.lang.reflect.Method;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = android.app.Application.class)
public class MatchAdapterTest {
    @SuppressWarnings("unchecked")
    private static void cache(MatchAdapter adapter, String name, String value) throws Exception {
        Field field = MatchAdapter.class.getDeclaredField(name);
        field.setAccessible(true);
        ((Map<String, String>) field.get(adapter)).put("technical-uid", value);
    }

    @Test public void mergedUpdatesRefreshEveryFieldRegardlessOfArrivalOrder() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        MatchAdapter adapter = new MatchAdapter(context, "self", "tournament");
        MatchAdapter.VH holder = adapter.onCreateViewHolder(new FrameLayout(context), 0);
        holder.oppUid = "technical-uid";
        holder.snap = mock(DocumentSnapshot.class);
        when(holder.snap.getString("status")).thenReturn("scheduled");
        cache(adapter, "nickCache", "Player nickname");
        cache(adapter, "presCache", "online");
        cache(adapter, "inviteStatsCache", "Updated statistics");
        String[][] orders = {{"nickname", "presence", "inviteStats"},
                {"presence", "inviteStats", "nickname"}, {"inviteStats", "nickname", "presence"}};
        for (String[] order : orders) {
            holder.opponent.setText("stale");
            holder.presence.setText("stale");
            holder.inviteStats.setText("stale");
            adapter.onBindViewHolder(holder, 0, Arrays.<Object>asList(order));
            assertEquals("Player nickname", holder.opponent.getText().toString());
            assertEquals(context.getString(R.string.presence_online), holder.presence.getText().toString());
            assertEquals("Updated statistics", holder.inviteStats.getText().toString());
        }
    }

    @Test public void missingNicknameNeverDisplaysTechnicalIdentifier() {
        Context context = RuntimeEnvironment.getApplication();
        MatchAdapter adapter = new MatchAdapter(context, "self", "tournament");
        MatchAdapter.VH holder = adapter.onCreateViewHolder(new FrameLayout(context), 0);
        holder.oppUid = "technical-uid";
        adapter.onBindViewHolder(holder, 0, Arrays.<Object>asList("nickname"));
        assertEquals(context.getString(R.string.player_label), holder.opponent.getText().toString());
    }

    @Test public void cancelledPresenceReadClearsStaleTimestampAndRefreshesRow() throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        MatchAdapter adapter=new MatchAdapter(context,"self","tournament");
        DocumentSnapshot match=mock(DocumentSnapshot.class);
        when(match.getId()).thenReturn("match");
        when(match.getString("player0")).thenReturn("self");
        when(match.getString("player1")).thenReturn("technical-uid");
        adapter.add(0,match);
        cache(adapter,"presCache","active");
        Field heartbeats=MatchAdapter.class.getDeclaredField("hbCache");heartbeats.setAccessible(true);
        @SuppressWarnings("unchecked") Map<String,Long> hb=(Map<String,Long>)heartbeats.get(adapter);
        hb.put("technical-uid",System.currentTimeMillis()-60000);
        androidx.recyclerview.widget.RecyclerView.AdapterDataObserver observer=mock(androidx.recyclerview.widget.RecyclerView.AdapterDataObserver.class);
        adapter.registerAdapterDataObserver(observer);
        Method failed=MatchAdapter.class.getDeclaredMethod("handlePresenceReadFailure",String.class);failed.setAccessible(true);
        failed.invoke(adapter,"technical-uid");
        verify(observer).onItemRangeChanged(0,1,"presence");
        assertFalse(hb.containsKey("technical-uid"));
        MatchAdapter.VH holder=adapter.onCreateViewHolder(new FrameLayout(context),0);
        holder.oppUid="technical-uid";holder.snap=match;
        adapter.onBindViewHolder(holder,0,java.util.Collections.singletonList("presence"));
        assertEquals(context.getString(R.string.presence_unavailable),holder.presence.getText().toString());
        assertEquals(android.view.View.GONE,holder.inviteBtn.getVisibility());
        cache(adapter,"presCache","online");
        adapter.onBindViewHolder(holder,0,java.util.Collections.singletonList("presence"));
        assertEquals(context.getString(R.string.presence_online),holder.presence.getText().toString());
    }

    @Test public void lastSeenRequiresUsableTimestamp() throws Exception {
        assertFalse(MatchAdapter.usableHeartbeat(null));
        assertFalse(MatchAdapter.usableHeartbeat(0L));
        assertFalse(MatchAdapter.usableHeartbeat(-1L));
        assertFalse(MatchAdapter.usableHeartbeat(System.currentTimeMillis()+86400000));
        assertTrue(MatchAdapter.usableHeartbeat(System.currentTimeMillis()-60000));
        Context context=RuntimeEnvironment.getApplication();
        MatchAdapter adapter=new MatchAdapter(context,"self","tournament");
        Method label=MatchAdapter.class.getDeclaredMethod("presenceLabel",String.class,long.class);label.setAccessible(true);
        assertEquals(context.getString(R.string.presence_unavailable),label.invoke(adapter,"active",0L));
        assertEquals(context.getString(R.string.presence_offline),label.invoke(adapter,"offline",0L));
        assertEquals(context.getString(R.string.presence_last_seen,MatchAdapter.englishRelative(1000L)),label.invoke(adapter,"active",1000L));
    }
}
