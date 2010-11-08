package edu.illinois.CS598rhk;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MyStatusActivity extends Activity {
    private static final String MSG_TAG = "AdhocClient";
	public static MyStatusActivity currentInstance = null;
	private AdhocClient application = null;
	private Button buttonUpdate = null;
	private Button buttonStartService = null;
	private Button buttonStopService = null;
	private EditText myStatus = null;
	private TextView statusField = null;
	
	private static void setCurrent(MyStatusActivity current){
		MyStatusActivity.currentInstance = current;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.mystatus);

	  MyStatusActivity.setCurrent(this);
	  
	  this.application = (AdhocClient)this.getApplication();
	  
	  this.myStatus=(EditText)findViewById(R.id.MyStatus);
	  //this.myStatus.setText(this.application.getMyStatusMsg());
	  
	  this.statusField=(TextView)findViewById(R.id.StatusField);
	  
	  this.buttonUpdate = (Button)findViewById(R.id.UpdateStatus);
      this.buttonUpdate.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				//application.setMyStatusMsg(myStatus.getText().toString());
				Log.d(MSG_TAG, "Update pressed ...");
			}
		});
	  
      this.buttonStartService = (Button)findViewById(R.id.StartService);
      this.buttonStartService.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "Start Service pressed ...");
				//application.startService();
				buttonStartService.setVisibility(View.GONE);
				buttonStopService.setVisibility(View.VISIBLE);
			}
		});

      this.buttonStopService = (Button)findViewById(R.id.StopService);
      this.buttonStopService.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Log.d(MSG_TAG, "Stop Service pressed ...");
				//application.stopService();
				buttonStartService.setVisibility(View.VISIBLE);
				buttonStopService.setVisibility(View.GONE);
			}
		});
      if (application.serviceStarted) {
    	  this.buttonStartService.setVisibility(View.GONE);
    	  this.buttonStopService.setVisibility(View.VISIBLE);
      }
      else {
    	  this.buttonStartService.setVisibility(View.VISIBLE);
    	  this.buttonStopService.setVisibility(View.GONE);
      }
      
      updateStatus();
	}

  @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	boolean supRetVal = super.onCreateOptionsMenu(menu);
    	SubMenu setup = menu.addSubMenu(0, 0, 0, "Setup");
    	setup.setIcon(R.drawable.setup);
    	SubMenu events = menu.addSubMenu(0, 1, 0, "Incoming Events");
    	events.setIcon(R.drawable.log);
    	SubMenu friends = menu.addSubMenu(0, 2, 0, "Friend List");
    	friends.setIcon(R.drawable.about);
    	SubMenu mystatus = menu.addSubMenu(0, 3, 0, "My Status");
    	mystatus.setIcon(R.drawable.install);    	
    	return supRetVal;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
    	boolean supRetVal = super.onOptionsItemSelected(menuItem);
    	Log.d(MSG_TAG, "Menuitem:getId  -  "+menuItem.getItemId()); 
    	if (menuItem.getItemId() == 0) {
    		Intent i = new Intent(MyStatusActivity.this, SetupActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 1) {
	        Intent i = new Intent(MyStatusActivity.this, IncomingEventActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 2) {
	        Intent i = new Intent(MyStatusActivity.this, FriendListActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 3) {
	        Intent i = new Intent(MyStatusActivity.this, MyStatusActivity.class);
	        startActivityForResult(i, 0);
    	}  
    	return supRetVal;
    }    

    // Handler
    Handler updateHandler = new Handler() {
  	   public void handleMessage(Message msg) {
  		 MyStatusActivity.this.updateStatus();
  	   }
    };

	protected void updateStatus() {
		String newMsg="";
		/*
		switch (this.application.getCurrentState()) {
    	case NOT_RUNNING:
    		newMsg="Stopped";
    		break;
    	case ADHOC_SERVER:
    		newMsg="DHCP Server";
    		break;
    	case ADHOC_CLIENT:
    		newMsg="DHCP Client";
    		break;
    	case ACCESS_POINT:
    		newMsg="Access Point";
    		break;
		}*/
		
		/*this.statusField.setText(newMsg + ": " + 
				Integer.toString(this.application.numActiveFriends()) +
				" Active Friends");*/
	}
}
