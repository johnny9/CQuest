package edu.illinois.CS598rhk.models;

public abstract class Neighbor {
	public static final byte INDEX_OF_HEADER = 4;
	
	protected static final int INDEX_OF_NAME_LENGTH = 5;
	protected static final int INDEX_OF_NAME = 9;
	
	public static final byte BLUETOOTH_NEIGHBOR_HEADER = 0;
	public static final byte WIFI_NEIGHBOR_HEADER = 1;
	
	public String name;
	public String address;
	
	public abstract byte[] getBytes();
	
	public abstract byte getHeaderType();
}
