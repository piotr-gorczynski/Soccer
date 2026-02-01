package piotr_gorczynski.soccer2;

import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AppFlavourDetector
 */
@RunWith(RobolectricTestRunner.class)
public class AppFlavourDetectorTest {

    @Test
    public void testGetCurrentFlavour_GlobalPackage() {
        // Create mock context with global package name
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2");
        
        String flavour = AppFlavourDetector.getCurrentFlavour(mockContext);
        
        assertEquals("global", flavour);
    }

    @Test
    public void testGetCurrentFlavour_BangladeshPackage() {
        // Create mock context with Bangladesh package name
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2.bd");
        
        String flavour = AppFlavourDetector.getCurrentFlavour(mockContext);
        
        assertEquals("bangladesh", flavour);
    }

    @Test
    public void testIsBangladeshFlavour_True() {
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2.bd");
        
        assertTrue(AppFlavourDetector.isBangladeshFlavour(mockContext));
    }

    @Test
    public void testIsBangladeshFlavour_False() {
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2");
        
        assertFalse(AppFlavourDetector.isBangladeshFlavour(mockContext));
    }

    @Test
    public void testIsGlobalFlavour_True() {
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2");
        
        assertTrue(AppFlavourDetector.isGlobalFlavour(mockContext));
    }

    @Test
    public void testIsGlobalFlavour_False() {
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.soccer2.bd");
        
        assertFalse(AppFlavourDetector.isGlobalFlavour(mockContext));
    }

    @Test
    public void testGetCurrentFlavour_UnknownPackage_DefaultsToGlobal() {
        // Test with unknown package name (should default to global)
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("com.example.unknown");
        
        String flavour = AppFlavourDetector.getCurrentFlavour(mockContext);
        
        assertEquals("global", flavour);
    }

    @Test
    public void testGetCurrentFlavour_BdInMiddle_NotBangladesh() {
        // Test that .bd must be at the end
        Context mockContext = mock(Context.class);
        when(mockContext.getPackageName()).thenReturn("piotr_gorczynski.bd.soccer2");
        
        String flavour = AppFlavourDetector.getCurrentFlavour(mockContext);
        
        assertEquals("global", flavour);
    }
}
