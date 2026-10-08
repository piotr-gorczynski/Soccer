package piotr_gorczynski.soccer2;

import android.os.Looper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=android.app.Application.class,sdk=34)
public class FcmRegistrationThreadTest {
    @Test public void uiEntryPointsAlwaysDispatchRegistrationOffMainThread() throws Exception {
        CountDownLatch done=new CountDownLatch(2);
        AtomicReference<Thread> worker=new AtomicReference<>();
        SoccerApp app=new SoccerApp() {
            @Override protected synchronized void syncFcmRegistrationOnWorker() {
                worker.set(Thread.currentThread()); done.countDown();
            }
        };
        java.lang.reflect.Field field=SoccerApp.class.getDeclaredField("fcmExecutor");
        field.setAccessible(true);
        try {
            assertSame(Looper.getMainLooper().getThread(),Thread.currentThread());
            app.syncFcmRegistrationIfNeeded(); // MenuActivity / cleanup-success callback path
            app.enableFcmAutoInit(); // Auth listener path
            assertTrue(done.await(5,TimeUnit.SECONDS));
            assertNotSame(Looper.getMainLooper().getThread(),worker.get());
        } finally { ((ExecutorService)field.get(app)).shutdownNow(); }
    }
}
