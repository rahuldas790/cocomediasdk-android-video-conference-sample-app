package buzz.getcoco.media.sample;

import android.os.Bundle;
import android.text.Editable;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import buzz.getcoco.media.MediaSession;
import buzz.getcoco.media.sample.databinding.ActivityInviteUserBinding;

/**
 * Invite users into the session specified.
 */
public class InviteUserActivity extends AppCompatActivity {

  public static final String SESSION_ID = "session_id";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    String sessionId = getIntent().getStringExtra(SESSION_ID);
    ActivityInviteUserBinding binding = ActivityInviteUserBinding.inflate(getLayoutInflater());

    setContentView(binding.getRoot());

    MediaSession session = new MediaSession.DoNothingBuilder(this)
        .setSessionId(sessionId)
        .build();

    session
        .getUsers()
        .observe(this, response -> {
          if (null != response.getError()) {
            Toast.makeText(this, "error getting users of this session", Toast.LENGTH_SHORT).show();
            return;
          }

          Toast.makeText(this, "users: " + response.getValue(), Toast.LENGTH_LONG).show();
        });

    binding.btnContinue.setOnClickListener(v -> {

      Editable etUsername = binding.etUsername.getText();
      String username = (null == etUsername) ? "" : etUsername.toString();

      if (username.isEmpty()) {
        Toast.makeText(this, "enter valid user name", Toast.LENGTH_SHORT).show();
        return;
      }

      Toast.makeText(this, "inviting", Toast.LENGTH_SHORT).show();

      session
          .inviteExternalUser(username)
          .observe(this, response -> {
            if (null != response.getError()) {
              Toast.makeText(this, "error inviting user", Toast.LENGTH_SHORT).show();
              return;
            }

            Toast.makeText(this, "invited: " + username, Toast.LENGTH_SHORT).show();
          });
    });
  }
}
