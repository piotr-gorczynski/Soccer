package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

public class UniversalLoginActivity extends BaseActivity {

    private FirebaseAuthManager authManager;
    private String storedNickname;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_login);

        Toolbar toolbar = findViewById(R.id.universal_login_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.login);

        authManager = new FirebaseAuthManager(this);

        SharedPreferences prefs =
                getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        storedNickname = prefs.getString("nickname", null);

        Button btnEmail = findViewById(R.id.btnUniversalEmail);
        Button btnGoogle = findViewById(R.id.btnUniversalGoogle);
        Button btnFacebook = findViewById(R.id.btnUniversalFacebook);
        btnFacebook.setVisibility(View.VISIBLE);
        Button btnMicrosoft = findViewById(R.id.btnUniversalMicrosoft);

        btnEmail.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnGoogle.setOnClickListener(v -> handleProviderLogin("google.com"));
        btnFacebook.setOnClickListener(v -> handleProviderLogin("facebook.com"));
        btnMicrosoft.setOnClickListener(v -> handleProviderLogin("microsoft.com"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            finish();
        }
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
                Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() +
                                ": onLoginSuccess"
                );

                SharedPreferences prefs =
                        getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);

                String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get(Source.SERVER)
                        .addOnSuccessListener(doc -> {
                            String nick = doc.getString("nickname");
                            if (nick != null && !nick.isEmpty()) {
                                prefs.edit().putString("nickname", nick).apply();
                            } else {
                                Intent intent = new Intent(UniversalLoginActivity.this, PickNicknameActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                            Toast.makeText(UniversalLoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Log.w(
                                    "TAG_Soccer",
                                    getClass().getSimpleName() + ".onLoginSuccess: failed to verify nickname",
                                    e
                            );
                            String nick = prefs.getString("nickname", null);
                            if (nick == null || nick.isEmpty()) {
                                Intent intent = new Intent(UniversalLoginActivity.this, PickNicknameActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                            }
                            Toast.makeText(UniversalLoginActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                            finish();
                        });
            }

            @Override
            public void onLoginFailure(String message) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                        Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() +
                        ": onLoginFailure: " + message);
                Toast.makeText(UniversalLoginActivity.this, getString(R.string.login_failed, message), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
