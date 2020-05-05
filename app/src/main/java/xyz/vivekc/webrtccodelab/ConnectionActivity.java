package xyz.vivekc.webrtccodelab;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoRenderer;

import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import me.kevingleason.pnwebrtc.PnRTCClient;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import tech.gusavila92.websocketclient.WebSocketClient;

public class ConnectionActivity extends AppCompatActivity {

    private static final String TAG = "ConnectionActivity";
    private final String UPDATE_USERS = "updateUsers", LOGIN = "login", OFFER = "offer", ANSWER = "answer", CANDIDATE = "candidate", CONNECTION_ESTABLISHED = "connection_established";
    private ListView listContacts;
    private Button btnInitiate, btnDecline, btnAccept, btnOffer, btnSendCandidate;
    private TextView tvApiMsg;
    private EditText etUsername;
    private ProgressDialog progressDialog;
    private OkHttpClient client;
    private WebSocketClient webSocketClient;
    private PeerConnectionFactory peerConnectionFactory;
    //private PeerConnection peerConnection;

    private PeerConnection remotePeerConnection;
    private PeerConnection localPeerConnection;

    private String myPeerName;
    private String partnerPeerName;

    private static String AUDIO_TRACK_ID_LOCAL = "AUDIO_TRACK_ID_LOCAL";
    private static String AUDIO_TRACK_ID_REMOTE = "AUDIO_TRACK_ID_REMOTE";
    private static String LOCAL_MEDIA_STREAM_ID = "LOCAL_MEDIA_STREAM_ID";
    private static String REMOTE_MEDIA_STREAM_ID = "REMOTE_MEDIA_STREAM_ID";

    private NotificationManager notificationManager;


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);


        // client=new OkHttpClient.Builder().addInterceptor(new In);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("voip", "Channel",
                NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
        createPeerConnectionFactory();
        createWebSocketClient();
        progressDialog = new ProgressDialog(ConnectionActivity.this);


        listContacts = findViewById(R.id.list_contacts);
        btnOffer = findViewById(R.id.btn_offer);
        btnInitiate = findViewById(R.id.btn_initiate);
        btnAccept = findViewById(R.id.btn_accept);
        btnDecline = findViewById(R.id.btn_decline);
        btnSendCandidate = findViewById(R.id.btn_send_candidate);
        tvApiMsg = findViewById(R.id.tv_api_msg);
        etUsername = findViewById(R.id.et_user);


        btnInitiate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RequestData requestData = new RequestData();
                requestData.setType(LOGIN);
                myPeerName = etUsername.getText().toString();
                requestData.setName(myPeerName);
                createRequest(requestData);
            }
        });


        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //notificationManager.cancel(0);
                createAnswer();


            }
        });


        btnOffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createOffer();

            }
        });


        btnSendCandidate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });


    }


    private void createWebSocketClient() {
        URI uri;
        try {
            // Connect to local host
            uri = new URI(ConstantUtils.SERVER_SOCKET_URI_DEVICE);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                Log.i("WebSocket", "Session is starting");

            }

            @Override
            public void onTextReceived(String s) {
                progressDialog.dismiss();
                Log.i("WebSocket", "Message received");
                final String message = s;
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    onResponse(jsonObject);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onBinaryReceived(byte[] data) {
            }

            @Override
            public void onPingReceived(byte[] data) {
            }

            @Override
            public void onPongReceived(byte[] data) {
            }

            @Override
            public void onException(Exception e) {
                System.out.println(e.getMessage());
            }

            @Override
            public void onCloseReceived() {
                Log.i("WebSocket", "Closed ");
                System.out.println("onCloseReceived");
            }
        };
        webSocketClient.setConnectTimeout(10000);
        webSocketClient.setReadTimeout(60000);
        webSocketClient.enableAutomaticReconnection(5000);
        webSocketClient.connect();
    }

    private void createRequest(RequestData requestData) {

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://localhost:9000/";

        Gson gson = new Gson();
        String json = gson.toJson(requestData);
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        webSocketClient.send(json);

    }


    private void onResponse(JSONObject response) {

        String type = "";

        try {
            type = response.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        switch (type) {
            case LOGIN:
                parseLogin(response);
                break;
            case OFFER:
                parseOffer(response);
                break;
            case ANSWER:
                parseAnswer(response);
                break;
            case UPDATE_USERS:
                parseUpdateUsers(response);
                break;
            case CANDIDATE:
                parseCandidate(response);
                break;
            case CONNECTION_ESTABLISHED:
                connectionEstablished(response);
                break;


        }
    }


    private void connectionEstablished(JSONObject response) {
        Intent intent = new Intent(ConnectionActivity.this, CallActivity.class);
        startActivity(intent);
    }

    private void parseLogin(JSONObject response) {
        try {
            JSONArray jsonArray = response.getJSONArray("users");

            final ArrayList<String> contacts = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                contacts.add(jsonArray.getJSONObject(i).getString("userName"));
            }

            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(ConnectionActivity.this, android.R.layout.simple_list_item_1, contacts);

            arrayAdapter.notifyDataSetChanged();


            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    listContacts.setAdapter(arrayAdapter);
                    tvApiMsg.setText("Fetched all contacts");

                    listContacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                            partnerPeerName = contacts.get(i);

                        }
                    });
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    private void parseOffer(JSONObject response) {
        try {
            String userOffered = response.getString("name");
            partnerPeerName = userOffered;
            tvApiMsg.setText(partnerPeerName + " has request you for chat");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonObject = response.getJSONObject(OFFER);
            SessionDescription offerSdp = parseSdp(jsonObject);
            onGettingOffer(partnerPeerName, offerSdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private SessionDescription parseSdp(JSONObject jsonObject) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(jsonObject.toString());
        return new Gson().fromJson(jsonElement, SessionDescription.class);
    }


    private IceCandidate parseIceCandidate(JSONObject jsonObject) {
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(jsonObject.toString());
        return new Gson().fromJson(jsonElement, IceCandidate.class);
    }


    private void parseCandidate(JSONObject response) {


        tvApiMsg.setText(partnerPeerName + " has sent you candidate");


        try {
            JSONObject jsonObject = response.getJSONObject(CANDIDATE);
            IceCandidate iceCandidate = parseIceCandidate(jsonObject);
            // onGettingIceCandidate(iceCandidate);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }


    private void parseAnswer(JSONObject response) {

        try {
            String answer = response.getString(ANSWER);
            if (answer.equals("true")) {
                tvApiMsg.setText(partnerPeerName + " accepted your request for chat");
            } else {
                tvApiMsg.setText(partnerPeerName + " declined your request for chat");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            JSONObject jsonObject = response.getJSONObject(ANSWER);
            SessionDescription answerSdp = parseSdp(jsonObject);
            onGettingAnswer(answerSdp);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void parseUpdateUsers(JSONObject response) {


        try {
            String userOffered = response.getString("userName");
            partnerPeerName = userOffered;
            tvApiMsg.setText(partnerPeerName + " has request you for chat");
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    DataChannel localDataChannel;
    DataChannel remoteDataChannel;

    AudioTrack localAudioTrack;
    AudioTrack remoteAudioTrack;


    public void createNewPeerConnectionFactory() {


        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true);
        PeerConnectionFactory pcFactory = new PeerConnectionFactory();


        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
        mediaStream.addTrack(localAudioTrack);

        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));

        List<PeerConnection.IceServer> localIceServers = new ArrayList<>();
        List<PeerConnection.IceServer> remoteIceServers = new ArrayList<>();


//        localPeerConnection=pcFactory.createPeerConnection(localIceServers,audioConstraints,this);
//        remotePeerConnection=pcFactory.createPeerConnection(remoteIceServers,audioConstraints,this);
//

    }

    public void createPeerConnectionFactory() {


        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true);
        peerConnectionFactory = new PeerConnectionFactory();


        //  PeerConnectionFactory.InitializationOptions.Builder initializationOptions = PeerConnectionFactory.InitializationOptions.builder(getApplicationContext());

        // PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(getApplicationContext()).setEnableVideoHwAcceleration(true).createInitializationOptions());

        //  peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);


        AudioAttributes mPlaybackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build();





        Handler handler=new Handler();

        AudioManager.OnAudioFocusChangeListener listener
                =new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int i) {

                switch (i){
                    case AudioManager.AUDIOFOCUS_GAIN:
                        Log.d(TAG, "AUDIOFOCUS_GAIN: ");
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        Log.d(TAG, "AUDIOFOCUS_LOSS: ");
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT: ");
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        Log.d(TAG, "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK: ");
                }
            }
        };

        AudioFocusRequest mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mPlaybackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(listener, handler)
                .build();

        audioManager.requestAudioFocus(mFocusRequest);
        int res = audioManager.requestAudioFocus(mFocusRequest);
        final Object mFocusLock = new Object();
        boolean mPlaybackDelayed = false;
        synchronized (mFocusLock) {
            if (res == AudioManager.AUDIOFOCUS_REQUEST_FAILED) {
                Log.d(TAG, "AUDIOFOCUS_REQUEST_FAILED: ");
                mPlaybackDelayed = false;
            } else if (res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "AUDIOFOCUS_REQUEST_GRANTED: ");
                mPlaybackDelayed = false;
            } else if (res == AudioManager.AUDIOFOCUS_REQUEST_DELAYED) {
                mPlaybackDelayed = true;
                Log.d(TAG, "AUDIOFOCUS_REQUEST_DELAYED: ");
            }
        }


        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_UNMUTE, 0);
        Log.d(TAG, "createPeerConnectionFactory: isStreamMute " +
                audioManager.isStreamMute(AudioManager.STREAM_VOICE_CALL));


        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = new PeerConnection.IceServer("stun:stun.1.google.com:19302");
        PeerConnection.IceServer turn = new PeerConnection.IceServer("turn:numb.viagenie.ca", "muazkh", "webrtc@live.com");
       // iceServers.add("");
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
        MediaConstraints sdpConstraints = new MediaConstraints();
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("offerToReceiveAudio", "true"));
        sdpConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"));
        //peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, sdpConstraints, this);

        localPeerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, sdpConstraints, localConnectionObserver);
        remotePeerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, sdpConstraints, remoteConnectionObserver);


        //localAudioSource = peerConnectionFactory.createAudioSource(sdpConstraints);
        //remoteAudioSource = peerConnectionFactory.createAudioSource(sdpConstraints);


        // AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("local_voice_call", localAudioSource);
        //AudioTrack remoteAudioTrack = peerConnectionFactory.createAudioTrack("remote_voice_call", remoteAudioSource);

        //  AudioSource audioSource = peerConnectionFactory.createAudioSource(sdpConstraints);
        //AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);
        //localAudioTrack.setVolume(1f);

        //AudioSource remoteaudioSource = peerConnectionFactory.createAudioSource(sdpConstraints);


        AudioSource localAudioSource = peerConnectionFactory.createAudioSource(sdpConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID_LOCAL, localAudioSource);

        AudioSource remoteAudioSource = peerConnectionFactory.createAudioSource(sdpConstraints);
       // remoteAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID_REMOTE, remoteAudioSource);


        //AudioTrack remotelAudioTrack = peerConnectionFactory.createAudioTrack("ARDAMSa0", audioSource);


        MediaStream localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
        MediaStream remoteMediaStream = peerConnectionFactory.createLocalMediaStream(REMOTE_MEDIA_STREAM_ID);

        localMediaStream.addTrack(localAudioTrack);
       // remoteMediaStream.addTrack(remoteAudioTrack);

        localPeerConnection.addStream(localMediaStream);
       // remotePeerConnection.addStream(remoteMediaStream);

        //localPeerConnection.addTrack(localAudioTrack);
        //remotePeerConnection.addTrack(remotelAudioTrack);





       /* MediaRecorder mediaRecorder=new MediaRecorder();
        mediaRecorder.release();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);


        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }



        mediaRecorder.start();*/




        /*MediaStream stream = peerConnectionFactory.createLocalMediaStream("102");
        stream.addTrack(localAudioTrack);
        localPeerConnection.addStream(stream);*/

        /*DataChannel.Init init = new DataChannel.Init();
        init.ordered = false;
        localDataChannel = localPeerConnection.createDataChannel("pratik", init);


        localDataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                Log.d(TAG, "localDataChannel onBufferedAmountChange: ");
            }

            @Override
            public void onStateChange() {
                Log.d(TAG, "localDataChannel onStateChange: ");
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                // onChannelMsg(buffer);

            }
        });*/


    }


    private void createOffer() {


        localPeerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                localPeerConnection.setLocalDescription(this, sessionDescription);
                Log.d(TAG, "createOffer onCreateSuccess: ");
            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "createOffer onSetSuccess: ");
                RequestData requestData = new RequestData();
                requestData.setType(OFFER);
                requestData.setName(partnerPeerName);
                requestData.setOffer(localPeerConnection.getLocalDescription());
                createRequest(requestData);
            }

            @Override
            public void onCreateFailure(String s) {
                Log.d(TAG, "createOffer onCreateFailure: ");
            }

            @Override
            public void onSetFailure(String s) {
                Log.d(TAG, "createOffer onSetFailure: ");
            }
        }, new MediaConstraints());
    }


    private void createAnswer() {


        final SdpObserver setLocalDesObserver = new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "setLocalDesObserver onSetSuccess: ");
                RequestData requestData = new RequestData();
                requestData.setName(partnerPeerName);
                requestData.setType(ANSWER);
                requestData.setAnswer(remotePeerConnection.getLocalDescription());
                createRequest(requestData);
            }

            @Override
            public void onCreateFailure(String s) {
                Log.d(TAG, "setLocalDesObserver onCreateFailure: ");
            }

            @Override
            public void onSetFailure(String s) {
                Log.d(TAG, "setLocalDesObserver onSetFailure: ");
            }
        };

        SdpObserver createDescObserver = new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                remotePeerConnection.setLocalDescription(setLocalDesObserver, sessionDescription);
                Log.d(TAG, " createAnswer onCreateSuccess: ");
            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "createAnswer onSetSuccess: ");
            }

            @Override
            public void onCreateFailure(String s) {
                Log.d(TAG, "createAnswer onCreateFailure: ");
            }

            @Override
            public void onSetFailure(String s) {
                Log.d(TAG, "createAnswer onSetFailure: ");
            }
        };


        remotePeerConnection.createAnswer(createDescObserver, new MediaConstraints());


    }


    private void createNotification(String caller) {

        Intent intent = new Intent(this, CallActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "voip")
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle(caller)
                .setContentText("Is Calling You")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);


        Notification notification = builder.build();
        notificationManager.notify(NotificationID.getID(), notification);


    }

    private void onGettingOffer(String name, SessionDescription offerSdp) {
        partnerPeerName = name;

        ConnectionActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createNotification(partnerPeerName);
            }
        });


        remotePeerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                // createAnswer();

            }

            @Override
            public void onCreateFailure(String s) {

            }

            @Override
            public void onSetFailure(String s) {

            }
        }, offerSdp);


    }

    private void onGettingAnswer(SessionDescription sessionDescription) {

        localPeerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                Log.d(TAG, "onGettingAnswer onCreateSuccess: ");
            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "onGettingAnswer onSetSuccess: Connection Established");

            }

            @Override
            public void onCreateFailure(String s) {
                Log.d(TAG, "onGettingAnswer onCreateFailure: ");
            }

            @Override
            public void onSetFailure(String s) {
                Log.d(TAG, "onGettingAnswer onSetFailure: ");
            }
        }, sessionDescription);
    }

    private void onGettingIceCandidate(PeerConnection peerConnection, IceCandidate iceCandidate) {
        if (peerConnection == localPeerConnection) {
            remotePeerConnection.addIceCandidate(iceCandidate);
        } else {
            localPeerConnection.addIceCandidate(iceCandidate);
        }

    }


    private PeerConnection.Observer localConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "LocalPeer onSignalingChange: ");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "LocalPeer onIceConnectionChange: " + iceConnectionState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "LocalPeer onIceConnectionReceivingChange: ");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "LocalPeer onIceGatheringChange: " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "LocalPeer onIceCandidate: ");
            onGettingIceCandidate(localPeerConnection, iceCandidate);
        }

//        @Override
//        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
//            Log.d(TAG, "LocalPeer onIceCandidatesRemoved: ");
//        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "LocalPeer onAddStream: ");
            final List<AudioTrack> audioTracks = mediaStream.audioTracks;

//
            if (audioTracks.size() > 0) {
                AudioTrack audioTrack = audioTracks.get(0);
                audioTrack.setEnabled(true);
            }

            ConnectionActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (audioTracks.size() > 0) {
                        AudioTrack audioTrack = audioTracks.get(0);
                        audioTrack.setEnabled(true);
                    }
                }
            });


        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "LocalPeer onRemoveStream: ");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "LocalPeer onDataChannel: ");
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "LocalPeer onRenegotiationNeeded: ");
        }

//        @Override
//        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
//            Log.d(TAG, "LocalPeer onAddTrack: ");
//        }
    };


    private PeerConnection.Observer remoteConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "RemotePeer onSignalingChange: ");
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "RemotePeer onIceConnectionChange: " + iceConnectionState);
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "RemotePeer onIceConnectionReceivingChange: ");
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "RemotePeer onIceGatheringChange: " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "RemotePeer onIceCandidate: ");
            onGettingIceCandidate(remotePeerConnection, iceCandidate);
        }


//        @Override
//        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
//            Log.d(TAG, "RemotePeer onIceCandidatesRemoved: ");
//        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "RemotePeer onAddStream: ");
            final List<AudioTrack> audioTracks = mediaStream.audioTracks;

//
            if (audioTracks.size() > 0) {
                AudioTrack audioTrack = audioTracks.get(0);
                audioTrack.setEnabled(true);
            }

            ConnectionActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (audioTracks.size() > 0) {
                        AudioTrack audioTrack = audioTracks.get(0);
                        audioTrack.setEnabled(true);
                    }
                }
            });

        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "RemotePeer onRemoveStream: ");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "RemotePeer onDataChannel: ");
        }


        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "RemotePeer onRenegotiationNeeded: ");
        }

//        @Override
//        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
//            Log.d(TAG, "RemotePeer onAddTrack: ");
//
//        }
    };


    private void startMicroPhone() {
        MediaRecorder recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}

