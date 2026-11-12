package piotr_gorczynski.soccer2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.functions.FirebaseFunctionsException;
import java.util.Objects;

/** Shared foreground/session and service-check boundary for the two support screens. */
abstract class SupportScreenActivity extends BaseActivity {
    protected SupportGateway gateway;
    protected String ownerUid;
    protected TextView statusText;
    protected ProgressBar progress;
    protected Button retry;
    protected final Handler ui = new Handler(Looper.getMainLooper());
    private FirebaseAuth auth;
    private boolean started, serverReady, checkingService;
    private int generation;
    private ConnectivityManager connectivity;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean lastOnline;
    private final FirebaseAuth.AuthStateListener authListener = ignored -> verifyOwner();
    private final Runnable poll = new Runnable() {
        @Override public void run() { if (sessionActive()) { refresh(); ui.postDelayed(this, 30000); } }
    };
    protected SupportGateway createGateway() { return SupportGateway.firebase(); }
    protected FirebaseAuth supportAuth() { return FirebaseAuth.getInstance(); }
    protected void checkService(BackendServiceChecker.ServiceCheckCallback callback) {
        new BackendServiceChecker(this).checkServiceAvailability(callback);
    }
    static boolean deviceOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
    protected boolean isDeviceOnline() { return deviceOnline(this); }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        auth = supportAuth(); ownerUid = auth.getUid();
        if (ownerUid == null || (state != null && !Objects.equals(ownerUid, state.getString("support_owner")))) {
            finish(); return;
        }
        gateway = createGateway();
        setContentView(R.layout.activity_help_centre);
        Toolbar toolbar = findViewById(R.id.help_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        statusText = findViewById(R.id.help_status);
        progress = findViewById(R.id.help_progress);
        retry = findViewById(R.id.help_retry);
        retry.setOnClickListener(view -> { serverReady = false; refresh(); });
    }
    @Override protected void onStart() {
        super.onStart();
        if (isFinishing()) return;
        started = true; generation++; serverReady = false; checkingService = false;
        if (!verifyOwner()) return;
        auth.addAuthStateListener(authListener);
        lastOnline = isDeviceOnline();
        connectivity = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { changed(); }
            @Override public void onLost(Network network) { changed(); }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) { changed(); }
            private void changed() { ui.post(() -> {
                if (!sessionActive()) return;
                boolean online = isDeviceOnline();
                if (online == lastOnline) return;
                lastOnline = online; generation++; serverReady = false; checkingService = false;
                requestsInvalidated();
                if (online) refresh(); else connectionFailed();
            }); }
        };
        try { if (connectivity != null) connectivity.registerDefaultNetworkCallback(networkCallback); }
        catch (RuntimeException ignored) { networkCallback = null; }
        refresh(); ui.postDelayed(poll, 30000);
    }
    @Override protected void onStop() {
        started = false; generation++; checkingService = false; serverReady = false;
        ui.removeCallbacks(poll); if (auth != null) auth.removeAuthStateListener(authListener);
        if (networkCallback != null && connectivity != null) {
            try { connectivity.unregisterNetworkCallback(networkCallback); } catch (RuntimeException ignored) { }
        }
        networkCallback = null; requestsInvalidated(); super.onStop();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("support_owner", ownerUid); super.onSaveInstanceState(state);
    }
    protected final boolean sessionActive() {
        return started && !isFinishing() && !isDestroyed() && Objects.equals(ownerUid, auth.getUid());
    }
    private boolean verifyOwner() {
        if (Objects.equals(ownerUid, auth.getUid())) return true;
        if (ownerUid != null) SupportDraft.clearOwner(this, ownerUid);
        generation++; requestsInvalidated();
        View content = findViewById(android.R.id.content);
        if (content != null) content.setVisibility(View.INVISIBLE);
        finish(); return false;
    }
    protected final int requestGeneration() { return generation; }
    protected final boolean accepts(int token) { return token == generation && sessionActive(); }
    protected final void withBackend(Runnable action) {
        if (!sessionActive()) return;
        if (!isDeviceOnline()) { connectionFailed(); return; }
        if (serverReady) { action.run(); return; }
        if (checkingService) return;
        checkingService = true;
        final int token = generation;
        checkService(new BackendServiceChecker.ServiceCheckCallback() {
            @Override public void onServiceAvailable() { ui.post(() -> {
                if (!accepts(token)) return;
                checkingService = false;
                if (!isDeviceOnline()) { connectionFailed(); return; }
                serverReady = true;
                if (getApplication() instanceof SoccerApp) ((SoccerApp) getApplication()).setBackendAvailable(true);
                action.run();
            }); }
            @Override public void onServiceUnavailable(String ignored) { ui.post(() -> {
                if (!accepts(token)) return;
                checkingService = false; connectionFailed();
            }); }
        });
    }
    protected final void connectionFailed() {
        generation++; serverReady = false; checkingService = false;
        if (getApplication() instanceof SoccerApp) ((SoccerApp) getApplication()).setBackendAvailable(false);
        requestsInvalidated(); showStatus(R.string.server_unavailable_message, false, true);
    }
    protected final void showStatus(int text, boolean loading, boolean allowRetry) {
        if (statusText == null) return;
        statusText.setText(text == 0 ? "" : getString(text));
        statusText.setVisibility(text == 0 ? View.GONE : View.VISIBLE);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        retry.setVisibility(allowRetry ? View.VISIBLE : View.GONE);
    }
    protected static int errorText(Exception error) {
        if (error instanceof FirebaseFunctionsException) {
            switch (((FirebaseFunctionsException) error).getCode()) {
                case RESOURCE_EXHAUSTED: return R.string.help_rate_limit;
                case INVALID_ARGUMENT: return R.string.help_message_invalid;
                case UNAUTHENTICATED: return R.string.help_signin_required;
                case UNAVAILABLE: case DEADLINE_EXCEEDED: return R.string.server_unavailable_message;
                default: break;
            }
        }
        return R.string.help_load_failed;
    }
    protected static boolean definitiveSendFailure(Exception error) {
        if (!(error instanceof FirebaseFunctionsException)) return false;
        switch (((FirebaseFunctionsException) error).getCode()) {
            case INVALID_ARGUMENT: case RESOURCE_EXHAUSTED: case NOT_FOUND:
            case PERMISSION_DENIED: case UNAUTHENTICATED: case FAILED_PRECONDITION: return true;
            default: return false;
        }
    }
    protected abstract void refresh();
    protected abstract void requestsInvalidated();
    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
