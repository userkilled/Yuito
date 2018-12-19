package net.accelf.yuito;

import android.text.Spanned;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.keylesspalace.tusky.appstore.EventHub;
import com.keylesspalace.tusky.appstore.StatusDeletedEvent;
import com.keylesspalace.tusky.appstore.StreamUpdateEvent;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.entity.StreamEvent;
import com.keylesspalace.tusky.json.SpannedTypeAdapter;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class TimelineStreamingListener extends WebSocketListener {

    private Gson gson = buildGson();
    private boolean isFirstStatus = true;

    private EventHub eventHub;

    private static Gson buildGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Spanned.class, new SpannedTypeAdapter())
                .create();
    }

    public TimelineStreamingListener(EventHub eventHub) {
        this.eventHub = eventHub;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        Log.d("StreamingListener", "Stream connected.");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        StreamEvent event = gson.fromJson(text, StreamEvent.class);

        String payload = event.getPayload();
        switch (event.getEvent()) {
            case UPDATE:
                Status status = gson.fromJson(payload, Status.class);
                eventHub.dispatch(new StreamUpdateEvent(status, isFirstStatus));
                isFirstStatus = false;
                break;
            case DELETE:
                eventHub.dispatch(new StatusDeletedEvent(payload));
                break;
        }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        Log.d("StreamingListener", "Stream closed.");
    }
}
