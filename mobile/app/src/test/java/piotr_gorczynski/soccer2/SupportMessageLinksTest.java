package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.content.Intent;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.RuntimeEnvironment;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=SupportMessageLinksTest.TestApp.class,sdk=34)
public class SupportMessageLinksTest {
    public static class TestApp extends SoccerApp {
        final AnalyticsManager analytics=mock(AnalyticsManager.class);
        @Override public void onCreate() {}
        @Override public AnalyticsManager getAnalyticsManager() { return analytics; }
    }
    private SupportModels.Message message(String author,String body) {
        Map<String,Object> data=new HashMap<>();
        data.put("messageId","message");data.put("authorType",author);data.put("message",body);
        return new SupportModels.Message(data);
    }
    @Test public void rejectsNonHttpsCredentialsSpoofedHostsAndUnsafeSchemes() {
        for(String url:new String[]{"http://www.facebook.com/x","javascript:alert(1)","file:///etc/passwd",
                "https://www.facebook.com.evil.test/", "https://admin@www.facebook.com/x",
                "https://www.facebook.com:444/", "https://www.facebook.com\\@evil.test/", "https://evil.test/"})
            assertFalse(url,SupportMessageLinks.approved(url));
        assertTrue(SupportMessageLinks.approved("https://www.facebook.com/profile.php?id=123"));
    }
    @Test public void supportLinksOpenActionViewOnlyInBangladeshAndPlainTextRemainsLiteral() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        TextView text=new TextView(activity);
        String body="<b>Thanks</b> https://www.facebook.com/profile.php?id=123";
        text.setText(body);SupportMessageLinks.bind(text,message("support",body));
        assertEquals(body,text.getText().toString());
        ClickableSpan[] spans=text.getText() instanceof Spanned
                ? ((Spanned)text.getText()).getSpans(0,text.length(),ClickableSpan.class) : new ClickableSpan[0];
        if(!AnalyticsManager.isBangladesh()) {assertEquals(0,spans.length);return;}
        assertEquals(1,spans.length);spans[0].onClick(text);
        Intent intent=shadowOf(activity).getNextStartedActivity();
        assertEquals(Intent.ACTION_VIEW,intent.getAction());
        assertEquals("https://www.facebook.com/profile.php?id=123",intent.getDataString());
    }
    @Test public void userLinksAndLegacyPlainTextAreUnchanged() {
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        for(String author:new String[]{"support","user"}) {
            String body=author.equals("user") ? "https://www.facebook.com/profile.php?id=123" : "Old reply with no links";
            TextView text=new TextView(activity);text.setText(body);
            SupportMessageLinks.bind(text,message(author,body));
            assertEquals(body,text.getText().toString());
            assertNull(text.getMovementMethod());
        }
    }
    @Test public void feedbackClickIsTrackedOnlyAfterActualLinkTapWithoutPrivateParameters() {
        org.junit.Assume.assumeTrue(AnalyticsManager.isBangladesh());
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        TestApp app=(TestApp)RuntimeEnvironment.getApplication();
        String url="https://www.facebook.com/profile.php?id=123";
        Map<String,Object> data=new HashMap<>();
        data.put("messageId","message");data.put("authorType","support");data.put("message",url);
        data.put("kind","post_payout_feedback");data.put("feedbackUrl",url);data.put("destinationType","community");
        TextView text=new TextView(activity);text.setText(url);
        SupportMessageLinks.bind(text,new SupportModels.Message(data));
        verifyNoInteractions(app.analytics);
        ((Spanned)text.getText()).getSpans(0,text.length(),ClickableSpan.class)[0].onClick(text);
        verify(app.analytics).trackPostPayoutFeedbackLinkClick("community");verifyNoMoreInteractions(app.analytics);
    }
}
