package buzz.getcoco.media.sample;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import buzz.getcoco.media.MediaSession;
import buzz.getcoco.media.sample.adapter.SessionsAdapter;
import buzz.getcoco.media.sample.databinding.ActivityViewSessionsBinding;

/**
 * Get the sessions the user has created / invited to.
 */
public class ViewSessionsActivity extends AppCompatActivity {

  private static final String TAG = "ViewSessionActivity";

  private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
      new ActivityResultContracts.RequestMultiplePermissions(),
      grants -> {
        for (Boolean granted : grants.values()) {
          if (granted) {
            continue;
          }

          Toast
              .makeText(this,
                  "permissions MUST be granted, goto settings and grant permissions",
                  Toast.LENGTH_SHORT)
              .show();
          return;
        }
      });

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    SwipeRefreshLayout.OnRefreshListener listener;
    ActivityViewSessionsBinding binding = ActivityViewSessionsBinding.inflate(getLayoutInflater());
    MediaSession nothingSession = new MediaSession.DoNothingBuilder(this).build();

    SessionsAdapter adapter = new SessionsAdapter(new SessionsAdapter.OnClickListener() {
      @Override
      public void onClick(MediaSession.SessionHandle handle) {
        Intent intent = new Intent(ViewSessionsActivity.this, CallerActivity.class);

        intent.putExtra(CallerActivity.SESSION_CREATE, false);
        intent.putExtra(CallerActivity.SESSION_EXTRA, handle.getId());

        startActivity(intent);
      }

      @Override
      public void onDelete(MediaSession.SessionHandle handle) {
        handle.doNothingUpon(ViewSessionsActivity.this)
            .build()
            .delete()
            .observe(ViewSessionsActivity.this, response -> {
              String message;

              if (null != response.getError()) {
                Log.d(TAG, "onDelete: error", response.getError());
                message = "error deleting session: " + handle.getName();
              } else {
                message = "deleted: " + handle.getName();
              }

              Toast.makeText(ViewSessionsActivity.this, message, Toast.LENGTH_SHORT).show();
            });
      }
    });

    super.onCreate(savedInstanceState);

    binding.rvSessions.setAdapter(adapter);

    binding.srSessions.setOnRefreshListener(listener = () -> {

      Log.d(TAG, "onCreate: refreshing");

      nothingSession
          .getAllSessions()
          .observe(this, sessions -> {
            binding.srSessions.setRefreshing(false);

            if (null != sessions.getError()) {
              Toast.makeText(this, "error while fetching all sessions", Toast.LENGTH_SHORT).show();
              return;
            }

            adapter.setSessions(sessions.getValue());
          });
    });

    binding.srSessions.setRefreshing(true);
    listener.onRefresh();

    binding.btnCreateSession.setOnClickListener(v -> {
      Log.d(TAG, "onCreate: creating new session");
      startActivity(new Intent(this, CreateSessionActivity.class));
    });

    nothingSession.getAuthListener().observe(this, endpoint -> {
      Log.d(TAG, "onCreate: endpoint: " + endpoint);

      Utils.login()
          .observe(this, tokens -> {
            Log.d(TAG, "onCreate: setting tokens: " + tokens);
            nothingSession.setAuthTokens(tokens);
          });
    });

    setContentView(binding.getRoot());
  }

  @Override
  protected void onResume() {
    super.onResume();

    permissionLauncher.launch(new String[] {
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    });
  }
}
