package piotr_gorczynski.soccer2;

import android.content.SharedPreferences;
import android.os.Looper;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class EmailVerificationManagerTest {
    FirebaseAuth auth=mock(FirebaseAuth.class);
    FirebaseUser user=mock(FirebaseUser.class);
    FirebaseFirestore db=mock(FirebaseFirestore.class);
    DocumentReference profile=mock(DocumentReference.class);
    SharedPreferences prefs;
    EmailVerificationManager manager;
    @Before public void setup() {
        when(auth.getCurrentUser()).thenReturn(user);when(user.getUid()).thenReturn("owner");
        when(user.getEmail()).thenReturn("typo@example.invalid");
        UserInfo provider=mock(UserInfo.class);when(provider.getProviderId()).thenReturn("password");
        doReturn(Collections.singletonList(provider)).when(user).getProviderData();
        when(user.reload()).thenReturn(Tasks.forResult(null));
        when(user.getIdToken(true)).thenReturn(Tasks.forResult(mock(GetTokenResult.class)));
        CollectionReference users=mock(CollectionReference.class);when(db.collection("users")).thenReturn(users);
        when(users.document("owner")).thenReturn(profile);
        when(profile.set(any(),eq(SetOptions.merge()))).thenReturn(Tasks.forResult(null));
        prefs=RuntimeEnvironment.getApplication().getSharedPreferences("verification-test",0);
        prefs.edit().clear().commit();manager=new EmailVerificationManager(auth,db,prefs);
    }
    @Test public void correctedAddressIsPendingUntilVerifiedAndSurvivesRecreation() {
        when(user.verifyBeforeUpdateEmail("correct@example.invalid")).thenReturn(Tasks.forResult(null));
        Task<Void> result=manager.correctEmail(user,"correct@example.invalid");idle();assertTrue(result.isSuccessful());
        verify(auth,never()).signOut();verifyNoInteractions(db);
        assertEquals("typo@example.invalid",user.getEmail());
        assertEquals("correct@example.invalid",new EmailVerificationManager(auth,db,prefs).target(user));
        manager.resend(user);verify(user,times(2)).verifyBeforeUpdateEmail("correct@example.invalid");
        verify(user,never()).sendEmailVerification();
    }
    @Test public void failedCorrectionKeepsOriginalTargetAndSession() {
        when(user.verifyBeforeUpdateEmail(anyString())).thenReturn(Tasks.forException(new Exception("network")));
        Task<Void> result=manager.correctEmail(user,"correct@example.invalid");idle();assertFalse(result.isSuccessful());
        assertEquals("typo@example.invalid",manager.target(user));verify(auth,never()).signOut();
    }
    @Test public void resendUsesCurrentAddressBeforeCorrection() {
        when(user.sendEmailVerification()).thenReturn(Tasks.forResult(null));
        assertTrue(manager.resend(user).isSuccessful());verify(user).sendEmailVerification();verify(auth,never()).signOut();
    }
    @Test public void unverifiedReloadKeepsSessionAndDoesNotWriteProfile() {
        Task<Boolean> result=manager.check(user);idle();assertEquals(Boolean.FALSE,result.getResult());
        verifyNoInteractions(db);verify(auth,never()).signOut();assertTrue(EmailVerificationManager.needsVerification(user));
    }
    @Test public void verificationRefreshesTokenAndOnlyMergesConfirmedEmail() {
        prefs.edit().putString("verification_target_owner","correct@example.invalid").commit();
        when(user.isEmailVerified()).thenReturn(true);when(user.getEmail()).thenReturn("correct@example.invalid");
        Task<Boolean> result=manager.check(user);idle();assertTrue(result.getResult());
        verify(user).reload();verify(user).getIdToken(true);
        verify(profile).set(eq(Collections.singletonMap("email","correct@example.invalid")),eq(SetOptions.merge()));
        assertFalse(prefs.contains("verification_target_owner"));assertEquals("correct@example.invalid",prefs.getString("email",null));
        verify(auth,never()).signOut();assertFalse(EmailVerificationManager.needsVerification(user));
    }
    @Test public void failedReloadDoesNotReportVerificationOrChangeSession() {
        when(user.reload()).thenReturn(Tasks.forException(new Exception("network")));
        Task<Boolean> result=manager.check(user);idle();assertFalse(result.isSuccessful());
        verifyNoInteractions(db);verify(auth,never()).signOut();
    }
    @Test public void sessionChangeDuringReloadCannotWriteAnotherAccountsPreferences() {
        TaskCompletionSource<Void> reload=new TaskCompletionSource<>();when(user.reload()).thenReturn(reload.getTask());
        Task<Boolean> result=manager.check(user);when(auth.getCurrentUser()).thenReturn(null);
        reload.setResult(null);idle();assertFalse(result.isSuccessful());verifyNoInteractions(db);assertFalse(prefs.contains("email"));
    }
    @Test public void verificationOfOldAddressDoesNotConfirmTheCorrectedAddress() {
        prefs.edit().putString("verification_target_owner","correct@example.invalid").commit();
        when(user.isEmailVerified()).thenReturn(true);
        Task<Boolean> result=manager.check(user);idle();assertEquals(Boolean.FALSE,result.getResult());
        assertTrue(manager.needsAttention(user));verifyNoInteractions(db);
        assertEquals("correct@example.invalid",manager.target(user));
    }
    @Test public void correctingBackToCurrentAddressResendsAndClearsPendingChange() {
        prefs.edit().putString("verification_target_owner","correct@example.invalid").commit();
        when(user.sendEmailVerification()).thenReturn(Tasks.forResult(null));
        Task<Void> result=manager.correctEmail(user,"typo@example.invalid");idle();assertTrue(result.isSuccessful());
        verify(user).sendEmailVerification();verify(user,never()).verifyBeforeUpdateEmail(anyString());
        assertFalse(prefs.contains("verification_target_owner"));verify(auth,never()).signOut();
    }
    @Test public void profileWriteFailureDoesNotClearPendingRecoveryState() {
        prefs.edit().putString("verification_target_owner","correct@example.invalid").commit();
        when(user.isEmailVerified()).thenReturn(true);when(user.getEmail()).thenReturn("correct@example.invalid");
        when(profile.set(any(),eq(SetOptions.merge()))).thenReturn(Tasks.forException(new Exception("network")));
        Task<Boolean> result=manager.check(user);idle();assertFalse(result.isSuccessful());
        assertTrue(prefs.contains("verification_target_owner"));verify(auth,never()).signOut();
    }
    @Test public void freshSignInReconcilesConfirmedTargetWithoutAnotherVerificationRequest() {
        prefs.edit().putString("verification_target_owner","correct@example.invalid").commit();
        when(user.isEmailVerified()).thenReturn(true);when(user.getEmail()).thenReturn("correct@example.invalid");
        assertFalse(manager.needsAttention(user));assertTrue(manager.reconcileConfirmedAddress(user));
        assertFalse(prefs.contains("verification_target_owner"));assertEquals("correct@example.invalid",prefs.getString("email",null));
        verify(user,never()).reload();verify(user,never()).sendEmailVerification();verify(auth,never()).signOut();
    }
    @Test public void oldVerifiedAddressOrAnotherSessionCannotClearPendingCorrection() {
        prefs.edit().putString("verification_target_owner","correct@example.invalid").commit();
        when(user.isEmailVerified()).thenReturn(true);
        assertTrue(manager.needsAttention(user));assertFalse(manager.reconcileConfirmedAddress(user));
        when(user.getEmail()).thenReturn("correct@example.invalid");when(auth.getCurrentUser()).thenReturn(null);
        assertFalse(manager.reconcileConfirmedAddress(user));assertTrue(prefs.contains("verification_target_owner"));
    }
    private void idle() { shadowOf(Looper.getMainLooper()).idle(); }
}
