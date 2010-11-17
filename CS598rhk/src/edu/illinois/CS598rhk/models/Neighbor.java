package edu.illinois.CS598rhk.models;

public abstract class Neighbor {
	public static final byte BLUETOOTH_NEIGHBOR_HEADER_ID = 0;
	public static final byte WIFI_NEIGHBOR_HEADER_ID = 1;
	
	public String name;
	public String address;
	
	public abstract byte[] getBytes();
}
