package edu.illinois.CS598rhk.interfaces;

public interface IBluetoothMessage {
	public byte[] pack();
	
	public void unpack(byte[] bytes);
	
	public byte getMessageType();
}
