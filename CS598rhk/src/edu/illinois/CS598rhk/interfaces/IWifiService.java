package edu.illinois.CS598rhk.interfaces;

import java.util.List;

public interface IWifiService {
    public void pauseWifiService();
    public void forcedPauseWifiService();
    public void resumeWifiService(long delay);
    public long scheduleTimeRemaining();
}
