package piotr_gorczynski.soccer2;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Plain text only. Approved HTTPS hosts, support-authored messages, BD only. */
final class SupportMessageLinks {
    private static final Pattern URL = Pattern.compile("https://[^\\s<>]+", Pattern.CASE_INSENSITIVE);
    static boolean approved(String value) {
        if (value == null || value.length() > 512 || value.matches(".*[\\s\\\\\\p{Cntrl}].*")) return false;
        Uri uri = Uri.parse(value);
        String host = uri.getHost();
        return "https".equalsIgnoreCase(uri.getScheme()) && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443) && host != null
                && Arrays.asList("facebook.com", "www.facebook.com", "m.facebook.com",
                    "play.google.com", "www.youtube.com", "youtube.com", "youtu.be", "piotr-gorczynski.com")
                    .contains(host.toLowerCase(java.util.Locale.ROOT));
    }
    static void bind(TextView text, SupportModels.Message message) {
        Context context = text.getContext();
        if (!AppFlavourDetector.isBangladeshFlavour(context) || !"support".equals(message.author)) return;
        SpannableString content = new SpannableString(message.body);
        Matcher matcher = URL.matcher(message.body);
        boolean found = false;
        while (matcher.find()) {
            int end = matcher.end();
            while (end > matcher.start() && ".,;!?)".indexOf(message.body.charAt(end - 1)) >= 0) end--;
            String url = message.body.substring(matcher.start(), end);
            if (!approved(url)) continue;
            found = true;
            content.setSpan(new ClickableSpan() {
                @Override public void onClick(View view) {
                    try {
                        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        if ("post_payout_feedback".equals(message.kind) && url.equals(message.feedbackUrl)
                                && Arrays.asList("community", "tournament_post").contains(message.destinationType)
                                && context.getApplicationContext() instanceof SoccerApp) {
                            AnalyticsManager analytics=((SoccerApp)context.getApplicationContext()).getAnalyticsManager();
                            if (analytics != null) analytics.trackPostPayoutFeedbackLinkClick(message.destinationType);
                        }
                    } catch (ActivityNotFoundException ignored) {
                        // Keep the URL selectable for copying if no browser is installed.
                    }
                }
            }, matcher.start(), end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (found) { text.setText(content); text.setMovementMethod(LinkMovementMethod.getInstance()); }
    }
}
