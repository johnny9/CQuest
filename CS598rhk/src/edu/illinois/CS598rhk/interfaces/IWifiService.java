package edu.illinois.CS598rhk.interfaces;


public interface IWifiService {
    public void pauseWifiService();
    public void forcedPauseWifiService();
    public void resumeWifiService(long delay);
    public long scheduleTimeRemaining();
}
