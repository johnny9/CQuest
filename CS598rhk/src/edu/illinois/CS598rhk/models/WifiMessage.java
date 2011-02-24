package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;
import edu.illinois.CS598rhk.services.BluetoothService;

public class WifiMessage implements IBluetoothMessage {
	public static final byte WIFI_MESSAGE = 0;
	
	public Neighbor deviceInfo;
	private List<String> btDeviceAddrs;
	
	public WifiMessage(Neighbor device, List<String> btNeighborAddrs) {
		this.deviceInfo = device;
		this.btDeviceAddrs = btNeighborAddrs;
	}
	
	private WifiMessage() {
		// Do nothing
	}
	
	public static IMessageReader newMessageReader() {
		return new MessageReader();
	}
	
	private static class MessageReader implements IMessageReader {
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
		byte[] temp = new byte[4];
        
        int currentIndex = 0;
        System.arraycopy(bytes, currentIndex, temp, 0, 4);
        int neighborLength = ByteBuffer.wrap(temp).getInt();
        currentIndex += 4;
        
        byte[] neighborBytes = new byte[neighborLength];
        System.arraycopy(bytes, currentIndex, neighborBytes, 0, neighborLength);
        
        IMessageReader neighborReader = Neighbor.newNeighborReader();
        deviceInfo = (Neighbor) neighborReader.parse(neighborBytes);
        currentIndex += neighborLength;
        
        System.arraycopy(bytes, currentIndex, temp, 0, 4);
        int numAddrs = ByteBuffer.wrap(temp).getInt();
        currentIndex += 4;
        
        btDeviceAddrs = new ArrayList<String>();
        for(int i=0; i<numAddrs; ++i) {
        	byte[] addrBytes = new byte[BluetoothService.MAC_ADDR_BYTES_LENGTH];
        	System.arraycopy(bytes, currentIndex, addrBytes, 0, BluetoothService.MAC_ADDR_BYTES_LENGTH);
        	currentIndex += BluetoothService.MAC_ADDR_BYTES_LENGTH;
        	
        	btDeviceAddrs.add(new String(addrBytes));
        }
	}
	
	public byte getMessageType() {
		return WifiMessage.WIFI_MESSAGE;
	}

	public List<String> getNeighborAddrs() {
		List<String> addrs = new ArrayList<String>(btDeviceAddrs);
		Collections.copy(addrs, btDeviceAddrs);
		return addrs;
	}
}
