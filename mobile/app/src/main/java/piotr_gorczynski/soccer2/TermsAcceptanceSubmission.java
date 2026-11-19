package piotr_gorczynski.soccer2;

import androidx.lifecycle.ViewModel;
import com.google.android.gms.tasks.Task;
import java.util.function.Supplier;

/** Keeps one acceptance request across screen recreation without retaining an Activity. */
final class TermsAcceptanceSubmission extends ViewModel {
    enum Result { IDLE, SAVING, SAVED, FAILED }
    interface Observer { void changed(Result result); }
    private Result result=Result.IDLE;
    private Observer observer;
    private String uid, policyId;
    private Exception error;
    private boolean cleared;

    void observe(Observer next) {
        observer=next;
        next.changed(result);
    }
    void unobserve(Observer previous) { if(observer==previous) observer=null; }
    boolean isSaving() { return result==Result.SAVING; }
    boolean hasPendingResult() { return result!=Result.IDLE; }
    boolean belongsTo(String uid,String policyId) {
        return this.uid!=null && this.uid.equals(uid) && this.policyId.equals(policyId);
    }
    Exception error() { return error; }
    void consumeResult() { result=Result.IDLE; error=null; }

    void submit(String uid,String policyId,Supplier<Task<Void>> request) {
        if(cleared || hasPendingResult()) return;
        this.uid=uid; this.policyId=policyId;
        publish(Result.SAVING);
        request.get().addOnCompleteListener(task -> {
            if(cleared) return;
            error=task.getException();
            publish(task.isSuccessful() ? Result.SAVED : Result.FAILED);
        });
    }
    private void publish(Result next) {
        result=next;
        if(observer!=null) observer.changed(next);
    }
    @Override protected void onCleared() { cleared=true; observer=null; error=null; }
}
