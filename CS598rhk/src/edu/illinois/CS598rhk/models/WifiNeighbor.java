package edu.illinois.CS598rhk.models;

public class WifiNeighbor extends Neighbor {
	@Override
	public byte getMessageType() {
		return BluetoothMessage.WIFI_NEIGHBOR_HEADER;
	}
}