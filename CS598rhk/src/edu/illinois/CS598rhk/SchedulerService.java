package edu.illinois.CS598rhk;

import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import edu.illinois.CS598rhk.interfaces.IBluetoothService;
import edu.illinois.CS598rhk.interfaces.ISchedulerService;
import edu.illinois.CS598rhk.interfaces.IWifiService;

public class SchedulerService extends Service implements ISchedulerService {

	private final IBinder mBinder = new SchedulerBinder();
	
	private IWifiService wifiService;
	private IBluetoothService bluetoothService;
	
	private NeighborReceiver neighborReceiver = new NeighborReceiver();
	
	private List<WifiNeighbor> wifiNeighbors;
	private List<BTNeighbor> btNeighbors;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return mBinder;
	}

	public class SchedulerBinder extends Binder {
        public ISchedulerService getService() {
            return SchedulerService.this;
        }
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
		wifiNeighbors = new ArrayList<WifiNeighbor>();
		btNeighbors = new ArrayList<BTNeighbor>();
	}
	
	private ServiceConnection mConnection = new ServiceConnection()
    {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			wifiService = ( (WifiService.WifiBinder) service ).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			wifiService = null;	
		}
    };
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		IntentFilter filter = new IntentFilter(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR);
		registerReceiver(neighborReceiver, filter);
		
		filter = new IntentFilter(BluetoothService.INTENT_TO_ADD_BT_NEIGHBOR);
		registerReceiver(neighborReceiver, filter);
		
		bindService( new Intent( SchedulerService.this, WifiService.class ), mConnection, Context.BIND_AUTO_CREATE );

		return super.onStartCommand(intent, flags, startId);
	}
	
	private class NeighborReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR)) {
				WifiNeighbor neighbor = new WifiNeighbor();
				neighbor.name = intent.getStringExtra(WifiService.WIFI_NEIGHBOR_NAME);
				neighbor.ipAddr = intent.getStringExtra(WifiService.WIFI_IP_ADDRESS);
				
				if (!wifiNeighbors.contains(neighbor)) {
					wifiNeighbors.add(neighbor);
				}
			}
			else if (intent.getAction().equals(BluetoothService.INTENT_TO_ADD_BT_NEIGHBOR)) {
				BTNeighbor neighbor = new BTNeighbor();
				neighbor.name = intent.getStringExtra(BluetoothService.BT_NEIGHBOR_NAME);
				neighbor.macAddr = intent.getStringExtra(BluetoothService.BT_MAC_ADDRESS);
				
				if (!btNeighbors.contains(neighbor)) {
					btNeighbors.add(neighbor);
				}
			}
		}
	}
	
	private class WifiNeighbor {
		public String name;
		public String ipAddr;
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof WifiNeighbor) {
				WifiNeighbor neighbor = (WifiNeighbor) o;
				return (name.equals(neighbor.name) && ipAddr.equals(neighbor.ipAddr));
			}
			return false;
		}
	}
	
	private class BTNeighbor {
		public String name;
		public String macAddr;
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof BTNeighbor) {
				BTNeighbor neighbor = (BTNeighbor) o;
				return (name.equals(neighbor.name) && macAddr.equals(neighbor.macAddr));
			}
			return false;
		}
	}
}
