package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BluetoothNeighbor extends Neighbor {
	public long progress;
	public int neighborCount;
	public String[] schedule;

	// byte[] format [0:3] nameLength, [4:n] name, [n+1,n+4] addressLength,
	// [n+5:m] address,
	// [m+1:4] neighborCount, [m+5:m+13] progress, [m+14:?] schedule (\t
	// delimeter)
	public BluetoothNeighbor(String[] schedule) {
		this.schedule = schedule;
	}

	public BluetoothNeighbor() {
		schedule = new String[0];
	}

	@Override
	public byte[] pack() {
		byte[] tempBytes = super.pack();

		int scheduleLength = 0;
		for (String neighbor : schedule)
			scheduleLength += neighbor.getBytes().length + 1;

		byte[] bytes = new byte[tempBytes.length + 12 + scheduleLength];

		System.arraycopy(tempBytes, 0, bytes, 0, tempBytes.length);
		int currentIndex = tempBytes.length;

		byte[] neighborCountBytes = ByteBuffer.allocate(4)
				.putInt(neighborCount).array();
		System.arraycopy(neighborCountBytes, 0, bytes, currentIndex, 4);
		currentIndex += 4;

		byte[] progressBytes = ByteBuffer.allocate(8).putLong(progress).array();
		System.arraycopy(progressBytes, 0, bytes, currentIndex, 8);
		currentIndex += 8;

		for (String neighbor : schedule) {
			System.arraycopy((neighbor + '\t').getBytes(), 0, bytes,
					currentIndex, (neighbor + '\t').getBytes().length);
			currentIndex += (neighbor + '\t').getBytes().length;
		}

		return bytes;
	}

	@Override
	public void unpack(byte[] bytes) {
		super.unpack(bytes);

		// This is ugly ... it is redoing work already done ... not sure of good
		// fix
		byte[] temp = new byte[4];
		System.arraycopy(bytes, 0, temp, 0, 4);
		int nameLength = ByteBuffer.wrap(temp).getInt();

		System.arraycopy(bytes, 4 + nameLength, temp, 0, 4);
		int addressLength = ByteBuffer.wrap(temp).getInt();
		int indexOfAddress = 4 + nameLength + 4;
		// end of ugliness

		System.arraycopy(bytes, indexOfAddress + addressLength, temp, 0, 4);
		neighborCount = ByteBuffer.wrap(temp).getInt();

		temp = new byte[8];
		System.arraycopy(bytes, indexOfAddress + addressLength + 4, temp, 0, 8);
		progress = ByteBuffer.wrap(temp).getLong();

		// parse the schedule names
		ArrayList<String> scheduleList = new ArrayList<String>();
		int currentIndex = indexOfAddress + addressLength + 12;
		String newName = "";
		while (currentIndex < bytes.length) {
			if (bytes[currentIndex] == '\t')
			{
				scheduleList.add(newName);
				newName = "";
			} else {
				newName += (char) bytes[currentIndex];
			}
			
			currentIndex++;
		}
		schedule = scheduleList.toArray(schedule);

	}

	@Override
	public byte getMessageType() {
		return BluetoothMessage.BLUETOOTH_NEIGHBOR_HEADER;
	}

	@Override
	public String toString() {
		String prettyString = super.toString();
		prettyString += "\n\tProgress: " + String.valueOf(progress)
				+ "\n\tNeighborCount: " + String.valueOf(neighborCount);
		return prettyString;
	}
}
