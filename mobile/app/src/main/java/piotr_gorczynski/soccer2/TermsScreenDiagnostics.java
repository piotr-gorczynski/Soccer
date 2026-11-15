package piotr_gorczynski.soccer2;

import android.os.Bundle;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Foreground duration and explicit decisions; backgrounding is not a refusal. */
final class TermsScreenDiagnostics {
    private final Bundle state;
    private final String source;
    private final Consumer<Bundle> sink;
    private final LongSupplier clock;
    private long resumedAt=-1;
    TermsScreenDiagnostics(Bundle saved,String source,LongSupplier clock,Consumer<Bundle> sink) {
        state=saved==null ? new Bundle() : new Bundle(saved);
        this.source="tournament".equals(source) ? "tournament" : "onboarding";
        this.clock=clock; this.sink=sink;
    }
    void resume() {
        if (resumedAt>=0 || state.getBoolean("closed")) return;
        resumedAt=clock.getAsLong();
        if (!state.getBoolean("shown")) {
            state.putBoolean("shown",true); step("terms_screen","shown","none","NONE","none");
        }
    }
    void pause() {
        if (resumedAt<0) return;
        state.putLong("duration",duration()); resumedAt=-1;
        if (!state.getBoolean("closed")) step("terms_screen","paused","none","NONE","none");
    }
    Bundle save() { Bundle saved=new Bundle(state); saved.putLong("duration",duration()); return saved; }
    void step(String stage,String outcome,String reason,String code,String action) {
        if (state.getBoolean("closed")) return;
        Bundle params=new Bundle();
        params.putString("stage",stage); params.putString("outcome",outcome);
        params.putString("reason",reason); params.putString("code",code);
        params.putString("entry_source",source); params.putString("action",action);
        params.putLong("duration_ms",duration()); sink.accept(params);
        if ("terms_save".equals(stage) && ("success".equals(outcome) || "declined".equals(outcome)))
            state.putBoolean("closed",true);
    }
    private long duration() {
        return Math.max(0,Math.min(86400000L,state.getLong("duration")
                +(resumedAt<0 ? 0 : Math.max(0,clock.getAsLong()-resumedAt))));
    }
}
