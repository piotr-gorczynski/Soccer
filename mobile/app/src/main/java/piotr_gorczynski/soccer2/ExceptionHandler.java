package piotr_gorczynski.soccer2;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler implements
        Thread.UncaughtExceptionHandler {
    private final Activity myContext;
    private final String LINE_SEPARATOR = "\n";

    public ExceptionHandler(Activity context) {
        myContext = context;
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));

        String errorReport = "************ CAUSE OF ERROR ************\n\n" +
                stackTrace +
                "\n************ DEVICE INFORMATION ***********\n" +
                "Brand: " +
                Build.BRAND +
                LINE_SEPARATOR +
                "Device: " +
                Build.DEVICE +
                LINE_SEPARATOR +
                "Model: " +
                Build.MODEL +
                LINE_SEPARATOR +
                "Id: " +
                Build.ID +
                LINE_SEPARATOR +
                "Product: " +
                Build.PRODUCT +
                LINE_SEPARATOR +
                "\n************ FIRMWARE ************\n" +
                "SDK: " +
                Build.VERSION.SDK_INT +
                LINE_SEPARATOR +
                "Release: " +
                Build.VERSION.RELEASE +
                LINE_SEPARATOR +
                "Incremental: " +
                Build.VERSION.INCREMENTAL +
                LINE_SEPARATOR;

        //Intent intent = new Intent(myContext, CrashActivity.class); //start a new activity to show error message
        //intent.putExtra("error", errorReport.toString());
        //myContext.startActivity(intent);
        Log.d("pgorczynMove", errorReport);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
