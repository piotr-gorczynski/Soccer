package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

/** Bangladesh privacy documents follow the selected application language. */
public final class PrivacyPolicyLinks {
    public static final String BANGLADESH_URL = "https://piotr-gorczynski.com/bangladesh/privacy-en.html";
    private PrivacyPolicyLinks() {}
    public static String bangladeshUrl(String language) {
        return "https://piotr-gorczynski.com/bangladesh/privacy-" + ("bn".equals(language) ? "bn" : "en") + ".html";
    }

    public static void bind(Activity activity, int viewId) {
        View link = activity.findViewById(viewId);
        boolean bangladesh = AppFlavourDetector.isBangladeshFlavour(activity);
        link.setVisibility(bangladesh ? View.VISIBLE : View.GONE);
        if (bangladesh) link.setOnClickListener(view -> {
            String url = bangladeshUrl(LanguageManager.getCurrentLanguageCode(activity));
            try {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (ActivityNotFoundException error) {
                Toast.makeText(activity, url, Toast.LENGTH_LONG).show();
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
