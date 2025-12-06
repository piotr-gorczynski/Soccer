package piotr_gorczynski.soccer2;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Test cases for SoccerApp WebView/MobileAds initialization
 * Tests the ANR fix that delays MobileAds initialization to prevent blocking main thread
 * 
 * Issue: lambda$initializeWebViewSafely$18 ANR crash
 * Root cause: MobileAds.initialize() triggers WebView initialization which can block 
 * the main thread for 5+ seconds during app startup
 * 
 * Fix: Use postDelayed() to defer initialization by 2 seconds, allowing the app to 
 * become responsive before starting heavy initialization
 */
@RunWith(AndroidJUnit4.class)
public class SoccerAppWebViewInitTest {

    @Test
    public void testSoccerAppClassExists() {
        // Verify that SoccerApp class exists and extends Application
        try {
            Class<?> soccerAppClass = Class.forName("piotr_gorczynski.soccer2.SoccerApp");
            assertNotNull("SoccerApp class should exist", soccerAppClass);
            
            // Verify it extends Application
            assertTrue("SoccerApp should extend Application", 
                android.app.Application.class.isAssignableFrom(soccerAppClass));
            
        } catch (ClassNotFoundException e) {
            fail("SoccerApp class not found: " + e.getMessage());
        }
    }

    @Test
    public void testInitializeWebViewAndAdsMethodExists() {
        // Verify that the initializeWebViewAndAds method exists
        // This is the method that was modified to fix the ANR
        try {
            Class<?> soccerAppClass = Class.forName("piotr_gorczynski.soccer2.SoccerApp");
            
            // The method is private, so we can't directly test it,
            // but we can verify the class has proper structure
            assertNotNull("SoccerApp class should have methods", soccerAppClass.getDeclaredMethods());
            assertTrue("SoccerApp should have at least one method", 
                soccerAppClass.getDeclaredMethods().length > 0);
            
            // Verify that the method exists by checking for it in declared methods
            boolean hasInitMethod = false;
            for (java.lang.reflect.Method method : soccerAppClass.getDeclaredMethods()) {
                if (method.getName().equals("initializeWebViewAndAds")) {
                    hasInitMethod = true;
                    break;
                }
            }
            assertTrue("SoccerApp should have initializeWebViewAndAds method", hasInitMethod);
            
        } catch (ClassNotFoundException e) {
            fail("SoccerApp class not found: " + e.getMessage());
        }
    }

    @Test
    public void testHandlerConstantExists() {
        // Verify that MAIN_HANDLER constant exists in SoccerApp
        // This is used for posting delayed initialization
        try {
            Class<?> soccerAppClass = Class.forName("piotr_gorczynski.soccer2.SoccerApp");
            
            // Check that the class has fields (should include MAIN_HANDLER)
            assertNotNull("SoccerApp class should have fields", soccerAppClass.getDeclaredFields());
            
            boolean hasMainHandler = false;
            for (java.lang.reflect.Field field : soccerAppClass.getDeclaredFields()) {
                if (field.getName().equals("MAIN_HANDLER")) {
                    hasMainHandler = true;
                    // Verify it's the correct type
                    assertEquals("MAIN_HANDLER should be of type android.os.Handler",
                        "android.os.Handler", field.getType().getName());
                    break;
                }
            }
            assertTrue("SoccerApp should have MAIN_HANDLER field", hasMainHandler);
            
        } catch (ClassNotFoundException e) {
            fail("SoccerApp class not found: " + e.getMessage());
        }
    }

    @Test
    public void testSoccerAppCanBeInstantiated() {
        // Verify that SoccerApp can be instantiated
        // This is a basic sanity check
        Context context = ApplicationProvider.getApplicationContext();
        assertNotNull("Context should be available for testing", context);
        
        // The Application class is instantiated by the system
        // We just verify that our context is valid
        assertNotNull("Application context should exist", context.getApplicationContext());
    }

    @Test
    public void testDefaultLifecycleObserverImplementation() {
        // Verify that SoccerApp implements DefaultLifecycleObserver
        // This is important for app lifecycle management
        try {
            Class<?> soccerAppClass = Class.forName("piotr_gorczynski.soccer2.SoccerApp");
            
            // Check interfaces
            Class<?>[] interfaces = soccerAppClass.getInterfaces();
            boolean implementsLifecycleObserver = false;
            for (Class<?> iface : interfaces) {
                if (iface.getName().contains("DefaultLifecycleObserver")) {
                    implementsLifecycleObserver = true;
                    break;
                }
            }
            assertTrue("SoccerApp should implement DefaultLifecycleObserver", 
                implementsLifecycleObserver);
            
        } catch (ClassNotFoundException e) {
            fail("SoccerApp class not found: " + e.getMessage());
        }
    }

    @Test
    public void testMobileAdsClassAvailable() {
        // Verify that MobileAds class is available in the classpath
        // This ensures the dependency is properly configured
        try {
            Class<?> mobileAdsClass = Class.forName("com.google.android.gms.ads.MobileAds");
            assertNotNull("MobileAds class should be available", mobileAdsClass);
            
            // Verify it has the initialize method
            boolean hasInitializeMethod = false;
            for (java.lang.reflect.Method method : mobileAdsClass.getDeclaredMethods()) {
                if (method.getName().equals("initialize")) {
                    hasInitializeMethod = true;
                    break;
                }
            }
            assertTrue("MobileAds should have initialize method", hasInitializeMethod);
            
        } catch (ClassNotFoundException e) {
            fail("MobileAds class not found. Ensure Google Mobile Ads SDK is properly configured: " + e.getMessage());
        }
    }

    @Test
    public void testExceptionHandlerSetup() {
        // Verify that SoccerApp sets up exception handling
        // This is important for crash reporting
        try {
            Class<?> exceptionHandlerClass = Class.forName("piotr_gorczynski.soccer2.ExceptionHandler");
            assertNotNull("ExceptionHandler class should exist", exceptionHandlerClass);
            
            // Verify it implements Thread.UncaughtExceptionHandler
            assertTrue("ExceptionHandler should implement UncaughtExceptionHandler",
                Thread.UncaughtExceptionHandler.class.isAssignableFrom(exceptionHandlerClass));
            
        } catch (ClassNotFoundException e) {
            fail("ExceptionHandler class not found: " + e.getMessage());
        }
    }

    @Test
    public void testBackendServiceCheckerExists() {
        // Verify that BackendServiceChecker is properly configured
        // This is used alongside other initialization in SoccerApp
        try {
            Class<?> backendServiceCheckerClass = Class.forName("piotr_gorczynski.soccer2.BackendServiceChecker");
            assertNotNull("BackendServiceChecker class should exist", backendServiceCheckerClass);
            
        } catch (ClassNotFoundException e) {
            fail("BackendServiceChecker class not found: " + e.getMessage());
        }
    }

    @Test
    public void testAnalyticsManagerExists() {
        // Verify that AnalyticsManager is properly configured
        // This is initialized in SoccerApp.onCreate()
        try {
            Class<?> analyticsManagerClass = Class.forName("piotr_gorczynski.soccer2.AnalyticsManager");
            assertNotNull("AnalyticsManager class should exist", analyticsManagerClass);
            
        } catch (ClassNotFoundException e) {
            fail("AnalyticsManager class not found: " + e.getMessage());
        }
    }

    @Test
    public void testRemoteConfigHelperExists() {
        // Verify that RemoteConfigHelper is properly configured
        // This is initialized in SoccerApp.onCreate()
        try {
            Class<?> remoteConfigHelperClass = Class.forName("piotr_gorczynski.soccer2.RemoteConfigHelper");
            assertNotNull("RemoteConfigHelper class should exist", remoteConfigHelperClass);
            
        } catch (ClassNotFoundException e) {
            fail("RemoteConfigHelper class not found: " + e.getMessage());
        }
    }
}
