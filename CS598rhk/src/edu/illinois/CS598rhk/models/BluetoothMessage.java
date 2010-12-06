package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class BluetoothMessage {
	public static final int INDEX_OF_HEADER = 4;
	public static final int HEADER_LENGTH = 5;
	
	public static final byte NEIGHBOR_HEADER = 0;
	public static final byte BLUETOOTH_NEIGHBOR_HEADER = 1;
	public static final byte WIFI_NEIGHBOR_HEADER = 2;
	public static final byte INITIATE_ELECTION_HEADER = 3;
	public static final byte ELECTION_RESPONSE_HEADER = 4;
	public static final byte ELECTION_WINNER_ANNOUNCEMENT_HEADER = 5;
	public static final byte ACKNOWLEDGE_ELECTION_WINNER = 6;
	
	private IBluetoothMessage message;
	private byte[] header;
	
	public BluetoothMessage(IBluetoothMessage message) {
		this.message = message;
		
		header = new byte[5];
		header[4] = message.getMessageType();
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
	
	public static IBluetoothMessage parse(byte[] bytes) {
		IBluetoothMessage parsedMessage = null;
		
		byte[] messageHeader = new byte[HEADER_LENGTH];
		System.arraycopy(bytes, 0, messageHeader, 0, HEADER_LENGTH);
		
		byte messageType = messageHeader[4];
		
		byte[] messageLengthBytes = new byte[4];
		System.arraycopy(bytes, 0, messageLengthBytes, 0, 4);
		int messageLength = ByteBuffer.wrap(messageLengthBytes).getInt();
		
		if (messageLength == bytes.length) {
			byte[] messageBytes = new byte[messageLength];
			System.arraycopy(bytes, HEADER_LENGTH, messageBytes, 0, messageLength - HEADER_LENGTH);
			
			switch(messageType) {
			case NEIGHBOR_HEADER:
				parsedMessage = new Neighbor();
				break;
			case BLUETOOTH_NEIGHBOR_HEADER:
				parsedMessage = new BluetoothNeighbor();
				break;
			case WIFI_NEIGHBOR_HEADER:
				parsedMessage = new WifiNeighbor();
				break;
			case INITIATE_ELECTION_HEADER:
				parsedMessage = new ElectionMessage(messageType);
				break;
			case ELECTION_RESPONSE_HEADER:
				parsedMessage = new ElectionMessage(messageType);
				break;
			case ELECTION_WINNER_ANNOUNCEMENT_HEADER:
				parsedMessage = new ElectionMessage(messageType);
				break;
			case ACKNOWLEDGE_ELECTION_WINNER:
				parsedMessage = new ElectionMessage(messageType);
				break;
			default:
				break;
			}
			
			if (parsedMessage != null) {
				parsedMessage.unpack(messageBytes);
			}
		}
		
		return parsedMessage;
	}
	
	@Override
	public String toString() {
		return message.toString();
	}
}
