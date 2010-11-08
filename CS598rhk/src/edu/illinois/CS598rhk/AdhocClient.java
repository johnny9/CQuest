package edu.illinois.CS598rhk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

public class AdhocClient extends Application {
    private static final String MSG_TAG = "AdhocClient";
    public static final long SERVER_TIME = 60000; // 60 seconds
    public static final long CLIENT_TIME = 60000; // 60 seconds

    private Thread netControlThread = null;
    // private Thread clientSendThread = null;
    // private Thread clientReceiveThread = null;
    private Thread clientDiscoveryThread = null;
    private DiscoverSchedule discoveryScheduler = new AlwaysSchedule();

    // WifiManager
    private WifiManager wifiManager;

    private LocationManager locationManager;

    // CoreTask
    public CoreTask coretask = null;

    // Preferences
    public SharedPreferences settings = null;
    public SharedPreferences.Editor preferenceEditor = null;

    private List<String> eventList = new ArrayList<String>();
    private List<String> messageHeader = new ArrayList<String>();
    private List<String> messageList = new ArrayList<String>();
    public int curMessageIndex = -1;
    public int curFriendIndex = -1;

   
    public FriendData myData;

    private HashMap<Integer, FriendData> activeFriends = new HashMap<Integer, FriendData>();

   long lastSwitchTime = 0;
    boolean discovered = false;
    boolean serviceStarted = false;
    int staleFriendCount = 0;
    String myDhcpAddress;
    double dummyLat = 0;
    double dummyLong = 0;

    /** Called when the activity is first created. */
    public void onCreate() {

        coretask = new CoreTask();
        coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
        Log.d(MSG_TAG, "Current directory is "
                + this.getApplicationContext().getFilesDir().getParent());

        // Preferences
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        // preferenceEditor
        this.preferenceEditor = settings.edit();

        // init wifiManager
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

        // init locationManager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check Homedir, or create it
        this.checkDirs();

        // Check for binaries
        boolean filesetOutdated = coretask.filesetOutdated();
        if (binariesExists() == false || filesetOutdated) {
            if (coretask.hasRootPermission()) {
                installBinaries();
                // if (filesetOutdated) {
                // this.openConfigRecoverDialog();
                // }
            } else {
                this.openNotRootDialog();
            }
        }

         this.myData = new FriendData();

        this.eventList = new ArrayList<String>();
       
        //this.netControlThread = new Thread(new NetworkControl());
        //this.netControlThread.start();

        // this.clientSendThread = new Thread(new ClientSender());
        // this.clientSendThread.start();

        // this.clientReceiveThread = new Thread(new ClientReceiver());
        // this.clientReceiveThread.start();


    }

    public void onTerminate() {
        this.netControlThread.interrupt();
        // this.clientSendThread.interrupt();
        // this.clientReceiveThread.interrupt();
        this.clientDiscoveryThread.interrupt();
    }

    // From TetherApplication.java

    // Binary install
    public boolean binariesExists() {
        File file = new File(this.coretask.DATA_FILE_PATH + "/bin/tether");
        if (file.exists()) {
            return true;
        }
        return false;
    }

    public void installBinaries() {
        List<String> filenames = new ArrayList<String>();
        // tether
        this.copyBinary(this.coretask.DATA_FILE_PATH + "/bin/netcontrol", R.raw.netcontrol);
        filenames.add("netcontrol");
        // dnsmasq
        this.copyBinary(this.coretask.DATA_FILE_PATH + "/bin/dnsmasq", R.raw.dnsmasq);
        filenames.add("dnsmasq");
        // iwconfig
        this.copyBinary(this.coretask.DATA_FILE_PATH + "/bin/iwconfig", R.raw.iwconfig);
        filenames.add("iwconfig");
        try {
            this.coretask.chmodBin(filenames);
        } catch (Exception e) {
            displayToastMessage("Unable to change permission on binary files!");
        }
        // dnsmasq.conf
        this.copyBinary(this.coretask.DATA_FILE_PATH + "/conf/dnsmasq.conf", R.raw.dnsmasq_conf);
        // tiwlan.ini
        this.copyBinary(this.coretask.DATA_FILE_PATH + "/conf/tiwlan.ini", R.raw.tiwlan_ini);
        this.displayToastMessage("Binaries and config-files installed!");
    }

    private void copyBinary(String filename, int resource) {
        File outFile = new File(filename);
        InputStream is = this.getResources().openRawResource(resource);
        byte buf[] = new byte[1024];
        int len;
        try {
            OutputStream out = new FileOutputStream(outFile);
            while ((len = is.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            is.close();
        } catch (IOException e) {
            this.displayToastMessage("Couldn't install file - " + filename + "!");
        }
    }

    private void checkDirs() {
        File dir = new File(this.coretask.DATA_FILE_PATH);
        if (dir.exists() == false) {
            this.displayToastMessage("Application data-dir does not exist!");
        } else {
            dir = new File(this.coretask.DATA_FILE_PATH + "/bin");
            if (dir.exists() == false) {
                if (!dir.mkdir()) {
                    this.displayToastMessage("Couldn't create bin-directory!");
                }
            }
            dir = new File(this.coretask.DATA_FILE_PATH + "/var");
            if (dir.exists() == false) {
                if (!dir.mkdir()) {
                    this.displayToastMessage("Couldn't create var-directory!");
                }
            }
            dir = new File(this.coretask.DATA_FILE_PATH + "/conf");
            if (dir.exists() == false) {
                if (!dir.mkdir()) {
                    this.displayToastMessage("Couldn't create conf-directory!");
                }
            }
        }
    }

    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("H:mm:ss");
        return sdf.format(cal.getTime());
    }

    private void notifyIncomingEventActivity() {
        if (IncomingEventActivity.currentInstance != null) {
            IncomingEventActivity.currentInstance.updateHandler.sendMessage(new Message());
        }
    }

    private void notifyFriendListActivity() {
        if (FriendListActivity.currentInstance != null) {
            FriendListActivity.currentInstance.updateHandler.sendMessage(new Message());
        }
    }

    private void notifyStatusActivity() {
        if (MyStatusActivity.currentInstance != null) {
            MyStatusActivity.currentInstance.updateHandler.sendMessage(new Message());
        }
    }

    public synchronized void addEvent(String eventText) {
        eventList.add(0, now() + " " + eventText);
        notifyIncomingEventActivity();
    }

    // Display Toast-Message
    public void displayToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // From MainActivity.java
    private void openNotRootDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.norootview, null);
        new AlertDialog.Builder(this).setTitle("Not Root!").setIcon(R.drawable.warning)
                .setView(view).setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(MSG_TAG, "Close pressed");
                        // this.application.finish();
                    }
                }).setNeutralButton("Override", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Log.d(MSG_TAG, "Override pressed");
                        installBinaries();
                    }
                }).show();
    }

    // get preferences
    public boolean getAdhocMode() {
        int mode = Integer.parseInt(this.settings.getString("wifimode", "0"));
        return (mode == 0 ? true : false);
    }

    public String getUserName() {
        String n = this.settings.getString("username", "user");
        if (n.length() == 0)
            n = "user";
        return n;
    }


    public synchronized String getMessageHeader(int i) {
        return messageHeader.get(i);
    }

    public synchronized String getMessage(int i) {
        return messageList.get(i);
    }

    public synchronized List<String> getEventList() {
        return eventList;
    }

    public synchronized List<String> getMessageList() {
        return messageList;
    }

    public synchronized HashMap<Integer, FriendData> getActiveFriends() {
        return activeFriends;
    }



    public String getMyGPSString() {
        String myLoc = Double.toString(dummyLat++) + ", " + Double.toString(dummyLong++);

        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Double lat = location.getLatitude();
            Double lng = location.getLongitude();
            myLoc = Double.toString(lat) + ", " + Double.toString(lng);
        } catch (Exception e) {
            // GPS not available... use default string
        }
        return myLoc;
    }

    private static String toInetAddress(int ipAddress) {
        long ip = (ipAddress < 0) ? (long) Math.pow(2, 32) + ipAddress : ipAddress;
        String addr = String.valueOf((ip & 0xff) + "." + ((ip >> 8) & 0xff) + "."
                + ((ip >> 16) & 0xff) + "." + (ip >> 24));
        return addr;
    }

    public String[] getFriendList() {
        // TODO Auto-generated method stub
        return null;
    }
}