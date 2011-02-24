package edu.illinois.CS598rhk.services;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.sql.Time;
import java.util.List;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import edu.illinois.CS598rhk.MainActivity;
import edu.illinois.CS598rhk.interfaces.IMessageReader;
import edu.illinois.CS598rhk.interfaces.IWifiService;
import edu.illinois.CS598rhk.models.Neighbor;
import edu.illinois.CS598rhk.models.NeighborMetaData;
import edu.illinois.CS598rhk.models.WifiMessage;
import edu.illinois.CS598rhk.schedules.DiscoverSchedule;
import edu.illinois.CS598rhk.schedules.SearchLightSchedule;

public class WifiService extends Service implements IWifiService {
	public static final String INTENT_TO_UPDATE_SCHEDULE_PROGRESS = "intent to update schedule progress";
	public static final String SCHEDULE_PROGRESS_UPDATE = "schedule progress update";
	public static final String INTENT_TO_RESUME_WIFI = "intent to update the wifi state";
	public static final String INTENT_TO_PAUSE_WIFI = "(g\")-O";
	public static final String INTENT_TO_ADD_WIFI_NEIGHBOR = "intent to add a wifi neighbor";
	public static final String INTENT_TO_ADD_WIFI_NEIGHBOR_SOURCE = "source of discovered wifi neighbor";
	public static final String DISCOVERED_OVER_WIFI = "Wifi neighbor found over wifi";
	public static final String DISCOVERED_OVER_BLUETOOTH = "neighbor found over bluetooth";
	public static final String WIFI_NEIGHBOR_DATA = "wifi neighbor data";
	public static final String INTENT_TO_CHANGE_WIFI_ADDRESS = "WHERE ARE ALL MY WIFIS";
	public static final String NEW_WIFI_ADDRESS = "asdfasdfasdfasdfasdf";
	public static final String WIFI_NEIGHBOR_NAME = "phone name";
	public static final String WIFI_IP_ADDRESS = "ip address";
	public static final String WIFI_START_STATE = "initial wifi state";
	public static final int WIFI_STATE_DISCOVERYING = 1;
	public static final int WIFI_STATE_PAUSED = 0;

	private static final int DISCOVERY_TIMESLICE = 5000;
	private static final String MSG_TAG = "MyWifiService";
	public static final String RESUME_DELAY = "O_o";
	public static final String NEW_PHONE_NAME = "<><><><><><><><><><<";
	public static final String INTENT_TO_UPDATE_NAME = "^^;;";
	public static final String WIFI_NEIGHBOR_CURRENT_TIMESLICE = "())(())()()()()()()()(";
	public static final String WIFI_NEIGHBOR_SCHEDULE_LENGTH = "+++++++++++";

	private DiscoverSchedule discoveryScheduler;
	private boolean wifiEnabled = false;
	private CoreTask coretask;

	private WifiManager wifiManager;
	private WifiMessageReceiver messageReceiver;
	private WifiController wifiController;

	public static String myIPAddress;
	private static String myBluetoothAddress;
	private String myBroadcast;
	public static int timeSlice;
	public static int wifiState;
	private Neighbor myInfo;
	private boolean pauseForced;
	private int logPreviousState;

	InetAddress dest;

	private final IBinder mBinder = new WifiBinder();
	public static long resumeDelay;

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
		myBroadcast = "192.168.1.255";
		discoveryScheduler = new SearchLightSchedule(10);
		coretask = new CoreTask();
		logPreviousState = -1;
		try {
			dest = InetAddress.getByName(myBroadcast);
		} catch (UnknownHostException e) {
			Log.d(MSG_TAG, "Broadcast address " + myBroadcast + " as unknown.");
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Notification notification = new Notification();
		notification.tickerText = "WifiService";
		startForeground(3, notification);

		IntentFilter messageFilter = new IntentFilter();
		messageFilter.addAction(INTENT_TO_PAUSE_WIFI);
		messageFilter.addAction(INTENT_TO_RESUME_WIFI);
		messageFilter.addAction(INTENT_TO_CHANGE_WIFI_ADDRESS);
		messageFilter.addAction(INTENT_TO_UPDATE_NAME);
		messageReceiver = new WifiMessageReceiver();
		registerReceiver(messageReceiver, messageFilter);

		coretask = new CoreTask();
		coretask.setPath(this.getApplicationContext().getFilesDir().getParent());
		Log.d(MSG_TAG, "Current directory is "
				+ this.getApplicationContext().getFilesDir().getParent());

		if (wifiManager.isWifiEnabled())
			wifiManager.setWifiEnabled(false);

		myIPAddress = intent.getStringExtra(MainActivity.ADDRESS_KEY);
		if (myIPAddress == null || !validateIPAddress(myIPAddress))
			myIPAddress = "192.168.1.3";

		myBluetoothAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
		this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
				+ "/bin/load.sh " + myIPAddress);

		myInfo = new Neighbor("phone", myIPAddress, myBluetoothAddress);
		
		pauseForced = false;
		wifiState = intent.getIntExtra(WifiService.WIFI_START_STATE,
				WIFI_STATE_DISCOVERYING);
		wifiController.start();

		return START_STICKY;
	}

	public void setIPAddress(String ip) {
		if (!validateIPAddress(ip))
			return;
		myBroadcast = ip.substring(0, ip.lastIndexOf(".")) + ".255";
		this.coretask.runRootCommand("/system/bin/ifconfig tiwlan0 " + ip
				+ " netmask 255.255.255.0");
		try {
			dest = InetAddress.getByName(myBroadcast);

		} catch (UnknownHostException e) {
			Log.d(MSG_TAG, "Broadcast address " + myBroadcast + " as unknown.");
		}
	}

	private boolean validateIPAddress(String ipAddress) {
		String[] parts = ipAddress.split("\\.");
		if (parts.length != 4) {
			return false;
		}
		for (String s : parts) {
			int i = Integer.parseInt(s);

			if ((i < 0) || (i > 255)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void onDestroy() {
		wifiController.stop();
		disableWifi();
		unregisterReceiver(messageReceiver);
		stopForeground(true);
		super.onDestroy();
	}

	public void enableWifi() {
		this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
				+ "/bin/netcontrol startwifi " + this.myIPAddress);
		Log.d(MSG_TAG, "Wifi Enabled");
		wifiEnabled = true;
	}

	public void disableWifi() {
		this.coretask.runRootCommand(this.coretask.DATA_FILE_PATH
		 + "/bin/netcontrol stopwifi");
		Log.d(MSG_TAG, "Wifi disabled");
		wifiEnabled = false;
	}

	private class WifiMessageReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction() == INTENT_TO_RESUME_WIFI) {
				resumeWifiService(intent.getLongExtra(RESUME_DELAY, 0));
			} else if (intent.getAction() == INTENT_TO_PAUSE_WIFI) {
				pauseWifiService();
			} else if (intent.getAction() == INTENT_TO_CHANGE_WIFI_ADDRESS) {
				setIPAddress(intent.getStringExtra(NEW_WIFI_ADDRESS));
			}

		}
	}

	private class WifiController extends Thread {

		private void sendWifiBroadcast() {
			WifiMessage message = new WifiMessage(myInfo, BluetoothService.getNeighborAddrs());
			byte[] buf = message.pack();
			DatagramSocket sock;
			DatagramPacket pkt = new DatagramPacket(buf, buf.length, dest, 8888);
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

		private void listenForWifiBroadcast(long startTime) {
			byte[] buf = new byte[1024];
			DatagramPacket pkt = new DatagramPacket(buf, buf.length, dest, 8888);
			DatagramSocket sock = null;
			do {
				try {
					sock = new DatagramSocket(8888);
					int timeout = (int) (DISCOVERY_TIMESLICE - (System
							.currentTimeMillis() - startTime));
					if (timeout < 0)
						timeout = 0;

					sock.setSoTimeout(timeout);
					sock.receive(pkt);

					IMessageReader reader = WifiMessage.newMessageReader();
					WifiMessage message = (WifiMessage) reader.parse(pkt.getData());

					SchedulerService.updateNeighbor(message.deviceInfo, true, NeighborMetaData.WIFI_NETWORK);
					
					List<String> devices = message.getNeighborAddrs();
					for (String addr : devices) {
						SchedulerService.updateNeighbor(new Neighbor("GARBAGE NAME",
								"GARBAGE IP", addr), false,
								NeighborMetaData.WIFI_NETWORK);
					}
					
					sock.close();
				} catch (InterruptedIOException e) {
					// timed out, so no message was received
					if (!sock.isClosed())
						sock.close();
				} catch (SocketException e) {
					// couldn't create the socket
					enableWifi();
				} catch (IOException e) {
					// error receiving the packet, try again
				}
			} while (System.currentTimeMillis() - startTime < DISCOVERY_TIMESLICE);

		}

		@Override
		public void run() {
			setName("Wificontroller");
			enableWifi();
			int[] mySchedule = discoveryScheduler.generateScedule();
			timeSlice = 0;
			Looper.prepare();
			while (true) {
				if (wifiState == WIFI_STATE_PAUSED) {
					logStatePaused();
					while (wifiState != WIFI_STATE_DISCOVERYING) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// interrupted from sleep
						}
					}
					try {
						Thread.sleep(resumeDelay);
					} catch (InterruptedException e) {
						// interrupted from sleep >=(

					}
					timeSlice = 0;
					pauseForced = false;
				}

				do {
					if (wifiState == WIFI_STATE_PAUSED && pauseForced) {
						timeSlice = 0;
						pauseForced = false;
						break;
					}
					if (timeSlice == 0) {
						logStateDiscoverying();
						mySchedule = discoveryScheduler.generateScedule();
					}

					long startTime = System.currentTimeMillis();
					if (!wifiEnabled) {
						if (mySchedule[(timeSlice + 1) % mySchedule.length] != DiscoverSchedule.DO_NOTHING) {
							enableWifi();
						}
					} else {
						if (mySchedule[timeSlice] != DiscoverSchedule.DO_NOTHING) {
							if (mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT_N_LISTEN
									|| mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT) {
								sendWifiBroadcast();
							}

							if (mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT_N_LISTEN
									|| mySchedule[timeSlice] == DiscoverSchedule.LISTEN) {
								listenForWifiBroadcast(startTime);
							}
							if (mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT_N_LISTEN
									|| mySchedule[timeSlice] == DiscoverSchedule.TRANSMIT) {
								sendWifiBroadcast();
							}

						}

					}

					if (mySchedule[(timeSlice + 1) % mySchedule.length] == DiscoverSchedule.DO_NOTHING
							&& wifiEnabled) {
						disableWifi();
					}

					try {
						long sleepyTime = DISCOVERY_TIMESLICE
								- (System.currentTimeMillis() - startTime);
						if (sleepyTime < 0)
							sleepyTime = 0;
						Thread.sleep(sleepyTime);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}

					timeSlice = (timeSlice + 1) % mySchedule.length;
					Intent i = new Intent(INTENT_TO_UPDATE_SCHEDULE_PROGRESS);
					i.putExtra(SCHEDULE_PROGRESS_UPDATE,
							scheduleTimeRemaining());
					sendBroadcast(i);
				} while (timeSlice > 0);
			}
		}
	}

	public void resumeWifiService(long delay) {
		wifiState = WIFI_STATE_DISCOVERYING;
		resumeDelay = delay;
		pauseForced = true;
		wifiController.interrupt();
	}

	public void logStatePaused() {
		String message;
		String time;
		if (logPreviousState != WIFI_STATE_PAUSED) {
			logPreviousState = WIFI_STATE_PAUSED;
			time = (new Time(System.currentTimeMillis())).toString();
			message = time + ", " + myBluetoothAddress + ", 1\n";
			message += time + ", " + myBluetoothAddress + ", 0";

			sendToLogger(message);
		}
	}

	public void logStateDiscoverying() {
		String message;
		String time;

		if (logPreviousState != WIFI_STATE_DISCOVERYING) {
			logPreviousState = WIFI_STATE_DISCOVERYING;
			time = (new Time(System.currentTimeMillis())).toString();
			message = time + ", " + myBluetoothAddress + ", 0\n";
			message += time + ", " + myBluetoothAddress + ", 1";

			sendToLogger(message);
		}
	}

	public void pauseWifiService() {
		wifiState = WIFI_STATE_PAUSED;
	}

	@Override
	public long scheduleTimeRemaining() {
		return (discoveryScheduler.scheduleLength() - timeSlice)
				* DISCOVERY_TIMESLICE;
	}

	public void sendToLogger(String message) {
		Intent intentToLog = new Intent(LoggingService.ACTION_LOG_UPDATE);
		intentToLog.putExtra(LoggingService.LOG_MESSAGE, message);
		intentToLog.putExtra(LoggingService.WHICH_LOG, LoggingService.WIFI_LOG);
		sendBroadcast(intentToLog);
	}

	@Override
	public void forcedPauseWifiService() {
		pauseForced = true;
		wifiState = WIFI_STATE_PAUSED;
	}
}
