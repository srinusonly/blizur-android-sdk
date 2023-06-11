package com.blizur.androidsdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import android.util.Log;

public class BlizurAPI {
    private static BlizurAPI instance;

    private static String TAG = "BlizurAPI";
    private static ScreenTracker screenTracker;

    public static BlizurAPI getInstance() {
        if (instance == null) {
            instance = new BlizurAPI();
        }
        return instance;
    }

    public static void init(Context context, Application application, String apiKey, String secret) {
        if (context instanceof Activity) {
            try {
//                Log.d("BlizurAPI", "init called");
                final Activity activity = (Activity) context;
                screenTracker = new ScreenTracker();
                screenTracker.initialize(context);
                application.registerActivityLifecycleCallbacks(screenTracker);
                AuthenticationManager.getInstance().handleAuthentication(apiKey, secret, context, activity);
//                Log.d("BlizurAPI", "Setup is done");
            } catch (Exception e) {
                Log.e("Error while setting up blizur", e.getMessage());
            }
        } else {
            Log.d("Blizur setup", "Context is not an instance of Activity. To  setup blizur, pass an instance of an Activity to getInstance.");
        }
    }
    public static void screenVisited(String screenName) {
//        Log.d(TAG, "screenVisited called, screenName: " + screenName);
        if (screenTracker != null && screenName != null) {
            screenTracker.screenVisited(screenName);
        }
    }
    public static void screenLeft(String screenName) {
//        Log.d(TAG, "screenLeft called, screenName: "+screenName);
        if (screenTracker != null && screenName != null) {
            screenTracker.screenLeft(screenName);
        }
    }
}
