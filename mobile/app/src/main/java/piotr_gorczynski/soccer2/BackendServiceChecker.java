package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Utility class to check backend service availability
 * Mimics the logic from test_service_check.yaml
 */
public class BackendServiceChecker {
    
    private static final String TAG = "BackendServiceChecker";
    private static final String SERVICE_URL_TEMPLATE = "https://us-central1-%s.cloudfunctions.net/service-check";
    private static final String DEFAULT_PROJECT_ID = "soccer-dev"; // fallback if no project found
    private static final int TIMEOUT_SECONDS = 10;
    
    private final OkHttpClient httpClient;
    private final Context context;
    
    public interface ServiceCheckCallback {
        void onServiceAvailable();
        void onServiceUnavailable(String reason);
    }
    
    public BackendServiceChecker(Context context) {
        this.context = context.getApplicationContext();
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Check if the backend service is available
     * @param callback callback to handle the result
     */
    public void checkServiceAvailability(@NonNull ServiceCheckCallback callback) {
        Log.d(TAG, "Starting backend service availability check");
        
        String projectId = getProjectId();
        String serviceUrl = String.format(SERVICE_URL_TEMPLATE, projectId);
        
        Log.d(TAG, "Checking service URL: " + serviceUrl);
        
        Request.Builder requestBuilder = new Request.Builder()
                .url(serviceUrl)
                .addHeader("Cache-Control", "no-cache, no-store, must-revalidate")
                .addHeader("Pragma", "no-cache")
                .addHeader("Connection", "close");
        
        // Try to add secret key if available (for enhanced security)
        String secretKey = getSecretKey();
        if (secretKey != null && !secretKey.isEmpty()) {
            requestBuilder.addHeader("X-Secret-Key", secretKey);
            Log.d(TAG, "Added X-Secret-Key header");
        } else {
            Log.d(TAG, "No secret key available, proceeding without authentication");
        }
        
        Request request = requestBuilder.build();
        
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "Service check failed: " + e.getMessage(), e);
                callback.onServiceUnavailable("Network error: " + e.getMessage());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                try {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    Log.d(TAG, "Service check response: " + response.code() + " - " + responseBody);
                    
                    if (response.isSuccessful() && response.code() == 200) {
                        try {
                            JSONObject jsonResponse = new JSONObject(responseBody);
                            String status = jsonResponse.optString("status", "");
                            
                            if ("Active".equals(status)) {
                                Log.d(TAG, "Backend service is available");
                                callback.onServiceAvailable();
                                return;
                            } else {
                                Log.w(TAG, "Service returned unexpected status: " + status);
                                callback.onServiceUnavailable("Service status: " + status);
                                return;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse service response JSON", e);
                            callback.onServiceUnavailable("Invalid response format");
                            return;
                        }
                    }
                    
                    // If we get here, the response was not successful
                    String errorMsg = "HTTP " + response.code();
                    if (response.code() == 403) {
                        errorMsg = "Authentication failed";
                    } else if (response.code() >= 500) {
                        errorMsg = "Server error";
                    }
                    
                    Log.w(TAG, "Service check failed with response: " + response.code());
                    callback.onServiceUnavailable(errorMsg);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error processing service response", e);
                    callback.onServiceUnavailable("Response processing error");
                } finally {
                    if (response.body() != null) {
                        response.body().close();
                    }
                }
            }
        });
    }
    
    /**
     * Get the project ID for the service URL
     * This could be enhanced to read from configuration or detect dynamically
     */
    private String getProjectId() {
        // For now, try to detect from shared preferences or use default
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        
        String projectId = prefs.getString("backend_project_id", null);
        if (projectId == null || projectId.isEmpty()) {
            // Try some common patterns based on the test file
            projectId = DEFAULT_PROJECT_ID;
            Log.d(TAG, "Using default project ID: " + projectId);
        } else {
            Log.d(TAG, "Using configured project ID: " + projectId);
        }
        
        return projectId;
    }
    
    /**
     * Get the secret key for authentication
     * This could be enhanced to read from secure storage
     */
    private String getSecretKey() {
        // For now, return null to skip authentication
        // This can be enhanced later for better security
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        
        return prefs.getString("backend_secret_key", null);
    }
    
    /**
     * Set the project ID for service checks
     */
    public void setProjectId(String projectId) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("backend_project_id", projectId).apply();
        Log.d(TAG, "Project ID updated: " + projectId);
    }
    
    /**
     * Set the secret key for authentication
     */
    public void setSecretKey(String secretKey) {
        SharedPreferences prefs = context.getSharedPreferences(
                context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
        prefs.edit().putString("backend_secret_key", secretKey).apply();
        Log.d(TAG, "Secret key updated");
    }
}