package edu.illinois.CS598rhk;


public class QuorumSchedule extends DiscoverSchedule {
    int m;
    int row;
    int col;
    
    public QuorumSchedule(int m) {
        this.m = m;
        this.row = (int) Math.round(Math.random() * m);
        this.col = (int) Math.round(Math.random() * m);
    }
    
    public QuorumSchedule(int m , int row, int col) {
        this.m = m;
        this.row = row;
        this.col = col;
    }

    @Override
    int[] generateScedule() {
        int[] schedule = new int[m * m];
        
        for(int i = 0; i < m*m; i++) {
            schedule[i] = DO_NOTHING;
            
            if(i % m == col) {
                schedule[i] = TRANSMIT;
            }
            
            if(i - row * m < m && i - row * m >= 0) {
                if(schedule[i] == TRANSMIT)
                    schedule[i] = TRANSMIT_N_LISTEN;
                else
                    schedule[i] = LISTEN;
            }
        }
        
        return schedule;
    }
    
    
}
