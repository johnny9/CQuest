package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class BluetoothMessage {
	public static final int INDEX_OF_HEADER = 4;
	
	public static final byte NEIGHBOR_HEADER = 0;
	public static final byte BLUETOOTH_NEIGHBOR_HEADER = 1;
	public static final byte WIFI_NEIGHBOR_HEADER = 2;
	public static final byte WIFI_ELECTION_HEADER = 3;
	public static final byte WIFI_ELECTION_RESULTS_HEADER = 4;
	public static final byte WIFI_ELECTION_RESPONSE_HEADER = 5;
	
	private IBluetoothMessage message;
	private byte[] header;
	
	public BluetoothMessage(byte messageType, IBluetoothMessage message) {
		this.message = message;
		
		header = new byte[5];
		header[4] = messageType;
	}
	
	public byte[] getMessageWithHeader() {
		byte[] temp = message.pack();
		
		int messageLength = header.length + temp.length;
		byte[] finalMessage = new byte[messageLength];
		
		byte[] messageLengthBytes = ByteBuffer.allocate(4).putInt(messageLength).array();
		System.arraycopy(messageLengthBytes, 0, header, 0, 4);
		
		System.arraycopy(header, 0, finalMessage, 0, header.length);
		System.arraycopy(temp, 0, finalMessage, header.length, temp.length);
		
		return finalMessage;
	}
	
	public byte getMessageType() {
		return message.getMessageType();
	}
	
	public static byte[] stripHeader(byte[] message) {
		byte[] stripped = new byte[message.length - 5];
		System.arraycopy(message, 5, stripped, 0, stripped.length);
		return stripped;
	}
}
