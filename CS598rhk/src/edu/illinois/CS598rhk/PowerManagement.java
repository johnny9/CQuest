package edu.illinois.CS598rhk;

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
    
    @Override
    public void onCreate() {
    	//Log.d(POWER_TAG, "Blah zero");
    
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.d(POWER_TAG, "Blah one");
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryFilter.addAction(POWER_SERVICE);
        myBatteryInfoReceiver = new BatteryInfoReceiver();
        registerReceiver(myBatteryInfoReceiver, batteryFilter);
        return super.onStartCommand(intent, flags, startId);
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
	            
	            Log.d(POWER_TAG, "Time: " + curTime + "; Battery: level = " + level + 
	            		", scale = " + scale + ", status = " + status + 
	            		", health = " + health + ", plugged = " + plugged + 
	            		", voltage = " + voltage);
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
