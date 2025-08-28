package piotr_gorczynski.soccer2;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ExceptionHandler implements
        Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final Context context;

    public ExceptionHandler(Context context) {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        this.context = context;
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, Throwable exception) {
        try {
            // Generate comprehensive crash report
            String crashReport = generateCrashReport(thread, exception);
            
            // Log with multiple levels to ensure visibility
            Log.e("TAG_Soccer", "🚨 UNCAUGHT EXCEPTION DETECTED 🚨");
            Log.e("TAG_Soccer", crashReport);
            Log.wtf("TAG_Soccer", "CRASH REPORT", exception);
            
            // Also log to system error
            System.err.println("SOCCER APP CRASH REPORT:");
            System.err.println(crashReport);
            exception.printStackTrace(System.err);
            
        } catch (Throwable t) {
            // If our crash handler crashes, fall back to basic logging
            Log.e("TAG_Soccer", "ExceptionHandler failed to generate report", t);
            Log.e("TAG_Soccer", "Original exception:", exception);
        }

        // Call the original handler to maintain Android's default behavior
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, exception);
        } else {
            // Fallback if no default handler
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        }
    }

    private String generateCrashReport(@NonNull Thread thread, Throwable exception) {
        StringBuilder report = new StringBuilder();
        String LINE_SEPARATOR = "\n";
        
        // Timestamp
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
        report.append("===============================================").append(LINE_SEPARATOR);
        report.append("🚨 SOCCER APP CRASH REPORT").append(LINE_SEPARATOR);
        report.append("===============================================").append(LINE_SEPARATOR);
        report.append("Timestamp: ").append(sdf.format(new Date())).append(LINE_SEPARATOR);
        report.append("Thread: ").append(thread.getName()).append(" (ID: ").append(thread.getId()).append(")").append(LINE_SEPARATOR);
        report.append(LINE_SEPARATOR);
        
        // Exception information
        report.append("************ EXCEPTION DETAILS ************").append(LINE_SEPARATOR);
        report.append("Exception Type: ").append(exception.getClass().getName()).append(LINE_SEPARATOR);
        report.append("Message: ").append(exception.getMessage()).append(LINE_SEPARATOR);
        report.append(LINE_SEPARATOR);
        
        // Full stack trace
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        report.append("************ FULL STACK TRACE ************").append(LINE_SEPARATOR);
        report.append(stackTrace.toString()).append(LINE_SEPARATOR);
        
        // Root cause analysis
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        if (rootCause != exception) {
            report.append("************ ROOT CAUSE ************").append(LINE_SEPARATOR);
            report.append("Root Cause Type: ").append(rootCause.getClass().getName()).append(LINE_SEPARATOR);
            report.append("Root Cause Message: ").append(rootCause.getMessage()).append(LINE_SEPARATOR);
            StringWriter rootTrace = new StringWriter();
            rootCause.printStackTrace(new PrintWriter(rootTrace));
            report.append(rootTrace.toString()).append(LINE_SEPARATOR);
        }
        
        // Device information
        report.append("************ DEVICE INFORMATION ************").append(LINE_SEPARATOR);
        report.append("Brand: ").append(Build.BRAND).append(LINE_SEPARATOR);
        report.append("Manufacturer: ").append(Build.MANUFACTURER).append(LINE_SEPARATOR);
        report.append("Model: ").append(Build.MODEL).append(LINE_SEPARATOR);
        report.append("Device: ").append(Build.DEVICE).append(LINE_SEPARATOR);
        report.append("Product: ").append(Build.PRODUCT).append(LINE_SEPARATOR);
        report.append("Hardware: ").append(Build.HARDWARE).append(LINE_SEPARATOR);
        report.append("Board: ").append(Build.BOARD).append(LINE_SEPARATOR);
        report.append("ID: ").append(Build.ID).append(LINE_SEPARATOR);
        report.append(LINE_SEPARATOR);
        
        // Android version information
        report.append("************ ANDROID VERSION ************").append(LINE_SEPARATOR);
        report.append("SDK Version: ").append(Build.VERSION.SDK_INT).append(LINE_SEPARATOR);
        report.append("Release: ").append(Build.VERSION.RELEASE).append(LINE_SEPARATOR);
        report.append("Incremental: ").append(Build.VERSION.INCREMENTAL).append(LINE_SEPARATOR);
        report.append("Codename: ").append(Build.VERSION.CODENAME).append(LINE_SEPARATOR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            report.append("Security Patch: ").append(Build.VERSION.SECURITY_PATCH).append(LINE_SEPARATOR);
        }
        report.append(LINE_SEPARATOR);
        
        // Application state information
        if (context != null) {
            report.append("************ APPLICATION STATE ************").append(LINE_SEPARATOR);
            try {
                ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    List<ActivityManager.RunningAppProcessInfo> runningApps = activityManager.getRunningAppProcesses();
                    if (runningApps != null) {
                        for (ActivityManager.RunningAppProcessInfo processInfo : runningApps) {
                            if (processInfo.processName.equals(context.getPackageName())) {
                                report.append("Process Name: ").append(processInfo.processName).append(LINE_SEPARATOR);
                                report.append("Process PID: ").append(processInfo.pid).append(LINE_SEPARATOR);
                                report.append("Process Importance: ").append(getImportanceString(processInfo.importance)).append(LINE_SEPARATOR);
                                break;
                            }
                        }
                    }
                }
                
                // Memory information
                ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
                if (activityManager != null) {
                    activityManager.getMemoryInfo(memoryInfo);
                    report.append("Available Memory: ").append(memoryInfo.availMem / (1024 * 1024)).append(" MB").append(LINE_SEPARATOR);
                    report.append("Total Memory: ").append(memoryInfo.totalMem / (1024 * 1024)).append(" MB").append(LINE_SEPARATOR);
                    report.append("Low Memory: ").append(memoryInfo.lowMemory).append(LINE_SEPARATOR);
                    report.append("Memory Threshold: ").append(memoryInfo.threshold / (1024 * 1024)).append(" MB").append(LINE_SEPARATOR);
                }
            } catch (Exception e) {
                report.append("Failed to get application state: ").append(e.getMessage()).append(LINE_SEPARATOR);
            }
        }
        
        report.append("===============================================").append(LINE_SEPARATOR);
        
        return report.toString();
    }
    
    private String getImportanceString(int importance) {
        switch (importance) {
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND:
                return "FOREGROUND";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE:
                return "VISIBLE";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE:
                return "SERVICE";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND:
                return "BACKGROUND";
            case ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY:
                return "EMPTY";
            default:
                return "UNKNOWN(" + importance + ")";
        }
    }
}
