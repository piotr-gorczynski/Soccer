package piotr_gorczynski.soccer2;

import android.content.Context;
import android.util.Log;

/**
 * Utility class for safe string formatting that prevents crashes from malformed format specifiers
 * in localized string resources. This addresses the issue where malformed format specifiers like
 * "%1 $s" (with spaces) in translations cause UnknownFormatConversionException.
 */
public class SafeStringFormatter {
    
    private static final String TAG = "TAG_Soccer";
    
    /**
     * Safely format a string resource with parameters, providing fallback on format errors
     * @param context The context to get string resource from
     * @param stringRes The string resource ID
     * @param formatArgs Arguments for string formatting
     * @return Formatted string, or fallback string if formatting fails
     */
    public static String safeGetString(Context context, int stringRes, Object... formatArgs) {
        try {
            return context.getString(stringRes, formatArgs);
        } catch (java.util.UnknownFormatConversionException e) {
            Log.e(TAG, "SafeStringFormatter.safeGetString: Unknown format conversion in resource " + 
                  context.getResources().getResourceName(stringRes) + " (conversion: '" + e.getConversion() + "'), using fallback", e);
            
            // Get the raw string to use as fallback
            // Use getText instead of getString to avoid format processing which would throw the same exception
            String rawString = context.getResources().getText(stringRes).toString();
            
            // Create a simple fallback by concatenating the raw string with arguments
            StringBuilder fallback = new StringBuilder(rawString);
            if (formatArgs.length > 0) {
                fallback.append(" (");
                for (int i = 0; i < formatArgs.length; i++) {
                    if (i > 0) fallback.append(", ");
                    fallback.append(formatArgs[i]);
                }
                fallback.append(")");
            }
            
            return fallback.toString();
        } catch (java.util.IllegalFormatException e) {
            Log.e(TAG, "SafeStringFormatter.safeGetString: String formatting error for resource " + 
                  context.getResources().getResourceName(stringRes) + ", using fallback", e);
        } catch (IllegalArgumentException e) {
            // Catches UnknownFormatConversionException which extends IllegalArgumentException
            Log.e(TAG, "SafeStringFormatter.safeGetString: Illegal argument in string formatting for resource " + 
                  context.getResources().getResourceName(stringRes) + ", using fallback", e);
        }
        
        // Common fallback logic for both exception types
        try {
            // Use getText instead of getString to avoid format processing in fallback
            // This prevents recursive exceptions when the raw string itself has malformed format specifiers
            String rawString = context.getResources().getText(stringRes).toString();
            
            // Create a simple fallback by concatenating the raw string with arguments
            StringBuilder fallback = new StringBuilder(rawString);
            if (formatArgs.length > 0) {
                fallback.append(" (");
                for (int i = 0; i < formatArgs.length; i++) {
                    if (i > 0) fallback.append(", ");
                    fallback.append(formatArgs[i]);
                }
                fallback.append(")");
            }
            
            return fallback.toString();
        } catch (Exception e) {
            Log.e(TAG, "SafeStringFormatter.safeGetString: Unexpected error formatting string resource " + 
                  stringRes, e);
            
            // Ultimate fallback - just return the arguments joined with spaces
            StringBuilder ultimate = new StringBuilder();
            for (int i = 0; i < formatArgs.length; i++) {
                if (i > 0) ultimate.append(" ");
                ultimate.append(formatArgs[i]);
            }
            return ultimate.toString();
        }
    }
    
    /**
     * Safely format a string with String.format, providing fallback on format errors
     * @param format The format string
     * @param args Arguments for formatting
     * @return Formatted string, or fallback string if formatting fails
     */
    public static String safeFormat(String format, Object... args) {
        try {
            return String.format(format, args);
        } catch (java.util.UnknownFormatConversionException e) {
            Log.e(TAG, "SafeStringFormatter.safeFormat: Unknown format conversion in format '" + 
                  format + "' (conversion: '" + e.getConversion() + "'), using fallback", e);
            
            // Create a simple fallback by concatenating format with arguments
            StringBuilder fallback = new StringBuilder(format);
            if (args.length > 0) {
                fallback.append(" (");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) fallback.append(", ");
                    fallback.append(args[i]);
                }
                fallback.append(")");
            }
            
            return fallback.toString();
        } catch (java.util.IllegalFormatException e) {
            Log.e(TAG, "SafeStringFormatter.safeFormat: String formatting error for format '" + 
                  format + "', using fallback", e);
        } catch (IllegalArgumentException e) {
            // Catches UnknownFormatConversionException which extends IllegalArgumentException  
            Log.e(TAG, "SafeStringFormatter.safeFormat: Illegal argument in string formatting for format '" + 
                  format + "', using fallback", e);
        }
        
        // Common fallback logic for both exception types
        try {
            // Create a simple fallback by concatenating format with arguments
            StringBuilder fallback = new StringBuilder(format);
            if (args.length > 0) {
                fallback.append(" (");
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) fallback.append(", ");
                    fallback.append(args[i]);
                }
                fallback.append(")");
            }
            
            return fallback.toString();
        } catch (Exception e) {
            Log.e(TAG, "SafeStringFormatter.safeFormat: Unexpected error formatting string '" + 
                  format + "'", e);
            
            // Ultimate fallback - just return the arguments joined with spaces
            StringBuilder ultimate = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) ultimate.append(" ");
                ultimate.append(args[i]);
            }
            return ultimate.toString();
        }
    }
}