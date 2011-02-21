package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class BluetoothMessage {
	public static final byte NEIGHBOR_HEADER = 2;
	public static final byte INITIATE_ELECTION_HEADER = 3;
	public static final byte ELECTION_RESPONSE_HEADER = 4;
	public static final byte ELECTION_WINNER_ANNOUNCEMENT_HEADER = 5;
	public static final byte ACKNOWLEDGE_ELECTION_WINNER = 6;
	
	private static final Map<Byte, IMessageReader> messageReaders;
	
	static {
		final Map<Byte, IMessageReader> temp = new HashMap<Byte, IMessageReader>();
		
		temp.put(INITIATE_ELECTION_HEADER, ElectionInitiation.newInitiationReader());
		temp.put(ELECTION_RESPONSE_HEADER, ElectionResponse.newResponseReader());
		temp.put(ELECTION_WINNER_ANNOUNCEMENT_HEADER, ElectionResult.newResultReader());
		temp.put(ACKNOWLEDGE_ELECTION_WINNER, ElectionAcknowledgement.newAcknowledgementReader());
		
		messageReaders = Collections.unmodifiableMap(temp);
	}
	
	private byte[] header;
	private static final int HEADER_LENGTH = 5;
	private static final int INDEX_OF_MESSAGE_TYPE = 4;
	
	private IBluetoothMessage message;
	
	public BluetoothMessage(IBluetoothMessage message) {
		this.message = message;
		
		header = new byte[5];
		header[INDEX_OF_MESSAGE_TYPE] = message.getMessageType();
	}
	
	public byte[] getMessageWithHeader() {
		byte[] temp = message.pack();
		
		int messageLength = HEADER_LENGTH + temp.length;
		byte[] finalMessage = new byte[messageLength];
		
		byte[] messageLengthBytes = ByteBuffer.allocate(4).putInt(messageLength).array();
		System.arraycopy(messageLengthBytes, 0, header, 0, 4);
		
		System.arraycopy(header, 0, finalMessage, 0, HEADER_LENGTH);
		System.arraycopy(temp, 0, finalMessage, HEADER_LENGTH, temp.length);
		
		return finalMessage;
	}
	
	public byte getMessageType() {
		return message.getMessageType();
	}
	
	public static byte[] stripHeader(byte[] message) {
		byte[] stripped = new byte[message.length - HEADER_LENGTH];
		System.arraycopy(message, HEADER_LENGTH, stripped, 0, stripped.length);
		return stripped;
	}
	
	public static IBluetoothMessage parse(byte[] bytes) {
		IBluetoothMessage parsedMessage = null;
		
		byte[] messageHeader = new byte[HEADER_LENGTH];
		System.arraycopy(bytes, 0, messageHeader, 0, HEADER_LENGTH);
		
		byte[] messageLengthBytes = new byte[4];
		System.arraycopy(bytes, 0, messageLengthBytes, 0, 4);
		int messageLength = ByteBuffer.wrap(messageLengthBytes).getInt();
		
		if (messageLength == bytes.length) {
			byte[] messageBytes = stripHeader(bytes);
			
			byte messageType = messageHeader[INDEX_OF_MESSAGE_TYPE];
			parsedMessage = messageReaders.get(messageType).parse(messageBytes);
		}
		return parsedMessage;
	}
	
	@Override
	public String toString() {
		return message.toString();
	}
}