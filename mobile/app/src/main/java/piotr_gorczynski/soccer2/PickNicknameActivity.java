package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.activity.OnBackPressedCallback;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;

import java.security.SecureRandom;

public class PickNicknameActivity extends BaseActivity {

    private EditText editNickname;
    private Button btnConfirm;
    private Toast nickToast;
    private static final int NICK_MAX = 20;
    private static final String NICKNAME_DRAFT = "nickname_draft";
    private AnalyticsManager analyticsManager;
    private View nicknameCheckProgress;
    private NicknameSubmission submission;
    private boolean nicknameSubmissionInProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("TAG_Soccer","PickNicknameActivity.onCreate entered");
        setContentView(R.layout.activity_pick_nickname);

        // Get analytics manager from SoccerApp
        analyticsManager = ((SoccerApp) getApplicationContext()).getAnalyticsManager();

        editNickname = findViewById(R.id.editNickname);
        btnConfirm = findViewById(R.id.btnConfirmNickname);
        nicknameCheckProgress = findViewById(R.id.nicknameCheckProgress);
        submission = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override public <T extends ViewModel> T create(Class<T> modelClass) {
                return modelClass.cast(new NicknameSubmission(
                        NicknameSubmission.firebaseBackend(), analyticsManager));
            }
        }).get(NicknameSubmission.class);

        initializeNicknameInput();
        initializeNicknameSuggestion(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                // Disable back button to enforce nickname entry
            }
        });
    }

    private void initializeNicknameSuggestion(@Nullable Bundle savedState) {
        // Preserve even an intentionally cleared draft when the screen is recreated.
        String draft = savedState != null && savedState.containsKey(NICKNAME_DRAFT)
                ? savedState.getString(NICKNAME_DRAFT, "") : generateNicknameSuggestion();
        editNickname.setText(draft);
        editNickname.selectAll();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(NICKNAME_DRAFT, editNickname.getText().toString());
        super.onSaveInstanceState(outState);
    }

    static String generateNicknameSuggestion() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder suggestion = new StringBuilder("Player");
        for (int i = 0; i < 6; i++) {
            suggestion.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return suggestion.toString();
    }

    private void initializeNicknameInput() {
        editNickname.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(NICK_MAX),
                NO_LEADING_SPACE
        });
        editNickname.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                updateConfirmButton();
                int len = s.length();
                editNickname.setHint(len + " / " + NICK_MAX);
                    if (len == NICK_MAX) {
                      if (nickToast != null) nickToast.cancel();
                      nickToast = Toast.makeText(PickNicknameActivity.this,
                              R.string.nickname_max_length_reached,
                              Toast.LENGTH_SHORT);
                      nickToast.show();
                  } else if (len < NICK_MAX && nickToast != null) {
                    nickToast.cancel();
                    nickToast = null;
                }
            }
        });

        btnConfirm.setOnClickListener(v -> saveNickname());
        updateConfirmButton();

    }

    private void saveNickname() {
        if (nicknameSubmissionInProgress || (submission != null && submission.hasPendingResult())) return;
        final String nickname = editNickname.getText().toString().trim();
        analyticsManager.trackOnboardingStep("nickname", "submit", "none");
        if (nickname.isEmpty()) {
                analyticsManager.trackOnboardingStep("nickname", "failure", "empty");
            Toast.makeText(this, R.string.nickname_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (nickname.length() > NICK_MAX) {
                analyticsManager.trackOnboardingStep("nickname", "failure", "too_long");
            Toast.makeText(this, R.string.nickname_too_long, Toast.LENGTH_SHORT).show();
            return;
        }

        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        // The model survives Activity recreation and owns all SDK callbacks.
        setNicknameSubmissionInProgress(true);
        showNicknameCheckProgress(true);
        submission.submit(uid, nickname);
    }

    @Override protected void onResume() {
        super.onResume();
        if (submission != null) submission.observe(this::renderSubmission);
    }

    @Override protected void onPause() {
        if (submission != null) submission.observe(null);
        super.onPause();
    }

    private void renderSubmission(NicknameSubmission.Result result) {
        boolean busy = result == NicknameSubmission.Result.BUSY;
        setNicknameSubmissionInProgress(busy);
        showNicknameCheckProgress(busy);
        if (result == NicknameSubmission.Result.IDLE || busy) return;

        // A result from an old account must never update the new account's local session.
        if (!submission.isCurrentAccount()) {
            submission.consumeResult();
            finish();
            return;
        }
        String savedNickname = submission.nickname();
        submission.consumeResult();
        if (result == NicknameSubmission.Result.SAVED) {
            getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE).edit()
                    .putString("nickname", savedNickname).apply();
            FirebaseAuth auth = FirebaseAuth.getInstance();
            if (auth.getCurrentUser() != null) {
                analyticsManager.setUserProperties(
                        auth.getCurrentUser().isAnonymous() ? "anonymous" : "registered",
                        LanguageManager.getCurrentLanguageCode(this), true);
            }
            Toast.makeText(this, R.string.nickname_saved, Toast.LENGTH_SHORT).show();
            if (isTaskRoot()) startActivity(new Intent(this, MenuActivity.class));
            finish();
            return;
        }
        int message = result == NicknameSubmission.Result.TAKEN ? R.string.nickname_taken
                : result == NicknameSubmission.Result.REJECTED ? R.string.nickname_inappropriate
                : result == NicknameSubmission.Result.WRITE_ERROR ? R.string.nickname_save_failed
                : R.string.network_error_checking_nickname;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void updateConfirmButton() {
        btnConfirm.setEnabled(!nicknameSubmissionInProgress
                && !editNickname.getText().toString().trim().isEmpty());
        btnConfirm.setAlpha(btnConfirm.isEnabled() ? 1f : 0.5f);
    }

    private void setNicknameSubmissionInProgress(boolean inProgress) {
        nicknameSubmissionInProgress = inProgress;
        updateConfirmButton();
    }

    private void showNicknameCheckProgress(boolean show) {
        if (nicknameCheckProgress != null) {
            nicknameCheckProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private static final InputFilter NO_LEADING_SPACE = (source, start, end, dest, dstart, dend) -> {
        if (dstart == 0 && start < end && Character.isWhitespace(source.charAt(start))) {
            return "";
        }
        return null;
    };
}
