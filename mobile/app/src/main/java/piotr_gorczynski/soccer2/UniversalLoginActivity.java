package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class UniversalLoginActivity extends AppCompatActivity {

    private EditText editNickname;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_login);

        authManager = new FirebaseAuthManager(this);
        editNickname = findViewById(R.id.editUniversalNickname);

        Button btnEmail = findViewById(R.id.btnUniversalEmail);
        Button btnGoogle = findViewById(R.id.btnUniversalGoogle);
        Button btnFacebook = findViewById(R.id.btnUniversalFacebook);
        Button btnMicrosoft = findViewById(R.id.btnUniversalMicrosoft);

        btnEmail.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnGoogle.setOnClickListener(v -> handleProviderLogin("google.com"));
        btnFacebook.setOnClickListener(v -> handleProviderLogin("facebook.com"));
        btnMicrosoft.setOnClickListener(v -> handleProviderLogin("microsoft.com"));
    }

    private void handleProviderLogin(String provider) {
        String nickname = editNickname.getText().toString().trim();
        if (nickname.isEmpty()) {
            Toast.makeText(this, "Nickname is required", Toast.LENGTH_SHORT).show();
            return;
        }
        authManager.loginWithProvider(this, provider, nickname, new FirebaseAuthManager.LoginCallback() {
            @Override
            public void onLoginSuccess() {
                Toast.makeText(UniversalLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onLoginFailure(String message) {
                Toast.makeText(UniversalLoginActivity.this, "Login failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
