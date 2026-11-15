package piotr_gorczynski.soccer2;

import android.os.Bundle;
import com.google.firebase.firestore.FirebaseFirestoreException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class TermsDiagnosticsTest {
    @Test public void readingTournamentContextDoesNotBindClearOrChangeSelection() {
        android.content.Context c=org.robolectric.RuntimeEnvironment.getApplication();
        PendingTournamentPromotion.select(c,"cup-test",null,1000);
        var prefs=c.getSharedPreferences(LanguageManager.PREFS_FILE,0);
        Map<String,?> before=new HashMap<>(prefs.getAll());
        assertTrue(PendingTournamentPromotion.hasSelectionForUser(c,"owner",1001));
        assertFalse(PendingTournamentPromotion.hasSelectionForUser(c,null,1001));
        assertFalse(PendingTournamentPromotion.hasSelectionForUser(c,"owner",1000+86400001));
        assertEquals(before,prefs.getAll());
        PendingTournamentPromotion.select(c,"cup-test","owner",1000);
        assertFalse(PendingTournamentPromotion.hasSelectionForUser(c,"other",1001));
        PendingTournamentPromotion.clear(c);
    }
    @Test public void sdkCodesAndTypesNeverParsePrivateMessages() {
        assertFailure(new FirebaseFirestoreException("private uid",FirebaseFirestoreException.Code.PERMISSION_DENIED),"permission","PERMISSION_DENIED");
        assertFailure(new FirebaseFirestoreException("network",FirebaseFirestoreException.Code.UNAVAILABLE),"unavailable","UNAVAILABLE");
        assertFailure(new RuntimeException(new FirebaseFirestoreException("secret",FirebaseFirestoreException.Code.UNAUTHENTICATED)),"unauthenticated","UNAUTHENTICATED");
        assertFailure(new java.net.UnknownHostException("secret"),"network","NETWORK_ERROR");
        assertFailure(new com.google.firebase.FirebaseNetworkException("secret"),"network","NETWORK_ERROR");
        assertFailure(new java.net.SocketTimeoutException("secret"),"timeout","TIMEOUT");
        assertFailure(new java.io.IOException("secret"),"io_error","IO_ERROR");
        assertFailure(new IllegalStateException("PERMISSION_DENIED private uid"),"unknown","UNKNOWN");
        assertFailure(null,"unknown","UNKNOWN");
    }
    private void assertFailure(Throwable error,String reason,String code) {
        TermsFailureDiagnostics result=TermsFailureDiagnostics.from(error);
        assertEquals(reason,result.reason);assertEquals(code,result.code);
    }
    @Test public void visibleTimeExcludesBackgroundAndDecisionsAreTerminal() {
        long[] clock={100}; List<Bundle> events=new ArrayList<>();
        TermsScreenDiagnostics tracker=new TermsScreenDiagnostics(null,"tournament",()->clock[0],events::add);
        tracker.resume();tracker.resume();assertEquals(1,events.size());
        clock[0]=600;tracker.pause();assertEquals(500,events.get(1).getLong("duration_ms"));
        clock[0]=10000;tracker.resume();clock[0]=10200;
        tracker.step("terms_save","declined","none","NONE","back");tracker.pause();
        tracker.step("terms_save","declined","none","NONE","decline");
        assertEquals(3,events.size());assertEquals(700,events.get(2).getLong("duration_ms"));
        assertEquals("back",events.get(2).getString("action"));
        assertEquals("tournament",events.get(2).getString("entry_source"));
    }
    @Test public void rotationDoesNotRepeatShownAndSuccessfulWriteClosesTracking() {
        long[] clock={0};List<Bundle> events=new ArrayList<>();
        TermsScreenDiagnostics tracker=new TermsScreenDiagnostics(null,"onboarding",()->clock[0],events::add);
        tracker.resume();clock[0]=100;
        TermsScreenDiagnostics restored=new TermsScreenDiagnostics(tracker.save(),"onboarding",()->clock[0],events::add);
        restored.resume();assertEquals(1,events.size());clock[0]=200;
        restored.step("terms_save","submit","none","NONE","accept");
        restored.step("terms_save","failure","permission","PERMISSION_DENIED","none");
        restored.step("terms_save","success","none","NONE","accept");restored.pause();
        assertEquals(4,events.size());assertEquals(200,events.get(3).getLong("duration_ms"));
    }
}
