package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.OAuthProvider;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FacebookAuthProvider;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import android.content.SharedPreferences;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import org.json.JSONObject;

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

    public interface ResetPasswordCallback {
        void onResetPasswordSuccess();

        void onResetPasswordFailure(String message);
    }

    public void loginWithProvider(Activity activity, String providerId, @Nullable String nickname, LoginCallback callback) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() +
                ": Starting provider login. providerId=" + providerId + ", nickname=" + nickname);
        OAuthProvider.Builder provider = OAuthProvider.newBuilder(providerId);
        firebaseAuth.startActivityForSignInWithProvider(activity, provider.build())
                .addOnSuccessListener(authResult -> {
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                            Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() +
                            ": signInWithProvider success");
                    String uid = authResult.getUser().getUid();
                    String email = authResult.getUser().getEmail();
                    
                    // Extract Facebook-specific data if this is a Facebook login
                    String facebookId = null;
                    String facebookName = null;
                    String facebookPhotoUrl = null;
                    
                    if ("facebook.com".equals(providerId) && authResult.getUser() != null) {
                        for (com.google.firebase.auth.UserInfo profile : authResult.getUser().getProviderData()) {
                            if ("facebook.com".equals(profile.getProviderId())) {
                                facebookId = profile.getUid();
                                facebookName = profile.getDisplayName();
                                if (profile.getPhotoUrl() != null) {
                                    facebookPhotoUrl = profile.getPhotoUrl().toString();
                                }
                                break;
                            }
                        }
                    }
                    
                    // Make Facebook data final for lambda capture
                    final String finalFacebookId = facebookId;
                    final String finalFacebookName = facebookName;
                    final String finalFacebookPhotoUrl = facebookPhotoUrl;

                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    db.collection("users").document(uid).get(Source.SERVER)
                            .addOnSuccessListener(doc -> {
                                String existingNick = doc.getString("nickname");
                                Map<String, Object> data = new HashMap<>();
                                if (email != null) data.put("email", email);
                                data.put("method", providerId);
                                String langCode = LanguageManager.getCurrentLanguageCode(context);
                                data.put("language", langCode);
                                
                                // Add Facebook-specific data if this is a Facebook login
                                if ("facebook.com".equals(providerId)) {
                                    if (finalFacebookId != null) data.put("facebookId", finalFacebookId);
                                    if (finalFacebookName != null) data.put("facebookName", finalFacebookName);
                                    if (finalFacebookPhotoUrl != null) data.put("facebookPhotoUrl", finalFacebookPhotoUrl);
                                }

                                String nicknameToStore;
                                if (existingNick == null || existingNick.isEmpty()) {
                                    if (nickname != null && !nickname.isEmpty()) {
                                        data.put("nickname", nickname);
                                        data.put("nicknameLowercase", nickname.toLowerCase());
                                        nicknameToStore = nickname;
                                    } else {
                                        nicknameToStore = null;
                                    }
                                } else {
                                    nicknameToStore = existingNick;
                                }
                                final String finalNickname = nicknameToStore;

                                if (doc.exists()) {
                                    db.collection("users").document(uid).set(data, SetOptions.merge())
                                            .addOnCompleteListener(task -> {
                                                storeUserData(uid, email != null ? email : "", finalNickname != null ? finalNickname : "", providerId, finalFacebookId, finalFacebookName, finalFacebookPhotoUrl);
                                                ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                        Objects.requireNonNull(new Object() {
                                                        }.getClass().getEnclosingMethod()).getName() +
                                                        ": login success uid=" + uid + ", nickname=" +
                                                        (finalNickname != null ? finalNickname : "null"));
                                                callback.onLoginSuccess();
                                            });
                                } else {
                                    // Create-or-update without wiping existing fields like `nickname`
                                    db.collection("users").document(uid).set(data, SetOptions.merge())
                                            .addOnCompleteListener(task -> {
                                                storeUserData(uid, email != null ? email : "", finalNickname != null ? finalNickname : "", providerId, finalFacebookId, finalFacebookName, finalFacebookPhotoUrl);
                                                ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                                                Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                        Objects.requireNonNull(new Object() {
                                                        }.getClass().getEnclosingMethod()).getName() +
                                                        ": login success uid=" + uid + ", nickname=" +
                                                        (finalNickname != null ? finalNickname : "null"));
                                                callback.onLoginSuccess();
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                        Objects.requireNonNull(new Object() {
                                        }.getClass().getEnclosingMethod()).getName() +
                                        ": failed to read user data", e);
                                callback.onLoginFailure(e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                            Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() +
                            ": signInWithProvider FAILED", e);
                    callback.onLoginFailure(e.getMessage());
                });
    }

    /**
     * Fetch the real Facebook profile photo URL using the access token
     * @param facebookId The Facebook user ID
     * @param accessToken The Facebook access token
     * @return CompletableFuture<String> The real photo URL or null if failed
     */
    private CompletableFuture<String> fetchRealFacebookPhotoUrl(String facebookId, String accessToken) {
        return CompletableFuture.supplyAsync(() -> {
            if (facebookId == null || accessToken == null) {
                Log.w("TAG_Soccer", getClass().getSimpleName() + ".fetchRealFacebookPhotoUrl: Missing facebookId or accessToken");
                return null;
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build();

            String url = "https://graph.facebook.com/v20.0/" + facebookId + "/picture"
                    + "?type=large&redirect=0&access_token=" + accessToken;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    
                    if (jsonResponse.has("data")) {
                        JSONObject data = jsonResponse.getJSONObject("data");
                        if (data.has("url") && data.has("is_silhouette")) {
                            boolean isSilhouette = data.getBoolean("is_silhouette");
                            String photoUrl = data.getString("url");
                            
                            if (!isSilhouette && photoUrl != null && !photoUrl.isEmpty()) {
                                Log.d("TAG_Soccer", getClass().getSimpleName() + ".fetchRealFacebookPhotoUrl: Got real photo URL: " + photoUrl);
                                return photoUrl;
                            } else {
                                Log.w("TAG_Soccer", getClass().getSimpleName() + ".fetchRealFacebookPhotoUrl: Photo is silhouette or URL is empty");
                            }
                        }
                    }
                } else {
                    Log.w("TAG_Soccer", getClass().getSimpleName() + ".fetchRealFacebookPhotoUrl: HTTP error: " + response.code());
                }
            } catch (Exception e) {
                Log.e("TAG_Soccer", getClass().getSimpleName() + ".fetchRealFacebookPhotoUrl: Failed to fetch real photo URL", e);
            }
            
            // Return fallback URL if the Graph API call failed
            return "https://graph.facebook.com/" + facebookId + "/picture";
        });
    }

    public void loginWithFacebookToken(String token, @Nullable String nickname, LoginCallback callback) {
        AuthCredential credential = FacebookAuthProvider.getCredential(token);
        firebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    String providerId = "facebook.com";
                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                            Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() +
                            ": signInWithCredential success");
                    String uid = authResult.getUser().getUid();
                    String email = authResult.getUser().getEmail();
                    
                    // Extract Facebook-specific data from provider info
                    String facebookId = null;
                    String facebookName = null;
                    String facebookPhotoUrl = null;
                    
                    if (authResult.getUser() != null) {
                        for (com.google.firebase.auth.UserInfo profile : authResult.getUser().getProviderData()) {
                            if ("facebook.com".equals(profile.getProviderId())) {
                                facebookId = profile.getUid();
                                facebookName = profile.getDisplayName();
                                // Don't use profile.getPhotoUrl() - it returns the generic URL
                                // We'll fetch the real URL using the access token below
                                break;
                            }
                        }
                    }
                    
                    // Make Facebook data final for lambda capture
                    final String finalFacebookId = facebookId;
                    final String finalFacebookName = facebookName;
                    
                    // Fetch real Facebook photo URL using access token
                    CompletableFuture<String> photoUrlFuture = (finalFacebookId != null) 
                            ? fetchRealFacebookPhotoUrl(finalFacebookId, token)
                            : CompletableFuture.completedFuture(null);
                    
                    photoUrlFuture.thenAccept(realPhotoUrl -> {
                        final String finalFacebookPhotoUrl = realPhotoUrl;
                        
                        // Log extracted Facebook data for debugging
                        Log.d("TAG_Soccer", getClass().getSimpleName() + ".loginWithFacebookToken: extracted Facebook data - ID: " 
                                + (finalFacebookId != null ? finalFacebookId : "null") 
                                + ", Name: " + (finalFacebookName != null ? finalFacebookName : "null") 
                                + ", Photo: " + (finalFacebookPhotoUrl != null ? finalFacebookPhotoUrl : "null"));

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(uid).get(Source.SERVER)
                                .addOnSuccessListener(doc -> {
                                    String existingNick = doc.getString("nickname");
                                    Map<String, Object> data = new HashMap<>();
                                    if (email != null) data.put("email", email);
                                    data.put("method", providerId);
                                    String langCode = LanguageManager.getCurrentLanguageCode(context);
                                    data.put("language", langCode);
                                    
                                    // Add Facebook-specific data
                                    if (finalFacebookId != null) data.put("facebookId", finalFacebookId);
                                    if (finalFacebookName != null) data.put("facebookName", finalFacebookName);
                                    if (finalFacebookPhotoUrl != null) data.put("facebookPhotoUrl", finalFacebookPhotoUrl);

                                    String nicknameToStore;
                                    if (existingNick == null || existingNick.isEmpty()) {
                                        if (nickname != null && !nickname.isEmpty()) {
                                            data.put("nickname", nickname);
                                            data.put("nicknameLowercase", nickname.toLowerCase());
                                            nicknameToStore = nickname;
                                        } else {
                                            nicknameToStore = null;
                                        }
                                    } else {
                                        nicknameToStore = existingNick;
                                    }
                                    final String finalNickname = nicknameToStore;

                                    if (doc.exists()) {
                                        db.collection("users").document(uid).set(data, SetOptions.merge())
                                                .addOnCompleteListener(task -> {
                                                    storeUserData(uid, email != null ? email : "", finalNickname != null ? finalNickname : "", providerId, finalFacebookId, finalFacebookName, finalFacebookPhotoUrl);
                                                    ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                            Objects.requireNonNull(new Object() {
                                                            }.getClass().getEnclosingMethod()).getName() +
                                                            ": login success uid=" + uid + ", nickname=" +
                                                            (finalNickname != null ? finalNickname : "null"));
                                                    callback.onLoginSuccess();
                                                });
                                    } else {
                                        db.collection("users").document(uid).set(data, SetOptions.merge())
                                                .addOnCompleteListener(task -> {
                                                    storeUserData(uid, email != null ? email : "", finalNickname != null ? finalNickname : "", providerId, finalFacebookId, finalFacebookName, finalFacebookPhotoUrl);
                                                    ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                            Objects.requireNonNull(new Object() {
                                                            }.getClass().getEnclosingMethod()).getName() +
                                                            ": login success uid=" + uid + ", nickname=" +
                                                            (finalNickname != null ? finalNickname : "null"));
                                                    callback.onLoginSuccess();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                            Objects.requireNonNull(new Object() {
                                            }.getClass().getEnclosingMethod()).getName() +
                                            ": failed to read user data", e);
                                    callback.onLoginFailure(e.getMessage());
                                });
                    }).exceptionally(ex -> {
                        Log.e("TAG_Soccer", getClass().getSimpleName() + ".loginWithFacebookToken: Failed to fetch Facebook photo URL", ex);
                        // Continue with login even if photo URL fetch fails - just use null
                        final String finalFacebookPhotoUrl = null;
                        
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(uid).get(Source.SERVER)
                                .addOnSuccessListener(doc -> {
                                    String existingNick = doc.getString("nickname");
                                    Map<String, Object> data = new HashMap<>();
                                    if (email != null) data.put("email", email);
                                    data.put("method", providerId);
                                    String langCode = LanguageManager.getCurrentLanguageCode(context);
                                    data.put("language", langCode);
                                    
                                    // Add Facebook-specific data
                                    if (finalFacebookId != null) data.put("facebookId", finalFacebookId);
                                    if (finalFacebookName != null) data.put("facebookName", finalFacebookName);
                                    if (finalFacebookPhotoUrl != null) data.put("facebookPhotoUrl", finalFacebookPhotoUrl);

                                    String nicknameToStore;
                                    if (existingNick == null || existingNick.isEmpty()) {
                                        if (nickname != null && !nickname.isEmpty()) {
                                            data.put("nickname", nickname);
                                            data.put("nicknameLowercase", nickname.toLowerCase());
                                            nicknameToStore = nickname;
                                        } else {
                                            nicknameToStore = null;
                                        }
                                    } else {
                                        nicknameToStore = existingNick;
                                    }
                                    final String finalNickname = nicknameToStore;

                                    if (doc.exists()) {
                                        db.collection("users").document(uid).set(data, SetOptions.merge())
                                                .addOnCompleteListener(task -> {
                                                    storeUserData(uid, email != null ? email : "", finalNickname != null ? finalNickname : "", providerId, finalFacebookId, finalFacebookName, finalFacebookPhotoUrl);
                                                    ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                            Objects.requireNonNull(new Object() {
                                                            }.getClass().getEnclosingMethod()).getName() +
                                                            ": login success uid=" + uid + ", nickname=" +
                                                            (finalNickname != null ? finalNickname : "null"));
                                                    callback.onLoginSuccess();
                                                });
                                    } else {
                                        db.collection("users").document(uid).set(data, SetOptions.merge())
                                                .addOnCompleteListener(task -> {
                                                    storeUserData(uid, email != null ? email : "", finalNickname != null ? finalNickname : "", providerId, finalFacebookId, finalFacebookName, finalFacebookPhotoUrl);
                                                    ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                            Objects.requireNonNull(new Object() {
                                                            }.getClass().getEnclosingMethod()).getName() +
                                                            ": login success uid=" + uid + ", nickname=" +
                                                            (finalNickname != null ? finalNickname : "null"));
                                                    callback.onLoginSuccess();
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                            Objects.requireNonNull(new Object() {
                                            }.getClass().getEnclosingMethod()).getName() +
                                            ": failed to read user data", e);
                                    callback.onLoginFailure(e.getMessage());
                                });
                        return null;
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                            Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() +
                            ": signInWithCredential FAILED", e);
                    callback.onLoginFailure(e.getMessage());
                });
    }

    private void storeUserData(String uid, String email, String nickname, String method) {
        context.getSharedPreferences(LanguageManager.PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString("uid", uid)
                .putString("email", email)
                .putString("nickname", nickname)
                .putString("method", method)
                .apply();
    }

    private void storeUserData(String uid, String email, String nickname, String method, 
                              String facebookId, String facebookName, String facebookPhotoUrl) {
        SharedPreferences.Editor editor = context.getSharedPreferences(LanguageManager.PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putString("uid", uid)
                .putString("email", email)
                .putString("nickname", nickname)
                .putString("method", method);
        
        // Store Facebook-specific data if available
        if (facebookId != null) {
            editor.putString("facebookId", facebookId);
        }
        if (facebookName != null) {
            editor.putString("facebookName", facebookName);
        }
        if (facebookPhotoUrl != null) {
            editor.putString("facebookPhotoUrl", facebookPhotoUrl);
        }
        
        editor.apply();
    }

    public void loginUser(String email, String password, LoginCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (firebaseAuth.getCurrentUser() != null) {
                            boolean isVerified = firebaseAuth.getCurrentUser().isEmailVerified();
                            String uid = firebaseAuth.getCurrentUser().getUid();
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": User UID: " + uid);
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": Email Verified: " + isVerified);

                            if (isVerified) {
                                FirebaseFirestore db = FirebaseFirestore.getInstance();
                                // Always fetch the latest data directly from the server so that
                                // a stale or missing cache entry doesn't cause us to miss an
                                // existing nickname and prompt the user unnecessarily.
                                db.collection("users").document(uid)
                                        .get(Source.SERVER)
                                        .addOnSuccessListener(doc -> {
                                            String nickname = doc.getString("nickname");
                                            // Always record the login method as email when using this path
                                            Map<String, Object> data = new HashMap<>();
                                            data.put("method", "email");
                                            if (email != null) data.put("email", email);
                                            String langCode = LanguageManager.getCurrentLanguageCode(context);
                                            data.put("language", langCode);
                                            db.collection("users").document(uid).set(data, SetOptions.merge());

                                            // Preserve any previously-saved nickname instead of overwriting it
                                            SharedPreferences p = context.getSharedPreferences(
                                                    LanguageManager.PREFS_FILE,
                                                    Context.MODE_PRIVATE);
                                            String currentNick = p.getString("nickname", "");

                                            storeUserData(uid,
                                                    email,
                                                    (nickname != null && !nickname.isEmpty())
                                                            ? nickname
                                                            : currentNick,
                                                    "email");
                                            ((SoccerApp) context.getApplicationContext())
                                                    .enableFcmAutoInit();
                                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                    Objects.requireNonNull(new Object() {
                                                    }.getClass().getEnclosingMethod()).getName() +
                                                    ": login success uid=" + uid + ", nickname=" +
                                                    (nickname != null ? nickname : "null"));
                                            callback.onLoginSuccess();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                                            }.getClass().getEnclosingMethod()).getName() + ": ⚠️ Failed to load nickname from Firestore", e);
                                            // Important: don't overwrite prefs with an empty nickname.
                                            // Keep existing prefs; MenuActivity will do a fresh fetch.
                                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                    Objects.requireNonNull(new Object() {
                                                    }.getClass().getEnclosingMethod()).getName() +
                                                    ": login success uid=" + uid + ", nickname=null");
                                            callback.onLoginSuccess();
                                        });
                            } else {
                                Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() + ": Email is NOT verified, signing out...");
                                firebaseAuth.signOut();
                                callback.onLoginFailure(context.getString(R.string.please_verify_email_before_login));
                            }
                        } else {
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                            }.getClass().getEnclosingMethod()).getName() + ": firebaseAuth.getCurrentUser() is NULL after signIn!");
                            callback.onLoginFailure(context.getString(R.string.authentication_failed));
                        }
                    } else {
                        callback.onLoginFailure(Objects.requireNonNull(task.getException()).getMessage());
                    }
                });
    }


    public void registerUser(String email, String password) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName() + ": User registered: " + Objects.requireNonNull(firebaseAuth.getCurrentUser()).getEmail());
                        Toast.makeText(context, context.getString(R.string.registered_as, firebaseAuth.getCurrentUser().getEmail()), Toast.LENGTH_SHORT).show();

                        ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                        // Store basic user data in Firestore
                        String uid = firebaseAuth.getCurrentUser().getUid();
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("email", email); // optional

                        userData.put("blockInviteFriend", false); // new field for blocking invites

                        userData.put("method", "email");

                        String langCode = LanguageManager.getCurrentLanguageCode(context);
                        userData.put("language", langCode);

                        db.collection("users").document(uid).set(userData, SetOptions.merge())
                                .addOnFailureListener(e ->
                                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                                        }.getClass().getEnclosingMethod()).getName() + ": Failed to save user data: " + e.getMessage())
                                );


                        // Send verification email
                        firebaseAuth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(verifyTask -> {
                                    if (verifyTask.isSuccessful()) {
                                        new AlertDialog.Builder(context)
                                                .setTitle(R.string.verification_email_sent)
                                                .setMessage(R.string.check_email_to_verify)
                                                .setPositiveButton(R.string.ok, (dialog, which) -> {
                                                    // Close the current activity
                                                    firebaseAuth.signOut();
                                                    ((Activity) context).finish();
                                                })
                                                .show();
                                    } else {
                                        String error = verifyTask.getException() != null
                                                ? verifyTask.getException().getMessage()
                                                : "Unknown error";
                                        new AlertDialog.Builder(context)
                                                .setTitle(R.string.email_verification_failed)
                                                .setMessage(context.getString(R.string.could_not_send_verification_email, error))
                                                .setPositiveButton(R.string.ok, null)
                                                .show();
                                    }
                                });

                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." + Objects.requireNonNull(new Object() {
                        }.getClass().getEnclosingMethod()).getName() + ": Registration failed: " + errorMsg);
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.registration_failed_title)
                                .setMessage(errorMsg)
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    }
                });
    }

    public void resetPassword(String email, ResetPasswordCallback callback) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() +
                ": Starting password reset for email: " + email);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() +
                                ": Password reset email sent successfully for: " + email);
                        callback.onResetPasswordSuccess();
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() +
                                ": Password reset failed for: " + email + ", error: " + errorMsg);
                        callback.onResetPasswordFailure(errorMsg);
                    }
                });
    }

    public void loginAnonymously(LoginCallback callback) {
        Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                Objects.requireNonNull(new Object() {
                }.getClass().getEnclosingMethod()).getName() +
                ": Starting anonymous login");

        firebaseAuth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                    Objects.requireNonNull(new Object() {
                                    }.getClass().getEnclosingMethod()).getName() +
                                    ": Anonymous login successful, UID: " + uid);

                            FirebaseFirestore db = FirebaseFirestore.getInstance();
                            
                            // Check if user document already exists
                            db.collection("users").document(uid).get(Source.SERVER)
                                    .addOnSuccessListener(doc -> {
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("method", "anonymous");
                                        String langCode = LanguageManager.getCurrentLanguageCode(context);
                                        data.put("language", langCode);
                                        data.put("blockInviteFriend", false);

                                        // Create-or-update without wiping existing fields like `nickname`
                                        db.collection("users").document(uid).set(data, SetOptions.merge())
                                                .addOnCompleteListener(updateTask -> {
                                                    ((SoccerApp) context.getApplicationContext()).enableFcmAutoInit();
                                                    Log.d("TAG_Soccer", getClass().getSimpleName() + "." +
                                                            Objects.requireNonNull(new Object() {
                                                            }.getClass().getEnclosingMethod()).getName() +
                                                            ": Anonymous user document created/updated successfully");
                                                    callback.onLoginSuccess();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                                            Objects.requireNonNull(new Object() {
                                                            }.getClass().getEnclosingMethod()).getName() +
                                                            ": Failed to create/update anonymous user document: " + e.getMessage());
                                                    callback.onLoginFailure("Failed to setup user profile: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                                Objects.requireNonNull(new Object() {
                                                }.getClass().getEnclosingMethod()).getName() +
                                                ": Failed to check existing user document: " + e.getMessage());
                                        callback.onLoginFailure("Failed to verify user profile: " + e.getMessage());
                                    });
                        } else {
                            Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                    Objects.requireNonNull(new Object() {
                                    }.getClass().getEnclosingMethod()).getName() +
                                    ": Anonymous login succeeded but user is null");
                            callback.onLoginFailure("Authentication failed - user is null");
                        }
                    } else {
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error occurred";
                        Log.e("TAG_Soccer", getClass().getSimpleName() + "." +
                                Objects.requireNonNull(new Object() {
                                }.getClass().getEnclosingMethod()).getName() +
                                ": Anonymous login failed: " + errorMsg);
                        callback.onLoginFailure(errorMsg);
                    }
                });
    }
}
