package edu.illinois.CS598rhk.services;

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
import edu.illinois.CS598rhk.models.BluetoothNeighbor;
import edu.illinois.CS598rhk.models.WifiNeighbor;

public class SchedulerService extends Service implements ISchedulerService {
	
	private final IBinder mBinder = new SchedulerBinder();
	
	private IWifiService wifiService;
	private IBluetoothService bluetoothService;
	
	private NeighborReceiver neighborReceiver = new NeighborReceiver();
	
	private List<WifiNeighbor> wifiNeighbors;
	private List<BluetoothNeighbor> btNeighbors;
	
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
		btNeighbors = new ArrayList<BluetoothNeighbor>();
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
		
		filter = new IntentFilter(BluetoothService.INTENT_TO_ADD_BLUETOOTH_NEIGHBOR);
		registerReceiver(neighborReceiver, filter);
		
		bindService( new Intent( SchedulerService.this, WifiService.class ), mConnection, Context.BIND_AUTO_CREATE );

		return START_STICKY;
	}
	
	private class NeighborReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR)) {
				WifiNeighbor neighbor = new WifiNeighbor();
				neighbor.name = intent.getStringExtra(WifiService.WIFI_NEIGHBOR_NAME);
				neighbor.address = intent.getStringExtra(WifiService.WIFI_IP_ADDRESS);
				
				if (!wifiNeighbors.contains(neighbor)) {
					wifiNeighbors.add(neighbor);
				}
			}
			else if (intent.getAction().equals(BluetoothService.INTENT_TO_ADD_BLUETOOTH_NEIGHBOR)) {
				BluetoothNeighbor neighbor = BluetoothNeighbor.parseByteArray(intent.getByteArrayExtra(BluetoothService.BLUETOOTH_NEIGHBOR_DATA));
				
				if (!btNeighbors.contains(neighbor)) {
					btNeighbors.add(neighbor);
				}
			}
		}
	}
}
