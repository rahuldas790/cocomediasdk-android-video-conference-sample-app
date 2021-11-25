package buzz.getcoco.media.sample;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import buzz.getcoco.media.MediaSession;
import buzz.getcoco.media.sample.databinding.ActivitySplashBinding;

/**
 * Checks for login and shows login prompt.
 */
public class SplashActivity extends AppCompatActivity {

  private static final String TAG = "SplashActivity";

  private ActivitySplashBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Utils.init(this);

    super.onCreate(savedInstanceState);

    binding = ActivitySplashBinding.inflate(getLayoutInflater());

    setContentView(binding.getRoot());

    new Handler(Looper.getMainLooper()).postDelayed(this::proceed, 1_000);
  }

  private void proceed() {
    MediaSession nothingSession = new MediaSession.DoNothingBuilder(this).build();

    if (Utils.isLoggedIn()) {

      Utils.login().observe(this, tokens -> {
        Log.d(TAG, "proceed: using tokens: " + tokens);

        if (null == tokens) {
          Toast.makeText(this, "error fetching tokens, restart app", Toast.LENGTH_SHORT).show();
          return;
        }

        nothingSession.setAuthTokens(tokens);

        startActivity(new Intent(this, ViewSessionsActivity.class));
        finish();
      });
      return;
    }

    binding.tvAppName.setVisibility(View.GONE);

    binding.etBaseUrl.setVisibility(View.VISIBLE);
    binding.etUsername.setVisibility(View.VISIBLE);
    binding.btnContinue.setVisibility(View.VISIBLE);

    binding.btnContinue.setOnClickListener(v -> {

      Editable etBaseUrl = binding.etBaseUrl.getText();
      Editable etUsername = binding.etUsername.getText();

      String baseUrl = (null == etBaseUrl) ? "" : etBaseUrl.toString();
      String username = (null == etUsername) ? "" : etUsername.toString();

      if (baseUrl.isEmpty()) {
        Toast.makeText(this, "enter valid base url", Toast.LENGTH_SHORT).show();
        return;
      }

      if (username.isEmpty()) {
        Toast.makeText(this, "enter valid user name", Toast.LENGTH_SHORT).show();
        return;
      }

      Toast.makeText(this, "logging in", Toast.LENGTH_SHORT).show();

      Utils.setBaseUrl(this, baseUrl);
      Utils.setUsername(this, username);

      Utils.login()
          .observe(this, tokens -> {
            Log.d(TAG, "proceed: using tokens: " + tokens);

            if (null == tokens) {
              Toast.makeText(this, "error fetching tokens", Toast.LENGTH_SHORT).show();
              return;
            }

            nothingSession.setAuthTokens(tokens);

            startActivity(new Intent(this, ViewSessionsActivity.class));
            finish();
          });
    });
  }
}
