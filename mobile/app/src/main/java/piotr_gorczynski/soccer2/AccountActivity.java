package piotr_gorczynski.soccer2;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
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
        TextView methodView = findViewById(R.id.accountMethod);
        Button logoutBtn = findViewById(R.id.btnLogout);
        Button removeAccountBtn = findViewById(R.id.btnRemoveAccount);

        String prefsName = getPackageName() + "_preferences";
        String nickname = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("nickname", "-");
        String email = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("email", "-");
        String method = getSharedPreferences(prefsName, MODE_PRIVATE)
                .getString("method", "-");

        nickView.setText(getString(R.string.nickname_label, nickname));
        emailView.setText(getString(R.string.email_label, email));
        methodView.setText(getString(R.string.login_method_label, method));

        logoutBtn.setOnClickListener(v -> performLogout());
        removeAccountBtn.setOnClickListener(v -> showRemoveAccountDialog());
    }

    private void performLogout() {
        String uid = FirebaseAuth.getInstance().getUid();

        // Attempt to mark the user offline before signing out so permissions work
        if (uid != null) {
            ((SoccerApp) getApplication()).forceUserOffline(uid);
        }

        // Sign out immediately so the UI updates even if network operations fail
        FirebaseAuth.getInstance().signOut();

        finishLogoutUi();
    }

    private void finishLogoutUi() {
        String prefsName = getPackageName() + "_preferences";
        getSharedPreferences(prefsName, MODE_PRIVATE)
                .edit()
                .remove("fcmToken") // ensure FCM token is wiped
                .clear()
                .apply();

        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".finishLogoutUi: \u2705 FCM token deleted");
                    } else {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + ".finishLogoutUi: \u274C Failed to delete FCM token", t.getException());
                    }
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);

        Toast.makeText(this, R.string.logged_out, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void showRemoveAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.remove_account_dialog_title)
                .setMessage(R.string.remove_account_dialog_message)
                .setPositiveButton(R.string.proceed, (dialog, which) -> performAccountRemoval())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void performAccountRemoval() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.remove_account_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();
        
        // Show progress to user
        Toast.makeText(this, R.string.removing_account, Toast.LENGTH_SHORT).show();
        
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Starting account removal for uid=" + uid);

        // Step 1: Remove user from all friends lists
        removeUserFromAllFriendsLists(uid)
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Friends cleanup successful");
                    
                    // Step 2: Update user document in Firestore (remove email, set accountDeleted flag)
                    updateUserDocumentForRemoval(uid)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Firestore update successful");
                                
                                // Step 3: Remove user status from RTDB
                                removeUserStatusFromRTDB(uid)
                                        .addOnSuccessListener(aVoid3 -> {
                                            Log.d("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: RTDB removal successful");
                                            
                                            // Step 4: Delete Firebase Auth account (do this last)
                                            deleteFirebaseAuthAccount(currentUser);
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: RTDB removal failed", e);
                                            // Continue with auth deletion even if RTDB fails since it's less critical
                                            deleteFirebaseAuthAccount(currentUser);
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Firestore update failed", e);
                                Toast.makeText(this, R.string.remove_account_failed, Toast.LENGTH_SHORT).show();
                                // Don't proceed with auth deletion if Firestore update failed
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Friends cleanup failed", e);
                    // Continue with the rest of the process even if friends cleanup fails
                    // This ensures account removal isn't completely blocked by this step
                    updateUserDocumentForRemoval(uid)
                            .addOnSuccessListener(aVoid2 -> {
                                Log.d("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Firestore update successful");
                                removeUserStatusFromRTDB(uid)
                                        .addOnSuccessListener(aVoid3 -> deleteFirebaseAuthAccount(currentUser))
                                        .addOnFailureListener(e2 -> deleteFirebaseAuthAccount(currentUser));
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + ".performAccountRemoval: Firestore update failed", e2);
                                Toast.makeText(this, R.string.remove_account_failed, Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private com.google.android.gms.tasks.Task<Void> removeUserFromAllFriendsLists(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Use a collection group query to find all documents in "friends" subcollections.
        // We can't filter by document ID directly (collection group queries require the full
        // document path when using FieldPath.documentId()), so instead we fetch all friend
        // documents and filter client-side by matching IDs.
        return db.collectionGroup("friends")
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    com.google.firebase.firestore.QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot == null || querySnapshot.isEmpty()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".removeUserFromAllFriendsLists: No friends to remove for uid=" + uid);
                        return com.google.android.gms.tasks.Tasks.forResult(null);
                    }

                    // Create a batch to delete all the friend documents
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    int deleteCount = 0;

                    for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        // Only delete documents whose ID matches the user being removed
                        if (uid.equals(doc.getId())) {
                            // Each matching doc represents users/{someUserId}/friends/{uid}
                            batch.delete(doc.getReference());
                            deleteCount++;

                            Log.d("TAG_Soccer", getClass().getSimpleName() + ".removeUserFromAllFriendsLists: " +
                                    "Removing friendship from " + doc.getReference().getParent().getParent().getId() +
                                    " to " + uid);
                        }
                    }

                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".removeUserFromAllFriendsLists: " +
                            "Removing " + deleteCount + " friendship(s) for uid=" + uid);

                    return batch.commit();
                });
    }

    private com.google.android.gms.tasks.Task<Void> updateUserDocumentForRemoval(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", FieldValue.delete()); // Remove email field completely
        updates.put("nickname", "(Account removed)"); // Update nickname
        updates.put("nicknameLowercase", "(account removed)"); // Update lowercase version if it exists
        updates.put("accountDeleted", true); // Mark account as deleted to prevent future invitations
        
        return db.collection("users").document(uid).update(updates);
    }

    private com.google.android.gms.tasks.Task<Void> removeUserStatusFromRTDB(String uid) {
        DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference("status").child(uid);
        return statusRef.removeValue();
    }

    private void deleteFirebaseAuthAccount(FirebaseUser user) {
        user.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + ".deleteFirebaseAuthAccount: Auth account deleted successfully");
                    
                    // Account removal successful - perform logout cleanup and UI update
                    finishAccountRemoval();
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + ".deleteFirebaseAuthAccount: Auth account deletion failed", e);
                    
                    // Even if auth deletion fails, we should still logout the user since other data was removed
                    finishAccountRemoval();
                });
    }

    private void finishAccountRemoval() {
        // Force user offline in SoccerApp
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            ((SoccerApp) getApplication()).forceUserOffline(uid);
        }

        // Sign out from Firebase Auth
        FirebaseAuth.getInstance().signOut();

        // Clear local preferences
        String prefsName = getPackageName() + "_preferences";
        getSharedPreferences(prefsName, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();

        // Clear FCM token
        FirebaseMessaging.getInstance().deleteToken()
                .addOnCompleteListener(t -> {
                    if (t.isSuccessful()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".finishAccountRemoval: FCM token deleted");
                    } else {
                        Log.w("TAG_Soccer", getClass().getSimpleName() + ".finishAccountRemoval: Failed to delete FCM token", t.getException());
                    }
                });
        FirebaseMessaging.getInstance().setAutoInitEnabled(false);

        // Show success message
        Toast.makeText(this, R.string.account_removed, Toast.LENGTH_SHORT).show();
        
        Log.d("TAG_Soccer", getClass().getSimpleName() + ".finishAccountRemoval: Account removal completed");

        // Return to main menu (finish this activity to go back)
        finish();
    }
}
