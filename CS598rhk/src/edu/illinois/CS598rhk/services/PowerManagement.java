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
    private BatteryInfoReceiver myBatteryInfoReceiver;
    public static final String ACTION_LOG_UPDATE = "log update";
    String filename = "power_log";
    FileOutputStream fos;
    
    @Override
    public void onCreate() {
    	//Log.d(POWER_TAG, "Blah zero");

    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(POWER_TAG, "Blah one");
    	
    	File outputFile = new File("data/data/edu.illinois.CS598rhk/files", filename);
        if (outputFile.exists()) {
        	//Log.i(POWER_TAG, "Log file to be removed");
        	deleteFile(filename);
        }
    	
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(POWER_SERVICE);
        myBatteryInfoReceiver = new BatteryInfoReceiver();
        registerReceiver(myBatteryInfoReceiver, batteryFilter);
        return START_STICKY;
    }
    
    private class BatteryInfoReceiver extends BroadcastReceiver {
        
        public void onReceive(Context context, Intent intent) {
        	if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED))
        	{
	            //Log.d(POWER_TAG, "Blah two");
	            //context.unregisterReceiver(this);
	            int rawlevel = intent.getIntExtra("level", -1);
	            int scale = intent.getIntExtra("scale", -1);
	            int status = intent.getIntExtra("status", -1);
	            int health = intent.getIntExtra("health", -1);
	            int plugged = intent.getIntExtra("plugged", -1);
	            int voltage = intent.getIntExtra("voltage", -1);
	            int level = -1;  // percentage, or -1 for unknown
	            if (rawlevel >= 0 && scale > 0) {
	                level = (rawlevel * 100) / scale;
	            }
	            
	            long curTime = SystemClock.elapsedRealtime();
	            String log_output = "Time: " + curTime + "; Battery: level = " + level 
        						+ ", scale = " + scale + ", status = " + status + 
        						", health = " + health + ", plugged = " + plugged + 
        						", voltage = " + voltage + "\n";
	            
	            Log.d(POWER_TAG, log_output);
	            
	            
	            
				/*try {
					File dir = getDir("logs", Context.MODE_WORLD_READABLE);
		            File file = new File(dir, "power_log");
		            FileWriter fwriter = new FileWriter(file);
					BufferedWriter output = new BufferedWriter(fwriter);
	                output.write(log_output);
	                output.close();
				} catch (IOException e1) {
					Log.e(POWER_TAG, "Log file could not be open");
					e1.printStackTrace();
				}*/
				
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

	            /*FileInputStream fIn = null;
	            InputStreamReader isr = null;
	            try{
	            	char[] inputBuffer = new char[1024];
	            	String data = null;
	            	fIn = openFileInput(filename);
	            	isr = new InputStreamReader(fIn);
	            	isr.read(inputBuffer);
	            	data = new String(inputBuffer);
	            	isr.close();
	            	fIn.close();
	            	Log.i(POWER_TAG, "File output: " + data);
	            }catch(IOException e){
	            	e.printStackTrace(System.err);
	            }*/
        	}
        	
        	else if (intent.getAction().equals(ACTION_LOG_UPDATE))
        	{
        		
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
