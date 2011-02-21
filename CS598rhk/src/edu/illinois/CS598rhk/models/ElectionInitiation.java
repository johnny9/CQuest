package edu.illinois.CS598rhk.models;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class ElectionInitiation implements IBluetoothMessage {

	public static IMessageReader newInitiationReader() {
		return new InitiationReader();
	}
	
	private static class InitiationReader implements IMessageReader {
		@Override
		public IBluetoothMessage parse(byte[] message) {
			return new ElectionInitiation();
		}
	}
	
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
