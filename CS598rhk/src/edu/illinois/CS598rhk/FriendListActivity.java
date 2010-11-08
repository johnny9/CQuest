package edu.illinois.CS598rhk;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class FriendListActivity extends ListActivity {
	private static final String MSG_TAG = "AdhocClient";
	public static FriendListActivity currentInstance = null;
	private AdhocClient application = null;
	private ArrayAdapter<String> adapter = null;
	
	private static void setCurrent(FriendListActivity current){
		FriendListActivity.currentInstance = current;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.friendlist);
	  FriendListActivity.setCurrent(this);
	  
	  this.application = (AdhocClient)this.getApplication();
	  
	  this.adapter = new ArrayAdapter<String>(this,  
		  		                   R.layout.friendrow, R.id.friendlabel,  
		  		                   this.application.getFriendList());
	  setListAdapter(this.adapter);
	  getListView().setTextFilterEnabled(true);
	}

	public void onListItemClick(ListView parent, View v, int position, long id) {  
		Log.d(MSG_TAG, "Clicked: " + position);
		application.curMessageIndex=-1;
		application.curFriendIndex=position;
		if (application.curFriendIndex != -1) {
			Intent i = new Intent(FriendListActivity.this, MessageActivity.class);
			startActivityForResult(i, 0);
		}
		
	}  
	
    // Handler
    Handler updateHandler = new Handler() {
  	   public void handleMessage(Message msg) {
  		 FriendListActivity.this.updateListView();
  	   }
    };

	protected void updateListView() {
		adapter.notifyDataSetChanged();
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
    		Intent i = new Intent(FriendListActivity.this, SetupActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 1) {
	        Intent i = new Intent(FriendListActivity.this, IncomingEventActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 2) {
	        Intent i = new Intent(FriendListActivity.this, FriendListActivity.class);
	        startActivityForResult(i, 0);
    	}
    	else if (menuItem.getItemId() == 3) {
	        Intent i = new Intent(FriendListActivity.this, MyStatusActivity.class);
	        startActivityForResult(i, 0);
    	}  
    	return supRetVal;
    }    

}
