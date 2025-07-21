package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;

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
        Button logoutBtn = findViewById(R.id.btnLogout);

        String prefsName = getPackageName() + "_preferences";
        String nickname = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("nickname", "-");
        String email = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("email", "-");

        nickView.setText(getString(R.string.nickname_label, nickname));
        emailView.setText(getString(R.string.email_label, email));

        logoutBtn.setOnClickListener(v -> performLogout());
    }

    private void performLogout() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            ((SoccerApp) getApplication()).forceUserOffline(uid).addOnCompleteListener(t -> {
                FirebaseAuth.getInstance().signOut();
                finishLogoutUi();
            });
        } else {
            FirebaseAuth.getInstance().signOut();
            finishLogoutUi();
        }
    }

    private void finishLogoutUi() {
        getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE)
                .edit().clear().apply();
        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
