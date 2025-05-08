package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;

import com.google.firebase.auth.UserProfileChangeRequest;

import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FirebaseAuthManager {

    private final FirebaseAuth firebaseAuth;
    private final Context context;

    public FirebaseAuthManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public interface LoginCallback {
        void onLoginSuccess();
        void onLoginFailure(String message);
    }

    private void storeUserData(String uid, String email, String nickname) {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("uid", uid)
                .putString("email", email)
                .putString("nickname", nickname)
                .apply();
    }

    public void loginUser(String email, String password, LoginCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (firebaseAuth.getCurrentUser() != null) {
                            boolean isVerified = firebaseAuth.getCurrentUser().isEmailVerified();
                            String uid = firebaseAuth.getCurrentUser().getUid();
                            Log.d("Soccer2", "User UID: " + uid);
                            Log.d("Soccer2", "Email Verified: " + isVerified);

                            if (isVerified) {
                                String nickname = firebaseAuth.getCurrentUser().getDisplayName();
                                storeUserData(uid, email, nickname);
                                callback.onLoginSuccess();
                            } else {
                                Log.e("Soccer2", "Email is NOT verified, signing out...");
                                firebaseAuth.signOut();
                                callback.onLoginFailure("Please verify your email address before logging in.");
                            }
                        } else {
                            Log.e("Soccer2", "firebaseAuth.getCurrentUser() is NULL after signIn!");
                            callback.onLoginFailure("Authentication failed.");
                        }
                    } else {
                        callback.onLoginFailure(Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }




    public void registerUser(String email, String password, String nickname) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("Soccer2", "User registered: " + Objects.requireNonNull(firebaseAuth.getCurrentUser()).getEmail());
                        Toast.makeText(context, "Registered as: " + firebaseAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();

                        // Set display name
                        firebaseAuth.getCurrentUser().updateProfile(
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(nickname)
                                        .build()
                        ).addOnCompleteListener(profileTask -> {
                            if (profileTask.isSuccessful()) {
                                Log.d("Soccer2", "Nickname set to: " + nickname);
                            } else {
                                Log.e("Soccer2", "Failed to set nickname: " + Objects.requireNonNull(profileTask.getException()).getMessage());
                            }
                        });

                        // Store nickname in Firestore
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("nickname", nickname);
                        userData.put("email", email); // optional

                        db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid ->
                                    Log.d("Soccer2", "Nickname saved to Firestore")
                                )
                                .addOnFailureListener(e ->
                                    Log.e("Soccer2", "Failed to save nickname: " + e.getMessage())
                                );


                        // Send verification email
                        firebaseAuth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        new AlertDialog.Builder(context)
                                                .setTitle("Verification Email Sent")
                                                .setMessage("Please check your email to verify your account.")
                                                .setPositiveButton("OK", (dialog, which) -> {
                                                    // Close the current activity
                                                    ((Activity) context).finish();
                                                })
                                                .show();
                                    } else {
                                        String error = verifyTask.getException() != null
                                                ? verifyTask.getException().getMessage()
                                                : "Unknown error";
                                        new AlertDialog.Builder(context)
                                                .setTitle("Email Verification Failed")
                                                .setMessage("Could not send verification email: " + error)
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                });

                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Log.e("Soccer2", "Registration failed: " + errorMsg);
                        new AlertDialog.Builder(context)
                                .setTitle("Registration Failed")
                                .setMessage(errorMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
    }
}
