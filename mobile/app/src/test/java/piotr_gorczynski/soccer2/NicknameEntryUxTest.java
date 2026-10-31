package piotr_gorczynski.soccer2;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.google.firebase.auth.FirebaseAuth;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class NicknameEntryUxTest {
    PickNicknameActivity activity;
    EditText input;
    Button confirm;
    AnalyticsManager analytics;

    @Before public void setup() throws Exception {
        activity=Robolectric.buildActivity(PickNicknameActivity.class).get();
        View layout=LayoutInflater.from(new android.view.ContextThemeWrapper(
                RuntimeEnvironment.getApplication(),R.style.AppTheme))
                .inflate(R.layout.activity_pick_nickname,null);
        input=layout.findViewById(R.id.editNickname);
        confirm=layout.findViewById(R.id.btnConfirmNickname);
        analytics=mock(AnalyticsManager.class);
        field("editNickname",input);field("btnConfirm",confirm);field("analyticsManager",analytics);
        field("submission",new NicknameSubmission(NicknameSubmission.firebaseBackend(),analytics));
        call("initializeNicknameInput");
    }
    void field(String name,Object value) throws Exception {
        Field f=PickNicknameActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);
    }
    void call(String name) throws Exception {
        Method m=PickNicknameActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);
    }
    void suggest(android.os.Bundle state) throws Exception {
        Method m=PickNicknameActivity.class.getDeclaredMethod("initializeNicknameSuggestion",android.os.Bundle.class);
        m.setAccessible(true);m.invoke(activity,state);
    }
    @Test public void suggestionIsEditableAndDoesNotSubmitOrEmitAnalytics() throws Exception {
        suggest(null);
        assertTrue(input.getText().toString().matches("Player[A-HJ-NP-Z2-9]{6}"));
        assertTrue(confirm.isEnabled());
        assertEquals(0,input.getSelectionStart());
        assertEquals(input.length(),input.getSelectionEnd());
        input.setText("myNickname");assertTrue(confirm.isEnabled());
        verifyNoInteractions(analytics);
    }
    @Test public void restoredDraftIsPreservedIncludingEmptyValue() throws Exception {
        for(String draft:new String[]{"Player7K4M9Q","myNickname",""}) {
            android.os.Bundle state=new android.os.Bundle();state.putString("nickname_draft",draft);
            suggest(state);assertEquals(draft,input.getText().toString());
            assertEquals(!draft.isEmpty(),confirm.isEnabled());
        }
        verifyNoInteractions(analytics);
    }
    @Test public void generatedSuggestionsUseShortSafeFormat() {
        for(int i=0;i<100;i++)assertTrue(PickNicknameActivity.generateNicknameSuggestion()
                .matches("Player[A-HJ-NP-Z2-9]{6}"));
    }
    @Test public void emptyAndWhitespaceCannotSubmit() {
        for(String value:new String[]{""," ","   "}) {
            input.setText(value);assertFalse(confirm.isEnabled());
        }
        verifyNoInteractions(analytics);
    }
    @Test public void validInputEnablesAndDeletingDisables() {
        input.setText("player");assertTrue(confirm.isEnabled());
        input.setText("");assertFalse(confirm.isEnabled());
        verifyNoInteractions(analytics);
    }
    @Test public void editingDuringSubmissionCannotEnableButton() throws Exception {
        input.setText("player");field("nicknameSubmissionInProgress",true);
        input.setText("another");assertFalse(confirm.isEnabled());
        field("nicknameSubmissionInProgress",false);call("updateConfirmButton");
        assertTrue(confirm.isEnabled());
    }
    @Test public void defensiveEmptyCheckRemainsWithoutUserTextInAnalytics() throws Exception {
        call("saveNickname");
        verify(analytics).trackOnboardingStep("nickname","submit","none");
        verify(analytics).trackOnboardingStep("nickname","failure","empty");
        verifyNoMoreInteractions(analytics);
    }
    @Test public void validValueReachesExistingAuthenticationGateWithoutLoggingNickname() throws Exception {
        FirebaseAuth auth=mock(FirebaseAuth.class);
        try(MockedStatic<FirebaseAuth> sdk=mockStatic(FirebaseAuth.class)) {
            sdk.when(FirebaseAuth::getInstance).thenReturn(auth);
            input.setText("privateNickname");call("saveNickname");
            verify(auth).getUid();
            verify(analytics).trackOnboardingStep("nickname","submit","none");
            verifyNoMoreInteractions(analytics);
        }
    }
    @Test public void existingLengthValidationStillRuns() throws Exception {
        input.setFilters(new android.text.InputFilter[0]);
        input.setText("abcdefghijklmnopqrstuvwxyz");
        call("saveNickname");
        verify(analytics).trackOnboardingStep("nickname","submit","none");
        verify(analytics).trackOnboardingStep("nickname","failure","too_long");
        verifyNoMoreInteractions(analytics);
    }
    @Test public void validAuthenticatedInputStartsExistingUniquenessCheck() throws Exception {
        FirebaseAuth auth=mock(FirebaseAuth.class);
        com.google.firebase.firestore.FirebaseFirestore db=mock(com.google.firebase.firestore.FirebaseFirestore.class);
        com.google.firebase.firestore.CollectionReference users=mock(com.google.firebase.firestore.CollectionReference.class);
        com.google.firebase.firestore.Query query=mock(com.google.firebase.firestore.Query.class);
        when(auth.getUid()).thenReturn("test-user");
        when(db.collection("users")).thenReturn(users);
        when(users.whereEqualTo("nicknameLowercase","player")).thenReturn(query);
        when(query.get()).thenReturn(new com.google.android.gms.tasks.TaskCompletionSource<com.google.firebase.firestore.QuerySnapshot>().getTask());
        try(MockedStatic<FirebaseAuth> sdk=mockStatic(FirebaseAuth.class);
            MockedStatic<com.google.firebase.firestore.FirebaseFirestore> firestore=mockStatic(com.google.firebase.firestore.FirebaseFirestore.class)) {
            sdk.when(FirebaseAuth::getInstance).thenReturn(auth);
            firestore.when(com.google.firebase.firestore.FirebaseFirestore::getInstance).thenReturn(db);
            input.setText("player");call("saveNickname");
            verify(query).get();assertFalse(confirm.isEnabled());
            input.setText("other");assertFalse(confirm.isEnabled());
            call("saveNickname");verify(query,times(1)).get();
            verify(analytics).trackOnboardingStep("nickname","submit","none");
            verifyNoMoreInteractions(analytics);
        }
    }
    @Test public void allTwentyLocalesHaveHelpText() {
        for(String lang:new String[]{"en","bn","pl","de","fr","es","ar","fa","hi","ur","ne","si","am","sw","so","mg","mn","my","km","lo"}) {
            android.content.res.Configuration config=new android.content.res.Configuration(
                    RuntimeEnvironment.getApplication().getResources().getConfiguration());
            config.setLocale(Locale.forLanguageTag(lang));
            String text=RuntimeEnvironment.getApplication().createConfigurationContext(config)
                    .getString(R.string.nickname_entry_help);
            assertFalse(text.trim().isEmpty());
            if(!lang.equals("en"))assertNotEquals("Missing translation: "+lang,"Enter a nickname to continue.",text);
            String suggestion=RuntimeEnvironment.getApplication().createConfigurationContext(config)
                    .getString(R.string.nickname_suggestion_help);
            assertFalse(suggestion.trim().isEmpty());
            if(!lang.equals("en"))assertNotEquals("Missing suggestion translation: "+lang,
                    "Use the suggested nickname or enter your own.",suggestion);
        }
    }
}
