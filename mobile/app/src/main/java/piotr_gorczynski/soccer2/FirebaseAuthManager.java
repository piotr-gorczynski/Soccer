package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.OAuthProvider;

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

    public void loginWithProvider(Activity activity, String providerId, String nickname, LoginCallback callback) {
        OAuthProvider.Builder provider = OAuthProvider.newBuilder(providerId);
        firebaseAuth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    String email = authResult.getUser().getEmail();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("nickname", nickname);
                    if (email != null) {
                        userData.put("email", email);
                    }
                    userData.put("method", providerId);

                    FirebaseFirestore.getInstance().collection("users").document(uid)
                            .set(userData)
                            .addOnCompleteListener(task -> {
                                storeUserData(uid, email != null ? email : "", nickname, providerId);
                                ((SoccerApp) context.getApplicationContext()).syncFcmTokenIfNeeded();
                                callback.onLoginSuccess();
                            });
                })
                .addOnFailureListener(e -> callback.onLoginFailure(e.getMessage()));
    }

    private void storeUserData(String uid, String email, String nickname, String method) {
        context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE)
                .edit()
                .putString("uid", uid)
                .putString("email", email)
                .putString("nickname", nickname)
                .putString("method", method)
                .apply();
    }

    public void loginUser(String email, String password, LoginCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (firebaseAuth.getCurrentUser() != null) {
                            boolean isVerified = firebaseAuth.getCurrentUser().isEmailVerified();
                            String uid = firebaseAuth.getCurrentUser().getUid();
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": User UID: " + uid);
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Email Verified: " + isVerified);

                            if (isVerified) {
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                        .get()
                                        .addOnSuccessListener(doc -> {
                                            String nickname = doc.getString("nickname");
                                            String method = doc.getString("method");
                                            if (method == null) method = "email";
                                            storeUserData(uid, email, nickname != null ? nickname : "Unknown", method);
                                            ((SoccerApp) context.getApplicationContext())
                                                    .syncFcmTokenIfNeeded();   // new helper (see below)
                                            callback.onLoginSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": ⚠️ Failed to load nickname from Firestore", e);
                                            storeUserData(uid, email, "Unknown", "email");
                                            callback.onLoginSuccess();
                                        });
                            } else {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Email is NOT verified, signing out...");
                                firebaseAuth.signOut();
                                callback.onLoginFailure("Please verify your email address before logging in.");
                            }
                        } else {
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": firebaseAuth.getCurrentUser() is NULL after signIn!");
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
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": User registered: " + Objects.requireNonNull(firebaseAuth.getCurrentUser()).getEmail());
                        Toast.makeText(context, "Registered as: " + firebaseAuth.getCurrentUser().getEmail(), Toast.LENGTH_SHORT).show();

                        // Set display name
                        firebaseAuth.getCurrentUser().updateProfile(
                                new UserProfileChangeRequest.Builder()
                                        .setDisplayName(nickname)
                                        .build()
                        ).addOnCompleteListener(profileTask -> {
                            if (profileTask.isSuccessful()) {
                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Nickname set to: " + nickname);
                            } else {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to set nickname: " + Objects.requireNonNull(profileTask.getException()).getMessage());
                            }
                        });

                        // Store nickname in Firestore
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("nickname", nickname);
                        userData.put("email", email); // optional
                        userData.put("online", true); // optional
                        userData.put("method", "email");

                        db.collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid ->
                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Nickname saved to Firestore")
                                )
                                .addOnFailureListener(e ->
                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Failed to save nickname: " + e.getMessage())
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
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object(){}.getClass().getEnclosingMethod()).getName() + ": Registration failed: " + errorMsg);
                        new AlertDialog.Builder(context)
                                .setTitle("Registration Failed")
                                .setMessage(errorMsg)
                                .setPositiveButton("OK", null)
                                .show();
                    }
                });
    }
}
