package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;
import android.util.Log;

import java.util.Objects;

public class AccountActivity extends AppCompatActivity {
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
    }

    private void performLogout() {
        String uid = FirebaseAuth.getInstance().getUid();

        // Sign out immediately so the UI updates even if network operations fail
        FirebaseAuth.getInstance().signOut();

        // Attempt to mark the user offline in the background; no need to wait
        if (uid != null) {
            ((SoccerApp) getApplication()).forceUserOffline(uid);
        }

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

        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
