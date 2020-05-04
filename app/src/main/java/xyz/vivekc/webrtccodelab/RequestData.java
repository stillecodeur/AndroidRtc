package xyz.vivekc.webrtccodelab;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class RequestData {
    private String type="";
    private String name="";
    private SessionDescription offer;
    private SessionDescription answer;
    private IceCandidate candidate;

    public void setCandidate(IceCandidate candidate) {
        this.candidate = candidate;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SessionDescription getOffer() {
        return offer;
    }

    public void setOffer(SessionDescription offer) {
        this.offer = offer;
    }

    public SessionDescription getAnswer() {
        return answer;
    }

    public void setAnswer(SessionDescription answer) {
        this.answer = answer;
    }


}
