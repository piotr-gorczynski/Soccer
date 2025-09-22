package piotr_gorczynski.soccer2;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Centralized analytics manager for tracking user research events
 * Implements GA4 recommended events for auth and tournament funnels
 */
public class AnalyticsManager {
    private static final String TAG = "AnalyticsManager";
    
    private final FirebaseAnalytics firebaseAnalytics;
    private final FirebaseCrashlytics crashlytics;
    
    public AnalyticsManager(Context context) {
        this.firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        this.crashlytics = FirebaseCrashlytics.getInstance();
        
        Log.d(TAG, "AnalyticsManager initialized");
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // AUTH FUNNEL EVENTS (GA4 Recommended)
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track when login screen is opened
     */
    public void trackLoginScreenOpened() {
        crashlytics.log("Login screen opened");
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, new Bundle());
        Log.d(TAG, "Tracked: login screen opened");
    }
    
    /**
     * Track successful signup with method
     * @param method "google", "email", "facebook", "anonymous"
     */
    public void trackSignupSuccess(String method) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.METHOD, method);
        
        crashlytics.log("Signup success: " + method);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SIGN_UP, params);
        Log.d(TAG, "Tracked: signup success with method=" + method);
    }
    
    /**
     * Track signup error with details
     * @param method Authentication method (e.g. "google", "email", "facebook", "anonymous")
     * @param errorCode Error code identifier
     * @param errorMessage Human-readable error message
     * @param step Step in the authentication flow where error occurred
     */
    public void trackSignupError(String method, String errorCode, String errorMessage, String step) {
        // Null-safe parameter handling to prevent NullPointerExceptions
        String safeMethod = method != null ? method : "unknown";
        String safeErrorCode = errorCode != null ? errorCode : "unknown_error";
        String safeErrorMessage = errorMessage != null ? errorMessage : "Unknown error occurred";
        String safeStep = step != null ? step : "unknown_step";
        
        Bundle params = new Bundle();
        params.putString("method", safeMethod);
        params.putString("code", safeErrorCode);
        params.putString("message", safeErrorMessage);
        params.putString("step", safeStep);
        
        crashlytics.recordException(new Exception("Signup error: " + safeErrorMessage));
        crashlytics.log("Signup error - Method: " + safeMethod + ", Step: " + safeStep + ", Error: " + safeErrorMessage);
        firebaseAnalytics.logEvent("sign_up_error", params);
        Log.d(TAG, "Tracked: signup error - " + safeMethod + " at " + safeStep + ": " + safeErrorMessage);
    }
    
    /**
     * Track when user declines to register with reason
     * @param reason User's reason for declining registration
     */
    public void trackSignupDeclineReason(String reason) {
        // Null-safe parameter handling
        String safeReason = reason != null ? reason : "no_reason_provided";
        
        Bundle params = new Bundle();
        params.putString("reason", safeReason);
        
        crashlytics.log("Signup declined: " + safeReason);
        firebaseAnalytics.logEvent("signup_decline_reason", params);
        Log.d(TAG, "Tracked: signup decline reason=" + safeReason);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // TOURNAMENT FUNNEL EVENTS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track when tournament list is viewed
     */
    public void trackTournamentListViewed(int registeringCount, int runningCount, int endedCount) {
        Bundle params = new Bundle();
        params.putInt("registering_count", registeringCount);
        params.putInt("running_count", runningCount);
        params.putInt("ended_count", endedCount);
        params.putInt("total_count", registeringCount + runningCount + endedCount);
        
        crashlytics.log("Tournament list viewed: " + (registeringCount + runningCount + endedCount) + " tournaments");
        firebaseAnalytics.logEvent("tournament_view", params);
        Log.d(TAG, "Tracked: tournament list viewed with " + params.getInt("total_count") + " tournaments");
    }
    
    /**
     * Track when user starts joining a tournament
     */
    public void trackTournamentJoinStart(String tournamentId, boolean isUserAuthenticated) {
        Bundle params = new Bundle();
        params.putString("tournament_id", tournamentId);
        params.putBoolean("is_authenticated", isUserAuthenticated);
        
        crashlytics.log("Tournament join started: " + tournamentId + " (authenticated: " + isUserAuthenticated + ")");
        firebaseAnalytics.logEvent("tournament_join_start", params);
        Log.d(TAG, "Tracked: tournament join start for " + tournamentId + " (auth: " + isUserAuthenticated + ")");
    }
    
    /**
     * Track successful tournament join
     */
    public void trackTournamentJoinSuccess(String tournamentId) {
        Bundle params = new Bundle();
        params.putString("tournament_id", tournamentId);
        
        crashlytics.log("Tournament join success: " + tournamentId);
        firebaseAnalytics.logEvent("tournament_join_success", params);
        Log.d(TAG, "Tracked: tournament join success for " + tournamentId);
    }
    
    /**
     * Track tournament join error
     * @param tournamentId Tournament identifier
     * @param errorCode Error code identifier  
     * @param errorMessage Human-readable error message
     */
    public void trackTournamentJoinError(String tournamentId, String errorCode, String errorMessage) {
        // Null-safe parameter handling to prevent NullPointerExceptions
        String safeTournamentId = tournamentId != null ? tournamentId : "unknown_tournament";
        String safeErrorCode = errorCode != null ? errorCode : "unknown_error";
        String safeErrorMessage = errorMessage != null ? errorMessage : "Unknown error occurred";
        
        Bundle params = new Bundle();
        params.putString("tournament_id", safeTournamentId);
        params.putString("error_code", safeErrorCode);
        params.putString("error_message", safeErrorMessage);
        
        crashlytics.recordException(new Exception("Tournament join error: " + safeErrorMessage));
        crashlytics.log("Tournament join error: " + safeTournamentId + " - " + safeErrorMessage);
        firebaseAnalytics.logEvent("tournament_join_error", params);
        Log.d(TAG, "Tracked: tournament join error for " + safeTournamentId + ": " + safeErrorMessage);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ANONYMOUS USER FLOW EVENTS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track when anonymous user is prompted to link account
     * @param trigger What triggered the prompt (e.g. "tournament_join", "win_match", "pick_nickname")
     */
    public void trackAnonymousLinkPrompt(String trigger) {
        // Null-safe parameter handling
        String safeTrigger = trigger != null ? trigger : "unknown_trigger";
        
        Bundle params = new Bundle();
        params.putString("trigger", safeTrigger);
        
        crashlytics.log("Anonymous link prompt shown: " + safeTrigger);
        firebaseAnalytics.logEvent("anonymous_link_prompt", params);
        Log.d(TAG, "Tracked: anonymous link prompt - trigger=" + safeTrigger);
    }
    
    /**
     * Track anonymous user decision on linking
     * @param decision User's decision ("link", "dismiss", "later")
     * @param trigger What triggered the original prompt
     */
    public void trackAnonymousLinkDecision(String decision, String trigger) {
        // Null-safe parameter handling
        String safeDecision = decision != null ? decision : "unknown_decision";
        String safeTrigger = trigger != null ? trigger : "unknown_trigger";
        
        Bundle params = new Bundle();
        params.putString("decision", safeDecision);
        params.putString("trigger", safeTrigger);
        
        crashlytics.log("Anonymous link decision: " + safeDecision + " (trigger: " + safeTrigger + ")");
        firebaseAnalytics.logEvent("anonymous_link_decision", params);
        Log.d(TAG, "Tracked: anonymous link decision=" + safeDecision + " trigger=" + safeTrigger);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // USER ATTRIBUTES FOR SEGMENTATION
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Set user properties for segmentation
     */
    public void setUserProperties(String authMethod, String appVersion, String language, boolean hasNickname) {
        firebaseAnalytics.setUserProperty("auth_method", authMethod);
        firebaseAnalytics.setUserProperty("app_version", appVersion);
        firebaseAnalytics.setUserProperty("language", language);
        firebaseAnalytics.setUserProperty("has_nickname", hasNickname ? "true" : "false");
        
        crashlytics.setCustomKey("auth_method", authMethod);
        crashlytics.setCustomKey("app_version", appVersion);
        crashlytics.setCustomKey("language", language);
        
        Log.d(TAG, "Set user properties: auth=" + authMethod + ", version=" + appVersion + ", lang=" + language);
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
        
        crashlytics.log("AUTH: " + safeStep + " - " + safeDetails);
        Log.d(TAG, "Auth breadcrumb: " + safeStep + " - " + safeDetails);
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
        
        crashlytics.log("TOURNAMENT: " + safeStep + " [" + safeTournamentId + "] - " + safeDetails);
        Log.d(TAG, "Tournament breadcrumb: " + safeStep + " [" + safeTournamentId + "] - " + safeDetails);
    }
}