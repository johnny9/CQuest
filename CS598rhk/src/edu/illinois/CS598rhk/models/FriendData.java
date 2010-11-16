package edu.illinois.CS598rhk.models;

import java.util.StringTokenizer;

public class FriendData {
    public long lastHeartbeat = 0;
    public String name;
    public String IPaddress;
    public String statusMsg;

    public FriendData() {
    }

    FriendData(String _name, String _IPaddress, String _statusMsg) {
        name = _name;
        IPaddress = _IPaddress;
        statusMsg = _statusMsg;
    }

    boolean equals(FriendData other) {
        return name.compareTo(other.name) == 0 && IPaddress.compareTo(other.IPaddress) == 0;
    }

    public String makeHeartbeat(String gpsStr) {
        String hbString = new String();
        hbString = "Heartbeat:~" + name + "~" + statusMsg + "~" + gpsStr;
        return hbString;
    }

    public boolean isHeartbeat(String msg) {
        if (msg.startsWith("Heartbeat:"))
            return true;
        else
            return false;
    }

    public String parseHeartbeat(String hbString) {
        StringTokenizer st = new StringTokenizer(hbString, "~");
        if (st.hasMoreTokens()) {
            // Skip message identifier
            st.nextToken();
        }
        if (st.hasMoreTokens()) {
            name = st.nextToken();
        }
        if (st.hasMoreTokens()) {
            statusMsg = st.nextToken();
        }
        String gps = "";
        if (st.hasMoreTokens()) {
            gps = st.nextToken();
        }
        return gps;
    }

    String makeMessage(String message) {
        String msgString = new String();
        msgString = "Message:~" + name + "~";
        msgString += message + "~";
        return msgString;
    }

    public boolean isMessage(String msg) {
        if (msg.startsWith("Message:"))
            return true;
        else
            return false;
    }

    public String parseMessage(String msgString) {
        StringTokenizer st = new StringTokenizer(msgString, "~");
        if (st.hasMoreTokens()) {
            // Skip message identifier
            st.nextToken();
        }
        if (st.hasMoreTokens()) {
            name = st.nextToken();
        }
        String msgText = "";
        if (st.hasMoreTokens()) {
            msgText = st.nextToken();
        }
        return msgText;
    }
}