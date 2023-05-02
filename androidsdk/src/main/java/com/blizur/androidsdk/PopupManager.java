package com.blizur.androidsdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.PopupWindow;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public class PopupManager {

    private static PopupWindow popupWindow;

    public static void showPopup(Context context, Activity activity, JSONObject popupJson, @Nullable View anchorView) {
        try {
            if (popupWindow != null) {
                popupWindow.dismiss();
            }
            String title = popupJson.getString("title");
            String joinCta = popupJson.getString("cta");
            final String meetingUrl = popupJson.getString("meetingUrl");
            JSONObject declineData = new JSONObject();
            declineData.put("reason", "user declined");

            View popupView = LayoutInflater.from(activity).inflate(R.layout.popup_layout, null);
            TextView titleTextView = popupView.findViewById(R.id.titleTextView);
            Button joinButton = popupView.findViewById(R.id.joinButton);
            ImageButton closeButton = popupView.findViewById(R.id.closeButton);

            titleTextView.setText(title);
            joinButton.setText(joinCta);

            joinButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SocketManager.emit(AppBlizurConstants.SOCKET_EVENT_ACCEPTED_REQUEST, popupJson);
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(meetingUrl));
                    activity.startActivity(browserIntent);
                }
            });

            // Set the popup window width and height
            int width = LinearLayout.LayoutParams.MATCH_PARENT;
            int height = LinearLayout.LayoutParams.WRAP_CONTENT;
            popupWindow = new PopupWindow(popupView, width, height);

            // Set the popup window to be focusable
            popupWindow.setFocusable(true);

            // Set the popup window background color to be transparent
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Set the popup window animation style
            popupWindow.setAnimationStyle(R.style.PopupAnimation);

            // Show the popup window at the bottom of the screen
            popupWindow.showAtLocation(activity.getWindow().getDecorView().getRootView(), Gravity.BOTTOM, 0, 0);

            closeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    long expiryTimeSeconds = 24*60*60; // Set the expiry time to one day in seconds
                    long currentTimestamp = System.currentTimeMillis() / 1000; // Get the current Unix timestamp in seconds
                    long expiryTimestamp = currentTimestamp + expiryTimeSeconds; // Calculate the expiry Unix timestamp in seconds

                    AppBlizurPreferences.saveLong(context, "isRequestDeclined_expiry_timestamp", expiryTimestamp);
                    AppBlizurPreferences.saveBoolean(context, "isRequestDeclined", true);
                    SocketManager.emit(AppBlizurConstants.SOCKET_EVENT_DECLINED_REQUEST, declineData);
                    popupWindow.dismiss();
                }
            });
        } catch (JSONException error) {
            // Log.e("Show Popup", error.toString());
        }
    }

    public static void hidePopup() {
        if (popupWindow != null) {
            popupWindow.dismiss();
            popupWindow = null;
        }
    }
}
