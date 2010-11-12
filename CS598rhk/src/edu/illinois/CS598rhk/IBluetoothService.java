package edu.illinois.CS598rhk;

import java.util.List;

public interface IBluetoothService {
    public List<String> getNeighbors();
    
    public void send(String neighbor, String message);
    
    public void broadcast(String message);
}
