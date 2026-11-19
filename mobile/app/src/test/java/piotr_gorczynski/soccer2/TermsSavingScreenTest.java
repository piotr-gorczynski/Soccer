package piotr_gorczynski.soccer2;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.webkit.WebView;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=34)
public class TermsSavingScreenTest {
    public static class Screen extends TermsActivityTest.InstrumentedActivity {
        String account="test-user";
        @Override protected String currentUid() { return account; }
    }
    private boolean bd() { return BuildConfig.FLAVOR_market.equals("bangladesh"); }
    private void rendered(Screen screen) {
        WebView web=screen.findViewById(R.id.termsWebView);
        web.getWebViewClient().onPageFinished(web,screen.displayedPolicy.url);
    }
    private void drain() { shadowOf(Looper.getMainLooper()).idle(); }
    private void verifyEvent(Screen screen,String outcome,int times) {
        verify(screen.analytics,times(times)).trackTermsStep(argThat(b ->
                "terms_save".equals(b.getString("stage")) && outcome.equals(b.getString("outcome"))));
    }

    @Test public void pendingSaveBlocksBackAndDeclineWithoutDuplicateWritesOrFalseRefusal() {
        org.junit.Assume.assumeTrue(bd());
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            Screen screen=Robolectric.buildActivity(Screen.class,new Intent()).create().start().resume().get();
            rendered(screen);
            screen.findViewById(R.id.acceptTerms).performClick();
            screen.findViewById(R.id.acceptTerms).performClick();
            assertEquals(1,screen.writes);
            assertEquals(View.VISIBLE,screen.findViewById(R.id.termsSavingStatus).getVisibility());
            assertFalse(screen.findViewById(R.id.declineTerms).isEnabled());
            screen.getOnBackPressedDispatcher().onBackPressed();
            screen.findViewById(R.id.declineTerms).performClick();
            assertFalse(screen.isFinishing());verify(auth,never()).signOut();
            verifyEvent(screen,"submit",1);verifyEvent(screen,"declined",0);
            screen.write.setResult(null);drain();
            assertTrue(screen.isFinishing());verifyEvent(screen,"success",1);
            verifyEvent(screen,"declined",0);verify(auth,never()).signOut();
        }
    }
    @Test public void failureRestoresControlsAndAllowsRetryOrRefusal() {
        org.junit.Assume.assumeTrue(bd());
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            Screen screen=Robolectric.buildActivity(Screen.class,new Intent()).create().start().resume().get();rendered(screen);
            screen.findViewById(R.id.acceptTerms).performClick();
            screen.write.setException(new java.net.SocketTimeoutException("private details"));drain();
            assertFalse(screen.isFinishing());assertTrue(screen.findViewById(R.id.acceptTerms).isEnabled());
            assertTrue(screen.findViewById(R.id.declineTerms).isEnabled());
            assertEquals(View.GONE,screen.findViewById(R.id.termsSavingStatus).getVisibility());
            verifyEvent(screen,"failure",1);verifyEvent(screen,"declined",0);verify(auth,never()).signOut();
            screen.write=new TaskCompletionSource<>();screen.findViewById(R.id.acceptTerms).performClick();
            assertEquals(2,screen.writes);assertFalse(screen.findViewById(R.id.declineTerms).isEnabled());
            screen.write.setException(new java.io.IOException("private"));drain();
            screen.findViewById(R.id.declineTerms).performClick();
            assertTrue(screen.isFinishing());verify(auth).signOut();verifyEvent(screen,"declined",1);
        }
    }
    @Test public void recreatedScreenKeepsOriginalRequestAndNavigationGuard() {
        org.junit.Assume.assumeTrue(bd());
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            var controller=Robolectric.buildActivity(Screen.class,new Intent()).create().start().resume();
            Screen original=controller.get();rendered(original);
            original.findViewById(R.id.acceptTerms).performClick();
            var request=original.write;
            controller.recreate();Screen restored=controller.get();
            assertNotSame(original,restored);rendered(restored);
            assertEquals(View.VISIBLE,restored.findViewById(R.id.termsSavingStatus).getVisibility());
            restored.getOnBackPressedDispatcher().onBackPressed();
            assertFalse(restored.isFinishing());assertFalse(restored.findViewById(R.id.declineTerms).isEnabled());
            restored.findViewById(R.id.acceptTerms).performClick();assertEquals(0,restored.writes);
            request.setResult(null);drain();assertTrue(restored.isFinishing());
            verifyEvent(original,"submit",1);verifyEvent(restored,"submit",0);
            verifyEvent(restored,"success",1);verify(auth,never()).signOut();
        }
    }
    @Test public void completionInBackgroundWaitsForReturnWithoutResubmitting() {
        org.junit.Assume.assumeTrue(bd());
        var controller=Robolectric.buildActivity(Screen.class,new Intent()).create().start().resume();
        Screen screen=controller.get();rendered(screen);screen.findViewById(R.id.acceptTerms).performClick();
        controller.pause().stop();screen.write.setResult(null);drain();
        verifyEvent(screen,"success",0);assertFalse(screen.isFinishing());
        controller.start().resume();drain();
        verifyEvent(screen,"success",1);assertTrue(screen.isFinishing());assertEquals(1,screen.writes);
    }
    @Test public void completedOldAccountRequestDoesNotAcknowledgeNewAccount() {
        org.junit.Assume.assumeTrue(bd());
        Screen screen=Robolectric.buildActivity(Screen.class,new Intent()).create().start().resume().get();rendered(screen);
        screen.findViewById(R.id.acceptTerms).performClick();screen.account="different-account";
        screen.write.setResult(null);drain();
        assertTrue(screen.isFinishing());verifyEvent(screen,"success",0);verifyEvent(screen,"declined",0);
    }
    @Test public void globalPendingSaveShowsSharedOverlayAndKeepsExistingBackBehavior() {
        org.junit.Assume.assumeFalse(bd());
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> firebase=mockStatic(FirebaseAuth.class)) {
            firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
            Screen screen=Robolectric.buildActivity(Screen.class,new Intent()).create().start().resume().get();rendered(screen);
            screen.findViewById(R.id.acceptTerms).performClick();
            assertEquals(View.VISIBLE,screen.findViewById(R.id.termsSavingStatus).getVisibility());
            assertTrue(screen.findViewById(R.id.declineTerms).isEnabled());
            screen.getOnBackPressedDispatcher().onBackPressed();
            assertTrue(screen.isFinishing());verify(auth).signOut();
            verify(screen.analytics).trackOnboardingStep("terms_save","declined","none");
            verify(screen.analytics,never()).trackTermsStep(any(Bundle.class));
        }
    }
    @Test public void readOnlyNeverShowsSavingOrWrites() {
        Screen screen=Robolectric.buildActivity(Screen.class,new Intent().putExtra(TermsActivity.READ_ONLY,true)).create().start().resume().get();
        screen.findViewById(R.id.acceptTerms).performClick();
        assertEquals(0,screen.writes);assertEquals(View.GONE,screen.findViewById(R.id.termsSavingStatus).getVisibility());
    }
}
