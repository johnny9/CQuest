package edu.illinois.CS598rhk.models;

import java.sql.Time;

public class NeighborMetaData {
	public Time lastSeen;
	public boolean directContact;
	
	public NeighborMetaData(Time discovered, boolean directContact) {
		this.lastSeen = discovered;
		this.directContact = directContact;
	}
}
