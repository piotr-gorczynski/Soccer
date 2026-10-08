package piotr_gorczynski.soccer2;
import com.google.android.gms.tasks.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class FcmCleanupGateTest {
    @Test public void duplicateRequestsShareCleanupAndSuccessfulResult() {
        FcmCleanupGate gate=new FcmCleanupGate(); TaskCompletionSource<Void> source=new TaskCompletionSource<>();
        AtomicInteger calls=new AtomicInteger();
        Task<Void> first=gate.run(()->{calls.incrementAndGet();return source.getTask();});
        assertSame(first,gate.run(()->{fail();return null;}));
        source.setResult(null);
        assertSame(first,gate.run(()->{fail();return null;}));
        assertEquals(1,calls.get());
        gate.registrationStarted();
        assertNull(gate.current());
        assertNotSame(first,gate.run(()->Tasks.forResult(null)));
    }
    @Test public void failureCanRetryAndBlocksRegistration() {
        FcmCleanupGate gate=new FcmCleanupGate();
        gate.run(()->Tasks.forException(new Exception("unavailable")));
        try {gate.registrationStarted();fail();}catch(IllegalStateException expected){}
        assertTrue(gate.run(()->Tasks.forResult(null)).isSuccessful());
        gate.registrationStarted();
    }
    @Test public void pendingCleanupBlocksRegistration() {
        FcmCleanupGate gate=new FcmCleanupGate();
        gate.run(()->new TaskCompletionSource<Void>().getTask());
        try {gate.registrationStarted();fail();}catch(IllegalStateException expected){}
    }
}
