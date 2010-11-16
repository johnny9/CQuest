package edu.illinois.CS598rhk.interfaces;

import edu.illinois.CS598rhk.models.BluetoothNeighbor;

public interface IBluetoothService {
	public void updateContactInfo(BluetoothNeighbor contactInfo);
	
	public void updateNeighbors();
	
    public void broadcast(String message);
}
