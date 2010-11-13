package edu.illinois.CS598rhk;

import android.app.Activity;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private String phoneId;
	private String ipAddr;

	private EditText phoneIdText;
	private EditText ipAddrText;
	
	private Button startButton;
	private Button stopButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView( R.layout.main );
		
		phoneIdText = (EditText) findViewById(R.id.PhoneIdText);
		phoneIdText = (EditText) findViewById(R.id.IPAddrText);
		startButton = (Button) findViewById(R.id.StartButton);
		stopButton = (Button) findViewById(R.id.StopButton);
		
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (canStart()) {
					startServices();
				}
				else {
					// Display message saying why not?
				}
			}
		});
		
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopServices();
			}
		});
	}
	
	private boolean canStart() {
		return (validIP() && validPhoneId());
	}
	
	private boolean validIP() {
		return true;
	}
	
	private boolean validPhoneId() {
		return true;
	}
	
	private void startServices() {
//		startService(new Intent(MainActivity.this, PowerManagement.class));
//		startService(new Intent(MainActivity.this, WifiService.class));
		startService(new Intent(MainActivity.this, BluetoothService.class));
//		startService(new Intent(MainActivity.this, SchedulerService.class));
		
	}
	
	private void stopServices() {
//		stopService(new Intent(MainActivity.this, PowerManagement.class));
//		stopService(new Intent(MainActivity.this, WifiService.class));
		stopService(new Intent(MainActivity.this, BluetoothService.class));
//		stopService(new Intent(MainActivity.this, SchedulerService.class));
		
	}
}
