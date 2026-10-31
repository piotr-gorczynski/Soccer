package piotr_gorczynski.soccer2;

import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.functions.FirebaseFunctions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Owns the request, never an Activity. A stopped/recreated screen cannot lose callbacks. */
final class NicknameSubmission extends ViewModel {
    interface Backend {
        String currentUid();
        Task<Boolean> unique(String nickname);
        Task<Boolean> allowed(String nickname);
        Task<Void> save(String uid, String nickname);
    }

    enum Result { IDLE, BUSY, SAVED, TAKEN, NETWORK, CONTENT_ERROR, REJECTED, WRITE_ERROR, SESSION_CHANGED }
    interface Observer { void changed(Result result); }

    private final Backend backend;
    private final AnalyticsManager analytics;
    private Observer observer;
    private Result result = Result.IDLE;
    private String uid;
    private String nickname;
    private boolean cleared;

    NicknameSubmission(Backend backend, AnalyticsManager analytics) {
        this.backend = backend;
        this.analytics = analytics;
    }

    static Backend firebaseBackend() {
        return new Backend() {
            public String currentUid() { return FirebaseAuth.getInstance().getUid(); }
            public Task<Boolean> unique(String nickname) {
                return FirebaseFirestore.getInstance().collection("users")
                        .whereEqualTo("nicknameLowercase", nickname.toLowerCase()).get()
                        .continueWith(task -> task.getResult().isEmpty());
            }
            public Task<Boolean> allowed(String nickname) {
                return FirebaseFunctions.getInstance().getHttpsCallable("checkNickname")
                        .call(Collections.singletonMap("nickname", nickname))
                        .continueWith(task -> {
                            Object data = task.getResult().getData();
                            if (data instanceof Map && ((Map<?, ?>) data).get("allowed") instanceof Boolean) {
                                return (Boolean) ((Map<?, ?>) data).get("allowed");
                            }
                            throw new IllegalStateException("Invalid moderation response");
                        });
            }
            public Task<Void> save(String uid, String nickname) {
                Map<String, Object> data = new HashMap<>();
                data.put("nickname", nickname);
                data.put("nicknameLowercase", nickname.toLowerCase());
                return FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(data, SetOptions.merge());
            }
        };
    }

    void observe(Observer next) {
        observer = next;
        if (next != null) next.changed(result);
    }

    boolean hasPendingResult() { return result != Result.IDLE; }
    String nickname() { return nickname; }
    boolean isCurrentAccount() { return uid != null && uid.equals(backend.currentUid()); }

    void consumeResult() {
        result = Result.IDLE;
        nickname = null;
    }

    void submit(String uid, String nickname) {
        if (cleared || result != Result.IDLE) return;
        this.uid = uid;
        this.nickname = nickname;
        publish(Result.BUSY);
        if (!canContinue()) return;
        backend.unique(nickname).addOnCompleteListener(task -> {
            if (!canContinue()) return;
            if (!task.isSuccessful()) {
                failure("uniqueness", "unknown", Result.NETWORK);
            } else if (!Boolean.TRUE.equals(task.getResult())) {
                failure("uniqueness", "taken", Result.TAKEN);
            } else {
                moderate();
            }
        });
    }

    private void moderate() {
        backend.allowed(nickname).addOnCompleteListener(task -> {
            if (!canContinue()) return;
            if (!task.isSuccessful()) {
                // Existing helper sanitizes the payload; never pass entered text or exception messages.
                analytics.trackNicknameCheckError("", "");
                publish(Result.CONTENT_ERROR);
            } else if (!Boolean.TRUE.equals(task.getResult())) {
                failure("content", "rejected", Result.REJECTED);
            } else {
                save();
            }
        });
    }

    private void save() {
        if (!canContinue()) return;
        backend.save(uid, nickname).addOnCompleteListener(task -> {
            if (!canContinue()) return;
            if (!task.isSuccessful()) {
                failure("profile_write", "unknown", Result.WRITE_ERROR);
            } else {
                analytics.trackOnboardingStep("nickname", "success", "none");
                publish(Result.SAVED);
            }
        });
    }

    private boolean canContinue() {
        if (cleared) return false;
        if (isCurrentAccount()) return true;
        publish(Result.SESSION_CHANGED);
        return false;
    }

    private void failure(String stage, String reason, Result result) {
        analytics.trackOnboardingStep(stage, "failure", reason);
        publish(result);
    }

    private void publish(Result value) {
        result = value;
        if (observer != null) observer.changed(value);
    }

    @Override protected void onCleared() {
        cleared = true;
        observer = null;
        nickname = null;
    }
}
