package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

public class WifiNeighbor extends Neighbor {
	
	@Override
	public byte getHeaderType() {
		return WIFI_NEIGHBOR_HEADER;
	}
	
	@Override
	public byte[] getBytes() {
		byte[] tempName = name.getBytes();
		byte[] tempAddress = address.getBytes();
		
		int msgLength = 5 + tempName.length + tempAddress.length;
		byte[] msgLengthBytes = ByteBuffer.allocate(4).putInt(msgLength).array();
		
		byte[] bytes = new byte[msgLength];
		System.arraycopy(msgLengthBytes, 0, bytes, 0, 4);
		bytes[4] = WIFI_NEIGHBOR_HEADER;
		System.arraycopy(tempName, 0, bytes, INDEX_OF_NAME, tempName.length);
		System.arraycopy(tempAddress, 0, bytes, INDEX_OF_NAME + tempName.length, tempAddress.length);
		
		return bytes;
	}
	
	public static WifiNeighbor parseByteArray(byte[] bytes) {
		WifiNeighbor neighbor = new WifiNeighbor();
		
		byte[] temp = new byte[4];
		System.arraycopy(bytes, INDEX_OF_NAME_LENGTH, temp, 0, 4);
		
		ByteBuffer bb = ByteBuffer.wrap(temp);
		int nameLength = bb.getInt();
		
		neighbor.name = new String(bytes, INDEX_OF_NAME, nameLength);
		int indexOfAddress = INDEX_OF_NAME + nameLength;
		neighbor.address = new String(bytes, indexOfAddress, bytes.length-indexOfAddress);
		
		return neighbor;
	}
	
	
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof WifiNeighbor) {
			WifiNeighbor neighbor = (WifiNeighbor) o;
			return (name.equals(neighbor.name) && address.equals(neighbor.address));
		}
		return false;
	}
}