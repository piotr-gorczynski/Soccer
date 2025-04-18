package piotr_gorczynski.soccer2;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler implements
        Thread.UncaughtExceptionHandler {

    public ExceptionHandler() {
    }

    public void uncaughtException(@NonNull Thread thread, Throwable exception) {
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));

        String LINE_SEPARATOR = "\n";
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


        Log.d("pgorczynMove", errorReport);
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }
}
