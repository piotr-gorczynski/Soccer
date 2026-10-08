package piotr_gorczynski.soccer2;

import com.google.android.gms.tasks.Task;
import java.util.function.Supplier;

/** Coalesces duplicate account-boundary cleanup calls; failures remain retryable. */
final class FcmCleanupGate {
    private Task<Void> task;
    synchronized Task<Void> run(Supplier<Task<Void>> cleanup) {
        if (task == null || (task.isComplete() && !task.isSuccessful())) task = cleanup.get();
        return task;
    }
    synchronized Task<Void> current() { return task; }
    synchronized void registrationStarted() {
        if (task != null && !task.isSuccessful()) throw new IllegalStateException("FCM cleanup pending");
        task = null;
    }
}
