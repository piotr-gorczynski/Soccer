package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.messaging.FirebaseMessaging;
import android.util.Log;

import java.util.HashMap;
import java.util.Objects;

public class AccountActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        Toolbar toolbar = findViewById(R.id.account_toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        TextView nickView = findViewById(R.id.accountNickname);
        TextView emailView = findViewById(R.id.accountEmail);
        TextView methodView = findViewById(R.id.accountMethod);
        Button logoutBtn = findViewById(R.id.btnLogout);
        Button removeAccountBtn = findViewById(R.id.btnRemoveAccount);

        String prefsName = getPackageName() + "_preferences";
        String nickname = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("nickname", "-");
        String email = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("email", "-");
        String method = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("method", "-");

        nickView.setText(getString(R.string.nickname_label, nickname));
        emailView.setText(getString(R.string.email_label, email));
        methodView.setText(getString(R.string.login_method_label, method));

        logoutBtn.setOnClickListener(v -> performLogout());

        // Allow users to remove their account through the in-app option.
        // Tapping the button opens a confirmation dialog and triggers the deletion flow.
        removeAccountBtn.setOnClickListener(v -> showRemoveAccountDialog());
    }

    private void performLogout() {
        String uid = FirebaseAuth.getInstance().getUid();

        // Attempt to mark the user offline before signing out so permissions work
        if (uid != null) {
            ((SoccerApp) getApplication()).forceUserOffline(uid);
        }

        // Sign out immediately so the UI updates even if network operations fail
        FirebaseAuth.getInstance().signOut();

        finishLogoutUi();
    }

    private void finishLogoutUi() {
        String prefsName = getPackageName() + "_preferences";
        getSharedPreferences(prefsName, MODE_PRIVATE)
                .edit()
                .remove("fcmToken") // ensure FCM token is wiped
                .clear()
                .apply();

        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".finishLogoutUi: \u2705 FCM token deleted");
                    } else {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + ".finishLogoutUi: \u274C Failed to delete FCM token", t.getException());
                    }
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);

        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Displays a confirmation dialog before permanently deleting the user's account.
     * This method is invoked when the user selects the built-in "Remove Account" option
     * described in the privacy policy.
     */
    private void showRemoveAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_account_dialog_title)
                .setMessage(R.string.remove_account_dialog_message)
                .setPositiveButton(R.string.proceed, (dialog, which) -> performAccountRemoval())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performAccountRemoval() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.remove_account_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

        // Show progress to user
        Toast.makeText(this, R.string.removing_account, Toast.LENGTH_SHORT).show();

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Starting account removal for uid=" + uid);

        // Call Cloud Function to remove account
        removeAccountBackend(currentUser, uid);
    }

    private void removeAccountBackend(FirebaseUser user, String uid) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".removeAccountBackend: Calling removeAccount Cloud Function");

        FirebaseFunctions.getInstance("us-central1")
                .getHttpsCallable("removeAccount")
                .call(new HashMap<>())
                .addOnSuccessListener(r -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".removeAccountBackend: Function success");
                    finishAccountRemoval(uid);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthRecentLoginRequiredException) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".removeAccountBackend: Recent login required, prompting reauth");
                        promptReauthentication(user, uid);
                    } else {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + ".removeAccountBackend: Function failed", e);
                        Toast.makeText(this, R.string.remove_account_failed, Toast.LENGTH_SHORT).show();
                        finishAccountRemoval(uid);
                    }
                });
    }

    private void promptReauthentication(FirebaseUser user, String uid) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".promptReauthentication: Showing reauth dialog");

        EditText passwordInput = new EditText(this);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.reauth_title)
                .setMessage(R.string.reauth_message)
                .setView(passwordInput)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    AuthCredential cred = EmailAuthProvider.getCredential(Objects.requireNonNull(user.getEmail()), password);
                    user.reauthenticate(cred)
                            .addOnSuccessListener(v -> {
                                Log.d("TAG_Soccer", getClass().getSimpleName() + ".promptReauthentication: Re-auth successful, retrying deletion");
                                removeAccountBackend(user, uid);
                            })
                            .addOnFailureListener(err -> {
                                Toast.makeText(this, R.string.reauth_failed, Toast.LENGTH_SHORT).show();
                                Log.e("TAG_Soccer", getClass().getSimpleName() + ".promptReauthentication: Re-auth failed", err);
                            });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void finishAccountRemoval(String uid) {
        // Force user offline in SoccerApp using known UID since auth is already cleared
        ((SoccerApp) getApplication()).forceUserOffline(uid);

        // Sign out from Firebase Auth
        FirebaseAuth.getInstance().signOut();

        // Clear local preferences
        String prefsName = getPackageName() + "_preferences";
        getSharedPreferences(prefsName, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Clear FCM token
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".finishAccountRemoval: FCM token deleted");
                    } else {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + ".finishAccountRemoval: Failed to delete FCM token", t.getException());
                    }
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);

        // Show success message
        Toast.makeText(this, R.string.account_removed, Toast.LENGTH_SHORT).show();

        Log.d("TAG_Soccer", getClass().getSimpleName() + ".finishAccountRemoval: Account removal completed");

        // Return to main menu (finish this activity to go back)
        finish();
    }
}
