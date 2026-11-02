package piotr_gorczynski.soccer2;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.functions.FirebaseFunctions;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** Activity-owned offer list; stopped requests and expired cards cannot navigate. */
final class TournamentPromotionCard {
    interface Loader { Task<Object> load(); }
    private final LinearLayout container;
    private final Loader loader;
    private final Consumer<TournamentPromotion> click;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable expire=this::hideExpired;
    private final Runnable refresh=this::refresh;
    private final List<Entry> entries=new ArrayList<>();
    private int generation;
    private boolean active;
    private static final class Entry {
        final View view;
        final long expiresAt;
        Entry(View view,long expiresAt) { this.view=view; this.expiresAt=expiresAt; }
    }

    TournamentPromotionCard(View container, Loader loader, Consumer<TournamentPromotion> click) {
        this.container=(LinearLayout)container; this.loader=loader; this.click=click;
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
    // Failed membership reads hide only the affected offer, not unrelated tournaments.
    static Task<Object> excludeParticipant(Object response, String uid, FirebaseFirestore db) {
        List<TournamentPromotion> offers=TournamentPromotion.parseAll(response);
        if (offers.isEmpty() || uid==null) return Tasks.forResult(response);
        List<Task<Boolean>> checks=new ArrayList<>();
        for (TournamentPromotion offer:offers) {
            checks.add(db.collection("tournaments").document(offer.id).collection("participants")
                    .document(uid).get(Source.SERVER)
                    .continueWith(task -> task.isSuccessful() && !task.getResult().exists()));
        }
        return Tasks.whenAllComplete(checks).continueWith(ignored -> {
            List<Object> visible=new ArrayList<>();
            for (int i=0;i<offers.size();i++) {
                if (checks.get(i).isSuccessful() && Boolean.TRUE.equals(checks.get(i).getResult())) {
                    TournamentPromotion offer=offers.get(i);
                    visible.add(Map.of("id",offer.id,"name",offer.name,"regulationId",offer.regulationId,
                            "currency",offer.currency,"firstPrize",offer.firstPrize,
                            "registrationDeadlineMillis",offer.deadline,"minimumAge",offer.minimumAge));
                }
            }
            Map<String,Object> result=new HashMap<>();
            result.put("serverTimeMillis",offers.get(0).serverTime);
            result.put("tournaments",visible);
            return (Object)result;
        });
    }
    void start() {
        active=true;
        hide();
        refresh();
    }
    private void hide() {
        entries.clear(); container.removeAllViews(); container.setVisibility(View.GONE);
        handler.removeCallbacks(expire);
    }
    private void refresh() {
        handler.removeCallbacks(refresh);
        int request=++generation;
        loader.load().addOnCompleteListener(task -> {
            if (!active || request!=generation) return;
            handler.postDelayed(refresh,60_000);
            hide();
            List<TournamentPromotion> offers=task.isSuccessful()
                    ? TournamentPromotion.parseAll(task.getResult()) : List.of();
            for (TournamentPromotion offer:offers) addCard(offer);
            hideExpired();
        });
    }
    private void addCard(TournamentPromotion offer) {
        View card=LayoutInflater.from(container.getContext()).inflate(R.layout.item_tournament_promotion,container,false);
        long expiresAt=SystemClock.elapsedRealtime()+offer.deadline-offer.serverTime;
        Entry entry=new Entry(card,expiresAt);
        entries.add(entry);
        card.setOnClickListener(v -> {
            if (active && entries.contains(entry) && SystemClock.elapsedRealtime()<expiresAt) click.accept(offer);
        });
        var context=card.getContext();
        ((TextView)card.findViewById(R.id.tournamentPromoName)).setText(offer.name);
        ((TextView)card.findViewById(R.id.tournamentPromoPrize)).setText(context.getString(
                R.string.tournament_promo_prize,NumberFormat.getIntegerInstance(
                        context.getResources().getConfiguration().getLocales().get(0)).format(offer.firstPrize),offer.currency));
        ((TextView)card.findViewById(R.id.tournamentPromoEligibility)).setText(context.getString(
                R.string.tournament_promo_eligibility,offer.minimumAge));
        ((TextView)card.findViewById(R.id.tournamentPromoDeadline)).setText(context.getString(
                R.string.tournament_promo_deadline,android.text.format.DateFormat.getMediumDateFormat(context).format(new Date(offer.deadline))));
        container.addView(card);
    }
    private void hideExpired() {
        handler.removeCallbacks(expire);
        long now=SystemClock.elapsedRealtime(), next=Long.MAX_VALUE;
        var iterator=entries.iterator();
        while (iterator.hasNext()) {
            Entry entry=iterator.next();
            if (entry.expiresAt<=now) { container.removeView(entry.view); iterator.remove(); }
            else next=Math.min(next,entry.expiresAt-now);
        }
        container.setVisibility(entries.isEmpty()?View.GONE:View.VISIBLE);
        if (!entries.isEmpty()) handler.postDelayed(expire,next);
    }
    void stop() {
        active=false; generation++;
        handler.removeCallbacks(refresh);
        hide();
    }
}
