/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *
 *  Copyright (c) 2009 by Harald Mueller and Seth Lemons.
 */

package edu.illinois.CS598rhk;

import java.util.Hashtable;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class SetupActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	private AdhocClient application = null;
	
	public static final String MSG_TAG = "adhocClient -> SetupActivity";

    private String currentSSID;
    private String currentChannel;
    private String currentPowermode;
    
    private Hashtable<String,String> tiWlanConf = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Init Application
        this.application = (AdhocClient)this.getApplication();
        
        this.tiWlanConf = application.coretask.getTiWlanConf();
        addPreferencesFromResource(R.layout.setupview); 
    }
	
    @Override
    protected void onResume() {
    	Log.d(MSG_TAG, "Calling onResume()");
    	super.onResume();
    	getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        this.updatePreferences();
    }
    
    @Override
    protected void onPause() {
    	Log.d(MSG_TAG, "Calling onPause()");
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);   
    }

    
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	String message;
    	if (key.equals("ssidpref")) {
    		String newSSID = sharedPreferences.getString("ssidpref", "G1AdhocClient");
    		if (this.currentSSID.equals(newSSID) == false) {
    			if (this.validateSSID(newSSID)) {
	    			if (application.coretask.writeTiWlanConf("dot11DesiredSSID", newSSID)) {
	    				this.currentSSID = newSSID;
	    				message = "SSID changed to '"+newSSID+"'.";
		    			//this.application.restartService();
	    				this.displayToastMessage(message);
	    			}
	    			else {
	    				this.application.preferenceEditor.putString("ssidpref", this.currentSSID);
	    				this.application.preferenceEditor.commit();
	    				this.displayToastMessage("Couldn't change ssid to '"+newSSID+"'!");
	    			}
    			}
    		}
    	}
    	else if (key.equals("channelpref")) {
    		String newChannel = sharedPreferences.getString("channelpref", "6");
    		if (this.currentChannel.equals(newChannel) == false) {
    			if (application.coretask.writeTiWlanConf("dot11DesiredChannel", newChannel)) {
    				this.currentChannel = newChannel;
    				message = "Channel changed to '"+newChannel+"'.";
	    			//this.application.restartService();
    				this.displayToastMessage(message);
    			}
    			else {
    				this.application.preferenceEditor.putString("channelpref", this.currentChannel);
    				this.application.preferenceEditor.commit();
    				this.displayToastMessage("Couldn't change channel to  '"+newChannel+"'!");
    			}
    		}
    	}
    	else if (key.equals("powermodepref")) {
    		String newPowermode = sharedPreferences.getString("powermodepref", "0");
    		if (this.currentPowermode.equals(newPowermode) == false) {
    			if (application.coretask.writeTiWlanConf("dot11PowerMode", newPowermode)) {
    				this.currentPowermode = newPowermode;
    				message = "Powermode changed to '"+getResources().getStringArray(R.array.powermodenames)[new Integer(newPowermode)]+"'.";
	    			//this.application.restartService();
    				this.displayToastMessage(message);
				}
    			else {
    				this.application.preferenceEditor.putString("powermodepref", this.currentChannel);
    				this.application.preferenceEditor.commit();
    				this.displayToastMessage("Couldn't change powermode to  '"+newPowermode+"'!");
    			}
    		}
    	}    	

    }
    
    private void updatePreferences() {
    	// SSID
        this.currentSSID = this.getTiWlanConfValue("dot11DesiredSSID");
        this.application.preferenceEditor.putString("ssidpref", this.currentSSID);
        // Channel
        this.currentChannel = this.getTiWlanConfValue("dot11DesiredChannel");
        this.application.preferenceEditor.putString("channelpref", this.currentChannel);
        // Powermode
        this.currentPowermode = this.getTiWlanConfValue("dot11PowerMode");
        this.application.preferenceEditor.putString("powermodepref", this.currentPowermode);
        this.application.preferenceEditor.commit();  
    }
    
    private String getTiWlanConfValue(String name) {
    	if (this.tiWlanConf != null && this.tiWlanConf.containsKey(name)) {
    		if (this.tiWlanConf.get(name) != null && this.tiWlanConf.get(name).length() > 0) {
    			return this.tiWlanConf.get(name);
    		}
    	}
    	SetupActivity.this.displayToastMessage("Oooooops ... tiwlan.conf does not exist or config-parameter '"+name+"' is not available!");
    	return "";
    }
    
    public boolean validateSSID(String newSSID){
    	if (newSSID.contains("#") || newSSID.contains("`")){
    		SetupActivity.this.displayToastMessage("New SSID cannot contain '#' or '`'!");
    		return false;
    	}
		return true;
    }

    public void displayToastMessage(String message) {
		Toast.makeText(this, message, Toast.LENGTH_LONG).show();
	} 
}