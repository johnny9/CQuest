package edu.illinois.CS598rhk.schedules;

import java.util.Random;


public class UConnectSchedule extends DiscoverSchedule {

    private int prime;
    
    public UConnectSchedule(int period) {
        this.prime = period;
    }
    
    public void setPeriod(int period)
    {
        this.prime = period;
    }
    
    @Override
	public int[] generateScedule() {
        int[] schedule = new int[this.prime * this.prime];
        
        for (int i=0; i<schedule.length; ++i) {
            if (i % this.prime == 0) {
                schedule[i] = TRANSMIT_N_LISTEN;
            }
            else if ((i % this.prime*this.prime) < (this.prime + 1) / 2) {
            	schedule[i] = TRANSMIT_N_LISTEN;
            }
            else {
                schedule[i] = DO_NOTHING;
            }
        }
        
        return schedule;
    }

	public int scheduleLength() {
		return (int) Math.ceil(prime*prime);
	}
	

	@Override
	public String toString() {
		return "UConnect Schedule " + " prime=" + prime;
	}
}
