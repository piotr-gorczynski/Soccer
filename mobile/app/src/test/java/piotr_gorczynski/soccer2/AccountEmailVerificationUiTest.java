package piotr_gorczynski.soccer2;

import android.view.View;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.tasks.Tasks;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=AccountSignedOutTest.TestApp.class,sdk=34)
public class AccountEmailVerificationUiTest {
    @Test public void pendingEmailOffersRecoveryAndWarnsBeforeLogout() {
        FirebaseAuth auth=mock(FirebaseAuth.class);FirebaseUser user=mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);when(user.getUid()).thenReturn("owner");
        when(user.getEmail()).thenReturn("typo@example.invalid");
        UserInfo provider=mock(UserInfo.class);when(provider.getProviderId()).thenReturn("password");
        doReturn(Collections.singletonList(provider)).when(user).getProviderData();
        when(user.sendEmailVerification()).thenReturn(Tasks.forResult(null));
        try(MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class);
            MockedStatic<FirebaseFirestore> fs=mockStatic(FirebaseFirestore.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);
            fs.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));
            AccountActivity activity=Robolectric.buildActivity(AccountActivity.class).create().start().resume().get();
            assertEquals(View.VISIBLE,activity.findViewById(R.id.emailVerificationPanel).getVisibility());
            activity.findViewById(R.id.emailVerificationResend).performClick();ShadowLooper.idleMainLooper();
            verify(user).sendEmailVerification();verify(auth,never()).signOut();
            AlertDialog sent=(AlertDialog)ShadowDialog.getLatestDialog();
            sent.getButton(AlertDialog.BUTTON_POSITIVE).performClick();ShadowLooper.idleMainLooper();
            assertFalse(activity.isFinishing());verify(auth,never()).signOut();
            activity.findViewById(R.id.btnLogout).performClick();
            AlertDialog warning=(AlertDialog)ShadowDialog.getLatestDialog();
            assertEquals(activity.getString(R.string.email_verification_logout_warning),
                    ((android.widget.TextView)warning.findViewById(android.R.id.message)).getText().toString());
            warning.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();ShadowLooper.idleMainLooper();
            verify(auth,never()).signOut();assertFalse(activity.isFinishing());
        }
    }
    @Test public void resendToCorrectedAddressReauthenticatesAndRetriesWithoutSignOut() {
        checkRecentLogin("success");
    }
    @Test public void cancellingReauthenticationKeepsSessionAndDoesNotResend() {
        checkRecentLogin("cancel");
    }
    @Test public void wrongPasswordKeepsRecoveryDialogAndSession() {
        checkRecentLogin("wrong_password");
    }
    @Test public void retryCannotLoopIntoRepeatedReauthenticationPrompts() {
        checkRecentLogin("still_recent");
    }
    private void checkRecentLogin(String outcome) {
        FirebaseAuth auth=mock(FirebaseAuth.class);FirebaseUser user=mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);when(user.getUid()).thenReturn("owner");
        when(user.getEmail()).thenReturn("typo@example.invalid");
        UserInfo provider=mock(UserInfo.class);when(provider.getProviderId()).thenReturn("password");
        doReturn(Collections.singletonList(provider)).when(user).getProviderData();
        FirebaseAuthRecentLoginRequiredException recent=new FirebaseAuthRecentLoginRequiredException(
                "ERROR_REQUIRES_RECENT_LOGIN","sensitive operation");
        when(user.verifyBeforeUpdateEmail("correct@example.invalid")).thenReturn(
                Tasks.forException(recent), "still_recent".equals(outcome) ? Tasks.forException(recent) : Tasks.forResult(null));
        when(user.reauthenticate(any())).thenReturn("wrong_password".equals(outcome)
                ? Tasks.forException(new FirebaseAuthException("ERROR_WRONG_PASSWORD","wrong"))
                : Tasks.forResult(null));
        try(MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class);
            MockedStatic<FirebaseFirestore> fs=mockStatic(FirebaseFirestore.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);
            fs.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));
            AccountActivity activity=Robolectric.buildActivity(AccountActivity.class).create().start().resume().get();
            activity.getSharedPreferences(LanguageManager.PREFS_FILE,0).edit()
                    .putString("verification_target_owner","correct@example.invalid").commit();
            activity.findViewById(R.id.emailVerificationResend).performClick();ShadowLooper.idleMainLooper();
            AlertDialog reauth=(AlertDialog)ShadowDialog.getLatestDialog();assertTrue(reauth.isShowing());
            verify(user).verifyBeforeUpdateEmail("correct@example.invalid");
            if("cancel".equals(outcome)) {
                reauth.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();ShadowLooper.idleMainLooper();
                verify(user,never()).reauthenticate(any());
            } else {
                android.widget.EditText password=reauth.findViewById(R.id.editPassword);
                password.setText("test-password");
                reauth.getButton(AlertDialog.BUTTON_POSITIVE).performClick();ShadowLooper.idleMainLooper();
                verify(user).reauthenticate(any());
                if("wrong_password".equals(outcome)) {
                    assertTrue(reauth.isShowing());assertNotNull(password.getError());
                    assertTrue(reauth.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
                } else {
                    verify(user,times(2)).verifyBeforeUpdateEmail("correct@example.invalid");
                    assertFalse(reauth.isShowing());
                    if("success".equals(outcome)) {
                        AlertDialog sent=(AlertDialog)ShadowDialog.getLatestDialog();
                        assertTrue(((android.widget.TextView)sent.findViewById(android.R.id.message))
                                .getText().toString().contains("correct@example.invalid"));
                    }
                }
            }
            verify(user,never()).sendEmailVerification();verify(auth,never()).signOut();
            assertFalse(activity.isFinishing());
            assertEquals("correct@example.invalid",activity.getSharedPreferences(LanguageManager.PREFS_FILE,0)
                    .getString("verification_target_owner",null));
        }
    }
    @Test public void revokedSessionDuringVerificationShowsRecoveryNoticeInsteadOfSilence() {
        FirebaseAuth auth=mock(FirebaseAuth.class);FirebaseUser user=mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);when(user.getUid()).thenReturn("owner");
        when(user.getEmail()).thenReturn("typo@example.invalid");
        UserInfo provider=mock(UserInfo.class);when(provider.getProviderId()).thenReturn("password");
        doReturn(Collections.singletonList(provider)).when(user).getProviderData();
        when(user.reload()).thenAnswer(invocation -> {
            when(auth.getCurrentUser()).thenReturn(null);
            return Tasks.forException(new FirebaseAuthInvalidUserException("ERROR_USER_TOKEN_EXPIRED","expired"));
        });
        try(MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class);
            MockedStatic<FirebaseFirestore> fs=mockStatic(FirebaseFirestore.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);
            fs.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));
            AccountActivity activity=Robolectric.buildActivity(AccountActivity.class).create().start().resume().get();
            android.content.SharedPreferences prefs=activity.getSharedPreferences(LanguageManager.PREFS_FILE,0);
            prefs.edit().putString("verification_target_owner","correct@example.invalid")
                    .putString("nickname","test-player").putString("uid","owner").commit();
            activity.findViewById(R.id.emailVerificationCheck).performClick();ShadowLooper.idleMainLooper();
            AlertDialog notice=(AlertDialog)ShadowDialog.getLatestDialog();assertTrue(notice.isShowing());
            assertEquals(activity.getString(R.string.email_verification_sign_in_again,"correct@example.invalid"),
                    ((android.widget.TextView)notice.findViewById(android.R.id.message)).getText().toString());
            assertTrue(activity.findViewById(R.id.emailVerificationCheck).isEnabled());
            assertFalse(activity.isFinishing());
            notice.getButton(AlertDialog.BUTTON_POSITIVE).performClick();ShadowLooper.idleMainLooper();
            assertTrue(activity.isFinishing());assertFalse(prefs.contains("nickname"));assertFalse(prefs.contains("uid"));
            assertEquals("correct@example.invalid",prefs.getString("verification_target_owner",null));
            verify(auth,never()).signOut();verify(user,never()).delete();
        }
    }
    @Test public void verifiedCorrectedEmailHidesAndDisablesRecoveryActionsWithoutManualCheck() {
        FirebaseAuth auth=mock(FirebaseAuth.class);FirebaseUser user=mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);when(user.getUid()).thenReturn("owner");
        when(user.getEmail()).thenReturn("correct@example.invalid");when(user.isEmailVerified()).thenReturn(true);
        try(MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class);
            MockedStatic<FirebaseFirestore> fs=mockStatic(FirebaseFirestore.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);
            fs.when(FirebaseFirestore::getInstance).thenReturn(mock(FirebaseFirestore.class));
            org.robolectric.android.controller.ActivityController<AccountActivity> controller=Robolectric.buildActivity(AccountActivity.class);
            AccountActivity activity=controller.get();
            android.content.SharedPreferences prefs=activity.getSharedPreferences(LanguageManager.PREFS_FILE,0);
            prefs.edit().putString("verification_target_owner","correct@example.invalid")
                    .putString("email","typo@example.invalid").commit();
            controller.create().start().resume();
            assertEquals(View.GONE,activity.findViewById(R.id.emailVerificationPanel).getVisibility());
            for(int id : new int[]{R.id.emailVerificationResend,R.id.emailVerificationCorrect,R.id.emailVerificationCheck})
                assertFalse(activity.findViewById(id).isEnabled());
            assertFalse(prefs.contains("verification_target_owner"));
            assertTrue(((android.widget.TextView)activity.findViewById(R.id.accountEmail)).getText().toString()
                    .contains("correct@example.invalid"));
            verify(user,never()).reload();verify(user,never()).sendEmailVerification();verify(auth,never()).signOut();
        }
    }
}