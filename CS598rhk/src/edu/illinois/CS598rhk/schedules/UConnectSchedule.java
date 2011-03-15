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
        int[] schedule = new int[(this.prime * this.prime)+1];
        
        for (int i=0; i<(schedule.length-1); ++i) {
            if (i % this.prime == 0) {
                schedule[i+1] = TRANSMIT_N_LISTEN;
            }
            else if ((i % (this.prime*this.prime)) < (this.prime + 1) / 2) {
            	schedule[i+1] = TRANSMIT_N_LISTEN;
            }
            else {
                schedule[i+1] = DO_NOTHING;
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
