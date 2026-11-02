package piotr_gorczynski.soccer2;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.functions.FirebaseFunctions;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Objects;
import java.util.function.Consumer;

/** Activity-owned rendering only; old responses cannot reopen a stopped/expired card. */
final class TournamentPromotionCard {
    interface Loader { Task<Object> load(); }
    private final View card;
    private final Loader loader;
    private final Consumer<TournamentPromotion> click;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable expire=this::hide;
    private final Runnable refresh=this::refresh;
    private TournamentPromotion current;
    private int generation;
    private boolean active;
    private long expiresAt;

    TournamentPromotionCard(View card, Loader loader, Consumer<TournamentPromotion> click) {
        this.card=card; this.loader=loader; this.click=click;
        card.setOnClickListener(v -> {
            if (active && current!=null && SystemClock.elapsedRealtime()<expiresAt) click.accept(current);
        });
    }
    static Loader firebaseLoader() {
        return () -> {
            String uid=FirebaseAuth.getInstance().getUid();
            return FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("getBangladeshTournamentPromotion").call()
                .continueWithTask(task -> excludeParticipant(task.getResult().getData(),uid,
                        FirebaseFirestore.getInstance()))
                .continueWith(task -> Objects.equals(uid,FirebaseAuth.getInstance().getUid())
                        ? task.getResult() : null);
        };
    }
    // Check the actual participation record, not cached profile/onboarding state.
    // A failed read leaves the card hidden until the next refresh.
    static Task<Object> excludeParticipant(Object response, String uid, FirebaseFirestore db) {
        TournamentPromotion offer=TournamentPromotion.parse(response);
        if (offer==null || uid==null) return Tasks.forResult(response);
        return db.collection("tournaments").document(offer.id).collection("participants")
                .document(uid).get(Source.SERVER)
                .continueWith(task -> task.isSuccessful() && !task.getResult().exists() ? response : null);
    }
    void start() {
        active=true;
        hide();
        refresh();
    }
    private void hide() {
        current=null;
        card.setVisibility(View.GONE);
    }
    private void refresh() {
        handler.removeCallbacks(refresh);
        int request=++generation;
        loader.load().addOnCompleteListener(task -> {
            if (!active || request!=generation) return;
            handler.postDelayed(refresh,60_000);
            handler.removeCallbacks(expire);
            TournamentPromotion offer=task.isSuccessful() ? TournamentPromotion.parse(task.getResult()) : null;
            if (offer==null) { hide(); return; }
            current=offer;
            expiresAt=SystemClock.elapsedRealtime()+offer.deadline-offer.serverTime;
            var context=card.getContext();
            ((TextView)card.findViewById(R.id.tournamentPromoName)).setText(offer.name);
            ((TextView)card.findViewById(R.id.tournamentPromoPrize)).setText(context.getString(
                    R.string.tournament_promo_prize,NumberFormat.getIntegerInstance(
                            context.getResources().getConfiguration().getLocales().get(0)).format(offer.firstPrize),offer.currency));
            ((TextView)card.findViewById(R.id.tournamentPromoEligibility)).setText(context.getString(
                    R.string.tournament_promo_eligibility,offer.minimumAge));
            ((TextView)card.findViewById(R.id.tournamentPromoDeadline)).setText(context.getString(
                    R.string.tournament_promo_deadline,android.text.format.DateFormat.getMediumDateFormat(context).format(new Date(offer.deadline))));
            card.setVisibility(View.VISIBLE);
            handler.postDelayed(expire,offer.deadline-offer.serverTime);
        });
    }
    void stop() {
        active=false; generation++; current=null;
        handler.removeCallbacks(expire);
        handler.removeCallbacks(refresh);
        card.setVisibility(View.GONE);
    }
}
