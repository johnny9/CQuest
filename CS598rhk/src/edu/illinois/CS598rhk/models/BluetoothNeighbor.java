package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

public class BluetoothNeighbor extends Neighbor {
	public int progress;
	
	@Override
	public byte[] getBytes() {
		byte[] tempName = name.getBytes();
		byte[] tempAddress = address.getBytes();
		
		int msgLength = 5 + tempName.length + tempAddress.length + 1;
		byte[] msgLengthBytes = ByteBuffer.allocate(4).putInt(msgLength).array();
		
		byte[] bytes = new byte[msgLength];
		System.arraycopy(msgLengthBytes, 0, bytes, 0, 4);
		bytes[4] = BLUETOOTH_NEIGHBOR_HEADER_ID;
		System.arraycopy(tempName, 0, bytes, 1, tempName.length);
		System.arraycopy(tempAddress, 0, bytes, 1, tempAddress.length);
		bytes[bytes.length-1] = Integer.valueOf(progress).byteValue();
		
		return bytes;
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
