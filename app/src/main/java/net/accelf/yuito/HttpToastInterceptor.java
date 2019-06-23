package net.accelf.yuito;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class HttpToastInterceptor implements Interceptor {

    private Context context;

    public HttpToastInterceptor(Context context) {
        this.context = context;
    }

    @Override
    @NonNull
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            toast(request.method() + " " + request.url() + "\n" + e);
            throw e;
        }

        int code = response.code();
        if (code == 200) {
            return response;
        }

        toast(request.method() + " " + request.url() + "\n" + code + " " + response.message());

        return response;
    }

    private void toast(String text) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(context, text, Toast.LENGTH_SHORT).show());

    }
}
