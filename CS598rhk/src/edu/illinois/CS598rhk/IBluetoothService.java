package edu.illinois.CS598rhk;

import java.util.List;

public interface IBluetoothService {
    public void send(String neighbor, String message);
    
    public void broadcast(List<String> addrs, String message);
}
