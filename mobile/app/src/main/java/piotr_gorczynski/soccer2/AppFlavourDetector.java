package piotr_gorczynski.soccer2;

import android.content.Context;

/**
 * Utility class to detect which app flavour (market variant) is currently running.
 * This is used to filter tournaments and features based on the app variant.
 */
public class AppFlavourDetector {

    /**
     * Get the current app flavour based on the package name.
     * 
     * @param context Android context
     * @return "bangladesh" if running the Bangladesh variant, "global" otherwise
     */
    public static String getCurrentFlavour(Context context) {
        String packageName = context.getPackageName();
        
        // Bangladesh variant has package name ending with ".bd"
        if (packageName.endsWith(".bd")) {
            return "bangladesh";
        }
        
        // Default to global flavour
        return "global";
    }

    /**
     * Check if currently running the Bangladesh variant.
     * 
     * @param context Android context
     * @return true if running Bangladesh variant, false otherwise
     */
    public static boolean isBangladeshFlavour(Context context) {
        return "bangladesh".equals(getCurrentFlavour(context));
    }

    /**
     * Check if currently running the Global variant.
     * 
     * @param context Android context
     * @return true if running Global variant, false otherwise
     */
    public static boolean isGlobalFlavour(Context context) {
        return "global".equals(getCurrentFlavour(context));
    }
}
