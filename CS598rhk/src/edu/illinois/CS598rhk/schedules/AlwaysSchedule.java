package edu.illinois.CS598rhk.schedules;

public class AlwaysSchedule extends DiscoverSchedule {

    public AlwaysSchedule() {
        
    }
    
    @Override
	public
    int[] generateScedule() {
        // TODO Auto-generated method stub
        int[] schedule = new int[10];
        for(int i = 0; i < schedule.length; i++)
            schedule[i] = TRANSMIT_N_LISTEN;
        return schedule;
    }

	@Override
	public int scheduleLength() {
		// TODO Auto-generated method stub
		return 10;
	}
	
	@Override
	public String toString() {
		return "Always Schedule";
	}

}
