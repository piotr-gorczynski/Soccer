package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;

/**
 * Test activity to validate analytics implementation
 * This demonstrates all the analytics features working together
 */
public class AnalyticsTestActivity extends BaseActivity {
    private static final String TAG = "AnalyticsTest";
    
    private AnalyticsManager analyticsManager;
    private RemoteConfigHelper remoteConfigHelper;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Simple test layout
        TextView textView = new TextView(this);
        textView.setText("Analytics Test - Check logs for events");
        textView.setPadding(50, 50, 50, 50);
        
        Button testButton = new Button(this);
        testButton.setText("Test Analytics Events");
        testButton.setOnClickListener(v -> runAnalyticsTests());
        
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.addView(textView);
        layout.addView(testButton);
        
        setContentView(layout);
        
        // Get managers from SoccerApp
        SoccerApp app = (SoccerApp) getApplicationContext();
        analyticsManager = app.getAnalyticsManager();
        remoteConfigHelper = app.getRemoteConfigHelper();
        
        Log.d(TAG, "AnalyticsTestActivity created successfully");
    }
    
    private void runAnalyticsTests() {
        Log.d(TAG, "Running analytics tests...");
        
        // Test auth events
        analyticsManager.trackLoginScreenOpened();
        analyticsManager.trackSignupSuccess("test");
        analyticsManager.trackSignupError("test", "test_error", "Test error message", "test_step");
        analyticsManager.trackSignupDeclineReason("test_reason");
        
        // Test tournament events
        analyticsManager.trackTournamentListViewed(5, 3, 2);
        analyticsManager.trackTournamentJoinStart("test_tournament", true);
        analyticsManager.trackTournamentJoinSuccess("test_tournament");
        analyticsManager.trackTournamentJoinError("test_tournament", "test_error", "Test error");
        
        // Test anonymous events
        analyticsManager.trackAnonymousLinkPrompt("test_trigger");
        analyticsManager.trackAnonymousLinkDecision("link", "test_trigger");
        
        // Test breadcrumbs
        analyticsManager.addAuthBreadcrumb("test_step", "test details");
        analyticsManager.addTournamentBreadcrumb("test_step", "test_tournament", "test details");
        
        // Test user properties
        analyticsManager.setUserProperties("test", "9.0", "en", true);
        
        // Test Remote Config values
        Log.d(TAG, "Signup prompt variant: " + remoteConfigHelper.getSignupPromptVariant());
        Log.d(TAG, "Registration copy variant: " + remoteConfigHelper.getRegistrationCopyVariant());
        Log.d(TAG, "Should show decline dialog: " + remoteConfigHelper.shouldShowDeclineDialog());
        
        Log.d(TAG, "Analytics tests completed - check Firebase console for events");
    }
}