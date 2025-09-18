package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import java.util.Objects;

public class LoginActivity extends BaseActivity {

    private EditText editEmail, editPassword;
    private FirebaseAuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.login_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.login);

        authManager = new FirebaseAuthManager(this);

        editEmail = findViewById(R.id.editLoginEmail);
        editPassword = findViewById(R.id.editLoginPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView linkRegister = findViewById(R.id.linkRegister);
        TextView linkResetPassword = findViewById(R.id.linkResetPassword);

        btnLogin.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.enter_email_and_password, Toast.LENGTH_SHORT).show();
                return;
            }
            authManager.loginUser(email, password, new FirebaseAuthManager.LoginCallback() {
                @Override
                public void onLoginSuccess() {
                    Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    finish(); // Closes LoginActivity and returns to MenuActivity
                }

                @Override
                public void onLoginFailure(String message) {
                    Toast.makeText(LoginActivity.this, SafeStringFormatter.safeGetString(LoginActivity.this, R.string.login_failed, message), Toast.LENGTH_LONG).show();
                }
            });
        });

        linkRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterAccountActivity.class);
            startActivity(intent);
        });

        linkResetPassword.setOnClickListener(v -> {
            Log.d("TAG_Soccer", getClass().getSimpleName() + ".linkResetPassword.onClick: Reset password clicked");
            showResetPasswordDialog();
        });
    }

    private void showResetPasswordDialog() {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".showResetPasswordDialog: Showing reset password dialog");
        
        // Create an EditText for email input
        EditText emailInput = new EditText(this);
        emailInput.setHint(getString(R.string.reset_password_email_hint));
        emailInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        // Pre-fill with current email if available
        String currentEmail = editEmail.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            emailInput.setText(currentEmail);
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_password_title))
                .setMessage(getString(R.string.reset_password_message))
                .setView(emailInput)
                .setPositiveButton(getString(R.string.confirm), (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, R.string.enter_email_address, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".showResetPasswordDialog: Attempting password reset for email: " + email);
                    requestPasswordReset(email);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void requestPasswordReset(String email) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".requestPasswordReset: Starting password reset for: " + email);
        
        authManager.resetPassword(email, new FirebaseAuthManager.ResetPasswordCallback() {
            @Override
            public void onResetPasswordSuccess() {
                Log.d("TAG_Soccer", getClass().getSimpleName() + ".requestPasswordReset: Password reset success for: " + email);
                Toast.makeText(LoginActivity.this, getString(R.string.reset_password_success), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResetPasswordFailure(String message) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + ".requestPasswordReset: Password reset failed for: " + email + ", error: " + message);
                Toast.makeText(LoginActivity.this, getString(R.string.reset_password_failed), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
