package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editEmail, editPassword;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = new FirebaseAuthManager(this);

        editEmail = findViewById(R.id.editLoginEmail);
        editPassword = findViewById(R.id.editLoginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView linkRegister = findViewById(R.id.linkRegister);

        btnLogin.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }
            authManager.loginUser(email, password, new FirebaseAuthManager.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    finish(); // Closes LoginActivity and returns to MenuActivity
                }

                @Override
                public void onLoginFailure(String message) {
                    Toast.makeText(LoginActivity.this, "Login failed: " + message, Toast.LENGTH_LONG).show();
                }
            });
        });

        linkRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterAccountActivity.class);
            startActivity(intent);
        });
    }
}
