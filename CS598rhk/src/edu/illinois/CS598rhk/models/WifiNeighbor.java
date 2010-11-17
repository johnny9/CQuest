package edu.illinois.CS598rhk.models;

public class WifiNeighbor extends Neighbor {
	
	@Override
	public byte getHeaderType() {
		return WIFI_NEIGHBOR_HEADER;
	}
	
	@Override
	public byte[] getBytes() {
		
		return null;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof WifiNeighbor) {
			WifiNeighbor neighbor = (WifiNeighbor) o;
			return (name.equals(neighbor.name) && address.equals(neighbor.address));
		}
		return false;
	}
}