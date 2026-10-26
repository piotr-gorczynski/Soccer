package piotr_gorczynski.soccer2;

import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Source;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/** A failed or timed-out server read must never be interpreted as a missing nickname. */
final class NicknameProfileLoader {
    static final long TIMEOUT_MS = 15_000L;
    static final long RETRY_DELAY_MS = 1_000L;
    private final Supplier<Task<DocumentSnapshot>> read;
    private final Handler handler;
    private final TaskCompletionSource<DocumentSnapshot> result = new TaskCompletionSource<>();
    private final Runnable timeout = () -> result.trySetException(new TimeoutException("Profile read timed out"));
    private final Runnable retry = this::attempt;
    private int attempts;

    NicknameProfileLoader(Supplier<Task<DocumentSnapshot>> read, Handler handler) {
        this.read = read;
        this.handler = handler;
    }

    static Task<DocumentSnapshot> load(DocumentReference ref) {
        return new NicknameProfileLoader(() -> ref.get(Source.SERVER),
                new Handler(Looper.getMainLooper())).start();
    }

    Task<DocumentSnapshot> start() {
        handler.postDelayed(timeout, TIMEOUT_MS);
        result.getTask().addOnCompleteListener(task -> {
            handler.removeCallbacks(timeout);
            handler.removeCallbacks(retry);
        });
        attempt();
        return result.getTask();
    }

    private void attempt() {
        if (result.getTask().isComplete()) return;
        attempts++;
        read.get().addOnCompleteListener(task -> {
            if (result.getTask().isComplete()) return;
            if (task.isSuccessful()) {
                result.trySetResult(task.getResult());
            } else if (attempts < 3) {
                handler.postDelayed(retry, RETRY_DELAY_MS);
            } else {
                Exception error = task.getException();
                result.trySetException(error != null ? error : new IllegalStateException("Profile read failed"));
            }
        });
    }
}
