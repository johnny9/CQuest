package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class ElectionResponse implements IBluetoothMessage {
	public int value;
	
	public ElectionResponse(int value) {
		this.value = value;
	}
	
	public ElectionResponse() {
		// Do nothing
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
		return BluetoothMessage.ELECTION_RESPONSE_HEADER;
	}
	
	@Override
	public String toString() {
		return "Wifi Election Response Message with value " + String.valueOf(value);
	}
}
