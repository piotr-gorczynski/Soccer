package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.graphics.Rect;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RegulationActivity extends BaseActivity {

    private String tournamentId;
    private String regulationId;
    private AnalyticsManager analyticsManager;
    private Button acceptBtn;
    private LinearLayout eligibilityConfirmation;
    private TextView eligibilityConfirmationHint;
    private CheckBox confirmMinimumAge;
    private CheckBox confirmPayoutAccount;
    private CheckBox confirmTournamentTerms;
    private boolean regulationContentLoaded;
    private boolean eligibilityConfigurationValid;
    private boolean requiresEligibilityConfirmation;
    private TextView nameTv;
    private TextView bodyTv;
    private TextView regulationErrorMessage;
    private View regulationStatus;
    private View regulationScroll;
    private View regulationLoading;
    private View regulationLoadingMessage;
    private Button retryRegulation;
    private boolean loadInFlight;
    private int loadGeneration;
    private TournamentRegulationDiagnostics diagnostics;
    private final ViewTreeObserver.OnScrollChangedListener diagnosticScrollListener = this::inspectConfirmationVisibility;
    private final ViewTreeObserver.OnGlobalLayoutListener diagnosticLayoutListener = this::inspectConfirmationVisibility;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regulation);

        nameTv = findViewById(R.id.regulationName);
        bodyTv = findViewById(R.id.regulationBody);
        regulationStatus = findViewById(R.id.regulationStatus);
        regulationScroll = findViewById(R.id.regulationScroll);
        regulationLoading = findViewById(R.id.regulationLoading);
        regulationLoadingMessage = findViewById(R.id.regulationLoadingMessage);
        regulationErrorMessage = findViewById(R.id.regulationErrorMessage);
        retryRegulation = findViewById(R.id.retryRegulation);
        acceptBtn = findViewById(R.id.acceptRegulation);
        Button declineBtn = findViewById(R.id.declineRegulation);
        eligibilityConfirmation = findViewById(R.id.eligibilityConfirmation);
        eligibilityConfirmationHint = findViewById(R.id.eligibilityConfirmationHint);
        confirmMinimumAge = findViewById(R.id.confirmMinimumAge);
        confirmPayoutAccount = findViewById(R.id.confirmPayoutAccount);
        confirmTournamentTerms = findViewById(R.id.confirmTournamentTerms);

        View.OnClickListener confirmationListener = v -> updateAcceptButtonState();
        confirmMinimumAge.setOnClickListener(confirmationListener);
        confirmPayoutAccount.setOnClickListener(confirmationListener);
        confirmTournamentTerms.setOnClickListener(confirmationListener);

        tournamentId = getIntent().getStringExtra("tournamentId");
        regulationId = getIntent().getStringExtra("regulationId");
        
        // Get analytics manager from SoccerApp  
        analyticsManager = ((SoccerApp) getApplicationContext()).getAnalyticsManager();
        diagnostics = new TournamentRegulationDiagnostics(savedInstanceState == null ? null
                : savedInstanceState.getBundle("regulation_diagnostics"), parameters -> {
            if (analyticsManager != null) analyticsManager.trackTournamentRegulationStep(tournamentId, parameters);
        });
        eligibilityConfirmation.getViewTreeObserver().addOnScrollChangedListener(diagnosticScrollListener);
        eligibilityConfirmation.getViewTreeObserver().addOnGlobalLayoutListener(diagnosticLayoutListener);
        declineBtn.setOnClickListener(v -> {
            snapshotDiagnosticControls();
            diagnostics.exit(true);
            finish();
        });

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".onCreate" +
                ": tournamentId=" + tournamentId + " regulationId=" + regulationId);

        retryRegulation.setOnClickListener(v -> loadRegulation(true));
        acceptBtn.setOnClickListener(v -> acceptAndJoin());
        loadRegulation(false);
    }

    private void loadRegulation(boolean retry) {
        if (loadInFlight || isFinishing() || isDestroyed()) return;
        loadInFlight = true;
        int generation = ++loadGeneration;
        diagnostics.beginLoad(retry);
        regulationContentLoaded = false;
        eligibilityConfigurationValid = false;
        requiresEligibilityConfirmation = false;
        confirmMinimumAge.setChecked(false);
        confirmPayoutAccount.setChecked(false);
        confirmTournamentTerms.setChecked(false);
        eligibilityConfirmation.setVisibility(View.GONE);
        bodyTv.setText("");
        regulationScroll.setVisibility(View.GONE);
        regulationStatus.setVisibility(View.VISIBLE);
        regulationLoading.setVisibility(View.VISIBLE);
        regulationLoadingMessage.setVisibility(View.VISIBLE);
        regulationErrorMessage.setVisibility(View.GONE);
        retryRegulation.setVisibility(View.GONE);
        retryRegulation.setEnabled(false);
        updateAcceptButtonState();

        if (TextUtils.isEmpty(regulationId)) {
            showRegulationError(generation, R.string.regulation_not_found, "missing_regulation", false);
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            showRegulationError(generation, R.string.regulation_auth_required, "missing_user", false);
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("regulations").document(regulationId).get()
                .addOnSuccessListener(doc -> {
                    if (!isCurrentLoad(generation)) return;
                    if (!doc.exists()) {
                        showRegulationError(generation, R.string.regulation_not_found, "not_found", false);
                        return;
                    }
                    nameTv.setText(doc.getString("name"));
                    eligibilityConfigurationValid = configureEligibilityConfirmation(doc.getData());
                    String langCode = LanguageManager.getCurrentLanguageCode(this);
                    doc.getReference().collection(langCode).document("rules").get()
                            .addOnSuccessListener(ruleDoc -> {
                                if (!isCurrentLoad(generation)) return;
                                Object rulesValue = ruleDoc.exists() ? ruleDoc.get("rules") : null;
                                StringBuilder content = new StringBuilder();
                                if (rulesValue instanceof List<?> rules) {
                                    for (Object rule : rules) {
                                        if (rule instanceof String text && !text.trim().isEmpty())
                                            content.append("• ").append(text).append("\n\n");
                                    }
                                }
                                if (content.length() > 0) {
                                    showRegulationContent(generation, content.toString().trim(), "none");
                                } else {
                                    showFallbackOrError(generation, doc.getString("body"), "missing_content");
                                }
                            })
                            .addOnFailureListener(error -> {
                                if (isCurrentLoad(generation))
                                    showFallbackOrError(generation, doc.getString("body"), regulationFailureReason(error));
                            });
                })
                .addOnFailureListener(error -> {
                    if (isCurrentLoad(generation))
                        showRegulationError(generation, R.string.regulation_retry_message,
                                regulationFailureReason(error), false);
                });
    }

    private boolean isCurrentLoad(int generation) {
        return generation == loadGeneration && loadInFlight && !isFinishing() && !isDestroyed();
    }

    private void showFallbackOrError(int generation, String fallback, String failureReason) {
        if (!TextUtils.isEmpty(fallback) && !fallback.trim().isEmpty()) {
            showRegulationContent(generation, fallback, "fallback");
        } else {
            showRegulationError(generation, R.string.regulation_retry_message, failureReason, false);
        }
    }

    private void showRegulationContent(int generation, String content, String reason) {
        if (!isCurrentLoad(generation)) return;
        if (!eligibilityConfigurationValid) {
            showRegulationError(generation, R.string.eligibility_configuration_error,
                    "invalid_configuration", true);
            return;
        }
        loadInFlight = false;
        bodyTv.setText(content);
        regulationContentLoaded = true;
        regulationStatus.setVisibility(View.GONE);
        regulationScroll.setVisibility(View.VISIBLE);
        reportRegulationLoad(true, reason);
        updateAcceptButtonState();
    }

    private void showRegulationError(int generation, int message, String reason, boolean contentDownloaded) {
        if (!isCurrentLoad(generation)) return;
        loadInFlight = false;
        regulationContentLoaded = false;
        regulationScroll.setVisibility(View.GONE);
        eligibilityConfirmation.setVisibility(View.GONE);
        regulationStatus.setVisibility(View.VISIBLE);
        regulationLoading.setVisibility(View.GONE);
        regulationLoadingMessage.setVisibility(View.GONE);
        regulationErrorMessage.setText(message);
        regulationErrorMessage.setVisibility(View.VISIBLE);
        retryRegulation.setVisibility(View.VISIBLE);
        retryRegulation.setEnabled(true);
        reportRegulationLoad(contentDownloaded, reason);
        updateAcceptButtonState();
    }

    private void reportRegulationLoad(boolean loaded, String reason) {
        diagnostics.load(loaded, eligibilityConfigurationValid, requiresEligibilityConfirmation,
                loaded && !eligibilityConfigurationValid ? "invalid_configuration" : reason);
    }

    static String regulationFailureReason(Exception error) {
        if (error instanceof com.google.firebase.FirebaseNetworkException) return "network";
        if (error instanceof com.google.firebase.firestore.FirebaseFirestoreException firestore) {
            return switch (firestore.getCode()) {
                case UNAVAILABLE, DEADLINE_EXCEEDED -> "network";
                case PERMISSION_DENIED, UNAUTHENTICATED -> "permission";
                case NOT_FOUND -> "not_found";
                default -> "unknown";
            };
        }
        return "unknown";
    }

    private void snapshotDiagnosticControls() {
        if (diagnostics == null) return;
        int checked = (confirmMinimumAge.isChecked() ? 1 : 0)
                + (confirmPayoutAccount.isChecked() ? 1 : 0)
                + (confirmTournamentTerms.isChecked() ? 1 : 0);
        diagnostics.controls(checked, acceptBtn.isEnabled());
    }

    private void inspectConfirmationVisibility() {
        if (diagnostics == null || !hasWindowFocus() || isFinishing()
                || eligibilityConfirmation.getVisibility() != View.VISIBLE) return;
        snapshotDiagnosticControls();
        CheckBox[] boxes = {confirmMinimumAge, confirmPayoutAccount, confirmTournamentTerms};
        for (int i = 0; i < boxes.length; i++) {
            Rect visible = new Rect();
            if (boxes[i].isShown() && boxes[i].getGlobalVisibleRect(visible)
                    && visible.width() > 0 && visible.height() > 0) diagnostics.checkboxVisible(i);
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) inspectConfirmationVisibility();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (diagnostics != null) outState.putBundle("regulation_diagnostics", diagnostics.save());
        super.onSaveInstanceState(outState);
    }

    @Override protected void onStop() {
        if (diagnostics != null && isFinishing() && !isChangingConfigurations()) {
            snapshotDiagnosticControls();
            diagnostics.exit(false);
        }
        super.onStop();
    }

    @Override protected void onDestroy() {
        loadGeneration++;
        loadInFlight = false;
        if (eligibilityConfirmation != null && eligibilityConfirmation.getViewTreeObserver().isAlive()) {
            eligibilityConfirmation.getViewTreeObserver().removeOnScrollChangedListener(diagnosticScrollListener);
            eligibilityConfirmation.getViewTreeObserver().removeOnGlobalLayoutListener(diagnosticLayoutListener);
        }
        super.onDestroy();
    }

    @SuppressWarnings("unchecked")
    private boolean configureEligibilityConfirmation(Map<String, Object> regulation) {
        Object prizeRulesValue = regulation == null ? null : regulation.get("prizeRules");
        Map<String, Object> prizeRules = prizeRulesValue instanceof Map
                ? (Map<String, Object>) prizeRulesValue
                : null;
        requiresEligibilityConfirmation = prizeRules != null
                && Boolean.TRUE.equals(prizeRules.get("cashPrizesEnabled"));

        if (!requiresEligibilityConfirmation) {
            eligibilityConfirmation.setVisibility(View.GONE);
            return true;
        }

        Object minimumAgeValue = regulation.get("minimumAge");
        Object payoutMethodsValue = prizeRules.get("payoutMethods");
        if (!(minimumAgeValue instanceof Number)
                || ((Number) minimumAgeValue).intValue() < 1
                || !(payoutMethodsValue instanceof List)
                || ((List<?>) payoutMethodsValue).isEmpty()) {
            eligibilityConfirmation.setVisibility(View.GONE);
            return false;
        }

        int minimumAge = ((Number) minimumAgeValue).intValue();
        List<String> payoutMethods = new ArrayList<>();
        for (Object method : (List<?>) payoutMethodsValue) {
            if (!(method instanceof String) || TextUtils.isEmpty((String) method)) {
                return false;
            }
            if ("BD".equals(regulation.get("market"))
                    && !BangladeshPayoutAccountValidator.isSupported((String) method)) continue;
            payoutMethods.add(formatPayoutMethod((String) method));
        }

        if (payoutMethods.isEmpty()) return false;
        confirmMinimumAge.setText(getString(R.string.eligibility_confirm_age, minimumAge));
        confirmPayoutAccount.setText(getString(
                R.string.eligibility_confirm_payout_account,
                TextUtils.join(", ", payoutMethods)
        ));
        eligibilityConfirmation.setVisibility(View.VISIBLE);
        return true;
    }

    private String formatPayoutMethod(String method) {
        switch (method.toLowerCase(Locale.ROOT)) {
            case "bkash": return "bKash";
            case "rocket": return "Rocket";
            default: return method;
        }
    }

    private void updateAcceptButtonState() {
        eligibilityConfirmationHint.setVisibility(regulationContentLoaded
                && eligibilityConfigurationValid && requiresEligibilityConfirmation
                ? View.VISIBLE : View.GONE);
        boolean confirmationsComplete = !requiresEligibilityConfirmation
                || (confirmMinimumAge.isChecked()
                && confirmPayoutAccount.isChecked()
                && confirmTournamentTerms.isChecked());
        acceptBtn.setEnabled(regulationContentLoaded
                && eligibilityConfigurationValid
                && confirmationsComplete);
        snapshotDiagnosticControls();
        eligibilityConfirmation.post(this::inspectConfirmationVisibility);
    }

    private void acceptAndJoin() {
        if (loadInFlight || !regulationContentLoaded || !eligibilityConfigurationValid) return;
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                ": starting acceptAndJoin");

        if (requiresEligibilityConfirmation
                && (!confirmMinimumAge.isChecked()
                || !confirmPayoutAccount.isChecked()
                || !confirmTournamentTerms.isChecked())) {
            Toast.makeText(this, R.string.eligibility_confirmation_required, Toast.LENGTH_LONG).show();
            return;
        }

        analyticsManager.trackTournamentJoinSubmit();
        diagnostics.submitted();
        acceptBtn.setEnabled(false);
        Toast.makeText(this, R.string.registration_in_progress, Toast.LENGTH_SHORT).show();

        if (TextUtils.isEmpty(tournamentId)) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                    ": empty tournamentId");
            Toast.makeText(this, getString(R.string.tournament_not_found), Toast.LENGTH_LONG).show();
            acceptBtn.setEnabled(true);
            return;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                    ": user not logged-in");
            Toast.makeText(this, getString(R.string.must_be_logged_in), Toast.LENGTH_LONG).show();
            acceptBtn.setEnabled(true);
            return;
        }

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                ": refreshing ID token");
        user.getIdToken(true).addOnSuccessListener(tokenRes -> {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                    ": token refresh OK");
            FirebaseFunctions functions = FirebaseFunctions.getInstance("us-central1");
            Map<String,Object> data = new HashMap<>();
            data.put("tournamentId", tournamentId);
            data.put("regulation", "accepted");
            if (requiresEligibilityConfirmation) {
                Map<String, Object> eligibility = new HashMap<>();
                eligibility.put("ageConfirmed", confirmMinimumAge.isChecked());
                eligibility.put("hasSupportedPayoutAccount", confirmPayoutAccount.isChecked());
                eligibility.put("termsAccepted", confirmTournamentTerms.isChecked());
                data.put("eligibility", eligibility);
            }
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                    ": calling joinTournament");
            functions.getHttpsCallable("joinTournament")
                    .call(data)
                    .addOnSuccessListener(r -> {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                                ": joinTournament success");
                        
                        // Track successful tournament join
                        analyticsManager.trackTournamentJoinSuccess(tournamentId);
                        analyticsManager.addTournamentBreadcrumb("join_success", tournamentId, "regulation_accepted");
                        
                        Toast.makeText(this, getString(R.string.joined_wait_for_bracket), Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        String errorCode = "unknown";
                        String errorMessage = e.getMessage();
                        
                        if (e instanceof FirebaseFunctionsException ffe) {
                            errorCode = ffe.getCode().name();
                            Log.e("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin"
                                    + ": code=" + ffe.getCode()
                                    + "  msg=" + ffe.getMessage()
                                    + "  details=" + ffe.getDetails());
                        }
                        
                        // Track tournament join error
                        analyticsManager.trackTournamentJoinError(tournamentId, errorCode, errorMessage);
                        analyticsManager.addTournamentBreadcrumb("join_error", tournamentId, "join_request_failed");
                        
                        Log.e("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                                ": joinTournament failed", e);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                        acceptBtn.setEnabled(true);
                    });
        }).addOnFailureListener(e -> {
            Log.e("TAG_Soccer", getClass().getSimpleName() + ".acceptAndJoin" +
                    ": token refresh failed", e);
            analyticsManager.trackTournamentJoinError(tournamentId, "token_refresh", null);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            acceptBtn.setEnabled(true);
        });
    }
}
