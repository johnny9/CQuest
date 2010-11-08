package edu.illinois.CS598rhk;

public class DiscoSchedule extends DiscoverSchedule {
 
    int prime1;
    int prime2;
   
    public DiscoSchedule(int prime1, int prime2)
    {
        this.prime1 = prime1;
        this.prime2 = prime2;
    }
    
    public void setPrimes(int prime1, int prime2)
    {
        this.prime1 = prime1;
        this.prime2 = prime2;
    }

    @Override
    int[] generateScedule() {
        int[] schedule = new int[this.prime1 * this.prime2];
        
        for(int i = 0; i < schedule.length; i++)
        {
            if((i % prime1) == 0 || (i % prime2) == 0)
                schedule[i] = TRANSMIT_N_LISTEN;
            else
                schedule[i] = DO_NOTHING;
        }
        
        return schedule;
    }
}
