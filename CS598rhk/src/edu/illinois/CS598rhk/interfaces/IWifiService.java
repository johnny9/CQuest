package edu.illinois.CS598rhk.interfaces;

import java.util.List;

public interface IWifiService {
    public void pauseWifiService();
    public void resumeWifiService(long delay);
    public long scheduleTimeRemaining();
    public void broadcast(List<String> addrs, String message);
}
