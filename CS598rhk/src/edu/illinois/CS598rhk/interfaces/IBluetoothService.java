package edu.illinois.CS598rhk.interfaces;

public interface IBluetoothService {
	public void updateScheduleProgress(int progress);
	
	public void updateNeighbors();
	
    public void broadcast(String message);
}
