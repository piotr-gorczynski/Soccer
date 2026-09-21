package piotr_gorczynski.soccer2;

/** Dedicated prize destination that reuses the shared payout-details controller. */
public class PrizeDetailsActivity extends TournamentResultsActivity {
    @Override
    protected boolean isPrizeDetailsOnly() {
        return true;
    }
}
