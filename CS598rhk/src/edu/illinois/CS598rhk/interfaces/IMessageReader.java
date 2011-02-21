package edu.illinois.CS598rhk.interfaces;

public interface IMessageReader {

	public IBluetoothMessage parse(byte[] message);
}
