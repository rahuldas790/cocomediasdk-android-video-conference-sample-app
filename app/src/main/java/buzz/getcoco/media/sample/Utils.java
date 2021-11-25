package buzz.getcoco.media.sample;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

class Utils {

  private static final String TAG = "Utils";

  private static final String USER_INFO_PERF_KEY = "buzz.getcoco.media.sample.user_info";
  private static final String USERNAME_KEY = "username";
  private static final String BASE_URL_KEY = "base_url";

  @Nullable
  private static String username;

  @Nullable
  private static String baseUrl;

  public static void init(@NonNull Context c) {
    SharedPreferences userInfo = c.getSharedPreferences(USER_INFO_PERF_KEY, Context.MODE_PRIVATE);

    Utils.baseUrl = userInfo.getString(BASE_URL_KEY, null);
    Utils.username = userInfo.getString(USERNAME_KEY, null);
  }

  public static void setUsername(@NonNull Context c, @NonNull String username) {
    SharedPreferences userInfo = c.getSharedPreferences(USER_INFO_PERF_KEY, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = userInfo.edit();

    editor.putString(USERNAME_KEY, username);
    editor.apply();

    Utils.username = username;
  }

  public static void setBaseUrl(@NonNull Context c, @NonNull String baseUrl) {
    SharedPreferences userInfo = c.getSharedPreferences(USER_INFO_PERF_KEY, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = userInfo.edit();

    editor.putString(BASE_URL_KEY, baseUrl);
    editor.apply();

    Utils.baseUrl = baseUrl;
  }

  public static boolean isLoggedIn() {
    return null != username && !username.isEmpty();
  }

  @NonNull
  public static String requireUsername() {
    return Objects.requireNonNull(username);
  }

  public static String requireBaseUrl() {
    return Objects.requireNonNull(baseUrl);
  }

  @NonNull
  public static LiveData<String> login() {
    String username = requireUsername();
    String baseUrl = requireBaseUrl();
    MutableLiveData<String> responseLiveData = new MutableLiveData<>();

    OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followSslRedirects(false) // to stop redirects if any
        .followRedirects(false)
        .build();

    Request request = new Request.Builder()
        .post(
            RequestBody.create(
                String.format(Locale.US, "{\"userId\":\"%s\"}", username),
                MediaType.parse("application/json"))
        )
        .url(String.format("http://%s/v1.0/token/fetch-user-token", baseUrl))
        .build();

    Log.d(TAG, "login: sending request: " + request);

    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(@NonNull Call call, @NonNull IOException e) {
        Log.d(TAG, "onFailure: error", e);
        responseLiveData.postValue(null);
      }

      @Override
      public void onResponse(@NonNull Call call, @NonNull Response response)
          throws IOException, NullPointerException {

        ResponseBody responseBody = response.body();

        Log.d(TAG, "onResponse: response: " + response);
        Log.d(TAG, "onResponse: responseBody: " + responseBody);

        if (!response.isSuccessful()) {
          int code = response.code();

          Log.d(TAG, "onResponse: failed to fetch tokens, error: " + code);

          responseLiveData.postValue(null);
          return;
        }

        try {
          Objects.requireNonNull(responseBody);

          String responseStr = responseBody.string();
          responseLiveData.postValue(responseStr);
        } catch (IOException | NullPointerException e) {
          responseLiveData.postValue(null);
          throw e;
        }
      }
    });

    return responseLiveData;
  }
}
