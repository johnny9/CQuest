package edu.illinois.CS598rhk.schedules;

public abstract class DiscoverSchedule {
    //defined states for each timeslice
    public static final int DO_NOTHING = 0;
    public static final int TRANSMIT_N_LISTEN = 1;
	public static final int TRANSMIT = 2;
	public static final int LISTEN = 3;
	
    public abstract int[] generateScedule();

	public abstract int scheduleLength();
}
