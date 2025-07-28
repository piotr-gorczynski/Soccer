package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;

public class ConsentUtils {

    /**
     * Determine if the user allowed personalised ads based on the
     * Transparency & Consent Framework string stored by UMP.
     */
    public static boolean isPersonalisedAllowed(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(
                "com.google.android.ump.pref", Context.MODE_PRIVATE);
        String purposes = sp.getString("IABTCF_PurposeConsents", "");
        // purposes are 1-based, so purpose 4 is index 3
        return purposes.length() >= 4 && purposes.charAt(3) == '1';
    }

    private ConsentUtils() {
        // no instances
    }
}
