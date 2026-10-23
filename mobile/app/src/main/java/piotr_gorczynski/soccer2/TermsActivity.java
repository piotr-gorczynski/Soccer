package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.webkit.*;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TermsActivity extends BaseActivity {
    public static final String READ_ONLY = "terms_read_only";
    private boolean readOnly, accepted, loaded, saving;
    private String uid;
    private AnalyticsManager analytics;
    private TermsPolicy policy;
    private Button accept;
    private java.util.concurrent.ExecutorService executor;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_terms);
        PrivacyPolicyLinks.bind(this,R.id.termsPrivacyLink);
        analytics=resolveAnalytics();
        uid=currentUid(); readOnly=getIntent().getBooleanExtra(READ_ONLY,false);
        policy=TermsPolicy.required(AppFlavourDetector.isBangladeshFlavour(this),LanguageManager.getCurrentLanguageCode(this));
        accept=findViewById(R.id.acceptTerms); accept.setEnabled(false);
        if(readOnly) accept.setVisibility(android.view.View.GONE);
        WebView web=findViewById(R.id.termsWebView);
        web.getSettings().setJavaScriptEnabled(false);
        web.getSettings().setAllowFileAccess(false);
        web.getSettings().setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){ if(loaded && !saving) accept.setEnabled(true); }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                String url=request.getUrl().toString();
                if (AppFlavourDetector.isBangladeshFlavour(TermsActivity.this) && PrivacyPolicyLinks.isSharedPrivacyUrl(url)) url=PrivacyPolicyLinks.bangladeshUrl(LanguageManager.getCurrentLanguageCode(TermsActivity.this));
                if(url.startsWith("https://") || url.startsWith("mailto:")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url))); } catch(Exception ignored) {}
                }
                return true; // Never replace the exact HTML being accepted with linked content.
            }
        });
        executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        Button retry=new Button(this);retry.setText(R.string.retry);
        ((android.view.ViewGroup)web.getParent()).addView(retry,0);
        Runnable load=()->{
            loaded=false; accept.setEnabled(false);
            retry.setVisibility(android.view.View.GONE);retry.setEnabled(false);
            loadDocument(policy, html -> {
                if(isFinishing()||isDestroyed()) return;
                trackTerms("terms_load", "success", "none");
                loaded=true;
                web.loadDataWithBaseURL(policy.url,html,"text/html","UTF-8",policy.url);
            }, error -> {
                logFailure("loadDocument", error);
                if(!isFinishing()&&!isDestroyed()) {
                    retry.setVisibility(android.view.View.VISIBLE);retry.setEnabled(true);
                    Toast.makeText(this,R.string.failed_to_load_terms,Toast.LENGTH_LONG).show();
                }
            });
        };
        retry.setOnClickListener(v->load.run());
        accept.setOnClickListener(v->{
            if(readOnly||!loaded||saving||uid==null||!uid.equals(currentUid()))return;
            trackTerms("terms_save", "submit", "none");
            saving=true;accept.setEnabled(false);retry.setEnabled(false);
            saveAcceptance(uid,policy)
                .addOnSuccessListener(unused->{
                    if(!uid.equals(currentUid())){finish();return;}
                    trackTerms("terms_save", "success", "none");
                    accepted=true;finish();
                }).addOnFailureListener(error->{saving=false;accept.setEnabled(loaded);retry.setEnabled(true);
                    logFailure("saveAcceptance", error);
                    Toast.makeText(this,R.string.terms_acceptance_failed,Toast.LENGTH_LONG).show();});
        });
        if(readOnly) ((Button)findViewById(R.id.declineTerms)).setText(R.string.close);
        findViewById(R.id.declineTerms).setOnClickListener(v->leave());
        getOnBackPressedDispatcher().addCallback(this,new androidx.activity.OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){leave();}
        });
        load.run();
    }
    protected String currentUid() { return FirebaseAuth.getInstance().getUid(); }
    protected AnalyticsManager resolveAnalytics() {
        return getApplication() instanceof SoccerApp app ? app.getAnalyticsManager() : null;
    }
    private void logFailure(String stage, Exception error) {
        trackTerms("loadDocument".equals(stage) ? "terms_load" : "terms_save", "failure",
                error instanceof java.net.SocketTimeoutException
                    || error instanceof java.net.UnknownHostException
                    || error instanceof java.net.ConnectException ? "network" : "unknown");
        String code = error instanceof com.google.firebase.firestore.FirebaseFirestoreException
            ? ((com.google.firebase.firestore.FirebaseFirestoreException) error).getCode().name() : "N/A";
        // Exception messages/stack traces can contain account or document identifiers.
        android.util.Log.e("TAG_Soccer", "TermsActivity." + stage + " failed: scope=" + policy.scope
            + ", version=" + policy.version + ", language=" + policy.language
            + ", errorType=" + error.getClass().getSimpleName() + ", firestoreCode=" + code);
    }
    protected com.google.android.gms.tasks.Task<Void> saveAcceptance(String uid, TermsPolicy policy) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        android.util.Log.d("TAG_Soccer", "TermsActivity.saveAcceptance: project="
            + db.getApp().getOptions().getProjectId() + ", version=" + policy.version
            + ", language=" + policy.language);
        return new TermsRepository(db).accept(uid,policy);
    }
    protected void loadDocument(TermsPolicy policy, java.util.function.Consumer<String> success,
                                java.util.function.Consumer<Exception> failure) {
        executor.execute(() -> {
            try {
                String html=TermsDocumentLoader.load(policy);
                runOnUiThread(() -> success.accept(html));
            } catch(Exception error) { runOnUiThread(() -> failure.accept(error)); }
        });
    }
    private void trackTerms(String stage, String outcome, String reason) {
        if (!readOnly && analytics != null) analytics.trackOnboardingStep(stage, outcome, reason);
    }
    private void leave(){if(!readOnly&&!accepted)trackTerms("terms_save","declined","none");if(!readOnly&&!accepted&&uid!=null&&uid.equals(currentUid()))FirebaseAuth.getInstance().signOut();finish();}
    @Override protected void onDestroy(){if(executor!=null)executor.shutdownNow();super.onDestroy();}
}
