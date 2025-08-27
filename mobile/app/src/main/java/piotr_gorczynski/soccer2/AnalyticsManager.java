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
     */
    public void trackSignupError(String method, String errorCode, String errorMessage, String step) {
        Bundle params = new Bundle();
        params.putString("method", method);
        params.putString("code", errorCode);
        params.putString("message", errorMessage);
        params.putString("step", step);
        
        crashlytics.recordException(new Exception("Signup error: " + errorMessage));
        crashlytics.log("Signup error - Method: " + method + ", Step: " + step + ", Error: " + errorMessage);
        firebaseAnalytics.logEvent("sign_up_error", params);
        Log.d(TAG, "Tracked: signup error - " + method + " at " + step + ": " + errorMessage);
    }
    
    /**
     * Track when user declines to register with reason
     */
    public void trackSignupDeclineReason(String reason) {
        Bundle params = new Bundle();
        params.putString("reason", reason);
        
        crashlytics.log("Signup declined: " + reason);
        firebaseAnalytics.logEvent("signup_decline_reason", params);
        Log.d(TAG, "Tracked: signup decline reason=" + reason);
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
     */
    public void trackTournamentJoinError(String tournamentId, String errorCode, String errorMessage) {
        Bundle params = new Bundle();
        params.putString("tournament_id", tournamentId);
        params.putString("error_code", errorCode);
        params.putString("error_message", errorMessage);
        
        crashlytics.recordException(new Exception("Tournament join error: " + errorMessage));
        crashlytics.log("Tournament join error: " + tournamentId + " - " + errorMessage);
        firebaseAnalytics.logEvent("tournament_join_error", params);
        Log.d(TAG, "Tracked: tournament join error for " + tournamentId + ": " + errorMessage);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ANONYMOUS USER FLOW EVENTS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Track when anonymous user is prompted to link account
     */
    public void trackAnonymousLinkPrompt(String trigger) {
        Bundle params = new Bundle();
        params.putString("trigger", trigger); // "tournament_join", "win_match", "pick_nickname"
        
        crashlytics.log("Anonymous link prompt shown: " + trigger);
        firebaseAnalytics.logEvent("anonymous_link_prompt", params);
        Log.d(TAG, "Tracked: anonymous link prompt - trigger=" + trigger);
    }
    
    /**
     * Track anonymous user decision on linking
     */
    public void trackAnonymousLinkDecision(String decision, String trigger) {
        Bundle params = new Bundle();
        params.putString("decision", decision); // "link", "dismiss", "later"
        params.putString("trigger", trigger);
        
        crashlytics.log("Anonymous link decision: " + decision + " (trigger: " + trigger + ")");
        firebaseAnalytics.logEvent("anonymous_link_decision", params);
        Log.d(TAG, "Tracked: anonymous link decision=" + decision + " trigger=" + trigger);
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
     */
    public void addAuthBreadcrumb(String step, String details) {
        crashlytics.log("AUTH: " + step + " - " + details);
        Log.d(TAG, "Auth breadcrumb: " + step + " - " + details);
    }
    
    /**
     * Add breadcrumb for debugging tournament flow issues
     */
    public void addTournamentBreadcrumb(String step, String tournamentId, String details) {
        crashlytics.log("TOURNAMENT: " + step + " [" + tournamentId + "] - " + details);
        Log.d(TAG, "Tournament breadcrumb: " + step + " [" + tournamentId + "] - " + details);
    }
}