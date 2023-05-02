package com.blizur.androidsdk;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketManager {
    private static Socket socket;

    public static Socket getSocket() {
        return socket;
    }

    public static void setup(Context reactContext, Activity activity) {
        Context context = reactContext;
        long currentTimestamp = System.currentTimeMillis() / 1000; // Get the current Unix timestamp in seconds
        long expiryTimestamp = AppBlizurPreferences.getLong(context, "isRequestDeclined_expiry_timestamp", 0);
        if (currentTimestamp < expiryTimestamp) {
            return;
        }
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String token = AppBlizurPreferences.getString(context, "accessToken", null);
                    connect();
                    emit(AppBlizurConstants.SOCKET_EVENT_INIT_CONFIG, token);
                } catch (Exception e) {
                    Log.e("SocketManager", "Failed to connect to socket", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                on(AppBlizurConstants.SOCKET_EVENT_SHOW_POPUP_REQUEST, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject jsonPayload = (JSONObject) args[0];
                                long currentTimestamp = System.currentTimeMillis() / 1000; // Get the current Unix timestamp in seconds
                                long expiryTimestamp = AppBlizurPreferences.getLong(context, "isRequestDeclined_expiry_timestamp", 0);
                                if (currentTimestamp >= expiryTimestamp) {
                                    PopupManager.showPopup(context, activity, jsonPayload, null);
                                }
                            }
                        });
                    }
                });

                on(AppBlizurConstants.SOCKET_EVENT_HIDE_POPUP_REQUEST, new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                PopupManager.hidePopup();
                            }
                        });
                    }
                });
            }
        }.execute();
    }

    public static void connect() throws URISyntaxException {
        socket = IO.socket(AppBlizurConstants.SOCKET_URL);
        socket.connect();
    }

    public static void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }

    public static void on(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.on(event, listener);
        }
    }

    public static void off(String event, Emitter.Listener listener) {
        if (socket != null) {
            socket.off(event, listener);
        }
    }

    public static void emit(String event, Object... args) {
        if (socket != null) {
            socket.emit(event, args);
        }
    }
}
