package edu.illinois.CS598rhk.schedules;

import java.util.Random;


public class SearchLightSchedule extends DiscoverSchedule {

    private int prime;
    
    public SearchLightSchedule(int prime) {
        this.prime = prime;
    }
    
    public void setPrimes(int prime)
    {
        this.prime = prime;
    }
    
    @Override
    int[] generateScedule() {
        int[] schedule = new int[this.prime * (this.prime / 2)];
        
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
            int poll = (rand.nextInt(this.prime-1) + 1) + (i * this.prime);
            schedule[poll] = TRANSMIT_N_LISTEN;
        }
        
        return schedule;
    }
}
