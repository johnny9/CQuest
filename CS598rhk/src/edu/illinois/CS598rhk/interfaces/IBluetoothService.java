package edu.illinois.CS598rhk.interfaces;

public interface IBluetoothService {
	public void updateScheduleProgress(long progress);
	
	public void updateScheduleInfo(String[] schedule);
	
	public void updateNeighborCount(int neighborCount);
	
	public void updateNeighbors();
	
    public void broadcast(IBluetoothMessage message);

	public void hostWifiDiscoveryElection();
    
}
