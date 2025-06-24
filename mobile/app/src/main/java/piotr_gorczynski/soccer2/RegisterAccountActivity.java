package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.Editable;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class RegisterAccountActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private FirebaseAuthManager authManager;

    private EditText editConfirmPassword;

    private EditText editNickname;
    private static final int NICK_MAX = 20;          // ← single source of truth

    // optional live counter
    private Toast nickToast;                         // keep one toast instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_account);

        authManager = new FirebaseAuthManager(this);

        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword);
        Button btnRegister = findViewById(R.id.btnRegister);
        editConfirmPassword = findViewById(R.id.editConfirmPassword);
        editNickname = findViewById(R.id.editNickname);
        // 1. hard limit (cuts extra keystrokes)
        editNickname.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(NICK_MAX),
                NO_LEADING_SPACE
        });
        // 2. live counter + gentle warning at limit
        editNickname.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c) {}
            @Override public void afterTextChanged(Editable s) {
                int len = s.length();
                // show “12 / 20” as the hint
                editNickname.setHint(len + " / " + NICK_MAX);
                // if they just reached the limit, show a short toast
                if (len == NICK_MAX) {
                    if (nickToast != null) nickToast.cancel();
                    nickToast = Toast.makeText(
                            RegisterAccountActivity.this,
                            "Maximum 20 characters reached",
                            Toast.LENGTH_SHORT);
                    nickToast.show();
                } else if (len < NICK_MAX && nickToast != null) {
                    nickToast.cancel();
                    nickToast = null;
                }
            }
        });
        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String email = editEmail.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        String confirmPassword = editConfirmPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        String nickname = editNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            Toast.makeText(this, "Nickname is required", Toast.LENGTH_SHORT).show();
            return;
        }
        if (nickname.length() > NICK_MAX) {   // server-side / DB safety net
            Toast.makeText(this, "Nickname must be 20 characters or less",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        //------------------------------------------
        //  Firestore: is this nickname already used?
        //------------------------------------------
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        btnRegister.setEnabled(false);               // optional: disable while checking

        db.collection("users")
                .whereEqualTo("nickname", nickname)
                .get()
                .addOnCompleteListener(this, new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        btnRegister.setEnabled(true);  // re-enable either way
                        if (!task.isSuccessful()) {
                           Toast.makeText(RegisterAccountActivity.this,
                                    "Network error while checking nickname",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!task.getResult().isEmpty()) {
                            Toast.makeText(RegisterAccountActivity.this,
                                    "Nickname already taken — choose another",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // 100 % unique → proceed with auth
                        authManager.registerUser(email, password, nickname);
                        Toast.makeText(RegisterAccountActivity.this,
                                "Registration attempt in progress...",
                                Toast.LENGTH_SHORT).show();
                    }
                });
        //------------------------------------------
        //  End of Firestore nickname check
        //------------------------------------------
    }

    private static final InputFilter NO_LEADING_SPACE = (source, start, end,
                                                         dest, dstart, dend) -> {
        // If the user is inserting at position 0 and the first inserted char is a space → reject
        if (dstart == 0 && start < end && Character.isWhitespace(source.charAt(start))) {
            return "";
        }
        return null;   // accept everything else
    };
}
