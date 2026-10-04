package piotr_gorczynski.soccer2;
import static org.junit.Assert.*;
import android.app.Activity;
import android.content.Intent;
import android.widget.Button;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
@RunWith(RobolectricTestRunner.class)
public class PrivacyPolicyLinksTest {
 public static class FlavorActivity extends Activity {
  @Override public String getPackageName() {
   return BuildConfig.FLAVOR_market.equals("bangladesh")
     ? "piotr_gorczynski.soccer2.bd" : "piotr_gorczynski.soccer2";
  }
 }
 @Test public void onlyOwnRootPrivacyPagesAreRecognized() {
  assertTrue(PrivacyPolicyLinks.isSharedPrivacyUrl("https://piotr-gorczynski.com/privacy-pl.html"));
  assertFalse(PrivacyPolicyLinks.isSharedPrivacyUrl("https://example.com/privacy-en.html"));
  assertFalse(PrivacyPolicyLinks.isSharedPrivacyUrl("https://piotr-gorczynski.com/terms-en.html"));
  assertFalse(PrivacyPolicyLinks.isSharedPrivacyUrl(PrivacyPolicyLinks.BANGLADESH_URL));
 }
 @Test public void linkVisibilityAndDestinationFollowBuildFlavor() {
  Activity activity = Robolectric.buildActivity(FlavorActivity.class).setup().get();
  Button button = new Button(activity); button.setId(12345); activity.setContentView(button);
  PrivacyPolicyLinks.bind(activity, 12345);
  if (BuildConfig.FLAVOR_market.equals("bangladesh")) {
   assertEquals(android.view.View.VISIBLE, button.getVisibility()); button.performClick();
   Intent intent = Shadows.shadowOf(activity).getNextStartedActivity();
   assertEquals(Intent.ACTION_VIEW, intent.getAction());
   assertEquals("https://piotr-gorczynski.com/bangladesh/privacy-en.html", intent.getDataString());
  } else {
   assertEquals(android.view.View.GONE, button.getVisibility());
   assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
  }
 }
}
