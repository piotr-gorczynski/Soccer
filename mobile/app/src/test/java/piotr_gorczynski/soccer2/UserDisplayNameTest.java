package piotr_gorczynski.soccer2;
import com.google.firebase.firestore.DocumentSnapshot;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
public class UserDisplayNameTest {
 @Test public void deletedFlagOverridesAnyFormerNickname() {
  DocumentSnapshot user=mock(DocumentSnapshot.class);when(user.exists()).thenReturn(true);
  when(user.getBoolean("accountDeleted")).thenReturn(true);when(user.getString("nickname")).thenReturn("OLD TEST NAME");
  assertEquals("(Account removed)",UserDisplayName.from(user));verify(user,never()).getString("nickname");
 }
 @Test public void activeProfileUsesCurrentNicknameAndMissingProfileIsSafe() {
  DocumentSnapshot user=mock(DocumentSnapshot.class);when(user.exists()).thenReturn(true);when(user.getString("nickname")).thenReturn("TEST PLAYER");
  assertEquals("TEST PLAYER",UserDisplayName.from(user));assertEquals(UserDisplayName.REMOVED,UserDisplayName.from(null));
  assertEquals(UserDisplayName.REMOVED,UserDisplayName.from(mock(DocumentSnapshot.class)));
 }
}
