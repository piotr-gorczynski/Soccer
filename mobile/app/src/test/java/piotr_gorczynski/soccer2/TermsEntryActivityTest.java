package piotr_gorczynski.soccer2;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class TermsEntryActivityTest {
    @Test public void onlyExplicitInternalNotificationDestinationsAreAllowed() {
        for(Class<?> type : new Class<?>[]{PrizeDetailsActivity.class,TournamentResultsActivity.class,
                TournamentLobbyActivity.class,InvitationsActivity.class}) {
            assertTrue(TermsEntryActivity.isAllowedDestination(new Intent().setClassName(BuildConfig.APPLICATION_ID,type.getName())));
        }
        assertFalse(TermsEntryActivity.isAllowedDestination(null));
        assertFalse(TermsEntryActivity.isAllowedDestination(new Intent(Intent.ACTION_VIEW)));
        assertFalse(TermsEntryActivity.isAllowedDestination(new Intent().setClassName("another.app",InvitationsActivity.class.getName())));
        assertFalse(TermsEntryActivity.isAllowedDestination(new Intent().setClassName(BuildConfig.APPLICATION_ID,TermsActivity.class.getName())));
    }
}
