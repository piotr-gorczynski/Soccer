package piotr_gorczynski.soccer2;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.webkit.WebView;
import android.widget.Button;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=34)
public class TermsActivityTest {
    public static class TestActivity extends TermsActivity {
        int writes;
        TermsPolicy displayedPolicy;
        java.util.function.Consumer<String> pendingLoad;
        TaskCompletionSource<Void> write = new TaskCompletionSource<>();
        @Override protected void onCreate(Bundle state) {
            setTheme(R.style.AppTheme);super.onCreate(state);
        }
        @Override protected String currentUid() { return "test-user"; }
        @Override protected void loadDocument(TermsPolicy policy, java.util.function.Consumer<String> success,
                                              java.util.function.Consumer<Exception> failure) {
            displayedPolicy=policy;
            if(getIntent().getBooleanExtra("defer_load",false)) {
                pendingLoad=success;return;
            }
            if(getIntent().getBooleanExtra("load_failure",false)) {
                failure.accept(new java.io.IOException("hash mismatch"));return;
            }
            success.accept("<html><body>Verified test terms</body></html>");
        }
        @Override protected Task<Void> saveAcceptance(String uid, TermsPolicy policy) { writes++;return write.getTask(); }
    }
    private TestActivity open(boolean readOnly) {
        TestActivity activity=Robolectric.buildActivity(TestActivity.class,
            new Intent().putExtra(TermsActivity.READ_ONLY,readOnly)).create().get();
        WebView web=activity.findViewById(R.id.termsWebView);
        web.getWebViewClient().onPageFinished(web,"https://example.test");
        return activity;
    }
    @Test public void newGlobalUserSeesSelectedLanguageAndCanAcceptLoadedTerms() {
        org.junit.Assume.assumeFalse(BuildConfig.APPLICATION_ID.endsWith(".bd"));
        android.content.SharedPreferences prefs=org.robolectric.RuntimeEnvironment.getApplication()
            .getSharedPreferences(LanguageManager.PREFS_FILE,android.content.Context.MODE_PRIVATE);
        try {
            for(String language:new String[]{"am","ar","bn","my","en","fr","de","hi","km","lo","mg","mn","ne","fa","pl","si","so","es","sw","ur"}) {
                prefs.edit().putString(LanguageManager.PREF_LANGUAGE_CODE,language).commit();
                org.robolectric.android.controller.ActivityController<TestActivity> controller=
                    Robolectric.buildActivity(TestActivity.class,new Intent()).create();
                TestActivity activity=controller.get();
                assertEquals(language,activity.displayedPolicy.language);
                assertEquals(TermsPolicy.GLOBAL_VERSION,activity.displayedPolicy.version);
                Button accept=activity.findViewById(R.id.acceptTerms);
                assertFalse(accept.isEnabled());
                WebView web=activity.findViewById(R.id.termsWebView);
                web.getWebViewClient().onPageFinished(web,activity.displayedPolicy.url);
                assertTrue(accept.isEnabled());accept.performClick();
                assertEquals(1,activity.writes);
                activity.write.setResult(null);Shadows.shadowOf(Looper.getMainLooper()).idle();
                assertTrue(activity.isFinishing());controller.destroy();
            }
        } finally { prefs.edit().remove(LanguageManager.PREF_LANGUAGE_CODE).commit(); }
    }
    @Test public void unavailableDocumentCannotBeAccepted() {
        TestActivity activity=Robolectric.buildActivity(TestActivity.class,
            new Intent().putExtra("load_failure",true)).create().get();
        Button accept=activity.findViewById(R.id.acceptTerms);
        assertFalse(accept.isEnabled());accept.performClick();assertEquals(0,activity.writes);
        assertFalse(activity.isFinishing());
    }
    @Test public void retryOnlyAppearsAfterLoadFailureAndHidesDuringRetryAndSuccess() {
        TestActivity activity=Robolectric.buildActivity(TestActivity.class,
            new Intent().putExtra("load_failure",true)).create().get();
        WebView web=activity.findViewById(R.id.termsWebView);
        Button retry=(Button)((android.view.ViewGroup)web.getParent()).getChildAt(0);
        assertEquals(android.view.View.VISIBLE,retry.getVisibility());
        assertTrue(retry.isEnabled());
        activity.getIntent().putExtra("defer_load",true);
        retry.performClick();
        assertEquals(android.view.View.GONE,retry.getVisibility());
        assertFalse(retry.isEnabled());
        assertFalse(activity.findViewById(R.id.acceptTerms).isEnabled());
        activity.pendingLoad.accept("<html><body>Verified terms</body></html>");
        web.getWebViewClient().onPageFinished(web,activity.displayedPolicy.url);
        assertEquals(android.view.View.GONE,retry.getVisibility());
        assertTrue(activity.findViewById(R.id.acceptTerms).isEnabled());
    }
    @Test public void successfulInitialLoadDoesNotShowRetry() {
        TestActivity activity=open(false);
        WebView web=activity.findViewById(R.id.termsWebView);
        assertEquals(android.view.View.GONE,
            ((android.view.ViewGroup)web.getParent()).getChildAt(0).getVisibility());
    }
    @Test public void readOnlyNeverWritesOrFinishesAsAccepted() {
        TestActivity activity=open(true);
        Button accept=activity.findViewById(R.id.acceptTerms);
        assertEquals(android.view.View.GONE,accept.getVisibility());
        accept.performClick();assertEquals(0,activity.writes);
        assertFalse(activity.isFinishing());
        activity.findViewById(R.id.declineTerms).performClick();assertTrue(activity.isFinishing());
    }
    @Test public void failedWriteStaysOnTermsAndAllowsExplicitRetry() {
        TestActivity activity=open(false);Button accept=activity.findViewById(R.id.acceptTerms);
        accept.performClick();assertEquals(1,activity.writes);assertFalse(accept.isEnabled());
        assertFalse(activity.isFinishing());
        activity.write.setException(new IllegalStateException("permission denied"));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
        assertFalse(activity.isFinishing());assertTrue(accept.isEnabled());
        activity.write=new TaskCompletionSource<>();accept.performClick();assertEquals(2,activity.writes);
        assertFalse(activity.isFinishing());activity.write.setResult(null);
        Shadows.shadowOf(Looper.getMainLooper()).idle();assertTrue(activity.isFinishing());
    }
}
