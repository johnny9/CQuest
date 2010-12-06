package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class ElectionMessage implements IBluetoothMessage {
	public byte messageType;
	public String winnerAddress;
	public long delayUntilWinnerStarts;
	public int value;
	
	public ElectionMessage(byte messageType) {
		this.messageType = messageType;
		this.value = -1;
		winnerAddress = "N/A";
		delayUntilWinnerStarts = 0;
	}
	
	public ElectionMessage(int value) {
		this.messageType = BluetoothMessage.ELECTION_RESPONSE_HEADER;
		this.value = value;
		winnerAddress = "N/A";
		delayUntilWinnerStarts = 0;
	}
	
	public ElectionMessage(byte messageType, int value) {
		this.messageType = messageType;
		this.value = value;
		this.winnerAddress = "N/A";
		this.delayUntilWinnerStarts = 0;
	}
	
	public ElectionMessage(String address, long delay) {
		messageType = BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER;
		winnerAddress = address;
		delayUntilWinnerStarts = delay;
		value = -1;
	}
	
	@Override
	public byte[] pack() {
		byte[] valueBytes = ByteBuffer.allocate(4).putInt(value).array();
		byte[] addressBytes = winnerAddress.getBytes();
		byte[] addressLengthBytes = ByteBuffer.allocate(4).putInt(addressBytes.length).array();
		byte[] delayBytes = ByteBuffer.allocate(8).putLong(delayUntilWinnerStarts).array();
		
		int messageLength = valueBytes.length + addressBytes.length + addressLengthBytes.length + delayBytes.length;
		byte[] message = new byte[messageLength];
		
		int currentIndex = 0;
		System.arraycopy(valueBytes, 0, message, currentIndex, valueBytes.length);
		currentIndex += valueBytes.length;
		
		System.arraycopy(addressLengthBytes, 0, message, currentIndex, addressLengthBytes.length);
		currentIndex += addressLengthBytes.length;
		
		System.arraycopy(addressBytes, 0, message, currentIndex, addressBytes.length);
		currentIndex += addressBytes.length;
		
		System.arraycopy(delayBytes, 0, message, currentIndex, delayBytes.length);
		
		return message;
	}

	@Override
	public void unpack(byte[] bytes) {
		byte[] valueBytes = new byte[4];
		System.arraycopy(bytes, 0, valueBytes, 0, 4);
		value = ByteBuffer.wrap(valueBytes).getInt();
		
		System.arraycopy(bytes, 4, valueBytes, 0, 4);
		int addressLength = ByteBuffer.wrap(valueBytes).getInt();
		
		byte[] addressBytes = new byte[addressLength];
		System.arraycopy(bytes, 8, addressBytes, 0, addressLength);
		winnerAddress = new String(addressBytes);
		
		valueBytes = new byte[8];
		System.arraycopy(bytes, 8 + addressLength, valueBytes, 0, 8);
		delayUntilWinnerStarts = ByteBuffer.wrap(valueBytes).getLong();
	}

	@Override
	public byte getMessageType() {
		return messageType;
	}
	
	@Override 
	public String toString() {
		String prettyString = "Invalid Message Type for DiscoveryElectionMessage";
		
		switch(messageType) {
		case BluetoothMessage.INITIATE_ELECTION_HEADER:
			prettyString = "Wifi Election Initiation Message";
			break;
		case BluetoothMessage.ELECTION_RESPONSE_HEADER:
			prettyString = "Wifi Election Response Message with value " + String.valueOf(value);
			break;
		case BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER:
			prettyString = "Wifi Election Results Message with winner " + winnerAddress + " and delay " + String.valueOf(delayUntilWinnerStarts);
			break;
		case BluetoothMessage.ACKNOWLEDGE_ELECTION_WINNER:
			prettyString = "Wifi Election Results Message Election Acknowledged";
			break;
		default:
			break;
		}
		
		return prettyString;
	}
}
