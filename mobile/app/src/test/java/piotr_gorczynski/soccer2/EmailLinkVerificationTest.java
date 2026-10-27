package piotr_gorczynski.soccer2;

import android.os.Looper;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.mockito.Mockito.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class EmailLinkVerificationTest {
    @Test public void linkingPreservesProfileAndWaitsForVerificationEmail() {
        FirebaseAuth auth=mock(FirebaseAuth.class);FirebaseUser user=mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);when(user.getUid()).thenReturn("owner");when(user.isAnonymous()).thenReturn(true);
        when(user.linkWithCredential(any())).thenReturn(Tasks.forResult(mock(AuthResult.class)));
        TaskCompletionSource<Void> email=new TaskCompletionSource<>();when(user.sendEmailVerification()).thenReturn(email.getTask());
        FirebaseFirestore db=mock(FirebaseFirestore.class);CollectionReference users=mock(CollectionReference.class);
        DocumentReference ref=mock(DocumentReference.class);when(db.collection("users")).thenReturn(users);when(users.document("owner")).thenReturn(ref);
        when(ref.get(Source.SERVER)).thenReturn(Tasks.forResult(mock(DocumentSnapshot.class)));
        when(ref.set(any(),eq(SetOptions.merge()))).thenReturn(Tasks.forResult(null));
        FirebaseAuthManager.LinkCallback cb=mock(FirebaseAuthManager.LinkCallback.class);
        try(MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class);MockedStatic<FirebaseFirestore> fs=mockStatic(FirebaseFirestore.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);fs.when(FirebaseFirestore::getInstance).thenReturn(db);
            FirebaseAuthManager manager=new FirebaseAuthManager(RuntimeEnvironment.getApplication());
            manager.linkWithEmailPassword("test@example.invalid","password",cb);shadowOf(Looper.getMainLooper()).idle();
            verify(user).sendEmailVerification();verify(cb,never()).onLinkSuccess();
            verify(ref).set(argThat((java.util.Map<String,Object> data)->"email".equals(data.get("method"))
                    && !data.containsKey("nickname") && !data.containsKey("termsAccepted")),eq(SetOptions.merge()));
            email.setResult(null);shadowOf(Looper.getMainLooper()).idle();verify(cb).onLinkSuccess();
        }
    }
    @Test public void failedDeliveryReportsFailureAndCanBeRetriedWithoutRelinking() {
        FirebaseAuth auth=mock(FirebaseAuth.class);FirebaseUser user=mock(FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);when(user.getUid()).thenReturn("owner");
        when(user.getEmail()).thenReturn("test@example.invalid");when(user.isAnonymous()).thenReturn(false);when(user.isEmailVerified()).thenReturn(false);
        when(user.sendEmailVerification()).thenReturn(Tasks.forException(new Exception("network")),Tasks.forResult(null));
        FirebaseAuthManager.LinkCallback cb=mock(FirebaseAuthManager.LinkCallback.class);
        try(MockedStatic<FirebaseAuth> fa=mockStatic(FirebaseAuth.class)) {
            fa.when(FirebaseAuth::getInstance).thenReturn(auth);
            FirebaseAuthManager manager=new FirebaseAuthManager(RuntimeEnvironment.getApplication());
            manager.linkWithEmailPassword("test@example.invalid","password",cb);shadowOf(Looper.getMainLooper()).idle();
            verify(cb).onLinkFailure(anyString());verify(cb,never()).onLinkSuccess();
            manager.linkWithEmailPassword("test@example.invalid","password",cb);shadowOf(Looper.getMainLooper()).idle();
            verify(cb).onLinkSuccess();verify(user,times(2)).sendEmailVerification();verify(user,never()).linkWithCredential(any());
        }
    }
}
