package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.google.firebase.auth.FirebaseAuth;
import java.util.HashMap;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=TournamentLoginContextTest.TestApp.class,sdk=34)
public class TournamentLoginContextTest {
    public static class TestApp extends SoccerApp {
        final AnalyticsManager analytics=mock(AnalyticsManager.class);
        @Override public void onCreate() {}
        @Override public AnalyticsManager getAnalyticsManager() { return analytics; }
    }
    public static class Login extends UniversalLoginActivity {
        @Override protected void onCreate(Bundle state) { setTheme(R.style.AppTheme);super.onCreate(state); }
    }
    FirebaseAuth auth;
    MockedStatic<FirebaseAuth> firebase;
    MockedStatic<CallbackManager.Factory> facebookCallbacks;
    MockedStatic<FacebookSdk> facebook;
    MockedConstruction<FirebaseAuthManager> managers;
    TestApp app;

    @Before public void setup() {
        app=(TestApp)RuntimeEnvironment.getApplication();
        app.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit().clear().commit();
        auth=mock(FirebaseAuth.class);firebase=mockStatic(FirebaseAuth.class);
        firebase.when(FirebaseAuth::getInstance).thenReturn(auth);
        facebookCallbacks=mockStatic(CallbackManager.Factory.class);
        facebookCallbacks.when(CallbackManager.Factory::create).thenReturn(mock(CallbackManager.class));
        facebook=mockStatic(FacebookSdk.class);facebook.when(FacebookSdk::isInitialized).thenReturn(true);
        managers=mockConstruction(FirebaseAuthManager.class);
    }
    @After public void cleanup() { managers.close();facebook.close();facebookCallbacks.close();firebase.close(); }
    private boolean bd(){return AnalyticsManager.isBangladesh();}
    private Login open(Intent intent) {
        return Robolectric.buildActivity(Login.class,intent).create().start().resume().get();
    }
    private void checkContext(Login login,boolean expected) {
        View context=login.findViewById(R.id.tournamentLoginContext);
        if(!bd()) { assertNull(context);return; }
        assertEquals(expected?View.VISIBLE:View.GONE,context.getVisibility());
        if(expected) {
            assertEquals(login.getString(R.string.bd_tournament_login_title),
                    ((TextView)login.findViewById(R.id.tournamentLoginTitle)).getText().toString());
            assertEquals(login.getString(R.string.bd_tournament_login_message),
                    ((TextView)login.findViewById(R.id.tournamentLoginMessage)).getText().toString());
        }
        assertEquals(login.getString(login.getResources().getIdentifier("bd_start_playing","string",BuildConfig.APPLICATION_ID)),
                ((TextView)login.findViewById(R.id.btnUniversalAnonymous)).getText().toString());
    }
    @Test public void promotionLaunchShowsContextAndNormalLoginRemainsNeutralEvenWithPendingSelection() {
        PendingTournamentPromotion.select(app,"cup",null,System.currentTimeMillis());
        var prefs=app.getSharedPreferences(LanguageManager.PREFS_FILE,0);
        var before=new HashMap<>(prefs.getAll());
        Login tournament=open(PendingTournamentPromotion.loginIntent(app));checkContext(tournament,true);
        verify(app.analytics).trackLoginScreenOpened(bd()?"tournament":"account");
        assertEquals(before,prefs.getAll());clearInvocations(app.analytics);
        Login normal=open(new Intent());checkContext(normal,false);
        verify(app.analytics).trackLoginScreenOpened("account");assertEquals(before,prefs.getAll());
        assertFalse(normal.isFinishing());
    }
    @Test public void missingExpiredFutureOrOwnedSelectionCannotImplyTournamentContext() {
        Intent intent=PendingTournamentPromotion.loginIntent(app);
        checkContext(open(intent),false);
        PendingTournamentPromotion.select(app,"cup",null,System.currentTimeMillis()-86400001L);
        checkContext(open(intent),false);
        PendingTournamentPromotion.select(app,"cup",null,System.currentTimeMillis()+60000L);
        checkContext(open(intent),false);
        PendingTournamentPromotion.select(app,"cup","other-owner",System.currentTimeMillis());
        checkContext(open(intent),false);
    }
    @Test public void contextSurvivesRecreationAndBengaliCopyIsLocalized() {
        app.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit()
                .putString(LanguageManager.PREF_LANGUAGE_CODE,"bn").commit();
        PendingTournamentPromotion.select(app,"cup",null,System.currentTimeMillis());
        var controller=Robolectric.buildActivity(Login.class,PendingTournamentPromotion.loginIntent(app)).create().start().resume();
        checkContext(controller.get(),true);controller.recreate();checkContext(controller.get(),true);
        if(bd()) {
            assertEquals("টুর্নামেন্টে যোগ দিন",((TextView)controller.get().findViewById(R.id.tournamentLoginTitle)).getText().toString());
            assertEquals("নির্বাচিত টুর্নামেন্টে এগিয়ে যেতে লগইন করুন অথবা খেলা শুরু করুন।",
                    ((TextView)controller.get().findViewById(R.id.tournamentLoginMessage)).getText().toString());
        }
    }
    @Test public void authenticationStillFinishesLoginAndRetainsTheOriginalSelection() {
        PendingTournamentPromotion.select(app,"cup",null,System.currentTimeMillis());
        Login login=open(PendingTournamentPromotion.loginIntent(app));
        login.findViewById(R.id.btnUniversalAnonymous).performClick();
        ((AlertDialog)ShadowDialog.getLatestDialog()).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        var callback=ArgumentCaptor.forClass(FirebaseAuthManager.LoginCallback.class);
        verify(managers.constructed().get(0)).loginAnonymously(callback.capture());
        when(auth.getUid()).thenReturn("owner");callback.getValue().onLoginSuccess();
        assertTrue(login.isFinishing());
        assertEquals("cup",PendingTournamentPromotion.forUser(app,"owner",System.currentTimeMillis()));
        assertNull(Shadows.shadowOf(login).getNextStartedActivity());
    }
    @Test public void normalProviderAndEmailButtonsKeepTheirExistingDestinations() {
        Login login=open(new Intent());FirebaseAuthManager manager=managers.constructed().get(0);
        login.findViewById(R.id.btnUniversalGoogle).performClick();
        verify(manager).loginWithProvider(eq(login),eq("google.com"),isNull(),any());
        // A separate visit has enabled buttons, as before.
        Login email=open(new Intent());email.findViewById(R.id.btnUniversalEmail).performClick();
        assertEquals(LoginActivity.class.getName(),Shadows.shadowOf(email).getNextStartedActivity().getComponent().getClassName());
        assertEquals(View.VISIBLE,email.findViewById(R.id.btnUniversalFacebook).getVisibility());
    }
    @Test public void loginProgressIsSharedAndFailureRestoresButtons() {
        Login login=open(new Intent());
        View overlay=login.findViewById(R.id.loginProgressOverlay);
        assertNotNull(overlay);assertEquals(View.GONE,overlay.getVisibility());
        assertEquals(login.getString(R.string.login_in_progress),
                ((TextView)overlay.findViewById(R.id.game_loading_message)).getText().toString());
        login.findViewById(R.id.btnUniversalGoogle).performClick();
        assertEquals(View.VISIBLE,overlay.getVisibility());
        var callback=ArgumentCaptor.forClass(FirebaseAuthManager.LoginCallback.class);
        verify(managers.constructed().get(0)).loginWithProvider(eq(login),eq("google.com"),isNull(),callback.capture());
        callback.getValue().onLoginFailure("cancelled");
        assertEquals(View.GONE,overlay.getVisibility());
        assertTrue(login.findViewById(R.id.btnUniversalAnonymous).isEnabled());
        assertFalse(login.isFinishing());
    }
    @Test public void cancellingPromotionLoginReturnsToMenuAndExistingResultHandlerClearsSelection() {
        org.junit.Assume.assumeTrue(bd());
        MenuActivity menu=Robolectric.buildActivity(MenuActivity.class).get();menu.setTheme(R.style.AppTheme);
        PendingTournamentPromotion.select(menu,"cup",null,System.currentTimeMillis());
        menu.launchTournamentPromotionLogin();
        var launch=Shadows.shadowOf(menu).getNextStartedActivityForResult();
        assertEquals(UniversalLoginActivity.class.getName(),launch.intent.getComponent().getClassName());
        Login login=open(launch.intent);checkContext(login,true);
        login.getOnBackPressedDispatcher().onBackPressed();assertTrue(login.isFinishing());
        menu.onTournamentPromotionLoginReturned();
        assertFalse(PendingTournamentPromotion.isLoginEntry(menu,launch.intent,System.currentTimeMillis()));
        assertFalse(menu.isFinishing());
        verify(managers.constructed().get(0),never()).loginAnonymously(any());
    }
}
