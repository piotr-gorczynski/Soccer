package piotr_gorczynski.soccer2;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BalloonPositioningTest {

    @Test
    public void balloonUsesOppositeHalfFromActiveBall() {
        assertTrue(Field.shouldPlaceBalloonAtBottom(200f, 500f));
        assertFalse(Field.shouldPlaceBalloonAtBottom(800f, 500f));
        assertFalse(Field.shouldPlaceBalloonAtBottom(500f, 500f));
    }

    @Test
    public void detectsMovementLineCrossingBalloon() {
        assertTrue(Field.lineIntersectsRectangle(
                400f, 300f, 600f, 700f,
                300f, 450f, 700f, 550f));
    }

    @Test
    public void ignoresMovementLineOutsideBalloon() {
        assertFalse(Field.lineIntersectsRectangle(
                400f, 100f, 600f, 200f,
                300f, 450f, 700f, 550f));

        assertFalse(Field.lineIntersectsRectangle(
                50f, 450f, 100f, 450f,
                300f, 450f, 700f, 550f));
    }
}
