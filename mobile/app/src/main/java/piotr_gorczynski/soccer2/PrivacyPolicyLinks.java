package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

/** Bangladesh has an independently maintained English privacy notice. */
public final class PrivacyPolicyLinks {
    public static final String BANGLADESH_URL = "https://piotr-gorczynski.com/bangladesh/privacy-en.html";
    private PrivacyPolicyLinks() {}

    public static void bind(Activity activity, int viewId) {
        View link = activity.findViewById(viewId);
        boolean bangladesh = AppFlavourDetector.isBangladeshFlavour(activity);
        link.setVisibility(bangladesh ? View.VISIBLE : View.GONE);
        if (bangladesh) link.setOnClickListener(view -> {
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BANGLADESH_URL)));
            } catch (ActivityNotFoundException error) {
                Toast.makeText(activity, BANGLADESH_URL, Toast.LENGTH_LONG).show();
            }
        });
    }

    public static boolean isSharedPrivacyUrl(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        return "https".equals(uri.getScheme()) && "piotr-gorczynski.com".equals(uri.getHost())
                && uri.getPath() != null && uri.getPath().matches("/privacy-[a-zA-Z-]+\\.html");
    }
}
