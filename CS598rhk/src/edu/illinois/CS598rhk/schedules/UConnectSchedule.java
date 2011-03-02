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
    
    //TODO: Make it so Uconnect is Uconnect and not searchlight
    @Override
	public int[] generateScedule() {
        int[] schedule = new int[this.prime * this.prime];
        
        for (int i=0; i<schedule.length; ++i) {
            if (i % this.prime == 0) {
                schedule[i] = TRANSMIT_N_LISTEN;
            }
            else {
                schedule[i] = DO_NOTHING;
            }
        }
        
        Random rand = new Random();
        for (int i=0; i<(this.prime / 2); ++i) {
            //int poll = (rand.nextInt(this.prime-1) + 1) + (i * this.prime);
        	// ^ whatever this math is seems to be the only difference
            schedule[i] = TRANSMIT_N_LISTEN;
        }
        
        return schedule;
    }

	public int scheduleLength() {
		return (int) Math.ceil(prime*prime/2);
	}
	

	@Override
	public String toString() {
		return "Searchlight Schedule " + " t=" + prime;
	}
}
