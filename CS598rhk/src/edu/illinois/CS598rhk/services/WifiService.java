package edu.illinois.CS598rhk.services;

import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import edu.illinois.CS598rhk.schedules.DiscoverSchedule;
import edu.illinois.CS598rhk.models.FriendData;
import edu.illinois.CS598rhk.schedules.SearchLightSchedule;
import edu.illinois.CS598rhk.interfaces.IWifiService;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

public class WifiService extends Service implements IWifiService {
    public static final String INTENT_TO_RESUME_WIFI ="intent to update the wifi state";
    public static final String INTENT_TO_PAUSE_WIFI = "(g\")-O";
    public static final String INTENT_TO_ADD_WIFI_NEIGHBOR = "ASDFASDFSDFA";
    public static final String WIFI_NEIGHBOR_NAME = "phone name";
    public static final String WIFI_IP_ADDRESS = "ip address";
    
    private static final int DISCOVERY_PERIOD = 5000;
    private static final String MSG_TAG = "WifiService";

    private DiscoverSchedule discoveryScheduler;
    private boolean wifiEnabled=false;
//    private CoreTask coretask;

    private List<FriendData> friendList;
    private List<FriendData> activeFriends;
    private FriendData myData;
    private WifiManager wifiManager;
    private WifiMessageReceiver messageReceiver; 
    private WifiController wifiController;
    
    private String myIPAddress;
    private String myBroadcast;
    private int timeSlice;
    
    InetAddress dest;
    
    private final IBinder mBinder = new WifiBinder();
    
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }
    
    public class WifiBinder extends Binder {
        public IWifiService getService() {
            return WifiService.this;
        }
    }
    @Override
    public void onCreate() {
        wifiManager = (WifiManager) this.getSystemService(WIFI_SERVICE);
        wifiController = new WifiController();
        myData = new FriendData();
        friendList = new ArrayList<FriendData>();
        activeFriends = new ArrayList<FriendData>();
        myIPAddress = "192.168.1.2";
        myBroadcast = "192.168.1.255";
        discoveryScheduler = new SearchLightSchedule(7);
        
        try {
            dest = InetAddress.getByName(myBroadcast);
        } catch (UnknownHostException e) {
            Log.d(MSG_TAG,"Broadcast address "+myBroadcast+" as unknown.");
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	IntentFilter messageFilter = new IntentFilter();
    	messageFilter.addAction(INTENT_TO_PAUSE_WIFI);
    	messageFilter.addAction(INTENT_TO_RESUME_WIFI);
    	messageReceiver = new WifiMessageReceiver();
    	registerReceiver(messageReceiver, messageFilter);
    	
        if(wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(false);
        enableWifi();
        
        wifiController.start();
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        if(wifiEnabled)
            disableWifi();
        super.onDestroy();
    }
    
    public void enableWifi() {
        /*if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
                + "/bin/netcontrol start_wifi " + this.coretask.DATA_FILE_PATH)) {
            Log.d(MSG_TAG, "netcontrol start_wifi failed");
            // fall down below anyway
        }
        Log.d(MSG_TAG, "Wifi Enabled");*/
        if(!wifiManager.isWifiEnabled())
        {
            wifiManager.setWifiEnabled(true);
            while(!wifiManager.isWifiEnabled()) {
                try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
            }
        }
        
        wifiEnabled = true;
    }

    public void disableWifi() {
//        if (this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
//                + "/bin/netcontrol stop_wifi " + this.coretask.DATA_FILE_PATH)) {
//            Log.d(MSG_TAG, "netcontrol stop_wifi failed");
//            // fall down below anyway
//        }
        Log.d(MSG_TAG, "Wifi disabled");
        wifiEnabled = false;
    }
    
    private class WifiMessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction() == INTENT_TO_RESUME_WIFI) {
				wifiController.resumeWifiService();
			} else if(intent.getAction() == INTENT_TO_PAUSE_WIFI){
				wifiController.pauseWifiService();
			}
			
		}
    }
    
    
    private class WifiController extends Thread {
        
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
        
        public void resumeWifiService() {
			// TODO Auto-generated method stub
		}

		public void pauseWifiService() {
			// TODO Auto-generated method stub
			
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
        
        private void sendWifiBroadcast() {
            
        }
        
        private void listenForWifiBroadcast() {
            
        }
        
        @Override
        public void run() {
            byte[] buf = new byte[1024];
            DatagramPacket pkt = null;
            DatagramSocket sock = null;
            enableWifi();

            int[] mySchedule = discoveryScheduler.generateScedule();
            timeSlice = 0;
            Looper.prepare();
            while (true) {
                long startTime = System.currentTimeMillis();
                if (!wifiEnabled) {
                    if (mySchedule[(timeSlice + 1) % mySchedule.length] != DiscoverSchedule.DO_NOTHING) {
                        enableWifi();
                        wifiEnabled = true;
                    }
                } else {
                    if (mySchedule[timeSlice] != DiscoverSchedule.DO_NOTHING) {
                        try {
                            String msg = myData.makeHeartbeat("");
                            buf = msg.getBytes();
                            pkt = new DatagramPacket(buf, buf.length, dest, 8888);

                            if (mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT_N_LISTEN
                                    || mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT) {
                                try {
                                    sock = new DatagramSocket();
                                    sock.setBroadcast(true);
                                    sock.send(pkt);
                                    Log.d(MSG_TAG, "Packet was sent.");

                                    sendToLogger("Sent Wifi heartbeat");
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
                                            
                                            //inform the scheduling service
                                            Intent foundNewNeighbor = new Intent(INTENT_TO_ADD_WIFI_NEIGHBOR);
                                            foundNewNeighbor.putExtra(WIFI_NEIGHBOR_NAME, f.name);
                                            foundNewNeighbor.putExtra(WIFI_IP_ADDRESS,f.IPaddress);
                                            sendBroadcast(foundNewNeighbor);
                                            
                                            sendToLogger("Found neighbor "+f.name);
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
                                    sendToLogger("Sent Wifi heartbeat");
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
                }

                try {
                    long sleepyTime = DISCOVERY_PERIOD - (System.currentTimeMillis() - startTime);
                    if (sleepyTime < 0)
                        sleepyTime = 0;
                    Thread.sleep(sleepyTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

            
                // checkStaleFriends(); ???
                timeSlice = (timeSlice + 1) % mySchedule.length;
            
            }
        }
    }
	@Override
	public void pauseWifiService() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resumeWifiService(long delay) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long scheduleTimeRemaining() {
		return discoveryScheduler.scheduleLength()-timeSlice;
	}

	@Override
	public void broadcast(List<String> addrs, String message) {
		// TODO Auto-generated method stub
		
	}
	
	public void sendToLogger(String message) {
		Intent intentToLog = new Intent(PowerManagement.INPUT_METHOD_SERVICE);
		intentToLog.putExtra(PowerManagement.LOG_MESSAGE, message);
		sendBroadcast(intentToLog);
	}
}

