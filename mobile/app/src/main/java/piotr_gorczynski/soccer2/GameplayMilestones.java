package piotr_gorczynski.soccer2;

import android.content.SharedPreferences;

/** Installation-scoped gameplay milestones shared by both flavours and the BD prompt. */
final class GameplayMilestones {
    private static final Object LOCK = new Object();
    private static final String COMPLETED = "first_game_completed";
    // Keep the existing prompt keys: an upgrade must not create a second first-win definition.
    private static final String INITIALIZED = "first_win_prompt_initialized";
    private static final String WON = "first_win_prompt_win_recorded";
    private final SharedPreferences prefs;
    private final AnalyticsManager analytics;

    GameplayMilestones(SharedPreferences prefs, AnalyticsManager analytics) {
        this.prefs = prefs;
        this.analytics = analytics;
        synchronized (LOCK) {
            if (!prefs.getBoolean(INITIALIZED, false)) {
                // Historical installations do not have a reconstructable win history.
                prefs.edit().putBoolean(INITIALIZED, true)
                        .putBoolean(WON, prefs.getBoolean(COMPLETED, false)).commit();
            }
        }
    }

    static boolean playerWon(int gameType, int winner, int localPlayerIndex) {
        if (winner != 0 && winner != 1) return false;
        return switch (gameType) {
            case 1, 2 -> winner == 0;
            case 3 -> (localPlayerIndex == 0 || localPlayerIndex == 1) && winner == localPlayerIndex;
            default -> false;
        };
    }

    void completed(boolean won, int gameType, boolean anonymous) {
        synchronized (LOCK) {
            if (prefs.getBoolean(COMPLETED, false)) return;
            if (!prefs.edit().putBoolean(COMPLETED, true).commit()) return;
            // Preserve the historical local-game coverage and meaning of the legacy event.
            if (gameType == 1 || gameType == 2) analytics.trackFirstGameWin(won, gameType, anonymous);
            analytics.trackFirstGameCompleted(won, gameType, anonymous);
        }
    }

    boolean hasRecordedWin() {
        synchronized (LOCK) { return prefs.getBoolean(WON, false); }
    }

    /** Called only for an actual completed victory; returns the single first-win claim. */
    boolean won(int gameType) {
        synchronized (LOCK) {
            if (prefs.getBoolean(WON, false)) return false;
            if (!prefs.edit().putBoolean(WON, true).commit()) return false;
            analytics.trackFirstWin(gameType);
            return true;
        }
    }
}
