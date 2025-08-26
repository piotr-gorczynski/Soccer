package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

import java.util.Objects;

public class LinkAccountActivity extends BaseActivity {

    private FirebaseAuthManager authManager;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_link_account);

        Toolbar toolbar = findViewById(R.id.link_account_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        authManager = new FirebaseAuthManager(this);
        callbackManager = CallbackManager.Factory.create();

        Button btnEmail = findViewById(R.id.btnEmailLink);
        Button btnGoogle = findViewById(R.id.btnGoogleLink);
        Button btnMicrosoft = findViewById(R.id.btnMicrosoftLink);
        Button btnFacebook = findViewById(R.id.btnFacebookLink);

        btnEmail.setOnClickListener(v -> showEmailLinkDialog());
        btnGoogle.setOnClickListener(v -> handleProviderLink("google.com"));
        btnMicrosoft.setOnClickListener(v -> handleProviderLink("microsoft.com"));

        if (FacebookSdk.isInitialized()) {
            btnFacebook.setVisibility(android.view.View.VISIBLE);
            btnFacebook.setOnClickListener(v -> handleProviderLink("facebook.com"));
        } else {
            Log.w(
                    "TAG_Soccer",
                    getClass().getSimpleName() + ".onCreate: Facebook SDK not initialized; hiding Facebook link"
            );
            btnFacebook.setVisibility(android.view.View.GONE);
        }
    }

    private void showEmailLinkDialog() {
        // Create a dialog for email and password input
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.link_account_title);
        builder.setMessage(R.string.enter_email_and_password_to_link);

        // Create input fields
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);

        final EditText emailInput = new EditText(this);
        emailInput.setHint(R.string.email);
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        final EditText passwordInput = new EditText(this);
        passwordInput.setHint(R.string.password);
        passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(passwordInput);

        builder.setView(layout);

        builder.setPositiveButton(R.string.link_account_title, (dialog, which) -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.all_fields_required, Toast.LENGTH_SHORT).show();
                return;
            }

            // Link with email credentials
            authManager.linkWithEmailPassword(email, password, createLinkCallback());
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void handleProviderLink(String providerId) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".handleProviderLink: Starting " + providerId + " link process");

        FirebaseAuthManager.LinkCallback callback = createLinkCallback();
        authManager.linkWithProvider(this, providerId, callback);
    }

    private FirebaseAuthManager.LinkCallback createLinkCallback() {
        return new FirebaseAuthManager.LinkCallback() {
            @Override
            public void onLinkSuccess() {
                Log.d(
                        "TAG_Soccer",
                        getClass().getSimpleName() + ".onLinkSuccess: Account linked successfully"
                );

                SharedPreferences prefs = getSharedPreferences(LanguageManager.PREFS_FILE, MODE_PRIVATE);
                String uid = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

                // Fetch updated user data from Firestore
                FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .get(Source.SERVER)
                        .addOnSuccessListener(doc -> {
                            // Update stored preferences with new method
                            String newMethod = doc.getString("method");
                            String email = doc.getString("email");
                            if (newMethod != null) {
                                prefs.edit().putString("method", newMethod).apply();
                            }
                            if (email != null) {
                                prefs.edit().putString("email", email).apply();
                            }

                            Toast.makeText(LinkAccountActivity.this, getString(R.string.link_account_success), Toast.LENGTH_SHORT).show();
                            finish(); // Return to AccountActivity
                        })
                        .addOnFailureListener(e -> {
                            Log.w("TAG_Soccer", getClass().getSimpleName() + ".onLinkSuccess: Failed to fetch updated user data", e);
                            Toast.makeText(LinkAccountActivity.this, getString(R.string.link_account_success), Toast.LENGTH_SHORT).show();
                            finish(); // Return to AccountActivity anyway
                        });
            }

            @Override
            public void onLinkFailure(String message) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + ".onLinkFailure: " + message);
                Toast.makeText(LinkAccountActivity.this, getString(R.string.link_account_failed, message), Toast.LENGTH_LONG).show();
            }
        };
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