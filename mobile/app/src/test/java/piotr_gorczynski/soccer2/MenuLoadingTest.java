package piotr_gorczynski.soccer2;

import android.os.Looper;
import android.view.View;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=MenuLoadingTest.TestApp.class,sdk=34)
public class MenuLoadingTest {
    public static class TestApp extends SoccerApp {
        @Override public void onCreate() {}
        @Override public synchronized void unregisterFromFcm() {}
    }
    public static class TestMenu extends MenuActivity {
        final TaskCompletionSource<DocumentSnapshot> profile=new TaskCompletionSource<>();
        final TaskCompletionSource<Boolean> terms=new TaskCompletionSource<>();
        int profileReads;
        @Override protected Task<DocumentSnapshot> loadMenuProfile(String uid) { profileReads++;return profile.getTask(); }
        @Override protected Task<Boolean> readTermsAcceptance(String uid,TermsPolicy policy) {return terms.getTask();}
    }
    TestMenu menu;
    FirebaseAuth auth;
    BackendServiceChecker checker;
    MockedStatic<FirebaseAuth> firebase;
    MockedStatic<BangladeshMigrationHelper> migration;
    View overlay;
    @Before public void setup() throws Exception {
        auth=mock(FirebaseAuth.class);when(auth.getUid()).thenReturn("owner");
        when(auth.getCurrentUser()).thenReturn(mock(FirebaseUser.class));
        firebase=mockStatic(FirebaseAuth.class);firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
        migration=mockStatic(BangladeshMigrationHelper.class);
        menu=Robolectric.buildActivity(TestMenu.class).get();menu.setTheme(R.style.AppTheme);
        menu.setContentView(R.layout.activity_menu);
        menu.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit().clear().commit();
        overlay=menu.findViewById(R.id.menu_loading_overlay);set("loadingOverlay",overlay);
        checker=mock(BackendServiceChecker.class);set("serviceChecker",checker);
    }
    @After public void cleanup() { firebase.close();migration.close(); }
    void set(String name,Object value) throws Exception {
        Field field=MenuActivity.class.getDeclaredField(name);field.setAccessible(true);field.set(menu,value);
    }
    void call(String name) throws Exception {
        Method method=MenuActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(menu);
    }
    BackendServiceChecker.ServiceCheckCallback begin() throws Exception {
        call("checkBackendAvailabilityAndContinue");
        ArgumentCaptor<BackendServiceChecker.ServiceCheckCallback> cb=ArgumentCaptor.forClass(BackendServiceChecker.ServiceCheckCallback.class);
        verify(checker,atLeastOnce()).checkServiceAvailability(cb.capture());return cb.getValue();
    }
    void profileReady() {
        DocumentSnapshot doc=mock(DocumentSnapshot.class);when(doc.exists()).thenReturn(true);
        when(doc.getString("nickname")).thenReturn("existing-player");
        menu.profile.setResult(doc);shadowOf(Looper.getMainLooper()).idle();
    }
    void settle() { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(500)); }
    @Test public void initialLayoutCoversButtonsBeforeAnyStatusCallback() {
        assertEquals(View.VISIBLE,overlay.getVisibility());
        assertEquals(menu.getColor(R.color.colorWhite),((android.graphics.drawable.ColorDrawable)overlay.getBackground()).getColor());
    }
    @Test public void menuWaitsForBothProfileAndTermsBeforeShowingFinalOnlineState() throws Exception {
        BackendServiceChecker.ServiceCheckCallback cb=begin();settle();assertEquals(View.VISIBLE,overlay.getVisibility());
        cb.onServiceAvailable();shadowOf(Looper.getMainLooper()).idle();
        profileReady();settle();assertEquals(View.VISIBLE,overlay.getVisibility());
        menu.terms.setResult(true);settle();assertEquals(View.GONE,overlay.getVisibility());
        assertTrue(menu.findViewById(R.id.openTournamentsBtn).isEnabled());
        assertEquals(1f,menu.findViewById(R.id.openTournamentsBtn).getAlpha(),0f);
        assertTrue(((android.widget.TextView)menu.findViewById(R.id.nicknameLabel)).getText().toString().contains("existing-player"));
    }
    @Test public void termsCompletingFirstDoesNotRevealPendingProfile() throws Exception {
        begin().onServiceAvailable();shadowOf(Looper.getMainLooper()).idle();
        menu.terms.setResult(true);settle();assertEquals(View.VISIBLE,overlay.getVisibility());
        profileReady();settle();assertEquals(View.GONE,overlay.getVisibility());
    }
    @Test public void unavailableBackendRevealsUsableOfflineMenuWithoutProfileReads() throws Exception {
        begin().onServiceUnavailable("network");settle();
        assertEquals(View.GONE,overlay.getVisibility());assertEquals(0,menu.profileReads);
        assertFalse(menu.findViewById(R.id.openTournamentsBtn).isEnabled());
        assertTrue(menu.findViewById(R.id.youVsAndroidBtn).isEnabled());
    }
    @Test public void failedProfileRevealsOfflineMenuWithoutWaitingForHangingTerms() throws Exception {
        begin().onServiceAvailable();shadowOf(Looper.getMainLooper()).idle();
        menu.profile.setException(new IllegalStateException("unavailable"));settle();
        assertEquals(View.GONE,overlay.getVisibility());assertFalse(menu.findViewById(R.id.openTournamentsBtn).isEnabled());
        menu.terms.setResult(true);settle();assertEquals(View.GONE,overlay.getVisibility());
        assertNull(shadowOf(menu).getNextStartedActivity());
    }
    @Test public void timeoutReleasesOfflineMenuAndIgnoresLateOnlineCallback() throws Exception {
        BackendServiceChecker.ServiceCheckCallback late=begin();
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(21));
        assertEquals(View.GONE,overlay.getVisibility());assertFalse(menu.findViewById(R.id.openTournamentsBtn).isEnabled());
        late.onServiceAvailable();settle();
        assertEquals(0,menu.profileReads);assertEquals(View.GONE,overlay.getVisibility());
        assertFalse(menu.findViewById(R.id.openTournamentsBtn).isEnabled());
    }
    @Test public void adCompletionCannotUncoverUnresolvedMenu() throws Exception {
        begin().onServiceAvailable();shadowOf(Looper.getMainLooper()).idle();
        call("showLoadingOverlay");call("hideLoadingOverlayWithMinimumDuration");settle();
        assertEquals(View.VISIBLE,overlay.getVisibility());
        profileReady();menu.terms.setResult(true);settle();assertEquals(View.GONE,overlay.getVisibility());
    }
    @Test public void menuCompletionCannotUncoverPendingAdTransition() throws Exception {
        begin().onServiceAvailable();shadowOf(Looper.getMainLooper()).idle();call("showLoadingOverlay");
        profileReady();menu.terms.setResult(true);settle();assertEquals(View.VISIBLE,overlay.getVisibility());
        call("hideLoadingOverlayWithMinimumDuration");settle();assertEquals(View.GONE,overlay.getVisibility());
    }
    @Test public void newResumeCancelsPreviouslyScheduledReveal() throws Exception {
        begin().onServiceAvailable();shadowOf(Looper.getMainLooper()).idle();
        profileReady();menu.terms.setResult(true);shadowOf(Looper.getMainLooper()).idle();
        call("coverMenuWhileResolving");settle();assertEquals(View.VISIBLE,overlay.getVisibility());
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS,
                menu.findViewById(R.id.menu_content).getImportantForAccessibility());
    }
}
