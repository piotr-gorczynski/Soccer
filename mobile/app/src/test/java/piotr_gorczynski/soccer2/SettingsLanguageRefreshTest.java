package piotr_gorczynski.soccer2;

import android.app.AlertDialog;
import android.content.Context;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=AccountSignedOutTest.TestApp.class, sdk=30, qualifiers="en")
public class SettingsLanguageRefreshTest {
    @Test public void englishToBengaliRefreshesSettingsWithoutLeaving() throws Exception {
        checkChange("en", "bn");
    }
    @Test public void polishToSomaliRefreshesSettingsWithoutEnglishFallback() throws Exception {
        checkChange("pl", "so");
    }
    @Test public void bengaliToEnglishRefreshesSettings() throws Exception {
        checkChange("bn", "en");
    }
    @Test public void englishToPolishRefreshesSettings() throws Exception {
        checkChange("en", "pl");
    }

    private void checkChange(String initial, String target) throws Exception {
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try (MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class);
             MockedStatic<FirebaseFirestore> fs=mockStatic(FirebaseFirestore.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);
            fs.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));
            Context app=androidx.test.core.app.ApplicationProvider.getApplicationContext();
            app.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit()
                    .putString(LanguageManager.PREF_LANGUAGE_CODE, initial).commit();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(initial));
            ActivityController<SettingsActivity> controller=Robolectric.buildActivity(SettingsActivity.class);
            controller.get().setTheme(R.style.AppTheme);
            controller.create().start().resume().visible();
            SettingsActivity activity=controller.get();
            activity.getSupportFragmentManager().executePendingTransactions();
            SettingsFragment fragment=(SettingsFragment)activity.getSupportFragmentManager()
                    .findFragmentById(R.id.settings_container);
            String[] names=LanguageManager.getAvailableLanguages(activity);
            int selection=-1;
            for(int i=0;i<names.length;i++) {
                if(target.equals(LanguageManager.getLanguageCodeFromLocalizedName(activity,names[i]))) selection=i;
            }
            assertTrue(selection>=0);
            java.lang.reflect.Method show=SettingsFragment.class.getDeclaredMethod("showLanguageSelectionDialog");
            show.setAccessible(true); show.invoke(fragment);
            AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();
            dialog.getListView().performItemClick(null,selection,selection);
            ShadowLooper.idleMainLooper();
            assertTrue("The old Settings screen must finish",activity.isFinishing());
            android.content.Intent reopened=shadowOf(activity).getNextStartedActivity();
            assertNotNull("Settings must reopen without user navigation",reopened);
            assertEquals(SettingsActivity.class.getName(),reopened.getComponent().getClassName());
            controller.pause().stop().destroy();
            controller=Robolectric.buildActivity(SettingsActivity.class,reopened);
            controller.get().setTheme(R.style.AppTheme);
            controller.create().start().resume().visible();
            activity=controller.get();
            activity.getSupportFragmentManager().executePendingTransactions();
            assertEquals(target,LanguageManager.getCurrentLanguageCode(activity));
            assertEquals(target,activity.getResources().getConfiguration().getLocales().get(0).getLanguage());
            Context expected=LanguageManager.applyLanguage(app,target);
            fragment=(SettingsFragment)activity.getSupportFragmentManager().findFragmentById(R.id.settings_container);
            assertEquals(expected.getString(R.string.pref_title_language),
                    fragment.findPreference("language_preference").getTitle().toString());
            controller.pause().stop().destroy();
        } finally {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList());
        }
    }
}
