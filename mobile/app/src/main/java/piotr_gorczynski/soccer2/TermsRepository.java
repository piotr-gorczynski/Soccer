package piotr_gorczynski.soccer2;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.*;
import java.util.*;

/** Server-confirmed checks only: never grant access from pending/offline writes. */
public final class TermsRepository {
    private final FirebaseFirestore db;
    public TermsRepository(FirebaseFirestore db) { this.db = db; }
    public Task<Boolean> accepted(String uid, TermsPolicy policy) {
        DocumentReference user = db.collection("users").document(uid);
        return user.get(Source.SERVER).continueWithTask(task -> {
            DocumentSnapshot profile = task.getResult();
            return user.collection("legalAcceptances").document(policy.id()).get(Source.SERVER)
                .continueWith(record -> policy.accepts(profile.getData(), record.getResult().getData()));
        });
    }
    public Task<Void> accept(String uid, TermsPolicy policy) {
        DocumentReference user = db.collection("users").document(uid);
        DocumentReference ref = user.collection("legalAcceptances").document(policy.id());
        return db.runTransaction(tx -> {
            DocumentSnapshot existing = tx.get(ref);
            DocumentSnapshot profile = tx.get(user);
            if (existing.exists()) {
                if (!policy.matches(existing.getData())) throw new IllegalStateException("Invalid existing acceptance");
                return null; // Idempotent: never rewrite timestamp or metadata.
            }
            tx.set(ref, policy.newRecord());
            Map<String,Object> legacy = new HashMap<>();
            legacy.put("termsAccepted",true);
            legacy.put("termsAcceptanceDate",FieldValue.serverTimestamp());
            // Document language belongs to the evidence; preserve the profile notification language.
            String profileLanguage = profile.getString("language");
            legacy.put("language",profileLanguage == null ? policy.language : profileLanguage);
            legacy.put("termsAcceptanceModel",2);
            boolean oldGlobal = Boolean.TRUE.equals(profile.getBoolean("legacyGlobalTermsAccepted"))
                || (!profile.contains("termsAcceptanceModel") && Boolean.TRUE.equals(profile.getBoolean("termsAccepted")));
            legacy.put("legacyGlobalTermsAccepted",oldGlobal);
            tx.set(user, legacy, SetOptions.merge());
            return null;
        });
    }
}
