package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class BluetoothMessage {
	private static final int HEADER_LENGTH = 5;
	private static final int INDEX_OF_MESSAGE_TYPE = 4;
	public static final byte WIFI_NEIGHBOR_HEADER = 2;
	public static final byte INITIATE_ELECTION_HEADER = 3;
	public static final byte ELECTION_RESPONSE_HEADER = 4;
	public static final byte ELECTION_WINNER_ANNOUNCEMENT_HEADER = 5;
	public static final byte ACKNOWLEDGE_ELECTION_WINNER = 6;
	
	private static final Map<Byte, IMessageReader> messageReaders;
	
	static {
		final Map<Byte, IMessageReader> temp = new HashMap<Byte, IMessageReader>();
		
		temp.put(INITIATE_ELECTION_HEADER, MessageReaders.newInitiationReader());
		temp.put(ELECTION_RESPONSE_HEADER, MessageReaders.newResponseReader());
		temp.put(ELECTION_WINNER_ANNOUNCEMENT_HEADER, MessageReaders.newResultReader());
		temp.put(ACKNOWLEDGE_ELECTION_WINNER, MessageReaders.newAcknowledgementReader());
		
		messageReaders = Collections.unmodifiableMap(temp);
	}
	
	private IBluetoothMessage message;
	private byte[] header;
	
	public BluetoothMessage(IBluetoothMessage message) {
		this.message = message;
		
		header = new byte[5];
		header[INDEX_OF_MESSAGE_TYPE] = message.getMessageType();
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
			byte[] messageBytes = new byte[messageLength- HEADER_LENGTH];
			System.arraycopy(bytes, HEADER_LENGTH, messageBytes, 0, messageLength - HEADER_LENGTH);
			
			parsedMessage = messageReaders.get(messageType).parse(messageBytes);
			
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