package piotr_gorczynski.soccer2;

import android.app.Application;
import android.view.View;
import android.widget.FrameLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 30)
public class PlayerModerationTest {
    @Test public void removedAndMissingProfilesAreNotActiveRegardlessOfFormerNickname() {
        DocumentSnapshot profile = mock(DocumentSnapshot.class);
        assertTrue(UserDisplayName.isRemoved(null));
        assertTrue(UserDisplayName.isRemoved(profile));
        when(profile.exists()).thenReturn(true);
        when(profile.getString("nickname")).thenReturn("Former player");
        when(profile.getBoolean("accountDeleted")).thenReturn(true);
        assertTrue(UserDisplayName.isRemoved(profile));
        when(profile.getBoolean("accountDeleted")).thenReturn(false);
        assertFalse(UserDisplayName.isRemoved(profile));
    }

    @Test public void recycledButtonHidesAndDropsListenerForRemovedThenRestoresForActive() {
        try (MockedStatic<FirebaseAuth> factory = mockStatic(FirebaseAuth.class)) {
            FirebaseAuth auth = mock(FirebaseAuth.class);
            FirebaseUser user = mock(FirebaseUser.class);
            factory.when(FirebaseAuth::getInstance).thenReturn(auth);
            when(auth.getCurrentUser()).thenReturn(user);
            when(user.getUid()).thenReturn("me");
            Application app = androidx.test.core.app.ApplicationProvider.getApplicationContext();
            FrameLayout row = new FrameLayout(app);
            View button = new View(app); button.setId(R.id.playerActions); row.addView(button);
            PlayerModeration.bind(row, "other", "ranking", true);
            assertEquals(View.VISIBLE, button.getVisibility()); assertTrue(button.hasOnClickListeners());
            PlayerModeration.bind(row, "removed", "ranking", false);
            assertEquals(View.GONE, button.getVisibility()); assertFalse(button.hasOnClickListeners());
            PlayerModeration.bind(row, "active", "ranking", true);
            assertEquals(View.VISIBLE, button.getVisibility()); assertTrue(button.hasOnClickListeners());
            PlayerModeration.bind(row, "me", "ranking", true);
            assertEquals(View.GONE, button.getVisibility()); assertFalse(button.hasOnClickListeners());
            when(auth.getCurrentUser()).thenReturn(null);
            PlayerModeration.bind(row, "other", "ranking", true);
            assertEquals(View.GONE, button.getVisibility()); assertFalse(button.hasOnClickListeners());
        }
    }
}
