package buzz.getcoco.media.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.core.app.ActivityCompat;
import buzz.getcoco.media.CameraStreamHandler;
import buzz.getcoco.media.MediaSession;
import buzz.getcoco.media.MicStreamHandler;
import buzz.getcoco.media.Node;
import buzz.getcoco.media.sample.databinding.ActivityCallerBinding;
import buzz.getcoco.media.ui.NodePlayerView;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.common.collect.ImmutableList;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

/**
 * An activity to start/join a call.
 * NOTE:
 * Requires arguments to be passed in bundle:
 * <pre>
 *   - {@link #SESSION_CREATE} : boolean
 *     flag to indicate if a session has to be created.
 *
 *   - {@link #SESSION_EXTRA}  : String
 *      a) Name of the session if {@link #SESSION_CREATE} is true
 *      b) Session id retrieved using {@link MediaSession.SessionHandle#getId()}
 *         if {@link #SESSION_CREATE} is false.
 * </pre>
 */
public class CallerActivity extends AppCompatActivity {

  public static final String SESSION_CREATE = "start";
  public static final String SESSION_EXTRA = "session_extra";

  private static final String TAG = "CallerActivity";

  private static final String CHANNEL_NAME = "call channel";
  private static final String CHANNEL_METADATA = "-";

  private MediaSession session;

  private ArrayList<ExoPlayer> players;
  private ImmutableList<Node> participants = ImmutableList.of();

  private ImmutableList<NodePlayerView> playerViews;

  private ActivityCallerBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);

    boolean createMode = getIntent().getBooleanExtra(SESSION_CREATE, false);
    String sessionExtra = getIntent().getStringExtra(SESSION_EXTRA);

    Objects.requireNonNull(sessionExtra);

    binding = ActivityCallerBinding.inflate(getLayoutInflater());

    Log.d(TAG, "onCreate: createMode: " + createMode + ", session: " + sessionExtra);

    setContentView(binding.getRoot());

    if (ActivityCompat.checkSelfPermission(this,
        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(this,
        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

      Toast.makeText(this, "grant the permissions", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }

    if (createMode) {
      session = new MediaSession.CreateBuilder(this)
          .setName(sessionExtra)
          .setMetadata(DateTimeFormatter
              .ofPattern("dd-MM-yyyy hh:mm a")
              .format(ZonedDateTime.now()))
          .addChannel(new MediaSession.ChannelBuilder(CHANNEL_NAME, CHANNEL_METADATA))
          .build();
    } else {
      session = new MediaSession.JoinBuilder(this)
          .setSessionId(sessionExtra)
          .build();
    }

    session
        .getAuthListener()
        .observe(this, authEndpoint -> {
          Log.d(TAG, "onCreate: tokens expired, logging in");

          Utils.login()
              .observe(this, tokens -> {
                Log.d(TAG, "onCreate: setting tokens: " + tokens);
                session.setAuthTokens(tokens);
              });
        });

    session
        .start()
        .observe(this, response -> {
          Log.d(TAG, "onCreate: response: " + response);

          if (null != response.getError()) {
            Toast
                .makeText(this, "error: " + response.getError().getMessage(), Toast.LENGTH_SHORT)
                .show();
            return;
          }

          MediaSession.ChannelNodesContainer nodesContainer = response
              .getValue()
              .stream()
              .filter(container -> container.getChannelName().equals(CHANNEL_NAME))
              .findFirst()
              .orElse(null);

          if (null == nodesContainer) {
            Toast.makeText(this, "channel not present", Toast.LENGTH_SHORT).show();
            return;
          }

          this.participants = nodesContainer.getNodes();
          updateParticipants();
        });

    Log.d(TAG, "onCreate: adding streams");

    addMicStreamHandler();
    addCameraStreamHandler(CameraSelector.DEFAULT_FRONT_CAMERA);

    binding.btnFlipCam.setOnClickListener(v -> {
      CameraSelector selector =
          binding.btnFlipCam.isChecked()
              ? CameraSelector.DEFAULT_FRONT_CAMERA
              : CameraSelector.DEFAULT_BACK_CAMERA;

      addCameraStreamHandler(selector);
    });

    // ends call for everyone
    binding.btnEndCall.setOnClickListener(v -> {
      Toast.makeText(this, "ending session", Toast.LENGTH_SHORT).show();

      session
          .delete()
          .observe(this, response -> {
            if (null == response.getError()) {
              Toast.makeText(this, "session ended", Toast.LENGTH_SHORT).show();
              finish();
            } else {
              Log.w(TAG, "onCreate: error", response.getError());
              Toast.makeText(this, "failed to end session", Toast.LENGTH_SHORT).show();
            }
          });
    });

    binding.btnInvite.setOnClickListener(v -> {
      Toast.makeText(this, "resolving id to invite", Toast.LENGTH_SHORT).show();
      session.getHandle().observe(this, response -> {
        Intent intent = new Intent(this, InviteUserActivity.class);

        if (null != response.getError()) {
          Toast.makeText(this, "error fetching session info", Toast.LENGTH_SHORT).show();
          return;
        }

        intent.putExtra(InviteUserActivity.SESSION_ID, response.getValue().getId());

        startActivity(intent);
      });
    });

    binding.btnSend.setOnClickListener(v -> {
      String message = binding.etMessage.getText().toString();

      binding.etMessage.setText("");

      session.sendMessage(message);
    });

    binding.btnSendContentInfo.setOnClickListener(v -> {
      String message = binding.etMessage.getText().toString();

      binding.etMessage.setText("");

      session.sendContentInfoMessage(message, 0);
    });

    // using message listener from Network
    session.setMessageReceivedListener((message, sourceNodeId) -> {
      Log.d(TAG, "onCreate: message received from: " + sourceNodeId);

      runOnUiThread(() -> Toast.makeText(CallerActivity.this,
          sourceNodeId + ": " + message, Toast.LENGTH_SHORT).show());
    });

    session.setContentInfoReceivedListener((message, sourceNodeId, contentTime) -> {
      Log.d(TAG, "onCreate: message"
          + " sourceNodeId: " + sourceNodeId
          + ", message: " + message
          + ", contentTime: " + contentTime);

      runOnUiThread(() -> Toast.makeText(this,
          sourceNodeId + ": " + contentTime + ": " + message, Toast.LENGTH_SHORT).show());
    });

    session.setNodeStatusListener((nodeId, isOnline) -> {
      Log.d(TAG, "onCreate: onNodeStatusChanged: nodeId: " + nodeId + ", isOnline: " + isOnline);

      runOnUiThread(() ->
          Toast.makeText(this,
              "node: " + nodeId + ", isOnline: " + isOnline,
              Toast.LENGTH_SHORT)
              .show());
    });

    session.getConnectionStatus().observe(this, state -> {
      Log.d(TAG, "onCreate: status: " + state);
      Toast.makeText(this, "status: " + state, Toast.LENGTH_SHORT).show();
    });

    playerViews = ImmutableList.of(
        binding.pvParticipantA,
        binding.pvParticipantB,
        binding.pvParticipantC
    );
  }

  @Override
  protected void onStart() {
    super.onStart();

    for (int i = 0; i < playerViews.size(); i++) {
      NodePlayerView pv = playerViews.get(i);
      pv.onResume();
    }

    players = new ArrayList<>();
  }

  @Override
  protected void onResume() {
    super.onResume();
    updateParticipants();
  }

  @Override
  protected void onStop() {
    super.onStop();

    for (int i = 0; i < playerViews.size(); i++) {
      NodePlayerView pv = playerViews.get(i);
      pv.onPause();
      pv.setPlayer(null);
    }

    for (ExoPlayer player : players) {
      player.release();
    }

    players = null;
  }

  private void updateParticipants() {
    ImmutableList<Node> participants = this.participants;
    int participantsSize = participants.size();

    Log.d(TAG, "updateParticipants: nodes: " + participants);

    if (participants.size() > playerViews.size()) {
      // for sake of simplicity.
      // Better with a recycler view ?

      Log.d(TAG, "updateParticipants: chopping nodes "
          + "from: " + participants.size()
          + ", to: " + playerViews.size());

      participants = participants.subList(0, playerViews.size());
    }

    for (int i = 0; i < participantsSize; i++) {
      NodePlayerView pv = playerViews.get(i);

      if (null == pv.getPlayer()) {
        ExoPlayer player = MediaSession.getLowLatencyPlayer(this);

        players.add(player);
        pv.setPlayer(player);
      }

      pv.bindToNode(participants.get(i));
    }

    for (int i = participantsSize; i < playerViews.size(); i++) {
      ExoPlayer player;
      NodePlayerView pv = playerViews.get(i);

      if (null != (player = pv.getPlayer())) {
        player.release();
      }

      pv.setPlayer(null);
    }
  }

  @RequiresPermission(Manifest.permission.RECORD_AUDIO)
  private void addMicStreamHandler() {

    session
        .addStream(new MicStreamHandler.Builder(CHANNEL_NAME))
        .observe(this, audioHandler -> {

          if (null == audioHandler) {
            Log.w(TAG, "onCreate: unable to start the stream");
            return;
          }

          Log.d(TAG, "onCreate: handler: " + audioHandler);

          audioHandler.start();

          audioHandler.bindToLifecycle(this);

          binding.btnAudio.setOnClickListener(v -> {
            boolean isEnabled = binding.btnAudio.isChecked();

            Log.d(TAG, "addMicStreamHandler: enabled: " + isEnabled);

            if (isEnabled) {
              audioHandler.start();
            } else {
              audioHandler.stop();
            }

            Log.d(TAG, "addMicStreamHandler: command sent");
          });
        });
  }

  @RequiresPermission(Manifest.permission.CAMERA)
  private void addCameraStreamHandler(CameraSelector selector) {

    session
        .addStream(new CameraStreamHandler.Builder(
            CHANNEL_NAME,
            CameraStreamHandler.VideoQuality.SD,
            selector))
        .observe(this, videoHandler -> {

          if (null == videoHandler) {
            Log.w(TAG, "onCreate: unable to start the stream");
            return;
          }

          Log.d(TAG, "onCreate: handler: " + videoHandler);

          videoHandler.start();
          videoHandler.bindToLifecycle(this, this, binding.pvSelf);

          binding.btnVideo.setOnClickListener(v -> {
            boolean isEnabled = binding.btnVideo.isChecked();

            Log.d(TAG, "addCameraStreamHandler: enabled: " + isEnabled);

            if (isEnabled) {
              videoHandler.start();
              videoHandler.bindToLifecycle(this, this, binding.pvSelf);
            } else {
              videoHandler.stop();
            }

            Log.d(TAG, "addCameraStreamHandler: command sent");
          });
        });
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    if (null != session) {
      session.stop();
    }

    binding = null;
    playerViews = null;
    players = null;
  }
}
