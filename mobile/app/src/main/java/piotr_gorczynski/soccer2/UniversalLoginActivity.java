package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import java.util.Arrays;
import java.util.Objects;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

public class UniversalLoginActivity extends BaseActivity {

    private FirebaseAuthManager authManager;
    private String storedNickname;
    private CallbackManager callbackManager;
    private AnalyticsManager analyticsManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_universal_login);

        Toolbar toolbar = findViewById(R.id.universal_login_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.login);

        authManager = new FirebaseAuthManager(this);
        callbackManager = CallbackManager.Factory.create();
        
        // Get analytics manager from SoccerApp
        analyticsManager = ((SoccerApp) getApplicationContext()).getAnalyticsManager();
        
        // Track that login screen was opened
        analyticsManager.trackLoginScreenOpened();

        SharedPreferences prefs =
                getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
        storedNickname = prefs.getString("nickname", null);

        Button btnEmail = findViewById(R.id.btnUniversalEmail);
        Button btnGoogle = findViewById(R.id.btnUniversalGoogle);
        Button btnFacebook = findViewById(R.id.btnUniversalFacebook);
        Button btnMicrosoft = findViewById(R.id.btnUniversalMicrosoft);
        Button btnAnonymous = findViewById(R.id.btnUniversalAnonymous);

        btnEmail.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });

        btnGoogle.setOnClickListener(v -> handleProviderLogin("google.com"));
        btnMicrosoft.setOnClickListener(v -> handleProviderLogin("microsoft.com"));
        btnAnonymous.setOnClickListener(v -> handleAnonymousLogin());

        if (FacebookSdk.isInitialized()) {
            btnFacebook.setVisibility(View.VISIBLE);
            btnFacebook.setOnClickListener(v -> handleProviderLogin("facebook.com"));
        } else {
            Log.w(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".onCreate: Facebook SDK not initialized; hiding Facebook login"
            );
            btnFacebook.setVisibility(View.GONE);
        }
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

        FirebaseAuthManager.LoginCallback callback = createLoginCallback();

        if ("facebook.com".equals(provider)) {
            handleFacebookLogin(callback);
            return;
        }

        authManager.loginWithProvider(this, provider, nickname, callback);
    }

    private void handleFacebookLogin(FirebaseAuthManager.LoginCallback callback) {
        if (!FacebookSdk.isInitialized()) {
            Log.w(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".handleFacebookLogin: Facebook SDK not initialized"
            );
            callback.onLoginFailure("facebook_unavailable");
            return;
        }

        LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList( "public_profile"));
        LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult result) {
                authManager.loginWithFacebookToken(result.getAccessToken().getToken(), storedNickname, callback);
            }

            @Override
            public void onCancel() {
                callback.onLoginFailure("cancelled");
            }

            @Override
            public void onError(FacebookException error) {
                callback.onLoginFailure(error.getMessage());
            }
        });
    }

    private void handleAnonymousLogin() {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".handleAnonymousLogin: Starting anonymous login process");
        
        // Show warning dialog
        new AlertDialog.Builder(this)
                .setTitle(R.string.anonymous_login_warning_title)
                .setMessage(R.string.anonymous_login_warning_message)
                .setPositiveButton(R.string.proceed_anonymous_login, (dialog, which) -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".handleAnonymousLogin: User accepted warning, proceeding with anonymous login");
                    FirebaseAuthManager.LoginCallback callback = createLoginCallback();
                    authManager.loginAnonymously(callback);
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".handleAnonymousLogin: User cancelled anonymous login");
                    
                    // Show signup decline reason dialog to understand why
                    SignupDeclineReasonDialog.show(this, "anonymous_login_cancel", analyticsManager, reason -> {
                        // User has provided feedback, no further action needed
                        Log.d("TAG_Soccer", "User declined anonymous login, reason: " + reason);
                    });
                })
                .show();
    }

    private FirebaseAuthManager.LoginCallback createLoginCallback() {
        return new FirebaseAuthManager.LoginCallback() {
            @Override
            public void onLoginSuccess() {
                Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() +
                                ": onLoginSuccess"
                );

                // Track successful signup/login
                String authMethod = determineAuthMethod();
                analyticsManager.trackSignupSuccess(authMethod);
                analyticsManager.addAuthBreadcrumb("login_success", "method=" + authMethod);

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
                
                // Track login/signup error
                String authMethod = determineAuthMethod();
                analyticsManager.trackSignupError(authMethod, "login_failure", message, "authentication");
                analyticsManager.addAuthBreadcrumb("login_failure", "method=" + authMethod + ", error=" + message);
                
                Toast.makeText(UniversalLoginActivity.this, getString(R.string.login_failed, message), Toast.LENGTH_LONG).show();
            }
        };
    }

    /**
     * Helper method to determine the authentication method used
     */
    private String determineAuthMethod() {
        // This is a simple way to track which method was used
        // In a more sophisticated implementation, we could track this per-call
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            if (auth.getCurrentUser().isAnonymous()) {
                return "anonymous";
            }
            // Check provider data to determine method
            for (var userInfo : auth.getCurrentUser().getProviderData()) {
                String providerId = userInfo.getProviderId();
                if ("google.com".equals(providerId)) return "google";
                if ("facebook.com".equals(providerId)) return "facebook";
                if ("microsoft.com".equals(providerId)) return "microsoft";
                if ("password".equals(providerId)) return "email";
            }
        }
        return "unknown";
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
}
