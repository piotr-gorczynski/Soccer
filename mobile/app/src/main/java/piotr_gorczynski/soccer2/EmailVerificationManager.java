package piotr_gorczynski.soccer2;

import android.content.SharedPreferences;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.Collections;

/** Keeps a linked account usable while its email is being verified. */
final class EmailVerificationManager {
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final SharedPreferences prefs;
    EmailVerificationManager(FirebaseAuth auth, FirebaseFirestore db, SharedPreferences prefs) {
        this.auth=auth; this.db=db; this.prefs=prefs;
    }
    static boolean needsVerification(FirebaseUser user) {
        return user != null && !user.isAnonymous() && !user.isEmailVerified()
                && user.getProviderData().stream().anyMatch(p -> "password".equals(p.getProviderId()));
    }
    private boolean active(FirebaseUser user) {
        return auth.getCurrentUser()!=null && user.getUid().equals(auth.getCurrentUser().getUid());
    }
    private String key(FirebaseUser user) { return "verification_target_"+user.getUid(); }
    String target(FirebaseUser user) { return prefs.getString(key(user),user.getEmail()); }
    boolean needsAttention(FirebaseUser user) {
        if (user == null) return false;
        String pending = prefs.getString(key(user), null);
        return needsVerification(user) || (pending != null
                && (!user.isEmailVerified() || !pending.equalsIgnoreCase(user.getEmail())));
    }
    boolean reconcileConfirmedAddress(FirebaseUser user) {
        // A fresh sign-in already supplies authoritative verification state.
        // Do not ask the user to check the same confirmed address again.
        if (user == null || !active(user) || !user.isEmailVerified()) return false;
        String pending = prefs.getString(key(user), null);
        if (pending == null || !pending.equalsIgnoreCase(user.getEmail())) return false;
        prefs.edit().remove(key(user)).putString("email", user.getEmail()).apply();
        return true;
    }
    Task<Void> resend(FirebaseUser user) {
        if(!active(user)) return Tasks.forException(new IllegalStateException("Session changed"));
        String pending=prefs.getString(key(user),null);
        return pending == null ? user.sendEmailVerification() : user.verifyBeforeUpdateEmail(pending);
    }
    Task<Void> correctEmail(FirebaseUser user,String email) {
        if(!active(user)) return Tasks.forException(new IllegalStateException("Session changed"));
        boolean currentAddress=email.equalsIgnoreCase(user.getEmail());
        Task<Void> delivery=currentAddress ? user.sendEmailVerification() : user.verifyBeforeUpdateEmail(email);
        return delivery.continueWithTask(task -> {
            if(!task.isSuccessful()) return Tasks.forException(task.getException());
            if(!active(user)) return Tasks.forException(new IllegalStateException("Session changed"));
            // The Auth address changes only when the new mailbox is verified.
            if(currentAddress) prefs.edit().remove(key(user)).apply();
            else prefs.edit().putString(key(user),email).apply();
            return Tasks.forResult(null);
        });
    }
    Task<Boolean> check(FirebaseUser user) {
        if(!active(user)) return Tasks.forException(new IllegalStateException("Session changed"));
        return user.reload().continueWithTask(task -> {
            if(!task.isSuccessful()) return Tasks.forException(task.getException());
            if(!active(user)) return Tasks.forException(new IllegalStateException("Session changed"));
            String pending=prefs.getString(key(user),null);
            if(!user.isEmailVerified() || (pending!=null && !pending.equalsIgnoreCase(user.getEmail())))
                return Tasks.forResult(false);
            return user.getIdToken(true).continueWithTask(token -> {
                if(!token.isSuccessful()) return Tasks.forException(token.getException());
                if(!active(user)) return Tasks.forException(new IllegalStateException("Session changed"));
                return db.collection("users").document(user.getUid())
                        .set(Collections.singletonMap("email",user.getEmail()),SetOptions.merge())
                        .continueWithTask(write -> {
                            if(!write.isSuccessful()) return Tasks.forException(write.getException());
                            if(!active(user)) return Tasks.forException(new IllegalStateException("Session changed"));
                            prefs.edit().remove(key(user)).putString("email",user.getEmail()).apply();
                            return Tasks.forResult(true);
                        });
            });
        });
    }
}
