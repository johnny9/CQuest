package edu.illinois.CS598rhk.services;

import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

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
import edu.illinois.CS598rhk.AutoStartActivity;
import edu.illinois.CS598rhk.MainActivity;
import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IBluetoothService;
import edu.illinois.CS598rhk.interfaces.ISchedulerService;
import edu.illinois.CS598rhk.interfaces.IWifiService;
import edu.illinois.CS598rhk.models.Neighbor;
import edu.illinois.CS598rhk.models.NeighborMetaData;

public class SchedulerService extends Service implements ISchedulerService {

	private static final String TAG = "SchedulerService";

	public static final String ACTION_RESET_TIMEOUT = "reset wifi timeout";

	private final IBinder mBinder = new SchedulerBinder();

	private IWifiService wifiService;
	private IBluetoothService bluetoothService;

	private MessageReceiver neighborReceiver = new MessageReceiver();

	public static Map<Neighbor, NeighborMetaData> wifiNeighbors;
	private List<BluetoothDevice> bluetoothNeighborDevices;

	private static BluetoothAdapter myDevice;
	private long progress;
	private String address;
	private boolean stoppingWifi;

	private PowerManager pm;
	private PowerManager.WakeLock wl;

	private Timer wifiPausedTimeout;

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
		wifiNeighbors = new HashMap<Neighbor, NeighborMetaData>();

		bluetoothNeighborDevices = new ArrayList<BluetoothDevice>();
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
		registerReceiver(neighborReceiver, filter);

		filter = new IntentFilter(SchedulerService.ACTION_RESET_TIMEOUT);
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

		wifiPausedTimeout = new Timer();

		//sendToLogger("Current Time, My address, Neighbor address, Neighbor IP, Name, Network type, Direct, New, Size");

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

	public void sendToLogger(String message) {
		Log.d(TAG, message);
		Intent intentToLog = new Intent(LoggingService.ACTION_LOG_UPDATE);
		intentToLog.putExtra(LoggingService.LOG_MESSAGE, message);
		intentToLog.putExtra(LoggingService.WHICH_LOG,
				LoggingService.SCHEDULER_LOG);
		sendBroadcast(intentToLog);
	}

	private synchronized void lazyInitializeNeighbors() {
		if (wifiNeighbors == null) {
			wifiNeighbors = new HashMap<Neighbor, NeighborMetaData>();
		}
	}

	public synchronized Set<Neighbor> getDirectNeighbors() {
		lazyInitializeNeighbors();

		Set<Neighbor> neighbors = new HashSet<Neighbor>();

		Set<Neighbor> keys = wifiNeighbors.keySet();
		for (Neighbor neighbor : keys) {
			NeighborMetaData data = wifiNeighbors.get(neighbor);

			if (data.directContact) {
				neighbors.add(new Neighbor(neighbor));
			}
		}
		return neighbors;
	}

	public synchronized Set<Neighbor> getNeighbors() {
		lazyInitializeNeighbors();

		Set<Neighbor> neighbors = new HashSet<Neighbor>();

		Set<Neighbor> keys = wifiNeighbors.keySet();

		for (Neighbor neighbor : keys) {
			neighbors.add(new Neighbor(neighbor));
		}
		return neighbors;
	}

	// TODO: Wifi beacon sends directly discovered bluetooth neighbors

	public synchronized String updateNeighbor(Neighbor neighbor,
			boolean direct, String networkType) {
		lazyInitializeNeighbors();
		Time time = new Time(System.currentTimeMillis());

		String logMessage = myDevice.getAddress() + ", " + neighbor.btAddr;
		NeighborMetaData newData = new NeighborMetaData(time, direct,
				networkType);
		NeighborMetaData exists = wifiNeighbors.get(neighbor);

		if (exists == null || (exists != null && exists.directContact == false)) {
			wifiNeighbors.put(new Neighbor(neighbor), newData);
			//logMessage += ", " + newData.directContact;
			//logMessage += ", fresh";
			//logMessage += ", " + wifiNeighbors.size();
		} else {
			exists.lastSeen = time;
			wifiNeighbors.put(new Neighbor(neighbor), exists);
			//logMessage += ", " + exists.directContact;
			//logMessage += ", old";
			//logMessage += ", " + wifiNeighbors.size();

		}

		if (exists == null) {
			logMessage += ", " + AutoStartActivity.startTime.toString();
			logMessage += ", " + neighbor.timestamp;
			logMessage += ", " + time.toString();
			logMessage += ", " + AutoStartActivity.casenumber;
		} else {
			logMessage = "";
		}

		return logMessage;
	}

	private class MessageReceiver extends BroadcastReceiver {
		private static final int BLUETOOTH_DISCOVERY_STOP_TIME = 200000;
		private static final int ELECTION_START_TIME = 100000;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR.equals(intent
					.getAction())) {

				IBluetoothMessage message = Neighbor
						.newNeighborReader()
						.parse(intent
								.getByteArrayExtra(WifiService.WIFI_NEIGHBOR_DATA));

				String source = intent
						.getStringExtra(WifiService.INTENT_TO_ADD_WIFI_NEIGHBOR_SOURCE);
				Boolean isDirect = intent.getBooleanExtra(
						WifiService.INTENT_TO_ADD_DIRECT_NEIGHBOR, true);
				Neighbor neighbor = (Neighbor) message;
				if (neighbor.btAddr.equals(myDevice.getAddress()))
					return;
				if (WifiService.DISCOVERED_OVER_WIFI.equals(source)) {
					sendToLogger(updateNeighbor(neighbor, isDirect,
							NeighborMetaData.WIFI_NETWORK));
				} else if (WifiService.DISCOVERED_OVER_BLUETOOTH.equals(source)) {
					sendToLogger(updateNeighbor(neighbor, true,
							NeighborMetaData.BLUETOOTH_NETWORK));
				} else if (WifiService.DISCOVERED_OVER_BLUETOOTH_DIRECT
						.equals(source)) {
					sendToLogger(updateNeighbor(neighbor, true,
							NeighborMetaData.BLUETOOTH_NETWORK));
				}

				synchronized (BluetoothService.activeNeighbors) {
					for (BluetoothDevice device : BluetoothService.activeNeighbors) {
						if (neighbor.btAddr.equals(device.getAddress())) {
							// this neighbor is an active bt neighbor
							if (myDevice.getAddress()
									.compareTo(neighbor.btAddr) < 0) {
								// uh oh, he appears to be discovering as well
								// abort! abort!
								stoppingWifi = true;
								wifiService.forcedPauseWifiService();
								bluetoothService.stopDiscovery();
								resetTimeout();
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
						String address = device.getAddress();
						String name = device.getName();
						Neighbor neighb = new Neighbor("?", "GARBAGE", address);
						sendToLogger(updateNeighbor(neighb, true,
								NeighborMetaData.BLUETOOTH_DISCOVERY));
					}
				}

			} else if (WifiService.INTENT_TO_UPDATE_SCHEDULE_PROGRESS
					.equals(intent.getAction())) {
				progress = intent.getLongExtra(
						WifiService.SCHEDULE_PROGRESS_UPDATE, 0);
				bluetoothService.updateScheduleProgress(progress);
				if (progress > BLUETOOTH_DISCOVERY_STOP_TIME) {
					if (!stoppingWifi)
						bluetoothService.startDiscovery();
					bluetoothService.resetWifiDiscoveryElection();
				} else if (progress <= ELECTION_START_TIME && progress > 0
						&& !stoppingWifi) {
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
					wifiPausedTimeout.cancel();
					wifiPausedTimeout.purge();
				} else {
					stoppingWifi = true;
					if (progress > ELECTION_START_TIME)
						wifiService.forcedPauseWifiService();
					else
						wifiService.pauseWifiService();
					bluetoothService.stopDiscovery();
					resetTimeout();

				}
			} else if (SchedulerService.ACTION_RESET_TIMEOUT.equals(intent
					.getAction())) {
				resetTimeout();
			}
		}

		private void resetTimeout() {
			wifiPausedTimeout.cancel();
			wifiPausedTimeout.purge();
			wifiPausedTimeout = new Timer();
			wifiPausedTimeout.schedule(new KickStartTimerTask(), 200 * 5000);
		}
	}

	public class KickStartTimerTask extends TimerTask {
		@Override
		public void run() {
			Log.d(TAG, "Haven't heard from my leader yet.  Taking charge!");
			stoppingWifi = false;
			wifiService.resumeWifiService(10);
		}
	}
}