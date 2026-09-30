package piotr_gorczynski.soccer2;

import android.os.Bundle;

/** Dedicated prize destination that reuses the shared payout-details controller. */
public class PrizeDetailsActivity extends TournamentResultsActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null && !isFinishing()) {
            ((SoccerApp) getApplication()).getAnalyticsManager().trackPrizeDetailsView();
        }
    }

    @Override
    protected boolean isPrizeDetailsOnly() {
        return true;
    }
}
