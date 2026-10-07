package piotr_gorczynski.soccer2;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.*;
import com.google.firebase.Timestamp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class TermsRepositoryTest {
    FirebaseFirestore db;
    DocumentReference user, acceptance;
    DocumentSnapshot profile, existing;
    Transaction tx;
    TermsRepository repository;
    @Before public void setup() throws Exception {
        db=mock(FirebaseFirestore.class);user=mock(DocumentReference.class);acceptance=mock(DocumentReference.class);
        CollectionReference users=mock(CollectionReference.class), records=mock(CollectionReference.class);
        when(db.collection("users")).thenReturn(users);when(users.document("owner")).thenReturn(user);
        when(user.collection("legalAcceptances")).thenReturn(records);when(records.document(anyString())).thenReturn(acceptance);
        profile=mock(DocumentSnapshot.class);existing=mock(DocumentSnapshot.class);tx=mock(Transaction.class);
        when(tx.get(user)).thenReturn(profile);when(tx.get(acceptance)).thenReturn(existing);
        when(db.runTransaction(any())).thenAnswer(call -> {
            Transaction.Function<Void> fn=call.getArgument(0);
            try { fn.apply(tx);return Tasks.forResult(null); }catch(Exception error){return Tasks.forException(error);}
        });
        repository=new TermsRepository(db);
    }
    @Test public void newGlobalAcceptanceWritesExactRecordAndCompatibilityAtomically() {
        TermsPolicy policy=TermsPolicy.required(false,"pl");
        assertTrue(repository.accept("owner",policy).isSuccessful());
        ArgumentCaptor<Map> record=ArgumentCaptor.forClass(Map.class);
        verify(tx).set(eq(acceptance),record.capture());
        assertEquals(policy.newRecord(),record.getValue());
        ArgumentCaptor<Map> legacy=ArgumentCaptor.forClass(Map.class);
        verify(tx).set(eq(user),legacy.capture(),any(SetOptions.class));
        assertEquals(true,legacy.getValue().get("termsAccepted"));
        assertEquals(false,legacy.getValue().get("legacyGlobalTermsAccepted"));
        assertEquals(2,legacy.getValue().get("termsAcceptanceModel"));
        verify(user,never()).set(any());
    }
    @Test public void bangladeshPreservesOldGlobalEligibilityWithoutCreatingGlobalEvidence() {
        when(profile.getBoolean("termsAccepted")).thenReturn(true);
        when(profile.getString("language")).thenReturn("pl");
        assertTrue(repository.accept("owner",TermsPolicy.required(true,"en")).isSuccessful());
        ArgumentCaptor<Map> legacy=ArgumentCaptor.forClass(Map.class);
        verify(tx).set(eq(user),legacy.capture(),any(SetOptions.class));
        assertEquals(true,legacy.getValue().get("legacyGlobalTermsAccepted"));
        assertEquals("pl",legacy.getValue().get("language"));
        verify(tx,times(1)).set(eq(acceptance),any());
    }
    @Test public void repeatAcceptanceNeverRewritesEvidence() {
        TermsPolicy policy=TermsPolicy.required(true,"en");Map<String,Object> saved=policy.newRecord();
        saved.put("acceptedAt",new Timestamp(100,0));when(existing.exists()).thenReturn(true);when(existing.getData()).thenReturn(saved);
        assertTrue(repository.accept("owner",policy).isSuccessful());
        verify(tx,never()).set(any(DocumentReference.class),any());
        verify(tx,never()).set(any(DocumentReference.class),any(),any(SetOptions.class));
    }
    @Test public void invalidExistingEvidenceFailsRatherThanOverwriting() {
        when(existing.exists()).thenReturn(true);when(existing.getData()).thenReturn(Collections.emptyMap());
        assertFalse(repository.accept("owner",TermsPolicy.required(true,"en")).isSuccessful());
        verify(tx,never()).set(any(DocumentReference.class),any());
    }
}
