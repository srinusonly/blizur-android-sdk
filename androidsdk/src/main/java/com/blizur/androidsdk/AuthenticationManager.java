package com.blizur.androidsdk;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AuthenticationManager {
    private static AuthenticationManager instance;
    private RefreshAccessTokenTask refreshAccessTokenTask;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String AUTH_ENDPOINT = "/v1/auth/verify-creds";
    private static final String REFRESH_AUTH_ENDPOINT = "/v1/auth/refresh-tokens";

    private AuthenticationManager() {}

    public static AuthenticationManager getInstance() {
        if (instance == null) {
            instance = new AuthenticationManager();
        }
        return instance;
    }
    private Map<String, String> getValues(String response) throws JSONException {
        JSONObject jsonObj = new JSONObject(response);
        Map<String, String> accesTokensMap = new HashMap<>();

        // Extract access token and its expiry time
        JSONObject accessObj = jsonObj.getJSONObject("tokens").getJSONObject("access");
        String accessToken = accessObj.getString("token");
        String accessTokenExpiry = accessObj.getString("expires");

        // Extract refresh token and its expiry time
        JSONObject refreshObj = jsonObj.getJSONObject("tokens").getJSONObject("refresh");
        String refreshToken = refreshObj.getString("token");
        String refreshTokenExpiry = refreshObj.getString("expires");

        // Extract user ID
        String userId = jsonObj.getString("userId");

        accesTokensMap.put("accessToken", accessToken);
        accesTokensMap.put("accessTokenExpiry", accessTokenExpiry);
        accesTokensMap.put("refreshToken", refreshToken);
        accesTokensMap.put("refreshTokenExpiry", refreshTokenExpiry);
        accesTokensMap.put("userId", userId);

        return accesTokensMap;
    }

    private void saveTokens(String response, Context context) throws IOException, JSONException {

        Map<String, String> accesTokensMap = getValues(response);

        AppBlizurPreferences.saveString(context, "accessToken", accesTokensMap.get("accessToken"));
        AppBlizurPreferences.saveString(context, "accessTokenExpiry", accesTokensMap.get("accessTokenExpiry"));
        AppBlizurPreferences.saveString(context, "refreshToken", accesTokensMap.get("refreshToken"));
        AppBlizurPreferences.saveString(context, "refreshTokenExpiry", accesTokensMap.get("refreshTokenExpiry"));
        AppBlizurPreferences.saveString(context, "userId", accesTokensMap.get("userId"));
    }
    private boolean isAccessTokenValid(Context context) {

        String accessToken = AppBlizurPreferences.getString(context, "accessToken", null);
        String accessTokenExpiry = AppBlizurPreferences.getString(context, "accessTokenExpiry", null);

        if (accessToken == null || accessTokenExpiry == null) {
            return false;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        LocalDateTime expiryDateTime = LocalDateTime.parse(accessTokenExpiry, formatter);

        // Convert the expiry time to a Unix timestamp in seconds
        long expiryTimestamp = expiryDateTime.atZone(ZoneId.of("UTC")).toEpochSecond();

        // Check if the access token is still valid
        if (expiryTimestamp > System.currentTimeMillis() / 1000L) {
            return true;
        } else {
            return false;
        }
    }

    private class AccessTokenRequestTask extends AsyncTask<String, Void, Boolean> {
        private Context mContext;

        public AccessTokenRequestTask(Context context) {
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            OkHttpClient client = new OkHttpClient();
            Map<String, String> requestBodyMap = new HashMap<>();
            String userId = AppBlizurPreferences.getString(mContext, "userId", null);

            requestBodyMap.put("clientId", params[0]);
            requestBodyMap.put("secret", params[1]);
            requestBodyMap.put("platform", "android");
            if (userId != null) {
                requestBodyMap.put("userId", userId);
            }
            String requestBody = new JSONObject(requestBodyMap).toString();
            String url = AppBlizurConstants.API_BASE_URL + AUTH_ENDPOINT;
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(JSON, requestBody))
                    .build();

            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    saveTokens(response.body().string(), mContext);
                    return true;
                }
            } catch (IOException | JSONException e) {
//                e.printStackTrace();
                Log.e("AccessTokenRequestTask", e.getMessage());
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Handle the result of the access token request
        }
    }

    private class RefreshAccessTokenTask extends AsyncTask<Void, Void, Boolean> {
        private Context context;

        public RefreshAccessTokenTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();
            String refreshToken = AppBlizurPreferences.getString(context, "refreshToken", null);

            if (refreshToken == null) {
                return false;
            }

            Map<String, String> requestBodyMap = new HashMap<>();
            requestBodyMap.put("refreshToken", refreshToken);

            String requestBody = new JSONObject(requestBodyMap).toString();
            String url = AppBlizurConstants.API_BASE_URL + REFRESH_AUTH_ENDPOINT;
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(JSON, requestBody))
                    .build();
            try {
                Response response = client.newCall(request).execute();

                // Check if the response is successful
                if (response.isSuccessful()) {
                    saveTokens(response.body().string(), context);
                    return true;
                } else {
                    return false;
                }
            } catch (IOException | JSONException e) {
//                e.printStackTrace();
                Log.e("RefreshAccessTokenTask", e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            refreshAccessTokenTask = null;
            if (!result) {
                // Handle failure case
            }
        }
    }

    public void handleAuthentication(String apiKey, String secret, Context context) throws IOException, JSONException {
        boolean isAccessTokenPresent = isAccessTokenValid(context);
        boolean isTokenRefreshed = false;
        if (!isAccessTokenPresent) {
            if (refreshAccessTokenTask == null) {
//                Log.d("handleAuthentication", "hitting refresh token api..");
                refreshAccessTokenTask = new RefreshAccessTokenTask(context);
                refreshAccessTokenTask.execute();
            }
        }
        if (isAccessTokenPresent || isTokenRefreshed) {
//            Log.d("handleAuthentication", "not hitting verify creds api, token is available..");
            return;
        }
//        Log.d("handleAuthentication", "hitting verify creds api for authentication..");
        new AccessTokenRequestTask(context).execute(apiKey, secret);
    }
}
