package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class ElectionAcknowledgement implements IBluetoothMessage {
	public int value;
	
	public static IMessageReader newAcknowledgementReader() {
		return new AcknowledgementReader();
	}
	
	private static class AcknowledgementReader implements IMessageReader {

		@Override
		public IBluetoothMessage parse(byte[] message) {
			ElectionAcknowledgement acknowledgement = new ElectionAcknowledgement();
			acknowledgement.unpack(message);
			return acknowledgement;
		}
	}
	
	@Override
	public byte[] pack() {
		return ByteBuffer.allocate(4).putInt(value).array();
	}

	@Override
	public void unpack(byte[] bytes) {
		value = ByteBuffer.wrap(bytes).getInt();
	}

	@Override
	public byte getMessageType() {
		return BluetoothMessage.ACKNOWLEDGE_ELECTION_WINNER;
	}
	
	@Override
	public String toString() {
		return "Wifi Election Results Message Election Acknowledged";
	}
}
