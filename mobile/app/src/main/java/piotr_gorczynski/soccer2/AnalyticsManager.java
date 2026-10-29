package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Centralized analytics manager for tracking user research events
 * BD funnel schema 2 corrects legacy auth meanings; see docs/BANGLADESH_ANALYTICS_FUNNEL.md.
 */
public class AnalyticsManager {

    static boolean isBangladesh() { return "bangladesh".equals(BuildConfig.FLAVOR_market); }

    static String normalizeMethod(String method) {
        return switch (method == null ? "" : method) {
            case "google", "google.com" -> "google";
            case "facebook", "facebook.com" -> "facebook";
            case "microsoft", "microsoft.com" -> "microsoft";
            case "email", "password" -> "email";
            case "anonymous" -> "anonymous";
            default -> "unknown";
        };
    }

    public void trackLoginAttempt(String method) {
        if (isBangladesh()) trackControlledOutcome("login_attempt", "authentication", "submit", "none", normalizeMethod(method));
    }

    public void trackAnonymousWarning(String action) {
        String choice = allowed(action, "shown", "proceed", "cancel", "dismiss");
        if (crashlytics != null) {
            try { crashlytics.log("AUTH: anonymous_warning - " + choice); }
            catch (Exception ignored) { Log.w(TAG, "Could not report warning breadcrumb"); }
        }
        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString("action", choice);
            params.putString("method", "anonymous");
            try { firebaseAnalytics.logEvent("anonymous_login_warning", params); }
            catch (Exception ignored) { Log.w(TAG, "Could not report warning choice"); }
        }
    }

    public void trackAuthenticationSucceeded(String method, boolean newAccount) {
        if (!isBangladesh() || firebaseAnalytics == null) return;
        Bundle params = new Bundle();
        params.putString("method", normalizeMethod(method));
        try {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, params);
            if (newAccount) firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, params);
        } catch (Exception ignored) { Log.w(TAG, "Could not report authentication result"); }
    }

    public void trackAccountCreated() {
        if (!isBangladesh() || firebaseAnalytics == null) return;
        Bundle params = new Bundle();
        params.putString("method", "email");
        try { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, params); }
        catch (Exception ignored) { Log.w(TAG, "Could not report account creation"); }
    }

    /** Fixed vocabularies only. Views reuse automatic screen_view; no field contents. */
    public void trackOnboardingStep(String stage, String outcome, String reason) {
        if (isBangladesh()) trackControlledOutcome("onboarding_step", stage, outcome, reason, null);
    }

    private void trackControlledOutcome(String event, String stage, String outcome, String reason, String method) {
        if (firebaseAnalytics == null) return;
        Bundle params = new Bundle();
        params.putString("stage", allowed(stage, "nickname", "uniqueness", "content", "profile_write", "terms_load", "terms_save", "authentication", "join", "readiness"));
        params.putString("outcome", allowed(outcome, "submit", "success", "failure", "declined"));
        params.putString("reason", allowed(reason, "none", "empty", "too_long", "taken", "rejected", "network", "permission", "capacity", "not_eligible", "unknown"));
        if (method != null) params.putString("method", normalizeMethod(method));
        try { firebaseAnalytics.logEvent(event, params); }
        catch (Exception ignored) { Log.w(TAG, "Could not report funnel event"); }
    }

    private static String allowed(String value, String... values) {
        for (String candidate : values) if (candidate.equals(value)) return candidate;
        return "unknown";
    }

    public void trackTournamentJoinSubmit() {
        if (isBangladesh()) trackSimpleEvent("tournament_join_submit");
    }

    private static final String TAG = "TAG_Soccer";
    
    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseCrashlytics crashlytics;

    public void trackAppVariantConflictShown() {
        trackSimpleEvent("app_variant_conflict_shown");
    }

    public void trackAppVariantConflictClosed() {
        trackSimpleEvent("app_variant_conflict_closed");
    }

    public void trackPrizesView() {
        trackSimpleEvent("prizes_view");
    }

    public void trackPrizeDetailsView() {
        trackSimpleEvent("prize_details_view");
    }

    private void trackSimpleEvent(String event) {
        if (firebaseAnalytics == null) return;
        try {
            firebaseAnalytics.logEvent(event, new Bundle());
            Log.d(TAG, "Tracked: " + event);
        } catch (Exception e) {
            Log.e(TAG, "Failed to log event: " + event, e);
        }
    }
    
    public AnalyticsManager(Context context) {
        if (context == null) {
            Log.e(TAG, "AnalyticsManager: Context is null, Firebase services will be disabled");
            this.firebaseAnalytics = null;
            this.crashlytics = null;
            return;
        }
        
        try {
            this.firebaseAnalytics = FirebaseAnalytics.getInstance(context);
            // Build metadata is available before login, including for automatic events.
            Bundle defaults = new Bundle();
            defaults.putString("app_variant", BuildConfig.FLAVOR_market);
            defaults.putLong("funnel_schema", isBangladesh() ? 2 : 1);
            defaults.putString("app_environment", BuildConfig.FLAVOR_environment);
            defaults.putString("app_build_type", BuildConfig.BUILD_TYPE);
            firebaseAnalytics.setDefaultEventParameters(defaults);
            firebaseAnalytics.setUserProperty("app_variant", BuildConfig.FLAVOR_market);
            firebaseAnalytics.setUserProperty("app_environment", BuildConfig.FLAVOR_environment);
            firebaseAnalytics.setUserProperty("app_build_type", BuildConfig.BUILD_TYPE);
            firebaseAnalytics.setUserProperty("app_version", BuildConfig.VERSION_NAME);
            this.crashlytics = FirebaseCrashlytics.getInstance();
            Log.d(TAG, "AnalyticsManager initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "AnalyticsManager: Failed to initialize Firebase services", e);
            // This should not happen, but in case of any initialization issues
            // we'll set them to null to prevent NPE later
            this.firebaseAnalytics = null;
            this.crashlytics = null;
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // AUTH FUNNEL EVENTS (recommended login/sign_up plus documented legacy events)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track when login screen is opened
     */
    public void trackLoginScreenOpened() {
        Log.d(TAG, "Tracked: login screen opened");
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Login screen opened");
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                if (!isBangladesh()) firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, new Bundle());
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }

    /** Explicit sign-in completed through the required Terms and saved nickname gates. */
    public void trackLoginSuccess(String method) {
        if (firebaseAnalytics == null) return;
        String normalized = switch (method == null ? "unknown" : method) {
            case "google.com", "google" -> "google";
            case "facebook.com", "facebook" -> "facebook";
            case "microsoft.com", "microsoft" -> "microsoft";
            case "password", "email" -> "email";
            case "anonymous" -> "anonymous";
            default -> "unknown";
        };
        try {
            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.METHOD, normalized);
            firebaseAnalytics.logEvent("login_success", params);
        } catch (Exception ignored) {
            Log.w(TAG, "Could not report successful login");
        }
    }
    
    /**
     * Track successful signup with method
     * @param method "google", "email", "facebook", "anonymous"
     */
    public void trackSignupSuccess(String method) {
        if (isBangladesh()) return; // BD sign_up is emitted from Auth new-user evidence.
        String safeMethod = method != null ? method : "unknown";
        Log.d(TAG, "Tracked: signup success with method=" + safeMethod);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Signup success: " + safeMethod);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log signup success to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString(FirebaseAnalytics.Param.METHOD, safeMethod);
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log signup success to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track signup error with details
     * @param method Authentication method (e.g. "google", "email", "facebook", "anonymous")
     * @param errorCode Error code identifier
     * @param errorMessage Human-readable error message
     * @param step Step in the authentication flow where error occurred
     */
    public void trackSignupError(String method, String errorCode, String errorMessage, String step) {
        trackControlledOutcome("sign_up_error", "authentication", "failure", "unknown", normalizeMethod(method));
    }

    /** Reports one failed auth operation without sending SDK messages or causes. */
    public void trackAuthFailure(String method, String step, Exception error) {
        if (isBangladesh() && "anonymous".equals(method)) {
            trackAnonymousAuthFailure(step, error);
            return;
        }
        if ("google.com".equals(method)) method = "google";
        if ("facebook.com".equals(method)) method = "facebook";
        if ("microsoft.com".equals(method)) method = "microsoft";
        AuthFailureException diagnostic = new AuthFailureException(method, step, error);
        boolean cancelled = "ERROR_WEB_CONTEXT_CANCELED".equals(diagnostic.code)
                || error instanceof com.facebook.FacebookOperationCanceledException
                || "cancelled".equals(step);
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("method", method);
                params.putString("step", step);
                params.putString("code", diagnostic.code);
                params.putString("reason", cancelled ? "cancelled" :
                    "UNKNOWN".equals(diagnostic.code) ? "unknown" :
                    error instanceof com.google.firebase.FirebaseNetworkException || diagnostic.code.contains("NETWORK") || diagnostic.code.contains("UNAVAILABLE") ? "network" :
                    diagnostic.code.contains("INVALID_CREDENTIAL") || diagnostic.code.contains("WRONG_PASSWORD") ? "invalid_credentials" : "provider_error");
                firebaseAnalytics.logEvent(cancelled ? "auth_cancelled" : "sign_up_error", params);
            } catch (Exception ignored) {
                Log.w(TAG, "Could not report auth analytics");
            }
        }
        if (!cancelled && crashlytics != null) {
            try {
                crashlytics.recordException(diagnostic);
            } catch (Exception ignored) {
                Log.w(TAG, "Could not report auth non-fatal");
            }
        }
    }

    private void trackAnonymousAuthFailure(String step, Exception error) {
        AnonymousAuthDiagnostic diagnostic = AnonymousAuthDiagnostic.from(step, error);
        if (firebaseAnalytics != null) {
            Bundle params = new Bundle();
            params.putString("method", "anonymous");
            params.putString("stage", diagnostic.step);
            params.putString("step", diagnostic.step);
            params.putString("outcome", "failure");
            params.putString("reason", diagnostic.reason);
            params.putString("code", diagnostic.code);
            try { firebaseAnalytics.logEvent("sign_up_error", params); }
            catch (Exception ignored) { Log.w(TAG, "Could not report anonymous auth analytics"); }
        }
        if (crashlytics != null) {
            try { crashlytics.log(diagnostic.breadcrumb()); }
            catch (Exception ignored) { Log.w(TAG, "Could not report anonymous auth breadcrumb"); }
        }
    }
    
    /**
     * Track when user declines to register with reason
     * @param reason User's reason for declining registration
     */
    public void trackSignupDeclineReason(String reason) {
        // Null-safe parameter handling
        String safeReason = allowed(reason, "just_playing_offline", "privacy_concerns", "too_complicated", "dont_use_google_email", "other", "no_reason_provided");
        
        Log.d(TAG, "Tracked: signup decline reason=" + safeReason);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Signup declined: " + safeReason);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("reason", safeReason);
                firebaseAnalytics.logEvent("signup_decline_reason", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOURNAMENT FUNNEL EVENTS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track when tournament list is viewed
     */
    public void trackTournamentListViewed(int registeringCount, int runningCount, int endedCount) {
        int totalCount = registeringCount + runningCount + endedCount;
        Log.d(TAG, "Tracked: tournament list viewed with " + totalCount + " tournaments");
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Tournament list viewed: " + totalCount + " tournaments");
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putInt("registering_count", registeringCount);
                params.putInt("running_count", runningCount);
                params.putInt("ended_count", endedCount);
                params.putInt("total_count", totalCount);
                firebaseAnalytics.logEvent("tournament_view", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track when user starts joining a tournament
     */
    public void trackTournamentJoinStart(String tournamentId, boolean isUserAuthenticated) {
        Log.d(TAG, "Tracked: tournament join start for " + tournamentId + " (auth: " + isUserAuthenticated + ")");
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Tournament join started: " + tournamentId + " (authenticated: " + isUserAuthenticated + ")");
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("tournament_id", tournamentId);
                params.putLong("is_authenticated", isUserAuthenticated ? 1 : 0);
                firebaseAnalytics.logEvent("tournament_join_start", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track successful tournament join
     */
    public void trackTournamentJoinSuccess(String tournamentId) {
        Log.d(TAG, "Tracked: tournament join success for " + tournamentId);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Tournament join success: " + tournamentId);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("tournament_id", tournamentId);
                firebaseAnalytics.logEvent("tournament_join_success", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track tournament join error
     * @param tournamentId Tournament identifier
     * @param errorCode Error code identifier  
     * @param errorMessage Human-readable error message
     */
    public void trackTournamentJoinError(String tournamentId, String errorCode, String errorMessage) {
        String reason = switch (errorCode == null ? "" : errorCode.toUpperCase(java.util.Locale.ROOT)) {
            case "UNAVAILABLE", "DEADLINE_EXCEEDED", "NETWORK_ERROR" -> "network";
            case "PERMISSION_DENIED", "UNAUTHENTICATED" -> "permission";
            case "RESOURCE_EXHAUSTED" -> "capacity";
            case "FAILED_PRECONDITION", "ALREADY_EXISTS", "NOT_FOUND", "INVALID_ARGUMENT" -> "not_eligible";
            default -> "unknown";
        };
        trackControlledOutcome("tournament_join_error", "join", "failure", reason, null);
    }

    // ═══════════════════════════════════════════════════════════════════
    // NICKNAME CHECK EVENTS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track nickname content check error (AI check failure)
     * @param nickname The nickname that was being checked
     * @param errorMessage Error message from the check
     */
    public void trackNicknameCheckError(String nickname, String errorMessage) {
        // Compatibility entry point: never transmit the nickname or the message.
        trackControlledOutcome("nickname_check_error", "content", "failure", "unknown", null);
    }

    // ═══════════════════════════════════════════════════════════════════
    // ANONYMOUS USER FLOW EVENTS
    // ═══════════════════════════════════════════════════════════════════
    
    // ═══════════════════════════════════════════════════════════════════
    // USER RETENTION METRICS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track when tutorial is completed
     * @param tutorialType Type of tutorial completed ("hand_tutorial" or "tutorial_messages")
     */
    public void trackTutorialCompleted(String tutorialType) {
        String safeTutorialType = tutorialType != null ? tutorialType : "unknown";
        Log.d(TAG, "Tracked: tutorial_completed - type=" + safeTutorialType);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Tutorial completed: " + safeTutorialType);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("tutorial_type", safeTutorialType);
                firebaseAnalytics.logEvent("tutorial_completed", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track number of games played before signup when user converts from anonymous to registered
     * @param gamesPlayed Number of games played as anonymous user
     * @param signupMethod Method used to sign up ("google", "email", "facebook", "microsoft")
     */
    public void trackGamesPlayedBeforeSignup(int gamesPlayed, String signupMethod) {
        String safeSignupMethod = signupMethod != null ? signupMethod : "unknown";
        Log.d(TAG, "Tracked: games_played_before_signup - count=" + gamesPlayed + ", method=" + safeSignupMethod);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Games played before signup: " + gamesPlayed + " (method: " + safeSignupMethod + ")");
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putInt("games_count", gamesPlayed);
                params.putString("signup_method", safeSignupMethod);
                firebaseAnalytics.logEvent("games_played_before_signup", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track first game completion and outcome
     * @param won Whether the user won their first game
     * @param gameType Type of game (1=PvP local, 2=vs Android, 3=PvP online)
     * @param isAnonymous Whether the user is anonymous
     */
    public void trackFirstGameWin(boolean won, int gameType, boolean isAnonymous) {
        Log.d(TAG, "Tracked: first_game_win - won=" + won + ", gameType=" + gameType + ", anonymous=" + isAnonymous);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("First game: " + (won ? "WON" : "LOST") + " (type: " + gameType + ", anonymous: " + isAnonymous + ")");
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putLong("won", won ? 1 : 0);
                params.putInt("game_type", gameType);
                params.putLong("is_anonymous", isAnonymous ? 1 : 0);
                firebaseAnalytics.logEvent("first_game_win", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track time to first game from app installation
     * @param timeToFirstGameMs Time in milliseconds from first app open to first game start
     */
    public void trackTimeToFirstGame(long timeToFirstGameMs) {
        long timeInSeconds = timeToFirstGameMs / 1000;
        Log.d(TAG, "Tracked: time_to_first_game - " + timeInSeconds + " seconds");
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Time to first game: " + timeInSeconds + "s");
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putLong("time_seconds", timeInSeconds);
                params.putLong("time_ms", timeToFirstGameMs);
                firebaseAnalytics.logEvent("time_to_first_game", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // USER ATTRIBUTES FOR SEGMENTATION
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Set user properties for segmentation
     */
    public void setUserProperties(String authMethod, String language, boolean hasNickname) {
        String appVersion = BuildConfig.VERSION_NAME;
        Log.d(TAG, "Set user properties: auth=" + authMethod + ", version=" + appVersion + ", lang=" + language);
        
        if (firebaseAnalytics != null) {
            try {
                firebaseAnalytics.setUserProperty("auth_method", authMethod);
                firebaseAnalytics.setUserProperty("app_version", appVersion);
                firebaseAnalytics.setUserProperty("language", language);
                firebaseAnalytics.setUserProperty("has_nickname", hasNickname ? "true" : "false");
            } catch (Exception e) {
                Log.e(TAG, "Failed to set Firebase Analytics user properties", e);
            }
        }
        
        if (crashlytics != null) {
            try {
                crashlytics.setCustomKey("auth_method", authMethod);
                crashlytics.setCustomKey("app_version", appVersion);
                crashlytics.setCustomKey("language", language);
            } catch (Exception e) {
                Log.e(TAG, "Failed to set Crashlytics custom keys", e);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BREADCRUMBS FOR DEBUGGING
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Add breadcrumb for debugging auth flow issues
     * @param step Current step in the auth flow
     * @param details Additional details about the step
     */
    public void addAuthBreadcrumb(String step, String details) {
        // Null-safe parameter handling
        String safeStep = step != null ? step : "unknown_step";
        String safeDetails = details != null ? details : "no_details";
        
        Log.d(TAG, "Auth breadcrumb: " + safeStep + " - " + safeDetails);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("AUTH: " + safeStep + " - " + safeDetails);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log auth breadcrumb to Crashlytics", e);
            }
        }
    }
    
    /**
     * Add breadcrumb for debugging tournament flow issues
     * @param step Current step in the tournament flow
     * @param tournamentId Tournament identifier
     * @param details Additional details about the step
     */
    public void addTournamentBreadcrumb(String step, String tournamentId, String details) {
        // Null-safe parameter handling
        String safeStep = step != null ? step : "unknown_step";
        String safeTournamentId = tournamentId != null ? tournamentId : "unknown_tournament";
        String safeDetails = details != null ? details : "no_details";
        
        Log.d(TAG, "Tournament breadcrumb: " + safeStep + " [" + safeTournamentId + "] - " + safeDetails);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("TOURNAMENT: " + safeStep + " [" + safeTournamentId + "] - " + safeDetails);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log tournament breadcrumb to Crashlytics", e);
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // BANGLADESH MIGRATION PROMOTION EVENTS
    // ═══════════════════════════════════════════════════════════════════
}
