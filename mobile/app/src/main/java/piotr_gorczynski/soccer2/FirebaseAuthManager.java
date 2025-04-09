package piotr_gorczynski.soccer2;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;

public class FirebaseAuthManager {

    private FirebaseAuth firebaseAuth;
    private Context context;

    public FirebaseAuthManager(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public void registerUser(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FirebaseAuth", "User registered: " + firebaseAuth.getCurrentUser().getEmail());
                    } else {
                        Log.e("FirebaseAuth", "Registration failed: " + task.getException().getMessage());
                    }
                });
    }
}
