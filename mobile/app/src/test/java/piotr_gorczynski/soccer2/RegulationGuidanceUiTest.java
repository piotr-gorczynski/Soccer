package piotr_gorczynski.soccer2;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=RegulationGuidanceUiTest.TestApp.class,sdk=30,qualifiers="w800dp-h1000dp-mdpi")
public class RegulationGuidanceUiTest {
    public static class TestApp extends SoccerApp {
        final AnalyticsManager analytics=mock(AnalyticsManager.class);
        @Override public void onCreate() {}
        @Override public AnalyticsManager getAnalyticsManager(){return analytics;}
    }
    public static class Screen extends RegulationActivity {
        @Override protected void onCreate(Bundle b){setTheme(R.style.AppTheme);super.onCreate(b);}
        @Override public boolean hasWindowFocus(){return true;}
    }
    MockedStatic<FirebaseAuth> authSdk;
    MockedStatic<FirebaseFirestore> firestoreSdk;
    DocumentReference reference,translation;
    DocumentSnapshot document;
    ActivityController<Screen> controller;
    Screen a;
    AnalyticsManager analytics;
    @Before public void setup() {
        var app=(TestApp)RuntimeEnvironment.getApplication();
        app.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit().clear().commit();
        analytics=app.analytics;
        var auth=mock(FirebaseAuth.class);when(auth.getCurrentUser()).thenReturn(mock(FirebaseUser.class));
        var db=mock(FirebaseFirestore.class,RETURNS_DEEP_STUBS);
        authSdk=mockStatic(FirebaseAuth.class);authSdk.when(FirebaseAuth::getInstance).thenReturn(auth);
        firestoreSdk=mockStatic(FirebaseFirestore.class);firestoreSdk.when(FirebaseFirestore::getInstance).thenReturn(db);
        reference=db.collection("regulations").document("rules");
        translation=mock(DocumentReference.class);var languages=mock(CollectionReference.class);
        when(reference.collection(anyString())).thenReturn(languages);when(languages.document("rules")).thenReturn(translation);
        document=mock(DocumentSnapshot.class);when(document.exists()).thenReturn(true);
        when(document.getReference()).thenReturn(reference);when(document.getString("name")).thenReturn("Cash tournament rules");
        when(document.getData()).thenReturn(Map.of("market","BD","minimumAge",18,
                "prizeRules",Map.of("cashPrizesEnabled",true,"payoutMethods",List.of("bkash","rocket"))));
        var localized=mock(DocumentSnapshot.class);when(localized.exists()).thenReturn(true);
        when(localized.get("rules")).thenReturn(List.of("Full regulation text remains here.\n".repeat(100)));
        when(reference.get()).thenReturn(Tasks.forResult(document));when(translation.get()).thenReturn(Tasks.forResult(localized));
    }
    @After public void cleanup() {
        if(controller!=null)controller.pause().stop().destroy();
        firestoreSdk.close();authSdk.close();
        android.provider.Settings.Global.putFloat(RuntimeEnvironment.getApplication().getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,1f);
    }
    void bd(){Assume.assumeTrue(AnalyticsManager.isBangladesh());}
    void open() {
        controller=Robolectric.buildActivity(Screen.class,new Intent().putExtra("regulationId","rules").putExtra("tournamentId","cup"))
                .create().start().resume().visible();a=controller.get();drain();layout();
    }
    void drain(){shadowOf(android.os.Looper.getMainLooper()).idle();}
    void layout() {
        View decor=a.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));
        decor.layout(0,0,800,1000);decor.getViewTreeObserver().dispatchOnGlobalLayout();drain();
    }
    TextView hint(){return a.findViewById(R.id.eligibilityConfirmationHint);}
    ScrollView scroll(){return a.findViewById(R.id.regulationScroll);}
    void bottom(){scroll().scrollTo(0,scroll().getChildAt(0).getHeight());scroll().getViewTreeObserver().dispatchOnGlobalLayout();drain();}
    String counter(int n){return a.getString(R.string.bd_regulation_confirmations_count,n);}
    String visibilityState() {
        View box=a.findViewById(R.id.confirmMinimumAge);android.graphics.Rect rect=new android.graphics.Rect();
        return "scroll="+scroll().getScrollY()+"/"+scroll().getHeight()+" child="+scroll().getChildAt(0).getHeight()
                +" boxShown="+box.isShown()+" boxVisible="+box.getGlobalVisibleRect(rect)+" rect="+rect;
    }
    @Test public void beforeVisibilityHintIsPersistentAndCheckboxesRemainBelowTheFullRules() {
        bd();open();
        assertEquals(a.getString(R.string.bd_regulation_scroll_hint),hint().getText().toString());
        assertEquals(View.VISIBLE,hint().getVisibility());assertTrue(hint().isClickable());
        assertSame(scroll().getParent(),hint().getParent());
        assertSame(a.findViewById(R.id.regulationBody).getParent(),a.findViewById(R.id.eligibilityConfirmation).getParent());
        assertTrue(a.findViewById(R.id.eligibilityConfirmation).getTop()>a.findViewById(R.id.regulationBody).getTop());
        assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
        assertEquals(View.VISIBLE,a.findViewById(R.id.regulationJoinHeading).getVisibility());
    }
    @Test public void tappingHintSmoothlyReachesConfirmationsAndChangesToZeroOfThree() {
        bd();open();assertEquals(0,scroll().getScrollY());
        hint().performClick();scroll().computeScroll();
        shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(600));scroll().computeScroll();
        scroll().getViewTreeObserver().dispatchOnGlobalLayout();drain();
        assertTrue(scroll().getScrollY()>0);assertEquals(visibilityState(),counter(0),hint().getText().toString());
        assertFalse(hint().isClickable());assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
    }
    @Test public void counterTracksEveryCheckboxAndAcceptRequiresExactlyAllThree() {
        bd();open();bottom();assertEquals(visibilityState(),counter(0),hint().getText().toString());
        int[] ids={R.id.confirmMinimumAge,R.id.confirmPayoutAccount,R.id.confirmTournamentTerms};
        for(int i=0;i<3;i++) {
            a.findViewById(ids[i]).performClick();
            assertEquals(counter(i+1),hint().getText().toString());
            assertEquals(i==2,a.findViewById(R.id.acceptRegulation).isEnabled());
        }
        assertEquals(a.getString(R.string.bd_regulation_accept_join),((TextView)a.findViewById(R.id.acceptRegulation)).getText());
        a.findViewById(ids[0]).performClick();assertEquals(counter(2),hint().getText().toString());
        assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
        verify(analytics,never()).trackTournamentJoinSubmit();
    }
    @Test public void recreationRestoresCounterScrollAndCheckedStateWithoutPreselectingFreshConsent() {
        bd();open();bottom();a.findViewById(R.id.confirmMinimumAge).performClick();a.findViewById(R.id.confirmPayoutAccount).performClick();
        controller.recreate();a=controller.get();drain();layout();
        assertEquals(counter(2),hint().getText().toString());assertTrue(scroll().getScrollY()>0);
        assertTrue(((CheckBox)a.findViewById(R.id.confirmMinimumAge)).isChecked());
        assertTrue(((CheckBox)a.findViewById(R.id.confirmPayoutAccount)).isChecked());
        assertFalse(((CheckBox)a.findViewById(R.id.confirmTournamentTerms)).isChecked());
        assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
        a.findViewById(R.id.confirmTournamentTerms).performClick();
        controller.recreate();a=controller.get();drain();layout();
        assertEquals(counter(3),hint().getText().toString());assertTrue(a.findViewById(R.id.acceptRegulation).isEnabled());
    }
    @Test public void asynchronousReloadCannotEnableAcceptBeforeTheRegulationArrives() {
        bd();open();bottom();a.findViewById(R.id.confirmMinimumAge).performClick();
        var pending=new TaskCompletionSource<DocumentSnapshot>();when(reference.get()).thenReturn(pending.getTask());
        controller.recreate();a=controller.get();drain();
        assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());assertEquals(View.GONE,hint().getVisibility());
        pending.setResult(document);drain();layout();
        assertEquals(counter(1),hint().getText().toString());assertFalse(a.findViewById(R.id.acceptRegulation).isEnabled());
    }
    @Test public void reducedMotionDisablesPulseAndRecreationDoesNotRepeatIt() throws Exception {
        bd();android.provider.Settings.Global.putFloat(RuntimeEnvironment.getApplication().getContentResolver(),
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,0f);open();
        var field=RegulationActivity.class.getDeclaredField("guidancePulse");field.setAccessible(true);
        assertNull(field.get(a));
        controller.recreate();a=controller.get();drain();layout();assertNull(field.get(a));
    }
    @Test public void globalPreservesOriginalHintAcceptLabelAndHasNoNewHeading() {
        Assume.assumeFalse(AnalyticsManager.isBangladesh());open();
        assertEquals(a.getString(R.string.eligibility_confirmation_hint),hint().getText().toString());
        assertFalse(hint().isClickable());assertEquals(View.GONE,a.findViewById(R.id.regulationJoinHeading).getVisibility());
        assertEquals(a.getString(R.string.accept),((TextView)a.findViewById(R.id.acceptRegulation)).getText());
        bottom();a.findViewById(R.id.confirmMinimumAge).performClick();
        assertEquals(a.getString(R.string.eligibility_confirmation_hint),hint().getText().toString());
    }
    @Test public void nonCashBangladeshKeepsTheOriginalPresentationAndBengaliCopyExists() {
        bd();when(document.getData()).thenReturn(Map.of("market","BD"));open();
        assertEquals(View.GONE,hint().getVisibility());assertEquals(View.GONE,a.findViewById(R.id.regulationJoinHeading).getVisibility());
        assertEquals(a.getString(R.string.accept),((TextView)a.findViewById(R.id.acceptRegulation)).getText());
        var configuration=new android.content.res.Configuration(a.getResources().getConfiguration());
        configuration.setLocale(java.util.Locale.forLanguageTag("bn"));var bn=a.createConfigurationContext(configuration);
        assertEquals("যোগ্যতা নিশ্চিত করতে নিচে স্ক্রল করুন ↓",bn.getString(R.string.bd_regulation_scroll_hint));
        assertEquals("টুর্নামেন্টে যোগ দেওয়ার শেষ ধাপ",bn.getString(R.string.bd_regulation_last_step));
    }
}
