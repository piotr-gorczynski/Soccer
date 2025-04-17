package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterAccountActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private FirebaseAuthManager authManager;

    private EditText editConfirmPassword;

    private EditText editNickname;

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

        authManager.registerUser(email, password, nickname);

        Toast.makeText(this, "Registration attempt in progress...", Toast.LENGTH_SHORT).show();
    }
}
