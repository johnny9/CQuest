package edu.illinois.CS598rhk.models;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class DiscoveryElectionMessage implements IBluetoothMessage {

	public byte messageType;
	byte[] bytes;
	public int value;
	
	public DiscoveryElectionMessage(byte messageType) {
		this.messageType = messageType;
		this.value = -1;
	}
	
	public DiscoveryElectionMessage(int value) {
		this.messageType = BluetoothMessage.WIFI_ELECTION_RESPONSE_HEADER;
		this.value = value;
	}
	
	public DiscoveryElectionMessage(byte[] bytes) {
		this.messageType = BluetoothMessage.WIFI_ELECTION_RESULTS_HEADER;
		this.bytes = bytes;
		this.value = -1;
	}
	
	@Override
	public byte[] pack() {
		return new byte[1];
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
