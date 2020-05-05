package xyz.vivekc.webrtccodelab;

import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;

public class PeerConInstance {


    private static PeerConInstance INSTANCE;

    private PeerConnection peerConnection;
    private PeerConnectionFactory peerConnectionFactory;


    public static PeerConInstance getInstance(PeerConnection peerConnection) {

        if (INSTANCE == null) {
            new PeerConInstance(peerConnection);
        }
        return INSTANCE;
    }


    private PeerConInstance(PeerConnection peerConnection) {
        this.peerConnection = peerConnection;
    }

    public PeerConnection getPeerConnection(){
        return peerConnection;
    }

    public PeerConnectionFactory getPeerConnectionFactory() {
        return peerConnectionFactory;
    }
}
