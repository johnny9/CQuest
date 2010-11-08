package edu.illinois.CS598rhk;

public class AlwaysSchedule extends DiscoverSchedule {

    public AlwaysSchedule() {
        
    }
    
    @Override
    int[] generateScedule() {
        // TODO Auto-generated method stub
        int[] schedule = new int[10];
        for(int i = 0; i < schedule.length; i++)
            schedule[i] = TRANSMIT_N_LISTEN;
        return schedule;
    }

}
