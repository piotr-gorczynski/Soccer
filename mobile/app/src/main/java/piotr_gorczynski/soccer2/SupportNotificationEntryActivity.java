package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import com.google.firebase.auth.FirebaseAuth;

/** Help must remain accessible without passing through payout or Terms routing. */
public class SupportNotificationEntryActivity extends BaseActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        String ticket = getIntent().getStringExtra(SupportConversationActivity.TICKET_ID);
        if (FirebaseAuth.getInstance().getUid() == null || !SupportUpdates.validTicket(ticket)) {
            startActivity(new Intent(this, MenuActivity.class));
        } else {
            startActivities(new Intent[] {
                    new Intent(this, MenuActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    new Intent(this, HelpCentreActivity.class),
                    new Intent(this, SupportConversationActivity.class)
                            .putExtra(SupportConversationActivity.TICKET_ID, ticket)
            });
        }
        finish();
    }
}
