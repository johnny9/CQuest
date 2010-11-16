package edu.illinois.CS598rhk.models;

public class WifiNeighbor {
	public String name;
	public String ipAddr;
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof WifiNeighbor) {
			WifiNeighbor neighbor = (WifiNeighbor) o;
			return (name.equals(neighbor.name) && ipAddr.equals(neighbor.ipAddr));
		}
		return false;
	}
}