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
    private static final String TAG = "TAG_Soccer";
    
    private FirebaseAnalytics firebaseAnalytics;
    private FirebaseCrashlytics crashlytics;
    
    public AnalyticsManager(Context context) {
        if (context == null) {
            Log.e(TAG, "AnalyticsManager: Context is null, Firebase services will be disabled");
            this.firebaseAnalytics = null;
            this.crashlytics = null;
            return;
        }
        
        try {
            this.firebaseAnalytics = FirebaseAnalytics.getInstance(context);
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
    // AUTH FUNNEL EVENTS (GA4 Recommended)
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
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, new Bundle());
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
    }
    
    /**
     * Track successful signup with method
     * @param method "google", "email", "facebook", "anonymous"
     */
    public void trackSignupSuccess(String method) {
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
        // Null-safe parameter handling to prevent NullPointerExceptions
        String safeMethod = method != null ? method : "unknown";
        String safeErrorCode = errorCode != null ? errorCode : "unknown_error";
        String safeErrorMessage = errorMessage != null ? errorMessage : "Unknown error occurred";
        String safeStep = step != null ? step : "unknown_step";
        
        // Always log locally for debugging, even if Firebase is unavailable
        Log.d(TAG, "Tracked: signup error - " + safeMethod + " at " + safeStep + ": " + safeErrorMessage);
        
        // Check if Firebase services are available before using them
        if (crashlytics != null) {
            try {
                crashlytics.log("Signup error - Method: " + safeMethod + ", Step: " + safeStep + ", Error: " + safeErrorMessage);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log crash analytics for signup error", e);
            }
        } else {
            Log.w(TAG, "Crashlytics not available, skipping crash analytics for signup error");
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("method", safeMethod);
                params.putString("code", safeErrorCode);
                params.putString("message", safeErrorMessage);
                params.putString("step", safeStep);
                firebaseAnalytics.logEvent("sign_up_error", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log Firebase analytics for signup error", e);
            }
        } else {
            Log.w(TAG, "Firebase Analytics not available, skipping analytics for signup error");
        }
    }
    
    /**
     * Track when user declines to register with reason
     * @param reason User's reason for declining registration
     */
    public void trackSignupDeclineReason(String reason) {
        // Null-safe parameter handling
        String safeReason = reason != null ? reason : "no_reason_provided";
        
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
                params.putBoolean("is_authenticated", isUserAuthenticated);
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
        // Null-safe parameter handling to prevent NullPointerExceptions
        String safeTournamentId = tournamentId != null ? tournamentId : "unknown_tournament";
        String safeErrorCode = errorCode != null ? errorCode : "unknown_error";
        String safeErrorMessage = errorMessage != null ? errorMessage : "Unknown error occurred";
        
        Log.d(TAG, "Tracked: tournament join error for " + safeTournamentId + ": " + safeErrorMessage);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Tournament join error: " + safeTournamentId + " - " + safeErrorMessage);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("tournament_id", safeTournamentId);
                params.putString("error_code", safeErrorCode);
                params.putString("error_message", safeErrorMessage);
                firebaseAnalytics.logEvent("tournament_join_error", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
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
        // Null-safe parameter handling
        String safeNickname = nickname != null ? nickname : "unknown_nickname";
        String safeErrorMessage = errorMessage != null ? errorMessage : "Unknown error occurred";
        
        Log.d(TAG, "Tracked: nickname check error for " + safeNickname + ": " + safeErrorMessage);
        
        if (crashlytics != null) {
            try {
                crashlytics.log("Nickname AI check error: " + safeNickname + " - " + safeErrorMessage);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Crashlytics", e);
            }
        }
        
        if (firebaseAnalytics != null) {
            try {
                Bundle params = new Bundle();
                params.putString("nickname", safeNickname);
                params.putString("error_message", safeErrorMessage);
                firebaseAnalytics.logEvent("nickname_check_error", params);
            } catch (Exception e) {
                Log.e(TAG, "Failed to log to Firebase Analytics", e);
            }
        }
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
                params.putBoolean("won", won);
                params.putInt("game_type", gameType);
                params.putBoolean("is_anonymous", isAnonymous);
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
    public void setUserProperties(String authMethod, String appVersion, String language, boolean hasNickname) {
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
}