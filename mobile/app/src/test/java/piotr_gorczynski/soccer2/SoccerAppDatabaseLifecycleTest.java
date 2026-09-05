package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SoccerAppDatabaseLifecycleTest {

    @Test
    public void heartbeatWorkerKeepsDatabaseOnlineWhenAppIsForeground() {
        assertFalse(SoccerApp.shouldDisconnectDatabase(true));
    }

    @Test
    public void heartbeatWorkerDisconnectsDatabaseWhenAppIsBackgrounded() {
        assertTrue(SoccerApp.shouldDisconnectDatabase(false));
    }
}
