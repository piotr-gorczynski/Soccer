package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.UserMessagingPlatform;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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
@Config(application=MenuAdNavigationTest.TestApp.class, sdk=34)
public class MenuAdNavigationTest {
    public static class TestApp extends SoccerApp {
        @Override public void onCreate() {}
        @Override public boolean canRequestAds() { return false; }
        @Override public boolean isStartupConsentFinished() { return true; }
    }
    public static class TestMenu extends MenuActivity {
        boolean ready;
        final List<TaskCompletionSource<Boolean>> reads = new ArrayList<>();
        @Override protected boolean canContinueAfterAd() { return ready; }
        @Override protected Task<Boolean> readTermsAcceptance(String uid, TermsPolicy policy) {
            TaskCompletionSource<Boolean> result = new TaskCompletionSource<>();
            reads.add(result); return result.getTask();
        }
    }
    public static class RouteMenu extends MenuActivity {
        Runnable selected;
        Intent target;
        int gates;
        @Override public void showAdThenRun(Runnable action) { selected = action; gates++; }
        @Override public void startActivity(Intent intent) { target = intent; }
    }

    private TestMenu menu;
    private FirebaseAuth auth;
    private ConsentInformation consent;
    private SharedPreferences prefs;
    private InterstitialAd ad;
    private MockedStatic<FirebaseAuth> firebase;
    private MockedStatic<BangladeshMigrationHelper> migration;
    private MockedStatic<UserMessagingPlatform> ump;

    @Before public void setup() throws Exception {
        auth = mock(FirebaseAuth.class);
        when(auth.getUid()).thenReturn("owner");
        when(auth.getCurrentUser()).thenReturn(mock(FirebaseUser.class));
        firebase = mockStatic(FirebaseAuth.class);
        firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
        migration = mockStatic(BangladeshMigrationHelper.class);
        consent = mock(ConsentInformation.class);
        when(consent.getConsentStatus()).thenReturn(ConsentInformation.ConsentStatus.OBTAINED);
        ump = mockStatic(UserMessagingPlatform.class);
        ump.when(() -> UserMessagingPlatform.getConsentInformation(any())).thenReturn(consent);
        menu = Robolectric.buildActivity(TestMenu.class).get();
        menu.setTheme(R.style.AppTheme);
        menu.setContentView(R.layout.activity_menu);
        prefs = menu.getSharedPreferences(LanguageManager.PREFS_FILE, 0);
        prefs.edit().clear().commit();
        ad = mock(InterstitialAd.class);
        set("mInterstitialAd", ad);
    }
    @After public void cleanup() { ump.close(); migration.close(); firebase.close(); }

    private void set(String name, Object value) throws Exception {
        Field f = MenuActivity.class.getDeclaredField(name);
        f.setAccessible(true); f.set(menu, value);
    }
    private void call(String name) throws Exception {
        Method m = MenuActivity.class.getDeclaredMethod(name);
        m.setAccessible(true); m.invoke(menu);
    }
    private FullScreenContentCallback show(Runnable action, int frequency) throws Exception {
        Method m = MenuActivity.class.getDeclaredMethod("processAdLogic", Runnable.class,
                SharedPreferences.class, int.class);
        m.setAccessible(true); m.invoke(menu, action, prefs, frequency);
        ArgumentCaptor<FullScreenContentCallback> cb = ArgumentCaptor.forClass(FullScreenContentCallback.class);
        verify(ad).setFullScreenContentCallback(cb.capture());
        verify(ad).show(menu);
        return cb.getValue();
    }
    private void checkTerms() throws Exception {
        Method m = MenuActivity.class.getDeclaredMethod("checkTerms", String.class, Runnable.class);
        m.setAccessible(true); m.invoke(menu, "owner", null);
    }
    private void drain() { shadowOf(Looper.getMainLooper()).idle(); }

    @Test public void dismissalBeforeMenuResumesWaitsThenOpensSelectedScreenOnce() throws Exception {
        FullScreenContentCallback cb = show(() -> menu.startActivity(new Intent(menu, FriendsListActivity.class)), 1);
        cb.onAdDismissedFullScreenContent(); drain();
        assertEquals(0, menu.reads.size());
        assertNull(shadowOf(menu).getNextStartedActivity());
        menu.ready = true; call("runPendingAdContinuation");
        assertEquals(1, menu.reads.size());
        menu.reads.get(0).setResult(true); drain();
        assertEquals(FriendsListActivity.class.getName(), shadowOf(menu).getNextStartedActivity().getComponent().getClassName());
        call("runPendingAdContinuation");
        assertNull(shadowOf(menu).getNextStartedActivity());
    }

    @Test public void resumeRestartingTermsCheckKeepsDestinationAndIgnoresOldResponse() throws Exception {
        menu.startActivity(new Intent(menu, FriendsListActivity.class));
        assertEquals(1, menu.reads.size());
        set("serviceChecker", mock(BackendServiceChecker.class));
        call("checkBackendAvailabilityAndContinue"); // Restart/generation change used on menu resume.
        checkTerms();
        assertEquals(2, menu.reads.size());
        menu.reads.get(0).setResult(true); drain();
        assertNull(shadowOf(menu).getNextStartedActivity());
        menu.reads.get(1).setResult(true); drain();
        assertEquals(FriendsListActivity.class.getName(), shadowOf(menu).getNextStartedActivity().getComponent().getClassName());
        assertNull(shadowOf(menu).getNextStartedActivity());
    }

    @Test public void oldDestinationIsNotTransferredToAnotherAccountDuringResume() throws Exception {
        menu.startActivity(new Intent(menu, FriendsListActivity.class));
        set("serviceChecker", mock(BackendServiceChecker.class));
        call("checkBackendAvailabilityAndContinue");
        when(auth.getUid()).thenReturn("another-owner");
        Method m = MenuActivity.class.getDeclaredMethod("checkTerms", String.class, Runnable.class);
        m.setAccessible(true); m.invoke(menu, "another-owner", null);
        menu.reads.get(1).setResult(true); drain();
        assertNull(shadowOf(menu).getNextStartedActivity());
    }

    @Test public void resumedMenuContinuesImmediatelyAndDuplicateCallbacksCannotRunTwice() throws Exception {
        menu.ready = true;
        AtomicInteger runs = new AtomicInteger();
        FullScreenContentCallback cb = show(runs::incrementAndGet, 1);
        cb.onAdShowedFullScreenContent();
        cb.onAdDismissedFullScreenContent();
        cb.onAdFailedToShowFullScreenContent(new AdError(1, "test", "test"));
        cb.onAdDismissedFullScreenContent();
        assertEquals(1, runs.get());
        assertEquals(0, prefs.getInt("adsCounter", -1));
    }

    @Test public void showFailureAlsoContinuesOnceAfterFocusReturns() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        FullScreenContentCallback cb = show(runs::incrementAndGet, 1);
        cb.onAdFailedToShowFullScreenContent(new AdError(1, "test", "test"));
        assertEquals(0, runs.get());
        menu.ready = true; call("runPendingAdContinuation");
        cb.onAdDismissedFullScreenContent(); call("runPendingAdContinuation");
        assertEquals(1, runs.get());
    }

    @Test public void usedAdIsConsumedBeforeShowAndCannotClearNextCachedAd() throws Exception {
        FullScreenContentCallback cb = show(() -> {}, 1);
        InterstitialAd next = mock(InterstitialAd.class);
        set("mInterstitialAd", next);
        cb.onAdShowedFullScreenContent();
        Field f = MenuActivity.class.getDeclaredField("mInterstitialAd"); f.setAccessible(true);
        assertSame(next, f.get(menu));
    }

    @Test public void frequencyBelowThresholdStillRunsActionWithoutShowingAd() throws Exception {
        AtomicInteger runs = new AtomicInteger();
        Method m = MenuActivity.class.getDeclaredMethod("processAdLogic", Runnable.class, SharedPreferences.class, int.class);
        m.setAccessible(true); m.invoke(menu, (Runnable) runs::incrementAndGet, prefs, 10);
        assertEquals(1, runs.get());
        assertEquals(1, prefs.getInt("adsCounter", 0));
        verify(ad, never()).show(any());
    }

    @Test public void noCachedAdOrLostConsentDoesNotBlockAction() throws Exception {
        set("mInterstitialAd", null);
        AtomicInteger runs = new AtomicInteger();
        Method m = MenuActivity.class.getDeclaredMethod("processAdLogic", Runnable.class, SharedPreferences.class, int.class);
        m.setAccessible(true); m.invoke(menu, (Runnable) runs::incrementAndGet, prefs, 1);
        assertEquals(1, runs.get());
        when(consent.getConsentStatus()).thenReturn(ConsentInformation.ConsentStatus.REQUIRED);
        set("mInterstitialAd", ad);
        m.invoke(menu, (Runnable) runs::incrementAndGet, prefs, 1);
        assertEquals(2, runs.get());
        verify(ad, never()).show(any());
    }

    @Test public void duplicateMenuClickCannotReplacePendingAdDestination() throws Exception {
        set("adFlowInProgress", true);
        AtomicInteger runs = new AtomicInteger();
        menu.showAdThenRun(runs::incrementAndGet);
        assertEquals(0, runs.get());
    }

    @Test public void myPrizesUsesSameAdGateAsOtherMenuDestinations() {
        RouteMenu route = Robolectric.buildActivity(RouteMenu.class).get();
        try (MockedStatic<AppFlavourDetector> flavour = mockStatic(AppFlavourDetector.class)) {
            flavour.when(() -> AppFlavourDetector.supportsPrizeFeatures(route)).thenReturn(true);
            route.OpenMyPrizes(null);
            assertEquals(1, route.gates);
            assertNull(route.target);
            route.selected.run();
            assertEquals(MyPrizesActivity.class.getName(), route.target.getComponent().getClassName());
        }
    }

    @Test public void unsupportedMarketDoesNotExposeMyPrizesOrCountAnAdAction() {
        RouteMenu route = Robolectric.buildActivity(RouteMenu.class).get();
        try (MockedStatic<AppFlavourDetector> flavour = mockStatic(AppFlavourDetector.class)) {
            route.OpenMyPrizes(null);
            assertEquals(0, route.gates);
            assertNull(route.target);
        }
    }
}
