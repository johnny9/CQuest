package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class ElectionInitiation implements IBluetoothMessage {
	public Set<Neighbor> neighbors;
	
	public ElectionInitiation(Set<Neighbor> neighbors) {
		this.neighbors = neighbors;
	}
	
	private ElectionInitiation() {
		neighbors = new HashSet<Neighbor>();
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
		List<byte[]> neighborsAsBytes = new ArrayList<byte[]>();
		int totalBytes = 0;
		
		for( Neighbor neighbor : neighbors) {
			byte[] neighborBytes = neighbor.pack();
			totalBytes += 4 + neighborBytes.length;
			neighborsAsBytes.add(neighborBytes);
		}
		
		byte[] message = new byte[4 + totalBytes];
		
		int currentIndex = 0;
		byte[] numNeighborsBytes = ByteBuffer.allocate(4).putInt(neighbors.size()).array();
		System.arraycopy(numNeighborsBytes, 0, message, currentIndex, 4);
		currentIndex += 4;
		
		ListIterator<byte[]> iterator = neighborsAsBytes.listIterator();
		while(iterator.hasNext()) {
			byte[] neighborBytes = iterator.next();
			
			byte[] size = ByteBuffer.allocate(4).putInt(neighborBytes.length).array();
			System.arraycopy(size, 0, message, currentIndex, 4);
			currentIndex += 4;
			
			System.arraycopy(neighborBytes, 0, message, currentIndex, neighborBytes.length);
			currentIndex += neighborBytes.length;
		}
		
		return message;
	}
	
	@Override
	public void unpack(byte[] bytes) {
		byte[] temp = new byte[4];
        
        int currentIndex = 0;
        System.arraycopy(bytes, currentIndex, temp, 0, 4);
        int numNeighbors = ByteBuffer.wrap(temp).getInt();
        currentIndex += 4;
        
        IMessageReader neighborReader = Neighbor.newNeighborReader();
        for(int i=0; i<numNeighbors; ++i) {
        	 System.arraycopy(bytes, currentIndex, temp, 0, 4);
             int neighborLength = ByteBuffer.wrap(temp).getInt();
             currentIndex += 4;
             
             byte[] neighborBytes = new byte[neighborLength];
             System.arraycopy(bytes, currentIndex, neighborBytes, 0, neighborLength);
             currentIndex += neighborLength;
             
             Neighbor neighbor = (Neighbor) neighborReader.parse(neighborBytes);
             neighbors.add(neighbor);
        }
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
