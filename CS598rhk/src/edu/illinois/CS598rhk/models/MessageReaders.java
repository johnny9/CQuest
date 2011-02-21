package edu.illinois.CS598rhk.models;

import edu.illinois.CS598rhk.interfaces.IBluetoothMessage;
import edu.illinois.CS598rhk.interfaces.IMessageReader;

public class MessageReaders {

	private MessageReaders() {
		throw new AssertionError("Cannot create instances of MessageReaders.");
	}
	
	public static IMessageReader newInitiationReader() {
		return new InitiationReader();
	}
	
	public static IMessageReader newResponseReader() {
		return new ResponseReader();
	}
	
	public static IMessageReader newResultReader() {
		return new ResultReader();
	}
	
	public static IMessageReader newAcknowledgementReader() {
		return new AcknowledgementReader();
	}
	
	private static class InitiationReader implements IMessageReader {

		@Override
		public IBluetoothMessage parse(byte[] message) {
			return new ElectionInitiation();
		}
	}
	
	private static class ResponseReader implements IMessageReader {

		@Override
		public IBluetoothMessage parse(byte[] message) {
			ElectionResponse response = new ElectionResponse();
			response.unpack(message);
			return response;
		}
	}
	
	private static class ResultReader implements IMessageReader {

		@Override
		public IBluetoothMessage parse(byte[] message) {
			ElectionResult result = new ElectionResult();
			result.unpack(message);
			return result;
		}
	}
	
	private static class AcknowledgementReader implements IMessageReader {

		@Override
		public IBluetoothMessage parse(byte[] message) {
			ElectionAcknowledgement acknowledgement = new ElectionAcknowledgement();
			acknowledgement.unpack(message);
			return acknowledgement;
		}
	}
}
