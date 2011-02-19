package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class ElectionResult implements IBluetoothMessage {
	public String winnerAddress;
	public long delayUntilWinnerStarts;
	
	public ElectionResult(String address, long delay) {
		winnerAddress = address;
		delayUntilWinnerStarts = delay;
	}
	
	private ElectionResult() {
		// Do nothing
	}
	
	public static ElectionResult parse(byte[] message) {
		ElectionResult result = new ElectionResult();
		result.unpack(message);
		return result;
	}
	
	@Override
	public byte[] pack() {
		byte[] addressBytes = winnerAddress.getBytes();
		byte[] addressLengthBytes = ByteBuffer.allocate(4).putInt(addressBytes.length).array();
		byte[] delayBytes = ByteBuffer.allocate(8).putLong(delayUntilWinnerStarts).array();
		
		int messageLength = addressLengthBytes.length + addressBytes.length + delayBytes.length;
		byte[] message = new byte[messageLength];
		
		int currentIndex = 0;
		System.arraycopy(addressLengthBytes, 0, message, currentIndex, addressLengthBytes.length);
		currentIndex += addressLengthBytes.length;
		
		System.arraycopy(addressBytes, 0, message, currentIndex, addressBytes.length);
		currentIndex += addressBytes.length;
		
		System.arraycopy(delayBytes, 0, message, currentIndex, delayBytes.length);
		
		return message;
	}

	@Override
	public void unpack(byte[] bytes) {
		byte[] temp = new byte[8];
		
		System.arraycopy(bytes, 0, temp, 0, 4);
		int addressLength = ByteBuffer.wrap(temp).getInt();
		
		byte[] addressBytes = new byte[addressLength];
		System.arraycopy(bytes, 4, addressBytes, 0, addressLength);
		winnerAddress = new String(addressBytes);

		System.arraycopy(bytes, 4 + addressLength, temp, 0, 8);
		delayUntilWinnerStarts = ByteBuffer.wrap(temp).getLong();
	}

	@Override
	public byte getMessageType() {
		return BluetoothMessage.ELECTION_WINNER_ANNOUNCEMENT_HEADER;
	}
	
	@Override
	public String toString() {
		return "Wifi Election Results Message with winner " + winnerAddress + " and delay " + String.valueOf(delayUntilWinnerStarts);
	}
}
