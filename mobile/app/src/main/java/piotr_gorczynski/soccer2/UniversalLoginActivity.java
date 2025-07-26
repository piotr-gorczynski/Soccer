package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class UniversalLoginActivity extends AppCompatActivity {

    private FirebaseAuthManager authManager;
    private String storedNickname;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_login);

        authManager = new FirebaseAuthManager(this);

        SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        storedNickname = prefs.getString("nickname", null);

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
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                ": Provider selected = " + provider);

        String nickname = storedNickname; // may be null
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                ": Calling authManager.loginWithProvider");

        authManager.loginWithProvider(this, provider, nickname, new FirebaseAuthManager.LoginCallback() {
            @Override
            public void onLoginSuccess() {
                Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                        Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                        ": onLoginSuccess");
                SharedPreferences prefs = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
                String nick = prefs.getString("nickname", null);
                if (nick == null || nick.isEmpty()) {
                    Intent intent = new Intent(UniversalLoginActivity.this, PickNicknameActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                Toast.makeText(UniversalLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onLoginFailure(String message) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                        Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                        ": onLoginFailure: " + message);
                Toast.makeText(UniversalLoginActivity.this, "Login failed: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
