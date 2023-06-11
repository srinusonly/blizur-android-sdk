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

    private static final String TAG = "SocketManager";

    private static Context gContext;
    private static Socket socket;

    public static Socket getSocket() {
        return socket;
    }

    public static void setup(Context reactContext, Activity activity) {
        Context context = reactContext;
        gContext = context;
        long currentTimestamp = System.currentTimeMillis() / 1000; // Get the current Unix timestamp in seconds
        long expiryTimestamp = AppBlizurPreferences.getLong(context, "isRequestDeclined_expiry_timestamp", 0);
        if (currentTimestamp < expiryTimestamp) {
            return;
        }
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    connect();
                } catch (Exception e) {
//                    Log.e("SocketManager", "Failed to connect to socket", e);
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

    public static void connect() {
        try {
            if (socket != null && socket.connected()) {
                return;
            }
            //        Log.d("BlizurAPI", "connect::establishing socket connection..");
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            options.reconnectionDelay = 2000;
            options.reconnectionDelayMax = 60000;
            options.reconnectionAttempts = Integer.MAX_VALUE;
            options.timeout = 10000;
            options.transports = new String[]{"websocket"};
            socket = IO.socket(AppBlizurConstants.SOCKET_URL, options);
            socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Exception ex = (Exception) args[0];
                    //                Log.e("BlizurAPI", "Connection error: " + ex.getMessage());
                }
            });
            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
//                    Log.d(TAG, "connect event called..");
                    String token = AppBlizurPreferences.getString(gContext, "accessToken", null);
                    emit(AppBlizurConstants.SOCKET_EVENT_INIT_CONFIG, token);
                }
            });
            socket.connect();
        } catch (URISyntaxException exception) {
//            Log.e("BlizurAPI", "Exception while setting up socket" + exception.getMessage());
        }
    }

    public static void disconnect() {
//        Log.d("BlizurAPI", "disconnect::disconnecting socket connection..");
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }

    public static void on(String event, Emitter.Listener listener) {
//        Log.d("BlizurAPI", "on::emitting event.."+event);
        if (socket != null) {
            socket.on(event, listener);
        }
    }

    public static void off(String event, Emitter.Listener listener) {
//        Log.d("BlizurAPI", "off::emitting event..");
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
