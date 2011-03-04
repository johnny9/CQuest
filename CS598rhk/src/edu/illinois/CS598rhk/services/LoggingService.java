package edu.illinois.CS598rhk.services;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class LoggingService extends Service implements Runnable {
	private static final String POWER_TAG = "AdHocClient_power";
	private static final String LOG_MESSAGE_TAG = "AdHocClient_logMessage";

	private BatteryInfoReceiver myBatteryInfoReceiver;

	private long startTime;

	public static final String ACTION_LOG_UPDATE = "log update";
	public static final String LOG_MESSAGE = "log message";
	public static final String WIFI_LOG = "wifi_discovery";
	public static final String SCHEDULER_LOG = "new_neighbor";
	public static final String WHICH_LOG = "which log?";
	public static final String POWER_LOG = "power_log";
	public static final String MISC_LOG = "misc_log";

	FileOutputStream fos;

	@Override
	public void onCreate() {
		// TODO implement the function
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// File outputFile = new File("data/data/edu.illinois.CS598rhk/files",
		// filename);
		// if (outputFile.exists()) {
		// deleteFile(filename);
		// }

    	Notification notification = new Notification();
    	notification.tickerText = "PowerManagement";
    	startForeground(0, notification);
		
		startTime = System.currentTimeMillis();

		IntentFilter batteryFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		batteryFilter.addAction(POWER_SERVICE);
		myBatteryInfoReceiver = new BatteryInfoReceiver();
		registerReceiver(myBatteryInfoReceiver, batteryFilter);

		IntentFilter logFilter = new IntentFilter(ACTION_LOG_UPDATE);
		registerReceiver(myBatteryInfoReceiver, logFilter);
		
		clearLog();

		return START_STICKY;
	}

	private class BatteryInfoReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			long curTime = System.currentTimeMillis();
			Time now = new Time(curTime-startTime);
			String timeString = now.toString();
			
			if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {

				int rawlevel = intent.getIntExtra("level", -1);
				int scale = intent.getIntExtra("scale", -1);
				int status = intent.getIntExtra("status", -1);
				int health = intent.getIntExtra("health", -1);
				int plugged = intent.getIntExtra("plugged", -1);
				int voltage = intent.getIntExtra("voltage", -1);
				int level = -1;
				if (rawlevel >= 0 && scale > 0) {
					level = (rawlevel * 100) / scale;
				}
				
				String log_output = (new Time(curTime)).toString() + ", " + timeString +", Battery: level = "
						+ level + ", scale = " + scale + ", status = " + status
						+ ", health = " + health + ", plugged = " + plugged
						+ ", voltage = " + voltage + "\n";

				Log.d(POWER_TAG, log_output);

				try {
					fos = openFileOutput(POWER_LOG, Context.MODE_APPEND);
					fos.write(log_output.getBytes());
					fos.close();
				} catch (FileNotFoundException e) {
					Log.e(POWER_TAG, "Log file could not be open");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(POWER_TAG, "Log file could not be written to");
					e.printStackTrace();
				}
			}
			else if (intent.getAction().equals(ACTION_LOG_UPDATE)) {
				String message = intent.getStringExtra(LOG_MESSAGE);
				String filenameString = intent.getStringExtra(WHICH_LOG);
				String log_output = message + "\n";

				Log.d(LOG_MESSAGE_TAG, log_output);

				try {
					fos = openFileOutput(filenameString, Context.MODE_APPEND);
					fos.write(log_output.getBytes());
					fos.close();
				} catch (FileNotFoundException e) {
					Log.e(LOG_MESSAGE_TAG, "Log file could not be open");
					e.printStackTrace();
				} catch (IOException e) {
					Log.e(LOG_MESSAGE_TAG, "Log file could not be written to");
					e.printStackTrace();
				}
			}
		}
	}

	public void clearLog()
	{
		deleteFile(POWER_LOG);
		deleteFile(WIFI_LOG);
		deleteFile(SCHEDULER_LOG);	
	}
	
	@Override
	public void onDestroy() {
		unregisterReceiver(myBatteryInfoReceiver);
		stopForeground(true);
		super.onDestroy();
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

}
