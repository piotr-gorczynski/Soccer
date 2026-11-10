package piotr_gorczynski.soccer2;

import android.os.Bundle;

/** Bounded, non-PII milestones for one regulation screen, including recreation. */
final class TournamentRegulationDiagnostics {
    interface Sink { void report(Bundle parameters); }
    private final Sink sink;
    private final Bundle state;

    TournamentRegulationDiagnostics(Bundle saved, Sink sink) {
        this.sink = sink;
        state = saved == null ? new Bundle() : new Bundle(saved);
    }

    Bundle save() { return new Bundle(state); }

    void beginLoad(boolean retry) {
        if (state.getBoolean("closed")) return;
        if (!state.containsKey("load_attempt")) state.putInt("load_attempt", 1);
        if (retry) {
            state.putInt("load_attempt", state.getInt("load_attempt") + 1);
            state.putBoolean("load_reported", false);
            state.putBoolean("content_loaded", false);
            state.putBoolean("configuration_valid", false);
            state.putBoolean("confirmations_required", false);
            state.putBoolean("accept_ready", false);
            state.putInt("checked_count", 0);
            report("retry", "submit", "none");
        }
    }

    void load(boolean contentLoaded, boolean configurationValid, boolean required, String reason) {
        if (state.getBoolean("closed")) return;
        boolean unchanged = state.getBoolean("load_reported")
                && contentLoaded == state.getBoolean("content_loaded")
                && configurationValid == state.getBoolean("configuration_valid");
        state.putBoolean("load_reported", true);
        state.putBoolean("content_loaded", contentLoaded);
        state.putBoolean("configuration_valid", configurationValid);
        state.putBoolean("confirmations_required", required);
        if (unchanged) return;
        String safeReason = switch (reason) {
            case "none", "fallback", "not_found", "missing_content", "network", "permission",
                    "invalid_configuration", "missing_user", "missing_regulation" -> reason;
            default -> "unknown";
        };
        report("load", contentLoaded && configurationValid ? "success" : "failure", safeReason);
    }

    void checkboxVisible(int index) {
        if (index < 0 || index > 2 || !state.getBoolean("content_loaded")
                || !state.getBoolean("configuration_valid") || !state.getBoolean("confirmations_required")
                || state.getBoolean("closed")) return;
        state.putInt("seen_mask", state.getInt("seen_mask") | (1 << index));
        if (state.getInt("seen_mask") == 7 && !state.getBoolean("visible_reported")) {
            state.putBoolean("visible_reported", true);
            report("confirmations_visible", "shown", "none");
        }
    }

    void controls(int checked, boolean ready) {
        state.putInt("checked_count", Math.max(0, Math.min(3, checked)));
        state.putBoolean("accept_ready", ready);
        if (ready && !state.getBoolean("ready_reported") && !state.getBoolean("closed")) {
            state.putBoolean("ready_reported", true);
            report("accept_ready", "ready", "none");
        }
    }

    void submitted() { state.putBoolean("submitted", true); }

    void exit(boolean declined) {
        if (state.getBoolean("closed")) return;
        state.putBoolean("closed", true);
        if (!state.getBoolean("submitted"))
            report("exit", declined ? "declined" : "abandoned", "none");
    }

    private void report(String stage, String outcome, String reason) {
        Bundle params = new Bundle();
        params.putString("stage", stage);
        params.putString("outcome", outcome);
        params.putString("reason", reason);
        params.putLong("content_loaded", state.getBoolean("content_loaded") ? 1 : 0);
        params.putLong("configuration_valid", state.getBoolean("configuration_valid") ? 1 : 0);
        params.putLong("confirmations_required", state.getBoolean("confirmations_required") ? 1 : 0);
        params.putLong("confirmations_seen", state.getBoolean("visible_reported") ? 1 : 0);
        params.putLong("checked_count", state.getInt("checked_count"));
        params.putLong("accept_ready", state.getBoolean("accept_ready") ? 1 : 0);
        params.putLong("load_attempt", state.getInt("load_attempt", 1));
        sink.report(params);
    }
}
