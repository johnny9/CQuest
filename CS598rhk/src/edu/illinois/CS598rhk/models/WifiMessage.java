package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class WifiMessage implements IBluetoothMessage {
	public static final byte WIFI_MESSAGE = 0;
	
	public Neighbor deviceInfo;
	public List<String> btDeviceAddrs;
	
	public WifiMessage(Neighbor device, List<String> btNeighborAddrs) {
		this.deviceInfo = device;
		this.btDeviceAddrs = btNeighborAddrs;
	}
	
	private WifiMessage() {
		// Do nothing
	}
	
	public IMessageReader newMessageReader() {
		return new MessageReader();
	}
	
	private class MessageReader implements IMessageReader {
		@Override
		public IBluetoothMessage parse(byte[] message) {
			WifiMessage wifiMessage = new WifiMessage();
			wifiMessage.unpack(message);
			return wifiMessage;
		}
	}
	
	public byte[] pack() {
		byte[] neighborBytes = deviceInfo.pack();
		
		List<byte[]> addrsAsBytes = new ArrayList<byte[]>();
		int totalBytes = 0;
		
		for( String addr : btDeviceAddrs) {
			byte[] addrBytes = addr.getBytes();
			totalBytes += addrBytes.length;
			addrsAsBytes.add(addrBytes);
		}
		
		byte[] message = new byte[4 + neighborBytes.length + 4 + totalBytes];
		int currentIndex = 0;
		
		byte[] lengthAsBytes = ByteBuffer.allocate(4).putInt(neighborBytes.length).array();
		System.arraycopy(lengthAsBytes, 0, message, currentIndex, 4);
		currentIndex += 4;
		
		System.arraycopy(neighborBytes, 0, message, currentIndex, neighborBytes.length);
		currentIndex += neighborBytes.length;
		
		lengthAsBytes = ByteBuffer.allocate(4).putInt(addrsAsBytes.size()).array();
		System.arraycopy(lengthAsBytes, 0, message, currentIndex, 4);
		currentIndex += 4;
		
		ListIterator<byte[]> iterator = addrsAsBytes.listIterator();
		while(iterator.hasNext()) {
			byte[] addrBytes = iterator.next();
			
			System.arraycopy(addrBytes, 0, message, currentIndex, addrBytes.length);
			currentIndex += addrBytes.length;
		}
		
		return message;
	}
	
	public void unpack(byte[] bytes) {
		
	}
	
	public byte getMessageType() {
		return WifiMessage.WIFI_MESSAGE;
	}
}
