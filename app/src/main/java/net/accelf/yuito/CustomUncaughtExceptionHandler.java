package net.accelf.yuito;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.keylesspalace.tusky.settings.PrefKeys;

import java.io.PrintWriter;
import java.io.StringWriter;

public class CustomUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private Context context;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    public CustomUncaughtExceptionHandler(Context context) {
        this.context = context;

        mDefaultUncaughtExceptionHandler = Thread
                .getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, Throwable e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        String stackTrace = stringWriter.toString();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(PrefKeys.STACK_TRACE, stackTrace).apply();

        mDefaultUncaughtExceptionHandler.uncaughtException(thread, e);
    }

}
