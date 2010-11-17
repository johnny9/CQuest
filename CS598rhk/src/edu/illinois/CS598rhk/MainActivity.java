package edu.illinois.CS598rhk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import edu.illinois.CS598rhk.services.BluetoothService;
import edu.illinois.CS598rhk.services.CoreTask;
import edu.illinois.CS598rhk.services.PowerManagement;
import edu.illinois.CS598rhk.services.SchedulerService;
import edu.illinois.CS598rhk.services.WifiService;

public class MainActivity extends Activity {
	
	private static String ICICLE_KEY = "main-activity";
	private static String PHONEID_KEY = "phoneID";
	private static String IPADDR_KEY = "ipAddr";
	private static String STATE_KEY = "state";
	private CoreTask coretask;
	
	private String phoneID;
	private String ipAddr;

	private EditText phoneIdText;
	private EditText ipAddrText;
	
	private Button startButton;
	private Button stopButton;
	
	private boolean servicesStarted;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null) {
			phoneID = null;
			ipAddr = null;
			servicesStarted = false;
		} else {
			Bundle map = savedInstanceState.getBundle(ICICLE_KEY);
			if (map != null) {
				phoneID = map.getString(PHONEID_KEY);
				ipAddr = map.getString(IPADDR_KEY);
				servicesStarted = map.getBoolean(STATE_KEY);
			}
		}

		setContentView( R.layout.main );
		
		phoneIdText = (EditText) findViewById(R.id.PhoneIdText);
		ipAddrText = (EditText) findViewById(R.id.IPAddrText);
		startButton = (Button) findViewById(R.id.StartButton);
		stopButton = (Button) findViewById(R.id.StopButton);
		
		updateButtons(servicesStarted);
		coretask = new CoreTask();
		if (coretask.hasRootPermission()) {
            
        } else {
            this.openNotRootDialog();
        }
		
		startButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (canStart()) {
					updateButtons(true);
					startServices();
				}
				else {
					displayError();
				}
			}
		});
		
		stopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				updateButtons(false);
				stopServices();
			}
		});
		
		if (phoneID != null) {
			phoneIdText.setText(phoneID);
		}
		if (ipAddr != null)  {
			ipAddrText.setText(ipAddr);
		}
		
		phoneIdText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					phoneID = phoneIdText.getText().toString();
				}
				return false;
			}
		});
	}
	
	// From MainActivity.java
    private void openNotRootDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View view = li.inflate(R.layout.norootview, null);
        new AlertDialog.Builder(this).setTitle("Not Root!").setIcon(R.drawable.warning)
                .setView(view).setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Log.d(MSG_TAG, "Close pressed");
                        // this.application.finish();
                    }
                }).setNeutralButton("Override", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        //Log.d(MSG_TAG, "Override pressed");
                        //installBinaries();
                    }
                }).show();
    }
	
	private void displayError() {
		AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setMessage("Invalid Phone Id and/or IP address.");
        alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                //Do Nothing
            }
        });
        alertbox.show();
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Bundle map = new Bundle();
		map.putString(PHONEID_KEY, phoneID);
		map.putString(IPADDR_KEY, ipAddr);
		map.putBoolean(STATE_KEY, servicesStarted);
		outState.putBundle(ICICLE_KEY, map);
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
	
	private void updateButtons(boolean state) {
		servicesStarted = state;
		startButton.setEnabled(!servicesStarted);
		stopButton.setEnabled(servicesStarted);
	}
	
	private void startServices() {
		startService(new Intent(MainActivity.this, PowerManagement.class));
		startService(new Intent(MainActivity.this, WifiService.class));
		startService(new Intent(MainActivity.this, BluetoothService.class));
		startService(new Intent(MainActivity.this, SchedulerService.class));
		
	}
	
	private void stopServices() {
		stopService(new Intent(MainActivity.this, PowerManagement.class));
		stopService(new Intent(MainActivity.this, WifiService.class));
		stopService(new Intent(MainActivity.this, BluetoothService.class));
		stopService(new Intent(MainActivity.this, SchedulerService.class));	
	}
}
