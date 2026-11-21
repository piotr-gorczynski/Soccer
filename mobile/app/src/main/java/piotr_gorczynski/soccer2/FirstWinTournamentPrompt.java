package piotr_gorczynski.soccer2;

import android.content.SharedPreferences;
import android.os.SystemClock;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.tasks.Task;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/** One first-win opportunity, owned by the result screen. Never queues a later Menu popup. */
final class FirstWinTournamentPrompt {
    private static final String INITIALIZED="first_win_prompt_initialized";
    private static final String WIN_RECORDED="first_win_prompt_win_recorded";
    private static final String SHOWN="first_win_prompt_shown_";
    private final AppCompatActivity activity;
    private final SharedPreferences prefs;
    private final TournamentPromotionCard.Loader loader;
    private final Supplier<String> uid;
    private final BooleanSupplier resultVisible;
    private final Runnable replaceResult, notNow;
    private final Consumer<TournamentPromotion> join;
    private final AnalyticsManager analytics;
    private final LongSupplier clock;
    private boolean stopped;
    private AlertDialog dialog;
    private Task<Object> prefetched;
    private String prefetchOwner;
    private long prefetchStartedAt;
    static final long DECISION_TIMEOUT_MS=1500;
    private final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean deciding, decided;
    private Runnable fallback, timeout;

    FirstWinTournamentPrompt(AppCompatActivity activity, SharedPreferences prefs,
            TournamentPromotionCard.Loader loader, Supplier<String> uid, BooleanSupplier resultVisible,
            Runnable replaceResult, Consumer<TournamentPromotion> join, Runnable notNow,
            AnalyticsManager analytics, LongSupplier clock) {
        this.activity=activity; this.prefs=prefs; this.loader=loader; this.uid=uid;
        this.resultVisible=resultVisible; this.replaceResult=replaceResult; this.join=join;
        this.notNow=notNow; this.analytics=analytics; this.clock=clock;
        if (AnalyticsManager.isBangladesh() && !prefs.getBoolean(INITIALIZED,false)) {
            // Old installations have no reliable win history. Do not relabel a later win as first.
            prefs.edit().putBoolean(INITIALIZED,true)
                    .putBoolean(WIN_RECORDED,prefs.getBoolean("first_game_completed",false)).commit();
        }
    }

    static FirstWinTournamentPrompt create(AppCompatActivity activity, AnalyticsManager analytics,
            BooleanSupplier visible, Runnable replaceResult, Consumer<TournamentPromotion> join, Runnable notNow) {
        FirstWinTournamentPrompt prompt=new FirstWinTournamentPrompt(activity,
                activity.getSharedPreferences(LanguageManager.PREFS_FILE,0),
                TournamentPromotionCard.firebaseLoader(), () -> FirebaseAuth.getInstance().getUid(),
                visible,replaceResult,join,notNow,analytics,SystemClock::elapsedRealtime);
        prompt.prefetch();
        return prompt;
    }

    /** Warm the same public/member-filtered promotion while playing, to avoid a post-win network pause. */
    void prefetch() {
        if (!AnalyticsManager.isBangladesh() || stopped || prefs.getBoolean(WIN_RECORDED,false)) return;
        prefetchOwner=uid.get();
        prefetchStartedAt=clock.getAsLong();
        prefetched=loader.load();
    }

    static boolean playerWon(int gameType, int winner, int localPlayerIndex) {
        if (winner!=0 && winner!=1) return false;
        return switch(gameType) {
            case 1,2 -> winner==0; // Same local-player convention as the existing game metrics.
            case 3 -> (localPlayerIndex==0 || localPlayerIndex==1) && winner==localPlayerIndex;
            default -> false;
        };
    }

    void completedWin() {
        completedWin(com.google.android.gms.tasks.Tasks.forResult(true),()->{});
    }

    /** Resolve exactly one result window, without first showing the standard winner dialog. */
    void completedWin(Task<Boolean> confirmation,Runnable standardResult) {
        if (deciding || decided || stopped) return;
        if (!AnalyticsManager.isBangladesh() || prefs.getBoolean(WIN_RECORDED,false)) {
            standardResult.run(); return;
        }
        deciding=true;
        fallback=standardResult;
        timeout=this::showStandardResult;
        handler.postDelayed(timeout,DECISION_TIMEOUT_MS);
        com.google.android.gms.tasks.OnCompleteListener<Boolean> confirmed=task -> {
            boolean won=task.isSuccessful() && Boolean.TRUE.equals(task.getResult());
            if(won) prefs.edit().putBoolean(WIN_RECORDED,true).commit();
            if (!deciding || stopped) return;
            if (!won) {
                showStandardResult();return;
            }
            loadWinPromotion();
        };
        if(confirmation.isComplete()) confirmed.onComplete(confirmation);
        else confirmation.addOnCompleteListener(confirmed);
    }

    private void showStandardResult() {
        if (!deciding || stopped) return;
        deciding=false;decided=true;
        handler.removeCallbacks(timeout);
        if (!activity.isFinishing() && !activity.isDestroyed() && resultVisible.getAsBoolean()) fallback.run();
    }

    private void loadWinPromotion() {
        String owner=uid.get();
        long now=clock.getAsLong();
        boolean fresh=prefetched!=null && Objects.equals(owner,prefetchOwner)
                && now-prefetchStartedAt>=0 && now-prefetchStartedAt<60_000
                && (!prefetched.isComplete() || prefetched.isSuccessful());
        long requestedAt=fresh?prefetchStartedAt:now;
        Task<Object> promotion=fresh?prefetched:loader.load();
        if (fresh && owner!=null) {
            // A player may have joined on another device since the prefetch.
            promotion=promotion.continueWithTask(task -> TournamentPromotionCard.excludeParticipant(
                    task.getResult(),owner,com.google.firebase.firestore.FirebaseFirestore.getInstance()));
        }
        promotion.addOnCompleteListener(task -> {
            if (!deciding || stopped) return;
            if (!task.isSuccessful() || !canShow(owner)) { showStandardResult();return; }
            for (TournamentPromotion offer:TournamentPromotion.parseAll(task.getResult())) {
                long expiresAt=requestedAt+offer.deadline-offer.serverTime;
                if (clock.getAsLong()>=expiresAt || prefs.getBoolean(SHOWN+offer.id,false)) continue;
                show(offer,owner,expiresAt);
                return; // One invitation even when DEV exposes several promotions.
            }
            showStandardResult();
        });
    }

    private boolean canShow(String owner) {
        return !stopped && !activity.isFinishing() && !activity.isDestroyed()
                && Objects.equals(owner,uid.get()) && resultVisible.getAsBoolean();
    }

    String message(TournamentPromotion offer) {
        var locale=activity.getResources().getConfiguration().getLocales().get(0);
        String text=activity.getString(R.string.first_win_tournament_message)+"\n\n"+offer.name+"\n"
                +activity.getString(R.string.tournament_promo_prize,
                    NumberFormat.getIntegerInstance(locale).format(offer.firstPrize),offer.currency)+"\n"
                +activity.getString(R.string.tournament_promo_eligibility,offer.minimumAge);
        if (!offer.payoutMethods.isEmpty()) {
            var labels=new ArrayList<String>();
            for(String method:offer.payoutMethods) labels.add(activity.getString("bkash".equals(method)
                    ? R.string.tournament_payout_bkash : R.string.tournament_payout_rocket));
            text+="\n"+activity.getString(R.string.tournament_promo_payout,android.text.TextUtils.join(" / ",labels));
        }
        return text+"\n"+activity.getString(R.string.tournament_promo_deadline,
                android.text.format.DateFormat.getMediumDateFormat(activity).format(new Date(offer.deadline)));
    }

    private void show(TournamentPromotion offer,String owner,long expiresAt) {
        dialog=new AlertDialog.Builder(activity)
                .setTitle(R.string.first_win_tournament_title).setMessage(message(offer))
                .setPositiveButton(R.string.first_win_tournament_join,(d,which)->{
                    if (stopped || !Objects.equals(owner,uid.get())) return;
                    analytics.trackFirstWinTournamentPrompt(offer,"join");
                    if (clock.getAsLong()>=expiresAt) {
                        Toast.makeText(activity,R.string.registration_closed,Toast.LENGTH_LONG).show();
                        notNow.run(); return;
                    }
                    join.accept(offer);
                })
                .setNegativeButton(R.string.first_win_tournament_not_now,(d,which)->decline(offer))
                .setOnCancelListener(d->decline(offer)).create();
        // Only a successfully displayed dialog consumes tournament exposure; failed loads do not.
        dialog.show();
        deciding=false;decided=true;
        handler.removeCallbacks(timeout);
        prefs.edit().putBoolean(SHOWN+offer.id,true).commit();
        analytics.trackFirstWinTournamentPrompt(offer,"shown");
        replaceResult.run();
    }

    private void decline(TournamentPromotion offer) {
        analytics.trackFirstWinTournamentPrompt(offer,"not_now");
        notNow.run();
    }

    void stop() {
        stopped=true;
        if(timeout!=null)handler.removeCallbacks(timeout);
        if(dialog!=null) dialog.dismiss();
    }
}
