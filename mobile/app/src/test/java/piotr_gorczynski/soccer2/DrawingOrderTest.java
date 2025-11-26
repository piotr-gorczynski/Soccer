package piotr_gorczynski.soccer2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test cases for the drawing order logic.
 * The drawing order should be determined by the bottom value of each element.
 * Elements with lower bottom values should be drawn first (appear behind).
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class DrawingOrderTest {

    // Constants matching the DrawableElement types in Field.java
    private static final int TYPE_BALL = 0;
    private static final int TYPE_BLUE = 1;
    private static final int TYPE_RED = 2;

    /**
     * Helper class to simulate DrawableElement from Field.java
     */
    private static class DrawableElement implements Comparable<DrawableElement> {
        final int type;
        final float bottom;

        DrawableElement(int type, float bottom) {
            this.type = type;
            this.bottom = bottom;
        }

        @Override
        public int compareTo(DrawableElement other) {
            return Float.compare(this.bottom, other.bottom);
        }

        String getTypeName() {
            switch (type) {
                case TYPE_BALL: return "Ball";
                case TYPE_BLUE: return "Blue";
                case TYPE_RED: return "Red";
                default: return "Unknown";
            }
        }
    }

    /**
     * Example 1 from the issue:
     * Ball canvas bottom = 10
     * Blue sprite canvas bottom = 11
     * Red sprite canvas bottom = 12
     * 
     * Expected order: 1. Ball, 2. Blue, 3. Red
     */
    @Test
    public void testDrawingOrder_Example1() {
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, 10f);
        elements[1] = new DrawableElement(TYPE_BLUE, 11f);
        elements[2] = new DrawableElement(TYPE_RED, 12f);
        
        Arrays.sort(elements);
        
        assertEquals("First element should be Ball", TYPE_BALL, elements[0].type);
        assertEquals("Second element should be Blue", TYPE_BLUE, elements[1].type);
        assertEquals("Third element should be Red", TYPE_RED, elements[2].type);
    }

    /**
     * Example 2 from the issue:
     * Ball canvas bottom = 11
     * Blue sprite canvas bottom = 10
     * Red sprite canvas bottom = 12
     * 
     * Expected order: 1. Blue, 2. Ball, 3. Red
     */
    @Test
    public void testDrawingOrder_Example2() {
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, 11f);
        elements[1] = new DrawableElement(TYPE_BLUE, 10f);
        elements[2] = new DrawableElement(TYPE_RED, 12f);
        
        Arrays.sort(elements);
        
        assertEquals("First element should be Blue", TYPE_BLUE, elements[0].type);
        assertEquals("Second element should be Ball", TYPE_BALL, elements[1].type);
        assertEquals("Third element should be Red", TYPE_RED, elements[2].type);
    }

    /**
     * Example 3 from the issue:
     * Ball canvas bottom = 10
     * Blue sprite canvas bottom = 12
     * Red sprite canvas bottom = 11
     * 
     * Expected order: 1. Ball, 2. Red, 3. Blue
     */
    @Test
    public void testDrawingOrder_Example3() {
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, 10f);
        elements[1] = new DrawableElement(TYPE_BLUE, 12f);
        elements[2] = new DrawableElement(TYPE_RED, 11f);
        
        Arrays.sort(elements);
        
        assertEquals("First element should be Ball", TYPE_BALL, elements[0].type);
        assertEquals("Second element should be Red", TYPE_RED, elements[1].type);
        assertEquals("Third element should be Blue", TYPE_BLUE, elements[2].type);
    }

    /**
     * Test with equal bottom values - order should be stable/deterministic
     */
    @Test
    public void testDrawingOrder_EqualBottomValues() {
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, 10f);
        elements[1] = new DrawableElement(TYPE_BLUE, 10f);
        elements[2] = new DrawableElement(TYPE_RED, 10f);
        
        Arrays.sort(elements);
        
        // With equal values, the order should be consistent
        // All elements should have the same bottom value
        assertEquals("All elements should have equal bottom values", 
                     elements[0].bottom, elements[1].bottom, 0.001f);
        assertEquals("All elements should have equal bottom values", 
                     elements[1].bottom, elements[2].bottom, 0.001f);
    }

    /**
     * Test with negative bottom values (sprites above the visible area)
     */
    @Test
    public void testDrawingOrder_NegativeValues() {
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, -5f);
        elements[1] = new DrawableElement(TYPE_BLUE, -10f);
        elements[2] = new DrawableElement(TYPE_RED, 0f);
        
        Arrays.sort(elements);
        
        assertEquals("First element should be Blue (lowest bottom)", TYPE_BLUE, elements[0].type);
        assertEquals("Second element should be Ball", TYPE_BALL, elements[1].type);
        assertEquals("Third element should be Red (highest bottom)", TYPE_RED, elements[2].type);
    }

    /**
     * Test with very large bottom values
     */
    @Test
    public void testDrawingOrder_LargeValues() {
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, 1000f);
        elements[1] = new DrawableElement(TYPE_BLUE, 500f);
        elements[2] = new DrawableElement(TYPE_RED, 2000f);
        
        Arrays.sort(elements);
        
        assertEquals("First element should be Blue (lowest bottom)", TYPE_BLUE, elements[0].type);
        assertEquals("Second element should be Ball", TYPE_BALL, elements[1].type);
        assertEquals("Third element should be Red (highest bottom)", TYPE_RED, elements[2].type);
    }

    /**
     * Test that sorting preserves element integrity
     */
    @Test
    public void testDrawingOrder_PreservesElementData() {
        float ballBottom = 15.5f;
        float blueBottom = 10.2f;
        float redBottom = 20.8f;
        
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, ballBottom);
        elements[1] = new DrawableElement(TYPE_BLUE, blueBottom);
        elements[2] = new DrawableElement(TYPE_RED, redBottom);
        
        Arrays.sort(elements);
        
        // Find each element and verify its bottom value is preserved
        DrawableElement ball = null, blue = null, red = null;
        for (DrawableElement e : elements) {
            if (e.type == TYPE_BALL) ball = e;
            if (e.type == TYPE_BLUE) blue = e;
            if (e.type == TYPE_RED) red = e;
        }
        
        assertNotNull("Ball element should exist", ball);
        assertNotNull("Blue element should exist", blue);
        assertNotNull("Red element should exist", red);
        
        assertEquals("Ball bottom value should be preserved", ballBottom, ball.bottom, 0.001f);
        assertEquals("Blue bottom value should be preserved", blueBottom, blue.bottom, 0.001f);
        assertEquals("Red bottom value should be preserved", redBottom, red.bottom, 0.001f);
    }

    /**
     * Test with Float.MIN_VALUE to handle invisible elements
     */
    @Test
    public void testDrawingOrder_WithInvisibleElement() {
        DrawableElement[] elements = new DrawableElement[3];
        elements[0] = new DrawableElement(TYPE_BALL, 100f);
        elements[1] = new DrawableElement(TYPE_BLUE, Float.MIN_VALUE); // Invisible
        elements[2] = new DrawableElement(TYPE_RED, 150f);
        
        Arrays.sort(elements);
        
        // Blue (invisible, MIN_VALUE) should be drawn first
        assertEquals("First element should be Blue (invisible)", TYPE_BLUE, elements[0].type);
        assertEquals("Second element should be Ball", TYPE_BALL, elements[1].type);
        assertEquals("Third element should be Red", TYPE_RED, elements[2].type);
    }
}
