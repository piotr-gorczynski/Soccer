package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PickNicknameActivity extends AppCompatActivity {

    private EditText editNickname;
    private Button btnConfirm;
    private Toast nickToast;
    private static final int NICK_MAX = 20;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pick_nickname);

        editNickname = findViewById(R.id.editNickname);
        btnConfirm = findViewById(R.id.btnConfirmNickname);

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
                            "Maximum 20 characters reached",
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
        String nickname = editNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            Toast.makeText(this, "Nickname is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nickname.length() > NICK_MAX) {
            Toast.makeText(this, "Nickname must be 20 characters or less", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirm.setEnabled(false);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("nickname", nickname)
                .get()
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        btnConfirm.setEnabled(true);
                        Toast.makeText(PickNicknameActivity.this,
                                "Network error while checking nickname",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!task.getResult().isEmpty()) {
                        btnConfirm.setEnabled(true);
                        Toast.makeText(PickNicknameActivity.this,
                                "Nickname already taken — choose another",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("nickname", nickname);
                    db.collection("users").document(uid)
                            .set(data, SetOptions.merge())
                            .addOnSuccessListener(v -> {
                                getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE)
                                        .edit()
                                        .putString("nickname", nickname)
                                        .apply();
                                Toast.makeText(PickNicknameActivity.this,
                                        "Nickname saved",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnConfirm.setEnabled(true);
                                Toast.makeText(PickNicknameActivity.this,
                                        "Failed to save nickname",
                                        Toast.LENGTH_SHORT).show();
                            });
                });
    }


    private static final InputFilter NO_LEADING_SPACE = (source, start, end, dest, dstart, dend) -> {
        if (dstart == 0 && start < end && Character.isWhitespace(source.charAt(start))) {
            return "";
        }
        return null;
    };
}
