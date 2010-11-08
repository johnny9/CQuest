package edu.illinois.CS598rhk;

import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Looper;
import android.util.Log;

public class NetworkControl {
    private volatile static NetworkControl netController;
    private static final String MSG_TAG = "AdhocClientNetworkControl";
    
    private WifiController wifiController;
    private BluetoothController bluetoothController;
     
    
    private NetworkControl(WifiManager wifiManager)
    {
        this.wifiController = WifiController.getInstance(wifiManager);
        this.bluetoothController = BluetoothController.getInstance();
    }
    
    public static NetworkControl getInstance(WifiManager wifiManager) {
        if(netController == null) {
            synchronized(NetworkControl.class) {
                if(netController == null) {
                    netController = new NetworkControl(wifiManager);
                }
            }
        }
        
        return netController;
    }
    
    public void enableWifi() {
        
    }
    
    public void disableWifi() {
        
    }
    
    public void setMyIPAddress(String ip) {
        
    }

    
    
}
