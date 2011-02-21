package edu.illinois.CS598rhk.services;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import edu.illinois.CS598rhk.MainActivity;
import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IBluetoothService;
import edu.illinois.CS598rhk.interfaces.ISchedulerService;
import edu.illinois.CS598rhk.interfaces.IWifiService;
import edu.illinois.CS598rhk.models.WifiNeighbor;

public class SchedulerService extends Service implements ISchedulerService {

	private static final String TAG = "SchedulerService";

	private final IBinder mBinder = new SchedulerBinder();

	private IWifiService wifiService;
	private IBluetoothService bluetoothService;

	private MessageReceiver neighborReceiver = new MessageReceiver();

	public static List<WifiNeighbor> wifiNeighbors;
	private List<BluetoothDevice> bluetoothNeighborDevices;

	private BluetoothAdapter myDevice;
	private long progress;
	private String address;
	private boolean stoppingWifi;

	private PowerManager pm;
	private PowerManager.WakeLock wl;

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
		bluetoothNeighborDevices = new ArrayList<BluetoothDevice>();
	}

	private ServiceConnection mWifiConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			wifiService = ((WifiService.WifiBinder) service).getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			wifiService = null;
		}
	};

	private ServiceConnection mBluetoothConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			bluetoothService = ((BluetoothService.BluetoothBinder) service)
					.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			bluetoothService = null;
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Notification notification = new Notification();
		notification.tickerText = "SchedulerService";
		startForeground(1, notification);

		IntentFilter filter = new IntentFilter(
				WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR);
		registerReceiver(neighborReceiver, filter);

		filter = new IntentFilter(
				WifiService.INTENT_TO_UPDATE_SCHEDULE_PROGRESS);
		registerReceiver(neighborReceiver, filter);

		filter = new IntentFilter(
				BluetoothService.ACTION_ELECTED_FOR_WIFI_DISCOVERY);
		registerReceiver(neighborReceiver, filter);

		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(neighborReceiver, filter); // Don't forget to
													// unregister during
													// onDestroy

		address = intent.getStringExtra(MainActivity.ADDRESS_KEY);
		stoppingWifi = false;
		myDevice = BluetoothAdapter.getDefaultAdapter();
		bluetoothNeighborDevices.addAll(myDevice.getBondedDevices());

		// on startup, since bt neighbors are currently static
		// set leader to be the one with the lowest mac address
		// this way a leader can be decided before the first election is even
		// held
		int wifiStartState = WifiService.WIFI_STATE_DISCOVERYING;
		
		bindService(new Intent(SchedulerService.this, WifiService.class),
				mWifiConnection, Context.BIND_AUTO_CREATE);
		bindService(new Intent(SchedulerService.this, BluetoothService.class),
				mBluetoothConnection, Context.BIND_AUTO_CREATE);

		Intent i = new Intent(SchedulerService.this, WifiService.class);
		i.putExtra(MainActivity.ADDRESS_KEY, address);
		i.putExtra(WifiService.WIFI_START_STATE, wifiStartState);
		startService(i);

		i = new Intent(SchedulerService.this, BluetoothService.class);
		startService(i);

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		wl.acquire();

		return START_STICKY;
	}

	public void onDestroy() {
		wl.release();
		unregisterReceiver(neighborReceiver);
		unbindService(mBluetoothConnection);
		unbindService(mWifiConnection);
		stopForeground(true);
		super.onDestroy();
	}

	public void sendToLoggerS(String message) {
		Log.d(TAG, message);
		Intent intentToLog = new Intent(PowerManagement.ACTION_LOG_UPDATE);
		intentToLog.putExtra(PowerManagement.LOG_MESSAGE, message);
		sendBroadcast(intentToLog);
	}

	private class MessageReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR.equals(intent
					.getAction())) {
				
				IBluetoothMessage message = WifiNeighbor.newWifiNeighborReader().parse(intent
						.getByteArrayExtra(WifiService.WIFI_NEIGHBOR_DATA));

				WifiNeighbor neighbor = (WifiNeighbor) message;
				
				if (!wifiNeighbors.contains(neighbor)) {
					wifiNeighbors.add(neighbor);
				}

				synchronized (BluetoothService.activeNeighbors) {
					for (BluetoothDevice device : BluetoothService.activeNeighbors) {
						if (neighbor.address.equals(device.getAddress())) {
							// this neighbor is an active bt neighbor
							if (myDevice.getAddress().compareTo(
									neighbor.address) < 0) { 
								// uh oh, he appears to be discovering as well
								// abort! abort!
								if (progress < 44000) {
									stoppingWifi = true;
									wifiService.forcedPauseWifiService();
									bluetoothService.stopDiscovery();
								}
							}
						}
					}
				}
			} else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
				// Get the BluetoothDevice object from the Intent
				BluetoothDevice device = intent
						.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

				if (!BluetoothService.activeNeighbors.contains(device)
						&& BluetoothService.potentialNeighbors.contains(device)) {
					synchronized (BluetoothService.activeNeighbors) {
						BluetoothService.activeNeighbors.add(device);
					}
				}

			} else if (WifiService.INTENT_TO_UPDATE_SCHEDULE_PROGRESS
					.equals(intent.getAction())) {
				progress = intent.getLongExtra(
						WifiService.SCHEDULE_PROGRESS_UPDATE, 0);
				bluetoothService.updateScheduleProgress(progress);
				if (progress > 20000) {
					if (!stoppingWifi)
						bluetoothService.startDiscovery();
					bluetoothService.resetWifiDiscoveryElection();
				} else if (progress <= 15000 && progress > 0 && !stoppingWifi) {
					if (BluetoothService.activeNeighbors.size() > 0) {
						bluetoothService.hostWifiDiscoveryElection();
					}
				}
			} else if (BluetoothService.ACTION_ELECTED_FOR_WIFI_DISCOVERY
					.equals(intent.getAction())) {
				Long delay = intent.getLongExtra(
						BluetoothService.DELY_UNTIL_STARTING_WIFI_DISCOVERY, 0);

				if (delay > 0) {
					stoppingWifi = false;
					wifiService.resumeWifiService(delay);

				} else {
					stoppingWifi = true;
					wifiService.pauseWifiService();
					bluetoothService.stopDiscovery();
				}
			}
		}
	}
}