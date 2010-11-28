package edu.illinois.CS598rhk.schedules;

public class NeverSchedule extends DiscoverSchedule {
	@Override
	public
    int[] generateScedule() {
        // TODO Auto-generated method stub
        int[] schedule = new int[10];
        for(int i = 0; i < schedule.length; i++)
            schedule[i] = DO_NOTHING;
        return schedule;
    }

	@Override
	public int scheduleLength() {
		// TODO Auto-generated method stub
		return 10;
	}
}
