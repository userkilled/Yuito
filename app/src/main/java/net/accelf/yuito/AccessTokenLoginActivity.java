package net.accelf.yuito;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.keylesspalace.tusky.MainActivity;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.db.AccountManager;
import com.keylesspalace.tusky.di.Injectable;

import java.io.IOException;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AccessTokenLoginActivity extends AppCompatActivity implements Injectable {

    @Inject
    AccountManager accountManager;

    TextInputEditText domainEditText;
    TextInputEditText accessTokenEditText;
    TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_token_login);

        domainEditText = findViewById(R.id.domainEditText);
        accessTokenEditText = findViewById(R.id.accessTokenEditText);
        Button authorizeButton = findViewById(R.id.authorizeButton);
        logTextView = findViewById(R.id.logTextView);

        authorizeButton.setOnClickListener(v -> authorize());
        log("Input domain and access token to login.");
    }

    private void log(String text) {
        runOnUiThread(() -> logTextView.setText(String.format("%s\n%s", logTextView.getText().toString(), text)));
    }

    private void authorize() {
        if (domainEditText.getText() != null) {
            String domain = domainEditText.getText().toString();
            String accessToken = accessTokenEditText.getText().toString();
            HttpUrl url;

            log("Starting login test. [domain: " + domain + ", accessToken: " + accessToken + "]");

            try {
                url = new HttpUrl.Builder().host(domain).scheme("https")
                        .addPathSegments("/api/v1/accounts/verify_credentials")
                        .addQueryParameter("access_token", accessToken)
                        .build();
            } catch (IllegalArgumentException e) {
                log("Wrong domain format. " + e.getMessage());
                log("Aborting.");
                return;
            }

            log("Access start -> " + url.toString());

            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url(url).get().build();
            okHttpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    log("Login failed. " + e.getMessage());
                    log("Aborting.");
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (response.body() != null) {
                        log(response.body().string());
                    }
                    if (response.code() != 200) {
                        throw new IOException("Invalid response code. Response code was " + response.code());
                    }
                    log("Login successful. Moving to account registration phase.");
                    authSucceeded(domain, accessToken);
                }
            });
        }
    }

    private void authSucceeded(String domain, String accessToken) {
        accountManager.addAccount(accessToken, domain);
        log("Completed. Enjoy!");

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.explode, R.anim.explode);
    }
}
