package piotr_gorczynski.soccer2;

import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.TextView;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class EmailLinkDialogTest {
    FirebaseAuthManager manager=mock(FirebaseAuthManager.class);
    LinkAccountActivity activity;
    private AlertDialog open() throws Exception {
        activity=Robolectric.buildActivity(LinkAccountActivity.class).get();
        activity.setTheme(R.style.AppTheme);
        Field f=LinkAccountActivity.class.getDeclaredField("authManager");f.setAccessible(true);f.set(activity,manager);
        Method m=LinkAccountActivity.class.getDeclaredMethod("showEmailLinkDialog");m.setAccessible(true);m.invoke(activity);
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
        return (AlertDialog)ShadowDialog.getLatestDialog();
    }
    private void fill(AlertDialog d,String password,String confirmation) {
        ((EditText)d.findViewById(R.id.editEmail)).setText("test@example.invalid");
        ((EditText)d.findViewById(R.id.editPassword)).setText(password);
        ((EditText)d.findViewById(R.id.editConfirmPassword)).setText(confirmation);
    }
    @Test public void mismatchedPasswordsKeepDialogOpenWithoutLinking() throws Exception {
        AlertDialog d=open();fill(d,"first-password","different-password");
        d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        verify(manager,never()).linkWithEmailPassword(anyString(),anyString(),any());assertTrue(d.isShowing());
    }
    @Test public void missingConfirmationDoesNotLink() throws Exception {
        AlertDialog d=open();fill(d,"first-password","");d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        verify(manager,never()).linkWithEmailPassword(anyString(),anyString(),any());assertTrue(d.isShowing());
    }
    @Test public void confirmationIsShownOnlyAfterSuccessfulLinkAndEmailSend() throws Exception {
        AlertDialog d=open();fill(d,"same-password","same-password");d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ArgumentCaptor<FirebaseAuthManager.LinkCallback> cb=ArgumentCaptor.forClass(FirebaseAuthManager.LinkCallback.class);
        verify(manager).linkWithEmailPassword(eq("test@example.invalid"),eq("same-password"),cb.capture());
        assertFalse(d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
        com.google.firebase.auth.FirebaseAuth auth=mock(com.google.firebase.auth.FirebaseAuth.class);
        com.google.firebase.auth.FirebaseUser user=mock(com.google.firebase.auth.FirebaseUser.class);
        when(auth.getCurrentUser()).thenReturn(user);when(user.getEmail()).thenReturn("test@example.invalid");
        try(org.mockito.MockedStatic<com.google.firebase.auth.FirebaseAuth> firebase=mockStatic(com.google.firebase.auth.FirebaseAuth.class)) {
        firebase.when(com.google.firebase.auth.FirebaseAuth::getInstance).thenReturn(auth);
        cb.getValue().onLinkSuccess();assertFalse(d.isShowing());
        android.app.Dialog sent=ShadowDialog.getLatestDialog();
        assertEquals(activity.getString(R.string.link_email_sent_keep_session,"test@example.invalid"),
                ((TextView)sent.findViewById(android.R.id.message)).getText().toString());
        assertFalse(activity.isFinishing());
        ((androidx.appcompat.app.AlertDialog)sent).getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        org.robolectric.shadows.ShadowLooper.idleMainLooper();
        verify(auth,never()).signOut();assertTrue(activity.isFinishing());
        }
    }
    @Test public void sendFailureKeepsFormAndAllowsRetry() throws Exception {
        AlertDialog d=open();fill(d,"same-password","same-password");d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ArgumentCaptor<FirebaseAuthManager.LinkCallback> cb=ArgumentCaptor.forClass(FirebaseAuthManager.LinkCallback.class);
        verify(manager).linkWithEmailPassword(anyString(),anyString(),cb.capture());cb.getValue().onLinkFailure("send failed");
        assertTrue(d.isShowing());assertTrue(d.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled());
        assertSame(d,ShadowDialog.getLatestDialog());
    }
}
