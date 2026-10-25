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
}
