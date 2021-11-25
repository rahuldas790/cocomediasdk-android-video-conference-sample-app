# cocomediasdk-android

# setup for running this app

## Get a Client ID using
- Head to [https://manage.getcoco.buzz](https://manage.getcoco.buzz) (Sign up if needed)
- Go to Applications tab
  ![step 1](docs/1.png?raw=true "Client ID Step 1")
- Click on + Application button
  ![step 2](docs/2.png?raw=true "Client ID Step 2")
- Choose the application name, group and type
  ![step 3](docs/3.png?raw=true "Client ID Step 3")
- Choose the capabilities and click submit
  ![step 4](docs/4.png?raw=true "Client ID Step 4")
- Click on the Created Application
  ![step 5](docs/5.png?raw=true "Client ID Step 5")
- Get the client id present there
  ![step 6](docs/6.png?raw=true "Client ID Step 6")

## Setup on server for custom login
- Clone [sample token server](https://github.com/elear-solutions/nodesampletokenserver)
  preferably in an linux machine and run the steps provided in that repo's README.md
- If the machine has domain name then it will be accessed using it's domain
- If the machine is local to the phone running the app. Then get the ip address of the machine
  running token server along with the port of the sample server

## Running the app
- Build and run the app
- Pass the ip_address:port (eg: 192.168.0.1:8080) or the domain name of the token server setup in
  above step in the "Base Url" and pass a random value in "User Name". Remember to note the
  username value it will be used during invite user

## Setup for other android apps
- Add jitpack dependency in root level build.gradle
  ```groovy
  allprojects {
    repositories {
      maven { url 'https://jitpack.io' }
    }
  }
  ```
- Add media sdk dependency in module level build.gradle
  ```groovy
  implementation 'buzz.getcoco:cocomediasdk-java:0.0.3-lite'
  implementation 'buzz.getcoco:cocomediasdk-android:0.0.4'
  implementation 'com.github.elear-solutions:ExoPlayer:1781b9d720'
  implementation 'androidx.camera:camera-view:1.0.0-alpha31'
  ```
- Add client id present in step 6 in AndroidManifest (Need not for Service Apps with custom login)
  ```xml
    <meta-data
      android:name="buzz.getcoco.auth.client_id"
      android:value="<client_id>" />
  ```
- Add Camera & Microphone permission in Manifest file
  ```xml
  <manifest>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
  </manifest>
  ```
- Fetch the tokens using OkHttp
  ```java
  class Scratch {

    public static String login(String baseUrl, String username) {

      OkHttpClient client = new OkHttpClient.Builder()
          .connectTimeout(30, TimeUnit.SECONDS)
          .writeTimeout(30, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .build();

      Request request = new Request.Builder()
          .post(
              RequestBody.create(
                  String.format(Locale.US, "{\"userId\":\"%s\"}", username),
                  MediaType.parse("application/json"))
          )
          .url(String.format("http://%s/v1.0/token/fetch-user-token", baseUrl))
          .build();

      Response response;

      try {
        response = client.newCall(request).execute();
      } catch (IOException ioe) {
        ioe.printStackTrace();
        response = null;
      }

      if (null == response || !response.isSuccessful()) {
        return null;
      }

      ResponseBody responseBody = response.body();

      if (null == responseBody) {
        return null;
      }

      String responseBodyStr;

      try {
        responseBodyStr = responseBody.string();
      } catch (IOException ioe) {
        ioe.printStackTrace();
        responseBodyStr = null;
      }

      return responseBodyStr;
    }
  }
  ```
- Set the access token
  ```java
  class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      /*
       * POST: http://{{external_token_server_url}}/v1.0/token/fetch-user-token
       * AUTH: NONE
       * BODY: {
       *         "userId":"foo"
       *       }
       */
      String tokens = "<fetch the tokens using>";

      new MediaSession.DoNothingBuilder(this)
         .build()
         .setTokens(tokens);
    }
  }
  ```
- Get all sessions of the current user and join all of them
  ```java
  class ViewSessionsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      new MediaSession.DoNothingBuilder(this)
          .getAllSessions()
          .observe(this, sessions -> {
            if (null != sessions.getError()) {
              Toast.makeText(this, "error while fetching all sessions", Toast.LENGTH_SHORT).show();
              return;
            }

            sessions.getValue().stream().forEach(handle -> {
              MediaSession session = handle.joinUpon(this).build();

              // join and start recieving data on the underlying network
              session.start();

              // delete the session and underlying network
              session.delete();

              // disconnect from the network
              session.stop();
            });
          });
    }
  }
  ```
- Creating a session and adding a channel
  ```java
  class CallerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      MediaSession session = new MediaSession.CreateBuilder(this)
          .setName("whatever")
          .setMetadata("anything")
          .addChannel(new MediaSession.ChannelBuilder("channel name", "channel metadata"))
          .build();

      // fetching session handle and network id
      session.getHandle().observe(this, handle -> {
        String networkId = handle.getValue().getId();
        String networkName = handle.getValue().getName();
        String metadata = handle.getValue().getMetadata();
      });
    }
  }
  ```
- Fetching users & Inviting User to created Network
  ```java
  class InviteUserActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

      MediaSession session = new MediaSession.DoNothingBuilder(this)
        .setSessionId("<Session Id>")
        .build();

      nothingSession.getUsers().observe(this, response -> {
        if (null != response.getError()) {
          response.getValue();
        }
      });

      session.inviteExternalUser("<external user id>").observe(this, response -> {
        if (null != response.getError()) {
          // success
        }
      });
    }
  }
  ```
- Sending Data to other nodes
  ```java
  class CallerActivity extends AppCompatActivity {
    private MediaSession session;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      session = new MediaSession.JoinBuilder(this)
        .setSessionId("<Session Id>")
        .build();

      session.start();

      session.getConnectionStatus().observe(this, status -> {
        // wait till session is connected, else sendMessage will fail.
      });

      // send message to every one in the network
      session.sendMessage("hello world");

      // send data to nodes with node id 1, 2
      session.sendMessage("hello world", 1, 2);

      // send content info to all nodes in the network
      session.sendContentInfoMessage("hello world", (int) System.currentTimeMillis());

      // send data to nodes with node id 1, 2
      session.sendContentInfoMessage("hello world", (int) System.currentTimeMillis(), 1, 2);
    }

    @Override
    protected void onDestroy() {
      session.stop();
    }
  }
  ```
- Listening for messages, content info
  ```java
  class CallerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      // Join or create a session

      session.setMessageReceivedListener((message, sourceNodeId) -> {
        // triggered on receiving a new message from sourceNodeId

        // NOTE: This code will be executed on a background thread.
        //       and this listener will be cleared during stop()
      });

      session.setContentInfoReceivedListener((message, sourceNodeId, contentTime) -> {
        // triggered on receiving a new content info message from sourceNodeId

        // NOTE: This code will be executed on a background thread.
        //       and this listener will be cleared during stop()
      });

      session.setNodeStatusListener((nodeId, isOnline) -> {
        // triggered when nodeId becomes online / offline

        // NOTE: This code will be executed on a background thread.
        //       and this listener will be cleared during stop()
      });
    }
  }
  ```
- Listening for connection status
  ```java
  class CallerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      MediaSession session = new MediaSession.JoinBuilder(this)
        .setSessionId("<Session Id>")
        .build();

      session.start();

      session.getConnectionStatus().observe(this, status -> {
        // wait till session is connected, else sendMessage will fail.
      });
    }
  }
  ```
- Adding Mic and Camera Streams to a given channel
  ```java
  class CallerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      MediaSession session = new MediaSession.JoinBuilder(this)
        .setSessionId("<Session Id>")
        .build();

      // Hard-coding channel names would be a best practice.

      // adding camera stream
      session
        .addStream(new CameraStreamHandler.Builder(
            "<CHANNEL NAME>",
            CameraStreamHandler.VideoQuality.SD,
            CameraSelector.DEFAULT_FRONT_CAMERA))
        .observe(this, videoHandler -> {

          if (null == videoHandler) {
            // error
            return;
          }

          videoHandler.start();
          videoHandler.bindToLifecycle(this, this, binding.pvSelf);
        });

      // adding microphone stream
      session
        .addStream(new MicStreamHandler.Builder("<CHANNEL NAME>"))
        .observe(this, audioHandler -> {

          if (null == audioHandler) {
            // error
            return;
          }

          audioHandler.start();
          audioHandler.bindToLifecycle(this);
        });
    }
  }
  ```
- Listening for streams and setting up views for each player
  ```java
    class CallerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
      MediaSession session = new MediaSession.JoinBuilder(this)
        .setSessionId("<Session Id>")
        .build();

      // binding each participant to their corresponding view
      session
        .start()
        .observe(this, response -> {

          if (null != response.getError()) {
            // error
            return;
          }

          MediaSession.ChannelNodesContainer nodesContainer = response
              .getValue()
              .stream()
              .filter(container -> container.getChannelName().equals("<CHANNEL NAME>"))
              .findFirst()
              .orElse(null);

          if (null == nodesContainer) {
            // ignore
            return;
          }

          this.participants = nodesContainer.getNodes();

          if (participants.size() > playerViews.size()) {
            // for sake of simplicity.
            // Better with a recycler view ?

            participants = participants.subList(0, playerViews.size());
          }

          // binding each participant to their corresponding view
          for (int i = 0; i < participants.size(); i++) {
            NodePlayerView pv = playerViews.get(i);

            if (null == pv.getPlayer()) {
              SimpleExoPlayer player = MediaSession.getLowLatencyPlayer(this);

              players.add(player);
              pv.setPlayer(player);
            }

            pv.bindToNode(participants.get(i));
          }

          // unbinding each view from participant
          for (int i = participants.size(); i < playerViews.size(); i++) {
            SimpleExoPlayer player;
            NodePlayerView pv = playerViews.get(i);

            if (null != (player = pv.getPlayer())) {
              player.release();
            }

            pv.setPlayer(null);
          }
        });
    }
  }
  ```
- Rules for proguard and r8 will be added soon here.
# Gist of using the api
```java
class CallerActivity extends AppCompatActivity {
  private MediaSession session;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    session = new MediaSession.CreateBuilder(this)
      .setName("whatever")
      .setMetadata("anything")
      .addChannel(new MediaSession.ChannelBuilder("channel name", "channel metadata"))
      .build();

    session.start().observe(this, container -> {
      // bind participant to the corresponding view
    });
    
    // adding camera stream
    session
      .addStream(new CameraStreamHandler.Builder(
        "channel name",
        CameraStreamHandler.VideoQuality.SD,
        CameraSelector.DEFAULT_FRONT_CAMERA))
      .observe(this, videoHandler -> {
        // start the handler
      });

    // adding microphone stream
    session
      .addStream(new MicStreamHandler.Builder("channel name"))
      .observe(this, audioHandler -> {
        // start the handler
      });

    session.getConnectionStatus().observe(this, status -> {
      // don't sendMessage() until status = connected, else it will fail.
    });

    // send message to every one in the network
    session.sendMessage("hello world");

    // send data to nodes with node id 1, 2
    session.sendMessage("hello world", 1, 2);

    // send content info to all nodes in the network
    session.sendContentInfoMessage("hello world", (int) System.currentTimeMillis());

    // send data to nodes with node id 1, 2
    session.sendContentInfoMessage("hello world", (int) System.currentTimeMillis(), 1, 2);

    session.delete().observe(this, response -> {
      if (null != response.getError()) {
        // success
      }
    });
  }

  @Override
  protected void onDestroy() {
    session.stop();
  }
}
```
