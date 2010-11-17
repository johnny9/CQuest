package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

public class BluetoothNeighbor extends Neighbor {
	private static final int INDEX_OF_NAME_LENGTH = 5;
	private static final int INDEX_OF_NAME = 9;
	
	public int progress;
	
	public byte getHeaderType() {
		return BLUETOOTH_NEIGHBOR_HEADER;
	}
	
	// byte[] format [0:3] messageLength, [4] messageType, [5:n] name, [n+1,m] address, [m+1] progress
	
	@Override
	public byte[] getBytes() {
		byte[] tempName = name.getBytes();
		byte[] tempAddress = address.getBytes();
		
		int msgLength = 5 + tempName.length + tempAddress.length + 1;
		byte[] msgLengthBytes = ByteBuffer.allocate(4).putInt(msgLength).array();
		
		byte[] bytes = new byte[msgLength];
		System.arraycopy(msgLengthBytes, 0, bytes, 0, 4);
		bytes[4] = BLUETOOTH_NEIGHBOR_HEADER;
		System.arraycopy(tempName, 0, bytes, INDEX_OF_NAME, tempName.length);
		System.arraycopy(tempAddress, 0, bytes, INDEX_OF_NAME + tempName.length, tempAddress.length);
		bytes[bytes.length-1] = Integer.valueOf(progress).byteValue();
		
		return bytes;
	}
	
	public static BluetoothNeighbor parseByteArray(byte[] bytes) {
		BluetoothNeighbor neighbor = new BluetoothNeighbor();
		
		byte[] temp = new byte[4];
		System.arraycopy(bytes, INDEX_OF_NAME_LENGTH, temp, 0, 4);
		
		ByteBuffer bb = ByteBuffer.wrap(temp);
		int nameLength = bb.getInt();
		
		neighbor.name = new String(bytes, INDEX_OF_NAME, nameLength);
		int indexOfAddress = INDEX_OF_NAME + nameLength;
		neighbor.address = new String(bytes, indexOfAddress, bytes.length-indexOfAddress);
		neighbor.progress = bytes[bytes.length-1];
		
		return neighbor;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof BluetoothNeighbor) {
			BluetoothNeighbor neighbor = (BluetoothNeighbor) o;
			return (name.equals(neighbor.name) && address.equals(neighbor.address));
		}
		return false;
	}
}
