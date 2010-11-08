package edu.illinois.CS598rhk;

import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.util.Log;

class WifiController implements Runnable {
    private static final int DISCOVERY_PERIOD = 5000;
    private static final String MSG_TAG = "AdhocClientWifiThread";

    private DiscoverSchedule discoveryScheduler;
    private boolean wifiEnabled;
    private CoreTask coretask = null;

    private List<FriendData> friendList;
    private List<FriendData> activeFriends;
    private FriendData myData;
    private WifiManager wifiManager;

    //Singleton pattern
    private volatile static WifiController INSTANCE = null;
    
    private WifiController(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }
    
    public static synchronized WifiController getInstance(WifiManager wifiManager) {
        if(INSTANCE == null)
        {
            INSTANCE = new WifiController(wifiManager);
        }
        return INSTANCE;
    }


    public void enableWifi() {
        if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
                + "/bin/netcontrol start_wifi " + this.coretask.DATA_FILE_PATH)) {
            Log.d(MSG_TAG, "netcontrol start_wifi failed");
            // fall down below anyway
        }
        Log.d(MSG_TAG, "Wifi Enabled");
    }

    public void disableWifi() {
        if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
                + "/bin/netcontrol stop_wifi " + this.coretask.DATA_FILE_PATH)) {
            Log.d(MSG_TAG, "netcontrol stop_wifi failed");
            // fall down below anyway
        }
        Log.d(MSG_TAG, "Wifi disabled");
    }

    public void setWifiIPAddress(String address) {

    }

    public String getMyGPSString() {
        String myLoc = Double.toString(10) + ", " + Double.toString(10);

        /*
         * try { Location location =
         * locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
         * Double lat = location.getLatitude(); Double lng =
         * location.getLongitude(); myLoc = Double.toString(lat) + ", " +
         * Double.toString(lng); } catch (Exception e) { // GPS not available...
         * use default string }
         */
        return myLoc;
    }

    // @Override
    public void run() {
        byte[] buf = new byte[1024];
        boolean wifiEnabled;
        DatagramPacket pkt = null;
        DatagramSocket sock = null;
        InetAddress dest = null;
        Date t = new Date();

        enableWifi();
        wifiEnabled = true;

        setWifiIPAddress("192.168.2.1");
        String myBcastAddress = "192.168.2.255";

        try {
            dest = InetAddress.getByName(myBcastAddress);
        } catch (UnknownHostException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        int[] mySchedule = discoveryScheduler.generateScedule();
        int timeSlice = 0;
        Looper.prepare();
        while (!Thread.currentThread().isInterrupted()) {
            long startTime = System.currentTimeMillis();
            if (!wifiEnabled) {
                if (mySchedule[(timeSlice + 1) % mySchedule.length] != DiscoverSchedule.DO_NOTHING) {
                    enableWifi();
                    wifiEnabled = true;
                }
            } else {
                if (mySchedule[timeSlice] != DiscoverSchedule.DO_NOTHING) {
                    try {
                        String msg = myData.makeHeartbeat(getMyGPSString());
                        buf = msg.getBytes();
                        pkt = new DatagramPacket(buf, buf.length, dest, 8888);

                        if (mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT_N_LISTEN
                                || mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT) {
                            try {
                                sock = new DatagramSocket();
                                sock.setBroadcast(true);
                                sock.send(pkt);
                                Log.d(MSG_TAG, "Packet was sent.");
                            } catch (Exception e) {
                                // e.printStackTrace();
                                Log.d(MSG_TAG, "attempt to send failed");
                            }
                        }

                        if (mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT_N_LISTEN
                                || mySchedule[timeSlice] == DiscoverSchedule.LISTEN) {
                            try {
                                do {
                                    sock = new DatagramSocket(8888);
                                    sock.setSoTimeout((int) (DISCOVERY_PERIOD - (System
                                            .currentTimeMillis() - startTime)));
                                    sock.receive(pkt);

                                    String rcv = new String(pkt.getData(), 0, pkt.getLength());

                                    Log.d(MSG_TAG, "Packet was received "
                                            + pkt.getAddress().toString() + ": " + rcv);

                                    if (myData.IPaddress == null) {
                                        // WTF
                                    } else if (pkt.getAddress().getHostAddress()
                                            .compareTo(myData.IPaddress) == 0) {
                                        Log.d(MSG_TAG, "Packet was received from myself");
                                    } else {
                                        FriendData f = new FriendData();
                                        f.IPaddress = pkt.getAddress().getHostAddress();
                                        if (f.isHeartbeat(rcv)) {
                                            String gps = f.parseHeartbeat(rcv);
                                            processHeartbeat(f, gps);
                                        }
                                        Log.d(MSG_TAG,
                                                "isHeartbeat returned: "
                                                        + (f.isHeartbeat(rcv) ? "true" : "false"));
                                        if (f.isMessage(rcv)) {
                                            String m = f.parseMessage(rcv);
                                            // processMessage(f, m);
                                        }
                                        Log.d(MSG_TAG, "isMessage returned: "
                                                + (f.isMessage(rcv) ? "true" : "false"));
                                    }
                                } while (System.currentTimeMillis() - startTime < DISCOVERY_PERIOD);
                            } catch (InterruptedIOException e) {
                                // timed out, so no message was received
                            }
                        }

                        if (mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT_N_LISTEN
                                || mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT) {
                            try {
                                // send our last one
                                sock = new DatagramSocket();
                                sock.setBroadcast(true);
                                sock.send(pkt);
                                Log.d(MSG_TAG, "Packet was sent.");
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.d(MSG_TAG, "attempt to send failed");
                            }
                        }
                    } catch (Exception e) {
                        // nothing
                        Log.d(MSG_TAG, "Couldn't setup socket: " + e.getMessage());
                    }
                }

            }

            if (mySchedule[(timeSlice + 1) % mySchedule.length] == DiscoverSchedule.DO_NOTHING
                    && wifiEnabled) {
                disableWifi();
                wifiEnabled = false;
            }

            try {
                long sleepyTime = DISCOVERY_PERIOD - (System.currentTimeMillis() - startTime);
                if (sleepyTime < 0)
                    sleepyTime = 0;
                Thread.sleep(sleepyTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        }
        // checkStaleFriends(); ???
        timeSlice = (timeSlice + 1) % mySchedule.length;
    }

    public synchronized void processHeartbeat(FriendData f, String gps) {

        boolean found = false;
        Collection<FriendData> c = activeFriends;
        Iterator<FriendData> itr = c.iterator();
        int i = 0;
        while (itr.hasNext()) {
            FriendData curFriend = itr.next();
            if (curFriend.equals(f)) {
                Log.d(MSG_TAG, "Updating existing friend: " + f.name + ", " + f.IPaddress);
                if (curFriend.lastHeartbeat == 0) {
                    // staleFriendCount--;
                    // notifyStatusActivity();
                }
                curFriend.lastHeartbeat = System.currentTimeMillis();
                if (curFriend.statusMsg.compareTo(f.statusMsg) != 0) {
                   // addEvent(String.valueOf(f.name + " status is now: " + f.statusMsg));
                }
                curFriend.statusMsg = f.statusMsg;
                //friendList.set(i,
                //      String.valueOf(f.name + ": " + f.statusMsg + "\n" + "GPS: " + gps));
                // notifyFriendListActivity();
                found = true;
                break;
            }
            i++;
        }
        if (!found) {
            Log.d(MSG_TAG, "Added a new friend: " + f.name + ", " + f.IPaddress);
            // Found a new friend
            int new_index = friendList.size();
            f.lastHeartbeat = System.currentTimeMillis();
            // addEvent(String.valueOf(f.name + " status is now: " +
            // f.statusMsg));
            //friendList.add(String.valueOf(f.name + ": " + f.statusMsg + "\n" + "GPS: " + gps));
            //activeFriends.put(new_index, f);
            // notifyFriendListActivity();
            // notifyStatusActivity();
        }

    }
    
    public synchronized void checkStaleFriends() {
        Collection<FriendData> c = activeFriends;
        Iterator<FriendData> itr = c.iterator();
        int i = 0;
        while (itr.hasNext()) {
            FriendData curFriend = itr.next();
            if (curFriend.lastHeartbeat > 0
                    && (System.currentTimeMillis() - curFriend.lastHeartbeat) > 15000) {
                Log.d(MSG_TAG, "Marking stale friend: " + curFriend.name + ", "
                        + curFriend.IPaddress);
                curFriend.lastHeartbeat = 0;
                curFriend.statusMsg = "unknown";
                //staleFriendCount++;
                //friendList.set(i,
                //      String.valueOf(curFriend.name + ": " + curFriend.statusMsg + " [stale]"));
                //notifyFriendListActivity();
                //notifyStatusActivity();
            }
            i++;
        }
    }

}