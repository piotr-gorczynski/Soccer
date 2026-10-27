package piotr_gorczynski.soccer2;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class AnonymousLoginPresentationTest {
    private final boolean bd = BuildConfig.APPLICATION_ID.endsWith(".bd");

    private Context context(String locale) {
        Configuration config = new Configuration(RuntimeEnvironment.getApplication().getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(locale));
        Context localized=RuntimeEnvironment.getApplication().createConfigurationContext(config);
        return new android.view.ContextThemeWrapper(localized,R.style.AppTheme);
    }
    private void checkCopy(String locale, String button, String support, String payout) {
        Context context=context(locale);
        View layout=LayoutInflater.from(context).inflate(R.layout.activity_universal_login,null);
        TextView start=layout.findViewById(R.id.btnUniversalAnonymous);
        if(bd) {
            assertEquals(button,start.getText().toString());
            int supporting=context.getResources().getIdentifier("bdStartSupportingText","id",BuildConfig.APPLICATION_ID);
            int help=context.getResources().getIdentifier("bdStartPayoutText","id",BuildConfig.APPLICATION_ID);
            assertEquals(support,((TextView)layout.findViewById(supporting)).getText().toString());
            assertEquals(payout,((TextView)layout.findViewById(help)).getText().toString());
        } else {
            assertEquals(context.getString(R.string.login_anonymous),start.getText().toString());
            assertEquals(0,context.getResources().getIdentifier("bdStartSupportingText","id",BuildConfig.APPLICATION_ID));
        }
    }
    @Test public void englishCopyIsScopedToBangladesh() {
        checkCopy("en","Start Playing","No account details required.",
                "You only provide payout details if you win a prize.");
    }
    @Test public void bengaliCopyUsesLocalizedResources() {
        checkCopy("bn","খেলা শুরু করুন","অ্যাকাউন্টের কোনো তথ্য দিতে হবে না।",
                "পুরস্কার জিতলেই শুধু টাকা পাওয়ার জন্য প্রয়োজনীয় তথ্য দিতে হবে।");
    }
    @Test public void otherBangladeshLocalesCannotFallBackToInternalLoginLabel() {
        Context context=context("pl");
        View layout=LayoutInflater.from(context).inflate(R.layout.activity_universal_login,null);
        assertEquals(bd ? "Start Playing" : context.getString(R.string.login_anonymous),
                ((TextView)layout.findViewById(R.id.btnUniversalAnonymous)).getText().toString());
    }
    @Test public void proceedReportsChoiceAndStartsAuthentication() throws Exception {
        checkWarning(AlertDialog.BUTTON_POSITIVE, "proceed", true);
    }
    @Test public void cancelReportsChoiceWithoutAuthentication() throws Exception {
        checkWarning(AlertDialog.BUTTON_NEGATIVE, "cancel", false);
    }
    @Test public void dismissReportsChoiceWithoutAuthentication() throws Exception {
        checkWarning(0, "dismiss", false);
    }
    private void checkWarning(int button, String action, boolean starts) throws Exception {
        UniversalLoginActivity activity=Robolectric.buildActivity(UniversalLoginActivity.class).get();
        activity.setTheme(R.style.AppTheme);
        FirebaseAuthManager manager=mock(FirebaseAuthManager.class);
        AnalyticsManager analytics=mock(AnalyticsManager.class);
        Field auth=UniversalLoginActivity.class.getDeclaredField("authManager");
        auth.setAccessible(true);auth.set(activity,manager);
        Field tracking=UniversalLoginActivity.class.getDeclaredField("analyticsManager");
        tracking.setAccessible(true);tracking.set(activity,analytics);
        Method click=UniversalLoginActivity.class.getDeclaredMethod("handleAnonymousLogin");
        click.setAccessible(true);click.invoke(activity);
        verify(manager,never()).loginAnonymously(any());
        verify(analytics).trackAnonymousWarning("shown");
        AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        if(button==0) dialog.cancel();
        else dialog.getButton(button).performClick();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        verify(analytics).trackAnonymousWarning(action);
        verify(analytics,times(2)).trackAnonymousWarning(anyString());
        verify(manager,times(starts ? 1 : 0)).loginAnonymously(any(FirebaseAuthManager.LoginCallback.class));
    }
}
