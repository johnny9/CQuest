package edu.illinois.CS598rhk.models;

import java.nio.ByteBuffer;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;

public class WifiNeighbor implements IBluetoothMessage {
	public String name;
    public String address;
    
    public byte[] pack() {
            byte[] tempName = name.getBytes();
            byte[] tempAddress = address.getBytes();
            
            int msgLength = 4 + tempName.length + 4 + tempAddress.length;
            byte[] bytes = new byte[msgLength];
            int currentIndex = 0;
            
            byte[] nameLengthBytes = ByteBuffer.allocate(4).putInt(tempName.length).array();
            System.arraycopy(nameLengthBytes, 0, bytes, currentIndex, 4);
            currentIndex += 4;
            
            System.arraycopy(tempName, 0, bytes, currentIndex, tempName.length);
            currentIndex += tempName.length;
            
            byte[] addressLengthBytes = ByteBuffer.allocate(4).putInt(tempAddress.length).array();
            System.arraycopy(addressLengthBytes, 0, bytes, currentIndex, 4);
            currentIndex += 4;
            
            System.arraycopy(tempAddress, 0, bytes, currentIndex, tempAddress.length);
            currentIndex += tempAddress.length;
            
            return bytes;
    }
    
    public void unpack(byte[] bytes) {
            byte[] temp = new byte[4];
            System.arraycopy(bytes, 0, temp, 0, 4);
            int nameLength = ByteBuffer.wrap(temp).getInt();
            name = new String(bytes, 4, nameLength);
            
            System.arraycopy(bytes, 4 + nameLength, temp, 0, 4);
            int addressLength = ByteBuffer.wrap(temp).getInt();
            int indexOfAddress = 4 + nameLength + 4;
            address = new String(bytes, indexOfAddress, addressLength);
    }

    @Override
    public String toString() {
            String prettyString = "Neighbor:"
                    + "\n\tName: " + name
                    + "\n\tAddress: " + address;
            return prettyString;
    }
    
    @Override
    public boolean equals(Object o) {
            if (o instanceof WifiNeighbor) {
            	WifiNeighbor neighbor = (WifiNeighbor) o;
                    return (name.equals(neighbor.name) && address.equals(neighbor.address));
            }
            return false;
    }
	
	@Override
    public byte getMessageType() {
            return BluetoothMessage.WIFI_NEIGHBOR_HEADER;
    }
}
