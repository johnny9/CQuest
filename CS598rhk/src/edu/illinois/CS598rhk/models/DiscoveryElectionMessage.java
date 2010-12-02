package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class DiscoveryElectionMessage implements IBluetoothMessage {

	public byte messageType;
	byte[] bytes;
	public int value;
	
	public DiscoveryElectionMessage(byte messageType) {
		this.messageType = messageType;
		this.value = -1;
		bytes = new byte[1];
	}
	
	public DiscoveryElectionMessage(int value) {
		this.messageType = BluetoothMessage.WIFI_ELECTION_RESPONSE_HEADER;
		this.value = value;
		bytes = new byte[1];
	}
	
	public DiscoveryElectionMessage(byte[] bytes) {
		this.messageType = BluetoothMessage.WIFI_ELECTION_RESULTS_HEADER;
		this.bytes = bytes;
		this.value = -1;
	}
	
	@Override
	public byte[] pack() {
		byte[] valueBytes = ByteBuffer.allocate(4).putInt(value).array();
		byte[] message = new byte[bytes.length + valueBytes.length];
		
		System.arraycopy(valueBytes, 0, message, 0, valueBytes.length);
		System.arraycopy(bytes, 0, message, valueBytes.length, bytes.length);
		return message;
	}

	@Override
	public void unpack(byte[] bytes) {
		// TODO Auto-generated method stub
	}

	@Override
	public byte getMessageType() {
		return messageType;
	}
}
