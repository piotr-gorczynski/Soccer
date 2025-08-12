package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.webkit.WebView;
import android.widget.Toast;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

public class TermsActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        WebView webView = findViewById(R.id.termsWebView);
        String langCode = LanguageManager.getCurrentLanguageCode(this);
        String url = "https://piotr-gorczynski.com/terms-" + langCode + ".html";
        Log.d("TermsActivity", "Loading terms from URL: " + url);
        webView.loadUrl(url);

        Button acceptBtn = findViewById(R.id.acceptTerms);
        Button declineBtn = findViewById(R.id.declineTerms);

        acceptBtn.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) {
                FirebaseAuth.getInstance().signOut();
                finish();
                return;
            }
            Map<String, Object> data = Map.of(
                    "termsAccepted", true,
                    "termsAcceptanceDate", FieldValue.serverTimestamp()
            );
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(r -> finish())
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
        });

        declineBtn.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            finish();
        });
    }
}
