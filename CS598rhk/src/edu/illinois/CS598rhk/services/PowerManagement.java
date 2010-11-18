package edu.illinois.CS598rhk.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;

import android.os.BatteryManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PowerManagement extends Service implements Runnable {
	private static final String POWER_TAG = "AdHocClient_power";
	private static final String LOG_MESSAGE_TAG = "AdHocClient_logMessage";

	private BatteryInfoReceiver myBatteryInfoReceiver;
	private BatteryInfoReceiver myBatteryInfoReceiver2;
	private long startTime;

	public static final String ACTION_LOG_UPDATE = "log update";
	public static final String LOG_MESSAGE = "log message";

	String filename = "power_log";
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

		startTime = SystemClock.elapsedRealtime();
		filename = filename + "." + startTime;

		IntentFilter batteryFilter = new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED);
		batteryFilter.addAction(POWER_SERVICE);
		myBatteryInfoReceiver = new BatteryInfoReceiver();
		registerReceiver(myBatteryInfoReceiver, batteryFilter);

		IntentFilter logFilter = new IntentFilter(ACTION_LOG_UPDATE);
		batteryFilter.addAction(LOG_MESSAGE);
		myBatteryInfoReceiver2 = new BatteryInfoReceiver();
		registerReceiver(myBatteryInfoReceiver2, logFilter);

		return START_STICKY;
	}

	private class BatteryInfoReceiver extends BroadcastReceiver {

		public void onReceive(Context context, Intent intent) {
			long curTime = SystemClock.elapsedRealtime();
			Date now = new Date(curTime - startTime);
			String timeString = "Time: " + now.getHours() + ":"
					+ now.getMinutes() + ":" + now.getSeconds();
			
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
				
				String log_output = timeString +"; Battery: level = "
						+ level + ", scale = " + scale + ", status = " + status
						+ ", health = " + health + ", plugged = " + plugged
						+ ", voltage = " + voltage + "\n";

				Log.d(POWER_TAG, log_output);

				try {
					fos = openFileOutput(filename, Context.MODE_APPEND);
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
				String log_output = timeString + "; "
						+ message + "\n";

				Log.d(LOG_MESSAGE_TAG, log_output);

				try {
					fos = openFileOutput(filename, Context.MODE_APPEND);
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
