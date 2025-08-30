package piotr_gorczynski.soccer2;

import android.util.Log;

/**
 * Utility class for testing crash handling improvements
 * This class provides methods to trigger controlled crashes for testing purposes
 */
public class CrashTestHelper {
    
    private static final String TAG = "TAG_Soccer";
    
    /**
     * Trigger a test NullPointerException to verify crash handling
     */
    public static void triggerNullPointerException() {
        Log.d(TAG, "CrashTestHelper.triggerNullPointerException: About to trigger test crash");
        String nullString = null;
        // This will cause a NullPointerException
        int length = nullString.length();
    }
    
    /**
     * Trigger a test IllegalStateException to verify crash handling
     */
    public static void triggerIllegalStateException() {
        Log.d(TAG, "CrashTestHelper.triggerIllegalStateException: About to trigger test crash");
        throw new IllegalStateException("Test crash to verify enhanced exception handling");
    }
    
    /**
     * Trigger a test ArrayIndexOutOfBoundsException to verify crash handling
     */
    public static void triggerArrayIndexOutOfBoundsException() {
        Log.d(TAG, "CrashTestHelper.triggerArrayIndexOutOfBoundsException: About to trigger test crash");
        int[] array = new int[3];
        // This will cause an ArrayIndexOutOfBoundsException
        int value = array[10];
    }
    
    /**
     * Trigger a nested exception to test root cause analysis
     */
    public static void triggerNestedException() {
        Log.d(TAG, "CrashTestHelper.triggerNestedException: About to trigger nested test crash");
        try {
            triggerNullPointerException();
        } catch (Exception e) {
            throw new RuntimeException("Nested exception test", e);
        }
    }
}