package com.blizur.androidsdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ScreenTracker implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ScreenTracker";
    private String currentScreen;

    private static boolean isTrackingScreenEnabled = false;
    private Activity gActivity;

    public static void initialize(Context context) {
        try {
            isAutomaticScreenTrackingEnabled(context);
        } catch (Exception e) {
//            Log.e(TAG, "Error while reading metadata: " + e.getMessage());
        }
    }

    private static void isAutomaticScreenTrackingEnabled(Context context) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle metaData = appInfo.metaData;
            if (metaData != null) {
                boolean isAutomaticScreenTrackingEnabled = metaData.getBoolean(AppBlizurConstants.AUTOMATIC_SCREEN_TRACKING_ENABLED, false);
//                Log.d(TAG, "Automatic Screen Tracking Enabled: " + isAutomaticScreenTrackingEnabled);
                isTrackingScreenEnabled = isAutomaticScreenTrackingEnabled;
            }
        } catch (PackageManager.NameNotFoundException e) {
//            Log.e(TAG, "Error while reading metadata: " + e.getMessage());
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
//        Log.d(TAG, "onActivityCreated");
        gActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        gActivity = activity;
        currentScreen = getActivityName(activity);
//        Log.d(TAG, "onActivityStarted");
        if (isTrackingScreenEnabled) {
            handleScreenVisited(activity, currentScreen);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
//        Log.d(TAG, "onActivityResumed");
        currentScreen = getActivityName(activity);
        if (isTrackingScreenEnabled) {
            handleScreenVisited(activity, currentScreen);
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
//        Log.d(TAG, "onActivityPaused");
        currentScreen = getActivityName(activity);
        if (isTrackingScreenEnabled) {
            handleScreenLeft(activity, currentScreen);
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
//        Log.d(TAG, "onActivityStopped");
        currentScreen = getActivityName(activity);
        if (isTrackingScreenEnabled) {
            handleScreenLeft(activity, currentScreen);
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
//        Log.d(TAG, "onActivityDestroyed");
        currentScreen = getActivityName(activity);
        if (isTrackingScreenEnabled) {
            handleScreenLeft(activity, currentScreen);
        }
    }

    private String getActivityName(Activity activity) {
        if (activity instanceof FragmentActivity) {
            FragmentActivity fragmentActivity = (FragmentActivity) activity;
            if (fragmentActivity.getSupportFragmentManager().getFragments().size() > 0) {
                // If there are fragments, use the top fragment's class name as the screen name
                return fragmentActivity.getSupportFragmentManager().getFragments().get(0).getClass().getSimpleName();
            }
        }
        // If there are no fragments or the activity is not a FragmentActivity, use the activity's class name
        return activity.getClass().getSimpleName();
    }

    private void trackViewsInActivity(Activity activity) {
        if (activity instanceof AppCompatActivity) {
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            FragmentManager fragmentManager = appCompatActivity.getSupportFragmentManager();
//            Log.d(TAG, "Size: " + fragmentManager.getFragments().size());
            if (fragmentManager.getFragments().size() > 0) {
                Fragment topFragment = fragmentManager.getFragments().get(0);
                String viewName = topFragment.getClass().getSimpleName();
//                Log.d(TAG, "Top Fragment: " + viewName);
//                sendScreenToServer(viewName);
            }
        }
    }

    private void handleScreenLeft(Context context, String screenName) {
        try {
//            Log.d(TAG, "handleScreenLeft called, screenName: " + screenName);
//            Log.d(TAG, "inside handleScreenLeft..");
            if (!TextUtils.isEmpty(screenName)) {
                Map<String, String> requestBodyMap = new HashMap<>();
                String accessToken = AppBlizurPreferences.getString(context, "accessToken", null);
                if (accessToken != null) {
                    requestBodyMap.put("token", accessToken);
                    requestBodyMap.put("event", AppBlizurConstants.SOCKET_EVENT_SCREEN_LEFT);
                    requestBodyMap.put("pageUrl", screenName);
                    String requestBody = new JSONObject(requestBodyMap).toString();
                    SocketManager.emit(AppBlizurConstants.SOCKET_EVENT_SCREEN_LEFT, requestBody);
                }
            }
        } catch (Exception e) {
//            Log.e(TAG, "Error while handling handling screen left: " + e.getMessage());
        }
    }

    private void handleScreenVisited(Context context, String screenName) {
        try {
//            Log.d(TAG, "handleScreenVisited called, screenName: " + screenName);
            if (TextUtils.isEmpty(screenName)) {
                return;
            }
            AppBlizurPreferences.saveString(context, "previousScreenName", screenName);
            Map<String, String> requestBodyMap = new HashMap<>();
            String accessToken = AppBlizurPreferences.getString(context, "accessToken", null);
            if (accessToken != null) {
                requestBodyMap.put("token", accessToken);
                requestBodyMap.put("event", AppBlizurConstants.SOCKET_EVENT_SCREEN_VISITED);
                requestBodyMap.put("pageUrl", screenName);
                String requestBody = new JSONObject(requestBodyMap).toString();
                SocketManager.emit(AppBlizurConstants.SOCKET_EVENT_SCREEN_VISITED, requestBody);
            }
        } catch (Exception e) {
//            Log.e(TAG, "Error while handling handling screen visited: " + e.getMessage());
        }
    }

    public void screenVisited(String screenName) {
//        Log.d(TAG, "screenVisited, Screen Name: " + screenName);
        handleScreenVisited(gActivity, screenName);
    }

    public void screenLeft(String screenName) {
//        Log.d(TAG, "screenLeft, Screen Name: " + screenName);
        handleScreenLeft(gActivity, screenName);
    }
}
