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
}
