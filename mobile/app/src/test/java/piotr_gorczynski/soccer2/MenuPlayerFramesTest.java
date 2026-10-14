package piotr_gorczynski.soccer2;

import android.app.Application;
import android.graphics.Bitmap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=34)
public class MenuPlayerFramesTest {
    @Test public void realResourcePreservesAllDirectionsAndFramesWithQuarterPixelMemory() {
        Bitmap[][] frames=MenuPlayerFrames.load(RuntimeEnvironment.getApplication().getResources(),()->false);
        try {
            assertEquals(16,frames.length);
            long bytes=0;
            for(Bitmap[] row:frames) {
                assertEquals(22,row.length);
                for(Bitmap frame:row) {
                    assertEquals(64,frame.getWidth());assertEquals(64,frame.getHeight());
                    assertFalse(frame.isRecycled());bytes+=frame.getAllocationByteCount();
                }
            }
            assertTrue(bytes<=16L*22*64*64*4);
        } finally { MenuPlayerFrames.recycle(frames); }
    }
    @Test public void allocationFailureReleasesSheetAndPartialFrames() {
        Bitmap sheet=Bitmap.createBitmap(1408,1024,Bitmap.Config.ARGB_8888);
        Bitmap first=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);
        int[] calls={0};
        try {
            MenuPlayerFrames.split(sheet,()->false,(s,x,y,size)->{
                if(calls[0]++==0)return first;
                throw new OutOfMemoryError("simulated frame allocation failure");
            });
            fail("Expected OOM");
        } catch(OutOfMemoryError expected) {
            assertTrue(sheet.isRecycled());assertTrue(first.isRecycled());
        }
    }
    @Test public void cancellationReleasesPartialFrames() {
        Bitmap sheet=Bitmap.createBitmap(1408,1024,Bitmap.Config.ARGB_8888);
        Bitmap first=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);
        boolean[] cancelled={false};
        assertNull(MenuPlayerFrames.split(sheet,()->cancelled[0],(s,x,y,size)->{
            cancelled[0]=true;return first;
        }));
        assertTrue(sheet.isRecycled());assertTrue(first.isRecycled());
    }
}
