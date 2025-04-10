package piotr_gorczynski.soccer2;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Log.e("FirebaseAuth", "Registration failed: " + errorMsg);
                        Toast.makeText(context, "Registration failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

}
