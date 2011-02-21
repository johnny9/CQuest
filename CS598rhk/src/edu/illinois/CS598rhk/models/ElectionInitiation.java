package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;
import java.util.List;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class ElectionInitiation implements IBluetoothMessage {
	private List<Neighbor> neighbors;
	
	
	public ElectionInitiation(List<Neighbor> neighbors) {
		this.neighbors = neighbors;
	}
	
	private ElectionInitiation() {
		// Do nothing
	}
	
	public static IMessageReader newInitiationReader() {
		return new InitiationReader();
	}
	
	private static class InitiationReader implements IMessageReader {
		@Override
		public IBluetoothMessage parse(byte[] message) {
			return new ElectionInitiation();
		}
	}
	
	@Override
	public byte[] pack() {
		// [# neighbors (4)] [neighbor mac 0] [neighbor mac 1] ... [neighbor mac n]
		
		byte[] numNeighborsBytes = ByteBuffer.allocate(4).putInt(neighbors.size()).array();
		return new byte[1];
	}
	
	@Override
	public void unpack(byte[] bytes) {
		// Do nothing
	}

	@Override
	public byte getMessageType() {
		return BluetoothMessage.INITIATE_ELECTION_HEADER;
	}

	@Override
	public String toString() {
		return "Wifi Election Initiation Message";
	}
}
