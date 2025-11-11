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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PickNicknameActivity extends BaseActivity {

    private EditText editNickname;
    private Button btnConfirm;
    private Toast nickToast;
    private static final int NICK_MAX = 20;
    private AnalyticsManager analyticsManager;
    private View nicknameCheckProgress;
    private FirebaseFunctions functions;

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
        functions = FirebaseFunctions.getInstance();

        editNickname.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(NICK_MAX),
                NO_LEADING_SPACE
        });
        editNickname.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
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

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                // Disable back button to enforce nickname entry
            }
        });
    }

    private void saveNickname() {
        final String nickname = editNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            Toast.makeText(this, R.string.nickname_required, Toast.LENGTH_SHORT).show();
            return;
        }
        if (nickname.length() > NICK_MAX) {
            Toast.makeText(this, R.string.nickname_too_long, Toast.LENGTH_SHORT).show();
            return;
        }

        final String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, R.string.not_logged_in, Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);
        showNicknameCheckProgress(true);
        
        Log.d("TAG_Soccer", "PickNicknameActivity.saveNickname: Checking uniqueness for nickname: " + nickname);
        
        // Step 1: Check uniqueness first
        checkNicknameUniqueness(nickname, (isUnique, hadError) -> {
            if (hadError) {
                showNicknameCheckProgress(false);
                btnConfirm.setEnabled(true);
                Toast.makeText(PickNicknameActivity.this,
                        R.string.network_error_checking_nickname,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isUnique) {
                showNicknameCheckProgress(false);
                btnConfirm.setEnabled(true);
                Log.d("TAG_Soccer", "PickNicknameActivity.saveNickname: Nickname already taken: " + nickname);
                Toast.makeText(PickNicknameActivity.this,
                        R.string.nickname_taken,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Step 2: Check content appropriateness via AI
            Log.d("TAG_Soccer", "PickNicknameActivity.saveNickname: Nickname is unique, checking content appropriateness via AI for: " + nickname);
            checkNicknameContent(nickname, (allowed, reason, hadContentError) -> {
                showNicknameCheckProgress(false);
                if (hadContentError) {
                    btnConfirm.setEnabled(true);
                    // Report error to Crashlytics
                    analyticsManager.trackNicknameCheckError(nickname, "AI content check failed");
                    Toast.makeText(PickNicknameActivity.this,
                            R.string.network_error_checking_nickname,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!allowed) {
                    btnConfirm.setEnabled(true);
                    Log.d("TAG_Soccer", "PickNicknameActivity.saveNickname: Nickname flagged as inappropriate: " + nickname + ", reason: " + reason);
                    Toast.makeText(PickNicknameActivity.this,
                            R.string.nickname_inappropriate,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Log.d("TAG_Soccer", "PickNicknameActivity.saveNickname: Nickname passed all checks, proceeding to save: " + nickname);
                proceedWithSavingNickname(uid, nickname);
            });
        });
    }


    private void checkNicknameUniqueness(String nickname, UniquenessCheckCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("nicknameLowercase", nickname.toLowerCase())
                .get()
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        Log.e("TAG_Soccer", "PickNicknameActivity.checkNicknameUniqueness: Uniqueness check failed", task.getException());
                        callback.onComplete(false, true);
                        return;
                    }
                    boolean isUnique = task.getResult().isEmpty();
                    callback.onComplete(isUnique, false);
                });
    }

    private void proceedWithSavingNickname(String uid, String nickname) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("nickname", nickname);
        data.put("nicknameLowercase", nickname.toLowerCase());
        db.collection("users").document(uid)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v -> {
                    getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE)
                            .edit()
                            .putString("nickname", nickname)
                            .apply();
                    Toast.makeText(PickNicknameActivity.this,
                            R.string.nickname_saved,
                            Toast.LENGTH_SHORT).show();

                    // Update user properties now that nickname is set
                    FirebaseAuth auth = FirebaseAuth.getInstance();
                    if (auth.getCurrentUser() != null) {
                        String authMethod = auth.getCurrentUser().isAnonymous() ? "anonymous" : "registered";
                        String language = LanguageManager.getCurrentLanguageCode(PickNicknameActivity.this);
                        analyticsManager.setUserProperties(authMethod, "9.0", language, true);
                    }

                    // Navigate to MenuActivity
                    if (isTaskRoot()) {
                        startActivity(new Intent(
                                PickNicknameActivity.this,
                                MenuActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirm.setEnabled(true);
                    Toast.makeText(PickNicknameActivity.this,
                            R.string.nickname_save_failed,
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showNicknameCheckProgress(boolean show) {
        if (nicknameCheckProgress != null) {
            nicknameCheckProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void checkNicknameContent(String nickname, ContentCheckCallback callback) {
        Log.d("TAG_Soccer", "PickNicknameActivity.checkNicknameContent: Calling cloud function for: " + nickname);
        functions
                .getHttpsCallable("checkNickname")
                .call(Collections.singletonMap("nickname", nickname))
                .addOnSuccessListener(this, result -> {
                    Object data = result.getData();
                    if (data instanceof Map) {
                        Map<?, ?> dataMap = (Map<?, ?>) data;
                        Object allowedValue = dataMap.get("allowed");
                        Object reasonValue = dataMap.get("reason");
                        if (allowedValue instanceof Boolean) {
                            String reason = reasonValue != null ? reasonValue.toString() : "";
                            Log.d("TAG_Soccer", "PickNicknameActivity.checkNicknameContent: Response - allowed=" + allowedValue + ", reason=" + reason);
                            callback.onComplete((Boolean) allowedValue, reason, false);
                            return;
                        }
                    }
                    Log.e("TAG_Soccer", "PickNicknameActivity.checkNicknameContent: Invalid response from checkNickname function: " + data);
                    callback.onComplete(false, "", true);
                })
                .addOnFailureListener(this, e -> {
                    Log.e("TAG_Soccer", "PickNicknameActivity.checkNicknameContent: checkNickname callable failed", e);
                    callback.onComplete(false, "", true);
                });
    }

    private interface UniquenessCheckCallback {
        void onComplete(boolean isUnique, boolean hadError);
    }

    private interface ContentCheckCallback {
        void onComplete(boolean allowed, String reason, boolean hadError);
    }


    private static final InputFilter NO_LEADING_SPACE = (source, start, end, dest, dstart, dend) -> {
        if (dstart == 0 && start < end && Character.isWhitespace(source.charAt(start))) {
            return "";
        }
        return null;
    };
}
