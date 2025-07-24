package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class UniversalLoginActivity extends AppCompatActivity {

    private EditText editNickname;
    private FirebaseAuthManager authManager;
    private String storedNickname;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_login);

        authManager = new FirebaseAuthManager(this);

        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        storedNickname = prefs.getString("nickname", null);
        if (storedNickname != null && !storedNickname.isEmpty()) {
            // Hide nickname input when a nickname already exists
            editNickname.setVisibility(View.GONE);
        }

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
        String nickname;
        if (storedNickname != null && !storedNickname.isEmpty()) {
            nickname = storedNickname;
        } else {
            nickname = editNickname.getText().toString().trim();
            if (nickname.isEmpty()) {
                Toast.makeText(this, "Nickname is required", Toast.LENGTH_SHORT).show();
                return;
            }
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
