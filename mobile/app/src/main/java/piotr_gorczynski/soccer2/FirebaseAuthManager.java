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

public class FirebaseAuthManager {

    private final FirebaseAuth firebaseAuth;
    private final Context context;

    public FirebaseAuthManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    private void storeUserData(String uid, String email, String nickname) {
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("uid", uid)
                .putString("email", email)
                .putString("nickname", nickname)
                .apply();
    }

    public void loginUser(String email, String password) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (firebaseAuth.getCurrentUser().isEmailVerified()) {
                            String uid = firebaseAuth.getCurrentUser().getUid();
                            String nickname = firebaseAuth.getCurrentUser().getDisplayName(); // Or query Firestore
                            storeUserData(uid, email, nickname);
                            Log.d("FirebaseAuth", "Login successful: " + email);
                            Toast.makeText(context, "Login successful!", Toast.LENGTH_SHORT).show();
                        } else {
                            firebaseAuth.signOut();
                            Toast.makeText(context, "Please verify your email address before logging in.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Log.e("FirebaseAuth", "Login failed: " + task.getException().getMessage());
                        Toast.makeText(context, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }



    public void registerUser(String email, String password, String nickname) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseAuth", "User registered: " + firebaseAuth.getCurrentUser().getEmail());
                        Toast.makeText(context, "Registered as: " + firebaseAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();

                        // Set display name
                        firebaseAuth.getCurrentUser().updateProfile(
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(nickname)
                                        .build()
                        ).addOnCompleteListener(profileTask -> {
                            if (profileTask.isSuccessful()) {
                                Log.d("FirebaseAuth", "Nickname set to: " + nickname);
                            } else {
                                Log.e("FirebaseAuth", "Failed to set nickname: " + profileTask.getException().getMessage());
                            }
                        });

                        // Store nickname in Firestore
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("nickname", nickname);
                        userData.put("email", email); // optional

                        db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("FirebaseAuth", "Nickname saved to Firestore");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("FirebaseAuth", "Failed to save nickname: " + e.getMessage());
                                });


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
                        Log.e("FirebaseAuth", "Registration failed: " + errorMsg);
                        new AlertDialog.Builder(context)
                                .setTitle("Registration Failed")
                                .setMessage(errorMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
    }
}
