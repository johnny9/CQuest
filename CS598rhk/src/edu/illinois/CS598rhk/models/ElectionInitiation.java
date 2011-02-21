package edu.illinois.CS598rhk.models;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class ElectionInitiation implements IBluetoothMessage {

	@Override
	public byte[] pack() {
		return new byte[1];
	}
	
	@Override
	public void unpack(byte[] bytes) {
		// Do nothing
	}

	@Override
	public byte getMessageType() {
		return BluetoothMessage.INITIATE_ELECTION_HEADER;
	}

	@Override
	public String toString() {
		return "Wifi Election Initiation Message";
	}
}
