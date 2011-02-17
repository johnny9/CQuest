package edu.illinois.CS598rhk.interfaces;

public interface IBluetoothService {
	public void updateScheduleProgress(long progress);
			
    public void broadcast(IBluetoothMessage message);

	public void hostWifiDiscoveryElection();

	public void startDiscovery();

    
}
