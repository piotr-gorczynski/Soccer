package piotr_gorczynski.soccer2;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import org.junit.Test;
import org.junit.After;
import org.robolectric.android.controller.ActivityController;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk=30, qualifiers="en")
public class BangladeshLanguageSelectionTest {
    private ActivityController<LanguageSelectionActivity> controller;
    private LanguageSelectionActivity createActivity() {
        controller=Robolectric.buildActivity(LanguageSelectionActivity.class);
        return controller.create().get();
    }
    @After public void closeActivity() {
        if (controller != null) controller.destroy();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
        if (controller != null) {
            while (shadowOf(controller.get()).getNextStartedActivity() != null) {
                // Consume navigation intents so they cannot leak into Settings tests.
            }
        }
    }
    private void prepareLanguage(MockedStatic<LanguageManager> language) {
        language.when(()->LanguageManager.getCurrentLanguageCode(any(Context.class))).thenReturn("en");
        language.when(()->LanguageManager.applyLanguage(any(Context.class),anyString())).thenAnswer(call->call.getArgument(0));
        language.when(()->LanguageManager.isLanguageSet(any(Context.class))).thenReturn(false);
        language.when(()->LanguageManager.setLanguage(any(Context.class),anyString())).thenAnswer(call->null);
    }
    @Test public void bengaliChoiceUsesCorrectCode() { checkPreferred(0,"bn"); }
    @Test public void englishChoiceUsesCorrectCode() { checkPreferred(1,"en"); }

    private void checkPreferred(int choice,String code) {
        try (MockedStatic<AppFlavourDetector> flavour=mockStatic(AppFlavourDetector.class);
             MockedStatic<LanguageManager> language=mockStatic(LanguageManager.class)) {
            prepareLanguage(language);
            flavour.when(()->AppFlavourDetector.isBangladeshFlavour(any(Context.class))).thenReturn(true);
            LanguageSelectionActivity activity=createActivity();
            AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();
            assertEquals(3,dialog.getListView().getCount());
            assertEquals("বাংলা",dialog.getListView().getAdapter().getItem(0));
            assertEquals("English",dialog.getListView().getAdapter().getItem(1));
            assertEquals(-1,dialog.getListView().getCheckedItemPosition());
            dialog.getListView().performItemClick(null,choice,choice);
            language.verify(()->LanguageManager.setLanguage(activity,code),times(1));
            assertEquals(MenuActivity.class.getName(),shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
        }
    }

    @Test public void otherLanguagesAndCancelPreserveExplicitChoice() {
        try (MockedStatic<AppFlavourDetector> flavour=mockStatic(AppFlavourDetector.class);
             MockedStatic<LanguageManager> language=mockStatic(LanguageManager.class)) {
            prepareLanguage(language);
            flavour.when(()->AppFlavourDetector.isBangladeshFlavour(any(Context.class))).thenReturn(true);
            prepareLanguage(language);
            language.when(()->LanguageManager.getAvailableLanguages(any(Context.class))).thenCallRealMethod();
            language.when(()->LanguageManager.getLanguageCodeFromLocalizedName(any(Context.class),anyString())).thenCallRealMethod();
            LanguageSelectionActivity activity=createActivity();
            ((AlertDialog)ShadowDialog.getLatestDialog()).getListView().performItemClick(null,2,2);
            AlertDialog all=(AlertDialog)ShadowDialog.getLatestDialog();
            assertEquals(20,all.getListView().getCount());
            assertEquals(-1,all.getListView().getCheckedItemPosition());
            all.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            org.robolectric.shadows.ShadowLooper.idleMainLooper();
            assertEquals(3,((AlertDialog)ShadowDialog.getLatestDialog()).getListView().getCount());
            language.verify(()->LanguageManager.setLanguage(any(Context.class),anyString()),never());
            assertNull(shadowOf(activity).getNextStartedActivity());
            ((AlertDialog)ShadowDialog.getLatestDialog()).getListView().performItemClick(null,2,2);
            all=(AlertDialog)ShadowDialog.getLatestDialog();
            String name=(String)all.getListView().getAdapter().getItem(5);
            String code=LanguageManager.getLanguageCodeFromLocalizedName(activity,name);
            all.getListView().performItemClick(null,5,5);
            language.verify(()->LanguageManager.setLanguage(activity,code));
        }
    }

    @Test public void globalKeepsFullListAndOriginalSelection() {
        try (MockedStatic<LanguageManager> language=mockStatic(LanguageManager.class);
             MockedStatic<AppFlavourDetector> flavour=mockStatic(AppFlavourDetector.class)) {
            prepareLanguage(language);
            language.when(()->LanguageManager.getAvailableLanguages(any(Context.class))).thenCallRealMethod();
            createActivity();
            AlertDialog dialog=(AlertDialog)ShadowDialog.getLatestDialog();
            assertEquals(20,dialog.getListView().getCount());
            assertEquals(0,dialog.getListView().getCheckedItemPosition());
        }
    }

    @Test public void savedLanguageSkipsSelection() {
        try (MockedStatic<LanguageManager> language=mockStatic(LanguageManager.class)) {
            prepareLanguage(language);
            language.when(()->LanguageManager.isLanguageSet(any(Context.class))).thenReturn(true);
            LanguageSelectionActivity activity=createActivity();
            assertEquals(MenuActivity.class.getName(),shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
            language.verify(()->LanguageManager.setLanguage(any(Context.class),anyString()),never());
        }
    }
}
