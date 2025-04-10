package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;

public class FirebaseAuthManager {

    private final FirebaseAuth firebaseAuth;
    private final Context context;

    public FirebaseAuthManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public void registerUser(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseAuth", "User registered: " + firebaseAuth.getCurrentUser().getEmail());
                        Toast.makeText(context, "Registered as: " + firebaseAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();

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
