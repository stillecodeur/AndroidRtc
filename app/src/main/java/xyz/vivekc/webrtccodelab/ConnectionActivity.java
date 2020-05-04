package xyz.vivekc.webrtccodelab;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
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
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import tech.gusavila92.websocketclient.WebSocketClient;

public class ConnectionActivity extends AppCompatActivity implements PeerConnection.Observer {

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
    private PeerConnection peerConnection;

    private String myPeerName;
    private String partnerPeerName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);


        // client=new OkHttpClient.Builder().addInterceptor(new In);

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
                createAnswer();
//                RequestData requestData = new RequestData();
//                requestData.setType(ANSWER);
//                requestData.setName(userOfferedForChat);
//                requestData.setAnswer("true");
//                createRequest(requestData);

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
                /*RequestData requestData = new RequestData();
                requestData.setType(CANDIDATE);
                requestData.setName(userAskedForChat);
                requestData.setCandidate("stun:stun.l.google.com:19302");
                createRequest(requestData);

                Intent intent = new Intent(ConnectionActivity.this, MainActivity.class);
                startActivity(intent);*/
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


        // progressDialog.show();

        webSocketClient.send(json);






        /*JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, jsonObject, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {

                        progressDialog.dismiss();
                        Log.d("Response", "onResponse: ");
                        ConnectionActivity.this.onResponse(response);

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle error
                        progressDialog.dismiss();
                        tvApiMsg.setText(error.getMessage());
                    }
                });


        progressDialog.show();

        queue.add(jsonObjectRequest);*/
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
        Intent intent = new Intent(ConnectionActivity.this, MainActivity.class);
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
                onGettingIceCandidate(iceCandidate);
            } catch (JSONException e) {
                e.printStackTrace();
            }



            /*RequestData requestData = new RequestData();
            requestData.setName(userAskedForChat);
            requestData.setType(CONNECTION_ESTABLISHED);
            createRequest(requestData);*/


            /*Intent intent = new Intent(ConnectionActivity.this, MainActivity.class);
            intent.putExtra(CANDIDATE, candidate);
            startActivity(intent);*/


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


    DataChannel dataChannel;

    public void createPeerConnectionFactory() {

        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(getApplicationContext()).setEnableVideoHwAcceleration(true).createInitializationOptions());


        peerConnectionFactory = PeerConnectionFactory.builder().createPeerConnectionFactory();
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.IceServer iceServer = new PeerConnection.IceServer("stun:stun.1.google.com:19302");
        iceServers.add(iceServer);
        PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
        peerConnection = peerConnectionFactory.createPeerConnection(rtcConfiguration, new MediaConstraints(), this);

        DataChannel.Init init=new DataChannel.Init();
        init.ordered=false;
        dataChannel=peerConnection.createDataChannel("pratik",init);


        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                Log.d(TAG, "dataChannel onBufferedAmountChange: ");
            }

            @Override
            public void onStateChange() {
                Log.d(TAG, "dataChannel onStateChange: ");
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                onChannelMsg(buffer);
                /*ByteBuffer byteBuffer = buffer.data;
                byteBuffer.flip();
                String msg = null;
                try {
                    msg = new String(byteBuffer.array(),"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }*/



            }
        });



    }


    private void createOffer() {


        peerConnection.createOffer(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(this, sessionDescription);
                Log.d(TAG, "createOffer onCreateSuccess: ");
            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "createOffer onSetSuccess: ");
                RequestData requestData = new RequestData();
                requestData.setType(OFFER);
                requestData.setName(partnerPeerName);
                requestData.setOffer(peerConnection.getLocalDescription());
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


    private void createAnswer(){


        final SdpObserver setLocalDesObserver=new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                Log.d(TAG, "setLocalDesObserver onSetSuccess: ");
                RequestData requestData=new RequestData();
                requestData.setName(partnerPeerName);
                requestData.setType(ANSWER);
                requestData.setAnswer(peerConnection.getLocalDescription());
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

        SdpObserver createDescObserver=new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {
                peerConnection.setLocalDescription(setLocalDesObserver, sessionDescription);
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


        peerConnection.createAnswer(createDescObserver,new MediaConstraints());


    }

    private void onGettingOffer(String name, SessionDescription offerSdp) {
        partnerPeerName = name;


        peerConnection.setRemoteDescription(new SdpObserver() {
            @Override
            public void onCreateSuccess(SessionDescription sessionDescription) {

            }

            @Override
            public void onSetSuccess() {
                createAnswer();
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

        peerConnection.setRemoteDescription(new SdpObserver() {
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

    private void onGettingIceCandidate(IceCandidate iceCandidate) {
        peerConnection.addIceCandidate(iceCandidate);
        Log.d(TAG, "onGettingIceCandidate: "+iceCandidate);

    }

    private void sendMessage(DataChannel dataChannel,String msg) {
        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = ByteBuffer.wrap(msg.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, false);
        dataChannel.send(buffer);
    }



    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.d(TAG, "onSignalingChange: "+signalingState);

    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
        Log.d(TAG, "onIceConnectionChange: "+iceConnectionState);
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

        RequestData requestData = new RequestData();
        requestData.setType(CANDIDATE);
        requestData.setName(partnerPeerName);
        requestData.setCandidate(iceCandidate);
        createRequest(requestData);


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
    public void onDataChannel(final DataChannel dataChannel) {
        Log.d(TAG, "onDataChannel: "+dataChannel.label());
        final DataChannel receiveDataChannel=dataChannel;
        receiveDataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {
                Log.d(TAG, "receiveDataChannel onBufferedAmountChange: ");
            }

            @Override
            public void onStateChange() {
                Log.d(TAG, "receiveDataChannel onStateChange: ");

                if(dataChannel.state()== DataChannel.State.OPEN){
                    Log.d(TAG, "receiveDataChannel StateOpen: ");
                   // sendChannelMsg(receiveDataChannel,"Hello");
                }
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                Log.d(TAG, " receiveDataChannel onMessage: ");
            }
        });

    }

    @Override
    public void onRenegotiationNeeded() {
        Log.d(TAG, "onRenegotiationNeeded: ");
    }

    @Override
    public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
        Log.d(TAG, "onAddTrack: ");
    }


    private void onChannelMsg(DataChannel.Buffer buffer){
        byte[] bytes;
        if (buffer.data.hasArray()) {
            bytes = buffer.data.array();

            String str="";
            try {
                 str = new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            Log.d(TAG, "onChannelMsg: "+str);

        } else {
            bytes = new byte[buffer.data.remaining()];
            buffer.data.get(bytes);
        }

    }

    private void sendChannelMsg(DataChannel channel,String msg){
        byte[] rawMessage = msg.getBytes(Charset.forName("UTF-8"));
        ByteBuffer directData = ByteBuffer.allocateDirect(rawMessage.length);
        directData.put(rawMessage);
        directData.flip();
        DataChannel.Buffer data = new DataChannel.Buffer(directData, false);
        channel.send(data);
    }
}

