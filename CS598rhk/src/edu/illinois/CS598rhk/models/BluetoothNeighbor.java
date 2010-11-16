package edu.illinois.CS598rhk.models;

public class BluetoothNeighbor {
	public String name;
	public String macAddr;
	public int progress;
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof BluetoothNeighbor) {
			BluetoothNeighbor neighbor = (BluetoothNeighbor) o;
			return (name.equals(neighbor.name) && macAddr.equals(neighbor.macAddr));
		}
		return false;
	}
}
