package edu.illinois.CS598rhk.models;

import java.sql.Time;

public class NeighborMetaData {
	public static final String WIFI_NETWORK = "wifi_network";
	public static final String BLUETOOTH_NETWORK = "bluetooth_network";
	public static final String BLUETOOTH_DISCOVERY = "bluetooth discovery";
	public Time lastSeen;
	public boolean directContact;
	public String networkType;
	
	public NeighborMetaData(Time discovered, boolean directContact, String network) {
		this.lastSeen = discovered;
		this.directContact = directContact;
	}
}
