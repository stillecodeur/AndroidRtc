package xyz.vivekc.webrtccodelab;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PeerConnection.Observer, DataChannel.Observer {
    private static final String TAG = "MainActivity";


    private final String CANDIDATE="candidate";
    private LinearLayout rootLayout;
    private EditText etChatMsg;
    private Button btnSend;
    private PeerConnection peerConnection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);

        rootLayout = findViewById(R.id.main_layout);
        etChatMsg = findViewById(R.id.et_chat_msg);
        btnSend = findViewById(R.id.btn_send);


        if (getIntent().getExtras() != null) {
            String uri = getIntent().getExtras().getString(CANDIDATE);

            List<PeerConnection.IceServer> list = new ArrayList<>();
            PeerConnection.IceServer iceServer = new PeerConnection.IceServer(uri,"","");
            list.add(iceServer);
            createPeerConnection(list);
        }

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = etChatMsg.getText().toString();

                if (!TextUtils.isEmpty(msg)) {
                    sendMessage(msg);
                }
            }
        });
        //Initialize PeerConnectionFactory globals.


        //Now create a VideoCapturer instance. Callback methods are there if you want to do something! Duh!
       /* VideoCapturer videoCapturerAndroid = createVideoCapturer();
        //Create MediaConstraints - Will be useful for specifying video and audio constraints. More on this later!
        MediaConstraints constraints = new MediaConstraints();

        //Create a VideoSource instance
        VideoSource videoSource = peerConnectionFactory.createVideoSource(videoCapturerAndroid);
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack("100", videoSource);

        //create an AudioSource instance
        AudioSource audioSource = peerConnectionFactory.createAudioSource(constraints);
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack("101", audioSource);

        //we will start capturing the video from the camera
        //width,height and fps
        videoCapturerAndroid.startCapture(1000, 1000, 30);

        //create surface renderer, init it and add the renderer to the track
        SurfaceViewRenderer videoView = (SurfaceViewRenderer) findViewById(R.id.surface_rendeer);
        videoView.setMirror(true);

        EglBase rootEglBase = EglBase.create();
        videoView.init(rootEglBase.getEglBaseContext(), null);

        localVideoTrack.addRenderer(new VideoRenderer(videoView));*/


    }


    private DataChannel channel;



    private void createPeerConnection(List<PeerConnection.IceServer> iceServers) {
        //Params are context, initAudio,initVideo and videoCodecHwAcceleration
        //PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true);
        //PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).setEnableVideoHwAcceleration(true).createInitializationOptions());

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).setEnableVideoHwAcceleration(true).createInitializationOptions());


        //Create a new PeerConnectionFactory instance.
        //PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        PeerConnectionFactory peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();


        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, this);


        DataChannel.Init init = new DataChannel.Init();
        init.id = 123;
        channel = peerConnection.createDataChannel("Chat", init);


        channel.registerObserver(this);

    }


    private void sendMessage(String msg) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(msg.getBytes());
        DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, true);
        channel.send(buffer);
        sendMsgLayout(msg);


    }


    private void sendMsgLayout(String msg) {
        View view = LayoutInflater.from(this).inflate(R.layout.chat_sent, null);
        TextView textView = findViewById(R.id.msg);
        textView.setText(msg);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(layoutParams);
        rootLayout.addView(view);

    }

    private void receiveMsgLayout(String msg) {
        View view = LayoutInflater.from(this).inflate(R.layout.chat_receive, null);
        TextView textView = findViewById(R.id.msg);
        textView.setText(msg);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(layoutParams);
        rootLayout.addView(view);
    }

    private VideoCapturer createVideoCapturer() {
        VideoCapturer videoCapturer;
        Logging.d(TAG, "Creating capturer using camera1 API.");
        videoCapturer = createCameraCapturer(new Camera1Enumerator(false));

        return videoCapturer;
    }

    private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
        final String[] deviceNames = enumerator.getDeviceNames();

        // First, try to find front facing camera
        Logging.d(TAG, "Looking for front facing cameras.");
        for (String deviceName : deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating front facing camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        // Front facing camera not found, try something else
        Logging.d(TAG, "Looking for other cameras.");
        for (String deviceName : deviceNames) {
            if (!enumerator.isFrontFacing(deviceName)) {
                Logging.d(TAG, "Creating other camera capturer.");
                VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

                if (videoCapturer != null) {
                    return videoCapturer;
                }
            }
        }

        return null;
    }

    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, "onSignalingChange: ");
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, "onIceConnectionChange: ");
    }

    @Override
    public void onIceConnectionReceivingChange(boolean b) {
        Log.d(TAG, "onIceConnectionReceivingChange: ");
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
        Log.d(TAG, "onIceGatheringChange: ");
    }

    @Override
    public void onIceCandidate(IceCandidate iceCandidate) {
        Log.d(TAG, "onIceCandidate: ");
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
        Log.d(TAG, "onIceCandidatesRemoved: ");
    }

    @Override
    public void onAddStream(MediaStream mediaStream) {
        Log.d(TAG, "onAddStream: ");
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        Log.d(TAG, "onRemoveStream: ");
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel: ");
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded: ");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack: ");
    }

    @Override
    public void onBufferedAmountChange(long l) {
        Log.d(TAG, "onBufferedAmountChange: ");
    }

    @Override
    public void onStateChange() {
        Log.d(TAG, "onStateChange: ");
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        ByteBuffer byteBuffer = buffer.data;
        String msg = new String(byteBuffer.array(), StandardCharsets.UTF_8);
        receiveMsgLayout(msg);
    }
}
