package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class ConsentUtils {

    /**
     * Determine if the user allowed personalised ads based on the
     * Transparency & Consent Framework string stored by UMP.
     */
    public static boolean isPersonalisedAllowed(Context ctx) {
        // UMP writes the IAB keys into the default shared‑prefs file
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);

        String purposes = sp.getString("IABTCF_PurposeConsents", "");
        Log.d("TAG_Soccer", "purpose string = \"" + purposes + "\"");

        // Purposes are 1‑based → purpose 4 (“Select personalised ads”) ⇒ index 3
        return purposes.length() >= 4 && purposes.charAt(3) == '1';
    }

    private ConsentUtils() {
        // no instances
    }
}
