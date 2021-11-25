package buzz.getcoco.media.sample;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import buzz.getcoco.media.sample.databinding.ActivityCreateSessionBinding;

/**
 * Determines to start a media session or join an existing one.
 */
public class CreateSessionActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ActivityCreateSessionBinding binding =
        ActivityCreateSessionBinding.inflate(getLayoutInflater());

    super.onCreate(savedInstanceState);

    setContentView(binding.getRoot());

    binding.btnContinue.setOnClickListener(v -> {
      String sessionName = binding.etSession.getEditableText().toString();

      finish();

      startActivity(new Intent(this, CallerActivity.class)
          .putExtra(CallerActivity.SESSION_CREATE, true)
          .putExtra(CallerActivity.SESSION_EXTRA, sessionName));
    });
  }
}
